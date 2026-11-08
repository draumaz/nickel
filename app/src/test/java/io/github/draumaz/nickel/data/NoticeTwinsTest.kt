package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeTwinsTest {

    private val cashAppPurchase = RecentCapture(
        packageName = "com.squareup.cash",
        amountCents = 883,
        kind = Kind.EXPENSE,
        postTime = 1_000_000L,
        transactionId = 42L,
        note = "Cash App",
        namesMerchant = false,
    )

    private val walletReceipt = NoticeMatch(
        amountCents = 883,
        kind = Kind.EXPENSE,
        source = "Google Wallet",
        note = "SAFEWAY #1551",
        namesMerchant = true,
    )

    @Test
    fun walletAlertForTheSameTapIsATwin() {
        val twin = NoticeTwins.findTwin(
            listOf(cashAppPurchase), "com.google.android.apps.walletnfcrel", walletReceipt, 1_030_000L,
        )
        assertEquals(42L, twin!!.transactionId)
    }

    @Test
    fun twinsMatchWhicheverAppArrivesFirst() {
        val walletFirst = cashAppPurchase.copy(
            packageName = "com.google.android.apps.walletnfcrel", note = "SAFEWAY #1551", namesMerchant = true,
        )
        val cashApp = walletReceipt.copy(source = "Cash App", note = "Cash App", namesMerchant = false)
        // Arrives a little *earlier* by post time than the stored one: still the same tap.
        assertNotNull(NoticeTwins.findTwin(listOf(walletFirst), "com.squareup.cash", cashApp, 990_000L))
    }

    @Test
    fun sameAppTwiceIsTwoRealPurchases() {
        val secondCoffee = walletReceipt.copy(source = "Cash App", namesMerchant = false)
        assertNull(NoticeTwins.findTwin(listOf(cashAppPurchase), "com.squareup.cash", secondCoffee, 1_060_000L))
    }

    @Test
    fun differentAmountOrDirectionIsNotATwin() {
        val wallet = "com.google.android.apps.walletnfcrel"
        assertNull(NoticeTwins.findTwin(listOf(cashAppPurchase), wallet, walletReceipt.copy(amountCents = 884), 1_000_000L))
        assertNull(NoticeTwins.findTwin(listOf(cashAppPurchase), wallet, walletReceipt.copy(kind = Kind.INCOME), 1_000_000L))
    }

    @Test
    fun matchingChargeOutsideTheWindowCountsOnItsOwn() {
        val later = 1_000_000L + NoticeTwins.WINDOW_MS + 1
        assertNull(NoticeTwins.findTwin(listOf(cashAppPurchase), "com.google.android.apps.walletnfcrel", walletReceipt, later))
        assertTrue(NoticeTwins.prune(listOf(cashAppPurchase), later).isEmpty())
    }

    @Test
    fun storageRoundTripsAndSurvivesAwkwardNotesAndJunk() {
        val awkward = cashAppPurchase.copy(note = "Tabs\there\nand lines", namesMerchant = true)
        val decoded = NoticeTwins.decode(NoticeTwins.encode(listOf(cashAppPurchase, awkward)) + "\ngarbage\tline")
        assertEquals(listOf(cashAppPurchase, awkward.copy(note = "Tabs here and lines")), decoded)
        assertTrue(NoticeTwins.decode(null).isEmpty())
    }

    @Test
    fun walletReceiptsNameTheMerchant() {
        val match = NoticeParser.parse(
            "com.google.android.apps.walletnfcrel", "Google Wallet", "SAFEWAY #1551", "$8.83 with Cash Card",
            channel = "tapandpay.transactions.low",
        )
        assertTrue(match!!.namesMerchant)
        val cashApp = NoticeParser.parse("com.squareup.cash", "Cash App", "Cash App", "You spent $8.83 at Safeway")
        assertEquals(false, cashApp!!.namesMerchant)
    }
}
