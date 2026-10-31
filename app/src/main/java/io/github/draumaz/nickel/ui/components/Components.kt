package io.github.draumaz.nickel.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.Tone
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Text whose characters roll vertically like an odometer when they change. Characters are
 * keyed by their distance from the end so place values (ones, tens, cents...) keep identity.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var previous by remember { mutableStateOf(text) }
    val rollUp = remember(text) { compareMagnitude(text, previous) >= 0 }
    previous = text

    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val dir = if (rollUp) 1 else -1
                        (slideInVertically(spatial) { dir * it } + fadeIn(effects))
                            .togetherWith(slideOutVertically(spatial) { -dir * it } + fadeOut(effects))
                            .using(SizeTransform(clip = false))
                    },
                    label = "digit",
                ) { c ->
                    Text(c.toString(), style = style, color = color, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

private fun compareMagnitude(a: String, b: String): Int {
    fun value(s: String): Double {
        val negative = s.contains('−') || s.contains('-')
        val digits = s.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        return if (negative) -digits else digits
    }
    return value(a).compareTo(value(b))
}

/** Draws a [Morph] between two normalized polygons as a [Shape]. */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

@Composable
fun toneContainer(tone: Tone): Color = when (tone) {
    Tone.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    Tone.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    Tone.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
fun onToneContainer(tone: Tone): Color = when (tone) {
    Tone.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    Tone.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    Tone.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
}

/**
 * A category icon sitting inside its signature Material shape. [shapeProgress] morphs from a
 * plain circle (0) to the category's shape (1). [shapeRotation] is read at draw time, so a
 * spinning shape doesn't recompose the badge every frame.
 */
@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shapeProgress: Float = 1f,
    shapeRotation: () -> Float = { 0f },
    containerColor: Color = toneContainer(category.tone),
    contentColor: Color = onToneContainer(category.tone),
) {
    val morph = remember(category) { Morph(MaterialShapes.Circle, category.shape) }
    val shape = remember(morph, shapeProgress) { MorphShape(morph, shapeProgress) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // Only the shape turns; the icon stays upright.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = shapeRotation() }
                .background(containerColor, shape)
        )
        Icon(
            category.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/**
 * Shape for an item in a segmented (grouped) list: generous outer corners on the ends of
 * the group, tight inner corners between neighbours.
 */
fun segmentShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 6.dp): RoundedCornerShape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Springy squish while pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Characters that pop in with a bouncy scale as they are typed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PoppingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            key(index, char) {
                val scale = remember { Animatable(0.3f) }
                LaunchedEffect(Unit) { scale.animateTo(1f, spatial) }
                Text(
                    char.toString(),
                    style = style,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        transformOrigin = TransformOrigin(0.5f, 0.8f)
                    },
                )
            }
        }
    }
}

/**
 * The large header shared by every screen, so titles and leading marks line up exactly
 * when moving between them.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

/**
 * Short time format that follows the device's 12/24-hour setting, which the locale's own
 * format ignores: `13:46` or `1:46 PM`.
 */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(is24Hour) {
        val locale = Locale.getDefault()
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    }
}

