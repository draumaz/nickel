package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TodayTest {

    private val zone = ZoneId.of("America/Los_Angeles")
    private val groceries = Categories.builtIns.first { it.id == "GROCERIES" }

    private fun at(id: Long, time: String) = Transaction(
        id, 100, Kind.EXPENSE, groceries, "",
        LocalDateTime.parse(time).atZone(zone).toInstant().toEpochMilli(),
    )

    @Test
    fun keepsOnlyThatLocalDayInOrder() {
        val ledger = listOf(
            at(1, "2026-11-11T00:00"),
            at(2, "2026-11-10T23:59:59"),
            at(3, "2026-11-10T09:30"),
            at(4, "2026-11-10T00:00"),
            at(5, "2026-11-09T23:59:59"),
        )
        assertEquals(listOf(2L, 3L, 4L), ledger.on(LocalDate.of(2026, 11, 10), zone).map { it.id })
    }

    @Test
    fun handlesDaylightSavingDays() {
        // 2026-11-01 is 25 hours long in Los Angeles.
        val ledger = listOf(at(1, "2026-11-01T23:30"), at(2, "2026-11-01T00:30"))
        assertEquals(listOf(1L, 2L), ledger.on(LocalDate.of(2026, 11, 1), zone).map { it.id })
    }
}
