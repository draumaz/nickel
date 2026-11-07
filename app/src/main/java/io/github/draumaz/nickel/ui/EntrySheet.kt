package io.github.draumaz.nickel.ui

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import io.github.draumaz.nickel.ui.components.localizedDateFormat
import io.github.draumaz.nickel.ui.components.displayLabel
import io.github.draumaz.nickel.R
import androidx.compose.ui.res.stringResource
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.PoppingText
import io.github.draumaz.nickel.ui.components.onCategoryContainer
import io.github.draumaz.nickel.ui.components.rememberTimeFormatter
import io.github.draumaz.nickel.ui.components.categoryContainer
import io.github.draumaz.nickel.ui.theme.NickelTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.positionInParent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toPath
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.rectangle
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.asAndroidPath
import androidx.graphics.shapes.toPath as toAndroidPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Where a spinning shape the sheet grows out of sits and how it looks right now: the add
 * button's scallop, or a ledger row's category badge. Its owner writes the geometry; the sheet
 * flips [covered] while it is drawing the shape itself, and the owner hides its own copy for
 * that time so there's only ever one of them on screen.
 */
@Stable
class ScallopOrigin {
    /** The shape's centre, in screen coordinates. */
    var centerOnScreen = Offset.Unspecified
    var diameter = 0f
    /** The shape's outline, normalized to a unit square like [MaterialShapes]' own. */
    var polygon: RoundedPolygon = MaterialShapes.Cookie12Sided
    var color = Color.Unspecified
    var icon: ImageVector = Icons.Rounded.Add
    var iconColor = Color.Unspecified
    /** The icon's size as a fraction of [diameter]. */
    var iconScale = 0.44f
    /** How far the icon twists, in degrees, as it fades out of the opening shape. */
    var iconTwist = 90f
    /** The shape's live turn in degrees; read per frame so it keeps spinning as it grows. */
    var rotation: () -> Float = { 0f }
    var covered by mutableStateOf(false)
    /**
     * Whether this is a category badge, which flies onto the matching chip in the sheet's
     * category carousel rather than dissolving into the sheet (see [CategoryLanding]).
     */
    var fliesToCategory = false
    /**
     * Set just before [covered] drops when the sheet came back with a saved entry rather than
     * being dismissed: the owner lands its shape with [slamLanding] instead of a soft settle, and
     * clears this once it has.
     */
    var slam = false
}

/**
 * The selected chip in the entry sheet's category carousel, as a landing spot. A sheet opened
 * from a ledger row's badge flies that badge across onto this chip while the sheet grows, and
 * back off it when the sheet closes, so the category reads as one object that moved rather
 * than one that vanished and another that appeared. The chip reports where it is and how it
 * looks, and hides its own copy while the flying one is out.
 */
@Stable
class CategoryLanding {
    /**
     * The category whose chip the badge lands on: the one it came from, so backing out after
     * picking another category flies the row's own badge home, until a save points it at the
     * category being saved. Null means the selected one.
     */
    var target by mutableStateOf<Category?>(null)
    var coords: LayoutCoordinates? = null
    var category by mutableStateOf<Category?>(null)
    var color = Color.Unspecified
    var iconColor = Color.Unspecified
    var scale: () -> Float = { 1f }
    var rotation: () -> Float = { 0f }
    /** True while the badge is in the air; read at draw time. */
    var inFlight: () -> Boolean = { false }
}

internal val LocalCategoryLanding = staticCompositionLocalOf<CategoryLanding?> { null }

/** The polygon a unit-square [MaterialShapes]-style shape becomes when placed in frame units. */
private fun RoundedPolygon.placed(cx: Float, cy: Float, diameter: Float, degrees: Float): RoundedPolygon {
    val turn = Math.toRadians(degrees.toDouble())
    val cos = cos(turn).toFloat()
    val sin = sin(turn).toFloat()
    return transformed { x, y ->
        val dx = (x - 0.5f) * diameter
        val dy = (y - 0.5f) * diameter
        TransformResult(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos)
    }
}

/**
 * The squash and rebound of a shape that has just been slammed back into place, as a scale
 * factor animated from 1. It starts moving *inward*, carrying on the speed the shrinking sheet
 * arrived with, so the dip reads as the impact rather than as a jump; a loose spring then lets
 * it bounce past full size and settle.
 */
suspend fun Animatable<Float, AnimationVector1D>.slamLanding() {
    snapTo(1f)
    animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 700f), initialVelocity = -7f)
}

/** The extra turn a slammed shape whips through: quicker and stiffer than the soft kick. */
val SlamSpin = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    initialKind: Kind,
    editing: Transaction?,
    customCategories: List<Category>,
    onCreateCategory: (Category) -> Unit,
    onDismiss: () -> Unit,
    /** Prefills a *new* entry (amount, kind, category, note) from it, timestamped now. */
    template: Transaction? = null,
    /** The shape this sheet was opened from, which it morphs out of and back into. */
    origin: ScallopOrigin? = null,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val motion = remember { ScallopMotion(morphing = origin != null) }
    val landing = remember {
        if (origin?.fliesToCategory == true) {
            CategoryLanding().apply { inFlight = { motion.grow.value < 1f } }
        } else null
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { motion.confirm(it) },
    )
    val shrinkSpec = remember { spring<Float>(dampingRatio = 1f, stiffness = 650f, visibilityThreshold = 0.001f) }

    // Shrinks back into the button, hands over to it and only then lets the sheet go, so the
    // scrim and sheet vanish with nothing left to show.
    // A save slams home instead: it speeds up all the way in, so it still has its speed when
    // it hits and the shape it lands in can take the impact (see [slamLanding]).
    val slamSpec = remember { tween<Float>(260, easing = CubicBezierEasing(0.45f, 0f, 0.9f, 0.45f)) }

    fun shrink(slam: Boolean = false, then: () -> Unit = {}) {
        if (origin == null || motion.shrinking) return
        motion.shrinking = true
        scope.launch {
            // A settling spring spends its last stretch creeping through sub-pixel changes;
            // waiting that out reads as a pause before the button wakes, so hand over as soon
            // as the scallop has visibly arrived.
            launch { motion.grow.animateTo(0f, if (slam) slamSpec else shrinkSpec) }
            snapshotFlow { motion.grow.value }.first { it <= HandoverAt }
            motion.grow.snapTo(0f)
            motion.handedOver = true
            origin.slam = slam
            origin.covered = false
            if (slam) Haptics.slam(context) else Haptics.fabToggle(context, opening = false)
            // Saving recomposes the list and closing tears down the sheet's window, which can
            // hold the main thread for several frames: right at the hit, that swallows the
            // squash whole. Let the squash play first; the sheet is already invisible and the
            // list update lands just after, as the shape bounces back.
            if (slam) delay(SlamSettleMillis)
            then()
            currentOnDismiss()
        }
    }

    if (origin != null) {
        motion.onShrinkRequest = { shrink() }
        val growSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        LaunchedEffect(Unit) {
            // The sheet snaps into place invisibly (see SnapInMotion) while the scallop starts
            // growing. Growth starts right away rather than once the sheet reports it has
            // settled, and with a push rather than from rest: a spring from rest spends its
            // first frames barely moving, which reads as lag after the tap.
            launch { motion.grow.animateTo(1f, growSpec, initialVelocity = GrowKick) }
            // The button only steps aside once the sheet is really on screen, so it can't
            // blink out a frame early.
            snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Expanded }
            origin.covered = true
        }
        DisposableEffect(Unit) { onDispose { origin.covered = false } }
    }

    fun close(slam: Boolean = false, then: () -> Unit) {
        if (origin != null) return shrink(slam, then)
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    val content: @Composable ColumnScope.() -> Unit = {
        CompositionLocalProvider(LocalCategoryLanding provides landing) {
        EntryContent(
            initialKind = initialKind,
            editing = editing,
            template = template,
            customCategories = customCategories,
            onCreateCategory = onCreateCategory,
            onSave = { t ->
                landing?.target = t.category
                close(slam = true) { onSave(t) }
            },
            onDelete = { close { onDelete(it) } },
        )
        }
    }

    if (origin == null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            content = content,
        )
        return
    }

    // A bottom sheet slides up from the edge, and nothing can be drawn over its scrim from
    // here, so the scallop has to live inside the sheet: it lands instantly and transparent,
    // and the scallop grows from its origin to fill it. The sheet spans the whole window
    // (see ScallopFrame) so an origin high up the screen isn't clipped away.
    val theme = MaterialTheme.motionScheme
    val haptics = LocalHapticFeedback.current
    val settleSpec = theme.defaultSpatialSpec<Float>()
    val flingVelocity = with(LocalDensity.current) { 600.dp.toPx() }
    val settled = sheetState.currentValue == SheetValue.Expanded
    val sheetMotion = remember(theme) { SnapInMotion(theme) }
    val scrim = BottomSheetDefaults.ScrimColor
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = if (settled) theme else sheetMotion,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = scrim.copy(alpha = scrim.alpha * motion.grow.value.coerceIn(0f, 1f)),
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0) },
            // ScallopFrame takes the drag instead, so pulling down un-grows the scallop.
            sheetGesturesEnabled = false,
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                motionScheme = theme,
                shapes = MaterialTheme.shapes,
                typography = MaterialTheme.typography,
            ) {
                ScallopFrame(
                    origin = origin,
                    grow = motion.grow::value,
                    handedOver = { motion.handedOver },
                    landing = landing,
                    onPull = { progress ->
                        if (!motion.shrinking) {
                            if (motion.grow.value >= PullDismissAt && progress < PullDismissAt) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            }
                            scope.launch { motion.grow.snapTo(progress) }
                        }
                    },
                    onRelease = { velocity ->
                        if (motion.grow.value < PullDismissAt || velocity > flingVelocity) shrink()
                        else if (!motion.shrinking) scope.launch { motion.grow.animateTo(1f, settleSpec) }
                    },
                    onDismissAction = { shrink() },
                ) {
                    // The frame spans the whole window, so only the bottom (nav bar, keyboard)
                    // insets belong to the sheet itself.
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
                        Box(Modifier.align(Alignment.CenterHorizontally)) { BottomSheetDefaults.DragHandle() }
                        content()
                    }
                }
            }
        }
    }
}

/**
 * The sheet's own motion, minus its slide-up: [MaterialTheme.motionScheme]'s default spatial
 * spec is what a bottom sheet shows itself with, and snapping it makes the sheet appear in
 * place for the scallop to fill. Only in force until the sheet has settled, so dragging it
 * afterwards moves as usual.
 */
private class SnapInMotion(base: MotionScheme) : MotionScheme by base {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
}

/**
 * Morph progress, and the veto that routes back presses and scrim taps into shrinking the
 * sheet into its origin rather than letting the sheet hide itself. A plain sheet (opened
 * without an origin) has nothing to shrink into, so it must hide as usual: vetoing there leaves
 * back, swipe-down and the scrim all dead.
 */
private class ScallopMotion(private val morphing: Boolean) {
    val grow = Animatable(if (morphing) 0f else 1f)
    var shrinking = false
    /**
     * Set the moment the shape is handed back to its origin. The sheet's window lingers a few
     * frames before it goes, and drawing its last frame of the shape over the origin's own copy
     * would hide the origin's landing (most visibly a slam's squash) until it went.
     */
    var handedOver by mutableStateOf(false)
    var onShrinkRequest: () -> Unit = {}

    fun confirm(value: SheetValue): Boolean {
        if (!morphing || value != SheetValue.Hidden) return true
        onShrinkRequest()
        return false
    }
}

/** Below this much growth a released pull carries on into the button instead of springing back. */
private const val PullDismissAt = 0.6f

/** How long a slam's squash gets the main thread to itself before the save is committed. */
private const val SlamSettleMillis = 140L

/** Growth at which a shrinking scallop is close enough to the button to hand over to it. */
private const val HandoverAt = 0.02f

/** Initial growth velocity, in progress per second, so the scallop moves on the first frame. */
private const val GrowKick = 4f

/**
 * Draws the sheet's background as a shape growing out of [origin] into the sheet's rounded-top
 * outline, clipping the contents to it. The contents fade in over the last stretch so the
 * keypad doesn't flash through a half-formed blob. Once fully grown it's a plain rounded rect,
 * so a settled sheet costs nothing extra per frame.
 *
 * The frame fills the whole (transparent) sheet window with the visible sheet at its bottom:
 * the sheet clips to its own bounds, and an origin above a shorter sheet's top edge, like a
 * row near the top of the list, would otherwise start out clipped away. That also covers
 * the sheet's own scrim, so taps above the visible sheet dismiss it here instead.
 */
@Composable
private fun ScallopFrame(
    origin: ScallopOrigin,
    grow: () -> Float,
    /** Whether the shape has gone back to [origin]; draws nothing from then on. */
    handedOver: () -> Boolean,
    /** Where a category badge origin lands in the carousel, if it flies there. */
    landing: CategoryLanding?,
    /** Called with the growth a downward pull has wound the shape back to. */
    onPull: (Float) -> Unit,
    /** Called with the downward fling velocity, in px/s, when the pull ends. */
    onRelease: (Float) -> Unit,
    onDismissAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val icon = rememberVectorPainter(origin.icon)
    val flightIcon = rememberVectorPainter(landing?.category?.icon ?: origin.icon)
    val flightPath = remember { Path() }
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    /** The visible sheet's top edge within the frame; NaN until first placed. */
    var sheetTop by remember { mutableFloatStateOf(Float.NaN) }
    val lift = with(LocalDensity.current) { 24.dp.toPx() }
    val currentOnPull by rememberUpdatedState(onPull)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnDismiss by rememberUpdatedState(onDismissAction)
    Box(
        Modifier
            .fillMaxSize()
            .onPlaced { placed[0] = it }
            // Taps on the scrim above the sheet dismiss it. Not detectTapGestures: that consumes
            // every down, including ones on the sheet, and the keypad reads a consumed touch as
            // a swipe and takes the keystroke back. This only ever touches scrim taps.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (sheetTop.isNaN() || down.position.y >= sheetTop) return@awaitEachGesture
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    up.consume()
                    currentOnDismiss()
                }
            }
            .pointerInput(Unit) {
                // The pull is mapped so the sheet's top edge roughly tracks the finger:
                // shrinking brings that edge down to the origin's top, so that's a full
                // pull's travel. An origin up near the sheet's top (or above it) would make
                // that tiny, so fall back to most of the sheet's height.
                var startGrow = 1f
                var pulled = 0f
                var travel = 1f
                val velocity = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        startGrow = grow()
                        pulled = 0f
                        velocity.resetTracking()
                        val top = if (sheetTop.isNaN()) 0f else sheetTop
                        val sheetHeight = size.height - top
                        val coords = placed[0]
                        val originTop = if (coords != null && coords.isAttached && origin.centerOnScreen.isSpecified) {
                            coords.screenToLocal(origin.centerOnScreen).y - origin.diameter / 2
                        } else size.height.toFloat()
                        val reach = originTop - top
                        travel = if (reach > sheetHeight * 0.3f) reach else sheetHeight * 0.8f
                    },
                    onDragEnd = { currentOnRelease(velocity.calculateVelocity().y) },
                    onDragCancel = { currentOnRelease(0f) },
                ) { change, dragAmount ->
                    change.consume()
                    velocity.addPointerInputChange(change)
                    pulled += dragAmount
                    currentOnPull((startGrow - pulled / travel).coerceIn(0f, 1f))
                }
            }
            .semantics { dismiss { currentOnDismiss(); true } }
            .drawWithCache {
                val top = sheetTop
                val corner = SheetCorner.toPx()
                val sheetHeight = (size.height - top).coerceAtLeast(2 * corner)
                val settled = Path().apply {
                    if (!top.isNaN()) addRoundRect(
                        RoundRect(
                            Rect(Offset(0f, top), Size(size.width, sheetHeight)),
                            topLeft = CornerRadius(corner),
                            topRight = CornerRadius(corner),
                        )
                    )
                }
                // The morph is built in frame-width units and scaled up afterwards: in raw
                // pixels, float error between neighbouring curve anchors outgrows the shapes
                // library's contiguity tolerance and it throws.
                val unit = size.width
                val toPixels = Matrix().apply { scale(unit, unit) }
                val sheet = if (top.isNaN()) null else RoundedPolygon.rectangle(
                    width = 1f,
                    height = sheetHeight / unit,
                    // Vertices run bottom-right, bottom-left, top-left, top-right.
                    perVertexRounding = listOf(
                        CornerRounding.Unrounded,
                        CornerRounding.Unrounded,
                        CornerRounding(corner / unit),
                        CornerRounding(corner / unit),
                    ),
                    centerX = 0.5f,
                    centerY = (top + sheetHeight / 2) / unit,
                )
                val morphPath = Path()
                onDrawWithContent {
                    if (sheet == null || handedOver()) return@onDrawWithContent
                    val p = grow().coerceIn(0f, 1f)
                    val coords = placed[0]
                    if (p >= 1f || coords == null || !coords.isAttached || !origin.centerOnScreen.isSpecified) {
                        drawPath(settled, surface)
                        drawContent()
                        return@onDrawWithContent
                    }
                    val from = coords.screenToLocal(origin.centerOnScreen)
                    // A badge flying to the carousel needs its chip on screen to aim at; one
                    // scrolled out of the carousel falls back to dissolving into the sheet.
                    val chip = landing?.coords?.takeIf { origin.fliesToCategory && it.isAttached }
                    val flying = landing?.category?.takeIf { chip != null }
                    // Rebuilt per frame so the shape keeps its live spin while it grows;
                    // matching a small polygon to a rectangle is cheap.
                    val start = origin.polygon.placed(from.x / unit, from.y / unit, origin.diameter / unit, origin.rotation())
                    Morph(start, sheet).toPath(p, morphPath)
                    morphPath.transform(toPixels)
                    // Under a flying badge the sheet comes out from behind it in its own colour,
                    // fading in as it clears the badge so it never shows as a dark rim around
                    // it; otherwise the origin's colour turns into the sheet's as it opens.
                    drawPath(
                        morphPath,
                        if (flying != null) surface.copy(alpha = surface.alpha * (p / 0.3f).coerceAtMost(1f))
                        else lerp(origin.color, surface, (p / 0.7f).coerceAtMost(1f)),
                    )
                    clipPath(morphPath) { this@onDrawWithContent.drawContent() }

                    if (chip != null && flying != null) {
                        val to = coords.localPositionOf(chip, Offset(chip.size.width / 2f, chip.size.height / 2f))
                        val at = androidx.compose.ui.geometry.lerp(from, to, p)
                        val diameter = androidx.compose.ui.util.lerp(origin.diameter, chip.size.width * landing.scale(), p)
                        // The shorter way round, so a badge never whirls a full turn to line up.
                        val fromTurn = origin.rotation()
                        val delta = ((landing.rotation() - fromTurn) % 360f + 540f) % 360f - 180f
                        flying.shape.placed(at.x / unit, at.y / unit, diameter / unit, fromTurn + delta * p)
                            .toAndroidPath(flightPath.asAndroidPath().apply { rewind() })
                        flightPath.transform(toPixels)
                        drawPath(flightPath, lerp(origin.color, landing.color, p))
                        val side = diameter * origin.iconScale
                        translate(at.x - side / 2, at.y - side / 2) {
                            with(flightIcon) {
                                draw(Size(side, side), colorFilter = ColorFilter.tint(lerp(origin.iconColor, landing.iconColor, p)))
                            }
                        }
                        return@onDrawWithContent
                    }

                    // The origin's icon (the button's plus, a badge's category), fading out
                    // as the shape opens up.
                    val iconAlpha = 1f - p / 0.25f
                    if (iconAlpha > 0f) {
                        val side = origin.diameter * origin.iconScale
                        rotate(origin.iconTwist * p / 0.25f, from) {
                            translate(from.x - side / 2, from.y - side / 2) {
                                with(icon) {
                                    draw(Size(side, side), alpha = iconAlpha, colorFilter = ColorFilter.tint(origin.iconColor))
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                // Ahead of the layer below, so the fade-in lift doesn't move the edge.
                .onPlaced { sheetTop = it.positionInParent().y }
                .graphicsLayer {
                    val shown = ((grow() - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    alpha = shown
                    translationY = (1f - shown) * lift
                }
        ) { content() }
    }
}

/** Matches [BottomSheetDefaults.ExpandedShape]'s top corners. */
private val SheetCorner = 28.dp

/** Up to 999,999,999.99, the same cap as before. */
private const val MAX_DIGITS = 11

// Digits fill in from the cents up, so there's no decimal point to type.
private fun applyKey(input: String, key: Char): String {
    if (key == '<') return input.dropLast(1)
    val digits = if (key == DOUBLE_ZERO) "00" else key.toString()
    return when {
        input.isEmpty() && digits.all { it == '0' } -> input // no leading zeros
        input.length + digits.length > MAX_DIGITS -> input
        else -> input + digits
    }
}

private val SwipeThreshold = 48.dp
private val SwipeReach = 96.dp

/** Where the keypad has been dragged to, and whether letting go now would switch kinds. */
private class KindSwipeState {
    val offset = Animatable(0f)
    var armed by mutableStateOf(false)
}

private fun Kind.other() = if (this == Kind.EXPENSE) Kind.INCOME else Kind.EXPENSE

/**
 * Swiping across the keypad flips between expense and income. It wraps around like an
 * endless carousel: a swipe either way switches to the other kind. The keypad follows the
 * finger with some resistance and springs back on release.
 */
@Composable
private fun Modifier.kindSwipe(state: KindSwipeState, kind: Kind, onSelect: (Kind) -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val springBack = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val currentKind by rememberUpdatedState(kind)
    val currentOnSelect by rememberUpdatedState(onSelect)

    return pointerInput(Unit) {
        val threshold = SwipeThreshold.toPx()
        val reach = SwipeReach.toPx()
        var total = 0f

        fun release() {
            if (state.armed) currentOnSelect(currentKind.other())
            state.armed = false
            scope.launch { state.offset.animateTo(0f, springBack) }
        }

        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = ::release,
            onDragCancel = ::release,
        ) { change, dragAmount ->
            change.consume()
            total += dragAmount
            // Ease out toward a limit.
            val eased = sign(total) * reach * (1 - exp(-abs(total) / reach))
            scope.launch { state.offset.snapTo(eased) }

            val nowArmed = abs(total) >= threshold
            if (nowArmed && !state.armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            state.armed = nowArmed
        }
    }.graphicsLayer { translationX = state.offset.value }
}

/**
 * The badge of the kind a swipe would switch to, growing into whichever gap the keypad
 * leaves as it's dragged aside and popping once letting go would switch. Sits behind
 * the keypad, filling its bounds; [gutter] is the padding beside the keypad, which the
 * gap includes.
 */
@Composable
private fun BoxScope.KindSwipeHints(state: KindSwipeState, kind: Kind, gutter: Dp) {
    val target = kind.other()
    val popped by animateFloatAsState(
        if (state.armed) 1.15f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "hintPop",
    )
    listOf(true, false).forEach { onLeft ->
        Box(
            Modifier
                .align(if (onLeft) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                .size(40.dp)
                .graphicsLayer {
                    // Dragging right uncovers the left edge, and vice versa.
                    val uncovered = (if (onLeft) state.offset.value else -state.offset.value).coerceAtLeast(0f)
                    val armedAt = SwipeReach.toPx() * (1 - exp(-SwipeThreshold.toPx() / SwipeReach.toPx()))
                    val progress = (uncovered / armedAt).coerceIn(0f, 1f)
                    // Centre the badge in the gap between the sheet edge and the keypad.
                    val centre = (uncovered - gutter.toPx()) / 2
                    translationX = (centre - size.width / 2) * if (onLeft) 1 else -1
                    alpha = progress
                    val scale = (0.5f + 0.5f * progress) * popped
                    scaleX = scale
                    scaleY = scale
                }
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (target == Kind.INCOME) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EntryContent(
    initialKind: Kind,
    editing: Transaction?,
    template: Transaction?,
    customCategories: List<Category>,
    onCreateCategory: (Category) -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    // A clone copies what was spent on what, but not when or which entry it was: it saves
    // under a fresh id and starts at the current time, like any new entry.
    val source = editing ?: template
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    var kind by rememberSaveable { mutableStateOf(source?.kind ?: initialKind) }
    var input by rememberSaveable { mutableStateOf(source?.let { Money.toInput(it.amountCents) } ?: "") }
    var category by rememberSaveable { mutableStateOf(source?.category ?: Categories.default(kind)) }
    var note by rememberSaveable { mutableStateOf(source?.note ?: "") }
    var timestamp by rememberSaveable { mutableLongStateOf(editing?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }

    val cents = Money.parseInput(input)
    val money = NickelTheme.money
    val amountColor by animateColorAsState(
        when {
            cents == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
            kind == Kind.INCOME -> money.income
            else -> MaterialTheme.colorScheme.onSurface
        },
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "amountColor",
    )

    fun selectKind(k: Kind) {
        if (kind == k) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        kind = k
        if (category.kind != k) category = Categories.default(k)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        // Expense / income switch, as a connected button group.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            listOf(Kind.EXPENSE, Kind.INCOME).forEachIndexed { index, k ->
                val checked = kind == k
                ToggleButton(
                    checked = checked,
                    onCheckedChange = { selectKind(k) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                    else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Icon(
                        if (k == Kind.EXPENSE) Icons.AutoMirrored.Rounded.TrendingDown
                        else Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(stringResource(if (k == Kind.EXPENSE) R.string.kind_expense else R.string.kind_income))
                }
            }
        }

        // Amount display.
        Box(
            Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.animateContentSize(
                MaterialTheme.motionScheme.fastSpatialSpec()
            )) {
                Text(
                    (if (kind == Kind.INCOME) "+" else "−") + Money.symbol,
                    style = MaterialTheme.typography.headlineLarge,
                    color = amountColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 4.dp),
                )
                PoppingText(
                    Money.displayInput(input),
                    style = MaterialTheme.typography.displayLargeEmphasized,
                    color = amountColor,
                )
            }
        }

        // Categories.
        val categories = Categories.forKind(kind, customCategories)
        // Opens already scrolled to the category, rather than gliding there, so a badge
        // flying in from the ledger has a still target to land on.
        val rowState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, categories.indexOf(category) - 1))
        LaunchedEffect(kind) { rowState.animateScrollToItem(maxOf(0, categories.indexOf(category) - 1)) }
        val landing = LocalCategoryLanding.current
        if (landing != null && landing.target == null) SideEffect { landing.target = category }
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(categories, key = { it.id }) { c ->
                CategoryChip(
                    category = c,
                    selected = c == category,
                    landing = landing.takeIf { c == (landing?.target ?: category) },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        category = c
                        focus.clearFocus()
                    },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "new") {
                NewCategoryChip(
                    onClick = { showNewCategory = true },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Note, date and time.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = note,
                onValueChange = { note = it.take(60) },
                placeholder = { Text(stringResource(R.string.add_note)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.weight(1f),
            )
            WhenPill(
                timestamp = timestamp,
                // Touching the pill reads as being done with the keyboard: dropping the
                // note's focus first keeps the picker dialog from handing focus back to
                // the field on close, which would pop the keyboard up again.
                onPickDate = {
                    focus.clearFocus()
                    showDatePicker = true
                },
                onPickTime = {
                    focus.clearFocus()
                    showTimePicker = true
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        val swipe = remember { KindSwipeState() }
        // The amount before the latest keystroke, so a key press that turns out to be the
        // start of a swipe can be taken back. Keys type on touch-down; see [Keypad].
        var beforeLastKey by remember { mutableStateOf<String?>(null) }
        Box(Modifier.padding(horizontal = 20.dp)) {
            KindSwipeHints(swipe, kind, gutter = 20.dp)
            Keypad(
                onKey = { key ->
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    val next = applyKey(input, key)
                    if (next == input && key != '<') {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    beforeLastKey = input
                    input = next
                },
                onClear = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    beforeLastKey = null
                    input = ""
                },
                onUndo = {
                    beforeLastKey?.let { input = it }
                    beforeLastKey = null
                },
                modifier = Modifier.kindSwipe(swipe, kind, ::selectKind),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editing != null) {
                FilledTonalIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        onDelete(editing)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.delete))
                }
            }
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave(
                        Transaction(
                            id = editing?.id ?: System.nanoTime(),
                            amountCents = cents,
                            kind = kind,
                            category = category,
                            note = note.trim(),
                            timestamp = timestamp,
                        )
                    )
                },
                enabled = cents > 0,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.contentPaddingFor(64.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(64.dp)))
                Text(
                    when {
                        editing != null -> stringResource(R.string.save_changes)
                        kind == Kind.INCOME -> stringResource(R.string.add_income)
                        else -> stringResource(R.string.add_expense)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (showNewCategory) {
        NewCategoryDialog(
            kind = kind,
            existing = Categories.forKind(kind, customCategories),
            onDismiss = { showNewCategory = false },
            onCreate = { created, isNew ->
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                if (isNew) onCreateCategory(created)
                category = created
                showNewCategory = false
            },
        )
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val localDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utc ->
                        val picked = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        val time = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
                        timestamp = picked.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
        val pickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.time)) },
            confirmButton = {
                TextButton(onClick = {
                    timestamp = dateTime
                        .with(LocalTime.of(pickerState.hour, pickerState.minute))
                        .toInstant()
                        .toEpochMilli()
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            TimePicker(state = pickerState)
        }
    }
}

/** Date and time in one pill, split down the middle: each half opens its own picker. */
@Composable
private fun WhenPill(timestamp: Long, onPickDate: () -> Unit, onPickTime: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .clickable(onClickLabel = stringResource(R.string.change_date), onClick = onPickDate)
                    .padding(start = 16.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(dayLabel(timestamp), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            VerticalDivider(
                Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.25f),
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .clickable(onClickLabel = stringResource(R.string.change_time), onClick = onPickTime)
                    .padding(start = 10.dp, end = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(timeLabel(timestamp, rememberTimeFormatter()), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

private fun timeLabel(timestamp: Long, formatter: DateTimeFormatter): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)

@Composable
private fun dayLabel(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(localizedDateFormat(if (date.year == today.year) "MMMd" else "yMMMd"))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 56.dp,
    /** Set on the selected chip of a sheet that a ledger badge flies into. */
    landing: CategoryLanding? = null,
) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val progress by animateFloatAsState(if (selected) 1f else 0f, spatial, label = "morph")
    val rotation by animateFloatAsState(if (selected) 0f else -60f, spatial, label = "spin")
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spatial, label = "scale")
    // The same slow turn as the ledger badges and add button, phase-shifted per category
    // so neighbouring chips don't spin in lockstep.
    val spin = rememberInfiniteTransition(label = "chipSpin")
    val turn by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "chipTurn",
    )
    val phase = remember(category.id) { abs(category.id.hashCode() % 360).toFloat() }
    val container by animateColorAsState(
        if (selected) categoryContainer(category) else MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "container",
    )
    val content by animateColorAsState(
        if (selected) onCategoryContainer(category) else MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "content",
    )
    val interaction = remember { MutableInteractionSource() }
    // The category's own colours rather than the chip's current ones: a dismissal after
    // picking another category lands on an unselected (grey) chip, and the badge should
    // still look like the row's.
    val badgeColor = categoryContainer(category)
    val badgeIconColor = onCategoryContainer(category)
    if (landing != null) SideEffect {
        landing.category = category
        landing.color = badgeColor
        landing.iconColor = badgeIconColor
        landing.scale = { scale }
        landing.rotation = { rotation + phase + turn }
    }

    Column(
        modifier
            .width(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryBadge(
            category = category,
            size = badgeSize,
            shapeProgress = progress,
            containerColor = container,
            contentColor = content,
            shapeRotation = { rotation + phase + turn },
            modifier = Modifier
                // Ahead of the scale, so the landing sees the unscaled size it multiplies up.
                .then(if (landing != null) Modifier.onGloballyPositioned { landing.coords = it } else Modifier)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (landing?.inFlight?.invoke() == true) 0f else 1f
                },
        )
        Spacer(Modifier.height(6.dp))
        // Long names ("Subscriptions", "Развлечения") shrink to fit the chip's fixed width
        // instead of truncating; the ellipsis only kicks in below a still-legible floor.
        val labelStyle = MaterialTheme.typography.labelMedium
        Text(
            category.displayLabel(),
            style = labelStyle,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 9.sp,
                maxFontSize = labelStyle.fontSize,
                stepSize = 0.25.sp,
            ),
        )
    }
}
