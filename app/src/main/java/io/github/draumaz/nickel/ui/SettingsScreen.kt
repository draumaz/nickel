package io.github.draumaz.nickel.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.ImportPlan
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.pressScale
import io.github.draumaz.nickel.ui.components.segmentShape
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(viewModel: NickelViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::prepareImport)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::export)
    }

    // Predictive back: the page shrinks and rounds off as the gesture progresses.
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler { events ->
        try {
            events.collect { backProgress = it.progress }
            onBack()
        } catch (e: CancellationException) {
            backProgress = 0f
            throw e
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = Modifier.graphicsLayer {
            val scale = 1f - 0.08f * backProgress
            scaleX = scale
            scaleY = scale
            shape = RoundedCornerShape(36.dp * backProgress)
            clip = backProgress > 0f
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            SectionLabel("Appearance")
            SettingRow(
                icon = Icons.Rounded.Palette,
                iconShape = MaterialShapes.Cookie7Sided,
                title = "Dynamic color",
                subtitle = if (viewModel.dynamicColorSupported) "Use the colors from your wallpaper"
                else "Needs Android 12 or newer",
                shape = segmentShape(0, 2),
                enabled = viewModel.dynamicColorSupported,
                checked = settings.dynamicColor,
                onCheckedChange = {
                    haptics.performHapticFeedback(
                        if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                    )
                    viewModel.setDynamicColor(it)
                },
            )
            PaletteRow(shape = segmentShape(1, 2))

            SectionLabel("Your data")
            SettingRow(
                icon = Icons.Rounded.Download,
                iconShape = MaterialShapes.Clover4Leaf,
                title = "Import from CSV",
                subtitle = "Bring in transactions from Dime or a spreadsheet",
                shape = segmentShape(0, 2),
                onClick = {
                    importLauncher.launch(
                        arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")
                    )
                },
            )
            SettingRow(
                icon = Icons.Rounded.Upload,
                iconShape = MaterialShapes.Sunny,
                title = "Export to CSV",
                subtitle = when (state.count) {
                    0 -> "Nothing to export yet"
                    1 -> "Save your 1 transaction as a CSV file"
                    else -> "Save all ${state.count} transactions as a CSV file"
                },
                shape = segmentShape(1, 2),
                enabled = state.count > 0,
                onClick = { exportLauncher.launch("nickel-${LocalDate.now()}.csv") },
            )

            Footer()
        }
    }

    TransferDialogs(
        state = transfer,
        onConfirm = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.confirmImport()
        },
        onDismiss = viewModel::dismissTransfer,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconShape: androidx.graphics.shapes.RoundedPolygon,
    title: String,
    subtitle: String,
    shape: RoundedCornerShape,
    enabled: Boolean = true,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val rowModifier = if (checked != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interaction,
            indication = androidx.compose.material3.ripple(),
            onValueChange = onCheckedChange,
        )
    } else Modifier
    Surface(
        onClick = onClick,
        enabled = enabled && checked == null,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .pressScale(interaction, 0.98f),
    ) {
        Row(
            rowModifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val alpha = if (enabled) 1f else 0.38f
            Box(
                Modifier
                    .size(44.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(MaterialTheme.colorScheme.secondaryContainer, iconShape.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer { this.alpha = alpha }
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (checked != null) {
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled,
                    thumbContent = if (checked) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
                )
            }
        }
    }
}

/** Swatches of the live palette, so toggling dynamic color has something to watch. */
@Composable
private fun PaletteRow(shape: RoundedCornerShape) {
    val c = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = c.surfaceBright,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                c.primary to MaterialShapes.Cookie9Sided,
                c.secondary to MaterialShapes.Clover4Leaf,
                c.tertiary to MaterialShapes.Sunny,
                c.primaryContainer to MaterialShapes.Flower,
                c.secondaryContainer to MaterialShapes.Pill,
                c.tertiaryContainer to MaterialShapes.Gem,
            ).forEach { (color, polygon) ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(color, polygon.toShape())
                )
            }
        }
    }
}

@Composable
private fun Footer() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialShapes.Cookie12Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_monticello),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Nickel 1.0",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransferDialogs(
    state: TransferState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        TransferState.Idle -> Unit
        TransferState.Reading -> BusyDialog("Reading your file…")
        TransferState.Importing -> BusyDialog("Tucking everything in…")
        is TransferState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't import") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
        is TransferState.Preview -> ImportPreviewDialog(state.plan, state.fileName, onConfirm, onDismiss)
    }
}

@Composable
private fun BusyDialog(message: String) {
    BasicAlertDialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadingIndicator(Modifier.size(72.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportPreviewDialog(
    plan: ImportPlan,
    fileName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val count = plan.transactions.size
    if (count == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Already imported") },
            text = {
                Text(
                    "All ${plan.duplicates} transactions in ${fileName ?: "this file"} are already in Nickel."
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
        return
    }

    val zone = ZoneId.systemDefault()
    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val range = if (plan.earliest != null && plan.latest != null) {
        val start = day(plan.earliest)
        val end = day(plan.latest)
        val full = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val short = DateTimeFormatter.ofPattern("MMM d")
        when {
            start == end -> start.format(full)
            start.year == end.year -> "${start.format(short)} – ${end.format(full)}"
            else -> "${start.format(full)} – ${end.format(full)}"
        }
    } else null
    val incomes = plan.transactions.count { it.kind == Kind.INCOME }
    val expenses = count - incomes

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = { Text("Import $count transaction${if (count == 1) "" else "s"}?", textAlign = TextAlign.Center) },
        text = {
            Column(Modifier.animateContentSize()) {
                fileName?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                range?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "$expenses expense${if (expenses == 1) "" else "s"} · $incomes income${if (incomes == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val skipped = buildList {
                    if (plan.duplicates > 0) add("${plan.duplicates} already in Nickel")
                    if (plan.unreadable > 0) add("${plan.unreadable} unreadable")
                }
                if (skipped.isNotEmpty()) {
                    Text(
                        "Skipping " + skipped.joinToString(" and "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (plan.newCategories.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "New categories",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        plan.newCategories.forEach { c ->
                            Row(
                                Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryBadge(c, size = 26.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(c.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconContentColor = Color.Unspecified,
    )
}
