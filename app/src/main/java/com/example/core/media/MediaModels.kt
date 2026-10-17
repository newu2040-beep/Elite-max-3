package com.example.core.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class MediaSourceInfo(
    val uri: Uri,
    val filename: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
    val bitrateBps: Int = 0,
    val frameRate: Int = 30,
    val mimeType: String = "",
    val hasTransparency: Boolean = false,
    val dateModifiedMillis: Long = System.currentTimeMillis()
) {
    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "$width × $height" else "Unknown"

    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedDate: String
        get() = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(dateModifiedMillis))
}

enum class EnhancePreset(
    val label: String,
    val sharpness: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val noiseReduction: Float
) {
    NATURAL("Natural", 0.32f, 0.04f, 0.12f, 0.14f, 0.02f, 0.25f),
    CLEAR("Clear", 0.58f, 0.06f, 0.28f, 0.22f, 0.0f, 0.45f),
    LOW_LIGHT("Low Light", 0.25f, 0.28f, 0.18f, 0.10f, 0.08f, 0.65f),
    CUSTOM("Custom", 0.32f, 0.0f, 0.15f, 0.15f, 0.0f, 0.30f)
}

enum class VideoResolutionPreset(
    val label: String,
    val scaleFactor: Float,
    val maxLongEdge: Int?
) {
    ORIGINAL("Original", 1.0f, null),
    UHD_4K("4K UHD", 1.0f, 3840),
    QHD_2K("2K QHD", 1.0f, 2560),
    FHD_1080P("1080p", 1.0f, 1920),
    HD_720P("720p", 0.75f, 1280),
    SD_480P("480p", 0.5f, 854),
    CUSTOM("Custom", 1.0f, null)
}

enum class PhotoResolutionMode(
    val label: String,
    val scaleFactor: Float
) {
    ORIGINAL_100("100%", 1.0f),
    SCALE_75("75%", 0.75f),
    SCALE_50("50%", 0.5f),
    CUSTOM("Custom px", 1.0f)
}

enum class VideoFormatOption(
    val label: String,
    val extension: String,
    val containerMime: String,
    val codecMime: String
) {
    MP4_H264_HQ("MP4 (H.264 High)", "mp4", "video/mp4", MediaFormat.MIMETYPE_VIDEO_AVC),
    MP4_HEVC_HQ("MP4 (H.265 HEVC)", "mp4", "video/mp4", MediaFormat.MIMETYPE_VIDEO_HEVC),
    MOV_QUICKTIME_HQ("MOV (QuickTime HQ)", "mov", "video/quicktime", MediaFormat.MIMETYPE_VIDEO_AVC),
    WEBM_VP9_HQ("WebM (VP9 HQ)", "webm", "video/webm", MediaFormat.MIMETYPE_VIDEO_VP9)
}

data class EnhancementSettings(
    val preset: EnhancePreset = EnhancePreset.CLEAR,
    val sharpness: Float = 0.32f,
    val brightness: Float = 0.04f,
    val contrast: Float = 0.28f,
    val saturation: Float = 0.22f,
    val temperature: Float = 0.0f,
    val noiseReduction: Float = 0.45f,
    // Video resolution, FPS, Bitrate & Format controls
    val resolutionScale: Float = 1.0f,
    val videoResolution: VideoResolutionPreset = VideoResolutionPreset.ORIGINAL,
    val customVideoWidth: Int = 1920,
    val customVideoHeight: Int = 1080,
    val videoFrameRate: Int = 30,
    val targetVideoBitrateKbps: Int = 14_000, // 14 Mbps Studio HQ default
    val videoFormat: VideoFormatOption = VideoFormatOption.MP4_H264_HQ,
    // Photo resolution, Crop & Format controls
    val cropAspect: CropAspect = CropAspect.ORIGINAL,
    val photoResolutionMode: PhotoResolutionMode = PhotoResolutionMode.ORIGINAL_100,
    val customPhotoWidth: Int = 1920,
    val customPhotoHeight: Int = 1080,
    val preserveAspectRatio: Boolean = true,
    val photoFormat: PhotoOutputFormat = PhotoOutputFormat.JPEG,
    val photoQuality: Int = 98
) {
    val clarityScore: Int
        get() {
            val base = 68
            val boost = (sharpness * 22f + contrast * 18f + noiseReduction * 12f).roundToInt()
            return (base + boost).coerceIn(50, 99)
        }
}

enum class CropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    WIDE_16_9("16:9", 16f / 9f),
    STANDARD_4_3("4:3", 4f / 3f),
    SQUARE_1_1("1:1", 1f)
}

enum class PhotoOutputFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val supportsTransparency: Boolean,
    val isLossless: Boolean = false
) {
    JPEG("JPEG Ultra HQ", "jpg", "image/jpeg", false, false),
    PNG("PNG Lossless", "png", "image/png", true, true),
    WEBP("WebP HQ", "webp", "image/webp", true, false),
    WEBP_LOSSLESS("WebP Lossless", "webp", "image/webp", true, true)
}

enum class CompressionPreset(
    val label: String,
    val description: String,
    val scaleFactor: Float,
    val bitrateFactor: Float,
    val photoQuality: Int
) {
    SMALLER("Smaller", "Maximum storage savings", 0.5f, 0.35f, 62),
    BALANCED("Balanced", "Smaller size. Same quality.", 0.75f, 0.55f, 80),
    QUALITY("Quality", "High visual fidelity", 1.0f, 0.78f, 92)
}

data class VideoCompressionSettings(
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val resolutionPreset: VideoResolutionPreset = VideoResolutionPreset.FHD_1080P,
    val resolutionScale: Float = 0.75f,
    val customWidth: Int = 1280,
    val customHeight: Int = 720,
    val keepAspectRatio: Boolean = true,
    val targetBitrateKbps: Int = 3200,
    val frameRate: Int = 30,
    val formatOption: VideoFormatOption = VideoFormatOption.MP4_H264_HQ,
    val preserveAudio: Boolean = true
)

data class PhotoCompressionSettings(
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val format: PhotoOutputFormat = PhotoOutputFormat.JPEG,
    val quality: Int = 80,
    val resolutionMode: PhotoResolutionMode = PhotoResolutionMode.ORIGINAL_100,
    val resolutionScale: Float = 0.85f,
    val customWidth: Int = 1440,
    val customHeight: Int = 1080,
    val keepAspectRatio: Boolean = true,
    val removeMetadata: Boolean = false
)

data class ProcessingProgress(
    val isRunning: Boolean = false,
    val filename: String = "",
    val stageLabel: String = "Preparing media...",
    val progressFraction: Float = 0f,
    val processedItems: Int = 0,
    val totalItems: Int = 100,
    val isBatch: Boolean = false,
    val batchIndex: Int = 1,
    val batchCount: Int = 1,
    val errorMessage: String? = null,
    val wasCanceled: Boolean = false
)

data class InbuiltPlayerMediaItem(
    val uri: Uri,
    val title: String,
    val isVideo: Boolean,
    val resolutionLabel: String = "",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val formatSummary: String = ""
)

data class DevicePermissionsStatus(
    val hasNotificationsPermission: Boolean = false,
    val hasGalleryImagesPermission: Boolean = false,
    val hasGalleryVideoPermission: Boolean = false,
    val hasPartialMediaAccess: Boolean = false
) {
    val hasAnyGalleryAccess: Boolean
        get() = hasGalleryImagesPermission || hasGalleryVideoPermission || hasPartialMediaAccess

    val allGranted: Boolean
        get() = hasNotificationsPermission && hasGalleryImagesPermission && hasGalleryVideoPermission
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return String.format(Locale.US, "%.1f KB", kb)
    }
    val mb = kb / 1024.0
    return if (mb >= 100.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.2f MB", mb)
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

object MediaInspector {

    fun checkPermissionsStatus(context: Context): DevicePermissionsStatus {
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val hasVideos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        return DevicePermissionsStatus(
            hasNotificationsPermission = hasNotif,
            hasGalleryImagesPermission = hasImages,
            hasGalleryVideoPermission = hasVideos,
            hasPartialMediaAccess = hasPartial
        )
    }

    fun getRequiredRuntimePermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return list.toTypedArray()
    }

    fun hasEnoughStorage(requiredBytes: Long): Boolean {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes > (requiredBytes * 2L).coerceAtLeast(15L * 1024L * 1024L)
        }.getOrDefault(true)
    }

    fun getAvailableStorageFormatted(): String {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val gb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB free on device", gb)
        }.getOrDefault("Device storage ready")
    }

    fun isHevcSupported(): Boolean {
        return isEncoderSupported(MediaFormat.MIMETYPE_VIDEO_HEVC)
    }

    fun isVp9Supported(): Boolean {
        return isEncoderSupported(MediaFormat.MIMETYPE_VIDEO_VP9)
    }

    fun isEncoderSupported(mimeType: String): Boolean {
        return runCatching {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            list.codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any {
                    it.equals(mimeType, ignoreCase = true)
                }
            }
        }.getOrDefault(false)
    }

    fun queryDeviceGalleryMedia(context: Context, limit: Int = 24): List<MediaSourceInfo> {
        val status = checkPermissionsStatus(context)
        if (!status.hasAnyGalleryAccess) return emptyList()

        val results = mutableListOf<MediaSourceInfo>()
        val resolver = context.contentResolver

        // 1. Query Real Device Videos
        runCatching {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_MODIFIED
            )
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                var count = 0
                while (cursor.moveToNext() && count < limit / 2) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val size = cursor.getLong(sizeCol)
                    if (size > 0L) {
                        results.add(
                            MediaSourceInfo(
                                uri = contentUri,
                                filename = cursor.getString(nameCol) ?: "Video_$id.mp4",
                                isVideo = true,
                                sizeBytes = size,
                                width = cursor.getInt(wCol).coerceAtLeast(0),
                                height = cursor.getInt(hCol).coerceAtLeast(0),
                                durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                                mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                                dateModifiedMillis = cursor.getLong(dateCol) * 1000L
                            )
                        )
                        count++
                    }
                }
            }
        }

        // 2. Query Real Device Photos
        runCatching {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                var count = 0
                while (cursor.moveToNext() && count < limit / 2) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    if (size > 0L) {
                        results.add(
                            MediaSourceInfo(
                                uri = contentUri,
                                filename = cursor.getString(nameCol) ?: "Photo_$id.jpg",
                                isVideo = false,
                                sizeBytes = size,
                                width = cursor.getInt(wCol).coerceAtLeast(0),
                                height = cursor.getInt(hCol).coerceAtLeast(0),
                                mimeType = mime,
                                hasTransparency = mime.contains("png", true) || mime.contains("webp", true),
                                dateModifiedMillis = cursor.getLong(dateCol) * 1000L
                            )
                        )
                        count++
                    }
                }
            }
        }

        return results.sortedByDescending { it.dateModifiedMillis }
    }

    fun inspectUri(context: Context, uri: Uri, forceVideo: Boolean? = null): MediaSourceInfo? {
        return runCatching {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                if (!file.exists()) return null
                val isVid = forceVideo ?: (file.extension.lowercase() in listOf("mp4", "mkv", "mov", "webm", "3gp"))
                return inspectFile(file, isVid)
            }

            val cr = context.contentResolver
            val mime = cr.getType(uri) ?: ""
            val isVideo = forceVideo ?: mime.startsWith("video/")
            var displayName = ""
            var sizeBytes = 0L

            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        displayName = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (displayName.isEmpty()) {
                displayName = uri.lastPathSegment ?: if (isVideo) "video.mp4" else "photo.jpg"
            }

            if (sizeBytes <= 0L) {
                cr.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0) sizeBytes = afd.length
                }
            }

            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                var width = 0
                var height = 0
                var durationMs = 0L
                var bitrate = 0
                var frameRate = 30
                try {
                    retriever.setDataSource(context, uri)
                    width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
                    frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.roundToInt() ?: 30
                } finally {
                    runCatching { retriever.release() }
                }
                MediaSourceInfo(
                    uri = uri,
                    filename = displayName,
                    isVideo = true,
                    sizeBytes = sizeBytes,
                    width = width,
                    height = height,
                    durationMs = durationMs,
                    bitrateBps = bitrate,
                    frameRate = frameRate.coerceIn(15, 120),
                    mimeType = mime.ifEmpty { "video/mp4" }
                )
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val hasAlpha = mime.contains("png", ignoreCase = true) || mime.contains("webp", ignoreCase = true)
                MediaSourceInfo(
                    uri = uri,
                    filename = displayName,
                    isVideo = false,
                    sizeBytes = sizeBytes,
                    width = opts.outWidth.coerceAtLeast(0),
                    height = opts.outHeight.coerceAtLeast(0),
                    mimeType = mime.ifEmpty { "image/jpeg" },
                    hasTransparency = hasAlpha
                )
            }
        }.getOrNull()
    }

    fun inspectFile(file: File, isVideo: Boolean): MediaSourceInfo {
        return if (isVideo) {
            var width = 0
            var height = 0
            var durationMs = 0L
            var bitrate = 0
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
            MediaSourceInfo(
                uri = Uri.fromFile(file),
                filename = file.name,
                isVideo = true,
                sizeBytes = file.length(),
                width = width,
                height = height,
                durationMs = durationMs,
                bitrateBps = bitrate,
                frameRate = 30,
                mimeType = when (file.extension.lowercase()) {
                    "webm" -> "video/webm"
                    "mov" -> "video/quicktime"
                    else -> "video/mp4"
                },
                dateModifiedMillis = file.lastModified()
            )
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            MediaSourceInfo(
                uri = Uri.fromFile(file),
                filename = file.name,
                isVideo = false,
                sizeBytes = file.length(),
                width = opts.outWidth,
                height = opts.outHeight,
                mimeType = when (file.extension.lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                },
                hasTransparency = file.extension.lowercase() in listOf("png", "webp"),
                dateModifiedMillis = file.lastModified()
            )
        }
    }
}
