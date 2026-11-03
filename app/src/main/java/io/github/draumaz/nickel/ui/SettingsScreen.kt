package io.github.draumaz.nickel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.ImportPlan
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.export.ExportShare
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.ScreenHeader
import io.github.draumaz.nickel.ui.components.pressScale
import io.github.draumaz.nickel.ui.components.segmentShape
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.shareExports.collect { ExportShare.share(context, it) }
    }

    // Predictive back is handled by MainActivity, which scrubs the transition back to Home.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            ScreenHeader(
                title = "Settings",
                subtitle = "Make Nickel yours",
                leading = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )

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
                    1 -> "Share or save your 1 transaction as a CSV file"
                    else -> "Share or save all ${state.count} transactions as a CSV file"
                },
                shape = segmentShape(1, 2),
                enabled = state.count > 0,
                onClick = viewModel::export,
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

private const val REPO_URL = "https://github.com/draumaz/nickel"
private const val AUTHOR_URL = "https://github.com/draumaz"

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
        val linkStyle = TextLinkStyles(
            SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
        )
        Text(
            buildAnnotatedString {
                withLink(LinkAnnotation.Url(REPO_URL, linkStyle)) { append("Nickel") }
                append(", 1.0\nby ")
                withLink(LinkAnnotation.Url(AUTHOR_URL, linkStyle)) { append("draumaz") }
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One dialog for the whole import: the card morphs between reading, preview, and saving
 * instead of swapping separate dialogs, so each step flows into the next.
 */
@Composable
private fun TransferDialogs(
    state: TransferState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == TransferState.Idle) return
    val busy = state == TransferState.Reading || state == TransferState.Importing
    BasicAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = !busy, dismissOnBackPress = !busy),
    ) {
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(effects) + scaleIn(initialScale = 0.92f))
                        .togetherWith(fadeOut(effects))
                        .using(SizeTransform(clip = true) { _, _ -> spatial })
                },
                contentAlignment = Alignment.Center,
                label = "transfer",
            ) { s ->
                when (s) {
                    TransferState.Reading -> BusyContent("Reading your file…")
                    TransferState.Importing -> BusyContent("Tucking everything in…")
                    is TransferState.Failed -> MessageContent("Couldn't import", s.message, onDismiss)
                    is TransferState.Preview -> if (s.plan.transactions.isEmpty()) {
                        MessageContent(
                            "Already imported",
                            "All ${s.plan.duplicates} transactions in ${s.fileName ?: "this file"} are already in Nickel.",
                            onDismiss,
                        )
                    } else {
                        PreviewContent(s.plan, s.fileName, onConfirm, onDismiss)
                    }
                    TransferState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun BusyContent(message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 32.dp),
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

@Composable
private fun MessageContent(title: String, body: String, onDismiss: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewContent(
    plan: ImportPlan,
    fileName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val count = plan.transactions.size
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

    Column(Modifier.padding(24.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Import $count transaction${if (count == 1) "" else "s"}?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        fileName?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        val body = MaterialTheme.typography.bodyMedium
        val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
        range?.let { Text(it, style = body, color = bodyColor) }
        Text(
            "$expenses expense${if (expenses == 1) "" else "s"} · $incomes income${if (incomes == 1) "" else "s"}",
            style = body,
            color = bodyColor,
        )
        val skipped = buildList {
            if (plan.duplicates > 0) add("${plan.duplicates} already in Nickel")
            if (plan.unreadable > 0) add("${plan.unreadable} unreadable")
        }
        if (skipped.isNotEmpty()) {
            Text("Skipping " + skipped.joinToString(" and "), style = body, color = bodyColor)
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
                        Text(
                            c.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text("Import") }
        }
    }
}
