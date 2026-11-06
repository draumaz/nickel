package io.github.draumaz.nickel.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Minimal RFC 4180 CSV reading and writing: quoted fields, escaped quotes, CRLF. */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("\uFEFF")
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                when {
                    c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { row.add(field.toString()); field.clear() }
                    '\r' -> {}
                    '\n' -> {
                        row.add(field.toString()); field.clear()
                        rows.add(row); row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filterNot { r -> r.all { it.isBlank() } }
    }

    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
}

/** What an import would do, shown to the user before anything is written. */
data class ImportPlan(
    val transactions: List<Transaction>,
    val newCategories: List<Category>,
    val duplicates: Int,
    val unreadable: Int,
    val earliest: Long?,
    val latest: Long?,
)

/** Why an import failed; the UI owns the wording so it can be translated. */
class ImportException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { UNOPENABLE, EMPTY_FILE, NO_COLUMNS }
}

object LedgerCsv {
    // Dime writes "2026-10-31 22:03:00 +0000"; exports from Nickel use the same shape.
    private val dimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US)

    private val zonedFormats = listOf(
        dimeFormat,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z", Locale.US),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
    )
    private val localDateTimeFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("M/d/yyyy H:mm", Locale.US),
        DateTimeFormatter.ofPattern("M/d/yyyy h:mm a", Locale.US),
    )
    private val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("M/d/yy", Locale.US),
    )

    fun parseDate(raw: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        for (f in zonedFormats) {
            try {
                return OffsetDateTime.parse(s, f).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
            try {
                return ZonedDateTime.parse(s, f).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        try {
            return Instant.parse(s).toEpochMilli()
        } catch (_: DateTimeParseException) {
        }
        for (f in localDateTimeFormats) {
            try {
                return LocalDateTime.parse(s, f).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        for (f in dateFormats) {
            try {
                // Date-only rows land at noon so they stay on the right day in any nearby zone.
                return LocalDate.parse(s, f).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    /** Parses "1,234.56", "$4.40", "-12", "(7.00)" into signed cents. */
    fun parseAmount(raw: String): Long? {
        var s = raw.trim()
        if (s.isEmpty()) return null
        var negative = false
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true
            s = s.substring(1, s.length - 1)
        }
        if (s.contains('-') || s.contains('−')) negative = true
        val digits = s.filter { it.isDigit() || it == '.' }
        if (digits.isEmpty() || digits.count { it == '.' } > 1) return null
        val cents = BigDecimal(digits).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        return if (negative) -cents else cents
    }

    private fun column(header: List<String>, vararg names: String): Int =
        header.indexOfFirst { h -> names.any { h.trim().equals(it, ignoreCase = true) } }

    /**
     * Turns CSV text into an [ImportPlan]. Columns are found by header name, so any export
     * with a date, an amount and ideally a category and type column will work.
     */
    fun plan(text: String, existing: List<Transaction>, customCategories: List<Category>): ImportPlan {
        val rows = Csv.parse(text)
        if (rows.isEmpty()) throw ImportException(ImportException.Reason.EMPTY_FILE)
        val header = rows.first()
        val dateCol = column(header, "date", "time", "timestamp", "datetime", "created")
        val amountCol = column(header, "amount", "value", "sum", "price")
        val noteCol = column(header, "note", "notes", "title", "description", "memo", "name", "payee")
        val categoryCol = column(header, "category", "categories", "tag")
        val typeCol = column(header, "type", "kind", "transaction type")
        if (dateCol < 0 || amountCol < 0) {
            throw ImportException(ImportException.Reason.NO_COLUMNS)
        }

        val known = (Categories.builtIns + customCategories).toMutableList()
        val created = mutableListOf<Category>()

        fun categoryFor(label: String, kind: Kind): Category {
            if (label.isBlank()) return Categories.fallback(kind)
            known.firstOrNull { it.kind == kind && it.label.equals(label.trim(), ignoreCase = true) }
                ?.let { return it }
            return Categories.custom(label.trim(), kind).also {
                known += it
                created += it
            }
        }

        fun key(t: Transaction) = listOf(
            t.timestamp, t.amountCents, t.kind, t.note.trim().lowercase(), t.category.label.lowercase(),
        )
        val seen = existing.mapTo(HashSet()) { key(it) }

        val baseId = System.currentTimeMillis() * 1000
        val result = mutableListOf<Transaction>()
        var duplicates = 0
        var unreadable = 0
        rows.drop(1).forEachIndexed { index, row ->
            fun cell(i: Int) = if (i >= 0) row.getOrNull(i).orEmpty().trim() else ""
            val timestamp = parseDate(cell(dateCol))
            val signed = parseAmount(cell(amountCol))
            if (timestamp == null || signed == null || signed == 0L) {
                unreadable++
                return@forEachIndexed
            }
            val type = cell(typeCol).lowercase()
            val kind = when {
                type.startsWith("inc") || type == "credit" || type == "deposit" -> Kind.INCOME
                type.startsWith("exp") || type == "debit" || type == "withdrawal" -> Kind.EXPENSE
                else -> if (signed > 0 && typeCol < 0) Kind.INCOME else Kind.EXPENSE
            }
            val t = Transaction(
                id = baseId + index,
                amountCents = kotlin.math.abs(signed),
                kind = kind,
                category = categoryFor(cell(categoryCol), kind),
                note = cell(noteCol).take(120),
                timestamp = timestamp,
            )
            if (!seen.add(key(t))) {
                duplicates++
            } else {
                result += t
            }
        }

        // Only keep new categories that something actually uses.
        val used = result.mapTo(HashSet()) { it.category.id }
        return ImportPlan(
            transactions = result,
            newCategories = created.filter { it.id in used },
            duplicates = duplicates,
            unreadable = unreadable,
            earliest = result.minOfOrNull { it.timestamp },
            latest = result.maxOfOrNull { it.timestamp },
        )
    }

    /** Writes the ledger in Dime's column layout so files round-trip between the two apps. */
    fun export(transactions: List<Transaction>): String = buildString {
        append("Date,Note,Amount,Category,Type\n")
        transactions.sortedByDescending { it.timestamp }.forEach { t ->
            val date = dimeFormat.format(Instant.ofEpochMilli(t.timestamp).atZone(ZoneOffset.UTC))
            val amount = BigDecimal(t.amountCents).movePointLeft(2).setScale(2).toPlainString()
            append(Csv.escape(date)).append(',')
            append(Csv.escape(t.note)).append(',')
            append(amount).append(',')
            append(Csv.escape(t.category.label)).append(',')
            append(if (t.kind == Kind.INCOME) "Income" else "Expense").append('\n')
        }
    }
}
