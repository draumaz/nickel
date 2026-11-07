package io.github.draumaz.nickel.data

import java.util.Locale

/**
 * What the ledger remembers about notes: for each note it has seen, the category it was
 * last filed under. Typing a note that matches an earlier one picks that entry's category,
 * so "Lime" lands in Transport without a trip round the category dial.
 *
 * Built once per ledger change (off the main thread; the ledger can be long after an import)
 * rather than searched per keystroke. Pure, so it's tested on the JVM like [LedgerCsv].
 */
class NoteMemory private constructor(
    /** Normalized note → kind → the latest two entries with that note, newest first. */
    private val byNote: Map<String, Map<Kind, List<Transaction>>>,
) {
    /**
     * The category to file [note] under, judging by earlier entries with the same note, or
     * null if there are none. Prefers the latest entry of [kind]; failing that, and only if
     * [anyKind], the latest of the other kind (an income note typed on an expense is still
     * a strong hint the entry is income). [excluding] is the entry being edited, which
     * mustn't count as its own precedent.
     */
    fun lookup(note: String, kind: Kind, anyKind: Boolean = true, excluding: Long? = null): Match? {
        val kinds = byNote[normalize(note) ?: return null] ?: return null
        fun latest(k: Kind) = kinds[k]?.firstOrNull { it.id != excluding }
        val hit = latest(kind) ?: (if (anyKind) latest(kind.other()) else null) ?: return null
        return Match(hit.kind, hit.category)
    }

    data class Match(val kind: Kind, val category: Category)

    companion object {
        val Empty = NoteMemory(emptyMap())

        fun of(transactions: List<Transaction>): NoteMemory {
            val byNote = HashMap<String, HashMap<Kind, MutableList<Transaction>>>()
            for (t in transactions) {
                val key = normalize(t.note) ?: continue
                val latest = byNote.getOrPut(key) { HashMap() }.getOrPut(t.kind) { ArrayList(2) }
                // Two per kind is enough to still have an answer once the edited entry is
                // set aside.
                latest.add(t)
                latest.sortByDescending { it.timestamp }
                if (latest.size > 2) latest.removeAt(latest.lastIndex)
            }
            return NoteMemory(byNote)
        }

        /**
         * Notes match ignoring case and spacing, so "lime " and "Lime" are the same; blank
         * notes never match anything.
         */
        internal fun normalize(note: String): String? =
            note.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT).ifEmpty { null }

        private fun Kind.other() = if (this == Kind.EXPENSE) Kind.INCOME else Kind.EXPENSE
    }
}
