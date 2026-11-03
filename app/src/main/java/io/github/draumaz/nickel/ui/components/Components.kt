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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

/** A container/on-container pair for one hue, in dark and light variants. */
private class HuePair(
    val darkContainer: Long, val onDarkContainer: Long,
    val lightContainer: Long, val onLightContainer: Long,
)

/**
 * Twelve hues around the wheel. Both themes use bright pastel containers with dark icons
 * (tone 80 in dark, 90 in light) so every badge pops against the surface.
 */
private val CategoryHues = listOf(
    HuePair(0xFFFFB4AB, 0xFF690005, 0xFFFFDAD6, 0xFF410002), // 0 red
    HuePair(0xFFFFB77C, 0xFF4A2800, 0xFFFFDCC2, 0xFF2E1500), // 1 orange
    HuePair(0xFFF0C048, 0xFF3F2E00, 0xFFFFDF9E, 0xFF261A00), // 2 amber
    HuePair(0xFFAED460, 0xFF1F3700, 0xFFC9F07A, 0xFF121F00), // 3 lime
    HuePair(0xFF7FDA96, 0xFF003918, 0xFF9AF6B0, 0xFF00210B), // 4 green
    HuePair(0xFF82D5CB, 0xFF003733, 0xFF9EF2E7, 0xFF00201D), // 5 teal
    HuePair(0xFF5FD4FC, 0xFF003545, 0xFFB8EAFF, 0xFF001F28), // 6 cyan
    HuePair(0xFFA9C7FF, 0xFF003063, 0xFFD6E3FF, 0xFF001B3E), // 7 blue
    HuePair(0xFFC3C0FF, 0xFF241F70, 0xFFE2DFFF, 0xFF120A55), // 8 indigo
    HuePair(0xFFDFB7FF, 0xFF4A1772, 0xFFF2DAFF, 0xFF2C0050), // 9 purple
    HuePair(0xFFFFAAEE, 0xFF5B0F55, 0xFFFFD7F5, 0xFF380035), // 10 magenta
    HuePair(0xFFFFB1C8, 0xFF650033, 0xFFFFD9E2, 0xFF3E001D), // 11 pink
)

/** Hand-picked hues for built-ins so neighbours in each list stay distinct. */
private val BuiltInHues = mapOf(
    "PAYCHECK" to 4, "SALARY" to 5, "FREELANCE" to 2, "INTEREST" to 3,
    "GIFT" to 11, "REFUND" to 6, "OTHER_INCOME" to 7,
    "GROCERIES" to 4, "DINING" to 1, "RENT" to 8, "UTILITIES" to 2, "TRANSPORT" to 7,
    "MEDICAL" to 0, "SHOPPING" to 10, "ENTERTAINMENT" to 9, "SUBSCRIPTIONS" to 11,
    "TRAVEL" to 6, "EDUCATION" to 8, "PETS" to 1, "NICOTINE" to 0, "INVESTMENTS" to 5,
    "OTHER_EXPENSE" to 7,
)

private fun hueFor(category: Category): HuePair {
    val index = BuiltInHues[category.id] ?: Math.floorMod(category.id.hashCode(), CategoryHues.size)
    return CategoryHues[index]
}

@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** The category's own colorful container, nudged slightly toward the theme so it sits in with dynamic color. */
@Composable
fun categoryContainer(category: Category): Color {
    val hue = hueFor(category)
    val dark = isDarkScheme()
    val base = Color(if (dark) hue.darkContainer else hue.lightContainer)
    // Wash toward white for a soft, pale pastel; the light-theme tints are already near it.
    val pale = lerp(base, Color.White, if (dark) 0.45f else 0.3f)
    return lerp(pale, MaterialTheme.colorScheme.primary, 0.05f)
}

@Composable
fun onCategoryContainer(category: Category): Color {
    val hue = hueFor(category)
    return Color(if (isDarkScheme()) hue.onDarkContainer else hue.onLightContainer)
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
    containerColor: Color = categoryContainer(category),
    contentColor: Color = onCategoryContainer(category),
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

