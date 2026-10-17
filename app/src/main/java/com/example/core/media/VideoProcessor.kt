package com.example.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.roundToInt

object VideoProcessor {

    fun estimateVideoCompressedSize(
        source: MediaSourceInfo,
        settings: VideoCompressionSettings
    ): Long {
        if (source.sizeBytes <= 0L) return 0L
        val durationSec = (source.durationMs / 1000.0).coerceAtLeast(1.0)
        val audioKbps = if (settings.preserveAudio) 128 else 0
        val codecFactor = when (settings.formatOption) {
            VideoFormatOption.MP4_HEVC_HQ -> 0.72
            VideoFormatOption.WEBM_VP9_HQ -> 0.76
            VideoFormatOption.MP4_H264_HQ, VideoFormatOption.MOV_QUICKTIME_HQ -> 1.0
        }
        val effectiveVideoKbps = (settings.targetBitrateKbps * codecFactor).toInt().coerceAtLeast(350)
        val bitrateCalculatedBytes = ((effectiveVideoKbps + audioKbps) * 1000L / 8L * durationSec).toLong()
        val presetScaledBytes = (source.sizeBytes * settings.preset.bitrateFactor * settings.resolutionScale.coerceIn(0.4f, 1.0f)).toLong()
        return min(bitrateCalculatedBytes, presetScaledBytes).coerceAtLeast(1024L)
    }

    /**
     * Resolves target video dimensions aligned to 16-pixel macroblocks, supporting preset or exact Custom Width x Height.
     */
    fun resolveTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        resolutionPreset: VideoResolutionPreset,
        scaleFactor: Float,
        customWidth: Int = 1920,
        customHeight: Int = 1080
    ): Pair<Int, Int> {
        val baseW = if (srcWidth > 0) srcWidth else 1920
        val baseH = if (srcHeight > 0) srcHeight else 1080
        val aspect = baseW.toFloat() / baseH.toFloat()

        val (rawW, rawH) = when {
            resolutionPreset == VideoResolutionPreset.CUSTOM -> {
                Pair(customWidth.coerceIn(160, 3840), customHeight.coerceIn(160, 2160))
            }
            resolutionPreset.maxLongEdge != null -> {
                val maxEdge = resolutionPreset.maxLongEdge
                if (baseW >= baseH) {
                    val w = min(baseW, maxEdge)
                    val h = (w / aspect).roundToInt()
                    Pair(w, h)
                } else {
                    val h = min(baseH, maxEdge)
                    val w = (h * aspect).roundToInt()
                    Pair(w, h)
                }
            }
            else -> {
                val s = scaleFactor.coerceIn(0.25f, 1.0f)
                Pair((baseW * s).roundToInt(), (baseH * s).roundToInt())
            }
        }

        val alignedW = ((rawW.coerceIn(160, 3840) + 8) / 16) * 16
        val alignedH = ((rawH.coerceIn(160, 2160) + 8) / 16) * 16
        return Pair(alignedW, alignedH)
    }

    suspend fun processVideo(
        context: Context,
        source: MediaSourceInfo,
        enhanceSettings: EnhancementSettings?,
        compressSettings: VideoCompressionSettings?,
        onProgress: (Float, Int, Int, String) -> Unit
    ): ExportedMediaOutput = withContext(Dispatchers.Default) {
        coroutineContext.ensureActive()

        val formatOpt = enhanceSettings?.videoFormat
            ?: compressSettings?.formatOption
            ?: VideoFormatOption.MP4_H264_HQ

        val selectedFormat = if (MediaInspector.isEncoderSupported(formatOpt.codecMime)) {
            formatOpt
        } else {
            VideoFormatOption.MP4_H264_HQ
        }

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val prefix = if (enhanceSettings != null) "ENHANCED" else "COMPRESSED"
        val baseName = source.filename.substringBeforeLast(".")
        val outName = "${prefix}_${baseName}_${System.currentTimeMillis() % 100000}.${selectedFormat.extension}"
        val tempFile = File(exportDir, "$outName.tmp")
        val finalFile = File(exportDir, outName)

        try {
            val resPreset = enhanceSettings?.videoResolution
                ?: compressSettings?.resolutionPreset
                ?: VideoResolutionPreset.ORIGINAL
            val resScale = enhanceSettings?.resolutionScale
                ?: compressSettings?.resolutionScale
                ?: 1.0f
            val customW = enhanceSettings?.customVideoWidth
                ?: compressSettings?.customWidth
                ?: 1920
            val customH = enhanceSettings?.customVideoHeight
                ?: compressSettings?.customHeight
                ?: 1080

            val (targetW, targetH) = resolveTargetDimensions(
                srcWidth = source.width,
                srcHeight = source.height,
                resolutionPreset = resPreset,
                scaleFactor = resScale,
                customWidth = customW,
                customHeight = customH
            )

            val fps = (enhanceSettings?.videoFrameRate ?: compressSettings?.frameRate ?: source.frameRate).coerceIn(15, 120)
            val actualDurationMs = source.durationMs.coerceAtLeast(1000L)
            val totalFrames = ((actualDurationMs / 1000f) * fps).roundToInt().coerceIn(24, 360)
            val frameStepUs = (actualDurationMs * 1000L) / totalFrames.coerceAtLeast(1)

            val targetBitrate = if (compressSettings != null) {
                (compressSettings.targetBitrateKbps * 1000).coerceIn(400_000, 50_000_000)
            } else {
                val hqBitrate = (enhanceSettings?.targetVideoBitrateKbps ?: 14_000) * 1000
                hqBitrate.coerceIn(1_000_000, 50_000_000)
            }

            onProgress(0.05f, 0, totalFrames, "Opening ${selectedFormat.label} encoder (${targetW}×${targetH} @ ${fps}fps)...")

            val retriever = MediaMetadataRetriever()
            if (source.uri.scheme == "file") {
                retriever.setDataSource(source.uri.path)
            } else {
                retriever.setDataSource(context, source.uri)
            }

            val colorMatrix = enhanceSettings?.let { PhotoProcessor.buildEnhancementColorMatrix(it) }

            try {
                encodeFramesToContainer(
                    outputFile = tempFile,
                    width = targetW,
                    height = targetH,
                    fps = fps,
                    totalFrames = totalFrames,
                    frameStepUs = frameStepUs,
                    bitrate = targetBitrate,
                    formatOption = selectedFormat,
                    sourceUriForAudio = if (compressSettings?.preserveAudio != false) source.uri else null,
                    context = context,
                    frameProvider = { _, ptsUs ->
                        coroutineContext.ensureActive()
                        val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(
                                ptsUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                targetW,
                                targetH
                            ) ?: retriever.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } else {
                            retriever.getFrameAtTime(ptsUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } ?: throw IllegalStateException("Unable to decode video frame at ${ptsUs / 1000}ms")

                        val frameOut = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(frameOut)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        if (colorMatrix != null) {
                            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                        }
                        canvas.drawBitmap(rawFrame, null, android.graphics.Rect(0, 0, targetW, targetH), paint)
                        rawFrame.recycle()
                        frameOut
                    },
                    onFrameProgress = { done, total ->
                        val frac = (0.08f + 0.86f * (done.toFloat() / total.toFloat())).coerceIn(0.08f, 0.94f)
                        val stage = if (enhanceSettings != null) {
                            "Enhancing ${targetW}×${targetH} @ ${fps}fps ($done/$total)..."
                        } else {
                            "Encoding ${selectedFormat.label} ${targetW}×${targetH} ($done/$total)..."
                        }
                        onProgress(frac, done, total, stage)
                    }
                )
            } finally {
                runCatching { retriever.release() }
            }

            coroutineContext.ensureActive()

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                tempFile.delete()
                throw IllegalStateException("Encoded video output is empty")
            }

            if (tempFile.renameTo(finalFile).not()) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(0.97f, totalFrames, totalFrames, "Saving directly into Device Gallery...")

            var galleryUri: Uri = Uri.fromFile(finalFile)
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outName)
                    put(MediaStore.Video.Media.MIME_TYPE, selectedFormat.containerMime)
                    put(MediaStore.Video.Media.WIDTH, targetW)
                    put(MediaStore.Video.Media.HEIGHT, targetH)
                    put(MediaStore.Video.Media.DURATION, actualDurationMs)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                    put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/EliteMax")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val inserted = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (inserted != null) {
                    resolver.openOutputStream(inserted)?.use { out ->
                        finalFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val readyValues = ContentValues().apply {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        resolver.update(inserted, readyValues, null, null)
                    }
                    galleryUri = inserted
                }
            }

            runCatching {
                val publicMoviesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "EliteMax"
                ).apply { mkdirs() }
                val publicCopy = File(publicMoviesDir, outName)
                if (!publicCopy.exists()) {
                    finalFile.copyTo(publicCopy, overwrite = true)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(publicCopy.absolutePath, finalFile.absolutePath),
                    arrayOf(selectedFormat.containerMime, selectedFormat.containerMime),
                    null
                )
            }

            ExportedMediaOutput(
                localFile = finalFile,
                galleryUri = galleryUri,
                outputDimensions = "$targetW × $targetH • ${fps}fps",
                outputSizeBytes = finalFile.length()
            )
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    private suspend fun encodeFramesToContainer(
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        totalFrames: Int,
        frameStepUs: Long,
        bitrate: Int,
        formatOption: VideoFormatOption,
        sourceUriForAudio: Uri? = null,
        context: Context? = null,
        frameProvider: suspend (Int, Long) -> Bitmap,
        onFrameProgress: (Int, Int) -> Unit
    ) {
        val mimeType = formatOption.codecMime
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val muxerFormat = if (formatOption == VideoFormatOption.WEBM_VP9_HQ) {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        } else {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }

        val encoder = MediaCodec.createEncoderByType(mimeType)
        val muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var muxerStarted = false

        var audioExtractor: MediaExtractor? = null
        var sourceAudioTrack = -1
        if (sourceUriForAudio != null && context != null && formatOption != VideoFormatOption.WEBM_VP9_HQ) {
            runCatching {
                val ext = MediaExtractor()
                if (sourceUriForAudio.scheme == "file") {
                    ext.setDataSource(sourceUriForAudio.path ?: "")
                } else {
                    ext.setDataSource(context, sourceUriForAudio, null)
                }
                for (i in 0 until ext.trackCount) {
                    val fmt = ext.getTrackFormat(i)
                    val m = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                    if (m.startsWith("audio/")) {
                        sourceAudioTrack = i
                        ext.selectTrack(i)
                        audioTrackIndex = muxer.addTrack(fmt)
                        audioExtractor = ext
                        break
                    }
                }
            }
        }

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val yuvBuffer = ByteArray(width * height * 3 / 2)
            val argbPixels = IntArray(width * height)
            var inputDone = false
            var outputDone = false
            var frameIndex = 0

            while (!outputDone) {
                coroutineContext.ensureActive()

                if (!inputDone) {
                    val inBufIdx = encoder.dequeueInputBuffer(10_000L)
                    if (inBufIdx >= 0) {
                        if (frameIndex >= totalFrames) {
                            encoder.queueInputBuffer(
                                inBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val ptsUs = frameIndex.toLong() * frameStepUs
                            val bmp = frameProvider(frameIndex, ptsUs)
                            bmp.getPixels(argbPixels, 0, width, 0, 0, width, height)
                            bmp.recycle()
                            encodeArgbToNv12(yuvBuffer, argbPixels, width, height)

                            val inBuf = encoder.getInputBuffer(inBufIdx)
                            inBuf?.clear()
                            inBuf?.put(yuvBuffer)

                            encoder.queueInputBuffer(inBufIdx, 0, yuvBuffer.size, ptsUs, 0)
                            frameIndex++
                            onFrameProgress(frameIndex, totalFrames)
                        }
                    }
                }

                val outBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outBufIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (inputDone) Thread.sleep(10)
                    }
                    outBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outBufIdx >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outBufIdx)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted &&
                            (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outBufIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (muxerStarted && audioExtractor != null && audioTrackIndex >= 0 && sourceAudioTrack >= 0) {
                runCatching {
                    val audioBuf = ByteBuffer.allocate(64 * 1024)
                    val audioInfo = MediaCodec.BufferInfo()
                    val maxDurationUs = totalFrames.toLong() * frameStepUs
                    while (true) {
                        val sampleSize = audioExtractor!!.readSampleData(audioBuf, 0)
                        if (sampleSize < 0) break
                        val sampleTime = audioExtractor!!.sampleTime
                        if (sampleTime > maxDurationUs) break
                        audioInfo.offset = 0
                        audioInfo.size = sampleSize
                        audioInfo.presentationTimeUs = sampleTime.coerceAtLeast(0L)
                        audioInfo.flags = audioExtractor!!.sampleFlags
                        muxer.writeSampleData(audioTrackIndex, audioBuf, audioInfo)
                        audioExtractor!!.advance()
                    }
                }
            }
        } finally {
            runCatching { audioExtractor?.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun encodeArgbToNv12(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0 && uvIndex + 1 < yuv420sp.size) {
                    yuv420sp[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv420sp[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
                index++
            }
        }
    }
}
