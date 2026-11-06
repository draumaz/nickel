package io.github.draumaz.nickel.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode

import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.draumaz.nickel.MainActivity
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.SettingsRepository
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.data.TransactionRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Home-screen widget: the net total over the latest few transactions. */
class NickelWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        SettingsRepository(context) // applies the chosen currency to Money
        val repository = TransactionRepository.get(context)
        repository.load()
        val transactions = repository.ledger.value?.transactions.orEmpty()
        val net = transactions.sumOf { it.signedCents }
        val recent = transactions.take(MAX_ROWS)

        provideContent {
            GlanceTheme {
                WidgetContent(net, recent)
            }
        }
    }

    private companion object {
        const val MAX_ROWS = 4
    }
}

// The app's income green, without the dynamic-color harmonizing Glance can't do.
private val IncomeColor = ColorProvider(day = Color(0xFF1B7A45), night = Color(0xFF8FDCA8))

private const val HEADER_HEIGHT_DP = 72
private const val ROW_HEIGHT_DP = 30

/** Below this width the date is dropped and padding tightens, leaving room for names. */
private val CompactWidth = 200.dp

@Composable
private fun WidgetContent(netCents: Long, recent: List<Transaction>) {
    // Show as many rows as the widget's height allows.
    val size = LocalSize.current
    val rows = ((size.height.value - HEADER_HEIGHT_DP) / ROW_HEIGHT_DP).toInt()
        .coerceIn(0, recent.size)
    val compact = size.width < CompactWidth

    // Too short for any rows: show the net total on its own, centered and sized to fit.
    if (recent.isNotEmpty() && rows == 0) {
        NetOnly(netCents)
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = 14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            "Net",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
        Text(
            Money.formatSigned(netCents),
            maxLines = 1,
            style = TextStyle(
                color = if (netCents > 0) IncomeColor else GlanceTheme.colors.onSurface,
                fontSize = if (compact) 22.sp else 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        if (recent.isEmpty()) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "No transactions yet",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        } else if (rows > 0) {
            Spacer(GlanceModifier.height(8.dp))
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(16.dp)
                    .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 4.dp),
            ) {
                recent.take(rows).forEach { TransactionRow(it, showDate = !compact) }
            }
        }
    }
}

/**
 * Centered net total for widget sizes too short to show any transactions. The amount is sized
 * from the space available and the length of the string, so long totals still fit on one line.
 */
@Composable
private fun NetOnly(netCents: Long) {
    val size = LocalSize.current
    val fontScale = LocalContext.current.resources.configuration.fontScale
    val amount = Money.formatSigned(netCents)

    val horizontalPadding = 12f
    val labelSize = 13f
    // Bold digits run about 0.6em wide; leave a little slack for symbols and separators.
    val byWidth = (size.width.value - 2 * horizontalPadding) / (amount.length * 0.62f)
    // Keep room for the label and vertical padding.
    val byHeight = (size.height.value - 2 * 10f - labelSize * 1.4f) / 1.3f
    val amountSize = (minOf(byWidth, byHeight, 40f) / fontScale).coerceAtLeast(14f)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = horizontalPadding.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Net",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = labelSize.sp),
        )
        Text(
            amount,
            maxLines = 1,
            style = TextStyle(
                color = if (netCents > 0) IncomeColor else GlanceTheme.colors.onSurface,
                fontSize = amountSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
private fun TransactionRow(transaction: Transaction, showDate: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height((ROW_HEIGHT_DP - 4).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            transaction.note.ifBlank { transaction.category.label },
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 14.sp),
        )
        if (showDate) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                dateFormat.format(Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault())),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Text(
            Money.formatSigned(transaction.signedCents),
            maxLines = 1,
            style = TextStyle(
                color = if (transaction.kind == Kind.INCOME) IncomeColor else GlanceTheme.colors.onSecondaryContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
