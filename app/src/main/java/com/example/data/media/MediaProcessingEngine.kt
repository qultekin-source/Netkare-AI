package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.PhotoExportFormat
import com.example.domain.model.PhotoManualSettings
import com.example.domain.model.VideoExportFormat
import com.example.domain.model.VideoMetadataInfo
import com.example.domain.model.VideoQualitySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val TAG = "MediaProcessingEngine"

object MediaProcessingEngine {

    suspend fun extractVideoMetadata(context: Context, uri: Uri): VideoMetadataInfo =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val fps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                        ?.toFloatOrNull()?.roundToInt() ?: 30
                } else {
                    30
                }
                val frame = retriever.getFrameAtTime(
                    (duration * 250L).coerceAtLeast(0L),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                val stripRatios = listOf(0.05f, 0.25f, 0.50f, 0.75f, 0.92f)
                val timelineFrames = stripRatios.mapNotNull { r ->
                    val tsUs = (duration * r * 1000L).toLong().coerceAtLeast(0L)
                    retriever.getFrameAtTime(tsUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.let { scaleDownBitmap(it, 220) }
                }
                val (name, size) = queryDisplayNameAndSize(context, uri)
                VideoMetadataInfo(
                    uri = uri,
                    title = name ?: "Secilen_Video.mp4",
                    width = width,
                    height = height,
                    durationMs = duration,
                    frameRate = fps.coerceIn(15, 60),
                    fileSizeBytes = size,
                    thumbnail = frame,
                    timelineThumbnails = timelineFrames
                )
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun extractRepresentativeFrames(
        context: Context,
        uri: Uri,
        count: Int = 3
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 2000L
            val ratios = if (count <= 1) listOf(0.5f) else listOf(0.15f, 0.50f, 0.85f).take(count)
            for (r in ratios) {
                val timeUs = (durationMs * r * 1000L).toLong().coerceAtLeast(0L)
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) {
                    frames.add(scaleDownBitmap(bmp, 720))
                }
            }
        } finally {
            runCatching { retriever.release() }
        }
        frames
    }

    /**
     * Creates a genuine low-resolution (320x240) retro-styled MP4 video clip in cache
     * so users can test real AI analysis and Media3 Transformer upscaling immediately.
     */
    suspend fun createSampleVintageVideo(context: Context): Uri = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "ornek_eski_video_1994.mp4")
        if (outFile.exists()) outFile.delete()

        val width = 320
        val height = 240
        val fps = 24
        val totalFrames = fps * 3 // 3 seconds
        val bitRate = 380_000

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val grainPaint = Paint()
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 224, 185)
                textSize = 18f
                isFakeBoldText = true
            }
            val rng = Random(1994)

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) throw RuntimeException("Format changed twice")
                            val newFormat = encoder.outputFormat
                            trackIndex = muxer!!.addTrack(newFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                        outputIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outputIndex)
                                ?: throw RuntimeException("Output buffer null")
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                val t = frameIdx.toFloat() / totalFrames.toFloat()
                val canvas: Canvas = inputSurface.lockCanvas(null)
                try {
                    // Faded sepia-blue vintage twilight sky
                    bgPaint.shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        Color.rgb(66, 58, 52),
                        Color.rgb(112, 92, 78),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                    bgPaint.shader = null

                    // Warm sun disc moving gently
                    val sunX = 70f + t * 180f
                    val sunY = 92f - sin(t * Math.PI).toFloat() * 24f
                    shapePaint.color = Color.argb(185, 224, 175, 118)
                    canvas.drawCircle(sunX, sunY, 26f, shapePaint)

                    // Silhouette skyline & tram-like block
                    shapePaint.color = Color.rgb(48, 42, 40)
                    canvas.drawRect(0f, 168f, width.toFloat(), height.toFloat(), shapePaint)
                    val tramX = -40f + t * 360f
                    shapePaint.color = Color.rgb(156, 72, 58)
                    canvas.drawRoundRect(RectF(tramX, 134f, tramX + 76f, 174f), 8f, 8f, shapePaint)
                    shapePaint.color = Color.rgb(218, 200, 165)
                    canvas.drawRect(tramX + 10f, 142f, tramX + 28f, 156f, shapePaint)
                    canvas.drawRect(tramX + 42f, 142f, tramX + 60f, 156f, shapePaint)

                    // Analog scanlines + VHS grain simulation
                    grainPaint.color = Color.argb(38, 255, 250, 230)
                    for (y in 0 until height step 4) {
                        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), grainPaint)
                    }
                    repeat(70) {
                        val gx = rng.nextFloat() * width
                        val gy = rng.nextFloat() * height
                        grainPaint.color = Color.argb(75, 230, 220, 205)
                        canvas.drawCircle(gx, gy, 1.3f, grainPaint)
                    }

                    // Retro camcorder OSD
                    val sec = frameIdx / fps
                    canvas.drawText("VHS 1994  00:0$sec", 14f, 28f, textPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(false)
            }
            drainEncoder(true)
            inputSurface.release()
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }

        Uri.fromFile(outFile)
    }

    @OptIn(UnstableApi::class)
    suspend fun enhanceVideoWithTransformer(
        context: Context,
        sourceUri: Uri,
        settings: VideoQualitySettings,
        onProgressPercent: (Int) -> Unit
    ): File = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())
        val outDir = File(context.filesDir, "restored").apply { mkdirs() }
        val outFile = File(outDir, "netkare_video_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        mainHandler.post {
            var finished = false
            var transformer: Transformer? = null

            val progressHolder = ProgressHolder()
            val pollRunnable = object : Runnable {
                override fun run() {
                    val activeTransformer = transformer ?: return
                    if (finished) return
                    val state = activeTransformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgressPercent(progressHolder.progress.coerceIn(0, 100))
                    }
                    if (!finished && state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        mainHandler.postDelayed(this, 200L)
                    } else if (!finished) {
                        mainHandler.postDelayed(this, 300L)
                    }
                }
            }

            try {
                val videoEffects = buildList<Effect> {
                    add(Presentation.createForHeight(settings.targetResolution.heightPx))
                    val factor = settings.enhancementLevel.intensityFactor
                    val contrastValue = ((settings.contrast + settings.sharpness * 0.12f) * factor)
                        .coerceIn(-0.9f, 0.9f)
                    add(Contrast(contrastValue))
                    add(
                        HslAdjustment.Builder()
                            .adjustSaturation((settings.saturation * factor.coerceAtMost(1.2f)).coerceIn(-100f, 100f))
                            .adjustLightness((settings.brightness * factor.coerceAtMost(1.25f)).coerceIn(-45f, 45f))
                            .build()
                    )
                    val warmth = settings.preset.warmth * factor
                    add(
                        RgbAdjustment.Builder()
                            .setRedScale(1f + warmth)
                            .setGreenScale(1f + settings.sharpness * 0.025f * factor)
                            .setBlueScale(1f - warmth * 0.45f)
                            .build()
                    )
                }

                val mediaItem = MediaItem.fromUri(sourceUri)
                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (!finished) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            onProgressPercent(100)
                            if (continuation.isActive) {
                                continuation.resume(outFile)
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (!finished) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            Log.e(TAG, "Transformer export error", exportException)
                            if (continuation.isActive) {
                                continuation.resumeWithException(exportException)
                            }
                        }
                    }
                }

                val builtTransformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(listener)
                    .build()
                transformer = builtTransformer

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        finished = true
                        mainHandler.removeCallbacks(pollRunnable)
                        runCatching { builtTransformer.cancel() }
                    }
                }

                builtTransformer.start(editedMediaItem, outFile.absolutePath)
                mainHandler.postDelayed(pollRunnable, 200L)
            } catch (e: Exception) {
                finished = true
                mainHandler.removeCallbacks(pollRunnable)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    suspend fun applyLocalPhotoAdjustments(
        source: Bitmap,
        settings: PhotoManualSettings
    ): Bitmap = withContext(Dispatchers.Default) {
        val working = scaleDownBitmap(source, 1600)
        val colorAdjusted = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(colorAdjusted)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val satMatrix = ColorMatrix().apply { setSaturation(settings.saturation) }
        val c = settings.contrast
        val t = (1f - c) * 128f + settings.brightness
        val warmR = 1f + (settings.warmth * 0.14f)
        val warmB = 1f - (settings.warmth * 0.14f)
        val toneMatrix = ColorMatrix(
            floatArrayOf(
                c * warmR, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c * warmB, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(toneMatrix)
        paint.colorFilter = ColorMatrixColorFilter(satMatrix)
        canvas.drawBitmap(working, 0f, 0f, paint)

        val amount = settings.sharpness.coerceIn(0f, 1f)
        if (amount <= 0.04f) {
            return@withContext colorAdjusted
        }

        val w = colorAdjusted.width
        val h = colorAdjusted.height
        val srcPixels = IntArray(w * h)
        val dstPixels = IntArray(w * h)
        colorAdjusted.getPixels(srcPixels, 0, w, 0, 0, w, h)
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.size)

        val k = amount * 0.85f
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val center = srcPixels[idx]
                val top = srcPixels[idx - w]
                val bottom = srcPixels[idx + w]
                val left = srcPixels[idx - 1]
                val right = srcPixels[idx + 1]

                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                val avgR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                    ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                val avgG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                    ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                val avgB = ((top and 0xFF) + (bottom and 0xFF) +
                    (left and 0xFF) + (right and 0xFF)) shr 2

                val outR = (cR + ((cR - avgR) * k)).roundToInt().coerceIn(0, 255)
                val outG = (cG + ((cG - avgG) * k)).roundToInt().coerceIn(0, 255)
                val outB = (cB + ((cB - avgB) * k)).roundToInt().coerceIn(0, 255)

                dstPixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
        Bitmap.createBitmap(dstPixels, w, h, Bitmap.Config.ARGB_8888)
    }

    suspend fun saveBitmapToAppFiles(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "netkare_photo"
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "restored").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        file
    }

    suspend fun exportPhotoWithOptions(
        context: Context,
        bitmap: Bitmap,
        format: PhotoExportFormat,
        quality: ExportQualityTier,
        prefix: String = "netkare_export"
    ): File = withContext(Dispatchers.IO) {
        val workingBitmap = quality.photoMaxDimPx?.let { maxDim ->
            scaleDownBitmap(bitmap, maxDim)
        } ?: bitmap

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.${format.extension}")
        FileOutputStream(file).use { out ->
            compressBitmapWithFormat(workingBitmap, format, quality, out)
        }
        file
    }

    suspend fun saveExportedPhotoToGallery(
        context: Context,
        bitmap: Bitmap,
        format: PhotoExportFormat,
        quality: ExportQualityTier,
        displayBaseName: String
    ): Pair<Uri, String>? = withContext(Dispatchers.IO) {
        val workingBitmap = quality.photoMaxDimPx?.let { maxDim ->
            scaleDownBitmap(bitmap, maxDim)
        } ?: bitmap
        val displayName = "${displayBaseName}.${format.extension}"
        val folderName = "Pictures/NetKare AI"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NetKare AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null
        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                compressBitmapWithFormat(workingBitmap, format, quality, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            Pair(itemUri, folderName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving exported photo to gallery", e)
            runCatching { resolver.delete(itemUri, null, null) }
            null
        }
    }

    private fun compressBitmapWithFormat(
        bitmap: Bitmap,
        format: PhotoExportFormat,
        quality: ExportQualityTier,
        out: java.io.OutputStream
    ) {
        val compressFormat = when (format) {
            PhotoExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            PhotoExportFormat.PNG -> Bitmap.CompressFormat.PNG
            PhotoExportFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (quality == ExportQualityTier.ULTRA) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
        val compressQuality = if (format == PhotoExportFormat.PNG) 100 else quality.photoCompressQuality
        bitmap.compress(compressFormat, compressQuality, out)
    }

    suspend fun exportVideoWithOptions(
        context: Context,
        sourceVideoFile: File,
        format: VideoExportFormat,
        quality: ExportQualityTier,
        onProgressPercent: (Int) -> Unit
    ): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, "NetKare_${System.currentTimeMillis()}.${format.extension}")

        return when (format) {
            VideoExportFormat.AVI_MJPEG -> {
                AviMjpegMuxer.transcodeToAvi(
                    sourceVideoFile = sourceVideoFile,
                    outputAviFile = outFile,
                    maxHeightPx = quality.videoMaxHeightPx,
                    jpegQuality = quality.aviJpegQuality,
                    onProgress = onProgressPercent
                )
            }
            VideoExportFormat.MP4_H264 -> {
                val sourceHeight = readVideoHeight(sourceVideoFile)
                val needsDownscale = quality.videoMaxHeightPx != null && sourceHeight > quality.videoMaxHeightPx
                if (!needsDownscale) {
                    withContext(Dispatchers.IO) {
                        sourceVideoFile.copyTo(outFile, overwrite = true)
                    }
                    onProgressPercent(100)
                    outFile
                } else {
                    transcodeVideoWithMime(
                        context = context,
                        sourceFile = sourceVideoFile,
                        outFile = outFile,
                        targetHeightPx = quality.videoMaxHeightPx,
                        videoMimeType = MimeTypes.VIDEO_H264,
                        onProgressPercent = onProgressPercent
                    )
                }
            }
            VideoExportFormat.MP4_HEVC -> {
                try {
                    transcodeVideoWithMime(
                        context = context,
                        sourceFile = sourceVideoFile,
                        outFile = outFile,
                        targetHeightPx = quality.videoMaxHeightPx,
                        videoMimeType = MimeTypes.VIDEO_H265,
                        onProgressPercent = onProgressPercent
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "HEVC export unavailable on device, falling back to H.264 MP4", e)
                    transcodeVideoWithMime(
                        context = context,
                        sourceFile = sourceVideoFile,
                        outFile = outFile,
                        targetHeightPx = quality.videoMaxHeightPx,
                        videoMimeType = MimeTypes.VIDEO_H264,
                        onProgressPercent = onProgressPercent
                    )
                }
            }
        }
    }

    private fun readVideoHeight(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
        } catch (_: Exception) {
            1080
        } finally {
            runCatching { retriever.release() }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun transcodeVideoWithMime(
        context: Context,
        sourceFile: File,
        outFile: File,
        targetHeightPx: Int?,
        videoMimeType: String,
        onProgressPercent: (Int) -> Unit
    ): File = suspendCancellableCoroutine { continuation ->
        val mainHandler = Handler(Looper.getMainLooper())
        if (outFile.exists()) outFile.delete()

        mainHandler.post {
            var finished = false
            var transformer: Transformer? = null
            val progressHolder = ProgressHolder()
            val pollRunnable = object : Runnable {
                override fun run() {
                    val active = transformer ?: return
                    if (finished) return
                    val state = active.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgressPercent(progressHolder.progress.coerceIn(0, 100))
                    }
                    if (!finished) {
                        mainHandler.postDelayed(this, 200L)
                    }
                }
            }

            try {
                val effects = buildList<Effect> {
                    if (targetHeightPx != null) {
                        add(Presentation.createForHeight(targetHeightPx))
                    }
                }
                val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))
                val edited = EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList(), effects))
                    .build()

                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (!finished) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            onProgressPercent(100)
                            if (continuation.isActive) continuation.resume(outFile)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (!finished) {
                            finished = true
                            mainHandler.removeCallbacks(pollRunnable)
                            if (continuation.isActive) continuation.resumeWithException(exportException)
                        }
                    }
                }

                val built = Transformer.Builder(context)
                    .setVideoMimeType(videoMimeType)
                    .addListener(listener)
                    .build()
                transformer = built

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        finished = true
                        mainHandler.removeCallbacks(pollRunnable)
                        runCatching { built.cancel() }
                    }
                }

                built.start(edited, outFile.absolutePath)
                mainHandler.postDelayed(pollRunnable, 200L)
            } catch (e: Exception) {
                finished = true
                mainHandler.removeCallbacks(pollRunnable)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }

    suspend fun saveExportedVideoToDevice(
        context: Context,
        sourceFile: File,
        format: VideoExportFormat,
        displayBaseName: String
    ): Pair<Uri, String>? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        val displayName = "${displayBaseName}.${format.extension}"
        val resolver = context.contentResolver

        // Try MediaStore.Video first
        val videoResult = runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NetKare AI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                }
                Pair(itemUri, "Movies/NetKare AI")
            } catch (e: Exception) {
                runCatching { resolver.delete(itemUri, null, null) }
                throw e
            }
        }.getOrNull()

        if (videoResult != null) return@withContext videoResult

        // Fallback to Downloads (API 29+) for non-standard video MIME types such as video/x-msvideo (.avi)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return@withContext runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, format.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NetKare AI")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    Pair(itemUri, "Downloads/NetKare AI")
                } catch (e: Exception) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw e
                }
            }.getOrNull()
        }

        null
    }

    suspend fun saveVideoToGallery(context: Context, sourceFile: File, displayName: String): Uri? =
        saveExportedVideoToDevice(
            context = context,
            sourceFile = sourceFile,
            format = VideoExportFormat.MP4_H264,
            displayBaseName = displayName.substringBeforeLast(".")
        )?.first

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? =
        saveExportedPhotoToGallery(
            context = context,
            bitmap = bitmap,
            format = PhotoExportFormat.JPEG,
            quality = ExportQualityTier.HIGH,
            displayBaseName = displayName.substringBeforeLast(".")
        )?.first

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        if (!file.exists()) return
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    private fun queryDisplayNameAndSize(context: Context, uri: Uri): Pair<String?, Long> {
        if (uri.scheme == "file") {
            val file = uri.path?.let { File(it) }
            return Pair(file?.name, file?.length() ?: 0L)
        }
        var name: String? = null
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        }
        return Pair(name, size)
    }

    fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val w = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
