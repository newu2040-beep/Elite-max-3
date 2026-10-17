package com.example.feature.compressor

import android.graphics.Bitmap
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.designsystem.EliteAdjustmentSlider
import com.example.core.designsystem.EliteCustomResolutionControls
import com.example.core.designsystem.EliteEditorialHeader
import com.example.core.designsystem.EliteIconButton
import com.example.core.designsystem.EliteInbuiltMediaPlayerDialog
import com.example.core.designsystem.ElitePresetSelector
import com.example.core.designsystem.ElitePrimaryButton
import com.example.core.designsystem.EliteSizeComparison
import com.example.core.designsystem.EliteTopBar
import com.example.core.media.CompressionPreset
import com.example.core.media.InbuiltPlayerMediaItem
import com.example.core.media.MediaSourceInfo
import com.example.core.media.PhotoCompressionSettings
import com.example.core.media.PhotoOutputFormat
import com.example.core.media.PhotoProcessor
import com.example.core.media.PhotoResolutionMode
import com.example.core.media.VideoCompressionSettings
import com.example.core.media.VideoFormatOption
import com.example.core.media.VideoProcessor
import com.example.core.media.VideoResolutionPreset
import com.example.ui.theme.EliteWarningBg
import com.example.ui.theme.EliteWarningText
import com.example.ui.theme.LocalEliteColors
import kotlin.math.roundToInt

@Composable
fun VideoCompressorScreen(
    sourceInfo: MediaSourceInfo?,
    videoThumbnailBitmap: Bitmap?,
    settings: VideoCompressionSettings,
    estimatedBytes: Long,
    isHevcSupported: Boolean,
    isVp9Supported: Boolean,
    onBack: () -> Unit,
    onPickVideo: () -> Unit,
    onPresetSelected: (CompressionPreset) -> Unit,
    onSettingsChange: ((VideoCompressionSettings) -> VideoCompressionSettings) -> Unit,
    onCompressVideo: () -> Unit
) {
    val colors = LocalEliteColors.current
    var showAdvanced by remember { mutableStateOf(true) }
    var isPlayingInline by remember { mutableStateOf(false) }
    var showInbuiltVideoPlayer by remember { mutableStateOf(false) }

    val origSize = sourceInfo?.sizeBytes ?: 0L
    val estSize = if (sourceInfo != null) estimatedBytes else 0L

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
        settings.resolutionPreset,
        settings.resolutionScale,
        settings.customWidth,
        settings.customHeight
    ) {
        VideoProcessor.resolveTargetDimensions(
            srcWidth = sourceInfo?.width ?: 1920,
            srcHeight = sourceInfo?.height ?: 1080,
            resolutionPreset = settings.resolutionPreset,
            scaleFactor = settings.resolutionScale,
            customWidth = settings.customWidth,
            customHeight = settings.customHeight
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Compress Video",
                    onBackClick = onBack,
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sourceInfo != null) {
                                EliteIconButton(
                                    icon = Icons.Filled.Fullscreen,
                                    contentDescription = "Open inbuilt video player",
                                    onClick = { showInbuiltVideoPlayer = true },
                                    modifier = Modifier.testTag("video_compressor_inbuilt_player_button")
                                )
                            }
                            EliteIconButton(
                                icon = Icons.Filled.FolderOpen,
                                contentDescription = "Select video to compress",
                                onClick = onPickVideo,
                                modifier = Modifier.testTag("video_compressor_pick_button")
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
                        "Compress & Save (${resolvedOutW}×${resolvedOutH} @ ${settings.frameRate}fps)"
                    } else {
                        "Select Video to Compress"
                    },
                    leadingIcon = Icons.Filled.Compress,
                    onClick = {
                        if (sourceInfo != null) onCompressVideo() else onPickVideo()
                    },
                    modifier = Modifier.testTag("compress_video_button")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EliteEditorialHeader(
                    lightLine = "Compression",
                    boldLine = "Result",
                    subtitleLine1 = "Smaller size. Same quality.",
                    subtitleLine2 = "On your device."
                )
            }

            // Real Source Video Preview or Tap-to-Select Surface
            item {
                if (sourceInfo == null) {
                    Surface(
                        onClick = onPickVideo,
                        shape = RoundedCornerShape(30.dp),
                        color = colors.surfacePure,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.5.dp, colors.cobaltPrimary.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceTinted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoFile,
                                    contentDescription = "Select video",
                                    tint = colors.cobaltPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to Select a Video from Gallery or Files",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Real-time size calculation & hardware encoding",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(232.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(colors.surfaceTinted)
                    ) {
                        if (isPlayingInline) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(sourceInfo.uri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (videoThumbnailBitmap != null) {
                            Image(
                                bitmap = videoThumbnailBitmap.asImageBitmap(),
                                contentDescription = "Source video frame",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, colors.background.copy(alpha = 0.92f))
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .align(Alignment.Center)
                                .shadow(10.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f))
                                .clickable { isPlayingInline = !isPlayingInline },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingInline) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Toggle inline video playback",
                                tint = colors.cobaltPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfacePure.copy(alpha = 0.90f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${sourceInfo.filename}  •  ${sourceInfo.resolutionLabel} → ${resolvedOutW}×${resolvedOutH}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            onClick = { showInbuiltVideoPlayer = true },
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cobaltPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .testTag("compressor_open_inbuilt_player_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = "Fullscreen Inbuilt Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Inbuilt Player",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Real Size Comparison Region
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EliteSizeComparison(
                        originalSizeBytes = origSize,
                        targetSizeBytes = estSize,
                        targetLabel = "Estimated"
                    )
                    Text(
                        text = "Calculated in real time from source duration, target bitrate, FPS, and codec efficiency. Output saves directly to your Gallery.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            // Compression Presets: Smaller, Balanced, Quality
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Compression Preset",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    ElitePresetSelector(
                        options = CompressionPreset.entries,
                        selectedOption = settings.preset,
                        labelProvider = { it.label },
                        onOptionSelected = onPresetSelected
                    )
                }
            }

            // High-Quality Formats, Custom Resolution, Custom FPS & Bitrate
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
                                .clickable { showAdvanced = !showAdvanced }
                                .testTag("toggle_advanced_video_compress"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Resolution, FPS, Bitrate & Format",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${settings.formatOption.label}  •  ${resolvedOutW}×${resolvedOutH}  •  ${settings.frameRate} fps  •  ${settings.targetBitrateKbps} kbps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Expand advanced settings",
                                tint = colors.cobaltPrimary
                            )
                        }

                        AnimatedVisibility(visible = showAdvanced) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "High-Quality Video Format & Hardware Codec",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = availableFormats,
                                    selectedOption = settings.formatOption,
                                    labelProvider = { it.label },
                                    onOptionSelected = { fmt ->
                                        onSettingsChange { it.copy(formatOption = fmt) }
                                    }
                                )

                                Text(
                                    text = "Target Video Resolution (${resolvedOutW} × ${resolvedOutH})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = VideoResolutionPreset.entries,
                                    selectedOption = settings.resolutionPreset,
                                    labelProvider = { it.label },
                                    onOptionSelected = { res ->
                                        onSettingsChange {
                                            it.copy(
                                                resolutionPreset = res,
                                                resolutionScale = res.scaleFactor
                                            )
                                        }
                                    }
                                )

                                AnimatedVisibility(visible = settings.resolutionPreset == VideoResolutionPreset.CUSTOM) {
                                    EliteCustomResolutionControls(
                                        width = settings.customWidth,
                                        height = settings.customHeight,
                                        sourceWidth = sourceInfo?.width ?: 1920,
                                        sourceHeight = sourceInfo?.height ?: 1080,
                                        keepAspectRatio = settings.keepAspectRatio,
                                        isVideo = true,
                                        onDimensionsChanged = { w, h ->
                                            onSettingsChange {
                                                it.copy(
                                                    resolutionPreset = VideoResolutionPreset.CUSTOM,
                                                    customWidth = w,
                                                    customHeight = h
                                                )
                                            }
                                        },
                                        onKeepAspectChanged = { keep ->
                                            onSettingsChange { it.copy(keepAspectRatio = keep) }
                                        }
                                    )
                                }

                                Text(
                                    text = "Frame Rate (${settings.frameRate} FPS)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = listOf(24, 30, 50, 60, 90, 120),
                                    selectedOption = settings.frameRate,
                                    labelProvider = { "$it FPS" },
                                    onOptionSelected = { fps ->
                                        onSettingsChange { it.copy(frameRate = fps) }
                                    }
                                )

                                EliteAdjustmentSlider(
                                    label = "Custom Frame Rate (FPS)",
                                    value = settings.frameRate.toFloat(),
                                    valueRange = 15f..120f,
                                    icon = Icons.Filled.Videocam,
                                    valueFormatter = { "${it.roundToInt()} FPS" },
                                    onValueChange = { fps ->
                                        onSettingsChange { it.copy(frameRate = fps.roundToInt()) }
                                    }
                                )

                                Text(
                                    text = "Target Video Bitrate (${settings.targetBitrateKbps} kbps)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                ElitePresetSelector(
                                    options = listOf(1200, 2500, 4500, 8000, 14000),
                                    selectedOption = settings.targetBitrateKbps,
                                    labelProvider = { "${it / 1000f} Mbps" },
                                    onOptionSelected = { kbps ->
                                        onSettingsChange { it.copy(targetBitrateKbps = kbps) }
                                    }
                                )

                                EliteAdjustmentSlider(
                                    label = "Custom Video Bitrate",
                                    value = settings.targetBitrateKbps.toFloat(),
                                    valueRange = 500f..24000f,
                                    icon = Icons.Filled.Speed,
                                    valueFormatter = { "${it.roundToInt()} kbps" },
                                    onValueChange = { kbps ->
                                        onSettingsChange { it.copy(targetBitrateKbps = kbps.roundToInt()) }
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Preserve Audio Track",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Keep synchronized AAC audio stream",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Switch(
                                        checked = settings.preserveAudio,
                                        onCheckedChange = { keep ->
                                            onSettingsChange { it.copy(preserveAudio = keep) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = colors.cobaltPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
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
                formatSummary = "${settings.formatOption.label} • ${settings.frameRate} FPS • ${settings.targetBitrateKbps} kbps"
            ),
            beforeBitmap = videoThumbnailBitmap,
            onDismiss = { showInbuiltVideoPlayer = false }
        )
    }
}

@Composable
fun PhotoCompressorScreen(
    sourceInfo: MediaSourceInfo?,
    previewBitmap: Bitmap?,
    settings: PhotoCompressionSettings,
    estimatedBytes: Long,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onPresetSelected: (CompressionPreset) -> Unit,
    onSettingsChange: ((PhotoCompressionSettings) -> PhotoCompressionSettings) -> Unit,
    onCompressPhoto: () -> Unit
) {
    val colors = LocalEliteColors.current
    var showInbuiltPhotoPreview by remember { mutableStateOf(false) }

    val origBytes = sourceInfo?.sizeBytes ?: 0L
    val srcW = sourceInfo?.width ?: (previewBitmap?.width ?: 1920)
    val srcH = sourceInfo?.height ?: (previewBitmap?.height ?: 1080)
    val (targetW, targetH) = remember(
        srcW,
        srcH,
        settings.resolutionMode,
        settings.resolutionScale,
        settings.customWidth,
        settings.customHeight,
        settings.keepAspectRatio
    ) {
        PhotoProcessor.resolvePhotoTargetDimensions(
            srcWidth = srcW,
            srcHeight = srcH,
            mode = settings.resolutionMode,
            scaleFactor = settings.resolutionScale,
            customWidth = settings.customWidth,
            customHeight = settings.customHeight,
            preserveAspect = settings.keepAspectRatio
        )
    }

    val showTransparencyWarning = (sourceInfo?.hasTransparency == true) && !settings.format.supportsTransparency
    val showLowQualityWarning = settings.quality < 50 && !settings.format.isLossless

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                EliteTopBar(
                    title = "Compress Photo",
                    onBackClick = onBack,
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sourceInfo != null || previewBitmap != null) {
                                EliteIconButton(
                                    icon = Icons.Filled.ZoomOutMap,
                                    contentDescription = "Open inbuilt photo preview",
                                    onClick = { showInbuiltPhotoPreview = true },
                                    modifier = Modifier.testTag("photo_compressor_inbuilt_preview_button")
                                )
                            }
                            EliteIconButton(
                                icon = Icons.Filled.FolderOpen,
                                contentDescription = "Select photo to compress",
                                onClick = onPickPhoto,
                                modifier = Modifier.testTag("photo_compressor_pick_button")
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
                        "Compress & Save (${targetW} × ${targetH})"
                    } else {
                        "Select Photo to Compress"
                    },
                    leadingIcon = Icons.Filled.Compress,
                    onClick = {
                        if (sourceInfo != null) onCompressPhoto() else onPickPhoto()
                    },
                    modifier = Modifier.testTag("compress_photo_button")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (previewBitmap == null && sourceInfo == null) {
                    Surface(
                        onClick = onPickPhoto,
                        shape = RoundedCornerShape(30.dp),
                        color = colors.surfacePure,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(216.dp)
                            .border(1.5.dp, colors.cobaltPrimary.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceTinted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Select photo",
                                    tint = colors.cobaltPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to Select a Photo from Gallery or Files",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Real-time JPEG, PNG & WebP encoding with Gallery save",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(216.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .clickable { showInbuiltPhotoPreview = true }
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Source photo thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (sourceInfo != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfacePure.copy(alpha = 0.90f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "${sourceInfo.filename}  •  ${srcW}×${srcH} → ${targetW}×${targetH}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { showInbuiltPhotoPreview = true },
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cobaltPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .testTag("photo_compressor_open_preview_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomOutMap,
                                    contentDescription = "Inbuilt Photo Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Preview",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Real Size Comparison Region
            item {
                EliteSizeComparison(
                    originalSizeBytes = origBytes,
                    targetSizeBytes = if (sourceInfo != null) estimatedBytes else 0L,
                    targetLabel = "Estimated"
                )
            }

            if (showTransparencyWarning || showLowQualityWarning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EliteWarningBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Quality warning",
                                tint = EliteWarningText
                            )
                            Text(
                                text = if (showTransparencyWarning) {
                                    "Converting a transparent image to JPEG will flatten alpha transparency. Choose WebP or PNG Lossless to preserve transparency."
                                } else {
                                    "Quality below 50% may introduce visible compression artifacts."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = EliteWarningText
                            )
                        }
                    }
                }
            }

            // Compression Presets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Compression Preset",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    ElitePresetSelector(
                        options = CompressionPreset.entries,
                        selectedOption = settings.preset,
                        labelProvider = { it.label },
                        onOptionSelected = onPresetSelected
                    )
                }
            }

            // High-Quality Formats, Quality Slider, Custom Resolution, Keep Aspect Ratio, and Metadata
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
                            text = "Output Photo Format & Custom Resolution",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        ElitePresetSelector(
                            options = PhotoOutputFormat.entries,
                            selectedOption = settings.format,
                            labelProvider = { it.label },
                            onOptionSelected = { fmt ->
                                onSettingsChange { it.copy(format = fmt) }
                            }
                        )

                        if (!settings.format.isLossless) {
                            EliteAdjustmentSlider(
                                label = "Encoding Quality",
                                value = settings.quality.toFloat(),
                                valueRange = 20f..100f,
                                icon = Icons.Filled.HighQuality,
                                valueFormatter = { "${it.roundToInt()}%" },
                                onValueChange = { q ->
                                    onSettingsChange { it.copy(quality = q.roundToInt()) }
                                }
                            )
                        }

                        Text(
                            text = "Target Resolution (${targetW} × ${targetH} px)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        ElitePresetSelector(
                            options = PhotoResolutionMode.entries,
                            selectedOption = settings.resolutionMode,
                            labelProvider = { it.label },
                            onOptionSelected = { mode ->
                                onSettingsChange {
                                    it.copy(
                                        resolutionMode = mode,
                                        resolutionScale = mode.scaleFactor
                                    )
                                }
                            }
                        )

                        AnimatedVisibility(visible = settings.resolutionMode == PhotoResolutionMode.CUSTOM) {
                            EliteCustomResolutionControls(
                                width = settings.customWidth,
                                height = settings.customHeight,
                                sourceWidth = srcW,
                                sourceHeight = srcH,
                                keepAspectRatio = settings.keepAspectRatio,
                                isVideo = false,
                                onDimensionsChanged = { w, h ->
                                    onSettingsChange {
                                        it.copy(
                                            resolutionMode = PhotoResolutionMode.CUSTOM,
                                            customWidth = w,
                                            customHeight = h
                                        )
                                    }
                                },
                                onKeepAspectChanged = { keep ->
                                    onSettingsChange { it.copy(keepAspectRatio = keep) }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Strip EXIF Metadata",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Remove camera & location tags to save space",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = settings.removeMetadata,
                                onCheckedChange = { strip ->
                                    onSettingsChange { it.copy(removeMetadata = strip) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.cobaltPrimary
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showInbuiltPhotoPreview && (sourceInfo != null || previewBitmap != null)) {
        EliteInbuiltMediaPlayerDialog(
            mediaItem = InbuiltPlayerMediaItem(
                uri = sourceInfo?.uri ?: android.net.Uri.EMPTY,
                title = sourceInfo?.filename ?: "Compressed Photo Preview",
                isVideo = false,
                resolutionLabel = "${targetW} × ${targetH}",
                sizeBytes = if (estimatedBytes > 0L) estimatedBytes else (sourceInfo?.sizeBytes ?: 0L),
                formatSummary = "${settings.format.label} (${settings.quality}%)"
            ),
            beforeBitmap = previewBitmap,
            onDismiss = { showInbuiltPhotoPreview = false }
        )
    }
}
