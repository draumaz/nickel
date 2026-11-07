package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteMemoryTest {

    private fun cat(id: String) = Categories.builtIns.first { it.id == id }

    private fun t(id: Long, note: String, category: String, timestamp: Long) =
        cat(category).let { Transaction(id, 100, it.kind, it, note, timestamp) }

    @Test
    fun matchesIgnoringCaseAndSpacing() {
        val memory = NoteMemory.of(listOf(t(1, "Lime", "TRANSPORT", 10)))
        assertEquals(cat("TRANSPORT"), memory.lookup("  lime ", Kind.EXPENSE)?.category)
        assertEquals(cat("TRANSPORT"), memory.lookup("LIME", Kind.EXPENSE)?.category)
        assertNull(memory.lookup("Limes", Kind.EXPENSE))
        assertNull(memory.lookup("   ", Kind.EXPENSE))
    }

    @Test
    fun latestEntryWins() {
        val memory = NoteMemory.of(
            listOf(
                t(1, "Co-op", "GROCERIES", 10),
                t(2, "Co-op", "TRANSPORT", 30),
                t(3, "Co-op", "GROCERIES", 20),
            )
        )
        assertEquals(cat("TRANSPORT"), memory.lookup("co-op", Kind.EXPENSE)?.category)
    }

    @Test
    fun theEntryBeingEditedIsNotItsOwnPrecedent() {
        val memory = NoteMemory.of(listOf(t(1, "Lime", "GROCERIES", 10), t(2, "Lime", "TRANSPORT", 20)))
        assertEquals(cat("GROCERIES"), memory.lookup("Lime", Kind.EXPENSE, excluding = 2)?.category)
        val alone = NoteMemory.of(listOf(t(2, "Lime", "TRANSPORT", 20)))
        assertNull(alone.lookup("Lime", Kind.EXPENSE, excluding = 2))
    }

    @Test
    fun prefersTheCurrentKindAndOnlyCrossesWhenAllowed() {
        val memory = NoteMemory.of(listOf(t(1, "Refund", "PAYCHECK", 30), t(2, "Refund", "GROCERIES", 10)))
        // Same kind wins even though the other kind's entry is newer.
        assertEquals(cat("GROCERIES"), memory.lookup("Refund", Kind.EXPENSE)?.category)

        val incomeOnly = NoteMemory.of(listOf(t(1, "Paycheck", "PAYCHECK", 30)))
        assertEquals(
            NoteMemory.Match(Kind.INCOME, cat("PAYCHECK")),
            incomeOnly.lookup("paycheck", Kind.EXPENSE),
        )
        assertNull(incomeOnly.lookup("paycheck", Kind.EXPENSE, anyKind = false))
    }
}
