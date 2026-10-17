package com.example.core.designsystem

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.core.media.EnhancementSettings
import com.example.core.media.formatFileSize
import com.example.ui.theme.LocalEliteColors
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EliteWelcomeShowcase(
    beforeBitmap: Bitmap? = null,
    afterBitmap: Bitmap? = null,
    clarityScore: Int = 82,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        EliteWaveMotifBackground(
            modifier = Modifier.fillMaxSize()
        )

        // Back Card ("Before")
        Box(
            modifier = Modifier
                .offset(x = (-34).dp, y = (-18).dp)
                .rotate(-7.5f)
                .size(width = 215.dp, height = 225.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
        ) {
            val softMatrix = remember {
                ColorMatrix().apply { setToSaturation(0.55f) }
            }
            if (beforeBitmap != null) {
                Image(
                    bitmap = beforeBitmap.asImageBitmap(),
                    contentDescription = "Before enhancement preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(softMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_alpine_sample),
                    contentDescription = "Before enhancement preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(softMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x38AFC5E3))
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF5D7290).copy(alpha = 0.78f),
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Before",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Front Card ("After")
        Box(
            modifier = Modifier
                .offset(x = 32.dp, y = 8.dp)
                .rotate(3.5f)
                .size(width = 235.dp, height = 242.dp)
                .shadow(20.dp, RoundedCornerShape(30.dp), spotColor = colors.cobaltPrimary.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(30.dp))
                .border(2.5.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(30.dp))
                .clickable(onClick = onPreviewClick)
        ) {
            if (afterBitmap != null) {
                Image(
                    bitmap = afterBitmap.asImageBitmap(),
                    contentDescription = "After enhancement preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_alpine_sample),
                    contentDescription = "After enhancement preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cobaltPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "After",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Select media",
                    tint = colors.cobaltPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Floating Bottom-Left Clarity Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfacePure.copy(alpha = 0.90f),
            shadowElevation = 14.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 8.dp)
                .width(172.dp)
                .border(1.dp, Color.White, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val w = size.width
                        val h = size.height
                        val p = Path().apply {
                            moveTo(0f, h * 0.55f)
                            lineTo(w * 0.25f, h * 0.55f)
                            lineTo(w * 0.42f, h * 0.15f)
                            lineTo(w * 0.60f, h * 0.88f)
                            lineTo(w * 0.76f, h * 0.55f)
                            lineTo(w, h * 0.55f)
                        }
                        drawPath(
                            path = p,
                            color = colors.cobaltPrimary,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                    Column {
                        Text(
                            text = "Clarity",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = clarityScore.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                EliteMiniWaveform(
                    barCount = 19,
                    accentColor = colors.cobaltPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                )
            }
        }
    }
}

@Composable
fun EliteMiniWaveform(
    barCount: Int = 22,
    accentColor: Color = LocalEliteColors.current.cobaltPrimary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = w / barCount.coerceAtLeast(1)
        val barWidth = (spacing * 0.42f).coerceIn(3f, 8f)
        for (i in 0 until barCount) {
            val normalized = 0.22f + 0.72f * kotlin.math.abs(sin(i * 0.58f + 0.4f))
            val barH = (h * normalized).coerceAtLeast(6f)
            val top = (h - barH) / 2f
            val alpha = if (i in (barCount / 3)..(barCount * 3 / 4)) 0.92f else 0.38f
            drawRoundRect(
                color = accentColor.copy(alpha = alpha),
                topLeft = Offset(i * spacing + (spacing - barWidth) / 2f, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth, barWidth)
            )
        }
    }
}

/**
 * Real-time Interactive Before/After Comparison Slider.
 * When a real video URI is playing, embeds a real Android VideoView; otherwise compares real decoded/enhanced Bitmaps.
 * If no media is selected yet, renders a direct tap-to-pick surface.
 */
@Composable
fun EliteBeforeAfterSlider(
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    videoUri: Uri? = null,
    isVideo: Boolean = false,
    isPlaying: Boolean = false,
    onTogglePlay: (() -> Unit)? = null,
    onSelectMediaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    var splitFraction by remember { mutableFloatStateOf(0.50f) }

    if (beforeBitmap == null && afterBitmap == null && videoUri == null) {
        // Real-time Empty Media Selection Prompt (No fake demo image)
        Surface(
            onClick = { onSelectMediaClick?.invoke() },
            shape = RoundedCornerShape(30.dp),
            color = colors.surfacePure,
            shadowElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
                .height(236.dp)
                .border(1.5.dp, colors.cobaltPrimary.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                .testTag("select_real_media_prompt")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceTinted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Filled.VideoFile else Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Select media from Gallery or Files",
                        tint = colors.cobaltPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isVideo) "Tap to Select a Video from Gallery" else "Tap to Select a Photo from Gallery",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real-time on-device preview & comparison will load immediately",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(248.dp)
            .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = colors.cobaltPrimary.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(30.dp))
            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(30.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    splitFraction = (splitFraction + dragAmount / widthPx).coerceIn(0.05f, 0.95f)
                }
            }
            .testTag("before_after_slider")
    ) {
        val maxW = maxWidth

        if (isVideo && isPlaying && videoUri != null) {
            // Real-time hardware VideoView playback
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) {
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Base Layer: AFTER (Real-time Enhanced Bitmap)
            val displayAfter = afterBitmap ?: beforeBitmap
            if (displayAfter != null) {
                Image(
                    bitmap = displayAfter.asImageBitmap(),
                    contentDescription = "Enhanced media preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Clipped Left Layer: BEFORE (Original Unmodified Source Bitmap)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(right = size.width * splitFraction) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                val displayBefore = beforeBitmap ?: afterBitmap
                if (displayBefore != null) {
                    Image(
                        bitmap = displayBefore.asImageBitmap(),
                        contentDescription = "Original media preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Before & After floating pills
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF475569).copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Text(
                text = "Original",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cobaltPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
        ) {
            Text(
                text = "Enhanced",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        if (!isPlaying) {
            // Vertical Divider Line + Draggable Handle
            Box(
                modifier = Modifier
                    .offset(x = maxW * splitFraction - 1.dp)
                    .fillMaxHeight()
                    .width(2.5.dp)
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .offset(x = maxW * splitFraction - 18.dp)
                    .align(Alignment.CenterStart)
                    .size(36.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, colors.cobaltPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↔",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.cobaltPrimary
                )
            }
        }

        // Video Play/Pause button at bottom-right
        if (isVideo && onTogglePlay != null && videoUri != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(44.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f))
                    .clickable(onClick = onTogglePlay)
                    .testTag("video_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                    tint = colors.cobaltPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Dominant Cobalt-Blue Feature Panel displaying real active media metadata and the 2x2 grid of
 * real-time enhancement metrics (`Sharpness`, `Detail`, `Noise`, `Color`).
 */
@Composable
fun EliteCobaltEnhancerCard(
    filename: String,
    dateAndDurationLabel: String,
    thumbnailBitmap: Bitmap? = null,
    settings: EnhancementSettings,
    onTileClick: ((String) -> Unit)? = null,
    onActionIconClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = colors.cobaltPrimary,
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.cobaltTile)
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VideoFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = filename,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateAndDurationLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onActionIconClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Choose media file",
                        tint = colors.cobaltPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val sharpnessPct = "+${(settings.sharpness * 100).roundToInt()}%"
            val detailPct = "${if (settings.contrast >= 0) "+" else ""}${(settings.contrast * 100).roundToInt()}%"
            val noisePct = "-${(settings.noiseReduction * 100).roundToInt()}%"
            val colorPct = "${if (settings.saturation >= 0) "+" else ""}${(settings.saturation * 100).roundToInt()}%"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CobaltMetricTile(
                    icon = Icons.Filled.AutoAwesome,
                    label = "Sharpness",
                    valueText = sharpnessPct,
                    onClick = { onTileClick?.invoke("Sharpness") },
                    modifier = Modifier.weight(1f)
                )
                CobaltMetricTile(
                    icon = Icons.Filled.Contrast,
                    label = "Detail",
                    valueText = detailPct,
                    onClick = { onTileClick?.invoke("Detail") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CobaltMetricTile(
                    icon = Icons.Filled.BlurOn,
                    label = "Noise",
                    valueText = noisePct,
                    onClick = { onTileClick?.invoke("Noise") },
                    modifier = Modifier.weight(1f)
                )
                CobaltMetricTile(
                    icon = Icons.Filled.Palette,
                    label = "Color",
                    valueText = colorPct,
                    onClick = { onTileClick?.invoke("Color") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CobaltMetricTile(
    icon: ImageVector,
    label: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cobaltTile)
            .border(1.dp, colors.cobaltTileBorder.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.cobaltPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
fun EliteFileRow(
    title: String,
    subtitle: String,
    mediaUri: String? = null,
    durationLabel: String? = null,
    savingsBadge: String? = null,
    onClick: () -> Unit,
    onOpenClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = colors.surfacePure.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 66.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceTinted),
                contentAlignment = Alignment.Center
            ) {
                if (!mediaUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = Uri.parse(mediaUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = colors.cobaltPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (savingsBadge != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.mintBg,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = savingsBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.mintText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EliteMiniWaveform(
                        barCount = 20,
                        accentColor = colors.cobaltPrimary.copy(alpha = 0.72f),
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                    )
                    if (!durationLabel.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
            }

            if (onOpenClick != null || onShareClick != null || onDeleteClick != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More actions",
                            tint = colors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (onOpenClick != null) {
                            DropdownMenuItem(
                                text = { Text("Open in Gallery / Player") },
                                leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenClick()
                                }
                            )
                        }
                        if (onShareClick != null) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onShareClick()
                                }
                            )
                        }
                        if (onDeleteClick != null) {
                            DropdownMenuItem(
                                text = { Text("Delete from History") },
                                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textMuted
                )
            }
        }
    }
}

@Composable
fun EliteSizeComparison(
    originalSizeBytes: Long,
    targetSizeBytes: Long,
    targetLabel: String = "Estimated",
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    val savingsPct = if (originalSizeBytes > 0L && targetSizeBytes > 0L) {
        (((originalSizeBytes - targetSizeBytes).toDouble() / originalSizeBytes.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(-99, 95)
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfacePure.copy(alpha = 0.85f))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Original",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (originalSizeBytes > 0L) formatFileSize(originalSizeBytes) else "Select file",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(colors.divider)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1.15f)) {
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (targetSizeBytes > 0L) formatFileSize(targetSizeBytes) else "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary
                    )
                    if (savingsPct > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.mintBg
                        ) {
                            Text(
                                text = "-$savingsPct%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.mintText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        val ratio = if (originalSizeBytes > 0L && targetSizeBytes > 0L) {
            (targetSizeBytes.toFloat() / originalSizeBytes.toFloat()).coerceIn(0.18f, 1.0f)
        } else {
            0.50f
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val totalBars = 18
            val spacing = size.width / totalBars
            val barW = (spacing * 0.34f).coerceIn(5f, 12f)
            val half = totalBars / 2

            for (i in 0 until totalBars) {
                val isCompressedSide = i >= half
                val baseWave = 0.45f + 0.50f * kotlin.math.abs(sin(i * 0.85f + 0.3f))
                val heightFrac = if (isCompressedSide) {
                    (baseWave * ratio).coerceIn(0.16f, 0.92f)
                } else {
                    baseWave.coerceIn(0.32f, 0.98f)
                }
                val barH = size.height * heightFrac
                val x = i * spacing + (spacing - barW) / 2f
                val y = size.height - barH
                val barColor = if (isCompressedSide) {
                    colors.cobaltPrimary
                } else {
                    colors.cobaltPrimary.copy(alpha = 0.34f)
                }
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW, barW)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Original",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
            Text(
                text = "Compressed",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun EliteProcessingIndicator(
    progressFraction: Float,
    stageLabel: String,
    processedItems: Int,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(colors.surfacePure)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${(progressFraction * 100).roundToInt().coerceIn(0, 100)}%",
            style = MaterialTheme.typography.displayMedium,
            color = colors.cobaltPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stageLabel,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Processed $processedItems of $totalItems frames/blocks in real time",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(18.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            val bars = 24
            val spacing = size.width / bars
            val barW = (spacing * 0.44f).coerceIn(5f, 12f)
            val activeBars = (bars * progressFraction).roundToInt()

            for (i in 0 until bars) {
                val wave = 0.30f + 0.65f * kotlin.math.abs(sin(i * 0.55f + phase))
                val barH = size.height * wave
                val x = i * spacing + (spacing - barW) / 2f
                val y = (size.height - barH) / 2f
                drawRoundRect(
                    color = if (i <= activeBars) colors.cobaltPrimary else colors.surfaceTinted,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW, barW)
                )
            }
        }
    }
}
