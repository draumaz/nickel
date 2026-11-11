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
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _backup = MutableStateFlow(run {
        skipPromptOnUpdatedInstall(context)
        readBackup()
    })
    val backup: StateFlow<BackupSettings> = _backup.asStateFlow()

    /**
     * The backup worker writes its result through its own instance, so this one watches the
     * shared preferences to show it. Held in a field: SharedPreferences keeps listeners
     * only weakly, and a lambda passed straight in would be collected and fall silent.
     */
    private val backupListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key.startsWith(BACKUP_PREFIX)) _backup.value = readBackup()
    }

    // Constructing the repository applies the saved currency, so the app and the widget
    // format money the same way.
    init {
        Money.chosenCurrency = currencyOf(_settings.value.currencyCode)
        prefs.registerOnSharedPreferenceChangeListener(backupListener)
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

    private companion object {
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_CURRENCY = "currency"
        const val KEY_AUTO_ADD_NOTICES = "auto_add_notices"
        const val BACKUP_PREFIX = "backup_"
        const val KEY_BACKUP_FOLDER = "backup_folder"
        const val KEY_BACKUP_FOLDER_NAME = "backup_folder_name"
        const val KEY_BACKUP_INTERVAL = "backup_interval_hours"
        const val KEY_BACKUP_PROMPTED = "backup_prompted"
        const val KEY_BACKUP_LAST = "backup_last"
        const val KEY_BACKUP_FAILED = "backup_failed"
    }
}
