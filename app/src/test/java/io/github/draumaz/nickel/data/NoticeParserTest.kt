package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoticeParserTest {

    @Test
    fun parsesZelleDeposit() {
        val match = NoticeParser.parse(
            "com.zellepay.zelle",
            "Zelle",
            "Payment received",
            "You received $25.00 from Alex via Zelle",
        )
        assertEquals(2500L, match!!.amountCents)
        assertEquals(Kind.INCOME, match.kind)
        assertEquals("Zelle", match.source)
        assertEquals("Payment received", match.note)
    }

    @Test
    fun parsesGroupedAmountFromSms() {
        val match = NoticeParser.parse(
            "com.google.android.apps.messaging",
            "Messages",
            null,
            "Chase: A deposit of $1,250.75 was credited to your account ending in 4321.",
        )
        assertEquals(125075L, match!!.amountCents)
        assertEquals(Kind.INCOME, match.kind)
        assertEquals("Text message", match.source)
        assertEquals("Text message", match.note)
    }

    @Test
    fun parsesExpenseFromPaymentApp() {
        val match = NoticeParser.parse(
            "com.venmo",
            "Venmo",
            "Payment sent",
            "You paid Joe $12.50 for dinner",
        )
        assertEquals(1250L, match!!.amountCents)
        assertEquals(Kind.EXPENSE, match.kind)
        assertEquals("Venmo", match.source)
    }

    @Test
    fun parsesSymbollessAmountWithCurrencyCode() {
        val match = NoticeParser.parse(
            "com.bank.app",
            "Bank",
            null,
            "A transfer of 500.00 USD was credited to your account",
        )
        assertEquals(50000L, match!!.amountCents)
        assertEquals(Kind.INCOME, match.kind)
    }

    @Test
    fun parsesAmountWithoutCents() {
        val match = NoticeParser.parse("com.squareup.cash", "Cash App", null, "You received $25 from Sam")
        assertEquals(2500L, match!!.amountCents)
    }

    @Test
    fun parsesSingleFractionDigitAsTenths() {
        val match = NoticeParser.parse("com.venmo", "Venmo", null, "You paid Joe $12.5")
        assertEquals(1250L, match!!.amountCents)
    }

    @Test
    fun skipsAmountsWithoutDirectionWord() {
        assertNull(NoticeParser.parse("com.weather", "Weather", null, "Your balance is $500"))
        assertNull(NoticeParser.parse("com.shop", "Shop", null, "Save $10 when you spend $50"))
    }

    @Test
    fun skipsTextsWithoutMoney() {
        assertNull(NoticeParser.parse("com.google.android.apps.messaging", "Messages", null, "Meet me at 3 pm"))
        assertNull(NoticeParser.parse("com.venmo", "Venmo", "Payment sent", ""))
    }

    @Test
    fun skipsZeroAmounts() {
        assertNull(NoticeParser.parse("com.venmo", "Venmo", null, "You received $0.00 from nobody"))
    }

    @Test
    fun fallsBackToPackageSlugWithoutAppName() {
        val match = NoticeParser.parse("com.unknown.bank", null, null, "Direct deposit of $900.00 posted")
        assertEquals(90000L, match!!.amountCents)
        assertEquals("bank", match.source)
    }
}
