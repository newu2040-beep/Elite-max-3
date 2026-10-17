package com.example.core.designsystem

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.widget.VideoView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.media.InbuiltPlayerMediaItem
import com.example.core.media.formatDuration
import com.example.core.media.formatFileSize
import com.example.ui.theme.LocalEliteColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full-screen Inbuilt Video Player & Interactive Photo Preview Modal.
 * Supports:
 * - Video: Play/Pause, -5s/+5s seek, real-time timeline scrubber, mute/unmute, loop toggle, playback speed (0.5x–2.0x)
 * - Photo: Pinch-to-zoom (1x–5x), pan, double-tap zoom, 1x/2x/4x quick buttons, Before/After compare toggle, 3x3 grid overlay
 */
@Composable
fun EliteInbuiltMediaPlayerDialog(
    mediaItem: InbuiltPlayerMediaItem,
    beforeBitmap: Bitmap? = null,
    afterBitmap: Bitmap? = null,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    onOpenExternal: (() -> Unit)? = null
) {
    val colors = LocalEliteColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A101D))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("inbuilt_media_player_dialog")
        ) {
            if (mediaItem.isVideo) {
                InbuiltVideoPlayerContent(
                    mediaItem = mediaItem,
                    onDismiss = onDismiss,
                    onShare = onShare,
                    onOpenExternal = onOpenExternal
                )
            } else {
                InbuiltPhotoPreviewContent(
                    mediaItem = mediaItem,
                    beforeBitmap = beforeBitmap,
                    afterBitmap = afterBitmap,
                    onDismiss = onDismiss,
                    onShare = onShare,
                    onOpenExternal = onOpenExternal
                )
            }
        }
    }
}

@Composable
private fun InbuiltVideoPlayerContent(
    mediaItem: InbuiltPlayerMediaItem,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenExternal: (() -> Unit)?
) {
    val colors = LocalEliteColors.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(mediaItem.durationMs.toInt().coerceAtLeast(1000)) }

    // Poll playback position while video is active
    LaunchedEffect(videoViewRef, isPlaying) {
        while (true) {
            val vv = videoViewRef
            if (vv != null && isPlaying) {
                runCatching {
                    if (vv.isPlaying) {
                        currentPosMs = vv.currentPosition.coerceAtLeast(0)
                        val dur = vv.duration
                        if (dur > 0) durationMs = dur
                    }
                }
            }
            delay(200L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Hardware Video Surface
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setVideoURI(mediaItem.uri)
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = isLooping
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (mp.duration > 0) {
                            durationMs = mp.duration
                        }
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        if (!isLooping) {
                            isPlaying = false
                            currentPosMs = durationMs
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    val vv = videoViewRef
                    if (vv != null) {
                        if (vv.isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            vv.start()
                            isPlaying = true
                        }
                    }
                }
        )

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.cobaltPrimary
                    ) {
                        Text(
                            text = "ELITEMAX PLAYER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (mediaItem.resolutionLabel.isNotEmpty()) {
                        Text(
                            text = mediaItem.resolutionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onShare != null) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share video", tint = Color.White)
                    }
                }
                if (onOpenExternal != null) {
                    IconButton(
                        onClick = onOpenExternal,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open external", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .testTag("close_inbuilt_player_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close player", tint = Color.White)
                }
            }
        }

        // Bottom Controls Dock
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF131D31).copy(alpha = 0.94f),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(18.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Seekbar + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = formatDuration(currentPosMs.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Slider(
                        value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { newPos ->
                            currentPosMs = newPos.roundToInt()
                            runCatching { videoViewRef?.seekTo(currentPosMs) }
                        },
                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = colors.cobaltPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inbuilt_video_seekbar")
                    )
                    Text(
                        text = formatDuration(durationMs.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }

                // Transport Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mute toggle + Loop toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                val vol = if (isMuted) 0f else 1f
                                runCatching { mediaPlayerRef?.setVolume(vol, vol) }
                            }
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = "Toggle audio mute",
                                tint = if (isMuted) Color(0xFFF59E0B) else Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                isLooping = !isLooping
                                runCatching { mediaPlayerRef?.isLooping = isLooping }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = "Toggle loop",
                                tint = if (isLooping) colors.cobaltPrimary else Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }

                    // Rewind 5s / Play-Pause / Forward 5s
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val target = (currentPosMs - 5000).coerceAtLeast(0)
                                currentPosMs = target
                                runCatching { videoViewRef?.seekTo(target) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay5,
                                contentDescription = "Rewind 5 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(colors.cobaltPrimary)
                                .clickable {
                                    val vv = videoViewRef
                                    if (vv != null) {
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlaying = false
                                        } else {
                                            vv.start()
                                            isPlaying = true
                                        }
                                    }
                                }
                                .testTag("inbuilt_player_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val target = (currentPosMs + 5000).coerceAtMost(durationMs)
                                currentPosMs = target
                                runCatching { videoViewRef?.seekTo(target) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward5,
                                contentDescription = "Forward 5 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Playback Speed Cycle (0.5x, 1.0x, 1.5x, 2.0x)
                    Surface(
                        onClick = {
                            val nextSpeed = when (playbackSpeed) {
                                0.5f -> 1.0f
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.5f
                            }
                            playbackSpeed = nextSpeed
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    mediaPlayerRef?.playbackParams = PlaybackParams().setSpeed(nextSpeed)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InbuiltPhotoPreviewContent(
    mediaItem: InbuiltPlayerMediaItem,
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenExternal: (() -> Unit)?
) {
    val colors = LocalEliteColors.current
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showOriginalCompare by remember { mutableStateOf(false) }
    var showRuleOfThirdsGrid by remember { mutableStateOf(false) }

    val activeBitmap = if (showOriginalCompare && beforeBitmap != null) {
        beforeBitmap
    } else {
        afterBitmap ?: beforeBitmap
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Interactive Zoom & Pan Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(1f, 5f)
                        zoomScale = newScale
                        if (newScale > 1.02f) {
                            offsetX = (offsetX + pan.x).coerceIn(-1200f, 1200f)
                            offsetY = (offsetY + pan.y).coerceIn(-1200f, 1200f)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                zoomScale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (activeBitmap != null) {
                Image(
                    bitmap = activeBitmap.asImageBitmap(),
                    contentDescription = mediaItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            } else {
                AsyncImage(
                    model = mediaItem.uri,
                    contentDescription = mediaItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            if (showRuleOfThirdsGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridColor = Color.White.copy(alpha = 0.32f)
                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.5f)
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showOriginalCompare) Color(0xFF475569) else colors.cobaltPrimary
                    ) {
                        Text(
                            text = if (showOriginalCompare) "ORIGINAL PHOTO" else "PHOTO PREVIEW • ${(zoomScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (mediaItem.resolutionLabel.isNotEmpty()) {
                        Text(
                            text = mediaItem.resolutionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mediaItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onShare != null) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share photo", tint = Color.White)
                    }
                }
                if (onOpenExternal != null) {
                    IconButton(
                        onClick = onOpenExternal,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open external", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .testTag("close_inbuilt_photo_preview_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                }
            }
        }

        // Bottom Photo Inspector & Zoom Controls
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF131D31).copy(alpha = 0.94f),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(18.dp)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append(mediaItem.resolutionLabel.ifEmpty { "Full Resolution" })
                            if (mediaItem.sizeBytes > 0L) {
                                append("  •  ")
                                append(formatFileSize(mediaItem.sizeBytes))
                            }
                            if (mediaItem.formatSummary.isNotEmpty()) {
                                append("  •  ")
                                append(mediaItem.formatSummary)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Pinch or double-tap to zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cobaltPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Zoom Pills: 1x, 2x, 4x
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1f to "1x Fit", 2f to "2x", 4f to "4x Detail").forEach { (targetZ, label) ->
                            val selected = kotlin.math.abs(zoomScale - targetZ) < 0.25f
                            Surface(
                                onClick = {
                                    zoomScale = targetZ
                                    if (targetZ == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) colors.cobaltPrimary else Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (beforeBitmap != null && afterBitmap != null) {
                            Surface(
                                onClick = { showOriginalCompare = !showOriginalCompare },
                                shape = RoundedCornerShape(14.dp),
                                color = if (showOriginalCompare) colors.cobaltPrimary else Color.White.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Compare,
                                        contentDescription = "Compare original and enhanced",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (showOriginalCompare) "Original" else "Compare",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showRuleOfThirdsGrid = !showRuleOfThirdsGrid },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showRuleOfThirdsGrid) colors.cobaltPrimary else Color.White.copy(alpha = 0.14f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridOn,
                                contentDescription = "Toggle grid",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Custom Width x Height Pixel Input Panel for both Video and Photo screens.
 * Includes quick resolution chips, Width & Height numeric fields, and Aspect Ratio lock.
 */
@Composable
fun EliteCustomResolutionControls(
    width: Int,
    height: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    keepAspectRatio: Boolean,
    isVideo: Boolean,
    onDimensionsChanged: (Int, Int) -> Unit,
    onKeepAspectChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    var widthText by remember(width) { mutableStateOf(width.toString()) }
    var heightText by remember(height) { mutableStateOf(height.toString()) }

    val aspect = remember(sourceWidth, sourceHeight) {
        if (sourceWidth > 0 && sourceHeight > 0) {
            sourceWidth.toFloat() / sourceHeight.toFloat()
        } else {
            16f / 9f
        }
    }

    val quickChips = remember(isVideo) {
        if (isVideo) {
            listOf(
                Triple("4K UHD", 3840, 2160),
                Triple("2K QHD", 2560, 1440),
                Triple("1080p FHD", 1920, 1080),
                Triple("720p HD", 1280, 720),
                Triple("Vertical 9:16", 1080, 1920),
                Triple("Square 1:1", 1080, 1080)
            )
        } else {
            listOf(
                Triple("4K Photo", 3840, 2160),
                Triple("2K QHD", 2560, 1440),
                Triple("1080p HD", 1920, 1080),
                Triple("Instagram 4:5", 1080, 1350),
                Triple("Square 1:1", 2048, 2048)
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceTinted.copy(alpha = 0.65f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Resolution (Width × Height px)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (keepAspectRatio) Icons.Filled.Link else Icons.Filled.LinkOff,
                        contentDescription = null,
                        tint = colors.cobaltPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lock Aspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Switch(
                        checked = keepAspectRatio,
                        onCheckedChange = onKeepAspectChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.cobaltPrimary
                        )
                    )
                }
            }

            // Width and Height Numeric Input Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        widthText = digits
                        val wVal = digits.toIntOrNull()
                        if (wVal != null && wVal >= 160) {
                            val maxLim = if (isVideo) 3840 else 8192
                            val clampedW = wVal.coerceIn(160, maxLim)
                            val newH = if (keepAspectRatio && aspect > 0f) {
                                (clampedW / aspect).roundToInt().coerceIn(160, maxLim)
                            } else {
                                height
                            }
                            if (keepAspectRatio) heightText = newH.toString()
                            onDimensionsChanged(clampedW, newH)
                        }
                    },
                    label = { Text("Width (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfacePure,
                        unfocusedContainerColor = colors.surfacePure,
                        focusedBorderColor = colors.cobaltPrimary,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_width_input")
                )

                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.cobaltPrimary
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        heightText = digits
                        val hVal = digits.toIntOrNull()
                        if (hVal != null && hVal >= 160) {
                            val maxLim = if (isVideo) 2160 else 8192
                            val clampedH = hVal.coerceIn(160, maxLim)
                            val newW = if (keepAspectRatio && aspect > 0f) {
                                (clampedH * aspect).roundToInt().coerceIn(160, if (isVideo) 3840 else 8192)
                            } else {
                                width
                            }
                            if (keepAspectRatio) widthText = newW.toString()
                            onDimensionsChanged(newW, clampedH)
                        }
                    },
                    label = { Text("Height (px)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfacePure,
                        unfocusedContainerColor = colors.surfacePure,
                        focusedBorderColor = colors.cobaltPrimary,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_height_input")
                )
            }

            // Quick Custom Dimension Chips
            ElitePresetSelector(
                options = quickChips,
                selectedOption = quickChips.firstOrNull { it.second == width && it.third == height } ?: quickChips.first(),
                labelProvider = { "${it.first} (${it.second}×${it.third})" },
                onOptionSelected = { triple ->
                    widthText = triple.second.toString()
                    heightText = triple.third.toString()
                    onDimensionsChanged(triple.second, triple.third)
                }
            )
        }
    }
}
