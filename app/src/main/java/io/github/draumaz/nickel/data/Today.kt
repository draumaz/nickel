package io.github.draumaz.nickel.data

import java.time.LocalDate
import java.time.ZoneId

/**
 * The transactions that happened on [date] in [zone], in the order given (the ledger keeps
 * newest first). Day boundaries are local midnight, so a purchase at 23:59 stays yesterday's
 * even after travelling to an earlier time zone and back.
 */
fun List<Transaction>.on(date: LocalDate, zone: ZoneId): List<Transaction> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return filter { it.timestamp in start until end }
}
