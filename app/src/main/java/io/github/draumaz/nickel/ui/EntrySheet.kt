package io.github.draumaz.nickel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.data.Transaction
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.PoppingText
import io.github.draumaz.nickel.ui.components.onToneContainer
import io.github.draumaz.nickel.ui.components.toneContainer
import io.github.draumaz.nickel.ui.theme.NickelTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    sheetState: SheetState,
    initialKind: Kind,
    editing: Transaction?,
    customCategories: List<Category>,
    onDismiss: () -> Unit,
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
            customCategories = customCategories,
            onSave = { close { onSave(it) } },
            onDelete = { close { onDelete(it) } },
        )
    }
}

private const val MAX_WHOLE_DIGITS = 9

private fun applyKey(input: String, key: Char): String = when (key) {
    '<' -> input.dropLast(1)
    '.' -> when {
        input.contains('.') -> input
        input.isEmpty() -> "0."
        else -> "$input."
    }
    else -> {
        val dot = input.indexOf('.')
        when {
            dot >= 0 && input.length - dot > 2 -> input // two decimals max
            dot < 0 && input.length >= MAX_WHOLE_DIGITS -> input
            input == "0" -> key.toString()
            else -> input + key
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EntryContent(
    initialKind: Kind,
    editing: Transaction?,
    customCategories: List<Category>,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    var kind by rememberSaveable { mutableStateOf(editing?.kind ?: initialKind) }
    var input by rememberSaveable { mutableStateOf(editing?.let { Money.toInput(it.amountCents) } ?: "") }
    var category by rememberSaveable { mutableStateOf(editing?.category ?: Categories.forKind(kind, customCategories).first()) }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var timestamp by rememberSaveable { mutableLongStateOf(editing?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                    onCheckedChange = {
                        if (kind != k) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            kind = k
                            if (category.kind != k) category = Categories.forKind(k, customCategories).first()
                        }
                    },
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
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Note + date.
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
            FilledTonalButton(
                onClick = { showDatePicker = true },
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(56.dp),
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(dayLabel(timestamp), maxLines = 1)
            }
        }

        Spacer(Modifier.height(16.dp))

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
            modifier = Modifier.padding(horizontal = 20.dp),
        )

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
}

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
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val progress by animateFloatAsState(if (selected) 1f else 0f, spatial, label = "morph")
    val rotation by animateFloatAsState(if (selected) 0f else -60f, spatial, label = "spin")
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spatial, label = "scale")
    val container by animateColorAsState(
        if (selected) toneContainer(category.tone) else MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "container",
    )
    val content by animateColorAsState(
        if (selected) onToneContainer(category.tone) else MaterialTheme.colorScheme.onSurfaceVariant,
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
            size = 56.dp,
            shapeProgress = progress,
            containerColor = container,
            contentColor = content,
            shapeRotation = rotation,
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
