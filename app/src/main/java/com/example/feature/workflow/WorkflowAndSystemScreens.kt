package com.example.feature.workflow

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.EliteAdjustmentSlider
import com.example.core.designsystem.EliteBottomNavigation
import com.example.core.designsystem.EliteBottomTab
import com.example.core.designsystem.EliteEditorialHeader
import com.example.core.designsystem.EliteEmptyState
import com.example.core.designsystem.EliteFileRow
import com.example.core.designsystem.EliteInbuiltMediaPlayerDialog
import com.example.core.designsystem.ElitePresetSelector
import com.example.core.designsystem.ElitePrimaryButton
import com.example.core.designsystem.EliteProcessingIndicator
import com.example.core.designsystem.EliteSizeComparison
import com.example.core.designsystem.EliteTopBar
import com.example.core.media.InbuiltPlayerMediaItem
import com.example.core.media.ProcessingProgress
import com.example.core.media.formatFileSize
import com.example.core.storage.ElitePreferencesState
import com.example.core.storage.MediaJobEntity
import com.example.core.storage.MediaOperationType
import com.example.ui.theme.LocalEliteColors
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProcessingScreen(
    progress: ProcessingProgress,
    previewBitmap: Bitmap?,
    onCancelProcessing: () -> Unit,
    onReturnToEditor: () -> Unit
) {
    val colors = LocalEliteColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = if (progress.isBatch) {
                        "Processing (${progress.batchIndex}/${progress.batchCount})"
                    } else {
                        "Real-Time Processing"
                    },
                    onBackClick = if (!progress.isRunning) onReturnToEditor else null
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(colors.surfaceTinted),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Media being processed",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfacePure.copy(alpha = 0.90f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = progress.filename.ifEmpty { "Active Media Stream" },
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                EliteProcessingIndicator(
                    progressFraction = progress.progressFraction,
                    stageLabel = progress.stageLabel,
                    processedItems = progress.processedItems,
                    totalItems = progress.totalItems
                )

                if (progress.errorMessage != null || progress.wasCanceled) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surfacePure,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (progress.wasCanceled) "Processing Canceled" else "Processing Interrupted",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progress.errorMessage
                                    ?: "Your original file was preserved unchanged and temporary buffers were removed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (progress.isRunning) {
                    OutlinedButton(
                        onClick = onCancelProcessing,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("cancel_processing_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Processing")
                    }
                } else {
                    ElitePrimaryButton(
                        text = "Return to Editor",
                        onClick = onReturnToEditor,
                        modifier = Modifier.testTag("return_from_processing_button")
                    )
                }
            }
        }
    }
}

@Composable
fun ExportResultScreen(
    job: MediaJobEntity?,
    previewBitmap: Bitmap?,
    onOpenFile: (MediaJobEntity) -> Unit,
    onShareFile: (MediaJobEntity) -> Unit,
    onProcessAnother: () -> Unit,
    onBackToHome: () -> Unit
) {
    val colors = LocalEliteColors.current
    var showInbuiltPlayer by remember { mutableStateOf(false) }

    val isVideoJob = job?.operationType?.contains("VIDEO") == true

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Export Result",
                    onBackClick = onBackToHome
                )
            }
        }
    ) { innerPadding ->
        if (job == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EliteEmptyState(
                    title = "No Recent Export",
                    subtitle = "Select a photo or video to enhance or compress and save directly into your Gallery.",
                    actionLabel = "Back to Home",
                    onActionClick = onBackToHome
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.mintBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Saved to Gallery",
                                    tint = colors.mintText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Saved into Device Gallery",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.mintText
                                )
                            }
                        }
                    }
                }

                item {
                    EliteEditorialHeader(
                        lightLine = "Export",
                        boldLine = "Complete",
                        subtitleLine1 = job.filename,
                        subtitleLine2 = "Resolution: ${job.outputDimensions}  •  ${job.settingsSummary}"
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(226.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(colors.surfaceTinted)
                            .clickable { showInbuiltPlayer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Exported media preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (job.outputUri.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(job.outputUri),
                                contentDescription = "Exported media preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Center Play / Zoom Trigger
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f))
                                .clickable { showInbuiltPlayer = true }
                                .testTag("result_inbuilt_player_trigger"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVideoJob) Icons.Filled.PlayArrow else Icons.Filled.ZoomOutMap,
                                contentDescription = if (isVideoJob) "Play in inbuilt player" else "Open inbuilt photo preview",
                                tint = colors.cobaltPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cobaltPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isVideoJob) "Tap to Play in Inbuilt Player" else "Tap for Full Zoom Preview",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                item {
                    EliteSizeComparison(
                        originalSizeBytes = job.sourceSizeBytes,
                        targetSizeBytes = job.outputSizeBytes,
                        targetLabel = "Actual Output"
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showInbuiltPlayer = true },
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(54.dp)
                                .testTag("result_open_file_button")
                        ) {
                            Icon(
                                imageVector = if (isVideoJob) Icons.Filled.PlayCircle else Icons.Filled.ZoomOutMap,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isVideoJob) "Inbuilt Player" else "Photo Preview")
                        }

                        OutlinedButton(
                            onClick = { onShareFile(job) },
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(54.dp)
                                .testTag("result_share_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ElitePrimaryButton(
                            text = "Back to Home",
                            onClick = onBackToHome,
                            modifier = Modifier.testTag("result_back_home_button")
                        )
                        TextButton(
                            onClick = onProcessAnother,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("result_process_another_button")
                        ) {
                            Text(
                                text = "Process Another File",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.cobaltPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInbuiltPlayer && job != null && job.outputUri.isNotEmpty()) {
        EliteInbuiltMediaPlayerDialog(
            mediaItem = InbuiltPlayerMediaItem(
                uri = Uri.parse(job.outputUri),
                title = job.filename,
                isVideo = isVideoJob,
                resolutionLabel = job.outputDimensions,
                sizeBytes = job.outputSizeBytes,
                durationMs = job.durationMs,
                formatSummary = job.settingsSummary
            ),
            afterBitmap = if (!isVideoJob) previewBitmap else null,
            onDismiss = { showInbuiltPlayer = false },
            onShare = { onShareFile(job) },
            onOpenExternal = { onOpenFile(job) }
        )
    }
}

@Composable
fun HistoryScreen(
    jobs: List<MediaJobEntity>,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onOpenFile: (MediaJobEntity) -> Unit,
    onShareFile: (MediaJobEntity) -> Unit,
    onDeleteJob: (MediaJobEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val colors = LocalEliteColors.current
    var filterTab by remember { mutableStateOf("All") }
    var activePreviewJob by remember { mutableStateOf<MediaJobEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.US) }

    val filtered = remember(jobs, filterTab) {
        when (filterTab) {
            "Videos" -> jobs.filter { it.operationType.contains("VIDEO") }
            "Photos" -> jobs.filter { it.operationType.contains("PHOTO") }
            else -> jobs
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Processing History",
                    onBackClick = onNavigateHome,
                    trailingContent = {
                        if (jobs.isNotEmpty()) {
                            TextButton(
                                onClick = onClearAll,
                                modifier = Modifier.testTag("clear_all_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Clear history",
                                    tint = colors.cobaltPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", color = colors.cobaltPrimary)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            EliteBottomNavigation(
                currentTab = EliteBottomTab.HISTORY,
                onTabSelected = { tab ->
                    when (tab) {
                        EliteBottomTab.HOME -> onNavigateHome()
                        EliteBottomTab.HISTORY -> {}
                        EliteBottomTab.SETTINGS -> onNavigateSettings()
                    }
                },
                onCenterStudioClick = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElitePresetSelector(
                    options = listOf("All", "Videos", "Photos"),
                    selectedOption = filterTab,
                    labelProvider = { it },
                    onOptionSelected = { filterTab = it }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EliteEmptyState(
                        title = "No History Entries",
                        subtitle = "Completed and canceled on-device enhancement or compression jobs saved to your Gallery appear here.",
                        icon = Icons.Outlined.History,
                        actionLabel = "Go to Tools",
                        onActionClick = onNavigateHome
                    )
                }
            } else {
                items(filtered, key = { it.id }) { job ->
                    val opType = runCatching { MediaOperationType.valueOf(job.operationType) }
                        .getOrDefault(MediaOperationType.PHOTO_ENHANCE)
                    val dateStr = dateFormat.format(Date(job.completedAt))
                    val savings = job.savingsPercent
                    val badge = if (savings != null && savings > 0) "-$savings%" else job.status

                    EliteFileRow(
                        title = job.filename,
                        subtitle = "${opType.displayLabel}  •  ${formatFileSize(job.outputSizeBytes)}  •  $dateStr",
                        mediaUri = job.outputUri,
                        durationLabel = job.outputDimensions,
                        savingsBadge = badge,
                        onClick = {
                            if (job.outputUri.isNotEmpty()) {
                                activePreviewJob = job
                            } else {
                                onOpenFile(job)
                            }
                        },
                        onOpenClick = {
                            if (job.outputUri.isNotEmpty()) {
                                activePreviewJob = job
                            } else {
                                onOpenFile(job)
                            }
                        },
                        onShareClick = { onShareFile(job) },
                        onDeleteClick = { onDeleteJob(job) }
                    )
                }
            }
        }
    }

    val previewJob = activePreviewJob
    if (previewJob != null && previewJob.outputUri.isNotEmpty()) {
        val isVid = previewJob.operationType.contains("VIDEO")
        EliteInbuiltMediaPlayerDialog(
            mediaItem = InbuiltPlayerMediaItem(
                uri = Uri.parse(previewJob.outputUri),
                title = previewJob.filename,
                isVideo = isVid,
                resolutionLabel = previewJob.outputDimensions,
                sizeBytes = previewJob.outputSizeBytes,
                durationMs = previewJob.durationMs,
                formatSummary = previewJob.settingsSummary
            ),
            onDismiss = { activePreviewJob = null },
            onShare = { onShareFile(previewJob) },
            onOpenExternal = { onOpenFile(previewJob) }
        )
    }
}

@Composable
fun SettingsScreen(
    preferences: ElitePreferencesState,
    availableStorageText: String,
    historyCount: Int,
    onNavigateHome: () -> Unit,
    onNavigateHistory: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDefaultPhotoQualityChanged: (Int) -> Unit,
    onDefaultVideoPresetChanged: (String) -> Unit,
    onPreserveMetadataChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    val colors = LocalEliteColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Settings",
                    onBackClick = onNavigateHome
                )
            }
        },
        bottomBar = {
            EliteBottomNavigation(
                currentTab = EliteBottomTab.SETTINGS,
                onTabSelected = { tab ->
                    when (tab) {
                        EliteBottomTab.HOME -> onNavigateHome()
                        EliteBottomTab.HISTORY -> onNavigateHistory()
                        EliteBottomTab.SETTINGS -> {}
                    }
                },
                onCenterStudioClick = onNavigateHistory
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Pale icy-blue default palette or solid charcoal dark theme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        ElitePresetSelector(
                            options = ThemeMode.entries,
                            selectedOption = preferences.themeMode,
                            labelProvider = {
                                when (it) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System"
                                }
                            },
                            onOptionSelected = onThemeModeChanged
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Output & Gallery Save Defaults",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )

                        EliteAdjustmentSlider(
                            label = "Default Photo Quality",
                            value = preferences.defaultPhotoQuality.toFloat(),
                            valueRange = 40f..100f,
                            icon = Icons.Filled.HighQuality,
                            valueFormatter = { "${it.roundToInt()}%" },
                            onValueChange = { onDefaultPhotoQualityChanged(it.roundToInt()) }
                        )

                        Text(
                            text = "Default Video Compression Preset",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        ElitePresetSelector(
                            options = listOf("Smaller", "Balanced", "Quality"),
                            selectedOption = preferences.defaultVideoPreset,
                            labelProvider = { it },
                            onOptionSelected = onDefaultVideoPresetChanged
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Preserve EXIF & Timestamps",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Keep capture orientation & metadata by default",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = preferences.preserveMetadata,
                                onCheckedChange = onPreserveMetadataChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.cobaltPrimary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = colors.cobaltPrimary
                            )
                            Column {
                                Text(
                                    text = "Gallery Export Location",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "$availableStorageText  •  Pictures/EliteMax & Movies/EliteMax",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Local History Records ($historyCount items)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary
                            )
                            TextButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("settings_clear_history_button")
                            ) {
                                Text("Clear History", color = colors.cobaltPrimary)
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.cobaltPrimary
                            )
                            Text(
                                text = "Real-Time On-Device Privacy Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = "All photo and video enhancement and compression run 100% locally in real time using Android MediaCodec and Bitmap hardware pipelines. Completed files are saved directly into your device Gallery (Pictures/EliteMax and Movies/EliteMax).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ELITEMAX v1.2.0 • Real-Time Native Android Media Studio",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
