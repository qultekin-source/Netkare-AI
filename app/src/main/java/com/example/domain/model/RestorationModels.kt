package com.example.domain.model

import android.graphics.Bitmap
import android.net.Uri

enum class TargetResolution(
    val heightPx: Int,
    val labelTr: String,
    val badge: String,
    val bitrateMbps: Int,
    val upscaleRouteTr: String,
    val shortRouteTr: String,
    val pixelDimsLabel: String
) {
    HD_720P(
        heightPx = 720,
        labelTr = "720p HD",
        badge = "SD→HD",
        bitrateMbps = 6,
        upscaleRouteTr = "SD'den HD'ye",
        shortRouteTr = "SD → HD",
        pixelDimsLabel = "1280×720"
    ),
    FHD_1080P(
        heightPx = 1080,
        labelTr = "1080p Full HD",
        badge = "FHD",
        bitrateMbps = 12,
        upscaleRouteTr = "SD/HD'den Full HD'ye",
        shortRouteTr = "SD/HD → FHD",
        pixelDimsLabel = "1920×1080"
    ),
    QHD_1440P(
        heightPx = 1440,
        labelTr = "1440p 2K",
        badge = "2K",
        bitrateMbps = 20,
        upscaleRouteTr = "HD'den 2K'ya",
        shortRouteTr = "HD → 2K",
        pixelDimsLabel = "2560×1440"
    ),
    UHD_4K(
        heightPx = 2160,
        labelTr = "2160p 4K UHD",
        badge = "HD→4K",
        bitrateMbps = 35,
        upscaleRouteTr = "HD'den 4K'ya",
        shortRouteTr = "HD → 4K",
        pixelDimsLabel = "3840×2160"
    )
}

enum class EnhancementLevel(
    val labelTr: String,
    val strengthPercentLabel: String,
    val subtitleTr: String,
    val tierCount: Int,
    val intensityFactor: Float,
    val sharpnessDelta: Float,
    val denoiseDelta: Float,
    val contrastDelta: Float,
    val saturationDelta: Float,
    val photoPromptHint: String
) {
    LOW(
        labelTr = "Düşük",
        strengthPercentLabel = "%65",
        subtitleTr = "Orijinal dokuyu koruyan hafif temizlik ve hızlı işleme",
        tierCount = 1,
        intensityFactor = 0.65f,
        sharpnessDelta = -0.18f,
        denoiseDelta = -0.22f,
        contrastDelta = -0.07f,
        saturationDelta = -6f,
        photoPromptHint = "Subtle low-intensity enhancement: gently reduce noise and slight softness while strictly preserving natural film texture and original tonal character."
    ),
    MEDIUM(
        labelTr = "Orta",
        strengthPercentLabel = "%100",
        subtitleTr = "Netlik, gürültü giderme ve renk arasında dengeli ayar",
        tierCount = 2,
        intensityFactor = 1.0f,
        sharpnessDelta = 0.0f,
        denoiseDelta = 0.0f,
        contrastDelta = 0.0f,
        saturationDelta = 0f,
        photoPromptHint = "Balanced medium-intensity studio restoration: cleanly remove blur, compression artifacts, and haze with natural sharpness and authentic color balance."
    ),
    HIGH(
        labelTr = "Yüksek",
        strengthPercentLabel = "%135",
        subtitleTr = "Maksimum keskinlik, derin gren temizliği ve güçlü netleştirme",
        tierCount = 3,
        intensityFactor = 1.35f,
        sharpnessDelta = 0.22f,
        denoiseDelta = 0.20f,
        contrastDelta = 0.10f,
        saturationDelta = 8f,
        photoPromptHint = "Maximum high-intensity super-resolution restoration: aggressively deblur, remove severe grain/scratches, and reconstruct crisp fine micro-details while keeping subjects accurate."
    )
}

enum class PhotoUpscaleOption(
    val shortBadge: String,
    val routeTr: String,
    val promptTarget: String
) {
    SD_TO_HD("SD → HD", "SD'den HD'ye (720p/1080p)", "HD 1080p crystal clarity"),
    HD_TO_2K("HD → 2K", "HD'den 2K'ya (1440p)", "2K Ultra HD"),
    HD_TO_4K("HD → 4K", "HD'den 4K'ya (2160p)", "4K Stüdyo Ultra HD")
}

enum class VideoEnhancementPreset(
    val titleTr: String,
    val subtitleTr: String,
    val sharpness: Float,
    val denoise: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float
) {
    VINTAGE_RESTORE(
        titleTr = "Eski Video Onarımı",
        subtitleTr = "VHS ve eski kayıt gürültüsünü temizler, kontrastı ve renkleri dengeler",
        sharpness = 0.72f,
        denoise = 0.80f,
        contrast = 0.22f,
        saturation = 18f,
        warmth = 0.04f
    ),
    SHARPEN_CLARITY(
        titleTr = "Süper Netlik",
        subtitleTr = "Bulanık kenarları keskinleştirir ve detay dokusunu güçlendirir",
        sharpness = 0.90f,
        denoise = 0.55f,
        contrast = 0.28f,
        saturation = 10f,
        warmth = 0.0f
    ),
    COLOR_REVIVE(
        titleTr = "Renk Canlandırma",
        subtitleTr = "Soluk renkleri ve düşük ışıklı sahneleri sinematik tona taşır",
        sharpness = 0.60f,
        denoise = 0.45f,
        contrast = 0.20f,
        saturation = 32f,
        warmth = 0.06f
    ),
    ARCHIVE_NATURAL(
        titleTr = "Doğal Arşiv",
        subtitleTr = "Orijinal dokuyu koruyarak hafif gürültü ve ışık iyileştirmesi uygular",
        sharpness = 0.48f,
        denoise = 0.60f,
        contrast = 0.12f,
        saturation = 8f,
        warmth = 0.02f
    )
}

enum class PhotoRestorationPreset(
    val titleTr: String,
    val subtitleTr: String,
    val promptDirective: String
) {
    ONE_TAP_ULTRA(
        titleTr = "Tek Tuş Süper Netlik",
        subtitleTr = "Bulanıklığı giderir, çözünürlüğü yükseltir, çizik ve gürültüyü temizler",
        promptDirective = "Restore this old or low-quality photograph to crystal-clear ultra-high-resolution modern quality. Remove motion blur, defocus blur, compression artifacts, film grain, dust, and scratches. Reconstruct fine natural textures and crisp edges while faithfully preserving the exact subjects, faces, composition, and framing."
    ),
    DEBLUR_FOCUS(
        titleTr = "Bulanıklık Giderme",
        subtitleTr = "Titreme ve odak kaybı kaynaklı bulanıklığı keskin şekilde düzeltir",
        promptDirective = "Deblur and sharpen this blurry photograph. Eliminate camera shake and out-of-focus softness, recover sharp architectural lines, eyes, and textures, and upscale clarity without altering the original scene or adding new objects."
    ),
    COLORIZE_REVIVE(
        titleTr = "Renk & Işık Canlandırma",
        subtitleTr = "Solmuş tonları canlandırır, dinamik aralığı ve ışık dengesini onarır",
        promptDirective = "Restore faded vintage colors, fix washed-out contrast and yellowing, recover shadow and highlight detail, and enhance natural vibrancy and clarity while keeping the photograph authentic and true to its original content."
    ),
    PORTRAIT_DETAIL(
        titleTr = "Yüz & Detay Onarımı",
        subtitleTr = "Düşük çözünürlüklü yüzleri, gözleri ve saç dokularını doğal biçimde netleştirir",
        promptDirective = "Enhance and upscale this low-resolution portrait or scene with lifelike facial, eye, clothing, and background detail restoration. Remove noise and pixelation while preserving the exact identity, expression, and natural skin texture."
    )
}

data class VideoMetadataInfo(
    val uri: Uri,
    val title: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val frameRate: Int,
    val fileSizeBytes: Long,
    val thumbnail: Bitmap?,
    val timelineThumbnails: List<Bitmap> = emptyList()
) {
    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "${width}×${height}" else "Bilinmiyor"

    val durationLabel: String
        get() {
            val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
            val mins = totalSec / 60
            val secs = totalSec % 60
            return "%02d:%02d".format(mins, secs)
        }

    val fileSizeLabel: String
        get() = when {
            fileSizeBytes <= 0L -> "—"
            fileSizeBytes < 1024L * 1024L -> "${(fileSizeBytes / 1024L).coerceAtLeast(1)} KB"
            else -> String.format("%.1f MB", fileSizeBytes / (1024f * 1024f))
        }
}

data class VideoQualitySettings(
    val targetResolution: TargetResolution = TargetResolution.FHD_1080P,
    val enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM,
    val preset: VideoEnhancementPreset = VideoEnhancementPreset.VINTAGE_RESTORE,
    val sharpness: Float = VideoEnhancementPreset.VINTAGE_RESTORE.sharpness,
    val denoise: Float = VideoEnhancementPreset.VINTAGE_RESTORE.denoise,
    val contrast: Float = VideoEnhancementPreset.VINTAGE_RESTORE.contrast,
    val saturation: Float = VideoEnhancementPreset.VINTAGE_RESTORE.saturation,
    val brightness: Float = 2.5f,
    val targetFps: Int = 30
) {
    fun withPresetAndLevel(
        newPreset: VideoEnhancementPreset,
        newLevel: EnhancementLevel
    ): VideoQualitySettings = copy(
        preset = newPreset,
        enhancementLevel = newLevel,
        sharpness = (newPreset.sharpness + newLevel.sharpnessDelta).coerceIn(0.15f, 1.0f),
        denoise = (newPreset.denoise + newLevel.denoiseDelta).coerceIn(0.10f, 1.0f),
        contrast = (newPreset.contrast + newLevel.contrastDelta).coerceIn(0.04f, 0.55f),
        saturation = (newPreset.saturation + newLevel.saturationDelta).coerceIn(-10f, 45f)
    )
}

data class AiGuidanceTip(
    val categoryTr: String,
    val messageTr: String,
    val actionBadgeTr: String
)

data class AiDiagnosticResult(
    val qualityScore: Int,
    val detectedIssues: List<String>,
    val summaryTr: String,
    val recommendedPreset: String,
    val recommendedPhotoPreset: String = "ONE_TAP_ULTRA",
    val recommendedLevel: String = "MEDIUM",
    val recommendedResolution: String,
    val suggestedSharpness: Float,
    val suggestedDenoise: Float,
    val suggestedContrast: Float,
    val suggestedSaturation: Float,
    val suggestedBrightness: Float = 3f,
    val guidanceTips: List<AiGuidanceTip> = emptyList()
)

enum class VideoProcessingStage(val stepIndex: Int, val labelTr: String) {
    IDLE(0, "Hazır"),
    ANALYZING_FRAMES(1, "Kare & Çözünürlük Analizi"),
    AI_OPTIMIZING(2, "Yapay Zeka Optimizasyonu"),
    UPSCALING_VIDEO(3, "Çözünürlük Yükseltme & Netleştirme"),
    FINALIZING_MP4(4, "Yüksek Kalite MP4 Kayıt"),
    COMPLETED(4, "İyileştirme Tamamlandı"),
    ERROR(0, "Hata Oluştu")
}

data class VideoProcessingProgress(
    val stage: VideoProcessingStage = VideoProcessingStage.IDLE,
    val progressPercent: Int = 0,
    val stageDetailTr: String = "",
    val elapsedSeconds: Int = 0,
    val outputFilePath: String? = null,
    val errorMessage: String? = null
) {
    val isRunning: Boolean
        get() = stage == VideoProcessingStage.ANALYZING_FRAMES ||
            stage == VideoProcessingStage.AI_OPTIMIZING ||
            stage == VideoProcessingStage.UPSCALING_VIDEO ||
            stage == VideoProcessingStage.FINALIZING_MP4
}

data class PhotoManualSettings(
    val sharpness: Float = 0.35f,
    val contrast: Float = 1.12f,
    val brightness: Float = 4f,
    val saturation: Float = 1.12f,
    val warmth: Float = 0.0f
)

sealed interface AiState<out T> {
    data object Idle : AiState<Nothing>
    data object Loading : AiState<Nothing>
    data class Success<T>(val data: T) : AiState<T>
    data class Error(val message: String) : AiState<Nothing>
}

enum class GeminiChatModel(
    val modelId: String,
    val labelTr: String,
    val badgeTr: String,
    val descriptionTr: String
) {
    HYDROGEN(
        modelId = "gemini-4-hydrogen",
        labelTr = "Genel Görevler",
        badgeTr = "Hydrogen",
        descriptionTr = "Günlük kurgu, restorasyon ve genel yaratıcı sorular için dengeli model"
    ),
    CARBON(
        modelId = "gemini-4-carbon",
        labelTr = "Derin Muhakeme",
        badgeTr = "Carbon",
        descriptionTr = "Karmaşık teknik analiz, kodek ve görüntü işleme mantığı için"
    ),
    ARGON(
        modelId = "gemini-4-argon",
        labelTr = "Karmaşık Etkileşim",
        badgeTr = "Argon",
        descriptionTr = "Çok adımlı senaryo, renk derecelendirme ve detaylı planlama için"
    ),
    FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        labelTr = "Hızlı Yanıt",
        badgeTr = "Flash-Lite",
        descriptionTr = "Anlık ipuçları ve hızlı sorular için ultra düşük gecikme"
    )
}

enum class ChatbotRole(
    val titleTr: String,
    val subtitleTr: String,
    val systemPromptTr: String
) {
    RESTORATION_EXPERT(
        titleTr = "Restorasyon Uzmanı",
        subtitleTr = "Eski video & fotoğraf onarım danışmanı",
        systemPromptTr = "Sen NetKare AI stüdyosunun kıdemli video ve fotoğraf restorasyon uzmanısın. Kullanıcılara eski VHS kayıtları, bulanık fotoğraflar, çözünürlük yükseltme (upscaling), gürültü azaltma ve renk düzeltme konularında net, uygulanabilir ve Türkçe rehberlik sağla."
    ),
    COLORIST_DIRECTOR(
        titleTr = "Renk & Sinematografi Yönetmeni",
        subtitleTr = "LUT, ışık ve sahne tasarımı uzmanı",
        systemPromptTr = "Sen ödüllü bir sinematografi ve renk derecelendirme (color grading) yönetmenisin. Sahne aydınlatması, kontrast dengesi, retro film dokusu ve görsel/video üretim istemleri (prompt) yazımı konusunda yaratıcı ve teknik Türkçe öneriler sun."
    ),
    TECHNICAL_ARCHITECT(
        titleTr = "Teknik Kodek & Piksel Analisti",
        subtitleTr = "Bitrate, FPS, keskinleştirme ve sıkıştırma analizi",
        systemPromptTr = "Sen dijital medya sıkıştırma, video kodekleri (H.264/HEVC), kare hızı interpolasyonu ve süper çözünürlük algoritmaları konusunda derin muhakeme yapan teknik bir mühendissin. Adım adım, analitik ve Türkçe yanıtlar ver."
    ),
    PROMPT_COACH(
        titleTr = "AI İstem (Prompt) Koçu",
        subtitleTr = "Görsel ve video üretimi için yaratıcı metin yazarı",
        systemPromptTr = "Sen yapay zeka görsel ve video üretim modelleri (Nano Banana 2.1 ve Gemini Omni 1.2 Flash) için yüksek kaliteli, sinematik ve detaylı istemler (prompt) oluşturan yaratıcı bir asistansın. Kısa, etkili ve Türkçe yanıt ver."
    )
}

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val modelName: String? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ImageAspectRatioOption(
    val labelTr: String,
    val shortBadge: String
) {
    SQUARE_1_1("1:1 Kare", "1:1"),
    LANDSCAPE_16_9("16:9 Yatay", "16:9"),
    PORTRAIT_9_16("9:16 Dikey", "9:16"),
    LANDSCAPE_4_3("4:3 Klasik", "4:3"),
    PORTRAIT_3_4("3:4 Portre", "3:4")
}

enum class VideoAspectRatio(
    val apiValue: String,
    val labelTr: String
) {
    LANDSCAPE_16_9("16:9", "16:9 Yatay (Landscape)"),
    PORTRAIT_9_16("9:16", "9:16 Dikey (Portrait)")
}

data class GeneratedVideoResult(
    val filePath: String,
    val prompt: String,
    val aspectRatio: VideoAspectRatio,
    val resolution: String,
    val duration: String,
    val interactionId: String?
)

enum class CompletionMediaType {
    VIDEO,
    PHOTO,
    GENERATED_VIDEO
}

data class EnhancementCompletionEvent(
    val id: Long = System.currentTimeMillis(),
    val mediaType: CompletionMediaType,
    val titleTr: String,
    val messageTr: String,
    val specSummaryTr: String,
    val elapsedSeconds: Int? = null
)

enum class ExportQualityTier(
    val labelTr: String,
    val badgeTr: String,
    val subtitleTr: String,
    val videoMaxHeightPx: Int?,
    val aviJpegQuality: Int,
    val photoCompressQuality: Int,
    val photoMaxDimPx: Int?
) {
    STANDARD(
        labelTr = "Standart",
        badgeTr = "Hızlı",
        subtitleTr = "720p uyumlu, düşük dosya boyutu ve hızlı paylaşım",
        videoMaxHeightPx = 720,
        aviJpegQuality = 75,
        photoCompressQuality = 80,
        photoMaxDimPx = 1280
    ),
    HIGH(
        labelTr = "Yüksek",
        badgeTr = "Önerilen",
        subtitleTr = "Orijinal hedef çözünürlük ile dengeli stüdyo kalitesi",
        videoMaxHeightPx = null,
        aviJpegQuality = 88,
        photoCompressQuality = 94,
        photoMaxDimPx = null
    ),
    ULTRA(
        labelTr = "Maksimum",
        badgeTr = "Ultra",
        subtitleTr = "En yüksek bit hızı ve kayıpsıza yakın piksel detayı",
        videoMaxHeightPx = null,
        aviJpegQuality = 96,
        photoCompressQuality = 100,
        photoMaxDimPx = null
    )
}

enum class VideoExportFormat(
    val labelTr: String,
    val extension: String,
    val mimeType: String,
    val codecBadge: String,
    val descriptionTr: String
) {
    MP4_H264(
        labelTr = "MP4 (H.264)",
        extension = "mp4",
        mimeType = "video/mp4",
        codecBadge = "H.264 AVC",
        descriptionTr = "Tüm telefonlar ve sosyal medya ile %100 uyumlu"
    ),
    AVI_MJPEG(
        labelTr = "AVI (MJPEG)",
        extension = "avi",
        mimeType = "video/x-msvideo",
        codecBadge = "RIFF MJPEG",
        descriptionTr = "Klasik masaüstü kurgu ve arşiv uyumlu kapsayıcı"
    ),
    MP4_HEVC(
        labelTr = "MP4 (H.265)",
        extension = "mp4",
        mimeType = "video/mp4",
        codecBadge = "HEVC 4K",
        descriptionTr = "Yüksek sıkıştırma verimliliği ve keskin 4K/HD detay"
    )
}

enum class PhotoExportFormat(
    val labelTr: String,
    val extension: String,
    val mimeType: String,
    val badge: String,
    val descriptionTr: String
) {
    JPEG(
        labelTr = "JPEG / JPG",
        extension = "jpg",
        mimeType = "image/jpeg",
        badge = "Evrensel",
        descriptionTr = "Hızlı paylaşım ve küçük dosya boyutu"
    ),
    PNG(
        labelTr = "PNG (Kayıpsız)",
        extension = "png",
        mimeType = "image/png",
        badge = "Kayıpsız",
        descriptionTr = "Sıfır sıkıştırma kaybı ve maksimum keskinlik"
    ),
    WEBP(
        labelTr = "WebP (Modern)",
        extension = "webp",
        mimeType = "image/webp",
        badge = "Kompakt",
        descriptionTr = "Yüksek netlikte daha küçük dosya boyutu"
    )
}

data class ExportUiState(
    val isDialogVisible: Boolean = false,
    val isExporting: Boolean = false,
    val progressPercent: Int = 0,
    val statusDetailTr: String = ""
)



