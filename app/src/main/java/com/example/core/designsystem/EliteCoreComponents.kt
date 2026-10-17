package com.example.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalEliteColors
import kotlin.math.roundToInt

/**
 * Circular Cobalt Blue 'E' emblem matching the top-left badge in Screen 1 of the ELITEMAX reference.
 */
@Composable
fun EliteBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalEliteColors.current
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape, spotColor = colors.cobaltPrimary.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2E7DFF), colors.cobaltPrimary)
                )
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.46f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.16f, cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(w * 0.78f, h * 0.16f)
                lineTo(w * 0.36f, h * 0.16f)
                cubicTo(w * 0.18f, h * 0.16f, w * 0.18f, h * 0.34f, w * 0.18f, h * 0.50f)
                cubicTo(w * 0.18f, h * 0.66f, w * 0.18f, h * 0.84f, w * 0.36f, h * 0.84f)
                lineTo(w * 0.78f, h * 0.84f)
            }
            drawPath(path = path, color = Color.White, style = stroke)
            drawLine(
                color = Color.White,
                start = Offset(w * 0.22f, h * 0.50f),
                end = Offset(w * 0.68f, h * 0.50f),
                strokeWidth = w * 0.16f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun EliteIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalEliteColors.current.surfaceTinted,
    contentColor: Color = LocalEliteColors.current.textPrimary,
    badgeCount: Int = 0
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LocalEliteColors.current.cobaltPrimary)
            )
        }
    }
}

@Composable
fun EliteTopBar(
    title: String? = null,
    showWordmark: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    activeJobCount: Int = 0,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBackClick != null) {
                EliteIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                )
            } else {
                EliteBrandEmblem(
                    modifier = Modifier.testTag("top_bar_brand_emblem")
                )
            }

            if (showWordmark) {
                Text(
                    text = "ELITEMAX",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    ),
                    color = colors.textPrimary
                )
            } else if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onSearchClick != null) {
                EliteIconButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search media history",
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_bar_search_button")
                )
            }
            if (onNotificationsClick != null) {
                EliteIconButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Processing status and permissions",
                    onClick = onNotificationsClick,
                    badgeCount = activeJobCount,
                    modifier = Modifier.testTag("top_bar_status_button")
                )
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * Editorial two-line heading matching "Media Quality / Review" and "Compression / Result" in the reference.
 */
@Composable
fun EliteEditorialHeader(
    lightLine: String,
    boldLine: String,
    subtitleLine1: String,
    subtitleLine2: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = lightLine,
            style = MaterialTheme.typography.displayLarge,
            color = colors.textPrimary
        )
        Text(
            text = boldLine,
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitleLine1,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary
        )
        if (!subtitleLine2.isNullOrEmpty()) {
            Text(
                text = subtitleLine2,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun ElitePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showTrailingChevron: Boolean = true,
    enabled: Boolean = true
) {
    val colors = LocalEliteColors.current
    val bgColor by animateColorAsState(
        targetValue = if (enabled) colors.cobaltPrimary else colors.cobaltPrimary.copy(alpha = 0.45f),
        label = "primary_btn_bg"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(30.dp),
        color = bgColor,
        contentColor = Color.White,
        shadowElevation = if (enabled) 10.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }
            if (showTrailingChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(22.dp)
                )
            }
        }
    }
}

/**
 * Delicate multi-strand flowing translucent cobalt wave ribbon from Screen 1 of the reference image.
 */
@Composable
fun EliteWaveMotifBackground(
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strandCount = 12
        for (i in 0 until strandCount) {
            val offset = i * 6.5f
            val alpha = (0.04f + (i % 4) * 0.035f).coerceAtMost(0.18f)
            val path = Path().apply {
                moveTo(w * 1.05f, h * 0.12f + offset)
                cubicTo(
                    w * 0.62f, h * 0.34f - offset * 1.3f,
                    w * 0.96f, h * 0.66f + offset,
                    w * 0.22f, h * 0.96f + offset * 0.8f
                )
            }
            drawPath(
                path = path,
                color = colors.cobaltPrimary.copy(alpha = alpha),
                style = Stroke(width = 2.2f)
            )
        }
    }
}

@Composable
fun <T> ElitePresetSelector(
    options: List<T>,
    selectedOption: T,
    labelProvider: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceTinted.copy(alpha = 0.7f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val containerBg by animateColorAsState(
                targetValue = if (isSelected) colors.cobaltPrimary else Color.Transparent,
                label = "preset_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.textSecondary,
                label = "preset_text"
            )
            val label = labelProvider(option)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(containerBg)
                    .clickable { onOptionSelected(option) }
                    .testTag("preset_option_${label.lowercase().replace(" ", "_")}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EliteAdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    valueFormatter: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfacePure.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceTinted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.cobaltPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            }
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelLarge,
                color = colors.cobaltPrimary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.cobaltPrimary,
                activeTrackColor = colors.cobaltPrimary,
                inactiveTrackColor = colors.surfaceTinted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_${label.lowercase().replace(" ", "_")}")
        )
    }
}

@Composable
fun EliteEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.PhotoLibrary,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfacePure.copy(alpha = 0.78f))
            .padding(28.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = colors.cobaltPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ElitePrimaryButton(
                text = actionLabel,
                onClick = onActionClick,
                modifier = Modifier.height(50.dp)
            )
        }
    }
}

enum class EliteBottomTab(val label: String) {
    HOME("Home"),
    HISTORY("History"),
    SETTINGS("Settings")
}

/**
 * Sculpted rounded bottom navigation bar matching the 3 circular controls at the bottom of the ELITEMAX reference screens.
 */
@Composable
fun EliteBottomNavigation(
    currentTab: EliteBottomTab,
    onTabSelected: (EliteBottomTab) -> Unit,
    onCenterStudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Surface(
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        color = colors.surfacePure.copy(alpha = 0.94f),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 36.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Home / Media Library
            BottomNavCircleItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentTab == EliteBottomTab.HOME,
                onClick = { onTabSelected(EliteBottomTab.HOME) },
                testTag = "nav_home_button"
            )

            // Center: Prominent Cobalt Blue circular action/studio hub (matching the center blue circle with target dot in the reference)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onCenterStudioClick)
                    .testTag("nav_history_center_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            spotColor = colors.cobaltPrimary.copy(alpha = 0.45f)
                        )
                        .clip(CircleShape)
                        .background(
                            if (currentTab == EliteBottomTab.HISTORY) {
                                Brush.linearGradient(listOf(Color(0xFF0E4EC2), colors.cobaltPrimary))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF2B7BFF), colors.cobaltPrimary))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric ring + center dot icon from the reference bottom bar, or History indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentTab == EliteBottomTab.HISTORY) colors.cobaltPrimary else colors.textSecondary
                )
            }

            // Right: Settings (sliders icon matching the reference)
            BottomNavCircleItem(
                icon = Icons.Outlined.Tune,
                label = "Settings",
                isSelected = currentTab == EliteBottomTab.SETTINGS,
                onClick = { onTabSelected(EliteBottomTab.SETTINGS) },
                testTag = "nav_settings_button"
            )
        }
    }
}

@Composable
private fun BottomNavCircleItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = LocalEliteColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) colors.cobaltPrimary.copy(alpha = 0.14f)
                    else colors.surfaceTinted.copy(alpha = 0.65f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) colors.cobaltPrimary else colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.cobaltPrimary else colors.textSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
