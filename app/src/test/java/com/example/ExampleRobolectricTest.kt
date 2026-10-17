package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.core.media.CompressionPreset
import com.example.core.media.EnhancePreset
import com.example.core.media.EnhancementSettings
import com.example.core.media.MediaInspector
import com.example.core.media.PhotoCompressionSettings
import com.example.core.media.PhotoOutputFormat
import com.example.core.media.PhotoProcessor
import com.example.core.media.VideoProcessor
import com.example.core.media.VideoResolutionPreset
import com.example.core.media.formatDuration
import com.example.core.media.formatFileSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and tagline strings from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val tagline = context.getString(R.string.tagline_main)
        assertEquals("EliteMax", appName)
        assertEquals("Sharper. Clearer. Better.", tagline)
    }

    @Test
    fun `photo enhancement produces valid non-empty bitmap with target dimensions`() = runBlocking {
        val src = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 120, 200))
        }
        val settings = EnhancementSettings(
            preset = EnhancePreset.CLEAR,
            sharpness = 0.32f,
            contrast = 0.28f,
            noiseReduction = 0.45f,
            saturation = 0.22f,
            resolutionScale = 0.75f
        )
        val out = PhotoProcessor.processEnhancedBitmap(src, settings)
        assertEquals(180, out.width)
        assertEquals(120, out.height)
    }

    @Test
    fun `photo compression estimate and lossless format options work in realtime`() = runBlocking {
        val src = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(30, 100, 180))
        }
        val settings = PhotoCompressionSettings(
            preset = CompressionPreset.BALANCED,
            format = PhotoOutputFormat.WEBP_LOSSLESS,
            quality = 100,
            resolutionScale = 1.0f
        )
        val estBytes = PhotoProcessor.estimateCompressedPhotoSize(src, 1_500_000L, settings)
        assertTrue(estBytes > 0L)
    }

    @Test
    fun `video resolution alignment and runtime permissions list are valid`() {
        val perms = MediaInspector.getRequiredRuntimePermissions()
        assertTrue(perms.isNotEmpty())
        val (w, h) = VideoProcessor.resolveTargetDimensions(3840, 2160, VideoResolutionPreset.UHD_4K, 1.0f)
        assertEquals(0, w % 16)
        assertEquals(0, h % 16)

        val (cw, ch) = VideoProcessor.resolveTargetDimensions(
            srcWidth = 1920,
            srcHeight = 1080,
            resolutionPreset = VideoResolutionPreset.CUSTOM,
            scaleFactor = 1.0f,
            customWidth = 1600,
            customHeight = 900
        )
        assertEquals(1600, cw)
        assertEquals(0, ch % 16)

        val (pw, ph) = PhotoProcessor.resolvePhotoTargetDimensions(
            srcWidth = 1920,
            srcHeight = 1080,
            mode = com.example.core.media.PhotoResolutionMode.CUSTOM,
            scaleFactor = 1.0f,
            customWidth = 1280,
            customHeight = 720,
            preserveAspect = true
        )
        assertEquals(1280, pw)
        assertEquals(720, ph)

        assertEquals("02:32", formatDuration(152_000L))
        assertTrue(formatFileSize(1024L * 1024L * 10L).contains("MB"))
    }
}
