package io.github.draumaz.nickel.data

/**
 * A money amount and its direction, parsed out of a notification from a payment app, bank,
 * or text message ("You received $25.00 from Alex via Zelle").
 */
data class NoticeMatch(
    val amountCents: Long,
    val kind: Kind,
    /** Where the notification came from, e.g. "Zelle" or "Text message". */
    val source: String,
    val note: String,
)

/**
 * Pure text parsing, so the notification listener is thin Android plumbing around logic
 * that can be unit-tested on the JVM like the CSV import.
 *
 * Deliberately conservative: a notification becomes a transaction only if it carries a
 * currency-marked amount *and* a direction word (received, paid, deposit, charged...).
 * Dollar figures in unrelated alerts ("Save $10 on your next order") shouldn't silently
 * land in the ledger, so a missing direction or a missing symbol is a skip, not a guess.
 */
object NoticeParser {

    // Two shapes only: a symbol- or code-prefixed amount ($1,234.56, USD 25) or a
    // code-suffixed one (25.00 USD). A bare number is never a transaction.
    private val amount = Regex(
        "(?:(?:[$€£¥₹]|(?:USD|EUR|GBP|CAD|AUD|JPY|CHF|INR)\\s)\\s?\\d[\\d,]*(?:\\.\\d{1,2})?)" +
            "|(?:\\d[\\d,]*(?:\\.\\d{1,2})?\\s(?:USD|EUR|GBP|CAD|AUD|JPY|CHF|INR))",
        RegexOption.IGNORE_CASE,
    )
    private val number = Regex("[\\d,]+(?:\\.\\d{1,2})?")

    // Checked before the expense list: "sent you" must win over a later bare "sent".
    private val incomeWords = listOf(
        "sent you", "paid you", "you received", "received", "deposited", "deposit",
        "credited", "refund", "reimbursed", "transfer from", "you got", "got paid",
        "paycheck", "direct deposit", "cash in", "payment received", "incoming payment",
    )
    private val expenseWords = listOf(
        "you sent", "sent to", "you paid", "paid to", "purchase", "charged", "charge of",
        "withdrawal", "withdrew", "debit", "you spent", "spent", "autopay", "auto-pay",
        "bill pay", "payment sent", "payment to", "subscription", "order of", "fee of",
        "fee was", "tipped",
    )

    // Word-bounded so "spend" doesn't trip "spent", and direction is judged on the body
    // first: the title ("Payment sent") can disagree with what actually happened.
    private val incomeRegex = anyOf(incomeWords)
    private val expenseRegex = anyOf(expenseWords)

    private fun anyOf(words: List<String>): Regex =
        Regex("\\b(?:${words.joinToString("|") { Regex.escape(it) }})\\b", RegexOption.IGNORE_CASE)

    // Text-message apps: the source label matters more than the package, and these are the
    // carriers banks and Zelle use for their alerts. Unknown packages aren't excluded:
    // parsing still demands a direction word, so a random app mentioning money is skipped.
    private val smsPackages = listOf(
        "com.google.android.apps.messaging", "com.android.mms", "com.android.messaging",
        "com.textra", "com.klinker.android.send_message", "com.simplemobiletools.smsmessenger",
    )

    /**
     * @param appName the resolved label of the posting app, or `null` if it couldn't be read.
     * @return the transaction to add, or `null` when the text isn't a clear money event.
     */
    fun parse(packageName: String, appName: String?, title: String?, text: String?): NoticeMatch? {
        val combined = listOfNotNull(title, text).filter { it.isNotBlank() }.joinToString(" ")
        if (combined.isBlank()) return null
        val cents = amount.find(combined)?.let { centsOf(it.value) } ?: return null
        if (cents == 0L) return null

        fun kindOf(part: String?): Kind? = when {
            part.isNullOrBlank() -> null
            incomeRegex.containsMatchIn(part) -> Kind.INCOME
            expenseRegex.containsMatchIn(part) -> Kind.EXPENSE
            else -> null
        }
        val kind = kindOf(text) ?: kindOf(title) ?: return null

        val source = when {
            smsPackages.any(packageName::startsWith) -> "Text message"
            !appName.isNullOrBlank() -> appName
            else -> packageName.substringAfterLast('.')
        }
        return NoticeMatch(
            amountCents = cents,
            kind = kind,
            source = source,
            note = title?.trim()?.takeIf { it.isNotEmpty() } ?: source,
        )
    }

    private fun centsOf(matched: String): Long? {
        // "1,234.5" -> one thousand two hundred thirty-four dollars and fifty cents, the
        // way payment apps write it; a lone fraction digit is read as tenths of a dollar.
        val parts = number.find(matched)?.value?.replace(",", "")?.split('.', limit = 2) ?: return null
        val whole = parts[0].toLongOrNull() ?: return null
        val fractionCents = when (val fraction = parts.getOrNull(1)) {
            null, "" -> 0L
            else -> when (fraction.length) {
                1 -> fraction.toLong() * 10
                else -> fraction.take(2).toLong()
            }
        }
        return whole * 100 + fractionCents
    }
}
