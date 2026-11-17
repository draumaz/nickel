package io.github.draumaz.nickel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeAppsTest {

    @Test
    fun recognizesBanksAndPaymentApps() {
        listOf(
            "com.chase.sig.android" to "Chase",
            "com.venmo" to "Venmo",
            "com.zellepay.zelle" to "Zelle",
            "com.squareup.cash" to "Cash App",
            "com.google.android.apps.walletnfcrel" to "Google Wallet",
            "com.samsung.android.spay" to "Samsung Wallet",
            "com.paypal.android.p2pmobile" to "PayPal",
            "com.discoverfinancial.mobile" to "Discover",
            "com.infonow.bofa" to "Bank of America",
            "ru.sberbankmobile" to "СберБанк",
        ).forEach { (pkg, label) -> assertTrue("$pkg should look financial", NoticeApps.looksFinancial(pkg, label)) }
    }

    @Test
    fun includesTextMessageApps() {
        assertTrue(NoticeApps.looksFinancial("com.google.android.apps.messaging", "Messages"))
    }

    @Test
    fun leavesOrdinaryAppsOut() {
        listOf(
            "com.android.vending" to "Play Store",
            "com.spotify.music" to "Spotify",
            "com.android.settings" to "Display & Settings",
            "com.instagram.android" to "Instagram",
            "com.amazon.mShop.android.shopping" to "Amazon Shopping",
        ).forEach { (pkg, label) -> assertFalse("$pkg shouldn't look financial", NoticeApps.looksFinancial(pkg, label)) }
    }

    @Test
    fun brandsMatchWholeWordsOnly() {
        // "chase" inside "purchased", "citi" inside "cities".
        assertFalse(NoticeApps.looksFinancial("com.example.purchased", "Cities Skylines"))
    }

    @Test
    fun worksWithoutALabel() {
        assertTrue(NoticeApps.looksFinancial("com.example.mybank", null))
    }

    @Test
    fun firstListKeepsEverythingForExistingUsers() {
        val installed = mapOf("com.venmo" to "Venmo", "com.spotify.music" to "Spotify")
        assertEquals(installed.keys, NoticeApps.initialSelection(installed, wasListeningToAll = true))
        assertEquals(setOf("com.venmo"), NoticeApps.initialSelection(installed, wasListeningToAll = false))
    }
}
