package io.github.draumaz.nickel.ui

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
import androidx.compose.material3.SheetState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    sheetState: SheetState,
    initialKind: Kind,
    editing: Transaction?,
    customCategories: List<Category>,
    onCreateCategory: (Category) -> Unit,
    onDismiss: () -> Unit,
    /** Prefills a *new* entry (amount, kind, category, note) from it, timestamped now. */
    template: Transaction? = null,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        EntryContent(
            initialKind = initialKind,
            editing = editing,
            template = template,
            customCategories = customCategories,
            onCreateCategory = onCreateCategory,
            onSave = { close { onSave(it) } },
            onDelete = { close { onDelete(it) } },
        )
    }
}

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
                    Text(if (k == Kind.EXPENSE) "Expense" else "Income")
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
        val rowState = rememberLazyListState()
        LaunchedEffect(kind) { rowState.animateScrollToItem(maxOf(0, categories.indexOf(category) - 1)) }
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(categories, key = { it.id }) { c ->
                CategoryChip(
                    category = c,
                    selected = c == category,
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
                placeholder = { Text("Add a note") },
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
        Box(Modifier.padding(horizontal = 20.dp)) {
            KindSwipeHints(swipe, kind, gutter = 20.dp)
            Keypad(
                onKey = { key ->
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    val next = applyKey(input, key)
                    if (next == input && key != '<') {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    }
                    input = next
                },
                onClear = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    input = ""
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
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
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
                        editing != null -> "Save changes"
                        kind == Kind.INCOME -> "Add income"
                        else -> "Add expense"
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
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
            title = { Text("Time") },
            confirmButton = {
                TextButton(onClick = {
                    timestamp = dateTime
                        .with(LocalTime.of(pickerState.hour, pickerState.minute))
                        .toInstant()
                        .toEpochMilli()
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
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
                    .clickable(onClickLabel = "Change date", onClick = onPickDate)
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
                    .clickable(onClickLabel = "Change time", onClick = onPickTime)
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

private fun dayLabel(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MMM d" else "MMM d, yyyy"))
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
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            category.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
