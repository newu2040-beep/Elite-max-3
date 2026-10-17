# ELITEMAX — On-Device Media Enhancement & Compression for Android

**Sharper. Clearer. Better. On your device.**

ELITEMAX is a native Android application built with Kotlin and Jetpack Compose for on-device video enhancement, photo enhancement, video compression, and photo compression.

## Key Features

- **100% On-Device Processing:** Uses Android native `MediaCodec`, `MediaMuxer`, `MediaExtractor`, `MediaMetadataRetriever`, and `Bitmap` / `ColorMatrix` / spatial convolution kernels. Zero cloud uploads and zero network dependency.
- **Visual System:** Editorial typography (`Plus Jakarta Sans` & `Roboto`), pale icy-blue surfaces (`#EEF5FD`), dominant cobalt-blue feature panels (`#1769F5`), interactive before/after sliders, and mint savings badges (`#DDF6EC` / `#0F7E56`), plus a coherent solid charcoal dark theme.
- **10 Dedicated Destinations:**
  1. **Welcome (`Media Quality Review`)**: Tilted `Before` & `After` alpine previews, flowing wave motif, floating `Clarity 82` waveform indicator, and `Get Started` CTA.
  2. **Home (`Better media. Less space.`)**: Four primary tools (`Video Enhancer`, `Photo Enhancer`, `Video Compressor`, `Photo Compressor`), active media studio card with 2×2 cobalt metrics, search, and recent history.
  3. **Video Enhancer**: Interactive Before/After comparison, presets (`Natural`, `Clear`, `Low Light`, `Custom`), sharpness/brightness/contrast/saturation/temperature/noise sliders, and real MP4 export.
  4. **Photo Enhancer**: Non-destructive pixel enhancement (`ColorMatrix`, edge-preserving noise reduction, and unsharp-mask sharpness kernel), aspect-ratio crop, resize, and format selection.
  5. **Video Compressor**: Size comparison with vertical bar chart, `Smaller` / `Balanced` / `Quality` presets, resolution, bitrate, FPS, H.264/HEVC codec, and audio preservation controls.
  6. **Photo Compressor**: JPEG, PNG, and WebP encoding with live size estimation, transparency loss warnings, resize scale, and EXIF metadata stripping.
  7. **Processing**: Live frame/block progress, animated waveform indicator, and safe cancellation with temporary file cleanup.
  8. **Export Result**: Actual output size, percentage saved, `Open File`, and `Share` via `FileProvider`.
  9. **History**: Local Room database persistence with filter tabs and per-item actions.
  10. **Settings**: Light/Dark/System themes, default quality settings, storage inspection, and privacy details.

## Build & Testing Instructions

- **Compile Debug APK:** `gradle assembleDebug`
- **Run Unit & Robolectric UI Tests:** `gradle :app:testDebugUnitTest`
