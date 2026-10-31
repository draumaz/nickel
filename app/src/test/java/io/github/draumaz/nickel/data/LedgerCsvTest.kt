package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class LedgerCsvTest {

    private val dime = listOf(
        "Date,Note,Amount,Category,Type",
        "2026-10-31 22:03:00 +0000,Corner store,1.99,Bad Food,Expense",
        "2026-10-31 01:52:00 +0000,₿1234,4.40,Crypto,Expense",
        "2026-10-29 17:23:00 +0000,Paycheck,1500.00,Paycheck,Income",
        "2026-10-02 00:41:00 +0000,\"2x Protein bars, chocolate\",4.98,Groceries,Expense",
        "2026-10-01 18:00:00 +0000,\"She said \"\"hi\"\"\",1234.50,Healthcare,Expense",
        "not a date,oops,1.00,Groceries,Expense",
    ).joinToString("\r\n", postfix = "\r\n")

    @Test
    fun importsDimeRows() {
        val plan = LedgerCsv.plan(dime, emptyList(), emptyList())
        assertEquals(5, plan.transactions.size)
        assertEquals(1, plan.unreadable)

        val corner = plan.transactions.first { it.note == "Corner store" }
        assertEquals(199L, corner.amountCents)
        assertEquals(Kind.EXPENSE, corner.kind)
        assertEquals(Instant.parse("2026-10-31T22:03:00Z").toEpochMilli(), corner.timestamp)

        val pay = plan.transactions.first { it.note == "Paycheck" }
        assertEquals(Kind.INCOME, pay.kind)
        assertEquals("PAYCHECK", pay.category.id) // matched to the built-in

        assertEquals("GROCERIES", plan.transactions.first { it.note.startsWith("2x") }.category.id)
        assertEquals("2x Protein bars, chocolate", plan.transactions.first { it.note.startsWith("2x") }.note)
        assertEquals("She said \"hi\"", plan.transactions.first { it.amountCents == 123450L }.note)
        assertEquals("₿1234", plan.transactions.first { it.amountCents == 440L }.note)

        val newLabels = plan.newCategories.map { it.label }.toSet()
        assertEquals(setOf("Bad Food", "Crypto", "Healthcare"), newLabels)
        assertEquals("bitcoin", plan.newCategories.first { it.label == "Crypto" }.iconKey)
        assertEquals("fastfood", plan.newCategories.first { it.label == "Bad Food" }.iconKey)
        assertEquals("health", plan.newCategories.first { it.label == "Healthcare" }.iconKey)
    }

    @Test
    fun skipsDuplicatesOnReimport() {
        val first = LedgerCsv.plan(dime, emptyList(), emptyList())
        val again = LedgerCsv.plan(dime, first.transactions, first.newCategories)
        assertEquals(0, again.transactions.size)
        assertEquals(5, again.duplicates)
        assertTrue(again.newCategories.isEmpty())
    }

    @Test
    fun exportRoundTrips() {
        val first = LedgerCsv.plan(dime, emptyList(), emptyList())
        val exported = LedgerCsv.export(first.transactions)
        val back = LedgerCsv.plan(exported, emptyList(), emptyList())
        assertEquals(
            first.transactions.map { listOf(it.timestamp, it.amountCents, it.kind, it.note, it.category.label) }.toSet(),
            back.transactions.map { listOf(it.timestamp, it.amountCents, it.kind, it.note, it.category.label) }.toSet(),
        )
    }

    @Test
    fun signedAmountsWithoutTypeColumn() {
        val plan = LedgerCsv.plan("date,description,amount\n2026-01-02,Coffee,-4.50\n2026-01-03,Refund,\"1,200.00\"\n", emptyList(), emptyList())
        assertEquals(Kind.EXPENSE, plan.transactions.first { it.note == "Coffee" }.kind)
        assertEquals(450L, plan.transactions.first { it.note == "Coffee" }.amountCents)
        assertEquals(Kind.INCOME, plan.transactions.first { it.note == "Refund" }.kind)
        assertEquals(120000L, plan.transactions.first { it.note == "Refund" }.amountCents)
    }

    /** Sanity check against a real export when one is present locally (never committed). */
    @Test
    fun realExportIfPresent() {
        val file = File("../export.csv")
        if (!file.exists()) return
        val text = file.readText()
        val plan = LedgerCsv.plan(text, emptyList(), emptyList())
        val rows = Csv.parse(text).size - 1
        println("real export: rows=$rows imported=${plan.transactions.size} dup=${plan.duplicates} bad=${plan.unreadable}")
        println("new categories: " + plan.newCategories.joinToString { "${it.label}(${it.kind}, ${it.iconKey})" })
        val net = plan.transactions.sumOf { it.signedCents }
        println("net cents: $net")
        assertEquals(rows, plan.transactions.size + plan.duplicates + plan.unreadable)
        assertEquals(0, plan.unreadable)
    }
}
