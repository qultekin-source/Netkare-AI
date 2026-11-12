package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.BatchItemStatus
import com.example.domain.model.BatchMediaKind
import com.example.domain.model.BatchQueueItem
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.PhotoExportFormat
import com.example.domain.model.PhotoRestorationPreset
import com.example.domain.model.PhotoUpscaleOption
import com.example.domain.model.TargetResolution
import com.example.domain.model.VideoExportFormat
import com.example.ui.adaptive.AdaptiveWindowState
import com.example.ui.adaptive.FoldPosture
import com.example.ui.components.EnhancementLevelSelectorRow
import com.example.ui.components.ProminentPrimaryButton
import com.example.ui.components.ProminentSecondaryButton
import com.example.ui.components.ProminentSuccessButton
import com.example.ui.components.StudioHeroBanner
import com.example.ui.viewmodel.BatchStudioUiState
import com.example.ui.viewmodel.BatchStudioViewModel
import kotlin.math.roundToInt

@Composable
fun BatchStudioScreen(
    viewModel: BatchStudioViewModel,
    adaptiveState: AdaptiveWindowState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addMediaUris(uris, forcedKind = BatchMediaKind.PHOTO)
            }
        }
    )

    val multiVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addMediaUris(uris, forcedKind = BatchMediaKind.VIDEO)
            }
        }
    )

    val multiMixedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addMediaUris(uris, forcedKind = null)
            }
        }
    )

    val onPickPhotos: () -> Unit = {
        multiPhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val onPickVideos: () -> Unit = {
        multiVideoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
    val onPickMixed: () -> Unit = {
        multiMixedLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    val topPad = contentPadding.calculateTopPadding() + 12.dp
    val bottomPad = contentPadding.calculateBottomPadding() + 24.dp

    if (adaptiveState.posture == FoldPosture.TABLETOP) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = topPad, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BatchQueueOverviewCard(
                        uiState = uiState,
                        onStartBatch = viewModel::startBatchProcessing,
                        onStopBatch = viewModel::stopBatchProcessing,
                        onSaveAllCompleted = viewModel::saveAllCompletedToDevice,
                        onClearCompleted = viewModel::clearCompletedItems,
                        onClearAll = viewModel::clearAllItems
                    )
                }
                if (uiState.queue.isEmpty()) {
                    item { EmptyBatchQueueCard() }
                } else {
                    itemsIndexed(uiState.queue, key = { _, item -> item.id }) { index, item ->
                        BatchQueueItemCard(
                            item = item,
                            index = index,
                            totalCount = uiState.queue.size,
                            isBatchRunning = uiState.isBatchRunning,
                            onMoveUp = { viewModel.moveItemUp(item.id) },
                            onMoveDown = { viewModel.moveItemDown(item.id) },
                            onRemove = { viewModel.removeItem(item.id) },
                            onSave = { viewModel.saveCompletedItemToDevice(item.id) },
                            onShare = { viewModel.shareCompletedItem(item.id) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BatchMultiPickerCard(
                        isImporting = uiState.isImportingMedia,
                        isBatchRunning = uiState.isBatchRunning,
                        onPickPhotos = onPickPhotos,
                        onPickVideos = onPickVideos,
                        onPickMixed = onPickMixed,
                        onAddSampleBatch = viewModel::addSampleBatchQueue
                    )
                }
                item {
                    BatchSettingsCard(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    } else if (adaptiveState.isDualPaneRecommended) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Pane: Hero, Multi-select pickers & Batch settings
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StudioHeroBanner(
                        title = "Toplu İşlem Kuyruğu",
                        subtitle = "Aynı anda birden fazla fotoğraf ve videoyu sıraya alın, tek dokunuşla art arda iyileştirip dışa aktarın.",
                        badgeText = "Çoklu Kuyruk Motoru"
                    )
                }
                item {
                    BatchMultiPickerCard(
                        isImporting = uiState.isImportingMedia,
                        isBatchRunning = uiState.isBatchRunning,
                        onPickPhotos = onPickPhotos,
                        onPickVideos = onPickVideos,
                        onPickMixed = onPickMixed,
                        onAddSampleBatch = viewModel::addSampleBatchQueue
                    )
                }
                item {
                    BatchSettingsCard(uiState = uiState, viewModel = viewModel)
                }
            }

            // Right Pane: Overall Progress + Live Queue Items
            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    BatchQueueOverviewCard(
                        uiState = uiState,
                        onStartBatch = viewModel::startBatchProcessing,
                        onStopBatch = viewModel::stopBatchProcessing,
                        onSaveAllCompleted = viewModel::saveAllCompletedToDevice,
                        onClearCompleted = viewModel::clearCompletedItems,
                        onClearAll = viewModel::clearAllItems
                    )
                }
                if (uiState.queue.isEmpty()) {
                    item { EmptyBatchQueueCard() }
                } else {
                    itemsIndexed(uiState.queue, key = { _, item -> item.id }) { index, item ->
                        BatchQueueItemCard(
                            item = item,
                            index = index,
                            totalCount = uiState.queue.size,
                            isBatchRunning = uiState.isBatchRunning,
                            onMoveUp = { viewModel.moveItemUp(item.id) },
                            onMoveDown = { viewModel.moveItemDown(item.id) },
                            onRemove = { viewModel.removeItem(item.id) },
                            onSave = { viewModel.saveCompletedItemToDevice(item.id) },
                            onShare = { viewModel.shareCompletedItem(item.id) }
                        )
                    }
                }
            }
        }
    } else {
        // Compact Phone Layout
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topPad,
                bottom = bottomPad
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StudioHeroBanner(
                    title = "Toplu İşlem Kuyruğu",
                    subtitle = "Birden fazla fotoğraf ve videoyu aynı anda seçip sıraya alın, otomatik olarak arka arkaya iyileştirin.",
                    badgeText = "Çoklu Kuyruk Motoru"
                )
            }
            item {
                BatchMultiPickerCard(
                    isImporting = uiState.isImportingMedia,
                    isBatchRunning = uiState.isBatchRunning,
                    onPickPhotos = onPickPhotos,
                    onPickVideos = onPickVideos,
                    onPickMixed = onPickMixed,
                    onAddSampleBatch = viewModel::addSampleBatchQueue
                )
            }
            item {
                BatchQueueOverviewCard(
                    uiState = uiState,
                    onStartBatch = viewModel::startBatchProcessing,
                    onStopBatch = viewModel::stopBatchProcessing,
                    onSaveAllCompleted = viewModel::saveAllCompletedToDevice,
                    onClearCompleted = viewModel::clearCompletedItems,
                    onClearAll = viewModel::clearAllItems
                )
            }
            if (uiState.queue.isEmpty()) {
                item { EmptyBatchQueueCard() }
            } else {
                itemsIndexed(uiState.queue, key = { _, item -> item.id }) { index, item ->
                    BatchQueueItemCard(
                        item = item,
                        index = index,
                        totalCount = uiState.queue.size,
                        isBatchRunning = uiState.isBatchRunning,
                        onMoveUp = { viewModel.moveItemUp(item.id) },
                        onMoveDown = { viewModel.moveItemDown(item.id) },
                        onRemove = { viewModel.removeItem(item.id) },
                        onSave = { viewModel.saveCompletedItemToDevice(item.id) },
                        onShare = { viewModel.shareCompletedItem(item.id) }
                    )
                }
            }
            item {
                BatchSettingsCard(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun BatchMultiPickerCard(
    isImporting: Boolean,
    isBatchRunning: Boolean,
    onPickPhotos: () -> Unit,
    onPickVideos: () -> Unit,
    onPickMixed: () -> Unit,
    onAddSampleBatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. Çoklu Medya Seçimi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tek seferde en fazla 20 fotoğraf veya video seçip kuyruğa ekleyin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProminentPrimaryButton(
                    onClick = onPickPhotos,
                    enabled = !isImporting,
                    minHeight = 52.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_pick_photos_button")
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Çoklu Foto", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                ProminentPrimaryButton(
                    onClick = onPickVideos,
                    enabled = !isImporting,
                    minHeight = 52.dp,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_pick_videos_button")
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Çoklu Video", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProminentSecondaryButton(
                    onClick = onPickMixed,
                    enabled = !isImporting,
                    accentBorder = true,
                    minHeight = 50.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_pick_mixed_button")
                ) {
                    Text("Foto + Video Karışık", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                ProminentSecondaryButton(
                    onClick = onAddSampleBatch,
                    enabled = !isImporting && !isBatchRunning,
                    minHeight = 50.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_add_sample_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Örnek Kuyruk", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BatchQueueOverviewCard(
    uiState: BatchStudioUiState,
    onStartBatch: () -> Unit,
    onStopBatch: () -> Unit,
    onSaveAllCompleted: () -> Unit,
    onClearCompleted: () -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_queue_overview_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "İşlem Kuyruğu (${uiState.totalCount} Dosya)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.photoCount} Fotoğraf • ${uiState.videoCount} Video • ${uiState.completedCount} Tamamlandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (uiState.isBatchRunning) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (uiState.isBatchRunning) {
                            "İşleniyor %${(uiState.overallProgressFraction * 100).roundToInt()}"
                        } else if (uiState.totalCount > 0 && uiState.completedCount == uiState.totalCount) {
                            "Tümü Hazır"
                        } else {
                            "${uiState.queuedOrPendingCount} Bekliyor"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.isBatchRunning) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { uiState.overallProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (uiState.completedCount == uiState.totalCount) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    strokeCap = StrokeCap.Round
                )
            }

            if (uiState.isBatchRunning) {
                ProminentSecondaryButton(
                    onClick = onStopBatch,
                    minHeight = 54.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_stop_button")
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Toplu İşlemi Durdur", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                }
            } else {
                ProminentPrimaryButton(
                    onClick = onStartBatch,
                    enabled = uiState.queuedOrPendingCount > 0,
                    minHeight = 56.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_start_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.queuedOrPendingCount > 0) {
                            "Toplu İşlemi Başlat (${uiState.queuedOrPendingCount} Dosya)"
                        } else {
                            "Sırada Bekleyen Dosya Yok"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (uiState.completedCount > 0 || (uiState.totalCount > 0 && !uiState.isBatchRunning)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.completedCount > 0) {
                        ProminentSuccessButton(
                            onClick = onSaveAllCompleted,
                            enabled = !uiState.isSavingBatch,
                            minHeight = 48.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("batch_save_all_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isSavingBatch) "Kaydediliyor..." else "Tümünü Kaydet (${uiState.completedCount})",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!uiState.isBatchRunning && uiState.totalCount > 0) {
                        TextButton(
                            onClick = if (uiState.completedCount > 0 && uiState.completedCount < uiState.totalCount) {
                                onClearCompleted
                            } else {
                                onClearAll
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("batch_clear_button")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (uiState.completedCount > 0 && uiState.completedCount < uiState.totalCount) {
                                    "Bitenleri Sil"
                                } else {
                                    "Temizle"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBatchQueueCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Collections,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Kuyruk Şu An Boş",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Birden fazla fotoğraf veya video seçerek aynı anda sıraya alabilir; ayarladığınız profille hepsini art arda iyileştirebilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BatchQueueItemCard(
    item: BatchQueueItem,
    index: Int,
    totalCount: Int,
    isBatchRunning: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val borderColor = when (item.status) {
        BatchItemStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
        BatchItemStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        BatchItemStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_queue_item_${item.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(if (item.status == BatchItemStatus.PROCESSING) 2.dp else 1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnail != null) {
                        Image(
                            bitmap = item.thumbnail.asImageBitmap(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (item.mediaKind == BatchMediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (item.mediaKind == BatchMediaKind.VIDEO) "VİDEO" else "FOTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.mediaKind == BatchMediaKind.VIDEO) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${item.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(item.resolutionLabel)
                            if (item.outputResolutionLabel != null) {
                                append(" → ${item.outputResolutionLabel}")
                            }
                            append(" • ${item.fileSizeLabel}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (item.status) {
                            BatchItemStatus.COMPLETED -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(15.dp)
                            )
                            BatchItemStatus.FAILED -> Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                            BatchItemStatus.PROCESSING -> CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            else -> Unit
                        }
                        Text(
                            text = item.errorMessage ?: item.statusDetailTr,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (item.status) {
                                BatchItemStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                                BatchItemStatus.FAILED -> MaterialTheme.colorScheme.error
                                BatchItemStatus.PROCESSING -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!isBatchRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            if (index > 0) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "Yukarı taşı",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (index < totalCount - 1) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Aşağı taşı",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("batch_remove_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kuyruktan çıkar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (item.status == BatchItemStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = { (item.progressPercent.coerceIn(0, 100) / 100f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }

            if (item.status == BatchItemStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProminentSuccessButton(
                        onClick = onSave,
                        minHeight = 48.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_save_item_${item.id}")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kaydet", style = MaterialTheme.typography.labelLarge)
                    }
                    ProminentSecondaryButton(
                        onClick = onShare,
                        minHeight = 48.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_share_item_${item.id}")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paylaş", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchSettingsCard(
    uiState: BatchStudioUiState,
    viewModel: BatchStudioViewModel
) {
    val settings = uiState.settings
    val locked = uiState.isBatchRunning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "2. Ortak İyileştirme & Çıktı Ayarları",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            EnhancementLevelSelectorRow(
                selectedLevel = settings.enhancementLevel,
                onSelectLevel = viewModel::selectEnhancementLevel,
                enabled = !locked
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // Video batch target resolution + format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Video Hedef Çözünürlüğü & Formatı",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetResolution.entries.forEach { res ->
                        FilterChip(
                            selected = settings.videoResolution == res,
                            onClick = { viewModel.selectVideoResolution(res) },
                            enabled = !locked,
                            label = { Text(res.labelTr, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoExportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = settings.videoExportFormat == fmt,
                            onClick = { viewModel.selectVideoExportFormat(fmt) },
                            enabled = !locked,
                            label = { Text(fmt.labelTr, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // Photo batch preset + upscale + format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Fotoğraf Onarım Modu, Ölçek & Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhotoRestorationPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.photoPreset == preset,
                            onClick = { viewModel.selectPhotoPreset(preset) },
                            enabled = !locked,
                            label = { Text(preset.titleTr, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhotoUpscaleOption.entries.forEach { scale ->
                        FilterChip(
                            selected = settings.photoUpscale == scale,
                            onClick = { viewModel.selectPhotoUpscale(scale) },
                            enabled = !locked,
                            label = { Text(scale.shortBadge, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                    PhotoExportFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = settings.photoExportFormat == fmt,
                            onClick = { viewModel.selectPhotoExportFormat(fmt) },
                            enabled = !locked,
                            label = { Text(fmt.labelTr, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // Export Quality Tier
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Çıktı Kalitesi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportQualityTier.entries.forEach { tier ->
                        FilterChip(
                            selected = settings.exportQualityTier == tier,
                            onClick = { viewModel.selectExportQualityTier(tier) },
                            enabled = !locked,
                            label = { Text("${tier.labelTr} (${tier.badgeTr})", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tamamlananları Otomatik Cihaza Kaydet",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "İşlenen her dosya otomatik olarak Galeri/İndirilenler klasörüne aktarılır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.autoSaveToDevice,
                    onCheckedChange = viewModel::setAutoSaveToDevice,
                    enabled = !locked,
                    modifier = Modifier.testTag("batch_auto_save_switch")
                )
            }
        }
    }
}
