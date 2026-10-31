package io.github.draumaz.nickel.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val dynamicColor: Boolean,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val dynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val _settings = MutableStateFlow(
        Settings(dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, dynamicColorSupported))
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    private companion object {
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
