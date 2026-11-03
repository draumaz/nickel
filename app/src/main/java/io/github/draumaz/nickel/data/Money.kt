package io.github.draumaz.nickel.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Money {
    /**
     * The currency chosen in Settings, or `null` to follow the system locale. Snapshot state,
     * so composables that format money recompose when it changes. Amounts aren't converted:
     * switching only changes the symbol they're shown with.
     */
    var chosenCurrency: Currency? by mutableStateOf(null)

    /** The system locale's currency, falling back to US dollars where the locale has none. */
    val systemCurrency: Currency
        get() = runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()
            ?: Currency.getInstance("USD")

    val currency: Currency get() = chosenCurrency ?: systemCurrency

    // Always two decimals, whatever the currency's usual minor unit: amounts are stored and
    // typed in hundredths, so a yen total still shows the hundredths it was entered with.
    private val format: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = this@Money.currency
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    val symbol: String
        get() = currency.getSymbol(Locale.getDefault())

    /** Formats an unsigned amount, e.g. `$1,234.50`. */
    fun format(cents: Long): String = format.format(abs(cents) / 100.0)

    /** Formats with an explicit sign, e.g. `+$12.00` / `−$4.50`. */
    fun formatSigned(cents: Long): String = when {
        cents > 0 -> "+" + format(cents)
        cents < 0 -> "−" + format(cents)
        else -> format(0)
    }

    /**
     * Keypad input is a string of digits that fill in from the cents up, like a cash
     * register: `449` is 4.49 and `18945528` is 189,455.28.
     */
    fun parseInput(input: String): Long = input.toLongOrNull() ?: 0

    /** The inverse of [parseInput]: 1250 -> `1250`, and zero is empty input. */
    fun toInput(cents: Long): String = if (cents == 0L) "" else cents.toString()

    /** Renders keypad input with grouping and two decimals, e.g. `1,894.55`. */
    fun displayInput(input: String): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(parseInput(input), 2))
}
