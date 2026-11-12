package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.EnhancementRecord
import com.example.data.media.MediaProcessingEngine
import com.example.ui.adaptive.AdaptiveWindowState
import com.example.ui.adaptive.AppWindowWidthClass
import com.example.ui.components.ProminentPrimaryButton
import com.example.ui.components.ProminentSecondaryButton
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MediaFilterTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    adaptiveState: AdaptiveWindowState,
    onOpenInStudio: (EnhancementRecord) -> Unit,
    onNavigateToVideoStudio: () -> Unit,
    onNavigateToPhotoStudio: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale("tr")) }
    val columns = when (adaptiveState.widthClass) {
        AppWindowWidthClass.COMPACT -> GridCells.Fixed(1)
        AppWindowWidthClass.MEDIUM -> GridCells.Adaptive(minSize = 300.dp)
        AppWindowWidthClass.EXPANDED -> GridCells.Adaptive(minSize = 320.dp)
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (adaptiveState.widthClass == AppWindowWidthClass.COMPACT) 16.dp else 20.dp,
            end = if (adaptiveState.widthClass == AppWindowWidthClass.COMPACT) 16.dp else 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with total count & Clear All
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Düzenlenen Medya Arşivi",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.videoCount} video • ${uiState.photoCount} fotoğraf kaydedildi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.allCount > 0) {
                    TextButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("clear_history_button")
                    ) {
                        Text("Tümünü Temizle", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Dedicated Media Type Tabs: Tümü | Videolar | Fotoğraflar
        item(span = { GridItemSpan(maxLineSpan) }) {
            MediaTypeFilterTabsRow(
                selectedFilter = uiState.selectedFilter,
                allCount = uiState.allCount,
                videoCount = uiState.videoCount,
                photoCount = uiState.photoCount,
                onSelectFilter = viewModel::selectFilter
            )
        }

        if (uiState.filteredRecords.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HistoryEmptyStateCard(
                    selectedFilter = uiState.selectedFilter,
                    onNavigateToVideoStudio = onNavigateToVideoStudio,
                    onNavigateToPhotoStudio = onNavigateToPhotoStudio
                )
            }
        } else {
            items(uiState.filteredRecords, key = { it.id }) { record ->
                HistoryRecordCard(
                    record = record,
                    dateText = dateFormatter.format(Date(record.createdAt)),
                    onOpenInStudio = { onOpenInStudio(record) },
                    onSaveToGallery = { viewModel.saveRecordToGallery(record) },
                    onShare = { viewModel.shareRecord(record) },
                    onDelete = { viewModel.deleteRecord(record.id) }
                )
            }
        }
    }
}

@Composable
private fun MediaTypeFilterTabsRow(
    selectedFilter: MediaFilterTab,
    allCount: Int,
    videoCount: Int,
    photoCount: Int,
    onSelectFilter: (MediaFilterTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MediaFilterTab.entries.forEach { tab ->
            val selected = selectedFilter == tab
            val count = when (tab) {
                MediaFilterTab.ALL -> allCount
                MediaFilterTab.VIDEOS -> videoCount
                MediaFilterTab.PHOTOS -> photoCount
            }
            val icon = when (tab) {
                MediaFilterTab.ALL -> Icons.Rounded.Collections
                MediaFilterTab.VIDEOS -> Icons.Rounded.VideoLibrary
                MediaFilterTab.PHOTOS -> Icons.Rounded.PhotoLibrary
            }
            val testTag = when (tab) {
                MediaFilterTab.ALL -> "history_filter_all"
                MediaFilterTab.VIDEOS -> "history_filter_videos"
                MediaFilterTab.PHOTOS -> "history_filter_photos"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) {
                    null
                } else {
                    BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectFilter(tab) }
                    .testTag(testTag)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${tab.labelTr} ($count)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyStateCard(
    selectedFilter: MediaFilterTab,
    onNavigateToVideoStudio: () -> Unit,
    onNavigateToPhotoStudio: () -> Unit
) {
    val (title, subtitle, ctaLabel, onCta) = when (selectedFilter) {
        MediaFilterTab.VIDEOS -> listOf(
            "Henüz düzenlenmiş video yok",
            "Video Stüdyo'da bir video iyileştirdiğinizde veya oluşturduğunuzda hızlı erişim için burada listelenir.",
            "Video Stüdyo'ya Git",
            onNavigateToVideoStudio
        )
        MediaFilterTab.PHOTOS -> listOf(
            "Henüz düzenlenmiş fotoğraf yok",
            "Fotoğraf Stüdyosu'nda tek tuşla onardığınız veya oluşturduğunuz görseller burada arşivlenir.",
            "Fotoğraf Stüdyosu'na Git",
            onNavigateToPhotoStudio
        )
        MediaFilterTab.ALL -> listOf(
            "Henüz iyileştirme kaydı yok",
            "Video Stüdyo veya Fotoğraf Stüdyosu'ndan ilk restorasyonunuzu başlattığınızda sonuçlar burada arşivlenir.",
            "Video İyileştirmeye Başla",
            onNavigateToVideoStudio
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("history_empty_state"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    imageVector = when (selectedFilter) {
                        MediaFilterTab.VIDEOS -> Icons.Rounded.Movie
                        MediaFilterTab.PHOTOS -> Icons.Rounded.Image
                        MediaFilterTab.ALL -> Icons.Rounded.History
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp)
                )
            }
            Text(
                text = title as String,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle as String,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            @Suppress("UNCHECKED_CAST")
            ProminentPrimaryButton(
                onClick = onCta as () -> Unit,
                minHeight = 52.dp,
                modifier = Modifier.testTag("history_empty_cta_button")
            ) {
                Text(ctaLabel as String, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: EnhancementRecord,
    dateText: String,
    onOpenInStudio: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = record.mediaType == "VIDEO"
    val fileExists = remember(record.outputPath) { File(record.outputPath).exists() }

    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = record.outputPath, key2 = isVideo) {
        value = withContext(Dispatchers.IO) {
            val file = File(record.outputPath)
            if (!file.exists()) return@withContext null
            runCatching {
                if (isVideo) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?.let { MediaProcessingEngine.scaleDownBitmap(it, 240) }
                    } finally {
                        runCatching { retriever.release() }
                    }
                } else {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                        ?.let { MediaProcessingEngine.scaleDownBitmap(it, 240) }
                }
            }.getOrNull()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_record_${record.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Visual Thumbnail or Fallback Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isVideo) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = thumbnailBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = if (isVideo) "Video küçük resmi" else "Fotoğraf küçük resmi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isVideo) Icons.Rounded.Movie else Icons.Rounded.Image,
                            contentDescription = if (isVideo) "Video kaydı" else "Fotoğraf kaydı",
                            tint = if (isVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isVideo) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = if (isVideo) "VİDEO" else "FOTOĞRAF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isVideo) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.presetName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("history_delete_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Kaydı sil",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${record.sourceResolution} → ${record.targetResolution}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (record.aiSummary.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = record.aiSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (fileExists) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProminentSecondaryButton(
                        onClick = onOpenInStudio,
                        minHeight = 48.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_open_${record.id}")
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Stüdyoda Aç", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    ProminentSecondaryButton(
                        onClick = onSaveToGallery,
                        accentBorder = true,
                        minHeight = 48.dp,
                        modifier = Modifier.testTag("history_save_${record.id}")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Galeriye Kaydet", modifier = Modifier.size(20.dp))
                    }

                    ProminentPrimaryButton(
                        onClick = onShare,
                        minHeight = 48.dp,
                        modifier = Modifier.testTag("history_share_${record.id}")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Paylaş", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
