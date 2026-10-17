package com.example.feature

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.media.CompressionPreset
import com.example.core.media.DevicePermissionsStatus
import com.example.core.media.EnhancePreset
import com.example.core.media.EnhancementSettings
import com.example.core.media.MediaInspector
import com.example.core.media.MediaSourceInfo
import com.example.core.media.PhotoCompressionSettings
import com.example.core.media.PhotoProcessor
import com.example.core.media.ProcessingProgress
import com.example.core.media.VideoCompressionSettings
import com.example.core.media.VideoFormatOption
import com.example.core.media.VideoProcessor
import com.example.core.storage.EliteDatabase
import com.example.core.storage.ElitePreferencesState
import com.example.core.storage.JobStatus
import com.example.core.storage.MediaJobEntity
import com.example.core.storage.MediaJobRepository
import com.example.core.storage.MediaOperationType
import com.example.core.storage.UserPreferencesRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class EliteUiState(
    val permissionsStatus: DevicePermissionsStatus = DevicePermissionsStatus(),
    val deviceGalleryMedia: List<MediaSourceInfo> = emptyList(),
    val activeVideoSource: MediaSourceInfo? = null,
    val activePhotoSource: MediaSourceInfo? = null,
    val batchQueue: List<MediaSourceInfo> = emptyList(),
    val beforeVideoFrameBitmap: Bitmap? = null,
    val afterVideoFrameBitmap: Bitmap? = null,
    val beforePhotoBitmap: Bitmap? = null,
    val afterPhotoBitmap: Bitmap? = null,
    val enhanceSettings: EnhancementSettings = EnhancementSettings(),
    val videoCompressSettings: VideoCompressionSettings = VideoCompressionSettings(),
    val photoCompressSettings: PhotoCompressionSettings = PhotoCompressionSettings(),
    val estimatedPhotoCompressedBytes: Long = 0L,
    val estimatedVideoCompressedBytes: Long = 0L,
    val processingProgress: ProcessingProgress = ProcessingProgress(),
    val lastCompletedJob: MediaJobEntity? = null,
    val searchQuery: String = "",
    val isHevcSupported: Boolean = false,
    val isVp9Supported: Boolean = false,
    val availableStorageText: String = "Device storage ready",
    val statusBannerMessage: String? = null
)

class EliteViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = EliteDatabase.getInstance(context)
    private val jobRepository = MediaJobRepository(database.mediaJobDao())
    private val prefsRepository = UserPreferencesRepository(context)

    val preferences: StateFlow<ElitePreferencesState> = prefsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ElitePreferencesState()
        )

    val historyJobs: StateFlow<List<MediaJobEntity>> = jobRepository.allJobs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(EliteUiState())
    val uiState: StateFlow<EliteUiState> = _uiState.asStateFlow()

    private var activeProcessingJob: Job? = null
    private var previewRenderJob: Job? = null

    init {
        createNotificationChannel()
        refreshPermissionsAndDeviceGallery()
    }

    /**
     * Checks all runtime permissions (Notifications, Gallery Images, Gallery Videos) and loads real media from MediaStore.
     */
    fun refreshPermissionsAndDeviceGallery() {
        viewModelScope.launch {
            val perms = MediaInspector.checkPermissionsStatus(context)
            val hevc = MediaInspector.isHevcSupported()
            val vp9 = MediaInspector.isVp9Supported()
            val storageText = MediaInspector.getAvailableStorageFormatted()

            val galleryItems = withContext(Dispatchers.IO) {
                MediaInspector.queryDeviceGalleryMedia(context, limit = 30)
            }

            _uiState.update { state ->
                state.copy(
                    permissionsStatus = perms,
                    deviceGalleryMedia = galleryItems,
                    isHevcSupported = hevc,
                    isVp9Supported = vp9,
                    availableStorageText = storageText
                )
            }

            // Automatically load real device media if none is active yet and gallery has items
            val currentState = _uiState.value
            if (currentState.activeVideoSource == null) {
                galleryItems.firstOrNull { it.isVideo }?.let { firstVideo ->
                    loadRealVideoSource(firstVideo)
                }
            }
            if (currentState.activePhotoSource == null) {
                galleryItems.firstOrNull { !it.isVideo }?.let { firstPhoto ->
                    loadRealPhotoSource(firstPhoto)
                }
            }
        }
    }

    fun selectGalleryItem(item: MediaSourceInfo) {
        viewModelScope.launch {
            if (item.isVideo) {
                loadRealVideoSource(item)
            } else {
                loadRealPhotoSource(item)
            }
        }
    }

    fun selectMediaUris(uris: List<Uri>, forceVideo: Boolean?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val inspected = withContext(Dispatchers.IO) {
                uris.mapNotNull { MediaInspector.inspectUri(context, it, forceVideo) }
            }
            if (inspected.isEmpty()) {
                _uiState.update { it.copy(statusBannerMessage = "Unable to read the selected file.") }
                return@launch
            }
            val primary = inspected.first()
            _uiState.update {
                it.copy(
                    batchQueue = inspected,
                    statusBannerMessage = if (inspected.size > 1) {
                        "${inspected.size} real media files queued for batch processing"
                    } else {
                        "Loaded ${primary.filename} (${primary.formattedSize})"
                    }
                )
            }
            if (primary.isVideo) {
                loadRealVideoSource(primary)
            } else {
                loadRealPhotoSource(primary)
            }
        }
    }

    private suspend fun loadRealVideoSource(videoInfo: MediaSourceInfo) {
        val rawFrame = withContext(Dispatchers.IO) {
            PhotoProcessor.extractVideoFrameBitmap(
                context = context,
                uri = videoInfo.uri,
                timeUs = (videoInfo.durationMs * 250L).coerceAtLeast(0L),
                maxDimension = 1280
            )
        }
        val enhancedFrame = if (rawFrame != null) {
            withContext(Dispatchers.Default) {
                PhotoProcessor.processEnhancedBitmap(rawFrame, _uiState.value.enhanceSettings, isPreviewPass = true)
            }
        } else null

        val estCompressed = VideoProcessor.estimateVideoCompressedSize(
            videoInfo,
            _uiState.value.videoCompressSettings
        )

        _uiState.update {
            it.copy(
                activeVideoSource = videoInfo,
                beforeVideoFrameBitmap = rawFrame,
                afterVideoFrameBitmap = enhancedFrame,
                estimatedVideoCompressedBytes = estCompressed
            )
        }
    }

    private suspend fun loadRealPhotoSource(photoInfo: MediaSourceInfo) {
        val decoded = withContext(Dispatchers.IO) {
            PhotoProcessor.decodeRealMediaBitmap(
                context = context,
                uri = photoInfo.uri,
                isVideo = false,
                maxDimension = 1440
            )
        }
        val enhanced = if (decoded != null) {
            withContext(Dispatchers.Default) {
                PhotoProcessor.processEnhancedBitmap(decoded, _uiState.value.enhanceSettings, isPreviewPass = true)
            }
        } else null

        val estPhoto = if (decoded != null) {
            PhotoProcessor.estimateCompressedPhotoSize(
                decoded,
                photoInfo.sizeBytes,
                _uiState.value.photoCompressSettings
            )
        } else 0L

        _uiState.update {
            it.copy(
                activePhotoSource = photoInfo,
                beforePhotoBitmap = decoded,
                afterPhotoBitmap = enhanced,
                estimatedPhotoCompressedBytes = estPhoto
            )
        }
    }

    fun updateEnhancementSettings(transform: (EnhancementSettings) -> EnhancementSettings) {
        val updated = transform(_uiState.value.enhanceSettings)
        _uiState.update { it.copy(enhanceSettings = updated) }
        refreshEnhancedPreviews(updated)
    }

    fun applyEnhancePreset(preset: EnhancePreset) {
        val current = _uiState.value.enhanceSettings
        val updated = current.copy(
            preset = preset,
            sharpness = preset.sharpness,
            brightness = preset.brightness,
            contrast = preset.contrast,
            saturation = preset.saturation,
            temperature = preset.temperature,
            noiseReduction = preset.noiseReduction
        )
        _uiState.update { it.copy(enhanceSettings = updated) }
        refreshEnhancedPreviews(updated)
    }

    fun resetEnhancementSettings() {
        applyEnhancePreset(EnhancePreset.NATURAL)
    }

    private fun refreshEnhancedPreviews(settings: EnhancementSettings) {
        previewRenderJob?.cancel()
        previewRenderJob = viewModelScope.launch {
            val state = _uiState.value
            val updatedPhoto = state.beforePhotoBitmap?.let {
                PhotoProcessor.processEnhancedBitmap(it, settings, isPreviewPass = true)
            }
            val updatedVideoFrame = state.beforeVideoFrameBitmap?.let {
                PhotoProcessor.processEnhancedBitmap(it, settings, isPreviewPass = true)
            }
            _uiState.update {
                it.copy(
                    afterPhotoBitmap = updatedPhoto ?: it.afterPhotoBitmap,
                    afterVideoFrameBitmap = updatedVideoFrame ?: it.afterVideoFrameBitmap
                )
            }
        }
    }

    fun updateVideoCompressionSettings(transform: (VideoCompressionSettings) -> VideoCompressionSettings) {
        val current = _uiState.value.videoCompressSettings
        val updated = transform(current)
        val source = _uiState.value.activeVideoSource
        val est = if (source != null) {
            VideoProcessor.estimateVideoCompressedSize(source, updated)
        } else 0L
        _uiState.update {
            it.copy(
                videoCompressSettings = updated,
                estimatedVideoCompressedBytes = est
            )
        }
    }

    fun selectVideoCompressionPreset(preset: CompressionPreset) {
        val baseBitrate = when (preset) {
            CompressionPreset.SMALLER -> 1400
            CompressionPreset.BALANCED -> 3200
            CompressionPreset.QUALITY -> 6800
        }
        updateVideoCompressionSettings {
            it.copy(
                preset = preset,
                resolutionScale = preset.scaleFactor,
                targetBitrateKbps = baseBitrate
            )
        }
    }

    fun updatePhotoCompressionSettings(transform: (PhotoCompressionSettings) -> PhotoCompressionSettings) {
        val updated = transform(_uiState.value.photoCompressSettings)
        _uiState.update { it.copy(photoCompressSettings = updated) }
        viewModelScope.launch {
            val bmp = _uiState.value.beforePhotoBitmap ?: return@launch
            val srcSize = _uiState.value.activePhotoSource?.sizeBytes ?: 0L
            val est = PhotoProcessor.estimateCompressedPhotoSize(bmp, srcSize, updated)
            _uiState.update { it.copy(estimatedPhotoCompressedBytes = est) }
        }
    }

    fun selectPhotoCompressionPreset(preset: CompressionPreset) {
        updatePhotoCompressionSettings {
            it.copy(
                preset = preset,
                quality = preset.photoQuality,
                resolutionScale = preset.scaleFactor.coerceAtLeast(0.5f)
            )
        }
    }

    fun executeOperation(
        operationType: MediaOperationType,
        onNavigateToProcessing: () -> Unit,
        onNavigateToResult: () -> Unit
    ) {
        activeProcessingJob?.cancel()

        val state = _uiState.value
        val itemsToProcess: List<MediaSourceInfo> = when (operationType) {
            MediaOperationType.VIDEO_ENHANCE, MediaOperationType.VIDEO_COMPRESS -> {
                val batchVideos = state.batchQueue.filter { it.isVideo }
                if (batchVideos.isNotEmpty()) batchVideos else listOfNotNull(state.activeVideoSource)
            }
            MediaOperationType.PHOTO_ENHANCE, MediaOperationType.PHOTO_COMPRESS -> {
                val batchPhotos = state.batchQueue.filter { !it.isVideo }
                if (batchPhotos.isNotEmpty()) batchPhotos else listOfNotNull(state.activePhotoSource)
            }
        }

        if (itemsToProcess.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "Please select a real photo or video from your Gallery or Files first.")
            }
            return
        }

        val totalRequiredBytes = itemsToProcess.sumOf { it.sizeBytes }
        if (!MediaInspector.hasEnoughStorage(totalRequiredBytes)) {
            _uiState.update {
                it.copy(statusBannerMessage = "Insufficient device storage to complete export safely.")
            }
            return
        }

        onNavigateToProcessing()

        activeProcessingJob = viewModelScope.launch {
            var latestCompletedEntity: MediaJobEntity? = null
            val batchTotal = itemsToProcess.size

            for ((index, sourceItem) in itemsToProcess.withIndex()) {
                val batchNum = index + 1
                _uiState.update {
                    it.copy(
                        processingProgress = ProcessingProgress(
                            isRunning = true,
                            filename = sourceItem.filename,
                            stageLabel = "Initializing ${operationType.displayLabel}...",
                            progressFraction = 0.04f,
                            processedItems = 0,
                            totalItems = 100,
                            isBatch = batchTotal > 1,
                            batchIndex = batchNum,
                            batchCount = batchTotal
                        )
                    )
                }

                try {
                    val createdAt = System.currentTimeMillis()
                    if (sourceItem.isVideo) {
                        val isEnhance = operationType == MediaOperationType.VIDEO_ENHANCE
                        val result = VideoProcessor.processVideo(
                            context = context,
                            source = sourceItem,
                            enhanceSettings = if (isEnhance) state.enhanceSettings else null,
                            compressSettings = if (!isEnhance) state.videoCompressSettings else null,
                            onProgress = { frac, done, total, stage ->
                                _uiState.update { current ->
                                    current.copy(
                                        processingProgress = current.processingProgress.copy(
                                            progressFraction = frac,
                                            processedItems = done,
                                            totalItems = total,
                                            stageLabel = stage
                                        )
                                    )
                                }
                            }
                        )
                        val summary = if (isEnhance) {
                            "${state.enhanceSettings.preset.label} • ${state.enhanceSettings.videoFormat.label} • ${state.enhanceSettings.videoResolution.label}"
                        } else {
                            "${state.videoCompressSettings.preset.label} • ${state.videoCompressSettings.formatOption.label} • ${state.videoCompressSettings.targetBitrateKbps} kbps"
                        }
                        val jobEntity = MediaJobEntity(
                            operationType = operationType.name,
                            filename = result.localFile.name,
                            sourceUri = sourceItem.uri.toString(),
                            outputUri = result.galleryUri.toString(),
                            sourceSizeBytes = sourceItem.sizeBytes,
                            outputSizeBytes = result.outputSizeBytes,
                            sourceDimensions = sourceItem.resolutionLabel,
                            outputDimensions = result.outputDimensions,
                            durationMs = sourceItem.durationMs,
                            status = JobStatus.COMPLETED.name,
                            createdAt = createdAt,
                            completedAt = System.currentTimeMillis(),
                            settingsSummary = summary
                        )
                        val id = jobRepository.insertJob(jobEntity)
                        latestCompletedEntity = jobEntity.copy(id = id)
                    } else {
                        val isEnhance = operationType == MediaOperationType.PHOTO_ENHANCE
                        val fullBitmap = withContext(Dispatchers.IO) {
                            PhotoProcessor.decodeRealMediaBitmap(
                                context = context,
                                uri = sourceItem.uri,
                                isVideo = false,
                                maxDimension = 4096 // High-resolution processing up to 4K
                            )
                        } ?: throw IllegalStateException("Could not decode source image from URI")

                        val processedBitmap = if (isEnhance) {
                            PhotoProcessor.processEnhancedBitmap(
                                source = fullBitmap,
                                settings = state.enhanceSettings,
                                onProgress = { frac, stage ->
                                    _uiState.update { current ->
                                        current.copy(
                                            processingProgress = current.processingProgress.copy(
                                                progressFraction = frac,
                                                processedItems = (frac * 100).roundToInt(),
                                                totalItems = 100,
                                                stageLabel = stage
                                            )
                                        )
                                    }
                                }
                            )
                        } else {
                            val scale = state.photoCompressSettings.resolutionScale.coerceIn(0.25f, 1.0f)
                            val targetW = (fullBitmap.width * scale).roundToInt().coerceAtLeast(64)
                            val targetH = (fullBitmap.height * scale).roundToInt().coerceAtLeast(64)
                            _uiState.update { current ->
                                current.copy(
                                    processingProgress = current.processingProgress.copy(
                                        progressFraction = 0.55f,
                                        processedItems = 55,
                                        totalItems = 100,
                                        stageLabel = "Resizing and encoding ${state.photoCompressSettings.format.label}..."
                                    )
                                )
                            }
                            withContext(Dispatchers.Default) {
                                Bitmap.createScaledBitmap(fullBitmap, targetW, targetH, true)
                            }
                        }

                        val format = if (isEnhance) state.enhanceSettings.photoFormat else state.photoCompressSettings.format
                        val quality = if (isEnhance) state.enhanceSettings.photoQuality else state.photoCompressSettings.quality
                        val preserveMeta = if (isEnhance) preferences.value.preserveMetadata else !state.photoCompressSettings.removeMetadata

                        val result = PhotoProcessor.exportProcessedPhoto(
                            context = context,
                            sourceInfo = sourceItem,
                            bitmap = processedBitmap,
                            format = format,
                            quality = quality,
                            preserveMetadata = preserveMeta,
                            prefix = if (isEnhance) "ENHANCED" else "COMPRESSED"
                        )

                        val summary = if (isEnhance) {
                            "${state.enhanceSettings.preset.label} • ${format.label} ${if (format.isLossless) "Lossless" else "${quality}%"}"
                        } else {
                            "${state.photoCompressSettings.preset.label} • ${format.label} ${if (format.isLossless) "Lossless" else "${quality}%"}"
                        }
                        val jobEntity = MediaJobEntity(
                            operationType = operationType.name,
                            filename = result.localFile.name,
                            sourceUri = sourceItem.uri.toString(),
                            outputUri = result.galleryUri.toString(),
                            sourceSizeBytes = sourceItem.sizeBytes,
                            outputSizeBytes = result.outputSizeBytes,
                            sourceDimensions = sourceItem.resolutionLabel,
                            outputDimensions = result.outputDimensions,
                            durationMs = 0L,
                            status = JobStatus.COMPLETED.name,
                            createdAt = createdAt,
                            completedAt = System.currentTimeMillis(),
                            settingsSummary = summary
                        )
                        val id = jobRepository.insertJob(jobEntity)
                        latestCompletedEntity = jobEntity.copy(id = id)
                    }
                } catch (ce: CancellationException) {
                    val canceledEntity = MediaJobEntity(
                        operationType = operationType.name,
                        filename = sourceItem.filename,
                        sourceUri = sourceItem.uri.toString(),
                        outputUri = "",
                        sourceSizeBytes = sourceItem.sizeBytes,
                        outputSizeBytes = 0L,
                        sourceDimensions = sourceItem.resolutionLabel,
                        outputDimensions = sourceItem.resolutionLabel,
                        status = JobStatus.CANCELED.name,
                        errorCode = "User canceled operation"
                    )
                    jobRepository.insertJob(canceledEntity)
                    _uiState.update {
                        it.copy(
                            processingProgress = it.processingProgress.copy(
                                isRunning = false,
                                wasCanceled = true,
                                stageLabel = "Processing canceled safely. Temporary buffers cleaned."
                            )
                        )
                    }
                    throw ce
                } catch (e: Exception) {
                    val failedEntity = MediaJobEntity(
                        operationType = operationType.name,
                        filename = sourceItem.filename,
                        sourceUri = sourceItem.uri.toString(),
                        outputUri = "",
                        sourceSizeBytes = sourceItem.sizeBytes,
                        outputSizeBytes = 0L,
                        sourceDimensions = sourceItem.resolutionLabel,
                        outputDimensions = sourceItem.resolutionLabel,
                        status = JobStatus.FAILED.name,
                        errorCode = e.message ?: "Encoding error"
                    )
                    jobRepository.insertJob(failedEntity)
                    _uiState.update {
                        it.copy(
                            processingProgress = it.processingProgress.copy(
                                isRunning = false,
                                errorMessage = e.message ?: "Unsupported format or hardware limitation"
                            )
                        )
                    }
                }
            }

            if (latestCompletedEntity != null) {
                refreshPermissionsAndDeviceGallery()
                _uiState.update {
                    it.copy(
                        lastCompletedJob = latestCompletedEntity,
                        processingProgress = it.processingProgress.copy(
                            isRunning = false,
                            progressFraction = 1f,
                            stageLabel = "Saved into Device Gallery!"
                        ),
                        availableStorageText = MediaInspector.getAvailableStorageFormatted(),
                        statusBannerMessage = "Saved ${latestCompletedEntity.filename} directly to Device Gallery"
                    )
                }
                postCompletionNotification(latestCompletedEntity)
                onNavigateToResult()
            }
        }
    }

    fun cancelActiveProcessing() {
        activeProcessingJob?.cancel()
        _uiState.update {
            it.copy(
                processingProgress = it.processingProgress.copy(
                    isRunning = false,
                    wasCanceled = true,
                    stageLabel = "Processing canceled. Original media untouched."
                )
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun deleteHistoryJob(job: MediaJobEntity, deleteOutputFile: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (deleteOutputFile && job.outputUri.isNotEmpty()) {
                runCatching {
                    val uri = Uri.parse(job.outputUri)
                    if (uri.scheme == "file") {
                        File(uri.path ?: "").delete()
                    } else if (uri.scheme == "content") {
                        context.contentResolver.delete(uri, null, null)
                    }
                }
            }
            jobRepository.deleteJobById(job.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            jobRepository.clearAllJobs()
        }
    }

    fun openExportedFile(context: Context, job: MediaJobEntity) {
        runCatching {
            val uri = Uri.parse(job.outputUri)
            val isVid = job.operationType.contains("VIDEO")
            val mime = if (isVid) "video/*" else "image/*"

            val viewUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewUri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open in Gallery").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(context, "Saved in Gallery: ${job.filename}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareExportedFile(context: Context, job: MediaJobEntity) {
        runCatching {
            val uri = Uri.parse(job.outputUri)
            val isVid = job.operationType.contains("VIDEO")
            val mime = if (isVid) "video/*" else "image/*"

            val shareUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${job.filename}")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, "Unable to launch system share sheet.", Toast.LENGTH_SHORT).show()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun markWelcomeCompleted() {
        viewModelScope.launch { prefsRepository.setHasSeenWelcome(true) }
    }

    fun setDefaultPhotoQuality(quality: Int) {
        viewModelScope.launch { prefsRepository.setDefaultPhotoQuality(quality) }
    }

    fun setDefaultVideoPreset(preset: String) {
        viewModelScope.launch { prefsRepository.setDefaultVideoPreset(preset) }
    }

    fun setPreserveMetadata(preserve: Boolean) {
        viewModelScope.launch { prefsRepository.setPreserveMetadata(preserve) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ELITEMAX Gallery Exports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when enhanced or compressed media is saved to your Gallery"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun postCompletionNotification(job: MediaJobEntity) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Saved to Gallery • ${job.filename}")
                .setContentText("Output: ${com.example.core.media.formatFileSize(job.outputSizeBytes)} (${job.outputDimensions})")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify(job.id.toInt().coerceAtLeast(1001), notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "elitemax_exports"
    }
}
