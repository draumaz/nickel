package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun digitsFillFromTheCentsUp() {
        assertEquals(449L, Money.parseInput("449"))
        assertEquals(18945528L, Money.parseInput("18945528"))
        assertEquals(0L, Money.parseInput(""))
    }

    @Test
    fun displaysTwoDecimalsWithGrouping() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("0.00", Money.displayInput(""))
            assertEquals("0.05", Money.displayInput("5"))
            assertEquals("4.49", Money.displayInput("449"))
            assertEquals("189,455.28", Money.displayInput("18945528"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun roundTripsThroughInput() {
        assertEquals("", Money.toInput(0))
        assertEquals(1250L, Money.parseInput(Money.toInput(1250)))
    }
}
