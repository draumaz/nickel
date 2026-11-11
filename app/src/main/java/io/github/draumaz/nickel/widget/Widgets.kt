package io.github.draumaz.nickel.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Redraws every Nickel home-screen widget. Callers go through here rather than naming a widget,
 * so a new widget can't be forgotten when the ledger or currency changes.
 */
suspend fun updateNickelWidgets(context: Context) {
    runCatching { NickelWidget().updateAll(context) }
    runCatching { TodayWidget().updateAll(context) }
}
