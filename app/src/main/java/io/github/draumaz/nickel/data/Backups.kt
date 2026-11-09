package io.github.draumaz.nickel.data

import java.time.LocalDate

/**
 * Naming and retention for the automatic CSV backups. One file per day, named by ISO date
 * so names sort oldest-first, and a later backup on the same day replaces that day's file
 * rather than piling up. Kept free of Android so the pruning rule is unit-testable.
 */
object BackupFiles {
    /** How many daily files survive pruning: about a week at the 24-hour interval. */
    const val KEEP = 7

    private const val PREFIX = "nickel-backup-"
    private const val SUFFIX = ".csv"
    private val pattern = Regex("""^nickel-backup-\d{4}-\d{2}-\d{2}\.csv$""")

    /** The choices offered in settings; 0 means automatic backups are off. */
    val INTERVALS_HOURS = listOf(0, 24, 48, 72)

    fun nameFor(date: LocalDate): String = "$PREFIX$date$SUFFIX"

    /**
     * Only files that look exactly like ours are ever considered, so a folder the user shares
     * with other things (or a renamed copy they want to keep) is never touched.
     */
    fun isBackup(name: String): Boolean = pattern.matches(name)

    /** The backup names to delete so only the newest [keep] remain. */
    fun toPrune(names: Collection<String>, keep: Int = KEEP): List<String> =
        names.filter(::isBackup).sortedDescending().drop(keep)
}
