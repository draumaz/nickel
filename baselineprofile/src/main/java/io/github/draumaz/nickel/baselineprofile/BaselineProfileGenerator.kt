package io.github.draumaz.nickel.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks Nickel's everyday paths so their code ships precompiled: cold start, logging a few
 * entries, scrolling and filtering the list, and the trip to Settings and back.
 *
 * Generate with `./gradlew :app:generateBaselineProfile` while the phone is unlocked. It runs
 * against the `.profiling` install, never the real release app or its ledger.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitFor("Nickel")

        // A fresh install has an empty ledger: log some entries so the list has rows to draw.
        if (device.findObject(By.text("Your ledger is empty")) != null) {
            listOf("1250", "899", "4200", "315", "2075", "640").forEach { device.addExpense(it) }
        }

        val list = device.findObject(By.scrollable(true))
        if (list != null) {
            list.setGestureMargin(device.displayWidth / 5)
            repeat(2) {
                list.fling(Direction.DOWN)
                device.waitForIdle()
                list.fling(Direction.UP)
                device.waitForIdle()
            }
        }

        listOf("Expenses", "Income", "All").forEach {
            device.findObject(By.text(it))?.click()
            device.waitForIdle()
        }

        device.findObject(By.desc("Settings"))?.click()
        device.waitFor("Make Nickel yours")
        device.pressBack()
        device.waitFor("Nickel")
    }

    private fun UiDevice.addExpense(digits: String) {
        findObject(By.desc("Add transaction"))?.click() ?: return
        waitFor("Add expense")
        digits.forEach { findObject(By.desc(it.toString()))?.click() }
        findObject(By.text("Add expense"))?.click()
        wait(Until.gone(By.text("Add expense")), TIMEOUT)
        waitForIdle()
    }

    private fun UiDevice.waitFor(text: String) {
        wait(Until.hasObject(By.text(text)), TIMEOUT)
    }

    private companion object {
        const val PACKAGE = "xyz.draumaz.nickel.profiling"
        const val TIMEOUT = 5_000L
    }
}
