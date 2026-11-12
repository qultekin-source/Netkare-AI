package com.example.ui.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.EnhancementRecord
import com.example.data.local.HistoryRepository
import com.example.data.media.MediaProcessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MediaFilterTab(
    val labelTr: String,
    val mediaTypeKey: String?
) {
    ALL("Tümü", null),
    VIDEOS("Videolar", "VIDEO"),
    PHOTOS("Fotoğraflar", "PHOTO")
}

data class HistoryUiState(
    val selectedFilter: MediaFilterTab = MediaFilterTab.ALL,
    val filteredRecords: List<EnhancementRecord> = emptyList(),
    val allCount: Int = 0,
    val videoCount: Int = 0,
    val photoCount: Int = 0,
    val statusMessage: String? = null
)

class HistoryViewModel(
    application: Application,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _selectedFilter = MutableStateFlow(MediaFilterTab.ALL)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.allRecords,
        _selectedFilter,
        _statusMessage
    ) { allRecords, filter, status ->
        val videos = allRecords.filter { it.mediaType == "VIDEO" }
        val photos = allRecords.filter { it.mediaType == "PHOTO" }
        val filtered = when (filter) {
            MediaFilterTab.ALL -> allRecords
            MediaFilterTab.VIDEOS -> videos
            MediaFilterTab.PHOTOS -> photos
        }
        HistoryUiState(
            selectedFilter = filter,
            filteredRecords = filtered,
            allCount = allRecords.size,
            videoCount = videos.size,
            photoCount = photos.size,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun selectFilter(filter: MediaFilterTab) {
        _selectedFilter.value = filter
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    fun saveRecordToGallery(record: EnhancementRecord) {
        val file = File(record.outputPath)
        if (!file.exists()) {
            _statusMessage.value = "Dosya cihazda bulunamadı."
            return
        }
        viewModelScope.launch {
            val savedUri = if (record.mediaType == "VIDEO") {
                MediaProcessingEngine.saveVideoToGallery(
                    getApplication(),
                    file,
                    "NetKare_${System.currentTimeMillis()}.mp4"
                )
            } else {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                if (bitmap != null) {
                    MediaProcessingEngine.saveBitmapToGallery(
                        getApplication(),
                        bitmap,
                        "NetKare_Photo_${System.currentTimeMillis()}.jpg"
                    )
                } else {
                    null
                }
            }
            _statusMessage.value = if (savedUri != null) {
                if (record.mediaType == "VIDEO") {
                    "Video cihaz galerisine (Movies/NetKare AI) kaydedildi!"
                } else {
                    "Fotoğraf cihaz galerisine (Pictures/NetKare AI) kaydedildi!"
                }
            } else {
                "Galeriye kayıt başarısız oldu."
            }
        }
    }

    fun shareRecord(record: EnhancementRecord) {
        val file = File(record.outputPath)
        if (!file.exists()) {
            _statusMessage.value = "Paylaşılacak dosya bulunamadı."
            return
        }
        val mime = if (record.mediaType == "VIDEO") "video/mp4" else "image/jpeg"
        MediaProcessingEngine.shareFile(
            getApplication(),
            file,
            mime,
            "${record.title} Paylaş"
        )
    }

    fun clearStatusMessage() {
        _statusMessage.update { null }
    }

    companion object {
        fun provideFactory(
            application: Application,
            historyRepository: HistoryRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(application, historyRepository)
            }
        }
    }
}
