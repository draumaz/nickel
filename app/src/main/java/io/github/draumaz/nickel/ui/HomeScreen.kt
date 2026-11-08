package io.github.draumaz.nickel.ui

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.abs
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.ui.platform.LocalResources
import io.github.draumaz.nickel.ui.components.localizedDateFormat
import io.github.draumaz.nickel.ui.components.displayLabel
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.BuildConfig
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.categoryContainer
import io.github.draumaz.nickel.ui.components.onCategoryContainer
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.draumaz.nickel.ui.components.RollingText
import io.github.draumaz.nickel.ui.components.ScreenHeader
import io.github.draumaz.nickel.ui.components.pressScale
import io.github.draumaz.nickel.ui.components.rememberTimeFormatter
import io.github.draumaz.nickel.ui.components.segmentShape
import io.github.draumaz.nickel.ui.theme.NickelTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    viewModel: NickelViewModel,
    onOpenSettings: () -> Unit,
    addEntryPending: Boolean = false,
    onAddEntryHandled: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    // One clock turns every row's badge, rather than an infinite animation per row.
    val badgeSpin = rememberInfiniteTransition(label = "badges")
    val badgeTurn = badgeSpin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "badgeTurn",
    )

    // Hide the add button while scrolling down through the list; bring it back on any
    // upward scroll or at the top.
    var fabShownByScroll by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val delta = when {
                    index > lastIndex -> Int.MAX_VALUE
                    index < lastIndex -> Int.MIN_VALUE
                    else -> offset - lastOffset
                }
                when {
                    index == 0 && offset == 0 -> fabShownByScroll = true
                    delta > 12 -> fabShownByScroll = false
                    delta < -12 -> fabShownByScroll = true
                }
                lastIndex = index
                lastOffset = offset
            }
    }
    // Hidden only while actually moving through the list: once the user stops (a fling
    // counts as moving until it settles) and lingers, the add button comes back.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress || fabShownByScroll }
            .collectLatest { busyOrShown ->
                if (!busyOrShown) {
                    delay(FAB_LINGER_MS)
                    fabShownByScroll = true
                }
            }
    }

    var sheetKind by remember { mutableStateOf<Kind?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    /** The transaction a swipe-right is cloning into a new entry, if any. */
    var cloning by remember { mutableStateOf<Transaction?>(null) }
    /** The add button or row badge the open sheet grows out of; null for a plain sheet. */
    var morphOrigin by remember { mutableStateOf<ScallopOrigin?>(null) }
    val fabOrigin = remember { ScallopOrigin() }
    /** Shared by all rows: the tapped one fills it in, and only it reports while open. */
    val rowOrigin = remember { ScallopOrigin() }

    // New entries start as expenses; the sheet's switch flips to income.
    fun openNewEntry() {
        Haptics.fabToggle(context, opening = true)
        editing = null
        cloning = null
        morphOrigin = fabOrigin
        sheetKind = Kind.EXPENSE
    }

    // A widget tap: the same as pressing the scallop, once there's a scallop to press. The
    // button is only measured a frame or two after loading finishes, and the sheet grows out
    // of wherever it was measured, so wait for that rather than morphing from nowhere. An
    // entry already open is left alone instead of being thrown away for a blank one.
    LaunchedEffect(addEntryPending, state.loading) {
        if (!addEntryPending || state.loading) return@LaunchedEffect
        fabShownByScroll = true
        if (sheetKind == null) {
            repeat(10) { if (!fabOrigin.centerOnScreen.isSpecified) withFrameNanos { } }
            openNewEntry()
            if (!fabOrigin.centerOnScreen.isSpecified) morphOrigin = null
        }
        onAddEntryHandled()
    }

    fun deleteWithUndo(t: Transaction) {
        viewModel.delete(t)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = resources.getString(R.string.deleted_item, t.note.ifBlank { t.category.displayLabel(resources) }),
                actionLabel = resources.getString(R.string.undo),
                withDismissAction = false,
                duration = SnackbarDuration.Long,
            )
            // Restore under a fresh id: reusing the old key would revive the row's
            // lingering composition, still in its swiped-away state.
            if (result == SnackbarResult.ActionPerformed) viewModel.save(t.copy(id = System.nanoTime()))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val fabVisible by remember { derivedStateOf { !state.loading && fabShownByScroll } }
            ScallopFab(
                origin = fabOrigin,
                enabled = fabVisible && sheetKind == null,
                onClick = ::openNewEntry,
                modifier = Modifier
                    .semantics { traversalIndex = -1f }
                    .animateFloatingActionButton(
                        visible = fabVisible,
                        alignment = Alignment.BottomCenter,
                    ),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator(Modifier.size(72.dp))
            }
            return@Scaffold
        }

        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                viewModel.refresh()
            },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 112.dp,
                ),
            ) {
                item(key = "header", contentType = "header") { Header(onOpenSettings) }
                item(key = "hero", contentType = "hero") {
                    HeroCard(state.totals, state.summary)
                }
                item(key = "filter", contentType = "filter") {
                    Column {
                        FilterRow(
                            selected = state.filter,
                            onSelect = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                viewModel.setFilter(it)
                            },
                        )
                        CategoryFilterCarousel(
                            categories = state.filterCategories,
                            selected = state.categoryFilter,
                            onSelect = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                viewModel.setCategoryFilter(it)
                            },
                        )
                    }
                }

                when {
                    state.isEmpty -> item(key = "empty", contentType = "empty") {
                        EmptyState(
                            title = stringResource(R.string.empty_ledger_title),
                            body = stringResource(R.string.empty_ledger_body),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    state.transactions.isEmpty() -> item(key = "none", contentType = "empty") {
                        EmptyState(
                            title = stringResource(if (state.filter == Filter.INCOME) R.string.no_income_yet else R.string.no_expenses_yet),
                            body = stringResource(R.string.empty_filter_body),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    else -> transactionList(
                        transactions = state.transactions,
                        badgeTurn = badgeTurn,
                        rowOrigin = rowOrigin,
                        originId = (editing ?: cloning)?.id.takeIf { morphOrigin === rowOrigin },
                        onClick = {
                            editing = it
                            cloning = null
                            morphOrigin = rowOrigin
                            sheetKind = it.kind
                        },
                        onDelete = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            deleteWithUndo(it)
                        },
                        onClone = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            editing = null
                            cloning = it
                            morphOrigin = rowOrigin
                            sheetKind = it.kind
                        },
                    )
                }
            }
        }
    }

    sheetKind?.let { kind ->
        val noteMemory by viewModel.noteMemory.collectAsStateWithLifecycle()
        EntrySheet(
            initialKind = kind,
            editing = editing,
            template = cloning,
            origin = morphOrigin,
            customCategories = state.customCategories,
            noteMemory = noteMemory,
            onCreateCategory = viewModel::addCategory,
            onDismiss = {
                sheetKind = null
                editing = null
                cloning = null
                morphOrigin = null
            },
            onSave = { t ->
                val isNew = editing == null
                viewModel.save(t)
                if (isNew) scope.launch { listState.animateScrollToItem(0) }
            },
            onDelete = { deleteWithUndo(it) },
        )
    }
}

/** How long the list must sit still before a scrolled-away add button comes back. */
private const val FAB_LINGER_MS = 2_000L

/** The category icon's size relative to its badge, as [CategoryBadge] draws it. */
private const val BadgeIconScale = 0.46f

private fun LazyListScope.transactionList(
    transactions: List<Transaction>,
    badgeTurn: State<Float>,
    rowOrigin: ScallopOrigin,
    /** The row whose badge the open sheet grew out of, if any. */
    originId: Long?,
    onClick: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onClone: (Transaction) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val byDay = transactions.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    byDay.forEach { (day, items) ->
        item(key = "day-$day", contentType = "day") {
            DayHeader(day, items.sumOf { it.signedCents }, Modifier.animateItem())
        }
        itemsIndexed(items, key = { _, t -> t.id }, contentType = { _, _ -> "txn" }) { index, t ->
            TransactionRow(
                transaction = t,
                shape = segmentShape(index, items.size),
                badgeTurn = badgeTurn,
                origin = rowOrigin,
                isOrigin = t.id == originId,
                onClick = { onClick(t) },
                onDelete = { onDelete(t) },
                onClone = { onClone(t) },
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = 2.dp),
            )
        }
    }
}

/**
 * The add button: the same scalloped coin as the header mark, slowly turning. Tapping it
 * opens the entry sheet, which grows out of this scallop and shrinks back into it; the
 * button reports where it is and how it's turned to [origin] for that, and steps aside while
 * the sheet is drawing the scallop. Each handover gives the spin a springy kick.
 */
@Composable
private fun ScallopFab(
    origin: ScallopOrigin,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fast = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val idle = rememberInfiniteTransition(label = "fabSpin")
    val idleRotation by idle.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "fabIdle",
    )
    // Extra turn added on every handover; a low-damping spring lets it overshoot and settle.
    val kick = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    val covered = origin.covered
    LaunchedEffect(covered) {
        if (!covered && origin.slam) {
            // A save slams the sheet back in: take the hit, then whip round harder.
            origin.slam = false
            launch { squash.slamLanding() }
            kick.animateTo(kick.value + SlamTurn, SlamSpin)
        } else {
            kick.animateTo(
                kick.value + 120f,
                spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
            )
        }
    }

    // Slightly see-through, so no shadow: one would show through as a dark disc. The icon
    // stays opaque to keep its contrast over whatever scrolls underneath.
    val container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    SideEffect {
        origin.color = container
        origin.iconColor = content
        origin.rotation = { idleRotation + kick.value }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, fast, label = "fabPress")

    val addLabel = stringResource(R.string.add_transaction)
    val shape = MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier
            .size(68.dp)
            .onGloballyPositioned {
                origin.centerOnScreen = it.localToScreen(Offset(it.size.width / 2f, it.size.height / 2f))
                origin.diameter = it.size.width.toFloat()
            }
            .graphicsLayer {
                squashAndStretch(squash.value, scale)
                alpha = if (covered) 0f else 1f
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !covered,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = addLabel },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = idleRotation + kick.value }
                .background(container, shape)
        )
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val greeting = if (BuildConfig.DEBUG) {
        BuildConfig.VERSION_NAME
    } else {
        when (LocalTime.now().hour) {
            in 5..11 -> stringResource(R.string.greeting_morning)
            in 12..16 -> stringResource(R.string.greeting_afternoon)
            else -> stringResource(R.string.greeting_evening)
        }
    }
    ScreenHeader(
        title = stringResource(R.string.app_name),
        subtitle = greeting,
        leading = { CoinMark() },
        trailing = { SettingsButton(onOpenSettings) },
    )
}

/** Gear that gives a springy quarter-turn while pressed. */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val turn by animateFloatAsState(
        if (pressed) 90f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "gear",
    )
    val haptics = LocalHapticFeedback.current
    FilledTonalIconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        shapes = IconButtonDefaults.shapes(),
        interactionSource = interaction,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.settings),
            modifier = Modifier.graphicsLayer { rotationZ = turn },
        )
    }
}

/** The app's little scalloped coin, gently turning. */
@Composable
private fun CoinMark() {
    val spin = rememberInfiniteTransition(label = "coin")
    val rotation by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "coinRotation",
    )
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie12Sided.toShape())
        )
        // Monticello, as on the back of a US nickel.
        Icon(
            painterResource(R.drawable.ic_monticello),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(totals: Totals, summary: FilterSummary?) {
    val money = NickelTheme.money
    val pulse = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val bouncy = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val headlineCents = summary?.totalCents ?: totals.netCents
    LaunchedEffect(headlineCents) {
        if (first) { first = false; return@LaunchedEffect }
        pulse.animateTo(1.035f, spatial)
        pulse.animateTo(1f, bouncy)
    }
    val spin = rememberInfiniteTransition(label = "hero")
    val rotation by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "heroRotation",
    )

    Surface(
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
    ) {
        Box {
            // Decorative shapes drifting in the corner.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 56.dp, y = (-48).dp)
                    .size(200.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialShapes.Cookie9Sided.toShape(),
                    )
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-72).dp, y = 36.dp)
                    .size(96.dp)
                    .graphicsLayer { rotationZ = -rotation * 1.5f }
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                        MaterialShapes.Flower.toShape(),
                    )
            )
            Column(Modifier.padding(24.dp)) {
                val title = when {
                    summary == null -> stringResource(R.string.net_total)
                    summary.category != null && summary.kind == Kind.INCOME ->
                        stringResource(R.string.income_from, summary.category.displayLabel())
                    summary.category != null -> stringResource(R.string.spent_on, summary.category.displayLabel())
                    summary.kind == Kind.INCOME -> stringResource(R.string.total_income)
                    else -> stringResource(R.string.total_spent)
                }
                Crossfade(title, animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "heroTitle") {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                RollingText(
                    text = if (summary == null) Money.formatSigned(totals.netCents) else Money.format(summary.totalCents),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (summary == null) {
                        StatPill(
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            label = stringResource(R.string.money_in),
                            value = Money.format(totals.incomeCents),
                            container = money.incomeContainer,
                            content = money.onIncomeContainer,
                            modifier = Modifier.weight(1f),
                        )
                        StatPill(
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            label = stringResource(R.string.money_out),
                            value = Money.format(totals.expenseCents),
                            container = money.expenseContainer,
                            content = money.onExpenseContainer,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        val income = summary.kind == Kind.INCOME
                        val container = if (income) money.incomeContainer else money.expenseContainer
                        val content = if (income) money.onIncomeContainer else money.onExpenseContainer
                        StatPill(
                            icon = Icons.Rounded.CalendarMonth,
                            label = stringResource(R.string.this_month),
                            value = Money.format(summary.thisMonthCents),
                            container = container,
                            content = content,
                            modifier = Modifier.weight(1f),
                        )
                        // Within a category, how much of the whole it makes up says more than last month.
                        if (summary.share != null) {
                            StatPill(
                                icon = Icons.Rounded.PieChart,
                                label = stringResource(if (income) R.string.of_income else R.string.of_spending),
                                value = stringResource(R.string.percent, (summary.share * 100).roundToInt()),
                                container = container,
                                content = content,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            StatPill(
                                icon = Icons.Rounded.History,
                                label = stringResource(R.string.last_month),
                                value = Money.format(summary.lastMonthCents),
                                container = container,
                                content = content,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(content.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.8f))
            RollingText(value, style = MaterialTheme.typography.titleSmall, color = content)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterRow(selected: Filter, onSelect: (Filter) -> Unit) {
    val options = listOf(
        Filter.ALL to stringResource(R.string.filter_all),
        Filter.EXPENSE to stringResource(R.string.filter_expenses),
        Filter.INCOME to stringResource(R.string.filter_income),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (filter, label) ->
            val checked = filter == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(filter) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                AnimatedVisibility(
                    checked,
                    enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                    exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
                ) {
                    Row {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    }
                }
                Text(label, maxLines = 1)
            }
        }
    }
}

/**
 * Categories within the income or expense filter, popping open under the filter bar.
 * Tapping a category filters to it; tapping it again clears the filter.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryFilterCarousel(
    categories: List<Category>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    // Keep showing the last categories while the carousel collapses, rather than emptying it first.
    var shown by remember { mutableStateOf(categories) }
    LaunchedEffect(categories) { if (categories.isNotEmpty()) shown = categories }

    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    AnimatedVisibility(
        visible = categories.isNotEmpty(),
        enter = expandVertically(spatial, expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(spatial, shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        LazyRow(
            contentPadding = PaddingValues(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(shown, key = { it.id }) { category ->
                CategoryChip(
                    category = category,
                    selected = category.id == selected,
                    onClick = { onSelect(if (category.id == selected) null else category.id) },
                    badgeSize = 44.dp,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}


@Composable
private fun DayHeader(day: LocalDate, netCents: Long, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> remember(day, today.year) {
            day.format(localizedDateFormat(if (day.year == today.year) "EEEEMMMd" else "EEEyMMMd"))
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            Money.formatSigned(netCents),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransactionRow(
    transaction: Transaction,
    shape: RoundedCornerShape,
    badgeTurn: State<Float>,
    /** Filled in with this row's badge when it's tapped, so the edit sheet grows out of it. */
    origin: ScallopOrigin,
    /** Whether the open sheet grew out of this row's badge; it then keeps [origin] current. */
    isOrigin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeHaptics(dismissState)
    val scope = rememberCoroutineScope()
    val money = NickelTheme.money
    val timeFormatter = rememberTimeFormatter()
    val time = remember(transaction.timestamp, timeFormatter) {
        Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }
    // The same slow turn as the add button and coin, started at a different angle per row
    // so neighbouring shapes don't spin in lockstep.
    val phase = remember(transaction.id) { (transaction.id % 360).toFloat() }

    val badgeColor = categoryContainer(transaction.category)
    val badgeIconColor = onCategoryContainer(transaction.category)
    val badgeAt = remember { arrayOfNulls<LayoutCoordinates>(1) }
    /** Extra turn from a slammed landing, on top of the shared slow spin. */
    val badgeSlamTurn = remember { Animatable(0f) }
    val currentIsOrigin by rememberUpdatedState(isOrigin)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    fun claimOrigin() {
        val coords = badgeAt[0]?.takeIf { it.isAttached } ?: return
        // Measured where the row rests, not where a swipe has dragged it: a clone claims
        // this with the row still swiped off to the side, about to spring back.
        val swipe = runCatching { dismissState.requireOffset() }.getOrDefault(0f) * if (rtl) -1 else 1
        origin.centerOnScreen = coords.localToScreen(Offset(coords.size.width / 2f, coords.size.height / 2f)) -
            Offset(swipe, 0f)
        origin.diameter = coords.size.width.toFloat()
        origin.polygon = transaction.category.shape
        origin.color = badgeColor
        origin.icon = transaction.category.icon
        origin.iconColor = badgeIconColor
        origin.iconScale = BadgeIconScale
        origin.iconTwist = 0f
        origin.fliesToCategory = true
        origin.rotation = { phase + badgeTurn.value + badgeSlamTurn.value }
    }
    val badgeHidden = isOrigin && origin.covered
    // The badge takes the same slam as the add button when an edit is saved back into it.
    val badgeSquash = remember { Animatable(1f) }
    var wasHidden by remember { mutableStateOf(false) }
    LaunchedEffect(badgeHidden) {
        val landed = wasHidden && !badgeHidden
        wasHidden = badgeHidden
        if (landed && origin.slam) {
            origin.slam = false
            launch { badgeSquash.slamLanding() }
            badgeSlamTurn.animateTo(badgeSlamTurn.value + SlamTurn, SlamSpin)
        }
    }
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnClone by rememberUpdatedState(onClone)
    val currentClaim by rememberUpdatedState<() -> Unit> { claimOrigin() }
    // Kept stable on purpose: SwipeToDismissBox reruns this whenever the lambda changes while
    // the row sits dismissed, and a clone recomposes the row (it becomes the sheet's origin)
    // before it has sprung back, which fired the clone twice.
    val onSwiped: (SwipeToDismissBoxValue) -> Unit = remember(dismissState) {
        { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> currentOnDelete()
                // Cloning keeps the row: open the sheet and spring the row back into place.
                // Already handled on release (below); if the swipe did settle all the way
                // out, just bring the row back.
                SwipeToDismissBoxValue.StartToEnd -> scope.launch { dismissState.reset() }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }

    // Cloning keeps the row, so there's no need to wait for it to finish sliding offscreen
    // (the box only reports a dismissal once settled, a long invisible tail): as soon as a
    // release commits to it, open the sheet and spring the row back into place. targetValue
    // only flips once the finger lifts.
    // (currentValue is no use as a guard: it tracks the nearest anchor, so it already reads
    // dismissed once the drag passes halfway.)
    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.targetValue }.collect {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                currentClaim()
                currentOnClone()
                dismissState.reset()
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(shape),
        onDismiss = onSwiped,
        backgroundContent = {
            val cloning = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val active = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val colors = MaterialTheme.colorScheme
            val bg by animateColorAsState(
                when {
                    cloning && active -> colors.primary
                    cloning -> colors.primaryContainer
                    active -> colors.error
                    else -> colors.errorContainer
                },
                MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "dismissBg",
            )
            val iconScale by animateFloatAsState(
                if (active) 1.25f else 0.9f,
                MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "dismissIcon",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (cloning) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    if (cloning) Icons.Rounded.ContentCopy else Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(if (cloning) R.string.duplicate else R.string.delete),
                    tint = when {
                        cloning && active -> colors.onPrimary
                        cloning -> colors.onPrimaryContainer
                        active -> colors.onError
                        else -> colors.onErrorContainer
                    },
                    modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                )
            }
        },
    ) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = {
                claimOrigin()
                onClick()
            },
            color = MaterialTheme.colorScheme.surfaceBright,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interaction, 0.98f),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(
                    transaction.category,
                    size = 44.dp,
                    shapeRotation = { phase + badgeTurn.value + badgeSlamTurn.value },
                    containerColor = badgeColor,
                    contentColor = badgeIconColor,
                    // The sheet draws this badge itself while it grows out of it and shrinks
                    // back in, so the row's own copy steps aside for that time.
                    modifier = Modifier
                        .onGloballyPositioned {
                            badgeAt[0] = it
                            if (currentIsOrigin) claimOrigin()
                        }
                        .graphicsLayer {
                            alpha = if (badgeHidden) 0f else 1f
                            squashAndStretch(badgeSquash.value)
                        },
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.note.ifBlank { transaction.category.displayLabel() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (transaction.note.isBlank()) time else stringResource(R.string.dot_pair, transaction.category.displayLabel(), time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    Money.formatSigned(transaction.signedCents),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = if (transaction.kind == Kind.INCOME) money.income else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Makes the clone/delete swipe feel like it has detents: light ticks as the row is dragged
 * toward the point where letting go would act, a firm click on crossing it, a softer tick
 * on backing off below it, and a settling tick if released without acting. The action
 * itself keeps its own confirm buzz.
 */
@Composable
private fun SwipeHaptics(state: SwipeToDismissBoxState) {
    val haptics = LocalHapticFeedback.current
    val step = with(LocalDensity.current) { SwipeTickStep.toPx() }
    val threshold = with(LocalDensity.current) { SwipeActThreshold.toPx() }
    LaunchedEffect(state) {
        var ticks = 0
        var armed = false
        var moving = false
        /** Set once an action fires; the row's return trip isn't the finger's doing. */
        var acted = false
        snapshotFlow {
            runCatching { abs(state.requireOffset()) }.getOrDefault(0f) to state.targetValue
        }.collect { (offset, target) ->
            // targetValue flips only when a release commits to acting (a clone then springs
            // straight back), so it marks an action. currentValue can't: it tracks the nearest
            // anchor and reads dismissed mid-drag.
            if (target != SwipeToDismissBoxValue.Settled) acted = true
            if (offset == 0f) {
                // Back at rest: a soft landing if it was let go without acting, then start over.
                if (moving && !acted) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                ticks = 0
                armed = false
                moving = false
                acted = false
                return@collect
            }
            if (acted) return@collect
            moving = true
            // The state's targetValue only flips once the finger lifts, so judge "letting go
            // now would act" from the distance against the same threshold it uses.
            val nowArmed = offset >= threshold
            when {
                nowArmed && !armed -> haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                !nowArmed && armed -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                !nowArmed -> {
                    val reached = (offset / step).toInt()
                    if (reached > ticks) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    ticks = reached
                }
            }
            armed = nowArmed
        }
    }
}

/** [SwipeToDismissBoxDefaults]' positional threshold: past it, letting go acts. */
private val SwipeActThreshold = 56.dp

/** Spacing of the detent ticks; four of them lead up to [SwipeActThreshold]. */
private val SwipeTickStep = 14.dp

@Composable
private fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    val spin = rememberInfiniteTransition(label = "empty")
    val rotation by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "emptyRotation",
    )
    val bob by spin.animateFloat(
        -6f, 6f,
        infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "bob",
    )
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(152.dp)
                .graphicsLayer { translationY = bob * density },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie9Sided.toShape())
            )
            Icon(
                Icons.Rounded.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
