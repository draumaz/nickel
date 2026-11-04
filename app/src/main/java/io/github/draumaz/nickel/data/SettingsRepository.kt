package io.github.draumaz.nickel.data

import android.content.Context
import android.os.Build
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

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val _settings = MutableStateFlow(
        Settings(
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, dynamicColorSupported),
            currencyCode = prefs.getString(KEY_CURRENCY, null),
            autoAddNotices = prefs.getBoolean(KEY_AUTO_ADD_NOTICES, false),
        )
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    // Constructing the repository applies the saved currency, so the app and the widget
    // format money the same way.
    init {
        Money.chosenCurrency = currencyOf(_settings.value.currencyCode)
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
    }
}
