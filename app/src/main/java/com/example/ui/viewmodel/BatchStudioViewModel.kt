package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import com.example.domain.model.BatchItemStatus
import com.example.domain.model.BatchMediaKind
import com.example.domain.model.BatchProcessingSettings
import com.example.domain.model.BatchQueueItem
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.PhotoExportFormat
import com.example.domain.model.PhotoRestorationPreset
import com.example.domain.model.PhotoUpscaleOption
import com.example.domain.model.TargetResolution
import com.example.domain.model.VideoEnhancementPreset
import com.example.domain.model.VideoExportFormat
import com.example.domain.model.VideoQualitySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "BatchStudioViewModel"

data class BatchStudioUiState(
    val queue: List<BatchQueueItem> = emptyList(),
    val settings: BatchProcessingSettings = BatchProcessingSettings(),
    val isImportingMedia: Boolean = false,
    val isBatchRunning: Boolean = false,
    val isSavingBatch: Boolean = false,
    val activeItemId: Long? = null,
    val statusMessage: String? = null
) {
    val totalCount: Int get() = queue.size
    val queuedOrPendingCount: Int
        get() = queue.count {
            it.status == BatchItemStatus.QUEUED ||
                it.status == BatchItemStatus.FAILED ||
                it.status == BatchItemStatus.CANCELLED
        }
    val completedCount: Int get() = queue.count { it.status == BatchItemStatus.COMPLETED }
    val failedCount: Int get() = queue.count { it.status == BatchItemStatus.FAILED }
    val photoCount: Int get() = queue.count { it.mediaKind == BatchMediaKind.PHOTO }
    val videoCount: Int get() = queue.count { it.mediaKind == BatchMediaKind.VIDEO }
    val overallProgressFraction: Float
        get() {
            if (queue.isEmpty()) return 0f
            val sum = queue.sumOf { item ->
                when (item.status) {
                    BatchItemStatus.COMPLETED -> 100
                    BatchItemStatus.PROCESSING -> item.progressPercent.coerceIn(0, 99)
                    else -> 0
                }
            }
            return (sum.toFloat() / (queue.size * 100f)).coerceIn(0f, 1f)
        }
}

class BatchStudioViewModel(
    application: Application,
    private val aiService: AiRestorationService,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BatchStudioUiState())
    val uiState: StateFlow<BatchStudioUiState> = _uiState.asStateFlow()

    private val idGenerator = AtomicLong(1L)
    private var batchJob: Job? = null

    fun addMediaUris(uris: List<Uri>, forcedKind: BatchMediaKind? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingMedia = true) }
            val addedItems = mutableListOf<BatchQueueItem>()
            for (uri in uris) {
                val item = runCatching { buildQueueItemFromUri(uri, forcedKind) }.getOrNull()
                if (item != null) {
                    addedItems.add(item)
                }
            }
            _uiState.update { state ->
                val existingUris = state.queue.map { it.uri }.toSet()
                val uniqueNew = addedItems.filterNot { it.uri in existingUris }
                val message = when {
                    addedItems.isEmpty() -> "Seçilen dosyalar okunamadı."
                    uniqueNew.isEmpty() -> "Seçilen dosyalar zaten kuyrukta."
                    else -> "${uniqueNew.size} medya dosyası toplu işlem kuyruğuna eklendi."
                }
                state.copy(
                    isImportingMedia = false,
                    queue = state.queue + uniqueNew,
                    statusMessage = message
                )
            }
        }
    }

    fun addSampleBatchQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingMedia = true) }
            try {
                val app = getApplication<Application>()
                val samplePhotoUri = withContext(Dispatchers.IO) {
                    val bmp = BitmapFactory.decodeResource(app.resources, R.drawable.img_sample_vintage)
                    val outFile = File(app.cacheDir, "batch_sample_photo_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    Uri.fromFile(outFile)
                }
                val sampleVideoUri = MediaProcessingEngine.createSampleVintageVideo(app)

                val photoItem = buildQueueItemFromUri(samplePhotoUri, BatchMediaKind.PHOTO)?.copy(
                    title = "Örnek_Eski_Fotoğraf.jpg"
                )
                val videoItem = buildQueueItemFromUri(sampleVideoUri, BatchMediaKind.VIDEO)?.copy(
                    title = "Örnek_VHS_1994.mp4"
                )

                val newItems = listOfNotNull(photoItem, videoItem)
                _uiState.update { state ->
                    state.copy(
                        isImportingMedia = false,
                        queue = state.queue + newItems,
                        statusMessage = "Örnek fotoğraf ve video kuyruğa eklendi."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding sample batch queue", e)
                _uiState.update {
                    it.copy(
                        isImportingMedia = false,
                        statusMessage = "Örnek medya oluşturulamadı: ${e.localizedMessage ?: "Hata"}"
                    )
                }
            }
        }
    }

    fun removeItem(itemId: Long) {
        if (_uiState.value.activeItemId == itemId && _uiState.value.isBatchRunning) return
        _uiState.update { state ->
            state.copy(queue = state.queue.filterNot { it.id == itemId })
        }
    }

    fun moveItemUp(itemId: Long) {
        if (_uiState.value.isBatchRunning) return
        _uiState.update { state ->
            val list = state.queue.toMutableList()
            val idx = list.indexOfFirst { it.id == itemId }
            if (idx > 0) {
                val item = list.removeAt(idx)
                list.add(idx - 1, item)
            }
            state.copy(queue = list)
        }
    }

    fun moveItemDown(itemId: Long) {
        if (_uiState.value.isBatchRunning) return
        _uiState.update { state ->
            val list = state.queue.toMutableList()
            val idx = list.indexOfFirst { it.id == itemId }
            if (idx in 0 until list.lastIndex) {
                val item = list.removeAt(idx)
                list.add(idx + 1, item)
            }
            state.copy(queue = list)
        }
    }

    fun clearCompletedItems() {
        _uiState.update { state ->
            state.copy(queue = state.queue.filterNot { it.status == BatchItemStatus.COMPLETED })
        }
    }

    fun clearAllItems() {
        if (_uiState.value.isBatchRunning) return
        _uiState.update { it.copy(queue = emptyList()) }
    }

    fun updateSettings(transform: (BatchProcessingSettings) -> BatchProcessingSettings) {
        _uiState.update { it.copy(settings = transform(it.settings)) }
    }

    fun selectEnhancementLevel(level: EnhancementLevel) {
        updateSettings { it.copy(enhancementLevel = level) }
    }

    fun selectVideoResolution(res: TargetResolution) {
        updateSettings { it.copy(videoResolution = res) }
    }

    fun selectVideoPreset(preset: VideoEnhancementPreset) {
        updateSettings { it.copy(videoPreset = preset) }
    }

    fun selectPhotoPreset(preset: PhotoRestorationPreset) {
        updateSettings { it.copy(photoPreset = preset) }
    }

    fun selectPhotoUpscale(option: PhotoUpscaleOption) {
        updateSettings { it.copy(photoUpscale = option) }
    }

    fun selectVideoExportFormat(format: VideoExportFormat) {
        updateSettings { it.copy(videoExportFormat = format) }
    }

    fun selectPhotoExportFormat(format: PhotoExportFormat) {
        updateSettings { it.copy(photoExportFormat = format) }
    }

    fun selectExportQualityTier(tier: ExportQualityTier) {
        updateSettings { it.copy(exportQualityTier = tier) }
    }

    fun setAutoSaveToDevice(enabled: Boolean) {
        updateSettings { it.copy(autoSaveToDevice = enabled) }
    }

    fun startBatchProcessing() {
        if (_uiState.value.isBatchRunning) return
        val runnableIds = _uiState.value.queue
            .filter {
                it.status == BatchItemStatus.QUEUED ||
                    it.status == BatchItemStatus.FAILED ||
                    it.status == BatchItemStatus.CANCELLED
            }
            .map { it.id }

        if (runnableIds.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "İşlenecek bekleyen dosya bulunmuyor.") }
            return
        }

        // Reset failed/cancelled items back to QUEUED before starting
        _uiState.update { state ->
            state.copy(
                isBatchRunning = true,
                queue = state.queue.map { item ->
                    if (item.id in runnableIds) {
                        item.copy(
                            status = BatchItemStatus.QUEUED,
                            progressPercent = 0,
                            statusDetailTr = "İşlem sırası bekleniyor",
                            errorMessage = null
                        )
                    } else {
                        item
                    }
                }
            )
        }

        batchJob = viewModelScope.launch {
            var processedOk = 0
            var processedErr = 0

            try {
                for (itemId in runnableIds) {
                    val currentItem = _uiState.value.queue.firstOrNull { it.id == itemId } ?: continue
                    val activeSettings = _uiState.value.settings

                    updateItem(itemId) {
                        it.copy(
                            status = BatchItemStatus.PROCESSING,
                            progressPercent = 5,
                            statusDetailTr = "Başlatılıyor..."
                        )
                    }
                    _uiState.update { it.copy(activeItemId = itemId) }

                    try {
                        when (currentItem.mediaKind) {
                            BatchMediaKind.PHOTO -> processSinglePhoto(currentItem, activeSettings)
                            BatchMediaKind.VIDEO -> processSingleVideo(currentItem, activeSettings)
                        }
                        processedOk++
                    } catch (e: CancellationException) {
                        updateItem(itemId) {
                            it.copy(
                                status = BatchItemStatus.CANCELLED,
                                statusDetailTr = "Kullanıcı tarafından durduruldu"
                            )
                        }
                        throw e
                    } catch (e: Exception) {
                        processedErr++
                        Log.e(TAG, "Batch item failed: ${currentItem.title}", e)
                        updateItem(itemId) {
                            it.copy(
                                status = BatchItemStatus.FAILED,
                                progressPercent = 0,
                                statusDetailTr = "İşlem başarısız oldu",
                                errorMessage = e.localizedMessage ?: "Bilinmeyen hata"
                            )
                        }
                    }
                }

                val summaryMsg = if (processedErr == 0) {
                    "Toplu işlem tamamlandı: $processedOk dosya başarıyla iyileştirildi!"
                } else {
                    "Toplu işlem bitti: $processedOk başarılı, $processedErr hatalı dosya."
                }
                _uiState.update {
                    it.copy(
                        isBatchRunning = false,
                        activeItemId = null,
                        statusMessage = summaryMsg
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update {
                    it.copy(
                        isBatchRunning = false,
                        activeItemId = null,
                        statusMessage = "Toplu işlem kuyruğu durduruldu."
                    )
                }
                throw e
            }
        }
    }

    fun stopBatchProcessing() {
        batchJob?.cancel()
        batchJob = null
    }

    fun saveCompletedItemToDevice(itemId: Long) {
        val item = _uiState.value.queue.firstOrNull {
            it.id == itemId && it.status == BatchItemStatus.COMPLETED
        } ?: return
        val path = item.outputPath ?: return
        val file = File(path)
        if (!file.exists()) return
        val settings = _uiState.value.settings

        viewModelScope.launch {
            val savedMsg = if (item.mediaKind == BatchMediaKind.PHOTO) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                val saved = if (bmp != null) {
                    MediaProcessingEngine.saveExportedPhotoToGallery(
                        context = getApplication(),
                        bitmap = bmp,
                        format = settings.photoExportFormat,
                        quality = settings.exportQualityTier,
                        displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
                    ).also { bmp.recycle() }
                } else null
                if (saved != null) "${item.title} kaydedildi (${saved.second})." else "${item.title} kaydedilemedi."
            } else {
                val saved = MediaProcessingEngine.saveExportedVideoToDevice(
                    context = getApplication(),
                    sourceFile = file,
                    format = settings.videoExportFormat,
                    displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
                )
                if (saved != null) "${item.title} kaydedildi (${saved.second})." else "${item.title} kaydedilemedi."
            }
            _uiState.update { it.copy(statusMessage = savedMsg) }
        }
    }

    fun shareCompletedItem(itemId: Long) {
        val item = _uiState.value.queue.firstOrNull {
            it.id == itemId && it.status == BatchItemStatus.COMPLETED
        } ?: return
        val path = item.outputPath ?: return
        val file = File(path)
        if (!file.exists()) return
        val mime = item.outputMimeType ?: if (item.mediaKind == BatchMediaKind.VIDEO) "video/mp4" else "image/jpeg"
        MediaProcessingEngine.shareFile(
            context = getApplication(),
            file = file,
            mimeType = mime,
            chooserTitle = "${item.title} Paylaş"
        )
    }

    fun saveAllCompletedToDevice() {
        val completed = _uiState.value.queue.filter {
            it.status == BatchItemStatus.COMPLETED && it.outputPath != null
        }
        if (completed.isEmpty() || _uiState.value.isSavingBatch) return
        val settings = _uiState.value.settings

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingBatch = true) }
            var savedCount = 0
            for (item in completed) {
                val file = File(item.outputPath!!)
                if (!file.exists()) continue
                val ok = if (item.mediaKind == BatchMediaKind.PHOTO) {
                    val bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
                    if (bmp != null) {
                        MediaProcessingEngine.saveExportedPhotoToGallery(
                            context = getApplication(),
                            bitmap = bmp,
                            format = settings.photoExportFormat,
                            quality = settings.exportQualityTier,
                            displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
                        ).also { bmp.recycle() } != null
                    } else false
                } else {
                    MediaProcessingEngine.saveExportedVideoToDevice(
                        context = getApplication(),
                        sourceFile = file,
                        format = settings.videoExportFormat,
                        displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
                    ) != null
                }
                if (ok) savedCount++
            }
            _uiState.update {
                it.copy(
                    isSavingBatch = false,
                    statusMessage = "$savedCount / ${completed.size} dosya cihaza kaydedildi!"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private suspend fun processSinglePhoto(
        item: BatchQueueItem,
        settings: BatchProcessingSettings
    ) {
        updateItem(item.id) {
            it.copy(progressPercent = 20, statusDetailTr = "Fotoğraf hazırlanıyor...")
        }
        val sourceBitmap = decodeBitmapFromUri(item.uri)
        updateItem(item.id) {
            it.copy(
                progressPercent = 45,
                statusDetailTr = "AI onarımı uygulanıyor (${settings.photoPreset.titleTr})..."
            )
        }

        val restoredBitmap = aiService.restorePhotoWithAi(
            sourceBitmap = sourceBitmap,
            preset = settings.photoPreset,
            upscaleTargetLabel = settings.photoUpscale.promptTarget,
            enhancementLevel = settings.enhancementLevel
        )

        updateItem(item.id) {
            it.copy(
                progressPercent = 85,
                statusDetailTr = "${settings.photoExportFormat.labelTr} olarak dışa aktarılıyor..."
            )
        }

        val exportedFile = MediaProcessingEngine.exportPhotoWithOptions(
            context = getApplication(),
            bitmap = restoredBitmap,
            format = settings.photoExportFormat,
            quality = settings.exportQualityTier,
            prefix = "netkare_batch_photo"
        )

        if (settings.autoSaveToDevice) {
            MediaProcessingEngine.saveExportedPhotoToGallery(
                context = getApplication(),
                bitmap = restoredBitmap,
                format = settings.photoExportFormat,
                quality = settings.exportQualityTier,
                displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
            )
        }

        val targetRes = "${restoredBitmap.width}×${restoredBitmap.height}"
        historyRepository.addRecord(
            EnhancementRecord(
                mediaType = "PHOTO",
                title = item.title,
                presetName = "Toplu • ${settings.photoPreset.titleTr} (${settings.enhancementLevel.labelTr})",
                sourceResolution = item.resolutionLabel,
                targetResolution = targetRes,
                outputPath = exportedFile.absolutePath,
                aiSummary = "Toplu işleme ile ${settings.photoUpscale.shortBadge} ve ${settings.photoExportFormat.labelTr} olarak onarıldı."
            )
        )

        updateItem(item.id) {
            it.copy(
                status = BatchItemStatus.COMPLETED,
                progressPercent = 100,
                statusDetailTr = if (settings.autoSaveToDevice) {
                    "Tamamlandı ve galeriye kaydedildi (${settings.photoExportFormat.extension.uppercase()})"
                } else {
                    "Tamamlandı (${settings.photoExportFormat.extension.uppercase()})"
                },
                outputPath = exportedFile.absolutePath,
                outputMimeType = settings.photoExportFormat.mimeType,
                outputResolutionLabel = targetRes
            )
        }
    }

    private suspend fun processSingleVideo(
        item: BatchQueueItem,
        settings: BatchProcessingSettings
    ) {
        val videoSettings = VideoQualitySettings(
            targetResolution = settings.videoResolution,
            enhancementLevel = settings.enhancementLevel,
            preset = settings.videoPreset
        ).withPresetAndLevel(settings.videoPreset, settings.enhancementLevel)

        updateItem(item.id) {
            it.copy(
                progressPercent = 12,
                statusDetailTr = "${settings.videoResolution.labelTr} ölçekleme hazırlanıyor..."
            )
        }

        val enhancedMp4 = MediaProcessingEngine.enhanceVideoWithTransformer(
            context = getApplication(),
            sourceUri = item.uri,
            settings = videoSettings,
            onProgressPercent = { pct ->
                val mapped = (12 + (pct * 0.68f)).toInt().coerceIn(12, 80)
                updateItem(item.id) {
                    it.copy(
                        progressPercent = mapped,
                        statusDetailTr = "Kare işleniyor (%$pct) • ${settings.videoResolution.labelTr}"
                    )
                }
            }
        )

        val finalExportedFile = if (
            settings.videoExportFormat != VideoExportFormat.MP4_H264 ||
            settings.exportQualityTier == ExportQualityTier.STANDARD
        ) {
            updateItem(item.id) {
                it.copy(
                    progressPercent = 82,
                    statusDetailTr = "${settings.videoExportFormat.labelTr} formatına dönüştürülüyor..."
                )
            }
            MediaProcessingEngine.exportVideoWithOptions(
                context = getApplication(),
                sourceVideoFile = enhancedMp4,
                format = settings.videoExportFormat,
                quality = settings.exportQualityTier,
                onProgressPercent = { pct ->
                    val mapped = (82 + (pct * 0.14f)).toInt().coerceIn(82, 96)
                    updateItem(item.id) {
                        it.copy(progressPercent = mapped)
                    }
                }
            )
        } else {
            enhancedMp4
        }

        if (settings.autoSaveToDevice) {
            MediaProcessingEngine.saveExportedVideoToDevice(
                context = getApplication(),
                sourceFile = finalExportedFile,
                format = settings.videoExportFormat,
                displayBaseName = "NetKare_Batch_${item.id}_${System.currentTimeMillis()}"
            )
        }

        val targetResLabel = settings.videoResolution.labelTr
        historyRepository.addRecord(
            EnhancementRecord(
                mediaType = "VIDEO",
                title = item.title,
                presetName = "Toplu • ${settings.videoPreset.titleTr} (${settings.enhancementLevel.labelTr})",
                sourceResolution = item.resolutionLabel,
                targetResolution = targetResLabel,
                outputPath = finalExportedFile.absolutePath,
                aiSummary = "Toplu video işleme ile $targetResLabel (${settings.videoExportFormat.labelTr}) olarak dışa aktarıldı.",
                durationMs = item.durationMs
            )
        )

        updateItem(item.id) {
            it.copy(
                status = BatchItemStatus.COMPLETED,
                progressPercent = 100,
                statusDetailTr = if (settings.autoSaveToDevice) {
                    "Tamamlandı ve cihaza kaydedildi (${settings.videoExportFormat.extension.uppercase()})"
                } else {
                    "Tamamlandı (${settings.videoExportFormat.extension.uppercase()})"
                },
                outputPath = finalExportedFile.absolutePath,
                outputMimeType = settings.videoExportFormat.mimeType,
                outputResolutionLabel = targetResLabel
            )
        }
    }

    private suspend fun buildQueueItemFromUri(
        uri: Uri,
        forcedKind: BatchMediaKind?
    ): BatchQueueItem? = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val mime = resolver.getType(uri)?.lowercase()
        val kind = forcedKind ?: when {
            mime?.startsWith("video/") == true -> BatchMediaKind.VIDEO
            mime?.startsWith("image/") == true -> BatchMediaKind.PHOTO
            uri.toString().lowercase().let { it.endsWith(".mp4") || it.endsWith(".avi") || it.endsWith(".mkv") } ->
                BatchMediaKind.VIDEO
            else -> BatchMediaKind.PHOTO
        }

        val id = idGenerator.getAndIncrement()
        return@withContext if (kind == BatchMediaKind.VIDEO) {
            val meta = MediaProcessingEngine.extractVideoMetadata(getApplication(), uri)
            BatchQueueItem(
                id = id,
                uri = uri,
                title = meta.title,
                mediaKind = BatchMediaKind.VIDEO,
                resolutionLabel = meta.resolutionLabel,
                fileSizeLabel = meta.fileSizeLabel,
                durationMs = meta.durationMs,
                thumbnail = meta.thumbnail?.let { MediaProcessingEngine.scaleDownBitmap(it, 200) }
            )
        } else {
            val (name, size) = queryDisplayNameAndSize(uri)
            val bmp = decodeBitmapFromUri(uri)
            val thumb = MediaProcessingEngine.scaleDownBitmap(bmp, 200)
            BatchQueueItem(
                id = id,
                uri = uri,
                title = name ?: "Fotoğraf_$id.jpg",
                mediaKind = BatchMediaKind.PHOTO,
                resolutionLabel = "${bmp.width}×${bmp.height}",
                fileSizeLabel = formatBytes(size),
                thumbnail = thumb
            )
        }
    }

    private suspend fun decodeBitmapFromUri(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
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

    private fun queryDisplayNameAndSize(uri: Uri): Pair<String?, Long> {
        if (uri.scheme == "file") {
            val file = uri.path?.let { File(it) }
            return Pair(file?.name, file?.length() ?: 0L)
        }
        var name: String? = null
        var size = 0L
        runCatching {
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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

    private fun formatBytes(bytes: Long): String = when {
        bytes <= 0L -> "—"
        bytes < 1024L * 1024L -> "${(bytes / 1024L).coerceAtLeast(1)} KB"
        else -> String.format("%.1f MB", bytes / (1024f * 1024f))
    }

    private fun updateItem(itemId: Long, transform: (BatchQueueItem) -> BatchQueueItem) {
        _uiState.update { state ->
            state.copy(
                queue = state.queue.map { item ->
                    if (item.id == itemId) transform(item) else item
                }
            )
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            aiService: AiRestorationService,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BatchStudioViewModel(application, aiService, historyRepository)
            }
        }
    }
}
