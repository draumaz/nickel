package io.github.draumaz.nickel.notices

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.NoticeParser
import io.github.draumaz.nickel.data.NoteMemory
import io.github.draumaz.nickel.data.NoticeTwins
import io.github.draumaz.nickel.data.RecentCapture
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.data.TransactionRepository
import io.github.draumaz.nickel.widget.updateNickelWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Turns payment and banking notifications into ledger entries. Runs only after the user
 * grants notification access in Android settings and flips the toggle on the Settings
 * screen; the toggle is the single source of truth, so revoking access just makes the
 * listener go quiet without touching stored data.
 *
 * Deliberately reads nothing but the notification text and writes nothing but a
 * transaction: no network, no clipboard, no contacts. The ledger is shared through the
 * same JSON files the app uses, so an entry added here shows up the moment Home refreshes.
 */
class NickelNoticeListener : NotificationListenerService() {

    // The service outlives any ViewModel, so it updates the widget itself: the ViewModel's
    // ledger watcher only runs while the app is open. The repository is the process-wide
    // one, so an open Home screen sees the capture immediately.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
        ).firstOrNull { !it.isNullOrBlank() }?.toString() ?: return

        // Fresh repository per check: reads the saved toggle even if the process was
        // restarted under a different task than the settings screen.
        if (!SettingsRepository(applicationContext).settings.value.autoAddNotices) return

        // Keyed on content as well as the notification key: messaging apps reuse one key per
        // conversation, so a bank's second alert in the same thread would otherwise be
        // dropped as already seen. A re-post of the same alert still matches and is skipped.
        val fingerprint = "${sbn.key}|${title.orEmpty()}|$text".hashCode().toString()
        val seen = seenKeys()
        if (fingerprint in seen) return

        val appName = labelFor(sbn.packageName)
        val match = NoticeParser.parse(sbn.packageName, appName, title, text, sbn.notification.channelId) ?: return
        remember(seen + fingerprint)

        // Matched and recorded here on the main thread, before any coroutine starts, so two
        // alerts for one tap arriving back to back can't both slip past as originals.
        val recent = NoticeTwins.prune(recentCaptures(), sbn.postTime)
        val twin = NoticeTwins.findTwin(recent, sbn.packageName, match, sbn.postTime)
        if (twin != null) {
            if (match.namesMerchant && !twin.namesMerchant) adoptMerchant(twin, match.note, recent)
            return
        }

        val note = localized(match.note)
        val transaction = Transaction(
            id = System.nanoTime(),
            amountCents = match.amountCents,
            kind = match.kind,
            // Notifications rarely name a category; "Other" until the coroutine below
            // checks the ledger for an earlier entry with the same note.
            category = Categories.fallback(match.kind),
            note = note,
            timestamp = sbn.postTime,
        )
        rememberCaptures(
            recent + RecentCapture(
                packageName = sbn.packageName,
                amountCents = match.amountCents,
                kind = match.kind,
                postTime = sbn.postTime,
                transactionId = transaction.id,
                note = note,
                namesMerchant = match.namesMerchant,
            )
        )

        scope.launch {
            val repository = TransactionRepository.get(applicationContext)
            // Same guess the entry sheet makes when a note is typed: file it where the last
            // entry with this note went. The kind stays as parsed, since the alert says
            // whether money came in or went out; anything unmatched stays in "Other".
            val filed = transaction.copy(category = recall(repository, note, match.kind) ?: transaction.category)
            repository.upsert(filed)
            updateNickelWidgets(applicationContext)
            notifyCaptured(filed, localized(match.source))
        }
    }

    private fun localized(label: String) =
        if (label == NoticeParser.SMS_SOURCE) getString(R.string.source_text_message) else label

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Fingerprints of notifications already turned into transactions, oldest first. Kept as
     * one newline-joined string rather than a string set, because a set has no order and
     * capping it would evict arbitrary entries, recent ones included.
     */
    private fun seenKeys(): List<String> =
        getSharedPreferences("notices", Context.MODE_PRIVATE).getString(KEY_SEEN, null)
            ?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    private fun remember(seen: List<String>) {
        getSharedPreferences("notices", Context.MODE_PRIVATE)
            .edit().putString(KEY_SEEN, seen.takeLast(MAX_SEEN).joinToString("\n")).apply()
    }

    /**
     * The card app's alert came first and saved a generic note ("Cash App"); Wallet's twin
     * names the store, which is what the user will want to read later. Swapped only while
     * the note is still the one we wrote, so an edit made in the meantime is never clobbered.
     */
    private fun adoptMerchant(twin: RecentCapture, merchant: String, recent: List<RecentCapture>) {
        rememberCaptures(recent.map { if (it == twin) it.copy(note = merchant, namesMerchant = true) else it })
        scope.launch {
            val repository = TransactionRepository.get(applicationContext)
            val category = recall(repository, merchant, twin.kind)
            repository.update(twin.transactionId) {
                if (it.note != twin.note) return@update it
                // The generic note's guess (usually "Other") gives way to the merchant's, but
                // only while it's still the default, so a category the user picked is kept.
                val keep = category == null || it.category != Categories.fallback(it.kind)
                it.copy(note = merchant, category = if (keep) it.category else category)
            }
            updateNickelWidgets(applicationContext)
        }
    }

    /**
     * The category earlier entries with [note] were filed under, via the same [NoteMemory]
     * the entry sheet uses. A cold process (the app never opened since boot) has no ledger
     * in memory yet, so it's read from disk first rather than guessed against nothing.
     */
    private suspend fun recall(repository: TransactionRepository, note: String, kind: Kind): Category? {
        val ledger = repository.ledger.value ?: run { repository.load(); repository.ledger.value } ?: return null
        return NoteMemory.of(ledger.transactions).lookup(note, kind, anyKind = false)?.category
    }

    private fun recentCaptures(): List<RecentCapture> = NoticeTwins.decode(
        getSharedPreferences("notices", Context.MODE_PRIVATE).getString(KEY_RECENT, null)
    )

    private fun rememberCaptures(recent: List<RecentCapture>) {
        getSharedPreferences("notices", Context.MODE_PRIVATE)
            .edit().putString(KEY_RECENT, NoticeTwins.encode(recent)).apply()
    }

    private fun labelFor(packageName: String): String? = runCatching {
        val info = packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        packageManager.getApplicationLabel(info).toString()
    }.getOrNull()

    /**
     * Announces a captured transaction with a Delete button, because a parse is a guess:
     * the undo puts the ledger back exactly as it was without opening the app. Skipped
     * silently when the runtime notification permission is missing, so capture still
     * works, just quieter, on Android 13+ installs that never granted it.
     */
    private fun notifyCaptured(transaction: Transaction, source: String) {
        if (!canNotify()) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_captured),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(R.string.channel_captured_description) }
        )

        val notificationId = (transaction.id % Int.MAX_VALUE).toInt()
        val undo = PendingIntent.getBroadcast(
            applicationContext,
            notificationId,
            Intent(applicationContext, NoticeUndoReceiver::class.java)
                .setAction(NoticeUndoReceiver.ACTION_UNDO)
                .putExtra(NoticeUndoReceiver.EXTRA_TRANSACTION_ID, transaction.id)
                .putExtra(NoticeUndoReceiver.EXTRA_NOTIFICATION_ID, notificationId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Tapping the body opens the app to edit the entry properly (reassign the
        // category, fix the amount); autoCancel clears the alert either way.
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                applicationContext,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        manager.notify(
            notificationId,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.transaction_added))
                .setContentText(getString(R.string.dot_pair, Money.formatSigned(transaction.signedCents), source))
                .setSubText(transaction.note)
                .setContentIntent(openApp)
                .addAction(0, getString(R.string.delete), undo)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .build(),
        )
    }

    private fun canNotify(): Boolean =
        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "captured"
        const val KEY_SEEN = "seen_fingerprints"
        const val KEY_RECENT = "recent_captures"
        const val MAX_SEEN = 256
    }
}
