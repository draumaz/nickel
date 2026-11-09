package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupFilesTest {

    @Test
    fun namesSortByDate() {
        assertEquals("nickel-backup-2026-11-08.csv", BackupFiles.nameFor(LocalDate.of(2026, 11, 8)))
        assertTrue(BackupFiles.nameFor(LocalDate.of(2026, 1, 9)) < BackupFiles.nameFor(LocalDate.of(2026, 1, 10)))
    }

    @Test
    fun onlyExactNamesCount() {
        assertTrue(BackupFiles.isBackup("nickel-backup-2026-11-08.csv"))
        assertFalse(BackupFiles.isBackup("nickel-backup-2026-11-08 (1).csv"))
        assertFalse(BackupFiles.isBackup("nickel-2026-11-08.csv"))
        assertFalse(BackupFiles.isBackup("budget.csv"))
    }

    @Test
    fun prunesOldestBeyondKeep() {
        val days = (1..10).map { BackupFiles.nameFor(LocalDate.of(2026, 11, it)) }
        val others = listOf("budget.csv", "nickel-backup-old.csv")
        val pruned = BackupFiles.toPrune((days + others).shuffled(), keep = 7)
        assertEquals(days.take(3), pruned.sorted())
    }

    @Test
    fun nothingToPruneUnderKeep() {
        assertEquals(emptyList<String>(), BackupFiles.toPrune(listOf(BackupFiles.nameFor(LocalDate.now()))))
    }
}
