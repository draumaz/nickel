package io.github.draumaz.nickel.notices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.NoticeApps
import io.github.draumaz.nickel.data.SettingsRepository

/** An app the picker can offer: its package and the name the launcher shows for it. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * The Android half of [NoticeApps]: which apps are installed, making the first list, and
 * letting newly installed financial apps join it with a notification saying so.
 *
 * Only launcher apps are visible (the manifest's `<queries>` asks for exactly those), which
 * is every app a person would think to pick; QUERY_ALL_PACKAGES would also expose services
 * and overlays nobody recognizes, and Play restricts it.
 */
object WatchedApps {

    private const val CHANNEL_ID = "watched_apps"

    /** Launcher apps other than Nickel itself, sorted by label. Slow: call off the main thread. */
    fun installed(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** The posting app's label, or `null` if it can't be seen or has gone. */
    fun labelFor(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))).toString()
    }.getOrNull()

    /**
     * Makes the first list if there isn't one yet, and returns the current list. Everything
     * installed now is marked as weighed, so only later installs are considered for joining
     * on their own and announced. Synchronized with [consider]: the Settings screen and the
     * listener can both get here first, on different threads.
     */
    @Synchronized
    fun ensureList(context: Context, settings: SettingsRepository): Set<String> {
        settings.readNoticeApps()?.let { return it }
        val installed = installed(context).associate { it.packageName to it.label }
        val selected = NoticeApps.initialSelection(installed, settings.settings.value.autoAddNotices)
        settings.initNoticeApps(selected, installed.keys)
        return selected
    }

    /**
     * Weighs an app the list hasn't met before: a fresh install, or one that was never in
     * the launcher and just posted a notification. A financial-looking one joins the list
     * and the user is told, so they can take it back out in Settings. Returns whether
     * [packageName] is now on the list.
     */
    @Synchronized
    fun consider(context: Context, settings: SettingsRepository, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        val list = ensureList(context, settings)
        if (packageName in list) return true
        if (packageName in settings.knownNoticeApps()) return false
        val label = labelFor(context, packageName)
        val join = NoticeApps.looksFinancial(packageName, label)
        settings.markNoticeAppKnown(packageName, select = join)
        // Announced only while auto-add is on: otherwise nothing is listening yet, and the
        // app still joins quietly so it's already picked if the feature is turned on later.
        if (join && settings.settings.value.autoAddNotices) {
            announce(context, label ?: packageName.substringAfterLast('.'))
        }
        return join
    }

    /**
     * Tells the user an app joined, tapping through to Nickel where Settings can undo it.
     * Skipped when the runtime notification permission is missing, like capture alerts.
     */
    private fun announce(context: Context, label: String) {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_watched_apps),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_watched_apps_description) }
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, label.hashCode(), it, PendingIntent.FLAG_IMMUTABLE)
        }
        manager.notify(
            "watched:$label".hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.app_now_watched, label))
                .setContentText(context.getString(R.string.app_now_watched_body))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .build(),
        )
    }

    /**
     * Hears installs while the notification listener is bound, which is exactly when auto-add
     * is running. PACKAGE_ADDED can't be declared in the manifest since Android 8, so the
     * listener registers this itself; an install missed while unbound is still caught by
     * [consider] the first time that app posts a notification.
     */
    class InstallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
            // An update of an app already installed isn't a new app.
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val packageName = intent.data?.schemeSpecificPart ?: return
            consider(context, SettingsRepository(context.applicationContext), packageName)
        }
    }
}
