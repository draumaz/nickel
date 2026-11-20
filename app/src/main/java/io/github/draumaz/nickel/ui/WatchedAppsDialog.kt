package io.github.draumaz.nickel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.notices.InstalledApp
import io.github.draumaz.nickel.notices.WatchedApps
import io.github.draumaz.nickel.ui.components.DialogListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks which apps' notifications auto-add reads. Every change saves at once, like the
 * Settings switches, so Done only closes the dialog.
 *
 * The order is fixed when the dialog opens (picked apps first, then the rest by name) and
 * doesn't reshuffle as boxes are ticked, so a row never jumps out from under a finger.
 */
@Composable
internal fun WatchedAppsDialog(viewModel: NickelViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val selected = viewModel.noticeApps.collectAsStateWithLifecycle().value.orEmpty()
    var query by rememberSaveable { mutableStateOf("") }

    val apps by produceState<List<InstalledApp>?>(null) {
        val initial = selected
        val installed = viewModel.installedApps()
        // Apps that joined without a launcher icon (they posted an alert and looked
        // financial) still need a row, or they could never be taken back out.
        val hidden = withContext(Dispatchers.IO) {
            (initial - installed.mapTo(HashSet()) { it.packageName }).map {
                InstalledApp(it, WatchedApps.labelFor(context, it) ?: it)
            }
        }
        value = (installed + hidden).sortedWith(
            compareByDescending<InstalledApp> { it.packageName in initial }.thenBy { it.label.lowercase() }
        )
    }

    // Wider than the platform's dialog width: each row carries an icon, a label, a package
    // name and a checkbox, and at the default width package names were cut off.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        title = { Text(stringResource(R.string.watched_apps)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.watched_apps_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                val list = apps
                if (list == null) {
                    Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(56.dp))
                    }
                    return@Column
                }
                val everyPackage = list.mapTo(HashSet()) { it.packageName }
                val allOn = everyPackage.isNotEmpty() && selected.containsAll(everyPackage)
                SelectAllRow(
                    checked = allOn,
                    onCheckedChange = { on ->
                        haptics.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        viewModel.setNoticeApps(if (on) selected + everyPackage else emptySet())
                    },
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(30) },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                val q = query.trim()
                val matches = if (q.isEmpty()) list else list.filter {
                    it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
                }
                LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                    items(matches, key = { it.packageName }) { app ->
                        AppOption(
                            app = app,
                            checked = app.packageName in selected,
                            onCheckedChange = { on ->
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                viewModel.setNoticeApps(if (on) selected + app.packageName else selected - app.packageName)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun SelectAllRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The click overload plus switch semantics, not the checked overload: that one announces a
    // checkbox and tints the row as selected, which suits the app rows below but not a switch.
    ListItem(
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
            )
        },
        colors = DialogListItem.colors(),
        contentPadding = DialogListItem.ContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            },
    ) {
        Text(stringResource(R.string.watched_apps_select_all), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AppOption(app: InstalledApp, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        leadingContent = { AppIcon(app.packageName) },
        supportingContent = { Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        colors = DialogListItem.colors(),
        contentPadding = DialogListItem.ContentPadding,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Loaded per row as it scrolls into view and off the main thread: decoding every launcher
 * icon up front would stall the dialog's opening on a phone with a few hundred apps.
 */
@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val size = 36.dp
    val icon by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(Modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape))
    }
}
