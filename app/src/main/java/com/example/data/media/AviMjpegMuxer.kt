package com.example.data.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure-Kotlin RIFF/AVI (MJPEG) muxer that produces spec-compliant .avi video files
 * from an input MP4/video file at the requested resolution cap and JPEG quality.
 */
object AviMjpegMuxer {

    private data class IndexEntry(val offsetFromMoviType: Int, val size: Int)

    suspend fun transcodeToAvi(
        sourceVideoFile: File,
        outputAviFile: File,
        maxHeightPx: Int?,
        jpegQuality: Int,
        targetFps: Int = 18,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (outputAviFile.exists()) outputAviFile.delete()

        val retriever = MediaMetadataRetriever()
        val indexEntries = mutableListOf<IndexEntry>()

        try {
            retriever.setDataSource(sourceVideoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(500L) ?: 2000L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()?.coerceAtLeast(64) ?: 640
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()?.coerceAtLeast(64) ?: 480

            val (outW, outH) = if (maxHeightPx != null && rawH > maxHeightPx) {
                val scale = maxHeightPx.toFloat() / rawH.toFloat()
                val w = ((rawW * scale).roundToInt() / 2) * 2
                val h = (maxHeightPx / 2) * 2
                Pair(w.coerceAtLeast(64), h.coerceAtLeast(64))
            } else {
                Pair((rawW / 2) * 2, (rawH / 2) * 2)
            }

            val fps = targetFps.coerceIn(12, 24)
            val totalFrames = ((durationMs / 1000.0) * fps).roundToInt().coerceIn(8, 240)
            val microSecPerFrame = (1_000_000L / fps).toInt()

            RandomAccessFile(outputAviFile, "rw").use { raf ->
                // RIFF header (12 bytes)
                raf.writeAscii("RIFF")
                val riffSizePos = raf.filePointer
                raf.writeIntLE(0) // patched at end
                raf.writeAscii("AVI ")

                // LIST 'hdrl'
                raf.writeAscii("LIST")
                val hdrlSizePos = raf.filePointer
                raf.writeIntLE(0) // patched after header
                val hdrlStart = raf.filePointer
                raf.writeAscii("hdrl")

                // 'avih' main AVI header (56 bytes payload)
                raf.writeAscii("avih")
                raf.writeIntLE(56)
                val avihPayloadPos = raf.filePointer
                raf.writeIntLE(microSecPerFrame)
                raf.writeIntLE(0) // dwMaxBytesPerSec (patched)
                raf.writeIntLE(0) // dwPaddingGranularity
                raf.writeIntLE(0x10) // AVIF_HASINDEX
                val totalFramesPos = raf.filePointer
                raf.writeIntLE(totalFrames)
                raf.writeIntLE(0) // dwInitialFrames
                raf.writeIntLE(1) // dwStreams
                val suggestedBufAvihPos = raf.filePointer
                raf.writeIntLE(outW * outH)
                raf.writeIntLE(outW)
                raf.writeIntLE(outH)
                repeat(4) { raf.writeIntLE(0) }

                // LIST 'strl'
                raf.writeAscii("LIST")
                val strlSizePos = raf.filePointer
                raf.writeIntLE(0)
                val strlStart = raf.filePointer
                raf.writeAscii("strl")

                // 'strh' stream header (56 bytes)
                raf.writeAscii("strh")
                raf.writeIntLE(56)
                raf.writeAscii("vids")
                raf.writeAscii("MJPG")
                raf.writeIntLE(0) // dwFlags
                raf.writeShortLE(0) // wPriority
                raf.writeShortLE(0) // wLanguage
                raf.writeIntLE(0) // dwInitialFrames
                raf.writeIntLE(1) // dwScale
                raf.writeIntLE(fps) // dwRate
                raf.writeIntLE(0) // dwStart
                val strhLengthPos = raf.filePointer
                raf.writeIntLE(totalFrames)
                val suggestedBufStrhPos = raf.filePointer
                raf.writeIntLE(outW * outH)
                raf.writeIntLE(-1) // dwQuality
                raf.writeIntLE(0) // dwSampleSize
                raf.writeShortLE(0)
                raf.writeShortLE(0)
                raf.writeShortLE(outW)
                raf.writeShortLE(outH)

                // 'strf' BITMAPINFOHEADER (40 bytes)
                raf.writeAscii("strf")
                raf.writeIntLE(40)
                raf.writeIntLE(40) // biSize
                raf.writeIntLE(outW)
                raf.writeIntLE(outH)
                raf.writeShortLE(1) // biPlanes
                raf.writeShortLE(24) // biBitCount
                raf.writeAscii("MJPG") // biCompression
                raf.writeIntLE(outW * outH * 3) // biSizeImage
                raf.writeIntLE(0)
                raf.writeIntLE(0)
                raf.writeIntLE(0)
                raf.writeIntLE(0)

                val strlEnd = raf.filePointer
                val hdrlEnd = raf.filePointer

                // Patch strl and hdrl sizes
                raf.seek(strlSizePos)
                raf.writeIntLE((strlEnd - strlStart).toInt())
                raf.seek(hdrlSizePos)
                raf.writeIntLE((hdrlEnd - hdrlStart).toInt())
                raf.seek(hdrlEnd)

                // LIST 'movi'
                raf.writeAscii("LIST")
                val moviSizePos = raf.filePointer
                raf.writeIntLE(0) // patched after frames
                val moviTypePos = raf.filePointer
                raf.writeAscii("movi")

                var maxFrameSize = 0
                val baos = ByteArrayOutputStream(outW * outH / 4)

                for (i in 0 until totalFrames) {
                    coroutineContext.ensureActive()
                    val timeUs = ((i.toDouble() / max(1, totalFrames - 1)) * durationMs * 1000.0)
                        .toLong()
                        .coerceAtLeast(0L)

                    val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: continue

                    val sizedFrame = if (rawFrame.width != outW || rawFrame.height != outH) {
                        Bitmap.createScaledBitmap(rawFrame, outW, outH, true).also {
                            if (it !== rawFrame) rawFrame.recycle()
                        }
                    } else {
                        rawFrame
                    }

                    baos.reset()
                    sizedFrame.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(50, 100), baos)
                    sizedFrame.recycle()

                    val jpegBytes = baos.toByteArray()
                    val offsetFromMoviType = (raf.filePointer - moviTypePos).toInt()
                    raf.writeAscii("00dc")
                    raf.writeIntLE(jpegBytes.size)
                    raf.write(jpegBytes)
                    if (jpegBytes.size % 2 != 0) {
                        raf.writeByte(0)
                    }

                    indexEntries.add(IndexEntry(offsetFromMoviType, jpegBytes.size))
                    if (jpegBytes.size > maxFrameSize) {
                        maxFrameSize = jpegBytes.size
                    }

                    val pct = (((i + 1) * 100f) / totalFrames.toFloat()).roundToInt().coerceIn(1, 98)
                    onProgress(pct)
                }

                val moviEnd = raf.filePointer
                raf.seek(moviSizePos)
                raf.writeIntLE((moviEnd - moviTypePos).toInt())
                raf.seek(moviEnd)

                // 'idx1' index chunk
                raf.writeAscii("idx1")
                raf.writeIntLE(indexEntries.size * 16)
                for (entry in indexEntries) {
                    raf.writeAscii("00dc")
                    raf.writeIntLE(0x10) // AVIIF_KEYFRAME
                    raf.writeIntLE(entry.offsetFromMoviType)
                    raf.writeIntLE(entry.size)
                }

                val fileEnd = raf.filePointer

                // Patch RIFF size and actual written frame counts
                raf.seek(riffSizePos)
                raf.writeIntLE((fileEnd - 8L).toInt())

                raf.seek(avihPayloadPos + 4)
                raf.writeIntLE(maxFrameSize * fps)
                raf.seek(totalFramesPos)
                raf.writeIntLE(indexEntries.size)
                raf.seek(suggestedBufAvihPos)
                raf.writeIntLE(maxFrameSize.coerceAtLeast(outW * outH))

                raf.seek(strhLengthPos)
                raf.writeIntLE(indexEntries.size)
                raf.seek(suggestedBufStrhPos)
                raf.writeIntLE(maxFrameSize.coerceAtLeast(outW * outH))
            }
            onProgress(100)
            outputAviFile
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun RandomAccessFile.writeAscii(tag: String) {
        write(tag.toByteArray(Charsets.US_ASCII))
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
        write(buf)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        val buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
        write(buf)
    }
}
