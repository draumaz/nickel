package io.github.draumaz.nickel.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Currency

data class Settings(
    val dynamicColor: Boolean,
    /** ISO 4217 code, or `null` to follow the system locale. */
    val currencyCode: String?,
    /** Add transactions parsed out of payment and bank notifications. */
    val autoAddNotices: Boolean,
    /** A widget tap opens straight into a new entry rather than just opening the app. */
    val widgetOpensEntry: Boolean,
)

data class BackupSettings(
    /** A persisted SAF tree URI, or `null` before a folder is chosen. */
    val folderUri: String?,
    /** What the folder was called when picked, for display without querying the provider. */
    val folderName: String?,
    /** 0 when automatic backups are off; otherwise one of [BackupFiles.INTERVALS_HOURS]. */
    val intervalHours: Int,
    /** Whether the first-run backup question has been answered either way. */
    val prompted: Boolean,
    /** Epoch millis of the last successful backup, or 0 for never. */
    val lastBackupAt: Long,
    /** The last attempt failed, usually because the folder was deleted or access revoked. */
    val lastBackupFailed: Boolean,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        Settings(
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            currencyCode = prefs.getString(KEY_CURRENCY, null),
            autoAddNotices = prefs.getBoolean(KEY_AUTO_ADD_NOTICES, false),
            widgetOpensEntry = prefs.getBoolean(KEY_WIDGET_OPENS_ENTRY, true),
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _backup = MutableStateFlow(run {
        skipPromptOnUpdatedInstall(context)
        readBackup()
    })
    val backup: StateFlow<BackupSettings> = _backup.asStateFlow()

    private val _noticeApps = MutableStateFlow(readNoticeApps())
    /**
     * Packages whose notifications auto-add reads, or `null` before the list has been made
     * (the first time the listener or the Settings screen meets this install).
     */
    val noticeApps: StateFlow<Set<String>?> = _noticeApps.asStateFlow()

    /**
     * The backup worker and the notification listener write through their own instances, so
     * this one watches the shared preferences to show what they wrote. Held in a field:
     * SharedPreferences keeps listeners only weakly, and a lambda passed straight in would
     * be collected and fall silent.
     */
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key.startsWith(BACKUP_PREFIX)) _backup.value = readBackup()
        if (key == null || key == KEY_NOTICE_APPS) _noticeApps.value = readNoticeApps()
    }

    // Constructing the repository applies the saved currency, so the app and the widget
    // format money the same way.
    init {
        Money.chosenCurrency = currencyOf(_settings.value.currencyCode)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    /**
     * The backup question belongs to first-run setup only. An install that has been updated
     * since it was first installed has already been set up, so people updating into this
     * feature aren't stopped by a modal; they find it in Settings instead. Settled once and
     * saved, so later updates can't bring it back for someone who installed fresh.
     */
    private fun skipPromptOnUpdatedInstall(context: Context) {
        if (prefs.contains(KEY_BACKUP_PROMPTED)) return
        val updated = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.lastUpdateTime > info.firstInstallTime
        }.getOrDefault(false)
        if (updated) prefs.edit().putBoolean(KEY_BACKUP_PROMPTED, true).apply()
    }

    private fun readBackup() = BackupSettings(
        folderUri = prefs.getString(KEY_BACKUP_FOLDER, null),
        folderName = prefs.getString(KEY_BACKUP_FOLDER_NAME, null),
        intervalHours = prefs.getInt(KEY_BACKUP_INTERVAL, 0),
        prompted = prefs.getBoolean(KEY_BACKUP_PROMPTED, false),
        lastBackupAt = prefs.getLong(KEY_BACKUP_LAST, 0L),
        lastBackupFailed = prefs.getBoolean(KEY_BACKUP_FAILED, false),
    )

    /** Choosing a folder also answers the first-run question. */
    fun setBackupFolder(uri: String, name: String?, intervalHours: Int) {
        prefs.edit()
            .putString(KEY_BACKUP_FOLDER, uri)
            .putString(KEY_BACKUP_FOLDER_NAME, name)
            .putInt(KEY_BACKUP_INTERVAL, intervalHours)
            .putBoolean(KEY_BACKUP_PROMPTED, true)
            .putBoolean(KEY_BACKUP_FAILED, false)
            .apply()
    }

    fun setBackupInterval(hours: Int) {
        prefs.edit().putInt(KEY_BACKUP_INTERVAL, hours).apply()
    }

    fun setBackupPrompted() {
        prefs.edit().putBoolean(KEY_BACKUP_PROMPTED, true).apply()
    }

    fun recordBackup(succeeded: Boolean, at: Long = System.currentTimeMillis()) {
        prefs.edit().apply {
            if (succeeded) putLong(KEY_BACKUP_LAST, at)
            putBoolean(KEY_BACKUP_FAILED, !succeeded)
        }.apply()
    }

    /** Pass `null` to go back to the system currency. */
    fun setCurrency(code: String?) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _settings.value = _settings.value.copy(currencyCode = code)
        Money.chosenCurrency = currencyOf(code)
    }

    private fun currencyOf(code: String?): Currency? =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setAutoAddNotices(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ADD_NOTICES, enabled).apply()
        _settings.value = _settings.value.copy(autoAddNotices = enabled)
    }

    /**
     * Straight from the preferences rather than [noticeApps], which another instance's write
     * reaches only once the change callback runs on the main thread. Copied: the set
     * SharedPreferences hands back must not be modified or kept.
     */
    fun readNoticeApps(): Set<String>? = prefs.getStringSet(KEY_NOTICE_APPS, null)?.toSet()

    /** Every package that has already been weighed for auto-joining, picked or not. */
    fun knownNoticeApps(): Set<String> = prefs.getStringSet(KEY_NOTICE_APPS_KNOWN, null)?.toSet().orEmpty()

    /**
     * Makes the first list. [known] is everything installed at the time, so only apps
     * installed later are weighed for joining automatically.
     */
    fun initNoticeApps(selected: Set<String>, known: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_NOTICE_APPS, selected)
            .putStringSet(KEY_NOTICE_APPS_KNOWN, known)
            .commit()
        _noticeApps.value = selected
    }

    /**
     * Records that [packageName] has been weighed, adding it to the list if [select].
     * Committed synchronously, so a second notification from the same new app, arriving
     * before an apply would land, can't announce it twice.
     */
    fun markNoticeAppKnown(packageName: String, select: Boolean) {
        val edit = prefs.edit().putStringSet(KEY_NOTICE_APPS_KNOWN, knownNoticeApps() + packageName)
        if (select) edit.putStringSet(KEY_NOTICE_APPS, readNoticeApps().orEmpty() + packageName)
        edit.commit()
        _noticeApps.value = readNoticeApps()
    }

    /**
     * Replaces the list outright, for the picker's own toggles and select all. Every app
     * whose box changed is marked as weighed too: one that came in through select all (or
     * was never met by the listener) and is then unticked would otherwise be weighed again
     * at its next alert and, looking financial, put itself back on the list.
     */
    fun setNoticeApps(packages: Set<String>) {
        val previous = readNoticeApps().orEmpty()
        val changed = (previous - packages) + (packages - previous)
        prefs.edit()
            .putStringSet(KEY_NOTICE_APPS, packages)
            .putStringSet(KEY_NOTICE_APPS_KNOWN, knownNoticeApps() + changed)
            .apply()
        _noticeApps.value = packages
    }

    fun setWidgetOpensEntry(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIDGET_OPENS_ENTRY, enabled).apply()
        _settings.value = _settings.value.copy(widgetOpensEntry = enabled)
    }

    private companion object {
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_CURRENCY = "currency"
        const val KEY_AUTO_ADD_NOTICES = "auto_add_notices"
        const val KEY_WIDGET_OPENS_ENTRY = "widget_opens_entry"
        const val KEY_NOTICE_APPS = "notice_apps"
        const val KEY_NOTICE_APPS_KNOWN = "notice_apps_known"
        const val BACKUP_PREFIX = "backup_"
        const val KEY_BACKUP_FOLDER = "backup_folder"
        const val KEY_BACKUP_FOLDER_NAME = "backup_folder_name"
        const val KEY_BACKUP_INTERVAL = "backup_interval_hours"
        const val KEY_BACKUP_PROMPTED = "backup_prompted"
        const val KEY_BACKUP_LAST = "backup_last"
        const val KEY_BACKUP_FAILED = "backup_failed"
    }
}
