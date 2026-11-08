package io.github.draumaz.nickel.data

/**
 * A transaction the notification listener recently added, remembered so a second app's
 * alert for the same payment can be recognized. One tap to pay with a Cash App card fires
 * both Google Wallet's receipt and Cash App's own alert, and each alone parses as a
 * perfectly good purchase.
 */
data class RecentCapture(
    val packageName: String,
    val amountCents: Long,
    val kind: Kind,
    val postTime: Long,
    val transactionId: Long,
    /** The note the transaction was saved with, to tell whether the user has edited it since. */
    val note: String,
    val namesMerchant: Boolean,
)

/**
 * Pure matching, kept off the Android side so it's unit-testable like [NoticeParser].
 *
 * A twin is the same amount in the same direction from a *different* app within
 * [WINDOW_MS]. The different-app rule is what keeps real repeats intact: two identical
 * coffees bought minutes apart arrive as two alerts from one app and are both kept.
 */
object NoticeTwins {

    /**
     * Wide enough for a card issuer whose push lags the tap by a minute or two, narrow
     * enough that a coincidentally equal charge on another card later in the day still
     * counts on its own.
     */
    const val WINDOW_MS = 5 * 60 * 1000L

    fun findTwin(
        recent: List<RecentCapture>,
        packageName: String,
        match: NoticeMatch,
        postTime: Long,
    ): RecentCapture? = recent.lastOrNull {
        it.packageName != packageName &&
            it.amountCents == match.amountCents &&
            it.kind == match.kind &&
            kotlin.math.abs(postTime - it.postTime) <= WINDOW_MS
    }

    /** Captures still young enough to have a twin arrive; everything else can be forgotten. */
    fun prune(recent: List<RecentCapture>, now: Long): List<RecentCapture> =
        recent.filter { now - it.postTime <= WINDOW_MS }

    /**
     * Stored one capture per line, fields tab-separated, because notes come from arbitrary
     * notification titles: tabs and newlines in them are flattened to spaces first.
     * Unreadable lines are dropped rather than failing the whole list.
     */
    fun encode(recent: List<RecentCapture>): String = recent.joinToString("\n") {
        listOf(
            it.packageName, it.amountCents, it.kind.name, it.postTime, it.transactionId,
            it.namesMerchant, it.note.replace(Regex("[\t\n\r]"), " "),
        ).joinToString("\t")
    }

    fun decode(stored: String?): List<RecentCapture> = stored.orEmpty().lines().mapNotNull { line ->
        val f = line.split('\t', limit = 7)
        if (f.size < 7) return@mapNotNull null
        RecentCapture(
            packageName = f[0],
            amountCents = f[1].toLongOrNull() ?: return@mapNotNull null,
            kind = runCatching { Kind.valueOf(f[2]) }.getOrNull() ?: return@mapNotNull null,
            postTime = f[3].toLongOrNull() ?: return@mapNotNull null,
            transactionId = f[4].toLongOrNull() ?: return@mapNotNull null,
            namesMerchant = f[5].toBooleanStrictOrNull() ?: return@mapNotNull null,
            note = f[6],
        )
    }
}
