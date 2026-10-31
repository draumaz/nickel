package io.github.draumaz.nickel.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

object Money {
    private val format: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.getDefault())

    val symbol: String
        get() = runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrDefault("$")

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
