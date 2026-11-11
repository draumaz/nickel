package io.github.draumaz.nickel.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.TransactionRepository
import io.github.draumaz.nickel.data.on
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home-screen widget: today's net total and the latest few of today's transactions. "Today" is
 * fixed when the widget renders, so the receiver also redraws it when the date or clock changes,
 * and the provider info asks for a periodic refresh as a backstop for midnight.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SettingsRepository(context) // applies the chosen currency to Money
        val repository = TransactionRepository.get(context)
        repository.load()
        val zone = ZoneId.systemDefault()
        val today = repository.ledger.value?.transactions.orEmpty().on(LocalDate.now(zone), zone)
        val net = today.sumOf { it.signedCents }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    label = context.getString(R.string.widget_today),
                    emptyText = context.getString(R.string.widget_today_empty),
                    netCents = net,
                    recent = today.take(MAX_ROWS),
                    stamp = WidgetStamp.TIME,
                )
            }
        }
    }

    private companion object {
        const val MAX_ROWS = 3
    }
}
