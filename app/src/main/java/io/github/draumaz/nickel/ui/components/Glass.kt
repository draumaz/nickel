package io.github.draumaz.nickel.ui.components

import android.graphics.RecordingCanvas
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Whether this device has QPR2's backdrop render effects; older 17 builds get plain tonal fills. */
val GlassSupported: Boolean = Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2

/**
 * Frosted glass: fills [shape] with [tint] and blurs whatever was already drawn behind it by
 * [blur], through QPR2's `RenderNode.setBackdropRenderEffect`.
 *
 * The tint stays a Material colour role at partial alpha rather than a neutral white wash,
 * so a glass surface still reads as the container it replaces (the scallop is still
 * primaryContainer, the sheet is still surfaceContainerLow) and keeps the same on-colour
 * contrast. Glass is a treatment of a tonal surface, not a new surface.
 *
 * The content is recorded into a platform [RenderNode] of its own because Compose's
 * graphicsLayer has no backdrop slot: its renderEffect only filters the layer's own pixels.
 * The node is drawn back into Compose's recording canvas, so it composites in place and
 * moves with any graphicsLayer above it. Without hardware acceleration (or before QPR2)
 * this falls back to [fallback], an opaque-enough tint that text behind can't muddle it.
 *
 * [blur] is read at draw time, so animating it (a scrim that frosts as a sheet rises)
 * re-records only this node, never recomposes.
 */
fun Modifier.glass(
    shape: Shape,
    tint: Color,
    fallback: Color = tint,
    blur: () -> Dp = DefaultBlur,
): Modifier = this then GlassElement(shape, tint, fallback, blur)

/** Shared, so the element compares equal across recompositions and doesn't redraw for nothing. */
private val DefaultBlur: () -> Dp = { GlassDefaults.Blur }

object GlassDefaults {
    /** Strong enough that list text behind turns to colour, not readable ghosts. */
    val Blur = 24.dp

    /**
     * The lighter glass for floating controls (the add button, header icon buttons): mostly
     * blur, with just enough primaryContainer to outline the shape over rows of the same grey.
     */
    const val ControlTintAlpha = 0.3f
}

/**
 * A header icon button in the add button's glass, so every floating control reads as one
 * family. Taps give the light ContextClick tick every header button shares. It squares up a
 * little while pressed, as Expressive's icon-button shapes do; the glass carries that shape,
 * since the button's own container is left clear for it. Before QPR2 the glass falls back to
 * the filled-tonal container the button used to have.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    // A percentage, not dp: IconButton enforces the 48dp touch target, so the glass can end up
    // larger than [size], and a dp radius of size / 2 then draws a squircle instead of a circle.
    val cornerPercent by animateFloatAsState(
        if (pressed) 30f else 50f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "glassButtonCorner",
    )
    // CornerSize(Float) is pixels; only the Int overload is a percentage.
    val shape = RoundedCornerShape(percent = cornerPercent.roundToInt())
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        shape = shape,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        interactionSource = interactionSource,
        modifier = modifier
            .size(size)
            .glass(
                shape = shape,
                tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = GlassDefaults.ControlTintAlpha),
                fallback = MaterialTheme.colorScheme.secondaryContainer,
            ),
        content = content,
    )
}

private data class GlassElement(
    val shape: Shape,
    val tint: Color,
    val fallback: Color,
    val blur: () -> Dp,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(shape, tint, fallback, blur)
    override fun update(node: GlassNode) {
        node.shape = shape
        node.tint = tint
        node.fallback = fallback
        node.blur = blur
        node.invalidateDraw()
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "glass"
        properties["shape"] = shape
        properties["tint"] = tint
    }
}

private class GlassNode(
    var shape: Shape,
    var tint: Color,
    var fallback: Color,
    var blur: () -> Dp,
) : Modifier.Node(), DrawModifierNode {
    private var renderNode: RenderNode? = null
    private var lastBlurPx = -1f
    /** The last outline built, with what it was built for. */
    private var cachedOutline: Pair<Triple<Size, LayoutDirection, Shape>, Outline>? = null

    override fun onDetach() {
        renderNode?.discardDisplayList()
        renderNode = null
        lastBlurPx = -1f
        cachedOutline = null
    }

    override fun ContentDrawScope.draw() {
        val key = Triple(size, layoutDirection, shape)
        val shapeOutline = cachedOutline?.takeIf { it.first == key }?.second
            ?: shape.createOutline(size, layoutDirection, this).also { cachedOutline = key to it }

        val target = drawContext.canvas.nativeCanvas
        if (!GlassSupported || target !is RecordingCanvas || size.isEmpty()) {
            drawOutline(shapeOutline, fallback)
            drawContent()
            return
        }

        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        val rn = renderNode ?: RenderNode("glass").also { renderNode = it }
        rn.setPosition(0, 0, w, h)
        applyOutline(rn, shapeOutline)

        // A zero radius would still pay for the backdrop pass; drop the effect instead.
        val blurPx = blur().toPx().coerceAtLeast(0f)
        if (blurPx != lastBlurPx) {
            rn.setBackdropRenderEffect(
                if (blurPx < 0.5f) null
                else RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
            )
            lastBlurPx = blurPx
        }

        val content = this
        val recording = rn.beginRecording(w, h)
        try {
            draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(recording), size) {
                drawOutline(shapeOutline, tint)
                content.drawContent()
            }
        } finally {
            rn.endRecording()
        }
        target.drawRenderNode(rn)
    }

    /**
     * Clips the node, and with it the backdrop pass, to the shape: otherwise a scalloped or
     * rounded surface would frost a rectangle of the list around itself.
     */
    private fun applyOutline(rn: RenderNode, outline: Outline) {
        val platform = android.graphics.Outline()
        when (outline) {
            is Outline.Rectangle -> platform.setRect(
                outline.rect.left.roundToInt(), outline.rect.top.roundToInt(),
                outline.rect.right.roundToInt(), outline.rect.bottom.roundToInt(),
            )
            is Outline.Rounded -> {
                val r = outline.roundRect
                if (r.topLeftCornerRadius == r.topRightCornerRadius &&
                    r.topLeftCornerRadius == r.bottomLeftCornerRadius &&
                    r.topLeftCornerRadius == r.bottomRightCornerRadius
                ) {
                    platform.setRoundRect(
                        r.left.roundToInt(), r.top.roundToInt(),
                        r.right.roundToInt(), r.bottom.roundToInt(),
                        r.topLeftCornerRadius.x,
                    )
                } else {
                    platform.setPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(r) }.asAndroidPath())
                }
            }
            is Outline.Generic -> platform.setPath(outline.path.asAndroidPath())
        }
        rn.setOutline(platform)
        rn.clipToOutline = true
    }
}

