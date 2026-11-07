package io.github.draumaz.nickel.ui

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
import android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
import android.os.VibrationEffect.Composition.PRIMITIVE_SPIN
import android.os.VibrationEffect.Composition.PRIMITIVE_THUD
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK

/**
 * Richer haptics than [androidx.compose.ui.hapticfeedback.HapticFeedback] offers, built from
 * vibration primitives where the hardware supports them (Pixels do).
 */
object Haptics {

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    /**
     * A saved entry landing back in its shape: a sharp click for the hit with a thud's low body
     * under it, which rolls on through [slamLanding]'s rebound.
     */
    fun slam(context: Context) {
        val vibrator = vibrator(context)?.takeIf { it.hasVibrator() } ?: return
        val effect = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            // Not every motor has THUD; without it a tick stands in for the rebound, landing
            // around the overshoot's peak.
            vibrator.areAllPrimitivesSupported(PRIMITIVE_CLICK, PRIMITIVE_THUD) ->
                VibrationEffect.startComposition()
                    .addPrimitive(PRIMITIVE_CLICK, 1f)
                    .addPrimitive(PRIMITIVE_THUD, 1f)
                    .compose()
            vibrator.areAllPrimitivesSupported(PRIMITIVE_CLICK, PRIMITIVE_TICK) ->
                VibrationEffect.startComposition()
                    .addPrimitive(PRIMITIVE_CLICK, 1f)
                    .addPrimitive(PRIMITIVE_TICK, 0.6f, 150)
                    .compose()
            else -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        play(vibrator, effect)
    }

    /** A little spin that lands on a click when opening; a soft fall and tick when closing. */
    fun fabToggle(context: Context, opening: Boolean) {
        val vibrator = vibrator(context)?.takeIf { it.hasVibrator() } ?: return
        val effect = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            vibrator.areAllPrimitivesSupported(PRIMITIVE_SPIN, PRIMITIVE_CLICK, PRIMITIVE_QUICK_FALL, PRIMITIVE_TICK)
        ) {
            if (opening) {
                VibrationEffect.startComposition()
                    .addPrimitive(PRIMITIVE_SPIN, 0.45f)
                    .addPrimitive(PRIMITIVE_CLICK, 1f, 20)
                    .compose()
            } else {
                VibrationEffect.startComposition()
                    .addPrimitive(PRIMITIVE_QUICK_FALL, 0.6f)
                    .addPrimitive(PRIMITIVE_TICK, 0.9f, 10)
                    .compose()
            }
        } else {
            VibrationEffect.createPredefined(
                if (opening) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
            )
        }
        play(vibrator, effect)
    }

    private fun play(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Touch usage, so the system's touch-feedback setting is respected.
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect)
        }
    }
}
