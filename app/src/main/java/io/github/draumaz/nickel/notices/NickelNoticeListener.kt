package io.github.draumaz.nickel.notices

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.glance.appwidget.updateAll
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.NoticeParser
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.data.TransactionRepository
import io.github.draumaz.nickel.widget.NickelWidget
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

    // The service outlives any ViewModel, so it owns its plumbing: a fresh repository per
    // write keeps it correct across process restarts, and the widget gets updated here too
    // because the ViewModel's ledger watcher only runs while the app is open.
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

        val seen = seenKeys().toMutableSet()
        if (!seen.add(sbn.key)) return

        val appName = labelFor(sbn.packageName)
        val match = NoticeParser.parse(sbn.packageName, appName, title, text) ?: return
        remember(seen)

        scope.launch {
            val repository = TransactionRepository(applicationContext)
            repository.load()
            repository.upsert(
                Transaction(
                    id = System.nanoTime(),
                    amountCents = match.amountCents,
                    kind = match.kind,
                    // Notifications rarely name a category; drop them in "Other" and let
                    // the user reassign when they next open the app.
                    category = Categories.fallback(match.kind),
                    note = match.note,
                    timestamp = sbn.postTime,
                )
            )
            runCatching { NickelWidget().updateAll(applicationContext) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Notification keys already turned into transactions, capped so the file stays small. */
    private fun seenKeys() =
        getSharedPreferences("notices", Context.MODE_PRIVATE).getStringSet(KEY_SEEN, emptySet())
            ?: emptySet()

    private fun remember(seen: Set<String>) {
        val capped = if (seen.size > MAX_SEEN) seen.drop(seen.size - MAX_SEEN).toSet() else seen
        getSharedPreferences("notices", Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_SEEN, capped).apply()
    }

    private fun labelFor(packageName: String): String? = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
        packageManager.getApplicationLabel(info).toString()
    }.getOrNull()

    private companion object {
        const val KEY_SEEN = "seen"
        const val MAX_SEEN = 256
    }
}
