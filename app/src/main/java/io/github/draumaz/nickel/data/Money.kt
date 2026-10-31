package io.github.draumaz.nickel.data

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

    /** Parses keypad input such as `12.5` into cents. */
    fun parseInput(input: String): Long {
        if (input.isEmpty() || input == ".") return 0
        val parts = input.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLong()
        val frac = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2).toLong()
        return whole * 100 + frac
    }

    /** The inverse of [parseInput], trimming a zero fraction: 1250 -> `12.5`. */
    fun toInput(cents: Long): String {
        val whole = cents / 100
        val frac = cents % 100
        return when {
            frac == 0L -> whole.toString()
            frac % 10 == 0L -> "$whole.${frac / 10}"
            else -> "$whole.${frac.toString().padStart(2, '0')}"
        }
    }

    /** Renders keypad input with grouping separators, keeping what the user typed. */
    fun displayInput(input: String): String {
        if (input.isEmpty()) return "0"
        val parts = input.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLong()
        val grouped = NumberFormat.getIntegerInstance(Locale.getDefault()).format(whole)
        return if (input.contains('.')) "$grouped.${parts.getOrElse(1) { "" }}" else grouped
    }
}
