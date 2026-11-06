package io.github.draumaz.nickel.ui

import androidx.compose.ui.platform.LocalContext
import io.github.draumaz.nickel.ui.components.displayLabel
import io.github.draumaz.nickel.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.draumaz.nickel.data.Categories
import io.github.draumaz.nickel.data.Category
import io.github.draumaz.nickel.data.CategoryIcons
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.categoryContainer
import io.github.draumaz.nickel.ui.components.onCategoryContainer

private const val MAX_NAME = 24

/** Trailing chip in the category row that opens [NewCategoryDialog]. Sized to match [CategoryChip]. */
@Composable
internal fun NewCategoryChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = stringResource(R.string.new_category), onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.new_short),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Names a new category of [kind] and picks its icon, with a live preview of the badge.
 * The icon follows the name (via [CategoryIcons.guess]) until the user picks one themselves.
 * A name that matches an existing category of the same kind selects that one instead of
 * creating a duplicate; [onCreate] says which happened.
 */
@Composable
internal fun NewCategoryDialog(
    kind: Kind,
    existing: List<Category>,
    onDismiss: () -> Unit,
    onCreate: (category: Category, isNew: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var pickedIcon by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val newLabel = stringResource(R.string.new_short)
    val label = name.trim()
    // Matches the translated name too, so typing "Продукты" finds the built-in Groceries.
    val match = existing.firstOrNull {
        it.label.equals(label, ignoreCase = true) || it.displayLabel(context).equals(label, ignoreCase = true)
    }
    val preview = Categories.custom(label.ifEmpty { newLabel }, kind).let { c ->
        c.copy(iconKey = pickedIcon ?: c.iconKey)
    }
    val canCreate = label.isNotEmpty()
    fun create() {
        if (!canCreate) return
        if (match != null) onCreate(match, false) else onCreate(preview.copy(label = label), true)
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            CategoryBadge(category = preview, size = 64.dp)
        },
        title = { Text(stringResource(if (kind == Kind.INCOME) R.string.new_income_category else R.string.new_expense_category)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME) },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    supportingText = match?.let { { Text(stringResource(R.string.category_exists, it.displayLabel())) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { create() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                ) {
                    items(CategoryIcons.keys, key = { it }) { key ->
                        val selected = key == preview.iconKey
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) categoryContainer(preview)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .clickable(onClickLabel = key) { pickedIcon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                CategoryIcons.get(key),
                                contentDescription = key,
                                tint = if (selected) onCategoryContainer(preview)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::create, enabled = canCreate) {
                Text(stringResource(if (match != null) R.string.use_existing else R.string.create), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
