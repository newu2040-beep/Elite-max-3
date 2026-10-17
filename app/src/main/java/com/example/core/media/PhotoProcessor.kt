package com.example.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

data class ExportedMediaOutput(
    val localFile: File,
    val galleryUri: Uri,
    val outputDimensions: String,
    val outputSizeBytes: Long
)

object PhotoProcessor {

    fun decodeRealMediaBitmap(
        context: Context,
        uri: Uri?,
        isVideo: Boolean = false,
        maxDimension: Int = 2048
    ): Bitmap? {
        if (uri == null) return null
        if (isVideo) {
            return extractVideoFrameBitmap(context, uri, timeUs = 500_000L, maxDimension = maxDimension)
        }
        return runCatching {
            if (uri.scheme == "file") {
                val path = uri.path ?: return null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, opts)
                opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDimension)
                opts.inJustDecodeBounds = false
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeFile(path, opts)
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDimension)
                opts.inJustDecodeBounds = false
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }
        }.getOrNull()
    }

    fun extractVideoFrameBitmap(
        context: Context,
        uri: Uri,
        timeUs: Long = 0L,
        maxDimension: Int = 1920
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxDimension,
                    maxDimension
                ) ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            rawFrame?.copy(Bitmap.Config.ARGB_8888, true)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun decodeSampledBitmap(context: Context, uri: Uri?, maxDimension: Int = 2048): Bitmap? {
        return decodeRealMediaBitmap(context, uri, isVideo = false, maxDimension = maxDimension)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        if (height > maxDimension || width > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= maxDimension || (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun resolvePhotoTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        mode: PhotoResolutionMode,
        scaleFactor: Float,
        customWidth: Int,
        customHeight: Int,
        preserveAspect: Boolean
    ): Pair<Int, Int> {
        val w0 = srcWidth.coerceAtLeast(64)
        val h0 = srcHeight.coerceAtLeast(64)
        return if (mode == PhotoResolutionMode.CUSTOM) {
            val cw = customWidth.coerceIn(64, 8192)
            val ch = if (preserveAspect && w0 > 0) {
                ((cw.toFloat() / w0.toFloat()) * h0.toFloat()).roundToInt().coerceIn(64, 8192)
            } else {
                customHeight.coerceIn(64, 8192)
            }
            Pair(cw, ch)
        } else {
            val s = scaleFactor.coerceIn(0.15f, 1.0f)
            Pair(
                (w0 * s).roundToInt().coerceAtLeast(64),
                (h0 * s).roundToInt().coerceAtLeast(64)
            )
        }
    }

    fun buildEnhancementColorMatrix(settings: EnhancementSettings): ColorMatrix {
        val combined = ColorMatrix()

        val satMatrix = ColorMatrix().apply {
            setSaturation((1f + settings.saturation).coerceIn(0f, 2.5f))
        }
        combined.postConcat(satMatrix)

        val c = (1f + settings.contrast).coerceIn(0.4f, 2.2f)
        val bOffset = settings.brightness * 180f + (1f - c) * 128f
        val cbMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, bOffset,
                0f, c, 0f, 0f, bOffset,
                0f, 0f, c, 0f, bOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combined.postConcat(cbMatrix)

        val temp = settings.temperature
        if (kotlin.math.abs(temp) > 0.005f) {
            val rScale = (1f + temp * 0.22f).coerceIn(0.75f, 1.25f)
            val gScale = (1f + temp * 0.04f).coerceIn(0.90f, 1.10f)
            val bScale = (1f - temp * 0.24f).coerceIn(0.75f, 1.25f)
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    rScale, 0f, 0f, 0f, 0f,
                    0f, gScale, 0f, 0f, 0f,
                    0f, 0f, bScale, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combined.postConcat(tempMatrix)
        }

        return combined
    }

    suspend fun processEnhancedBitmap(
        source: Bitmap,
        settings: EnhancementSettings,
        isPreviewPass: Boolean = false,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Bitmap = withContext(Dispatchers.Default) {
        coroutineContext.ensureActive()
        onProgress(0.12f, "Applying crop & custom resolution geometry...")

        val cropped = applyCrop(source, settings.cropAspect)

        val (targetW, targetH) = if (isPreviewPass) {
            val scale = settings.resolutionScale.coerceIn(0.25f, 1.0f)
            Pair(
                (cropped.width * scale).roundToInt().coerceAtLeast(64),
                (cropped.height * scale).roundToInt().coerceAtLeast(64)
            )
        } else {
            resolvePhotoTargetDimensions(
                srcWidth = cropped.width,
                srcHeight = cropped.height,
                mode = settings.photoResolutionMode,
                scaleFactor = settings.resolutionScale,
                customWidth = settings.customPhotoWidth,
                customHeight = settings.customPhotoHeight,
                preserveAspect = settings.preserveAspectRatio
            )
        }

        val working = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(working)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildEnhancementColorMatrix(settings))
        }
        val dstRect = android.graphics.Rect(0, 0, targetW, targetH)
        canvas.drawBitmap(cropped, null, dstRect, paint)

        coroutineContext.ensureActive()
        onProgress(0.45f, "Running edge-preserving noise reduction...")

        val pixels = IntArray(targetW * targetH)
        working.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        val denoised = if (settings.noiseReduction > 0.04f) {
            applyEdgePreservingDenoise(pixels, targetW, targetH, settings.noiseReduction)
        } else {
            pixels
        }

        coroutineContext.ensureActive()
        onProgress(0.76f, "Applying unsharp mask clarity kernel...")

        val sharpened = if (settings.sharpness > 0.04f) {
            applySharpenKernel(denoised, targetW, targetH, settings.sharpness)
        } else {
            denoised
        }

        coroutineContext.ensureActive()
        working.setPixels(sharpened, 0, targetW, 0, 0, targetW, targetH)
        onProgress(0.95f, "Finalizing high-bit-depth image buffer...")
        working
    }

    private fun applyCrop(src: Bitmap, cropAspect: CropAspect): Bitmap {
        val ratio = cropAspect.ratio ?: return src
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val currentRatio = srcW / srcH
        if (kotlin.math.abs(currentRatio - ratio) < 0.02f) return src

        val cropW: Int
        val cropH: Int
        if (currentRatio > ratio) {
            cropH = src.height
            cropW = (srcH * ratio).roundToInt().coerceAtMost(src.width)
        } else {
            cropW = src.width
            cropH = (srcW / ratio).roundToInt().coerceAtMost(src.height)
        }
        val x = ((src.width - cropW) / 2).coerceAtLeast(0)
        val y = ((src.height - cropH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(src, x, y, cropW, cropH)
    }

    private fun applyEdgePreservingDenoise(src: IntArray, w: Int, h: Int, strength: Float): IntArray {
        val out = src.copyOf()
        val blend = (strength * 0.65f).coerceIn(0f, 0.75f)
        val threshold = (18 + (strength * 36).toInt())

        for (y in 1 until h - 1) {
            val rowOffset = y * w
            for (x in 1 until w - 1) {
                val idx = rowOffset + x
                val c = src[idx]
                val a = (c ushr 24) and 0xFF
                val r0 = (c ushr 16) and 0xFF
                val g0 = (c ushr 8) and 0xFF
                val b0 = c and 0xFF

                var rSum = r0 * 2
                var gSum = g0 * 2
                var bSum = b0 * 2
                var weight = 2

                val neighbors = intArrayOf(
                    src[idx - 1], src[idx + 1],
                    src[idx - w], src[idx + w]
                )
                for (n in neighbors) {
                    val nr = (n ushr 16) and 0xFF
                    val ng = (n ushr 8) and 0xFF
                    val nb = n and 0xFF
                    val diff = kotlin.math.abs(nr - r0) + kotlin.math.abs(ng - g0) + kotlin.math.abs(nb - b0)
                    if (diff < threshold * 3) {
                        rSum += nr
                        gSum += ng
                        bSum += nb
                        weight += 1
                    }
                }
                val rAvg = rSum / weight
                val gAvg = gSum / weight
                val bAvg = bSum / weight

                val rf = (r0 * (1f - blend) + rAvg * blend).toInt().coerceIn(0, 255)
                val gf = (g0 * (1f - blend) + gAvg * blend).toInt().coerceIn(0, 255)
                val bf = (b0 * (1f - blend) + bAvg * blend).toInt().coerceIn(0, 255)
                out[idx] = (a shl 24) or (rf shl 16) or (gf shl 8) or bf
            }
        }
        return out
    }

    private fun applySharpenKernel(src: IntArray, w: Int, h: Int, amount: Float): IntArray {
        val out = src.copyOf()
        val k = (amount * 1.35f).coerceIn(0f, 1.5f)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val center = src[idx]
                val top = src[idx - w]
                val bottom = src[idx + w]
                val left = src[idx - 1]
                val right = src[idx + 1]

                val a = (center ushr 24) and 0xFF
                val rc = (center ushr 16) and 0xFF
                val gc = (center ushr 8) and 0xFF
                val bc = center and 0xFF

                val lapR = 4 * rc - ((top ushr 16) and 0xFF) - ((bottom ushr 16) and 0xFF) - ((left ushr 16) and 0xFF) - ((right ushr 16) and 0xFF)
                val lapG = 4 * gc - ((top ushr 8) and 0xFF) - ((bottom ushr 8) and 0xFF) - ((left ushr 8) and 0xFF) - ((right ushr 8) and 0xFF)
                val lapB = 4 * bc - (top and 0xFF) - (bottom and 0xFF) - (left and 0xFF) - (right and 0xFF)

                val nr = (rc + (lapR * k * 0.35f).toInt()).coerceIn(0, 255)
                val ng = (gc + (lapG * k * 0.35f).toInt()).coerceIn(0, 255)
                val nb = (bc + (lapB * k * 0.35f).toInt()).coerceIn(0, 255)

                out[idx] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return out
    }

    suspend fun estimateCompressedPhotoSize(
        previewBitmap: Bitmap,
        sourceSizeBytes: Long,
        settings: PhotoCompressionSettings
    ): Long = withContext(Dispatchers.Default) {
        runCatching {
            val (w, h) = resolvePhotoTargetDimensions(
                srcWidth = previewBitmap.width,
                srcHeight = previewBitmap.height,
                mode = settings.resolutionMode,
                scaleFactor = settings.resolutionScale,
                customWidth = settings.customWidth.coerceAtMost(2048),
                customHeight = settings.customHeight.coerceAtMost(2048),
                preserveAspect = settings.keepAspectRatio
            )
            val safeW = w.coerceIn(64, 1600)
            val safeH = h.coerceIn(64, 1600)
            val scaled = if (safeW != previewBitmap.width || safeH != previewBitmap.height) {
                Bitmap.createScaledBitmap(previewBitmap, safeW, safeH, true)
            } else {
                previewBitmap
            }
            val baos = ByteArrayOutputStream()
            val fmt = toAndroidCompressFormat(settings.format)
            val q = if (settings.format.isLossless) 100 else settings.quality.coerceIn(10, 100)
            scaled.compress(fmt, q, baos)
            baos.size().toLong().coerceAtLeast(1024L)
        }.getOrElse {
            sourceSizeBytes
        }
    }

    fun toAndroidCompressFormat(format: PhotoOutputFormat): Bitmap.CompressFormat {
        return when (format) {
            PhotoOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            PhotoOutputFormat.PNG -> Bitmap.CompressFormat.PNG
            PhotoOutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            PhotoOutputFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.PNG
            }
        }
    }

    suspend fun exportProcessedPhoto(
        context: Context,
        sourceInfo: MediaSourceInfo,
        bitmap: Bitmap,
        format: PhotoOutputFormat,
        quality: Int,
        preserveMetadata: Boolean,
        prefix: String
    ): ExportedMediaOutput = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val baseName = sourceInfo.filename.substringBeforeLast(".")
        val outName = "${prefix}_${baseName}_${System.currentTimeMillis() % 100000}.${format.extension}"
        val tempFile = File(exportDir, "$outName.tmp")
        val finalFile = File(exportDir, outName)

        val effectiveQuality = if (format.isLossless) 100 else quality.coerceIn(15, 100)
        FileOutputStream(tempFile).use { fos ->
            val ok = bitmap.compress(toAndroidCompressFormat(format), effectiveQuality, fos)
            fos.flush()
            if (!ok) throw IllegalStateException("Bitmap encoder returned false")
        }

        if (!tempFile.exists() || tempFile.length() <= 0L) {
            tempFile.delete()
            throw IllegalStateException("Encoded photo output is empty")
        }

        if (tempFile.renameTo(finalFile).not()) {
            tempFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()
        }

        if (format == PhotoOutputFormat.JPEG && preserveMetadata) {
            runCatching {
                val exif = ExifInterface(finalFile.absolutePath)
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "ELITEMAX On-Device Engine")
                exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, bitmap.width.toString())
                exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, bitmap.height.toString())
                exif.saveAttributes()
            }
        }

        var galleryUri: Uri = Uri.fromFile(finalFile)
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, outName)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EliteMax")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { outStream ->
                    finalFile.inputStream().use { inStream -> inStream.copyTo(outStream) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readyValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    resolver.update(insertedUri, readyValues, null, null)
                }
                galleryUri = insertedUri
            }
        }

        runCatching {
            val publicPicturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "EliteMax"
            ).apply { mkdirs() }
            val publicCopy = File(publicPicturesDir, outName)
            if (!publicCopy.exists()) {
                finalFile.copyTo(publicCopy, overwrite = true)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicCopy.absolutePath, finalFile.absolutePath),
                arrayOf(format.mimeType, format.mimeType),
                null
            )
        }

        ExportedMediaOutput(
            localFile = finalFile,
            galleryUri = galleryUri,
            outputDimensions = "${bitmap.width} × ${bitmap.height}",
            outputSizeBytes = finalFile.length()
        )
    }
}
