package io.github.draumaz.nickel.data

/**
 * Which apps' notifications auto-add listens to, and the guess that decides which newly
 * installed apps join that list on their own. Pure, like [NoticeParser], so the rules are
 * unit-tested on the JVM and the Android side only supplies package names and labels.
 */
object NoticeApps {

    /**
     * Brand names that don't contain a generic money word. Matched as whole words of the
     * label or package, never as substrings: "chase" shouldn't catch "purchased", and short
     * ones like "citi" or "wise" would hit half the app drawer as substrings.
     */
    private val brands = setOf(
        "venmo", "zelle", "chase", "revolut", "monzo", "chime", "sofi", "robinhood", "coinbase",
        "klarna", "affirm", "citi", "citimobile", "usaa", "amex", "americanexpress", "capitalone",
        "transferwise", "wise", "bofa", "starling", "n26", "nubank", "ynab", "splitwise",
        "etrade", "fidelity", "schwab", "vanguard", "barclays", "hsbc", "santander", "sberbank",
        "tinkoff", "tbank", "alfabank", "wellsfargo", "wellsfargomobile", "pnc", "truist",
        "ally", "navyfederal", "kraken", "binance", "brex", "creditkarma",
    )

    /**
     * Roots distinctive enough to match inside a word: "nubank", "discoverfinancial",
     * "walletnfcrel" (Google Wallet), "moneylion", "cashapp".
     */
    private val roots = listOf("bank", "financ", "wallet", "paypal", "credit", "invest", "money", "cash", "crypto")

    /**
     * Short "pay" only at either end of a word ("gpay", "spay", "payments", "paytm"), so
     * "display" and "playstore" stay out.
     */
    private const val PAY = "pay"

    private val split = Regex("[^\\p{L}\\p{N}]+")

    /**
     * A guess at whether an app posts payment or banking alerts, from its package name and
     * label alone. Text-message apps count too: that's where banks and Zelle send alerts.
     * It errs toward including, because a wrongly included app costs nothing (the parser
     * still demands an amount and a direction word) while a missed bank loses entries.
     */
    fun looksFinancial(packageName: String, label: String?): Boolean {
        if (NoticeParser.isSmsPackage(packageName)) return true
        val words = sequenceOf(packageName, label.orEmpty())
            .flatMap { it.lowercase().split(split) }
            .filter { it.isNotEmpty() }
        return words.any { word ->
            word in brands ||
                roots.any { it in word } ||
                word.startsWith(PAY) || word.endsWith(PAY)
        }
    }

    /**
     * The first list, made once when the feature meets an install. Someone who already had
     * auto-add on was being listened to from every app, so they start with everything picked
     * and lose nothing on update; everyone else starts with the apps that look financial.
     */
    fun initialSelection(installed: Map<String, String?>, wasListeningToAll: Boolean): Set<String> =
        if (wasListeningToAll) installed.keys
        else installed.filter { (pkg, label) -> looksFinancial(pkg, label) }.keys
}
