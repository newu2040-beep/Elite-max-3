package com.example.feature.home

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.EliteBottomNavigation
import com.example.core.designsystem.EliteBottomTab
import com.example.core.designsystem.EliteCobaltEnhancerCard
import com.example.core.designsystem.EliteEditorialHeader
import com.example.core.designsystem.EliteEmptyState
import com.example.core.designsystem.EliteFileRow
import com.example.core.designsystem.EliteInbuiltMediaPlayerDialog
import com.example.core.designsystem.ElitePrimaryButton
import com.example.core.designsystem.EliteTopBar
import com.example.core.designsystem.EliteWelcomeShowcase
import com.example.core.media.DevicePermissionsStatus
import com.example.core.media.EnhancementSettings
import com.example.core.media.InbuiltPlayerMediaItem
import com.example.core.media.MediaSourceInfo
import com.example.core.media.formatFileSize
import com.example.core.storage.MediaJobEntity
import com.example.core.storage.MediaOperationType
import com.example.ui.theme.LocalEliteColors
import android.net.Uri

@Composable
fun WelcomeScreen(
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    clarityScore: Int,
    permissionsStatus: DevicePermissionsStatus,
    onGetStarted: () -> Unit,
    onRequestAllPermissions: () -> Unit
) {
    val colors = LocalEliteColors.current

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                EliteTopBar(
                    showWordmark = true,
                    onSearchClick = onGetStarted,
                    onNotificationsClick = onRequestAllPermissions,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                EliteEditorialHeader(
                    lightLine = "Media Quality",
                    boldLine = "Review",
                    subtitleLine1 = "Sharper. Clearer. Better.",
                    subtitleLine2 = "On-device enhancement & compression."
                )
            }

            EliteWelcomeShowcase(
                beforeBitmap = beforeBitmap,
                afterBitmap = afterBitmap,
                clarityScore = clarityScore,
                onPreviewClick = {
                    onRequestAllPermissions()
                    onGetStarted()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!permissionsStatus.allGranted) {
                    Surface(
                        onClick = onRequestAllPermissions,
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfacePure,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("welcome_grant_permissions_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = colors.cobaltPrimary
                                )
                                Column {
                                    Text(
                                        text = "Enable Notification, Gallery & Files Access",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Required for real-time gallery loading & saving exports",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Text(
                                text = "Allow",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.cobaltPrimary
                            )
                        }
                    }
                }

                ElitePrimaryButton(
                    text = "Get Started",
                    onClick = {
                        if (!permissionsStatus.allGranted) {
                            onRequestAllPermissions()
                        }
                        onGetStarted()
                    },
                    modifier = Modifier.testTag("get_started_button")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "100% Real-Time On-Device Engine • Auto-Saves to Gallery",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    permissionsStatus: DevicePermissionsStatus,
    deviceGalleryMedia: List<MediaSourceInfo>,
    activeVideo: MediaSourceInfo?,
    activePhoto: MediaSourceInfo?,
    activeVideoThumbnail: Bitmap?,
    enhanceSettings: EnhancementSettings,
    recentJobs: List<MediaJobEntity>,
    searchQuery: String,
    availableStorageText: String,
    statusBannerMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onDismissBanner: () -> Unit,
    onOpenVideoEnhancer: () -> Unit,
    onOpenPhotoEnhancer: () -> Unit,
    onOpenVideoCompressor: () -> Unit,
    onOpenPhotoCompressor: () -> Unit,
    onSelectGalleryMediaItem: (MediaSourceInfo) -> Unit,
    onPickVideoFromSystem: () -> Unit,
    onPickPhotoFromSystem: () -> Unit,
    onPickDocumentFiles: () -> Unit,
    onRequestAllPermissions: () -> Unit,
    onViewAllHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcomeReview: () -> Unit,
    onOpenJobFile: (MediaJobEntity) -> Unit,
    onShareJobFile: (MediaJobEntity) -> Unit,
    onDeleteJob: (MediaJobEntity) -> Unit
) {
    val colors = LocalEliteColors.current
    var showSearchField by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var activePlayerMedia by remember { mutableStateOf<InbuiltPlayerMediaItem?>(null) }

    val filteredJobs = remember(recentJobs, searchQuery) {
        if (searchQuery.isBlank()) recentJobs.take(6)
        else recentJobs.filter {
            it.filename.contains(searchQuery, ignoreCase = true) ||
                it.operationType.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                EliteTopBar(
                    showWordmark = true,
                    onSearchClick = { showSearchField = !showSearchField },
                    onNotificationsClick = { showPermissionsDialog = true },
                    activeJobCount = if (!permissionsStatus.allGranted) 1 else 0
                )

                AnimatedVisibility(visible = showSearchField) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search processed filenames or tools...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfacePure,
                            unfocusedContainerColor = colors.surfacePure,
                            focusedBorderColor = colors.cobaltPrimary,
                            unfocusedBorderColor = colors.divider
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 6.dp)
                            .testTag("home_search_input")
                    )
                }
            }
        },
        bottomBar = {
            EliteBottomNavigation(
                currentTab = EliteBottomTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        EliteBottomTab.HOME -> {}
                        EliteBottomTab.HISTORY -> onViewAllHistory()
                        EliteBottomTab.SETTINGS -> onOpenSettings()
                    }
                },
                onCenterStudioClick = onViewAllHistory
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Permissions Status Card if any permission is missing
            if (!permissionsStatus.allGranted) {
                item {
                    Surface(
                        onClick = onRequestAllPermissions,
                        shape = RoundedCornerShape(24.dp),
                        color = colors.cobaltPrimary,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_all_permissions_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Grant Notification, Gallery & Files Access",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to allow full real-time access to your device photos, videos, and export notifications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White
                            ) {
                                Text(
                                    text = "Allow All",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.cobaltPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!statusBannerMessage.isNullOrEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.mintBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusBannerMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.mintText,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissBanner) {
                                Text("OK", color = colors.mintText)
                            }
                        }
                    }
                }
            }

            // Editorial Home Heading
            item {
                EliteEditorialHeader(
                    lightLine = "Better media.",
                    boldLine = "Less space.",
                    subtitleLine1 = "Enhance your photos and videos. Compress to save storage. All on your device."
                )
            }

            // Quick Media Pickers (Gallery, Videos, Full SAF Files)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSourceChip(
                        icon = Icons.Outlined.PhotoLibrary,
                        label = "Select Gallery",
                        onClick = onPickPhotoFromSystem,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_pick_gallery_button")
                    )
                    QuickSourceChip(
                        icon = Icons.Outlined.FolderOpen,
                        label = "Browse All Files",
                        onClick = onPickDocumentFiles,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_browse_files_button")
                    )
                }
            }

            // Real-Time Device Gallery Strip (when device has real photos/videos)
            if (deviceGalleryMedia.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Device Gallery (${deviceGalleryMedia.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Tap to load in Studio",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.cobaltPrimary
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(deviceGalleryMedia, key = { it.uri.toString() }) { media ->
                                DeviceGalleryThumbnailCard(
                                    media = media,
                                    isSelected = media.uri == activeVideo?.uri || media.uri == activePhoto?.uri,
                                    onClick = {
                                        onSelectGalleryMediaItem(media)
                                        if (media.isVideo) onOpenVideoEnhancer() else onOpenPhotoEnhancer()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Four Primary Tools
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = onOpenVideoEnhancer,
                            shape = RoundedCornerShape(28.dp),
                            color = colors.cobaltPrimary,
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .weight(1.08f)
                                .height(166.dp)
                                .testTag("tool_card_video_enhancer")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.HighQuality,
                                        contentDescription = "Video Enhancer",
                                        tint = colors.cobaltPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Video Enhancer",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "4K/1080p clarity, detail & noise control",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onOpenPhotoEnhancer,
                            shape = RoundedCornerShape(28.dp),
                            color = colors.surfacePure,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .weight(0.92f)
                                .height(166.dp)
                                .testTag("tool_card_photo_enhancer")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceTinted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoFixHigh,
                                        contentDescription = "Photo Enhancer",
                                        tint = colors.cobaltPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Photo Enhancer",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Lossless PNG, WebP & 100% JPEG HQ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    CompactToolRow(
                        icon = Icons.Filled.Compress,
                        title = "Video Compressor",
                        subtitle = "Real-time H.264, HEVC & VP9 hardware encoding with Gallery save",
                        badgeText = "HEVC / MP4",
                        onClick = onOpenVideoCompressor,
                        modifier = Modifier.testTag("tool_card_video_compressor")
                    )

                    CompactToolRow(
                        icon = Icons.Filled.PhotoFilter,
                        title = "Photo Compressor",
                        subtitle = "Real-time JPEG, PNG & WebP optimization saved to Gallery",
                        badgeText = "Lossless / HQ",
                        onClick = onOpenPhotoCompressor,
                        modifier = Modifier.testTag("tool_card_photo_compressor")
                    )
                }
            }

            // Active Media Inspector Section (Displays real loaded video/photo or prompt to pick)
            item {
                val activeSource = activeVideo ?: activePhoto
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Media Studio",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        TextButton(onClick = onOpenWelcomeReview) {
                            Text(
                                text = "Quality Review",
                                color = colors.cobaltPrimary
                            )
                        }
                    }

                    if (activeSource != null) {
                        EliteFileRow(
                            title = "${if (activeSource.isVideo) "Video" else "Photo"}  •  ${activeSource.formattedSize}",
                            subtitle = "${activeSource.filename}  •  ${activeSource.resolutionLabel}  •  Tap to Preview",
                            mediaUri = activeSource.uri.toString(),
                            durationLabel = if (activeSource.isVideo) activeSource.formattedDuration else activeSource.formattedDate,
                            onClick = {
                                activePlayerMedia = InbuiltPlayerMediaItem(
                                    uri = activeSource.uri,
                                    title = activeSource.filename,
                                    isVideo = activeSource.isVideo,
                                    resolutionLabel = activeSource.resolutionLabel,
                                    sizeBytes = activeSource.sizeBytes,
                                    durationMs = activeSource.durationMs,
                                    formatSummary = activeSource.mimeType
                                )
                            }
                        )

                        EliteCobaltEnhancerCard(
                            filename = activeSource.filename,
                            dateAndDurationLabel = "${activeSource.formattedDate}  •  ${if (activeSource.isVideo) activeSource.formattedDuration else activeSource.resolutionLabel}",
                            thumbnailBitmap = activeVideoThumbnail,
                            settings = enhanceSettings,
                            onTileClick = {
                                if (activeSource.isVideo) onOpenVideoEnhancer() else onOpenPhotoEnhancer()
                            },
                            onActionIconClick = onPickPhotoFromSystem
                        )
                    } else {
                        EliteCobaltEnhancerCard(
                            filename = "Select a Video or Photo from Device",
                            dateAndDurationLabel = "Tap folder icon to load real media from Gallery or Files",
                            thumbnailBitmap = null,
                            settings = enhanceSettings,
                            onTileClick = { onPickPhotoFromSystem() },
                            onActionIconClick = onPickPhotoFromSystem
                        )
                    }
                }
            }

            // Recent Processed Files Section (Real Room History only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Gallery Exports",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = availableStorageText,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                    TextButton(
                        onClick = onViewAllHistory,
                        modifier = Modifier.testTag("view_all_history_button")
                    ) {
                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.cobaltPrimary
                        )
                    }
                }
            }

            if (filteredJobs.isEmpty()) {
                item {
                    EliteEmptyState(
                        title = "No Exports Yet",
                        subtitle = "Select a real photo or video from your device Gallery or Files to enhance or compress and save directly into your Gallery.",
                        actionLabel = "Select Media from Gallery",
                        onActionClick = onPickPhotoFromSystem
                    )
                }
            } else {
                items(filteredJobs, key = { it.id }) { job ->
                    val opType = runCatching { MediaOperationType.valueOf(job.operationType) }
                        .getOrDefault(MediaOperationType.PHOTO_ENHANCE)
                    val savings = job.savingsPercent
                    val savingsLabel = if (savings != null && savings > 0) "-$savings%" else null

                    EliteFileRow(
                        title = job.filename,
                        subtitle = "${opType.displayLabel}  •  ${formatFileSize(job.outputSizeBytes)}  •  Saved to Gallery",
                        mediaUri = job.outputUri,
                        durationLabel = job.outputDimensions,
                        savingsBadge = savingsLabel,
                        onClick = {
                            if (job.outputUri.isNotEmpty()) {
                                activePlayerMedia = InbuiltPlayerMediaItem(
                                    uri = Uri.parse(job.outputUri),
                                    title = job.filename,
                                    isVideo = job.operationType.contains("VIDEO"),
                                    resolutionLabel = job.outputDimensions,
                                    sizeBytes = job.outputSizeBytes,
                                    durationMs = job.durationMs,
                                    formatSummary = job.settingsSummary
                                )
                            } else {
                                onOpenJobFile(job)
                            }
                        },
                        onOpenClick = {
                            if (job.outputUri.isNotEmpty()) {
                                activePlayerMedia = InbuiltPlayerMediaItem(
                                    uri = Uri.parse(job.outputUri),
                                    title = job.filename,
                                    isVideo = job.operationType.contains("VIDEO"),
                                    resolutionLabel = job.outputDimensions,
                                    sizeBytes = job.outputSizeBytes,
                                    durationMs = job.durationMs,
                                    formatSummary = job.settingsSummary
                                )
                            } else {
                                onOpenJobFile(job)
                            }
                        },
                        onShareClick = { onShareJobFile(job) },
                        onDeleteClick = { onDeleteJob(job) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    val playerItem = activePlayerMedia
    if (playerItem != null) {
        EliteInbuiltMediaPlayerDialog(
            mediaItem = playerItem,
            afterBitmap = if (!playerItem.isVideo) activeVideoThumbnail else null,
            onDismiss = { activePlayerMedia = null }
        )
    }

    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = colors.cobaltPrimary
                )
            },
            title = {
                Text("Permissions & Full Gallery Access")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Manage real-time permissions for Notifications, Gallery (Photos & Videos), and Full Files Access:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PermissionOptionRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Request All Permissions (Notifications & Gallery)",
                        subtitle = if (permissionsStatus.allGranted) "All permissions granted" else "Tap to prompt Android system permission dialog",
                        isGranted = permissionsStatus.allGranted,
                        onClick = {
                            showPermissionsDialog = false
                            onRequestAllPermissions()
                        }
                    )
                    PermissionOptionRow(
                        icon = Icons.Outlined.PhotoLibrary,
                        title = "Open Gallery Picker (Photos & Videos)",
                        subtitle = "Select single or multiple media files",
                        isGranted = permissionsStatus.hasAnyGalleryAccess,
                        onClick = {
                            showPermissionsDialog = false
                            onPickPhotoFromSystem()
                        }
                    )
                    PermissionOptionRow(
                        icon = Icons.Outlined.FolderOpen,
                        title = "Full Storage / Files Access (SAF)",
                        subtitle = "Browse device folders for high-bitrate MP4, MKV, RAW & PNG",
                        isGranted = true,
                        onClick = {
                            showPermissionsDialog = false
                            onPickDocumentFiles()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DeviceGalleryThumbnailCard(
    media: MediaSourceInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalEliteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfacePure,
        shadowElevation = if (isSelected) 8.dp else 3.dp,
        modifier = Modifier
            .width(126.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) colors.cobaltPrimary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceTinted),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = media.uri,
                    contentDescription = media.filename,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (media.isVideo) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
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
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = media.filename,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = media.formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun QuickSourceChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfacePure.copy(alpha = 0.9f),
        shadowElevation = 3.dp,
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.cobaltPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun CompactToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalEliteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = colors.surfacePure.copy(alpha = 0.92f),
        shadowElevation = 5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceTinted),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.cobaltPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.mintBg
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.mintText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted
            )
        }
    }
}

@Composable
private fun PermissionOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalEliteColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceTinted.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.cobaltPrimary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
        if (isGranted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = colors.mintText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
