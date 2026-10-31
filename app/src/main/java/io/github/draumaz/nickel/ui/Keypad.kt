package io.github.draumaz.nickel.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val rows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf(DOUBLE_ZERO, '0', '<'),
)

/** The key that types two zeros at once, for whole amounts. */
const val DOUBLE_ZERO = 'Z'

/** A cash-register number pad. `<` is backspace; long-pressing it clears. */
@Composable
fun Keypad(
    onKey: (Char) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isBackspace = key == '<'
                    Key(
                        onClick = { onKey(key) },
                        onLongClick = if (isBackspace) onClear else null,
                        container = if (isBackspace) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if (isBackspace) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        description = when (key) {
                            '<' -> "Backspace"
                            DOUBLE_ZERO -> "Double zero"
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
 * Material 3 Expressive button shape morph.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Key(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    container: Color,
    contentColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
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
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .background(container)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = contentColor),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) { content() }
    }
}
