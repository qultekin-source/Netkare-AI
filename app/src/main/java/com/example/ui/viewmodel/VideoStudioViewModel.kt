package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiRestorationService
import com.example.data.local.EnhancementRecord
import com.example.data.local.HistoryRepository
import com.example.data.media.MediaProcessingEngine
import com.example.domain.model.AiDiagnosticResult
import com.example.domain.model.AiState
import com.example.domain.model.CompletionMediaType
import com.example.domain.model.EnhancementCompletionEvent
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.ExportUiState
import com.example.domain.model.TargetResolution
import com.example.domain.model.VideoEnhancementPreset
import com.example.domain.model.VideoExportFormat
import com.example.domain.model.VideoMetadataInfo
import com.example.domain.model.VideoProcessingProgress
import com.example.domain.model.VideoProcessingStage
import com.example.domain.model.VideoQualitySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VideoStudioViewModel"

data class VideoStudioUiState(
    val selectedVideo: VideoMetadataInfo? = null,
    val isLoadingVideo: Boolean = false,
    val isCreatingSample: Boolean = false,
    val qualitySettings: VideoQualitySettings = VideoQualitySettings(),
    val aiDiagnosticState: AiState<AiDiagnosticResult> = AiState.Idle,
    val diagnosticApplied: Boolean = false,
    val processingProgress: VideoProcessingProgress = VideoProcessingProgress(),
    val showAfterPreview: Boolean = true,
    val completionEvent: EnhancementCompletionEvent? = null,
    val exportState: ExportUiState = ExportUiState(),
    val statusMessage: String? = null
)

class VideoStudioViewModel(
    application: Application,
    private val aiService: AiRestorationService,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoStudioUiState())
    val uiState: StateFlow<VideoStudioUiState> = _uiState.asStateFlow()

    private var enhancementJob: Job? = null
    private var diagnosticJob: Job? = null

    fun onVideoSelected(uri: Uri) {
        enhancementJob?.cancel()
        diagnosticJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVideo = true, statusMessage = null) }
            try {
                val meta = MediaProcessingEngine.extractVideoMetadata(getApplication(), uri)
                _uiState.update {
                    it.copy(
                        selectedVideo = meta,
                        isLoadingVideo = false,
                        aiDiagnosticState = AiState.Idle,
                        diagnosticApplied = false,
                        processingProgress = VideoProcessingProgress(),
                        statusMessage = null
                    )
                }
                analyzeVideoWithAi(autoApply = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading video metadata", e)
                _uiState.update {
                    it.copy(
                        isLoadingVideo = false,
                        statusMessage = "Video okunamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun clearSelectedVideo() {
        enhancementJob?.cancel()
        diagnosticJob?.cancel()
        _uiState.update {
            it.copy(
                selectedVideo = null,
                isLoadingVideo = false,
                aiDiagnosticState = AiState.Idle,
                diagnosticApplied = false,
                processingProgress = VideoProcessingProgress(),
                statusMessage = null
            )
        }
    }

    fun createAndLoadSampleVideo() {
        enhancementJob?.cancel()
        diagnosticJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSample = true, statusMessage = null) }
            try {
                val sampleUri = MediaProcessingEngine.createSampleVintageVideo(getApplication())
                val meta = MediaProcessingEngine.extractVideoMetadata(getApplication(), sampleUri)
                _uiState.update {
                    it.copy(
                        selectedVideo = meta,
                        isCreatingSample = false,
                        aiDiagnosticState = AiState.Idle,
                        diagnosticApplied = false,
                        processingProgress = VideoProcessingProgress(),
                        statusMessage = "Örnek 1994 VHS video klibi yüklendi; AI rehberi hazırlanıyor..."
                    )
                }
                analyzeVideoWithAi(autoApply = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating sample video", e)
                _uiState.update {
                    it.copy(
                        isCreatingSample = false,
                        statusMessage = "Örnek video oluşturulamadı: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectResolution(resolution: TargetResolution) {
        _uiState.update {
            it.copy(qualitySettings = it.qualitySettings.copy(targetResolution = resolution))
        }
    }

    fun selectEnhancementLevel(level: EnhancementLevel) {
        _uiState.update { state ->
            state.copy(
                qualitySettings = state.qualitySettings.withPresetAndLevel(
                    newPreset = state.qualitySettings.preset,
                    newLevel = level
                )
            )
        }
    }

    fun selectPreset(preset: VideoEnhancementPreset) {
        _uiState.update { state ->
            state.copy(
                qualitySettings = state.qualitySettings.withPresetAndLevel(
                    newPreset = preset,
                    newLevel = state.qualitySettings.enhancementLevel
                )
            )
        }
    }

    fun updateSharpness(value: Float) {
        _uiState.update { it.copy(qualitySettings = it.qualitySettings.copy(sharpness = value)) }
    }

    fun updateDenoise(value: Float) {
        _uiState.update { it.copy(qualitySettings = it.qualitySettings.copy(denoise = value)) }
    }

    fun updateContrast(value: Float) {
        _uiState.update { it.copy(qualitySettings = it.qualitySettings.copy(contrast = value)) }
    }

    fun updateSaturation(value: Float) {
        _uiState.update { it.copy(qualitySettings = it.qualitySettings.copy(saturation = value)) }
    }

    fun updateBrightness(value: Float) {
        _uiState.update { it.copy(qualitySettings = it.qualitySettings.copy(brightness = value)) }
    }

    fun toggleBeforeAfterPreview(showAfter: Boolean) {
        _uiState.update { it.copy(showAfterPreview = showAfter) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissCompletionEvent() {
        _uiState.update { it.copy(completionEvent = null) }
    }

    fun applyAiDiagnosticRecommendations() {
        val result = (_uiState.value.aiDiagnosticState as? AiState.Success)?.data ?: return
        val matchedPreset = VideoEnhancementPreset.entries.firstOrNull {
            it.name == result.recommendedPreset
        } ?: VideoEnhancementPreset.VINTAGE_RESTORE
        val matchedResolution = TargetResolution.entries.firstOrNull {
            it.name == result.recommendedResolution
        } ?: TargetResolution.FHD_1080P
        val matchedLevel = EnhancementLevel.entries.firstOrNull {
            it.name == result.recommendedLevel
        } ?: EnhancementLevel.MEDIUM

        _uiState.update { state ->
            state.copy(
                diagnosticApplied = true,
                qualitySettings = state.qualitySettings.copy(
                    targetResolution = matchedResolution,
                    enhancementLevel = matchedLevel,
                    preset = matchedPreset,
                    sharpness = result.suggestedSharpness,
                    denoise = result.suggestedDenoise,
                    contrast = result.suggestedContrast,
                    saturation = result.suggestedSaturation,
                    brightness = result.suggestedBrightness
                ),
                statusMessage = "AI önerileri uygulandı: ${matchedPreset.titleTr} • ${matchedLevel.labelTr} • ${matchedResolution.labelTr}"
            )
        }
    }

    fun analyzeVideoWithAi(autoApply: Boolean = true) {
        val video = _uiState.value.selectedVideo ?: return
        diagnosticJob?.cancel()
        diagnosticJob = viewModelScope.launch {
            _uiState.update { it.copy(aiDiagnosticState = AiState.Loading, diagnosticApplied = false) }
            try {
                val frames = MediaProcessingEngine.extractRepresentativeFrames(
                    getApplication(),
                    video.uri,
                    count = 3
                )
                val result = aiService.analyzeVideoFrames(
                    frames = frames,
                    width = video.width,
                    height = video.height,
                    durationMs = video.durationMs
                )
                _uiState.update { it.copy(aiDiagnosticState = AiState.Success(result)) }
                if (autoApply) {
                    applyAiDiagnosticRecommendations()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI video analysis error", e)
                _uiState.update {
                    it.copy(
                        aiDiagnosticState = AiState.Error(
                            e.localizedMessage ?: "Yapay zeka analizi başarısız oldu."
                        )
                    )
                }
            }
        }
    }

    fun startEnhancement() {
        val video = _uiState.value.selectedVideo ?: return
        val settings = _uiState.value.qualitySettings
        enhancementJob?.cancel()

        enhancementJob = viewModelScope.launch {
            val startTimeMs = System.currentTimeMillis()
            val timerJob = launch {
                while (true) {
                    delay(1000L)
                    val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                    _uiState.update {
                        it.copy(processingProgress = it.processingProgress.copy(elapsedSeconds = elapsed))
                    }
                }
            }

            try {
                // Stage 1: Frame & Resolution Inspection
                _uiState.update {
                    it.copy(
                        processingProgress = VideoProcessingProgress(
                            stage = VideoProcessingStage.ANALYZING_FRAMES,
                            progressPercent = 10,
                            stageDetailTr = "Video kareleri (${video.resolutionLabel}) ve gürültü profili taranıyor..."
                        )
                    )
                }
                val frames = MediaProcessingEngine.extractRepresentativeFrames(getApplication(), video.uri, 2)

                // Stage 2: AI Parameter Optimization (runs Gemini if not yet analyzed, or confirms profile)
                _uiState.update {
                    it.copy(
                        processingProgress = it.processingProgress.copy(
                            stage = VideoProcessingStage.AI_OPTIMIZING,
                            progressPercent = 24,
                            stageDetailTr = "Yapay zeka (${settings.preset.titleTr}) restorasyon matrisini optimize ediyor..."
                        )
                    )
                }
                var activeSettings = settings
                var aiSummaryText = settings.preset.subtitleTr
                if (_uiState.value.aiDiagnosticState !is AiState.Success && frames.isNotEmpty()) {
                    runCatching {
                        aiService.analyzeVideoFrames(frames, video.width, video.height, video.durationMs)
                    }.onSuccess { diag ->
                        aiSummaryText = diag.summaryTr
                        _uiState.update { it.copy(aiDiagnosticState = AiState.Success(diag)) }
                    }.onFailure { err ->
                        if (err is CancellationException) throw err
                        Log.w(TAG, "Optional pre-render AI analysis skipped: ${err.message}")
                    }
                } else if (_uiState.value.aiDiagnosticState is AiState.Success) {
                    aiSummaryText = (_uiState.value.aiDiagnosticState as AiState.Success<AiDiagnosticResult>).data.summaryTr
                }

                // Stage 3: Media3 Transformer Super-Resolution Upscaling & Color/Contrast Restoration
                _uiState.update {
                    it.copy(
                        processingProgress = it.processingProgress.copy(
                            stage = VideoProcessingStage.UPSCALING_VIDEO,
                            progressPercent = 35,
                            stageDetailTr = "${settings.targetResolution.labelTr} hedefine ölçekleniyor ve netleştiriliyor..."
                        )
                    )
                }

                val outputFile = MediaProcessingEngine.enhanceVideoWithTransformer(
                    context = getApplication(),
                    sourceUri = video.uri,
                    settings = activeSettings,
                    onProgressPercent = { transformerPct ->
                        val mapped = (35 + (transformerPct * 0.58f)).toInt().coerceIn(35, 93)
                        _uiState.update {
                            it.copy(
                                processingProgress = it.processingProgress.copy(
                                    stage = VideoProcessingStage.UPSCALING_VIDEO,
                                    progressPercent = mapped,
                                    stageDetailTr = "Kare işleniyor (%$transformerPct) — ${activeSettings.targetResolution.labelTr}"
                                )
                            )
                        }
                    }
                )

                // Stage 4: Finalize & Persist Record
                _uiState.update {
                    it.copy(
                        processingProgress = it.processingProgress.copy(
                            stage = VideoProcessingStage.FINALIZING_MP4,
                            progressPercent = 96,
                            stageDetailTr = "MP4 dosyası ve stüdyo geçmişi kaydediliyor..."
                        )
                    )
                }

                val outputMeta = MediaProcessingEngine.extractVideoMetadata(
                    getApplication(),
                    Uri.fromFile(outputFile)
                )
                historyRepository.addRecord(
                    EnhancementRecord(
                        mediaType = "VIDEO",
                        title = video.title,
                        presetName = "${activeSettings.preset.titleTr} • ${activeSettings.enhancementLevel.labelTr}",
                        sourceResolution = video.resolutionLabel,
                        targetResolution = outputMeta.resolutionLabel.ifBlank { activeSettings.targetResolution.labelTr },
                        outputPath = outputFile.absolutePath,
                        aiSummary = aiSummaryText,
                        durationMs = video.durationMs
                    )
                )

                val finalResLabel = outputMeta.resolutionLabel.ifBlank { activeSettings.targetResolution.labelTr }
                val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                _uiState.update {
                    it.copy(
                        showAfterPreview = true,
                        processingProgress = it.processingProgress.copy(
                            stage = VideoProcessingStage.COMPLETED,
                            progressPercent = 100,
                            stageDetailTr = "Video ${activeSettings.targetResolution.labelTr} kalitesine yükseltildi!",
                            outputFilePath = outputFile.absolutePath
                        ),
                        completionEvent = EnhancementCompletionEvent(
                            mediaType = CompletionMediaType.VIDEO,
                            titleTr = "Video İyileştirme Başarılı!",
                            messageTr = "Videonuz başarıyla iyileştirildi ve yüksek kalitede paylaşıma hazır.",
                            specSummaryTr = "${video.resolutionLabel} → $finalResLabel • ${activeSettings.enhancementLevel.labelTr} Seviye",
                            elapsedSeconds = elapsed
                        )
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update {
                    it.copy(
                        processingProgress = VideoProcessingProgress(
                            stage = VideoProcessingStage.IDLE,
                            progressPercent = 0,
                            stageDetailTr = "İşlem iptal edildi."
                        ),
                        statusMessage = "Video iyileştirme iptal edildi."
                    )
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video enhancement failed", e)
                _uiState.update {
                    it.copy(
                        processingProgress = it.processingProgress.copy(
                            stage = VideoProcessingStage.ERROR,
                            errorMessage = e.localizedMessage ?: "Video işleme sırasında hata oluştu.",
                            stageDetailTr = "İşlem tamamlanamadı."
                        )
                    )
                }
            } finally {
                timerJob.cancel()
            }
        }
    }

    fun cancelEnhancement() {
        enhancementJob?.cancel()
        enhancementJob = null
    }

    fun openExportOptions() {
        val path = _uiState.value.processingProgress.outputFilePath ?: return
        if (!File(path).exists()) return
        _uiState.update {
            it.copy(
                completionEvent = null,
                exportState = ExportUiState(isDialogVisible = true)
            )
        }
    }

    fun dismissExportOptions() {
        if (_uiState.value.exportState.isExporting) return
        _uiState.update { it.copy(exportState = ExportUiState()) }
    }

    fun exportVideoWithOptions(
        format: VideoExportFormat,
        quality: ExportQualityTier,
        shareAfterExport: Boolean
    ) {
        val path = _uiState.value.processingProgress.outputFilePath ?: return
        val sourceFile = File(path)
        if (!sourceFile.exists()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportState = ExportUiState(
                        isDialogVisible = true,
                        isExporting = true,
                        progressPercent = 5,
                        statusDetailTr = "${format.labelTr} (${quality.labelTr}) hazırlanıyor..."
                    )
                )
            }
            try {
                val exportedFile = MediaProcessingEngine.exportVideoWithOptions(
                    context = getApplication(),
                    sourceVideoFile = sourceFile,
                    format = format,
                    quality = quality,
                    onProgressPercent = { pct ->
                        _uiState.update { state ->
                            state.copy(
                                exportState = state.exportState.copy(
                                    progressPercent = pct.coerceIn(5, 95),
                                    statusDetailTr = "${format.labelTr} dışa aktarılıyor..."
                                )
                            )
                        }
                    }
                )

                if (shareAfterExport) {
                    _uiState.update {
                        it.copy(
                            exportState = ExportUiState(),
                            statusMessage = "${format.labelTr} dosyası paylaşıma hazır!"
                        )
                    }
                    MediaProcessingEngine.shareFile(
                        getApplication(),
                        exportedFile,
                        format.mimeType,
                        "İyileştirilmiş Videoyu Paylaş (${format.extension.uppercase()})"
                    )
                } else {
                    val saved = MediaProcessingEngine.saveExportedVideoToDevice(
                        context = getApplication(),
                        sourceFile = exportedFile,
                        format = format,
                        displayBaseName = "NetKare_${System.currentTimeMillis()}"
                    )
                    _uiState.update {
                        it.copy(
                            exportState = ExportUiState(),
                            statusMessage = if (saved != null) {
                                "${format.labelTr} (${quality.labelTr}) kaydedildi: ${saved.second}"
                            } else {
                                "Video cihaza kaydedilemedi."
                            }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video export failed", e)
                _uiState.update {
                    it.copy(
                        exportState = ExportUiState(isDialogVisible = true),
                        statusMessage = "Dışa aktarma hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun saveEnhancedVideoToGallery() {
        val path = _uiState.value.processingProgress.outputFilePath ?: return
        val file = File(path)
        viewModelScope.launch {
            val savedUri = MediaProcessingEngine.saveVideoToGallery(
                getApplication(),
                file,
                "NetKare_${System.currentTimeMillis()}.mp4"
            )
            _uiState.update {
                it.copy(
                    statusMessage = if (savedUri != null) {
                        "Video cihaz galerisine (Movies/NetKare AI) kaydedildi!"
                    } else {
                        "Galeriye kayıt başarısız oldu."
                    }
                )
            }
        }
    }

    fun shareEnhancedVideo() {
        val path = _uiState.value.processingProgress.outputFilePath ?: return
        val file = File(path)
        MediaProcessingEngine.shareFile(
            getApplication(),
            file,
            "video/mp4",
            "İyileştirilmiş Videoyu Paylaş"
        )
    }

    companion object {
        fun provideFactory(
            application: Application,
            aiService: AiRestorationService,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VideoStudioViewModel(application, aiService, historyRepository)
            }
        }
    }
}
