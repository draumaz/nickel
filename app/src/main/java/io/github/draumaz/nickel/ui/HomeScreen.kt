package io.github.draumaz.nickel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
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
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.RollingText
import io.github.draumaz.nickel.ui.components.ScreenHeader
import io.github.draumaz.nickel.ui.components.pressScale
import io.github.draumaz.nickel.ui.components.segmentShape
import io.github.draumaz.nickel.ui.theme.NickelTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(viewModel: NickelViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var sheetKind by remember { mutableStateOf<Kind?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(fabExpanded) { fabExpanded = false }

    fun deleteWithUndo(t: Transaction) {
        viewModel.delete(t)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Deleted ${t.note.ifBlank { t.category.label }}",
                actionLabel = "Undo",
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
        floatingActionButton = {
            val fabVisible by remember { derivedStateOf { !state.loading } }
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier = Modifier
                            .semantics {
                                traversalIndex = -1f
                                stateDescription = if (fabExpanded) "Expanded" else "Collapsed"
                            }
                            .animateFloatingActionButton(
                                visible = fabVisible || fabExpanded,
                                alignment = Alignment.BottomEnd,
                            ),
                        checked = fabExpanded,
                        onCheckedChange = {
                            haptics.performHapticFeedback(
                                if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                            )
                            fabExpanded = it
                        },
                    ) {
                        val icon by remember {
                            derivedStateOf { if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Add }
                        }
                        Icon(
                            painter = rememberVectorPainter(icon),
                            contentDescription = "Add transaction",
                            modifier = Modifier.animateIcon({ checkedProgress }),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabExpanded = false
                        editing = null
                        sheetKind = Kind.INCOME
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null) },
                    text = { Text("Income") },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabExpanded = false
                        editing = null
                        sheetKind = Kind.EXPENSE
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.TrendingDown, contentDescription = null) },
                    text = { Text("Expense") },
                )
            }
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
                    HeroCard(
                        netCents = state.totals.netCents,
                        incomeCents = state.totals.incomeCents,
                        expenseCents = state.totals.expenseCents,
                    )
                }
                item(key = "filter", contentType = "filter") {
                    FilterRow(
                        selected = state.filter,
                        onSelect = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            viewModel.setFilter(it)
                        },
                    )
                }

                when {
                    state.isEmpty -> item(key = "empty", contentType = "empty") {
                        EmptyState(
                            title = "Your ledger is empty",
                            body = "Tap + to log your first expense or income.",
                            modifier = Modifier.animateItem(),
                        )
                    }
                    state.transactions.isEmpty() -> item(key = "none", contentType = "empty") {
                        EmptyState(
                            title = if (state.filter == Filter.INCOME) "No income yet" else "No expenses yet",
                            body = "Nothing to see here, which is honestly kind of nice.",
                            modifier = Modifier.animateItem(),
                        )
                    }
                    else -> transactionList(
                        transactions = state.transactions,
                        onClick = {
                            editing = it
                            sheetKind = it.kind
                        },
                        onDelete = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            deleteWithUndo(it)
                        },
                    )
                }
            }
        }
    }

    sheetKind?.let { kind ->
        EntrySheet(
            sheetState = sheetState,
            initialKind = kind,
            editing = editing,
            customCategories = state.customCategories,
            onDismiss = {
                sheetKind = null
                editing = null
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

private fun LazyListScope.transactionList(
    transactions: List<Transaction>,
    onClick: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
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
                onClick = { onClick(t) },
                onDelete = { onDelete(t) },
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Up late?"
    }
    ScreenHeader(
        title = "Nickel",
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
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        interactionSource = interaction,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            Icons.Rounded.Settings,
            contentDescription = "Settings",
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
private fun HeroCard(netCents: Long, incomeCents: Long, expenseCents: Long) {
    val money = NickelTheme.money
    val pulse = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val bouncy = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(netCents) {
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
                Text(
                    "Net total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(4.dp))
                RollingText(
                    text = Money.formatSigned(netCents),
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        label = "In",
                        value = Money.format(incomeCents),
                        container = money.incomeContainer,
                        content = money.onIncomeContainer,
                        modifier = Modifier.weight(1f),
                    )
                    StatPill(
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        label = "Out",
                        value = Money.format(expenseCents),
                        container = money.expenseContainer,
                        content = money.onExpenseContainer,
                        modifier = Modifier.weight(1f),
                    )
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
    val options = listOf(Filter.ALL to "All", Filter.INCOME to "Income", Filter.EXPENSE to "Expenses")
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

@Composable
private fun DayHeader(day: LocalDate, netCents: Long, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(
            DateTimeFormatter.ofPattern(if (day.year == today.year) "EEEE, MMM d" else "EEE, MMM d, yyyy")
        )
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
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val money = NickelTheme.money
    val time = remember(transaction.timestamp) {
        Instant.ofEpochMilli(transaction.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(shape),
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            val active = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
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
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = if (active) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                )
            }
        },
    ) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
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
                CategoryBadge(transaction.category, size = 44.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.note.ifBlank { transaction.category.label },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (transaction.note.isBlank()) time else "${transaction.category.label} · $time",
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
