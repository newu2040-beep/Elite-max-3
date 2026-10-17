package com.example.feature.enhancer

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.EliteAdjustmentSlider
import com.example.core.designsystem.EliteBeforeAfterSlider
import com.example.core.designsystem.EliteCobaltEnhancerCard
import com.example.core.designsystem.EliteCustomResolutionControls
import com.example.core.designsystem.EliteIconButton
import com.example.core.designsystem.EliteInbuiltMediaPlayerDialog
import com.example.core.designsystem.ElitePresetSelector
import com.example.core.designsystem.ElitePrimaryButton
import com.example.core.designsystem.EliteTopBar
import com.example.core.media.CropAspect
import com.example.core.media.EnhancePreset
import com.example.core.media.EnhancementSettings
import com.example.core.media.InbuiltPlayerMediaItem
import com.example.core.media.MediaSourceInfo
import com.example.core.media.PhotoOutputFormat
import com.example.core.media.PhotoProcessor
import com.example.core.media.PhotoResolutionMode
import com.example.core.media.VideoFormatOption
import com.example.core.media.VideoProcessor
import com.example.core.media.VideoResolutionPreset
import com.example.ui.theme.LocalEliteColors
import kotlin.math.roundToInt

@Composable
fun VideoEnhancerScreen(
    sourceInfo: MediaSourceInfo?,
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    settings: EnhancementSettings,
    isHevcSupported: Boolean,
    isVp9Supported: Boolean,
    onBack: () -> Unit,
    onPickVideo: () -> Unit,
    onPresetSelected: (EnhancePreset) -> Unit,
    onSettingsChange: ((EnhancementSettings) -> EnhancementSettings) -> Unit,
    onResetSettings: () -> Unit,
    onExportVideo: () -> Unit
) {
    val colors = LocalEliteColors.current
    var isPlayingInlinePreview by remember { mutableStateOf(false) }
    var showInbuiltVideoPlayer by remember { mutableStateOf(false) }
    var showAdvancedPanel by remember { mutableStateOf(true) }
    var showContextMenu by remember { mutableStateOf(false) }

    val availableFormats = remember(isHevcSupported, isVp9Supported) {
        buildList {
            add(VideoFormatOption.MP4_H264_HQ)
            if (isHevcSupported) add(VideoFormatOption.MP4_HEVC_HQ)
            add(VideoFormatOption.MOV_QUICKTIME_HQ)
            if (isVp9Supported) add(VideoFormatOption.WEBM_VP9_HQ)
        }
    }

    val (resolvedOutW, resolvedOutH) = remember(
        sourceInfo,
        settings.videoResolution,
        settings.resolutionScale,
        settings.customVideoWidth,
        settings.customVideoHeight
    ) {
        VideoProcessor.resolveTargetDimensions(
            srcWidth = sourceInfo?.width ?: 1920,
            srcHeight = sourceInfo?.height ?: 1080,
            resolutionPreset = settings.videoResolution,
            scaleFactor = settings.resolutionScale,
            customWidth = settings.customVideoWidth,
            customHeight = settings.customVideoHeight
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Video Enhancer",
                    onBackClick = onBack,
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sourceInfo != null) {
                                EliteIconButton(
                                    icon = Icons.Filled.Fullscreen,
                                    contentDescription = "Open inbuilt video player",
                                    onClick = { showInbuiltVideoPlayer = true },
                                    modifier = Modifier.testTag("video_enhancer_inbuilt_player_button")
                                )
                            }
                            EliteIconButton(
                                icon = Icons.Filled.FolderOpen,
                                contentDescription = "Select video file",
                                onClick = onPickVideo,
                                modifier = Modifier.testTag("video_enhancer_pick_button")
                            )
                            Box {
                                EliteIconButton(
                                    icon = Icons.Filled.MoreVert,
                                    contentDescription = "Video Enhancer menu",
                                    onClick = { showContextMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showContextMenu,
                                    onDismissRequest = { showContextMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Select Video from Gallery / Files") },
                                        onClick = {
                                            showContextMenu = false
                                            onPickVideo()
                                        }
                                    )
                                    if (sourceInfo != null) {
                                        DropdownMenuItem(
                                            text = { Text("Open Inbuilt Video Player") },
                                            onClick = {
                                                showContextMenu = false
                                                showInbuiltVideoPlayer = true
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Reset Adjustments") },
                                        onClick = {
                                            showContextMenu = false
                                            onResetSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = colors.background.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                ElitePrimaryButton(
                    text = if (sourceInfo != null) {
                        "Enhance & Save (${resolvedOutW}×${resolvedOutH} @ ${settings.videoFrameRate}fps)"
                    } else {
                        "Select Video to Enhance"
                    },
                    leadingIcon = Icons.Filled.VideoFile,
                    onClick = {
                        if (sourceInfo != null) onExportVideo() else onPickVideo()
                    },
                    modifier = Modifier.testTag("export_video_button")
                )
            }
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EliteBeforeAfterSlider(
                        beforeBitmap = beforeBitmap,
                        afterBitmap = afterBitmap,
                        videoUri = sourceInfo?.uri,
                        isVideo = true,
                        isPlaying = isPlayingInlinePreview,
                        onTogglePlay = { isPlayingInlinePreview = !isPlayingInlinePreview },
                        onSelectMediaClick = onPickVideo
                    )

                    if (sourceInfo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfacePure.copy(alpha = 0.90f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sourceInfo.filename,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${sourceInfo.resolutionLabel} → ${resolvedOutW}×${resolvedOutH}  •  ${settings.videoFrameRate} FPS  •  ${(settings.targetVideoBitrateKbps / 1000f).roundToInt()} Mbps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }

                            Surface(
                                onClick = { showInbuiltVideoPlayer = true },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.cobaltPrimary,
                                modifier = Modifier.testTag("open_inbuilt_video_player_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Inbuilt Player",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Player",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Enhancement Presets: Natural, Clear, Low Light, Custom
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enhancement Preset",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        TextButton(onClick = onResetSettings) {
                            Text("Reset", color = colors.cobaltPrimary)
                        }
                    }
                    ElitePresetSelector(
                        options = EnhancePreset.entries,
                        selectedOption = settings.preset,
                        labelProvider = { it.label },
                        onOptionSelected = onPresetSelected
                    )
                }
            }

            // Cobalt-Blue 2x2 Live Metrics Card
            item {
                EliteCobaltEnhancerCard(
                    filename = sourceInfo?.filename ?: "Select Video from Gallery",
                    dateAndDurationLabel = if (sourceInfo != null) {
                        "${sourceInfo.formattedDate}  •  ${sourceInfo.formattedDuration}  •  Clarity ${settings.clarityScore}"
                    } else {
                        "Tap folder icon to load a video"
                    },
                    thumbnailBitmap = afterBitmap ?: beforeBitmap,
                    settings = settings,
                    onActionIconClick = onPickVideo
                )
            }

            // High-Quality Video Format, Custom Resolution, FPS & Bitrate Section
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedPanel = !showAdvancedPanel }
                                .testTag("toggle_advanced_video_enhance"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Resolution, FPS, Bitrate & HQ Format",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${settings.videoFormat.label}  •  ${resolvedOutW}×${resolvedOutH}  •  ${settings.videoFrameRate} FPS  •  ${(settings.targetVideoBitrateKbps / 1000f).roundToInt()} Mbps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = if (showAdvancedPanel) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle custom video controls",
                                tint = colors.cobaltPrimary
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedPanel) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. High-Quality Video Format
                                Text(
                                    text = "High-Quality Video Format & Codec",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = availableFormats,
                                    selectedOption = settings.videoFormat,
                                    labelProvider = { it.label },
                                    onOptionSelected = { fmt ->
                                        onSettingsChange { it.copy(videoFormat = fmt) }
                                    }
                                )

                                // 2. Target Video Resolution (Preset + Custom Width x Height)
                                Text(
                                    text = "Target Video Resolution (${resolvedOutW} × ${resolvedOutH})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = VideoResolutionPreset.entries,
                                    selectedOption = settings.videoResolution,
                                    labelProvider = { it.label },
                                    onOptionSelected = { res ->
                                        onSettingsChange {
                                            it.copy(
                                                videoResolution = res,
                                                resolutionScale = res.scaleFactor
                                            )
                                        }
                                    }
                                )

                                AnimatedVisibility(visible = settings.videoResolution == VideoResolutionPreset.CUSTOM) {
                                    EliteCustomResolutionControls(
                                        width = settings.customVideoWidth,
                                        height = settings.customVideoHeight,
                                        sourceWidth = sourceInfo?.width ?: 1920,
                                        sourceHeight = sourceInfo?.height ?: 1080,
                                        keepAspectRatio = settings.preserveAspectRatio,
                                        isVideo = true,
                                        onDimensionsChanged = { w, h ->
                                            onSettingsChange {
                                                it.copy(
                                                    videoResolution = VideoResolutionPreset.CUSTOM,
                                                    customVideoWidth = w,
                                                    customVideoHeight = h
                                                )
                                            }
                                        },
                                        onKeepAspectChanged = { keep ->
                                            onSettingsChange { it.copy(preserveAspectRatio = keep) }
                                        }
                                    )
                                }

                                // 3. Custom Frame Rate (FPS Presets + Fine Slider)
                                Text(
                                    text = "Frame Rate (${settings.videoFrameRate} FPS)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = listOf(24, 30, 50, 60, 90, 120),
                                    selectedOption = settings.videoFrameRate,
                                    labelProvider = { "${it} FPS" },
                                    onOptionSelected = { fps ->
                                        onSettingsChange { it.copy(videoFrameRate = fps) }
                                    }
                                )

                                EliteAdjustmentSlider(
                                    label = "Custom Frame Rate (FPS)",
                                    value = settings.videoFrameRate.toFloat(),
                                    valueRange = 15f..120f,
                                    icon = Icons.Filled.Videocam,
                                    valueFormatter = { "${it.roundToInt()} FPS" },
                                    onValueChange = { fps ->
                                        onSettingsChange { it.copy(videoFrameRate = fps.roundToInt()) }
                                    }
                                )

                                // 4. Custom Bitrate (Quick Studio Bitrates + Continuous Slider)
                                Text(
                                    text = "Target Video Bitrate (${settings.targetVideoBitrateKbps} kbps / ${(settings.targetVideoBitrateKbps / 1000f).roundToInt()} Mbps)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = listOf(4_000, 8_000, 14_000, 24_000, 36_000),
                                    selectedOption = settings.targetVideoBitrateKbps,
                                    labelProvider = { "${it / 1000} Mbps" },
                                    onOptionSelected = { kbps ->
                                        onSettingsChange { it.copy(targetVideoBitrateKbps = kbps) }
                                    }
                                )

                                EliteAdjustmentSlider(
                                    label = "Custom Video Bitrate",
                                    value = settings.targetVideoBitrateKbps.toFloat(),
                                    valueRange = 1000f..40000f,
                                    icon = Icons.Filled.Speed,
                                    valueFormatter = { "${it.roundToInt()} kbps" },
                                    onValueChange = { kbps ->
                                        onSettingsChange { it.copy(targetVideoBitrateKbps = kbps.roundToInt()) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Primary Optical Adjustment Sliders
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Real-Time Optical & Color Adjustments",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    EliteAdjustmentSlider(
                        label = "Sharpness",
                        value = settings.sharpness,
                        valueRange = 0f..1f,
                        icon = Icons.Filled.AutoAwesome,
                        valueFormatter = { "+${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, sharpness = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Brightness",
                        value = settings.brightness,
                        valueRange = -0.4f..0.4f,
                        icon = Icons.Filled.Brightness6,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, brightness = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Contrast",
                        value = settings.contrast,
                        valueRange = -0.4f..0.6f,
                        icon = Icons.Filled.Contrast,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, contrast = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Saturation",
                        value = settings.saturation,
                        valueRange = -0.5f..0.6f,
                        icon = Icons.Filled.Palette,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, saturation = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Color Temperature",
                        value = settings.temperature,
                        valueRange = -0.5f..0.5f,
                        icon = Icons.Filled.Thermostat,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, temperature = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Noise Reduction",
                        value = settings.noiseReduction,
                        valueRange = 0f..1f,
                        icon = Icons.Filled.BlurOn,
                        valueFormatter = { "-${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, noiseReduction = v) }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showInbuiltVideoPlayer && sourceInfo != null) {
        EliteInbuiltMediaPlayerDialog(
            mediaItem = InbuiltPlayerMediaItem(
                uri = sourceInfo.uri,
                title = sourceInfo.filename,
                isVideo = true,
                resolutionLabel = "${sourceInfo.resolutionLabel} → ${resolvedOutW}×${resolvedOutH}",
                sizeBytes = sourceInfo.sizeBytes,
                durationMs = sourceInfo.durationMs,
                formatSummary = "${settings.videoFormat.label} • ${settings.videoFrameRate} FPS"
            ),
            beforeBitmap = beforeBitmap,
            afterBitmap = afterBitmap,
            onDismiss = { showInbuiltVideoPlayer = false }
        )
    }
}

@Composable
fun PhotoEnhancerScreen(
    sourceInfo: MediaSourceInfo?,
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    settings: EnhancementSettings,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onPresetSelected: (EnhancePreset) -> Unit,
    onSettingsChange: ((EnhancementSettings) -> EnhancementSettings) -> Unit,
    onResetSettings: () -> Unit,
    onExportPhoto: () -> Unit
) {
    val colors = LocalEliteColors.current
    var showInbuiltPhotoPreview by remember { mutableStateOf(false) }

    val srcW = sourceInfo?.width ?: (beforeBitmap?.width ?: 1920)
    val srcH = sourceInfo?.height ?: (beforeBitmap?.height ?: 1080)
    val (outW, outH) = remember(
        srcW,
        srcH,
        settings.photoResolutionMode,
        settings.resolutionScale,
        settings.customPhotoWidth,
        settings.customPhotoHeight,
        settings.preserveAspectRatio
    ) {
        PhotoProcessor.resolvePhotoTargetDimensions(
            srcWidth = srcW,
            srcHeight = srcH,
            mode = settings.photoResolutionMode,
            scaleFactor = settings.resolutionScale,
            customWidth = settings.customPhotoWidth,
            customHeight = settings.customPhotoHeight,
            preserveAspect = settings.preserveAspectRatio
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Photo Enhancer",
                    onBackClick = onBack,
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sourceInfo != null || afterBitmap != null) {
                                EliteIconButton(
                                    icon = Icons.Filled.ZoomOutMap,
                                    contentDescription = "Open inbuilt photo preview",
                                    onClick = { showInbuiltPhotoPreview = true },
                                    modifier = Modifier.testTag("photo_enhancer_inbuilt_preview_button")
                                )
                            }
                            EliteIconButton(
                                icon = Icons.Filled.FolderOpen,
                                contentDescription = "Select photo",
                                onClick = onPickPhoto,
                                modifier = Modifier.testTag("photo_enhancer_pick_button")
                            )
                            EliteIconButton(
                                icon = Icons.Filled.Refresh,
                                contentDescription = "Reset adjustments",
                                onClick = onResetSettings,
                                modifier = Modifier.testTag("photo_enhancer_reset_button")
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = colors.background.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                ElitePrimaryButton(
                    text = if (sourceInfo != null) {
                        "Enhance & Save (${outW} × ${outH})"
                    } else {
                        "Select Photo to Enhance"
                    },
                    onClick = {
                        if (sourceInfo != null) onExportPhoto() else onPickPhoto()
                    },
                    modifier = Modifier.testTag("export_photo_button")
                )
            }
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EliteBeforeAfterSlider(
                        beforeBitmap = beforeBitmap,
                        afterBitmap = afterBitmap,
                        isVideo = false,
                        onSelectMediaClick = onPickPhoto
                    )

                    if (sourceInfo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfacePure.copy(alpha = 0.88f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sourceInfo.filename,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Original: ${srcW} × ${srcH}  →  Output: ${outW} × ${outH}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Surface(
                                onClick = { showInbuiltPhotoPreview = true },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.cobaltPrimary,
                                modifier = Modifier.testTag("open_inbuilt_photo_preview_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ZoomOutMap,
                                        contentDescription = "Full Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Preview",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enhancement Presets",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    ElitePresetSelector(
                        options = EnhancePreset.entries,
                        selectedOption = settings.preset,
                        labelProvider = { it.label },
                        onOptionSelected = onPresetSelected
                    )
                }
            }

            // High-Quality Photo Format, Custom Resolution, Quality & Crop
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.surfacePure.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "High-Quality Photo Format & Custom Resolution",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )

                        ElitePresetSelector(
                            options = PhotoOutputFormat.entries,
                            selectedOption = settings.photoFormat,
                            labelProvider = { it.label },
                            onOptionSelected = { fmt ->
                                onSettingsChange { it.copy(photoFormat = fmt) }
                            }
                        )

                        if (!settings.photoFormat.isLossless) {
                            EliteAdjustmentSlider(
                                label = "Export Quality",
                                value = settings.photoQuality.toFloat(),
                                valueRange = 60f..100f,
                                icon = Icons.Filled.HighQuality,
                                valueFormatter = { "${it.roundToInt()}%" },
                                onValueChange = { q ->
                                    onSettingsChange { it.copy(photoQuality = q.roundToInt()) }
                                }
                            )
                        }

                        Text(
                            text = "Output Resolution (${outW} × ${outH} px)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        ElitePresetSelector(
                            options = PhotoResolutionMode.entries,
                            selectedOption = settings.photoResolutionMode,
                            labelProvider = { it.label },
                            onOptionSelected = { mode ->
                                onSettingsChange {
                                    it.copy(
                                        photoResolutionMode = mode,
                                        resolutionScale = mode.scaleFactor
                                    )
                                }
                            }
                        )

                        AnimatedVisibility(visible = settings.photoResolutionMode == PhotoResolutionMode.CUSTOM) {
                            EliteCustomResolutionControls(
                                width = settings.customPhotoWidth,
                                height = settings.customPhotoHeight,
                                sourceWidth = srcW,
                                sourceHeight = srcH,
                                keepAspectRatio = settings.preserveAspectRatio,
                                isVideo = false,
                                onDimensionsChanged = { w, h ->
                                    onSettingsChange {
                                        it.copy(
                                            photoResolutionMode = PhotoResolutionMode.CUSTOM,
                                            customPhotoWidth = w,
                                            customPhotoHeight = h
                                        )
                                    }
                                },
                                onKeepAspectChanged = { keep ->
                                    onSettingsChange { it.copy(preserveAspectRatio = keep) }
                                }
                            )
                        }

                        Text(
                            text = "Crop Aspect Ratio",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        ElitePresetSelector(
                            options = CropAspect.entries,
                            selectedOption = settings.cropAspect,
                            labelProvider = { it.label },
                            onOptionSelected = { crop ->
                                onSettingsChange { it.copy(cropAspect = crop) }
                            }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Real-Time Tone & Clarity Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    EliteAdjustmentSlider(
                        label = "Sharpness",
                        value = settings.sharpness,
                        valueRange = 0f..1f,
                        icon = Icons.Filled.AutoAwesome,
                        valueFormatter = { "+${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, sharpness = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Brightness",
                        value = settings.brightness,
                        valueRange = -0.4f..0.4f,
                        icon = Icons.Filled.Brightness6,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, brightness = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Contrast",
                        value = settings.contrast,
                        valueRange = -0.4f..0.6f,
                        icon = Icons.Filled.Contrast,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, contrast = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Saturation",
                        value = settings.saturation,
                        valueRange = -0.5f..0.6f,
                        icon = Icons.Filled.Palette,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, saturation = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "White Balance",
                        value = settings.temperature,
                        valueRange = -0.5f..0.5f,
                        icon = Icons.Filled.Thermostat,
                        valueFormatter = { "${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, temperature = v) }
                        }
                    )

                    EliteAdjustmentSlider(
                        label = "Noise Reduction",
                        value = settings.noiseReduction,
                        valueRange = 0f..1f,
                        icon = Icons.Filled.BlurOn,
                        valueFormatter = { "-${(it * 100).roundToInt()}%" },
                        onValueChange = { v ->
                            onSettingsChange { it.copy(preset = EnhancePreset.CUSTOM, noiseReduction = v) }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showInbuiltPhotoPreview && (sourceInfo != null || afterBitmap != null)) {
        EliteInbuiltMediaPlayerDialog(
            mediaItem = InbuiltPlayerMediaItem(
                uri = sourceInfo?.uri ?: android.net.Uri.EMPTY,
                title = sourceInfo?.filename ?: "Enhanced Photo Preview",
                isVideo = false,
                resolutionLabel = "${outW} × ${outH}",
                sizeBytes = sourceInfo?.sizeBytes ?: 0L,
                formatSummary = "${settings.photoFormat.label} (${settings.photoQuality}%)"
            ),
            beforeBitmap = beforeBitmap,
            afterBitmap = afterBitmap,
            onDismiss = { showInbuiltPhotoPreview = false }
        )
    }
}
