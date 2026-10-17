package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.media.MediaInspector
import com.example.core.navigation.EliteRoutes
import com.example.core.storage.MediaOperationType
import com.example.feature.EliteViewModel
import com.example.feature.compressor.PhotoCompressorScreen
import com.example.feature.compressor.VideoCompressorScreen
import com.example.feature.enhancer.PhotoEnhancerScreen
import com.example.feature.enhancer.VideoEnhancerScreen
import com.example.feature.home.HomeScreen
import com.example.feature.home.WelcomeScreen
import com.example.feature.workflow.ExportResultScreen
import com.example.feature.workflow.HistoryScreen
import com.example.feature.workflow.ProcessingScreen
import com.example.feature.workflow.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EliteMaxApp()
        }
    }
}

@Composable
fun EliteMaxApp(
    viewModel: EliteViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyJobs by viewModel.historyJobs.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // Photo Picker for Images (Single or Batch)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.selectMediaUris(uris, forceVideo = false)
        }
    }

    // Photo Picker for Videos (Single or Batch)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.selectMediaUris(uris, forceVideo = true)
        }
    }

    // Photo Picker for Both Images and Videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.selectMediaUris(uris, forceVideo = null)
        }
    }

    // Storage Access Framework Full Document/File Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.selectMediaUris(uris, forceVideo = null)
        }
    }

    // Multi-Permission Launcher: Notifications + Gallery Photos + Gallery Videos + Storage
    val allPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshPermissionsAndDeviceGallery()
        val grantedAny = results.values.any { it }
        if (grantedAny) {
            Toast.makeText(
                context,
                "Permissions updated • Real-time Gallery synced",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val requestAllPermissions: () -> Unit = {
        val perms = MediaInspector.getRequiredRuntimePermissions()
        if (perms.isNotEmpty()) {
            allPermissionsLauncher.launch(perms)
        } else {
            viewModel.refreshPermissionsAndDeviceGallery()
        }
    }

    // Automatically request all permissions on launch so real-time gallery & notifications work immediately
    LaunchedEffect(Unit) {
        val status = MediaInspector.checkPermissionsStatus(context)
        if (!status.allGranted) {
            requestAllPermissions()
        } else {
            viewModel.refreshPermissionsAndDeviceGallery()
        }
    }

    MyApplicationTheme(themeMode = preferences.themeMode) {
        NavHost(
            navController = navController,
            startDestination = EliteRoutes.WELCOME
        ) {
            composable(EliteRoutes.WELCOME) {
                WelcomeScreen(
                    beforeBitmap = uiState.beforePhotoBitmap ?: uiState.beforeVideoFrameBitmap,
                    afterBitmap = uiState.afterPhotoBitmap ?: uiState.afterVideoFrameBitmap,
                    clarityScore = uiState.enhanceSettings.clarityScore,
                    permissionsStatus = uiState.permissionsStatus,
                    onGetStarted = {
                        viewModel.markWelcomeCompleted()
                        navController.navigate(EliteRoutes.HOME) {
                            popUpTo(EliteRoutes.WELCOME) { inclusive = false }
                        }
                    },
                    onRequestAllPermissions = requestAllPermissions
                )
            }

            composable(EliteRoutes.HOME) {
                HomeScreen(
                    permissionsStatus = uiState.permissionsStatus,
                    deviceGalleryMedia = uiState.deviceGalleryMedia,
                    activeVideo = uiState.activeVideoSource,
                    activePhoto = uiState.activePhotoSource,
                    activeVideoThumbnail = uiState.afterVideoFrameBitmap ?: uiState.beforeVideoFrameBitmap ?: uiState.afterPhotoBitmap,
                    enhanceSettings = uiState.enhanceSettings,
                    recentJobs = historyJobs,
                    searchQuery = uiState.searchQuery,
                    availableStorageText = uiState.availableStorageText,
                    statusBannerMessage = uiState.statusBannerMessage,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onDismissBanner = viewModel::dismissStatusBanner,
                    onOpenVideoEnhancer = { navController.navigate(EliteRoutes.VIDEO_ENHANCER) },
                    onOpenPhotoEnhancer = { navController.navigate(EliteRoutes.PHOTO_ENHANCER) },
                    onOpenVideoCompressor = { navController.navigate(EliteRoutes.VIDEO_COMPRESSOR) },
                    onOpenPhotoCompressor = { navController.navigate(EliteRoutes.PHOTO_COMPRESSOR) },
                    onSelectGalleryMediaItem = viewModel::selectGalleryItem,
                    onPickVideoFromSystem = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onPickPhotoFromSystem = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onPickDocumentFiles = {
                        documentPickerLauncher.launch(arrayOf("image/*", "video/*"))
                    },
                    onRequestAllPermissions = requestAllPermissions,
                    onViewAllHistory = { navController.navigate(EliteRoutes.HISTORY) },
                    onOpenSettings = { navController.navigate(EliteRoutes.SETTINGS) },
                    onOpenWelcomeReview = { navController.navigate(EliteRoutes.WELCOME) },
                    onOpenJobFile = { job -> viewModel.openExportedFile(context, job) },
                    onShareJobFile = { job -> viewModel.shareExportedFile(context, job) },
                    onDeleteJob = { job -> viewModel.deleteHistoryJob(job, deleteOutputFile = true) }
                )
            }

            composable(EliteRoutes.VIDEO_ENHANCER) {
                VideoEnhancerScreen(
                    sourceInfo = uiState.activeVideoSource,
                    beforeBitmap = uiState.beforeVideoFrameBitmap,
                    afterBitmap = uiState.afterVideoFrameBitmap,
                    settings = uiState.enhanceSettings,
                    isHevcSupported = uiState.isHevcSupported,
                    isVp9Supported = uiState.isVp9Supported,
                    onBack = { navController.popBackStack() },
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onPresetSelected = viewModel::applyEnhancePreset,
                    onSettingsChange = viewModel::updateEnhancementSettings,
                    onResetSettings = viewModel::resetEnhancementSettings,
                    onExportVideo = {
                        viewModel.executeOperation(
                            operationType = MediaOperationType.VIDEO_ENHANCE,
                            onNavigateToProcessing = { navController.navigate(EliteRoutes.PROCESSING) },
                            onNavigateToResult = {
                                navController.navigate(EliteRoutes.EXPORT_RESULT) {
                                    popUpTo(EliteRoutes.PROCESSING) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(EliteRoutes.PHOTO_ENHANCER) {
                PhotoEnhancerScreen(
                    sourceInfo = uiState.activePhotoSource,
                    beforeBitmap = uiState.beforePhotoBitmap,
                    afterBitmap = uiState.afterPhotoBitmap,
                    settings = uiState.enhanceSettings,
                    onBack = { navController.popBackStack() },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPresetSelected = viewModel::applyEnhancePreset,
                    onSettingsChange = viewModel::updateEnhancementSettings,
                    onResetSettings = viewModel::resetEnhancementSettings,
                    onExportPhoto = {
                        viewModel.executeOperation(
                            operationType = MediaOperationType.PHOTO_ENHANCE,
                            onNavigateToProcessing = { navController.navigate(EliteRoutes.PROCESSING) },
                            onNavigateToResult = {
                                navController.navigate(EliteRoutes.EXPORT_RESULT) {
                                    popUpTo(EliteRoutes.PROCESSING) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(EliteRoutes.VIDEO_COMPRESSOR) {
                VideoCompressorScreen(
                    sourceInfo = uiState.activeVideoSource,
                    videoThumbnailBitmap = uiState.beforeVideoFrameBitmap,
                    settings = uiState.videoCompressSettings,
                    estimatedBytes = uiState.estimatedVideoCompressedBytes,
                    isHevcSupported = uiState.isHevcSupported,
                    isVp9Supported = uiState.isVp9Supported,
                    onBack = { navController.popBackStack() },
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onPresetSelected = viewModel::selectVideoCompressionPreset,
                    onSettingsChange = viewModel::updateVideoCompressionSettings,
                    onCompressVideo = {
                        viewModel.executeOperation(
                            operationType = MediaOperationType.VIDEO_COMPRESS,
                            onNavigateToProcessing = { navController.navigate(EliteRoutes.PROCESSING) },
                            onNavigateToResult = {
                                navController.navigate(EliteRoutes.EXPORT_RESULT) {
                                    popUpTo(EliteRoutes.PROCESSING) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(EliteRoutes.PHOTO_COMPRESSOR) {
                PhotoCompressorScreen(
                    sourceInfo = uiState.activePhotoSource,
                    previewBitmap = uiState.beforePhotoBitmap,
                    settings = uiState.photoCompressSettings,
                    estimatedBytes = uiState.estimatedPhotoCompressedBytes,
                    onBack = { navController.popBackStack() },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPresetSelected = viewModel::selectPhotoCompressionPreset,
                    onSettingsChange = viewModel::updatePhotoCompressionSettings,
                    onCompressPhoto = {
                        viewModel.executeOperation(
                            operationType = MediaOperationType.PHOTO_COMPRESS,
                            onNavigateToProcessing = { navController.navigate(EliteRoutes.PROCESSING) },
                            onNavigateToResult = {
                                navController.navigate(EliteRoutes.EXPORT_RESULT) {
                                    popUpTo(EliteRoutes.PROCESSING) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(EliteRoutes.PROCESSING) {
                ProcessingScreen(
                    progress = uiState.processingProgress,
                    previewBitmap = uiState.afterPhotoBitmap ?: uiState.afterVideoFrameBitmap ?: uiState.beforePhotoBitmap ?: uiState.beforeVideoFrameBitmap,
                    onCancelProcessing = viewModel::cancelActiveProcessing,
                    onReturnToEditor = { navController.popBackStack() }
                )
            }

            composable(EliteRoutes.EXPORT_RESULT) {
                ExportResultScreen(
                    job = uiState.lastCompletedJob,
                    previewBitmap = uiState.afterPhotoBitmap ?: uiState.afterVideoFrameBitmap ?: uiState.beforePhotoBitmap ?: uiState.beforeVideoFrameBitmap,
                    onOpenFile = { job -> viewModel.openExportedFile(context, job) },
                    onShareFile = { job -> viewModel.shareExportedFile(context, job) },
                    onProcessAnother = { navController.popBackStack() },
                    onBackToHome = {
                        navController.navigate(EliteRoutes.HOME) {
                            popUpTo(EliteRoutes.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(EliteRoutes.HISTORY) {
                HistoryScreen(
                    jobs = historyJobs,
                    onNavigateHome = {
                        navController.navigate(EliteRoutes.HOME) {
                            popUpTo(EliteRoutes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateSettings = { navController.navigate(EliteRoutes.SETTINGS) },
                    onOpenFile = { job -> viewModel.openExportedFile(context, job) },
                    onShareFile = { job -> viewModel.shareExportedFile(context, job) },
                    onDeleteJob = { job -> viewModel.deleteHistoryJob(job, deleteOutputFile = true) },
                    onClearAll = viewModel::clearAllHistory
                )
            }

            composable(EliteRoutes.SETTINGS) {
                SettingsScreen(
                    preferences = preferences,
                    availableStorageText = uiState.availableStorageText,
                    historyCount = historyJobs.size,
                    onNavigateHome = {
                        navController.navigate(EliteRoutes.HOME) {
                            popUpTo(EliteRoutes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateHistory = { navController.navigate(EliteRoutes.HISTORY) },
                    onThemeModeChanged = viewModel::setThemeMode,
                    onDefaultPhotoQualityChanged = viewModel::setDefaultPhotoQuality,
                    onDefaultVideoPresetChanged = viewModel::setDefaultVideoPreset,
                    onPreserveMetadataChanged = viewModel::setPreserveMetadata,
                    onClearHistory = viewModel::clearAllHistory
                )
            }
        }
    }
}
