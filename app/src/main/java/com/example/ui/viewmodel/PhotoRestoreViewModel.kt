package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
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
import com.example.domain.model.ImageAspectRatioOption
import com.example.domain.model.PhotoExportFormat
import com.example.domain.model.PhotoManualSettings
import com.example.domain.model.PhotoRestorationPreset
import com.example.domain.model.PhotoUpscaleOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PhotoRestoreViewModel"

enum class PhotoStudioMode(val labelTr: String) {
    RESTORE("Eski Foto Onarımı"),
    CREATE_EDIT("Oluştur & Düzenle")
}

data class PhotoRestoreUiState(
    val studioMode: PhotoStudioMode = PhotoStudioMode.RESTORE,
    val sourceBitmap: Bitmap? = null,
    val sourceLabel: String = "",
    val selectedPreset: PhotoRestorationPreset = PhotoRestorationPreset.ONE_TAP_ULTRA,
    val enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM,
    val upscaleOption: PhotoUpscaleOption = PhotoUpscaleOption.HD_TO_2K,
    val upscaleTarget: String = PhotoUpscaleOption.HD_TO_2K.promptTarget,
    val aiRestoreState: AiState<Bitmap> = AiState.Idle,
    val aiDiagnosticState: AiState<AiDiagnosticResult> = AiState.Idle,
    val diagnosticApplied: Boolean = false,
    val manualModeEnabled: Boolean = false,
    val manualSettings: PhotoManualSettings = PhotoManualSettings(),
    val manualPreviewBitmap: Bitmap? = null,
    // Create & Edit with Nano Banana 2.1
    val createEditPrompt: String = "",
    val selectedAspectRatio: ImageAspectRatioOption = ImageAspectRatioOption.SQUARE_1_1,
    val aiCreateEditState: AiState<Bitmap> = AiState.Idle,
    val lastSavedFilePath: String? = null,
    val completionEvent: EnhancementCompletionEvent? = null,
    val exportState: ExportUiState = ExportUiState(),
    val statusMessage: String? = null
) {
    val activeAfterBitmap: Bitmap?
        get() = when {
            studioMode == PhotoStudioMode.CREATE_EDIT && aiCreateEditState is AiState.Success -> {
                aiCreateEditState.data
            }
            manualModeEnabled && manualPreviewBitmap != null -> manualPreviewBitmap
            aiRestoreState is AiState.Success -> aiRestoreState.data
            else -> manualPreviewBitmap
        }
}

class PhotoRestoreViewModel(
    application: Application,
    private val aiService: AiRestorationService,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhotoRestoreUiState())
    val uiState: StateFlow<PhotoRestoreUiState> = _uiState.asStateFlow()

    private var manualAdjustJob: Job? = null
    private var diagnosticJob: Job? = null

    fun setStudioMode(mode: PhotoStudioMode) {
        _uiState.update { it.copy(studioMode = mode) }
    }

    fun updateCreateEditPrompt(prompt: String) {
        _uiState.update { it.copy(createEditPrompt = prompt) }
    }

    fun selectAspectRatio(option: ImageAspectRatioOption) {
        _uiState.update { it.copy(selectedAspectRatio = option) }
    }

    fun onPhotoSelected(uri: Uri) {
        diagnosticJob?.cancel()
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUri(uri)
                _uiState.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        sourceLabel = "Galeriden Seçilen Fotoğraf",
                        aiRestoreState = AiState.Idle,
                        aiDiagnosticState = AiState.Idle,
                        diagnosticApplied = false,
                        aiCreateEditState = AiState.Idle,
                        manualPreviewBitmap = null,
                        lastSavedFilePath = null,
                        statusMessage = null
                    )
                }
                analyzePhotoWithAi(autoApply = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading selected photo", e)
                _uiState.update {
                    it.copy(statusMessage = "Fotoğraf yüklenemedi: ${e.localizedMessage}")
                }
            }
        }
    }

    fun loadSampleVintagePhoto() {
        diagnosticJob?.cancel()
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(getApplication<Application>().resources, R.drawable.img_sample_vintage)
            }
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        sourceBitmap = bmp,
                        sourceLabel = "Örnek Nostaljik İstanbul Fotoğrafı",
                        aiRestoreState = AiState.Idle,
                        aiDiagnosticState = AiState.Idle,
                        diagnosticApplied = false,
                        aiCreateEditState = AiState.Idle,
                        manualPreviewBitmap = null,
                        lastSavedFilePath = null,
                        statusMessage = "Örnek eski fotoğraf yüklendi; AI rehberi hazırlanıyor..."
                    )
                }
                analyzePhotoWithAi(autoApply = false)
            }
        }
    }

    fun clearSourcePhoto() {
        diagnosticJob?.cancel()
        _uiState.update {
            it.copy(
                sourceBitmap = null,
                sourceLabel = "",
                aiRestoreState = AiState.Idle,
                aiDiagnosticState = AiState.Idle,
                diagnosticApplied = false,
                manualPreviewBitmap = null,
                manualModeEnabled = false
            )
        }
    }

    fun useGeneratedImageAsSource() {
        val resultBmp = (_uiState.value.aiCreateEditState as? AiState.Success)?.data ?: return
        _uiState.update {
            it.copy(
                sourceBitmap = resultBmp,
                sourceLabel = "Nano Banana 2.1 Görseli",
                aiCreateEditState = AiState.Idle,
                statusMessage = "Görsel düzenleme kaynağı olarak ayarlandı. Şimdi yeni bir düzenleme istemi yazabilirsiniz."
            )
        }
    }

    fun generateImageFromPrompt() {
        val prompt = _uiState.value.createEditPrompt.trim()
        if (prompt.isEmpty()) return
        val ratio = _uiState.value.selectedAspectRatio

        viewModelScope.launch {
            _uiState.update { it.copy(aiCreateEditState = AiState.Loading, statusMessage = null) }
            try {
                val bitmap = aiService.generateImageFromPrompt(prompt, ratio)
                val savedFile = MediaProcessingEngine.saveBitmapToAppFiles(
                    getApplication(),
                    bitmap,
                    "netkare_gen"
                )
                historyRepository.addRecord(
                    EnhancementRecord(
                        mediaType = "PHOTO",
                        title = prompt.take(36),
                        presetName = "Görsel Oluştur (${ratio.shortBadge})",
                        sourceResolution = "Metin İstemi",
                        targetResolution = "${bitmap.width}×${bitmap.height}",
                        outputPath = savedFile.absolutePath,
                        aiSummary = prompt
                    )
                )
                _uiState.update {
                    it.copy(
                        aiCreateEditState = AiState.Success(bitmap),
                        lastSavedFilePath = savedFile.absolutePath,
                        completionEvent = EnhancementCompletionEvent(
                            mediaType = CompletionMediaType.PHOTO,
                            titleTr = "Görsel Oluşturuldu!",
                            messageTr = "Görseliniz Nano Banana 2.1 ile oluşturuldu ve paylaşıma hazır.",
                            specSummaryTr = "${bitmap.width}×${bitmap.height} • ${ratio.labelTr}"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Prompt image generation failed", e)
                _uiState.update {
                    it.copy(
                        aiCreateEditState = AiState.Error(
                            e.localizedMessage ?: "Görsel oluşturma başarısız oldu."
                        )
                    )
                }
            }
        }
    }

    fun editSourceImageWithPrompt() {
        val source = _uiState.value.sourceBitmap ?: return
        val prompt = _uiState.value.createEditPrompt.trim()
        if (prompt.isEmpty()) return
        val ratio = _uiState.value.selectedAspectRatio

        viewModelScope.launch {
            _uiState.update { it.copy(aiCreateEditState = AiState.Loading, statusMessage = null) }
            try {
                val edited = aiService.editImageWithPrompt(source, prompt, ratio)
                val savedFile = MediaProcessingEngine.saveBitmapToAppFiles(
                    getApplication(),
                    edited,
                    "netkare_edit"
                )
                historyRepository.addRecord(
                    EnhancementRecord(
                        mediaType = "PHOTO",
                        title = prompt.take(36),
                        presetName = "AI Görsel Düzenleme",
                        sourceResolution = "${source.width}×${source.height}",
                        targetResolution = "${edited.width}×${edited.height}",
                        outputPath = savedFile.absolutePath,
                        aiSummary = prompt
                    )
                )
                _uiState.update {
                    it.copy(
                        aiCreateEditState = AiState.Success(edited),
                        lastSavedFilePath = savedFile.absolutePath,
                        completionEvent = EnhancementCompletionEvent(
                            mediaType = CompletionMediaType.PHOTO,
                            titleTr = "Görsel Düzenlendi!",
                            messageTr = "Düzenlenen görseliniz hazır ve doğrudan paylaşıma uygun.",
                            specSummaryTr = "${source.width}×${source.height} → ${edited.width}×${edited.height}"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Prompt image editing failed", e)
                _uiState.update {
                    it.copy(
                        aiCreateEditState = AiState.Error(
                            e.localizedMessage ?: "Görsel düzenleme başarısız oldu."
                        )
                    )
                }
            }
        }
    }

    fun selectPreset(preset: PhotoRestorationPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun selectEnhancementLevel(level: EnhancementLevel) {
        _uiState.update { it.copy(enhancementLevel = level) }
    }

    fun selectUpscaleOption(option: PhotoUpscaleOption) {
        _uiState.update {
            it.copy(
                upscaleOption = option,
                upscaleTarget = option.promptTarget
            )
        }
    }

    fun selectUpscaleTarget(target: String) {
        _uiState.update { it.copy(upscaleTarget = target) }
    }

    fun toggleManualMode(enabled: Boolean) {
        _uiState.update { it.copy(manualModeEnabled = enabled) }
        if (enabled && _uiState.value.manualPreviewBitmap == null) {
            applyManualAdjustments(_uiState.value.manualSettings)
        }
    }

    fun updateManualSettings(transform: (PhotoManualSettings) -> PhotoManualSettings) {
        val updated = transform(_uiState.value.manualSettings)
        _uiState.update { it.copy(manualSettings = updated, manualModeEnabled = true) }
        applyManualAdjustments(updated)
    }

    private fun applyManualAdjustments(settings: PhotoManualSettings) {
        val base = (_uiState.value.aiRestoreState as? AiState.Success)?.data
            ?: _uiState.value.sourceBitmap
            ?: return
        manualAdjustJob?.cancel()
        manualAdjustJob = viewModelScope.launch {
            delay(80L)
            val adjusted = MediaProcessingEngine.applyLocalPhotoAdjustments(base, settings)
            _uiState.update { it.copy(manualPreviewBitmap = adjusted) }
        }
    }

    fun applyAiDiagnosticRecommendations() {
        val result = (_uiState.value.aiDiagnosticState as? AiState.Success)?.data ?: return
        val matchedPreset = PhotoRestorationPreset.entries.firstOrNull {
            it.name == result.recommendedPhotoPreset
        } ?: PhotoRestorationPreset.ONE_TAP_ULTRA
        val matchedLevel = EnhancementLevel.entries.firstOrNull {
            it.name == result.recommendedLevel
        } ?: EnhancementLevel.MEDIUM
        val matchedScale = when (result.recommendedResolution) {
            "UHD_4K" -> PhotoUpscaleOption.HD_TO_4K
            "QHD_1440P" -> PhotoUpscaleOption.HD_TO_2K
            else -> PhotoUpscaleOption.SD_TO_HD
        }
        val mappedContrast = (1.0f + result.suggestedContrast).coerceIn(0.8f, 1.55f)
        val mappedSaturation = (1.0f + (result.suggestedSaturation / 50f)).coerceIn(0.6f, 1.65f)
        val mappedBrightness = result.suggestedBrightness.coerceIn(-25f, 35f)
        val updatedManual = _uiState.value.manualSettings.copy(
            sharpness = result.suggestedSharpness.coerceIn(0.1f, 1f),
            contrast = mappedContrast,
            brightness = mappedBrightness,
            saturation = mappedSaturation
        )

        _uiState.update {
            it.copy(
                diagnosticApplied = true,
                selectedPreset = matchedPreset,
                enhancementLevel = matchedLevel,
                upscaleOption = matchedScale,
                upscaleTarget = matchedScale.promptTarget,
                manualSettings = updatedManual,
                statusMessage = "AI rehber ayarları uygulandı: ${matchedPreset.titleTr} • ${matchedLevel.labelTr}"
            )
        }
    }

    fun analyzePhotoWithAi(autoApply: Boolean = true) {
        val source = _uiState.value.sourceBitmap ?: return
        diagnosticJob?.cancel()
        diagnosticJob = viewModelScope.launch {
            _uiState.update { it.copy(aiDiagnosticState = AiState.Loading, diagnosticApplied = false) }
            try {
                val result = aiService.analyzePhoto(source)
                _uiState.update { it.copy(aiDiagnosticState = AiState.Success(result)) }
                if (autoApply) {
                    applyAiDiagnosticRecommendations()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo analysis failed", e)
                _uiState.update {
                    it.copy(
                        aiDiagnosticState = AiState.Error(
                            e.localizedMessage ?: "Fotoğraf analizi başarısız oldu."
                        )
                    )
                }
            }
        }
    }

    fun restoreOneTapWithAi() {
        val source = _uiState.value.sourceBitmap ?: return
        val preset = _uiState.value.selectedPreset
        val upscale = _uiState.value.upscaleTarget
        val level = _uiState.value.enhancementLevel

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    aiRestoreState = AiState.Loading,
                    manualModeEnabled = false,
                    manualPreviewBitmap = null,
                    statusMessage = null
                )
            }
            try {
                val restoredBitmap = aiService.restorePhotoWithAi(source, preset, upscale, level)
                val savedFile = MediaProcessingEngine.saveBitmapToAppFiles(
                    getApplication(),
                    restoredBitmap,
                    "netkare_photo"
                )
                historyRepository.addRecord(
                    EnhancementRecord(
                        mediaType = "PHOTO",
                        title = _uiState.value.sourceLabel.ifBlank { "Fotoğraf Onarımı" },
                        presetName = "${preset.titleTr} • ${level.labelTr}",
                        sourceResolution = "${source.width}×${source.height}",
                        targetResolution = "${restoredBitmap.width}×${restoredBitmap.height}",
                        outputPath = savedFile.absolutePath,
                        aiSummary = "${preset.titleTr} (${level.labelTr} seviye, ${_uiState.value.upscaleOption.shortBadge}) ile bulanıklık giderildi."
                    )
                )
                _uiState.update {
                    it.copy(
                        aiRestoreState = AiState.Success(restoredBitmap),
                        lastSavedFilePath = savedFile.absolutePath,
                        completionEvent = EnhancementCompletionEvent(
                            mediaType = CompletionMediaType.PHOTO,
                            titleTr = "Fotoğraf İyileştirme Başarılı!",
                            messageTr = "Fotoğrafınız başarıyla netleştirildi ve paylaşıma hazır.",
                            specSummaryTr = "${source.width}×${source.height} → ${restoredBitmap.width}×${restoredBitmap.height} • ${level.labelTr} Seviye"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "One-tap AI photo restoration failed", e)
                _uiState.update {
                    it.copy(
                        aiRestoreState = AiState.Error(
                            e.localizedMessage ?: "Yapay zeka fotoğraf onarımı başarısız oldu."
                        )
                    )
                }
            }
        }
    }

    fun openExportOptions() {
        if (_uiState.value.activeAfterBitmap == null) return
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

    fun exportPhotoWithOptions(
        format: PhotoExportFormat,
        quality: ExportQualityTier,
        shareAfterExport: Boolean
    ) {
        val bitmap = _uiState.value.activeAfterBitmap ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportState = ExportUiState(
                        isDialogVisible = true,
                        isExporting = true,
                        progressPercent = 40,
                        statusDetailTr = "${format.labelTr} (${quality.labelTr}) kodlanıyor..."
                    )
                )
            }
            try {
                val exportedFile = MediaProcessingEngine.exportPhotoWithOptions(
                    context = getApplication(),
                    bitmap = bitmap,
                    format = format,
                    quality = quality,
                    prefix = "netkare_photo_export"
                )
                if (shareAfterExport) {
                    _uiState.update {
                        it.copy(
                            lastSavedFilePath = exportedFile.absolutePath,
                            exportState = ExportUiState(),
                            statusMessage = "${format.labelTr} görseli paylaşıma hazır!"
                        )
                    }
                    MediaProcessingEngine.shareFile(
                        getApplication(),
                        exportedFile,
                        format.mimeType,
                        "Görseli Paylaş (${format.extension.uppercase()})"
                    )
                } else {
                    val saved = MediaProcessingEngine.saveExportedPhotoToGallery(
                        context = getApplication(),
                        bitmap = bitmap,
                        format = format,
                        quality = quality,
                        displayBaseName = "NetKare_Photo_${System.currentTimeMillis()}"
                    )
                    _uiState.update {
                        it.copy(
                            lastSavedFilePath = exportedFile.absolutePath,
                            exportState = ExportUiState(),
                            statusMessage = if (saved != null) {
                                "${format.labelTr} (${quality.labelTr}) kaydedildi: ${saved.second}"
                            } else {
                                "Görsel galeriye kaydedilemedi."
                            }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo export failed", e)
                _uiState.update {
                    it.copy(
                        exportState = ExportUiState(isDialogVisible = true),
                        statusMessage = "Görsel dışa aktarma hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                    )
                }
            }
        }
    }

    fun saveRestoredPhotoToGallery() {
        val bitmap = _uiState.value.activeAfterBitmap ?: return
        viewModelScope.launch {
            val appFile = MediaProcessingEngine.saveBitmapToAppFiles(
                getApplication(),
                bitmap,
                "netkare_export"
            )
            val savedUri = MediaProcessingEngine.saveBitmapToGallery(
                getApplication(),
                bitmap,
                "NetKare_Photo_${System.currentTimeMillis()}.jpg"
            )
            _uiState.update {
                it.copy(
                    lastSavedFilePath = appFile.absolutePath,
                    statusMessage = if (savedUri != null) {
                        "Görsel cihaz galerisine (Pictures/NetKare AI) kaydedildi!"
                    } else {
                        "Görsel galeriye kaydedilemedi."
                    }
                )
            }
        }
    }

    fun shareRestoredPhoto() {
        val bitmap = _uiState.value.activeAfterBitmap ?: return
        viewModelScope.launch {
            val file = _uiState.value.lastSavedFilePath?.let { File(it) }?.takeIf { it.exists() }
                ?: MediaProcessingEngine.saveBitmapToAppFiles(getApplication(), bitmap, "netkare_share")
            _uiState.update { it.copy(lastSavedFilePath = file.absolutePath) }
            MediaProcessingEngine.shareFile(
                getApplication(),
                file,
                "image/jpeg",
                "Görseli Paylaş"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissCompletionEvent() {
        _uiState.update { it.copy(completionEvent = null) }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(resolver, uri)
        }
        MediaProcessingEngine.scaleDownBitmap(decoded, 1600)
    }

    companion object {
        fun provideFactory(
            application: Application,
            aiService: AiRestorationService,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PhotoRestoreViewModel(application, aiService, historyRepository)
            }
        }
    }
}
