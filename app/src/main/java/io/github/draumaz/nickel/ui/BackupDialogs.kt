package io.github.draumaz.nickel.ui

import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.DateUtils
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.BackupFiles
import io.github.draumaz.nickel.data.BackupSettings

/**
 * The system folder picker, which already offers "New folder", opened on Documents so the
 * user starts somewhere sensible instead of the recents list. Android 11+ refuses the
 * storage root and Download itself as trees, which is one more reason to start a level down.
 */
@Composable
internal fun rememberBackupFolderPicker(onPicked: (Uri?) -> Unit): () -> Unit {
    val launcher: ManagedActivityResultLauncher<Uri?, Uri?> =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree(), onPicked)
    return { launcher.launch(DOCUMENTS_START) }
}

private val DOCUMENTS_START: Uri =
    DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Documents")

/** Asked once, on first launch: choosing a folder turns on daily backups. */
@Composable
internal fun BackupPrompt(onChooseFolder: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        title = { Text(stringResource(R.string.backup_prompt_title), textAlign = TextAlign.Center) },
        text = {
            Text(
                stringResource(R.string.backup_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.not_now)) } },
        confirmButton = {
            Button(onClick = onChooseFolder, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.backup_choose_folder))
            }
        },
    )
}

/** How often to back up, plus a manual run for peace of mind. */
@Composable
internal fun BackupIntervalDialog(
    viewModel: NickelViewModel,
    backup: BackupSettings,
    onDismiss: () -> Unit,
) {
    val busy by viewModel.backingUp.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_backup)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.backup_interval_body, BackupFiles.KEEP),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup()) {
                    BackupFiles.INTERVALS_HOURS.forEach { hours ->
                        val selected = backup.intervalHours == hours
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .selectable(selected, role = Role.RadioButton) { viewModel.setBackupInterval(hours) }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(intervalLabel(hours), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    lastBackupLabel(backup).replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backup.lastBackupFailed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::backupNow, enabled = !busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (busy) LoadingIndicator(Modifier.size(20.dp))
                    Text(stringResource(R.string.backup_now))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
internal fun intervalLabel(hours: Int): String = when {
    hours <= 0 -> stringResource(R.string.auto_backup_off)
    hours == 24 -> stringResource(R.string.backup_daily)
    else -> pluralStringResource(R.plurals.backup_every_days, hours / 24, hours / 24)
}

@Composable
internal fun lastBackupLabel(backup: BackupSettings): String = when {
    backup.lastBackupFailed -> stringResource(R.string.backup_last_failed)
    backup.lastBackupAt == 0L -> stringResource(R.string.backup_never)
    else -> stringResource(
        R.string.backup_last,
        DateUtils.getRelativeTimeSpanString(
            backup.lastBackupAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        ).toString(),
    )
}
