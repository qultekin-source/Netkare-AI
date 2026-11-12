package com.example.domain.model

import android.graphics.Bitmap
import android.net.Uri

enum class BatchMediaKind(val labelTr: String) {
    PHOTO("Fotoğraf"),
    VIDEO("Video")
}

enum class BatchItemStatus(val labelTr: String) {
    QUEUED("Sırada"),
    PROCESSING("İşleniyor"),
    COMPLETED("Tamamlandı"),
    FAILED("Başarısız"),
    CANCELLED("Durduruldu")
}

data class BatchQueueItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val mediaKind: BatchMediaKind,
    val resolutionLabel: String,
    val fileSizeLabel: String,
    val durationMs: Long = 0L,
    val thumbnail: Bitmap? = null,
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val progressPercent: Int = 0,
    val statusDetailTr: String = "Sırada bekliyor",
    val outputPath: String? = null,
    val outputMimeType: String? = null,
    val outputResolutionLabel: String? = null,
    val errorMessage: String? = null
)

data class BatchProcessingSettings(
    val enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM,
    val videoPreset: VideoEnhancementPreset = VideoEnhancementPreset.VINTAGE_RESTORE,
    val videoResolution: TargetResolution = TargetResolution.FHD_1080P,
    val photoPreset: PhotoRestorationPreset = PhotoRestorationPreset.ONE_TAP_ULTRA,
    val photoUpscale: PhotoUpscaleOption = PhotoUpscaleOption.HD_TO_2K,
    val videoExportFormat: VideoExportFormat = VideoExportFormat.MP4_H264,
    val photoExportFormat: PhotoExportFormat = PhotoExportFormat.JPEG,
    val exportQualityTier: ExportQualityTier = ExportQualityTier.HIGH,
    val autoSaveToDevice: Boolean = true
)
