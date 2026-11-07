package io.github.draumaz.nickel.ui

import io.github.draumaz.nickel.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val rows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf(DOUBLE_ZERO, '0', '<'),
)

private val KeyHeight = 58.dp
private val KeyGap = 8.dp

/** The key that types two zeros at once, for whole amounts. */
const val DOUBLE_ZERO = 'Z'

/** One finger down on the pad: which key it hit, and its press for the ripple. */
private class Press(val key: Char, val press: PressInteraction.Press, val longPress: Job?)

/**
 * A cash-register number pad. `<` is backspace; long-pressing it clears.
 *
 * Keys commit the instant a finger lands, not on release, and one handler watches every
 * finger on the whole pad rather than each key running its own click detector. A
 * per-key click only counts if the finger lifts inside that key without having moved
 * far, so fast, overlapping, sloppy taps used to silently drop digits: a skid off the
 * key's edge (made smaller by the press squish), a tap in the gap between keys, or a
 * few pixels of travel that the sheet pull or the kind swipe claimed. Now every touch
 * types something. The one tradeoff is that a press that turns out to be the start of
 * a swipe or sheet pull has already typed, so it's taken back with [onUndo].
 */
@Composable
fun Keypad(
    onKey: (Char) -> Unit,
    onClear: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { rows.flatten().associateWith { MutableInteractionSource() } }
    val currentOnKey by rememberUpdatedState(onKey)
    val currentOnClear by rememberUpdatedState(onClear)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(rtl) {
                val gap = KeyGap.toPx()
                val keyHeight = KeyHeight.toPx()

                /**
                 * The key under [position], splitting each gap down the middle so there's
                 * no dead space, plus where the touch sits within that key.
                 */
                fun hit(position: Offset): Pair<Char, Offset> {
                    val keyWidth = (size.width - 2 * gap) / 3
                    val visualCol = ((position.x + gap / 2) / (keyWidth + gap)).toInt().coerceIn(0, 2)
                    val row = ((position.y + gap / 2) / (keyHeight + gap)).toInt().coerceIn(0, rows.lastIndex)
                    val col = if (rtl) 2 - visualCol else visualCol
                    val local = Offset(
                        position.x - visualCol * (keyWidth + gap),
                        position.y - row * (keyHeight + gap),
                    )
                    return rows[row][col] to local
                }

                coroutineScope {
                    val presses = mutableMapOf<PointerId, Press>()
                    // The finger that typed most recently; only its keystroke can be undone,
                    // since anything typed after it would be lost by rolling back.
                    var lastTyped: PointerId? = null

                    fun release(id: PointerId, cancelled: Boolean) {
                        val p = presses.remove(id) ?: return
                        p.longPress?.cancel()
                        val source = interactions.getValue(p.key)
                        source.tryEmit(
                            if (cancelled) PressInteraction.Cancel(p.press) else PressInteraction.Release(p.press),
                        )
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                when {
                                    change.changedToDownIgnoreConsumed() -> {
                                        val (key, local) = hit(change.position)
                                        val press = PressInteraction.Press(local)
                                        interactions.getValue(key).tryEmit(press)
                                        val longPress = if (key == '<') launch {
                                            delay(viewConfiguration.longPressTimeoutMillis)
                                            currentOnClear()
                                        } else null
                                        presses[change.id] = Press(key, press, longPress)
                                        lastTyped = change.id
                                        currentOnKey(key)
                                    }
                                    !change.pressed -> release(change.id, cancelled = false)
                                }
                            }
                            // After everything above the pad has had its turn: a finger whose
                            // movement got claimed is a swipe or sheet pull, not a keystroke.
                            val final = awaitPointerEvent(PointerEventPass.Final)
                            final.changes.forEach { change ->
                                // Only movement counts: a tap detector further up may consume the down
                                // itself, which says nothing about a swipe.
                                if (change.id in presses && change.previousPressed && change.pressed && change.isConsumed) {
                                    release(change.id, cancelled = true)
                                    if (lastTyped == change.id) {
                                        lastTyped = null
                                        currentOnUndo()
                                    }
                                }
                            }
                        }
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { key ->
                    val isBackspace = key == '<'
                    Key(
                        interaction = interactions.getValue(key),
                        onClick = { currentOnKey(key) },
                        onLongClick = if (isBackspace) ({ currentOnClear() }) else null,
                        container = if (isBackspace) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (isBackspace) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        description = when (key) {
                            '<' -> stringResource(R.string.backspace)
                            DOUBLE_ZERO -> stringResource(R.string.double_zero)
                            else -> key.toString()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isBackspace) {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null)
                        } else {
                            Text(if (key == DOUBLE_ZERO) "00" else key.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A key that squishes from a full pill into a rounded square while pressed, mirroring the
 * Material 3 Expressive button shape morph. Touch is handled by the [Keypad] as a whole;
 * the key only shows presses fed into [interaction], and keeps click actions in its
 * semantics so accessibility services can still press it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Key(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    container: Color,
    contentColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 12.dp else 28.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "corner",
    )
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "scale",
    )
    Box(
        modifier
            .height(KeyHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .background(container)
            .indication(interaction, ripple(color = contentColor))
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
                if (onLongClick != null) onLongClick { onLongClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) { content() }
    }
}
