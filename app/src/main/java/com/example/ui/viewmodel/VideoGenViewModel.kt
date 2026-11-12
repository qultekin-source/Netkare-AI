package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.InteractionsVideoService
import com.example.data.local.EnhancementRecord
import com.example.data.local.HistoryRepository
import com.example.data.media.MediaProcessingEngine
import com.example.domain.model.AiState
import com.example.domain.model.CompletionMediaType
import com.example.domain.model.EnhancementCompletionEvent
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.ExportUiState
import com.example.domain.model.GeneratedVideoResult
import com.example.domain.model.VideoAspectRatio
import com.example.domain.model.VideoExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VideoGenViewModel"

data class VideoGenUiState(
    val prompt: String = "",
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.LANDSCAPE_16_9,
    val resolution: String = "720p",
    val duration: String = "5s",
    val progressStatusText: String = "",
    val generationState: AiState<GeneratedVideoResult> = AiState.Idle,
    val lastInteractionId: String? = null,
    val completionEvent: EnhancementCompletionEvent? = null,
    val exportState: ExportUiState = ExportUiState(),
    val statusMessage: String? = null
)

class VideoGenViewModel(
    application: Application,
    private val interactionsService: InteractionsVideoService,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoGenUiState())
    val uiState: StateFlow<VideoGenUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun updatePrompt(value: String) {
        _uiState.update { it.copy(prompt = value) }
    }

    fun selectAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun selectResolution(res: String) {
        _uiState.update { it.copy(resolution = res) }
    }

    fun selectDuration(dur: String) {
        _uiState.update { it.copy(duration = dur) }
    }

    fun clearInteractionChain() {
        _uiState.update {
            it.copy(
                lastInteractionId = null,
                statusMessage = "Yeni video oturumu başlatıldı."
            )
        }
    }

    fun generateVideo(refinePrevious: Boolean = false) {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isEmpty()) return

        val state = _uiState.value
        val prevId = if (refinePrevious) state.lastInteractionId else null

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    generationState = AiState.Loading,
                    progressStatusText = "Interactions API hazırlanıyor...",
                    statusMessage = null
                )
            }
            try {
                val result = interactionsService.generateVideoFromText(
                    context = getApplication(),
                    prompt = prompt,
                    aspectRatio = state.aspectRatio,
                    duration = state.duration,
                    resolution = state.resolution,
                    previousInteractionId = prevId,
                    onStatusUpdate = { statusMsg ->
                        _uiState.update { it.copy(progressStatusText = statusMsg) }
                    }
                )
                historyRepository.addRecord(
                    EnhancementRecord(
                        mediaType = "VIDEO",
                        title = prompt.take(36),
                        presetName = "Omni 1.2 Flash (${state.aspectRatio.apiValue})",
                        sourceResolution = "Metinden Video",
                        targetResolution = "${state.resolution} (${state.aspectRatio.apiValue})",
                        outputPath = result.filePath,
                        aiSummary = prompt
                    )
                )
                _uiState.update {
                    it.copy(
                        generationState = AiState.Success(result),
                        lastInteractionId = result.interactionId,
                        progressStatusText = "",
                        completionEvent = EnhancementCompletionEvent(
                            mediaType = CompletionMediaType.GENERATED_VIDEO,
                            titleTr = "Video Oluşturuldu!",
                            messageTr = "Yapay zeka videonuz tamamlandı ve paylaşıma hazır.",
                            specSummaryTr = "${state.resolution} • ${state.aspectRatio.apiValue} • ${state.duration}"
                        )
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update {
                    it.copy(
                        generationState = AiState.Idle,
                        progressStatusText = "",
                        statusMessage = "Video oluşturma iptal edildi."
                    )
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Text-to-video generation failed", e)
                _uiState.update {
                    it.copy(
                        generationState = AiState.Error(
                            e.localizedMessage ?: "Video üretimi başarısız oldu."
                        ),
                        progressStatusText = ""
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
    }

    fun openExportOptions() {
        val result = (_uiState.value.generationState as? AiState.Success)?.data ?: return
        if (!File(result.filePath).exists()) return
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

    fun exportGeneratedVideoWithOptions(
        format: VideoExportFormat,
        quality: ExportQualityTier,
        shareAfterExport: Boolean
    ) {
        val result = (_uiState.value.generationState as? AiState.Success)?.data ?: return
        val sourceFile = File(result.filePath)
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
                        "Üretilen Videoyu Paylaş (${format.extension.uppercase()})"
                    )
                } else {
                    val saved = MediaProcessingEngine.saveExportedVideoToDevice(
                        context = getApplication(),
                        sourceFile = exportedFile,
                        format = format,
                        displayBaseName = "NetKare_Omni_${System.currentTimeMillis()}"
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
                Log.e(TAG, "Generated video export failed", e)
                _uiState.update {
                    it.copy(
                        exportState = ExportUiState(isDialogVisible = true),
                        statusMessage = "Dışa aktarma hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun saveGeneratedVideoToGallery() {
        val result = (_uiState.value.generationState as? AiState.Success)?.data ?: return
        viewModelScope.launch {
            val savedUri = MediaProcessingEngine.saveVideoToGallery(
                getApplication(),
                File(result.filePath),
                "NetKare_Omni_${System.currentTimeMillis()}.mp4"
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

    fun shareGeneratedVideo() {
        val result = (_uiState.value.generationState as? AiState.Success)?.data ?: return
        MediaProcessingEngine.shareFile(
            getApplication(),
            File(result.filePath),
            "video/mp4",
            "Üretilen Videoyu Paylaş"
        )
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissCompletionEvent() {
        _uiState.update { it.copy(completionEvent = null) }
    }

    companion object {
        fun provideFactory(
            application: Application,
            interactionsService: InteractionsVideoService,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                VideoGenViewModel(application, interactionsService, historyRepository)
            }
        }
    }
}
