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

    /** Source label for texts; English here, swapped for the translation where it's shown. */
    const val SMS_SOURCE = "Text message"

    // A number as payment apps print it in any locale: "1,234.56", "1.234,56", "1 234,56"
    // (Russian and French group with no-break spaces). It must end on a digit so a sentence's
    // closing period isn't read as a decimal point. Which separator is decimal is settled in
    // centsOf, not here.
    private const val NUM = "\\d(?:[\\d,.\u00A0\u202F]*\\d)?"
    private const val CODE = "(?:USD|EUR|GBP|CAD|AUD|JPY|CHF|INR)"
    private const val SYMBOL = "[$€£¥₹]"
    private const val GAP = "[\\s\u00A0\u202F]"

    // Symbol- or code-marked amounts only, on either side of the number: "$1,234.56",
    // "USD 25", "25.00 USD", and "8,83 $" the way Google Wallet writes it in a European or
    // Russian locale. A bare number is never a transaction. A trailing symbol must not be
    // followed by a digit, so "5 $10 coupons" reads as $10 rather than $5.
    private val amount = Regex(
        "(?:(?:$SYMBOL|$CODE\\s)\\s?$NUM)" +
            "|(?:$NUM$GAP$CODE)" +
            "|(?:$NUM$GAP?$SYMBOL(?!\\d))",
        RegexOption.IGNORE_CASE,
    )
    private val number = Regex(NUM)

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
     * Google Wallet's tap-to-pay receipts ("SAFEWAY #1551" / "$8.83 with Visa ••1234") carry
     * no direction word in any language, but everything on that channel is a card purchase,
     * so the channel itself is the direction. Gated on the channel rather than the package
     * because Wallet also posts offers and pass updates that can mention money.
     */
    private const val WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"
    private const val WALLET_PURCHASE_CHANNEL = "tapandpay.transactions"

    /**
     * @param appName the resolved label of the posting app, or `null` if it couldn't be read.
     * @param channel the notification's channel id, used to recognize apps whose purchase
     *   alerts have no direction word.
     * @return the transaction to add, or `null` when the text isn't a clear money event.
     */
    fun parse(
        packageName: String,
        appName: String?,
        title: String?,
        text: String?,
        channel: String? = null,
    ): NoticeMatch? {
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
        val kind = kindOf(text) ?: kindOf(title)
            ?: Kind.EXPENSE.takeIf { isWalletPurchase(packageName, channel) }
            ?: return null

        val source = when {
            smsPackages.any(packageName::startsWith) -> SMS_SOURCE
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

    private fun isWalletPurchase(packageName: String, channel: String?) =
        packageName == WALLET_PACKAGE && channel?.startsWith(WALLET_PURCHASE_CHANNEL) == true

    private fun centsOf(matched: String): Long? {
        val raw = number.find(matched)?.value?.filterNot { it == '\u00A0' || it == '\u202F' }
            ?: return null
        // The last separator is the decimal point only when one or two digits follow it:
        // "8,83" and "1.234,56" are cents, "1,234" is a thousand. A lone fraction digit is
        // tenths ("$12.5" is twelve fifty), the way payment apps write it.
        val last = raw.indexOfLast { it == '.' || it == ',' }
        val fraction = if (last >= 0) raw.substring(last + 1) else ""
        val decimal = last >= 0 && fraction.length in 1..2
        val whole = (if (decimal) raw.substring(0, last) else raw).filter(Char::isDigit)
            .toLongOrNull() ?: return null
        val fractionCents = when {
            !decimal -> 0L
            fraction.length == 1 -> fraction.toLong() * 10
            else -> fraction.toLong()
        }
        return whole * 100 + fractionCents
    }
}
