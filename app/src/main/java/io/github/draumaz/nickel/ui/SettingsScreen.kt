package io.github.draumaz.nickel.ui

import io.github.draumaz.nickel.ui.components.GlassIconButton
import io.github.draumaz.nickel.ui.components.localizedDateFormat
import io.github.draumaz.nickel.ui.components.displayLabel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.draumaz.nickel.R
import io.github.draumaz.nickel.data.ImportPlan
import io.github.draumaz.nickel.data.Kind
import io.github.draumaz.nickel.data.Money
import io.github.draumaz.nickel.export.ExportShare
import io.github.draumaz.nickel.notices.NickelNoticeListener
import io.github.draumaz.nickel.ui.components.CategoryBadge
import io.github.draumaz.nickel.ui.components.ScreenHeader
import io.github.draumaz.nickel.ui.components.pressScale
import io.github.draumaz.nickel.ui.components.segmentShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency

@Composable
fun SettingsScreen(viewModel: NickelViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var showCurrency by rememberSaveable { mutableStateOf(false) }
    val backup by viewModel.backup.collectAsStateWithLifecycle()
    var showBackupInterval by rememberSaveable { mutableStateOf(false) }
    val pickBackupFolder = rememberBackupFolderPicker { uri ->
        uri?.let {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.chooseBackupFolder(it)
        }
    }

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
                title = stringResource(R.string.settings),
                subtitle = stringResource(R.string.settings_subtitle),
                leading = {
                    GlassIconButton(onClick = onBack, size = 44.dp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )

            SectionLabel(stringResource(R.string.section_appearance))
            SettingRow(
                icon = Icons.Rounded.Palette,
                iconShape = MaterialShapes.Cookie7Sided,
                title = stringResource(R.string.dynamic_color),
                subtitle = stringResource(R.string.dynamic_color_on),
                shape = segmentShape(0, 2),
                checked = settings.dynamicColor,
                onCheckedChange = {
                    haptics.performHapticFeedback(
                        if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                    )
                    viewModel.setDynamicColor(it)
                },
            )
            PaletteRow(shape = segmentShape(1, 2))

            SectionLabel(stringResource(R.string.section_money))
            SettingRow(
                icon = Icons.Rounded.CurrencyExchange,
                iconShape = MaterialShapes.Cookie9Sided,
                title = stringResource(R.string.currency),
                subtitle = settings.currencyCode
                    ?.let { runCatching { currencyLabel(Currency.getInstance(it)) }.getOrNull() }
                    ?: stringResource(R.string.system_default_currency, currencyLabel(Money.systemCurrency)),
                shape = segmentShape(0, 1),
                onClick = { showCurrency = true },
            )
            if (showCurrency) {
                CurrencyDialog(
                    selectedCode = settings.currencyCode,
                    onDismiss = { showCurrency = false },
                    onSelect = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.setCurrency(it)
                        showCurrency = false
                    },
                )
            }

            SectionLabel(stringResource(R.string.section_automation))
            NotificationAccessRow(viewModel, settings.autoAddNotices, haptics, shape = segmentShape(0, 2))
            SettingRow(
                icon = Icons.Rounded.Widgets,
                iconShape = MaterialShapes.Pentagon,
                title = stringResource(R.string.widget_opens_entry),
                subtitle = stringResource(R.string.widget_opens_entry_on),
                shape = segmentShape(1, 2),
                checked = settings.widgetOpensEntry,
                onCheckedChange = {
                    haptics.performHapticFeedback(
                        if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                    )
                    viewModel.setWidgetOpensEntry(it)
                },
            )

            SectionLabel(stringResource(R.string.section_data))
            SettingRow(
                icon = Icons.Rounded.Download,
                iconShape = MaterialShapes.Clover4Leaf,
                title = stringResource(R.string.import_csv),
                subtitle = stringResource(R.string.import_csv_subtitle),
                shape = segmentShape(0, 4),
                onClick = {
                    importLauncher.launch(
                        arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")
                    )
                },
            )
            SettingRow(
                icon = Icons.Rounded.Upload,
                iconShape = MaterialShapes.Sunny,
                title = stringResource(R.string.export_csv),
                subtitle = if (state.count == 0) stringResource(R.string.export_nothing)
                else pluralStringResource(R.plurals.export_subtitle, state.count, state.count),
                shape = segmentShape(1, 4),
                enabled = state.count > 0,
                onClick = viewModel::export,
            )
            SettingRow(
                icon = Icons.Rounded.Folder,
                iconShape = MaterialShapes.Cookie4Sided,
                title = stringResource(R.string.backup_folder),
                subtitle = backup.folderName ?: stringResource(R.string.backup_folder_unset),
                shape = segmentShape(2, 4),
                onClick = pickBackupFolder,
            )
            SettingRow(
                icon = Icons.Rounded.History,
                iconShape = MaterialShapes.Flower,
                title = stringResource(R.string.auto_backup),
                subtitle = when {
                    backup.folderUri == null -> stringResource(R.string.auto_backup_needs_folder)
                    backup.intervalHours <= 0 -> stringResource(R.string.auto_backup_off)
                    else -> stringResource(R.string.dot_pair, intervalLabel(backup.intervalHours), lastBackupLabel(backup))
                },
                shape = segmentShape(3, 4),
                enabled = backup.folderUri != null,
                onClick = { showBackupInterval = true },
            )
            if (showBackupInterval) {
                BackupIntervalDialog(viewModel, backup, onDismiss = { showBackupInterval = false })
            }

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

/**
 * Notification access is granted outside the app, so the switch mirrors reality on every
 * resume: switching on without access opens Android's notification-access screen and only
 * reads as on once the user actually grants it there.
 */
@Composable
private fun NotificationAccessRow(
    viewModel: NickelViewModel,
    autoAdd: Boolean,
    haptics: HapticFeedback,
    shape: RoundedCornerShape,
) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(hasListenerAccess(context)) }
    // Captured transactions surface as notifications (with a Delete button), so the
    // runtime notification permission is asked for where the feature is turned on.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    fun requestAlertsIfNeeded() {
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val currentAutoAdd by rememberUpdatedState(autoAdd)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val granted = hasListenerAccess(context)
            // Back from granting access after switching on: the tap went to Android's
            // settings instead of the permission prompt, so ask for alerts now, or the
            // Delete-button notification would never show.
            if (granted && !hasAccess && currentAutoAdd) requestAlertsIfNeeded()
            hasAccess = granted
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SettingRow(
        icon = Icons.Rounded.Notifications,
        iconShape = MaterialShapes.Cookie6Sided,
        title = stringResource(R.string.auto_add),
        subtitle = stringResource(if (hasAccess) R.string.auto_add_on else R.string.auto_add_needs_access),
        shape = shape,
        checked = autoAdd && hasAccess,
        onCheckedChange = { enabled ->
            haptics.performHapticFeedback(
                if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
            )
            // Re-query at tap time: hasAccess only refreshes on resume, so an access revoke
            // that happens while the app sits in the foreground (adb, another grant screen)
            // would leave it stale-true and this navigation would never fire.
            hasAccess = hasListenerAccess(context)
            viewModel.setAutoAddNotices(enabled)
            if (enabled && !hasAccess) {
                openNotificationAccess(context)
            } else if (enabled) {
                requestAlertsIfNeeded()
            }
        },
    )
}

private fun hasListenerAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/**
 * Opens the per-app page for Nickel's own listener, one switch away from a grant, instead of
 * dropping the user into the full notification-access list. OEMs that strip the detail
 * activity fall back to the list.
 */
private fun openNotificationAccess(context: Context) {
    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(context, NickelNoticeListener::class.java).flattenToString(),
        )
    runCatching { context.startActivity(detail) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
}

/** Swatches of the live palette, so toggling dynamic color has something to watch. */
@Composable
private fun PaletteRow(shape: RoundedCornerShape) {
    val c = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "palette")
    val paletteTurn = spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "paletteTurn",
    )
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
            ).forEachIndexed { index, (color, polygon) ->
                // The same slow turn as the badges and add button, phase-shifted per swatch
                // so neighbouring shapes don't spin in lockstep. Square, centred in its slot:
                // a shape stretched to a wide slot would wobble and overrun the row as it turns.
                Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .graphicsLayer { rotationZ = index * 47f + paletteTurn.value }
                            .background(color, polygon.toShape())
                    )
                }
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
        val appName = stringResource(R.string.app_name)
        val version = stringResource(R.string.about_version)
        // Split around the placeholder so only the author's name carries the link.
        val (byBefore, byAfter) = stringResource(R.string.about_by, "\u0000").split('\u0000', limit = 2)
            .let { it[0] to it.getOrElse(1) { "" } }
        Text(
            buildAnnotatedString {
                withLink(LinkAnnotation.Url(REPO_URL, linkStyle)) { append(appName) }
                append(version)
                append('\n')
                append(byBefore)
                withLink(LinkAnnotation.Url(AUTHOR_URL, linkStyle)) { append("draumaz") }
                append(byAfter)
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
    // A full-screen window with the card centered inside it, rather than a window that wraps
    // the card. A wrapping window resizes and re-centers on every frame of the card's size
    // morph, a frame or so behind the content, so the text rode upward and jittered for the
    // whole animation. With the window fixed, the card grows around text that holds still.
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = !busy,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        // The window now covers the whole screen, so "outside the card" is this backdrop.
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !busy,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .widthIn(min = 280.dp, max = 560.dp)
                    // Swallow taps on the card itself so they don't reach the backdrop.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
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
                        TransferState.Reading -> BusyContent(stringResource(R.string.import_reading))
                        TransferState.Importing -> BusyContent(stringResource(R.string.import_busy))
                        is TransferState.Failed -> MessageContent(stringResource(R.string.import_failed), stringResource(s.message), onDismiss)
                        is TransferState.Preview -> if (s.plan.transactions.isEmpty()) {
                            MessageContent(
                                stringResource(R.string.already_imported),
                                pluralStringResource(
                                    R.plurals.already_imported_body,
                                    s.plan.duplicates,
                                    s.plan.duplicates,
                                    s.fileName ?: stringResource(R.string.this_file),
                                ),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
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
        val full = localizedDateFormat("yMMMd")
        val short = localizedDateFormat("MMMd")
        when {
            start == end -> start.format(full)
            start.year == end.year -> stringResource(R.string.date_range, start.format(short), end.format(full))
            else -> stringResource(R.string.date_range, start.format(full), end.format(full))
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
            pluralStringResource(R.plurals.import_question, count, count),
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
            stringResource(
                R.string.dot_pair,
                pluralStringResource(R.plurals.expense_count, expenses, expenses),
                pluralStringResource(R.plurals.income_count, incomes, incomes),
            ),
            style = body,
            color = bodyColor,
        )
        val skipped = buildList {
            if (plan.duplicates > 0) add(stringResource(R.string.skip_duplicates, plan.duplicates))
            if (plan.unreadable > 0) add(stringResource(R.string.skip_unreadable, plan.unreadable))
        }
        when (skipped.size) {
            1 -> Text(stringResource(R.string.skipping, skipped[0]), style = body, color = bodyColor)
            2 -> Text(stringResource(R.string.skipping_both, skipped[0], skipped[1]), style = body, color = bodyColor)
        }
        if (plan.newCategories.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.new_categories),
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
                            c.displayLabel(),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Button(onClick = onConfirm, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.import_action)) }
        }
    }
}
