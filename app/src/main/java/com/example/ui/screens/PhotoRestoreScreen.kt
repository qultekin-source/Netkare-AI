package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AiState
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.ImageAspectRatioOption
import com.example.domain.model.PhotoRestorationPreset
import com.example.domain.model.PhotoUpscaleOption
import com.example.ui.adaptive.AdaptiveWindowState
import com.example.ui.adaptive.FoldPosture
import com.example.ui.components.AiDiagnosticCard
import com.example.ui.components.AiDiagnosticLoadingCard
import com.example.ui.components.BeforeAfterPhotoComparison
import com.example.ui.components.EnhancementLevelSelectorRow
import com.example.ui.components.ProminentPrimaryButton
import com.example.ui.components.ProminentSecondaryButton
import com.example.ui.components.ProminentSuccessButton
import com.example.ui.components.StudioHeroBanner
import com.example.ui.viewmodel.PhotoRestoreUiState
import com.example.ui.viewmodel.PhotoRestoreViewModel
import com.example.ui.viewmodel.PhotoStudioMode
import kotlin.math.roundToInt

@Composable
fun PhotoRestoreScreen(
    viewModel: PhotoRestoreViewModel,
    adaptiveState: AdaptiveWindowState,
    contentPadding: PaddingValues,
    onOpenBatchMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.onPhotoSelected(it) } }
    )

    val onPickPhoto = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
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
                    PhotoSelectionCard(
                        uiState = uiState,
                        onPickPhoto = onPickPhoto,
                        onLoadSample = viewModel::loadSampleVintagePhoto,
                        onClearSource = viewModel::clearSourcePhoto
                    )
                }
                if (uiState.studioMode == PhotoStudioMode.CREATE_EDIT) {
                    item {
                        CreateEditOutputPane(
                            uiState = uiState,
                            onUseGeneratedAsSource = viewModel::useGeneratedImageAsSource
                        )
                    }
                } else if (uiState.sourceBitmap != null) {
                    item {
                        PhotoRestorePreviewAndDiagnosticPane(
                            uiState = uiState,
                            onAnalyzePhoto = { viewModel.analyzePhotoWithAi(autoApply = true) },
                            onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
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
                    PhotoModeSwitcherRow(
                        selectedMode = uiState.studioMode,
                        onSelectMode = viewModel::setStudioMode
                    )
                }
                if (uiState.studioMode == PhotoStudioMode.CREATE_EDIT) {
                    item {
                        CreateEditComposerPane(
                            uiState = uiState,
                            onPromptChange = viewModel::updateCreateEditPrompt,
                            onSelectRatio = viewModel::selectAspectRatio,
                            onGenerateNew = viewModel::generateImageFromPrompt,
                            onEditSource = viewModel::editSourceImageWithPrompt
                        )
                    }
                } else {
                    item {
                        PhotoRestoreControlsPane(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
                if (uiState.activeAfterBitmap != null) {
                    item {
                        PhotoExportActionRow(
                            onSaveToGallery = viewModel::openExportOptions,
                            onShare = viewModel::shareRestoredPhoto
                        )
                    }
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
            // Left Pane: Hero, Mode Switcher, Source Picker, Before/After Preview & Diagnostic
            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StudioHeroBanner(
                        title = "Fotoğraf & Görsel Stüdyosu",
                        subtitle = "Tek tuşla eski fotoğrafları onarın veya metin istemiyle sıfırdan görsel oluşturup düzenleyin.",
                        badgeText = "Nano Banana 2.1 Görsel Motoru"
                    )
                }
                item {
                    PhotoModeSwitcherRow(
                        selectedMode = uiState.studioMode,
                        onSelectMode = viewModel::setStudioMode
                    )
                }
                item {
                    PhotoSelectionCard(
                        uiState = uiState,
                        onPickPhoto = onPickPhoto,
                        onLoadSample = viewModel::loadSampleVintagePhoto,
                        onClearSource = viewModel::clearSourcePhoto
                    )
                }

                if (uiState.studioMode == PhotoStudioMode.RESTORE) {
                    item {
                        ProminentSecondaryButton(
                            onClick = onOpenBatchMode,
                            accentBorder = true,
                            minHeight = 48.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("photo_open_batch_button")
                        ) {
                            Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Çoklu Fotoğraf Seç (Toplu İşlem Kuyruğu)", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                if (uiState.studioMode == PhotoStudioMode.CREATE_EDIT) {
                    item {
                        CreateEditOutputPane(
                            uiState = uiState,
                            onUseGeneratedAsSource = viewModel::useGeneratedImageAsSource
                        )
                    }
                } else if (uiState.sourceBitmap != null) {
                    item {
                        PhotoRestorePreviewAndDiagnosticPane(
                            uiState = uiState,
                            onAnalyzePhoto = { viewModel.analyzePhotoWithAi(autoApply = true) },
                            onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
                        )
                    }
                }
            }

            // Right Pane: Settings, One-Tap Action / Prompt Composer, Manual Sliders & Export
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.studioMode == PhotoStudioMode.CREATE_EDIT) {
                    item {
                        CreateEditComposerPane(
                            uiState = uiState,
                            onPromptChange = viewModel::updateCreateEditPrompt,
                            onSelectRatio = viewModel::selectAspectRatio,
                            onGenerateNew = viewModel::generateImageFromPrompt,
                            onEditSource = viewModel::editSourceImageWithPrompt
                        )
                    }
                } else {
                    item {
                        PhotoRestoreControlsPane(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }

                if (uiState.activeAfterBitmap != null) {
                    item {
                        PhotoExportActionRow(
                            onSaveToGallery = viewModel::openExportOptions,
                            onShare = viewModel::shareRestoredPhoto
                        )
                    }
                }
            }
        }
    } else {
        // Compact Phone / Flat Narrow Layout
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
                    title = "Fotoğraf & Görsel Stüdyosu",
                    subtitle = "Tek tuşla eski fotoğrafları onarın veya metin istemiyle sıfırdan görsel oluşturup düzenleyin.",
                    badgeText = "Nano Banana 2.1 Görsel Motoru"
                )
            }
            item {
                PhotoModeSwitcherRow(
                    selectedMode = uiState.studioMode,
                    onSelectMode = viewModel::setStudioMode
                )
            }
            item {
                PhotoSelectionCard(
                    uiState = uiState,
                    onPickPhoto = onPickPhoto,
                    onLoadSample = viewModel::loadSampleVintagePhoto,
                    onClearSource = viewModel::clearSourcePhoto
                )
            }

            if (uiState.studioMode == PhotoStudioMode.RESTORE) {
                item {
                    ProminentSecondaryButton(
                        onClick = onOpenBatchMode,
                        accentBorder = true,
                        minHeight = 48.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_open_batch_button")
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Çoklu Fotoğraf Seç (Toplu İşlem Kuyruğu)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (uiState.studioMode == PhotoStudioMode.CREATE_EDIT) {
                item {
                    CreateEditComposerPane(
                        uiState = uiState,
                        onPromptChange = viewModel::updateCreateEditPrompt,
                        onSelectRatio = viewModel::selectAspectRatio,
                        onGenerateNew = viewModel::generateImageFromPrompt,
                        onEditSource = viewModel::editSourceImageWithPrompt
                    )
                }
                item {
                    CreateEditOutputPane(
                        uiState = uiState,
                        onUseGeneratedAsSource = viewModel::useGeneratedImageAsSource
                    )
                }
            } else {
                if (uiState.sourceBitmap != null) {
                    item {
                        PhotoRestorePreviewAndDiagnosticPane(
                            uiState = uiState,
                            onAnalyzePhoto = { viewModel.analyzePhotoWithAi(autoApply = true) },
                            onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
                        )
                    }
                }
                item {
                    PhotoRestoreControlsPane(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            if (uiState.activeAfterBitmap != null) {
                item {
                    PhotoExportActionRow(
                        onSaveToGallery = viewModel::openExportOptions,
                        onShare = viewModel::shareRestoredPhoto
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoModeSwitcherRow(
    selectedMode: PhotoStudioMode,
    onSelectMode: (PhotoStudioMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PhotoStudioMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (selected) null else BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("photo_mode_${mode.name.lowercase()}")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = mode.labelTr,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSelectionCard(
    uiState: PhotoRestoreUiState,
    onPickPhoto: () -> Unit,
    onLoadSample: () -> Unit,
    onClearSource: () -> Unit
) {
    val isBusy = uiState.aiRestoreState is AiState.Loading || uiState.aiCreateEditState is AiState.Loading
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.studioMode == PhotoStudioMode.RESTORE) {
                            "Fotoğraf Seçimi"
                        } else {
                            "Kaynak Fotoğraf (Düzenleme İçin İsteğe Bağlı)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.sourceBitmap?.let {
                            "${uiState.sourceLabel} • ${it.width}×${it.height}"
                        } ?: "Galeriden fotoğraf seçin veya örnek kare yükleyin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (uiState.sourceBitmap != null) {
                    IconButton(
                        onClick = onClearSource,
                        modifier = Modifier.testTag("clear_source_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Fotoğrafı temizle",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProminentPrimaryButton(
                    onClick = onPickPhoto,
                    enabled = !isBusy,
                    minHeight = 52.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_photo_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fotoğraf Seç", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                ProminentSecondaryButton(
                    onClick = onLoadSample,
                    enabled = !isBusy,
                    accentBorder = true,
                    minHeight = 52.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_sample_photo_button")
                ) {
                    Text("Örnek Eski Kare", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PhotoRestorePreviewAndDiagnosticPane(
    uiState: PhotoRestoreUiState,
    onAnalyzePhoto: () -> Unit,
    onApplyRecommendations: () -> Unit
) {
    val source = uiState.sourceBitmap ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BeforeAfterPhotoComparison(
            beforeBitmap = source,
            afterBitmap = uiState.activeAfterBitmap,
            isProcessing = uiState.aiRestoreState is AiState.Loading
        )

        if (uiState.activeAfterBitmap != null) {
            Text(
                text = "İpucu: Öncesi ve sonrasını karşılaştırmak için görsel üzerindeki çizgiyi sağa veya sola kaydırın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (val diagState = uiState.aiDiagnosticState) {
            is AiState.Loading -> {
                AiDiagnosticLoadingCard(mediaTypeLabel = "fotoğraf")
            }
            is AiState.Success -> {
                val diag = diagState.data
                val presetLabel = PhotoRestorationPreset.entries
                    .firstOrNull { it.name == diag.recommendedPhotoPreset }?.titleTr
                    ?: "Tek Tuş Süper Netlik"
                val levelLabel = EnhancementLevel.entries
                    .firstOrNull { it.name == diag.recommendedLevel }?.labelTr
                    ?: "Orta"
                val upscaleLabel = when (diag.recommendedResolution) {
                    "UHD_4K" -> PhotoUpscaleOption.HD_TO_4K.shortBadge
                    "QHD_1440P" -> PhotoUpscaleOption.HD_TO_2K.shortBadge
                    else -> PhotoUpscaleOption.SD_TO_HD.shortBadge
                }
                val brightnessDelta = diag.suggestedBrightness.roundToInt()
                val brightnessLabel = if (brightnessDelta >= 0) "+$brightnessDelta" else "$brightnessDelta"

                AiDiagnosticCard(
                    diagnostic = diag,
                    isApplied = uiState.diagnosticApplied,
                    recommendedSummaryTr = "$presetLabel • $levelLabel • $upscaleLabel • Parlaklık $brightnessLabel",
                    onApplyRecommendations = onApplyRecommendations
                )
            }
            is AiState.Error -> Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Analiz Hatası: ${diagState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp)
                )
            }
            is AiState.Idle -> Unit
        }

        ProminentSecondaryButton(
            onClick = onAnalyzePhoto,
            enabled = uiState.aiDiagnosticState !is AiState.Loading && uiState.aiRestoreState !is AiState.Loading,
            accentBorder = true,
            minHeight = 52.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("analyze_photo_ai_button")
        ) {
            if (uiState.aiDiagnosticState is AiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(10.dp))
                Text("Fotoğraf İnceleniyor...", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.aiDiagnosticState is AiState.Success) {
                        "Yeniden Analiz Et & Otomatik Uygula"
                    } else {
                        "AI Fotoğraf Hasar & Işık Analizi"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoRestoreControlsPane(
    uiState: PhotoRestoreUiState,
    viewModel: PhotoRestoreViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Onarım Ayarları & Ölçeklendirme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                EnhancementLevelSelectorRow(
                    selectedLevel = uiState.enhancementLevel,
                    onSelectLevel = viewModel::selectEnhancementLevel,
                    enabled = uiState.aiRestoreState !is AiState.Loading
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Çözünürlük Ölçeklendirme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoUpscaleOption.entries.forEach { option ->
                            val selected = uiState.upscaleOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectUpscaleOption(option) },
                                enabled = uiState.aiRestoreState !is AiState.Loading,
                                label = { Text(option.routeTr, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = uiState.aiRestoreState !is AiState.Loading,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    borderWidth = 1.2.dp
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("photo_scale_${option.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Onarım Modu",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoRestorationPreset.entries.forEach { preset ->
                            val selected = uiState.selectedPreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectPreset(preset) },
                                enabled = uiState.aiRestoreState !is AiState.Loading,
                                label = { Text(preset.titleTr, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = uiState.aiRestoreState !is AiState.Loading,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    borderWidth = 1.2.dp
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("photo_preset_${preset.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = uiState.selectedPreset.subtitleTr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ProminentPrimaryButton(
            onClick = { viewModel.restoreOneTapWithAi() },
            enabled = uiState.sourceBitmap != null && uiState.aiRestoreState !is AiState.Loading,
            minHeight = 58.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("one_tap_enhance_button")
        ) {
            if (uiState.aiRestoreState is AiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(10.dp))
                Text("Yapay Zeka Onarıyor...", style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tek Tuşla AI İyileştir", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (uiState.aiRestoreState is AiState.Error) {
            val errorMsg = uiState.aiRestoreState.message
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photo_restore_error_banner")
            ) {
                Text(
                    text = "AI Onarım Hatası: $errorMsg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (uiState.sourceBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { viewModel.toggleManualMode(!uiState.manualModeEnabled) }
                            .testTag("toggle_manual_photo_editor"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Manuel Keskinlik & Renk Düzenleme",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (uiState.manualModeEnabled) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (uiState.manualModeEnabled) "Daralt" else "Genişlet"
                        )
                    }

                    AnimatedVisibility(visible = uiState.manualModeEnabled) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SettingSliderRow(
                                label = "Keskinleştirme (Bulanıklık Giderme)",
                                valueText = "%${(uiState.manualSettings.sharpness * 100).roundToInt()}",
                                value = uiState.manualSettings.sharpness,
                                range = 0f..1f,
                                onValueChange = { v -> viewModel.updateManualSettings { it.copy(sharpness = v) } },
                                testTag = "manual_sharpness_slider"
                            )
                            SettingSliderRow(
                                label = "Kontrast",
                                valueText = "${(uiState.manualSettings.contrast * 100).roundToInt()}%",
                                value = uiState.manualSettings.contrast,
                                range = 0.7f..1.6f,
                                onValueChange = { v -> viewModel.updateManualSettings { it.copy(contrast = v) } },
                                testTag = "manual_contrast_slider"
                            )
                            SettingSliderRow(
                                label = "Parlaklık",
                                valueText = "${uiState.manualSettings.brightness.roundToInt()}",
                                value = uiState.manualSettings.brightness,
                                range = -35f..45f,
                                onValueChange = { v -> viewModel.updateManualSettings { it.copy(brightness = v) } },
                                testTag = "manual_brightness_slider"
                            )
                            SettingSliderRow(
                                label = "Renk Doygunluğu",
                                valueText = "${(uiState.manualSettings.saturation * 100).roundToInt()}%",
                                value = uiState.manualSettings.saturation,
                                range = 0.4f..1.7f,
                                onValueChange = { v -> viewModel.updateManualSettings { it.copy(saturation = v) } },
                                testTag = "manual_saturation_slider"
                            )
                            SettingSliderRow(
                                label = "Sıcaklık Dengesi",
                                valueText = "${(uiState.manualSettings.warmth * 100).roundToInt()}",
                                value = uiState.manualSettings.warmth,
                                range = -0.5f..0.5f,
                                onValueChange = { v -> viewModel.updateManualSettings { it.copy(warmth = v) } },
                                testTag = "manual_warmth_slider"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEditComposerPane(
    uiState: PhotoRestoreUiState,
    onPromptChange: (String) -> Unit,
    onSelectRatio: (ImageAspectRatioOption) -> Unit,
    onGenerateNew: () -> Unit,
    onEditSource: () -> Unit
) {
    val isLoading = uiState.aiCreateEditState is AiState.Loading
    val quickPrompts = listOf(
        "Gün batımında Boğaz kıyısında sinematik 4K İstanbul manzarası",
        "Arka planı dramatik altın saat ışığına dönüştür, renkleri canlandır",
        "Minimalist koyu stüdyoda parlayan retro sinema kamerası illüstrasyonu"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Görsel Oluşturma & Düzenleme İstemi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = uiState.createEditPrompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text("Örn: 'Eski fotoğrafın gökyüzünü temizle ve sinematik netlik ekle' veya yeni bir sahne tarif edin...")
                },
                minLines = 3,
                maxLines = 5,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_create_edit_prompt_input"),
                shape = MaterialTheme.shapes.medium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEachIndexed { idx, sample ->
                    FilterChip(
                        selected = uiState.createEditPrompt == sample,
                        onClick = { onPromptChange(sample) },
                        label = {
                            Text(
                                text = sample,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("image_prompt_chip_$idx")
                    )
                }
            }

            Text(
                text = "En-Boy Oranı",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageAspectRatioOption.entries.forEach { ratio ->
                    FilterChip(
                        selected = uiState.selectedAspectRatio == ratio,
                        onClick = { onSelectRatio(ratio) },
                        enabled = !isLoading,
                        label = { Text(ratio.labelTr) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("image_ratio_${ratio.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProminentPrimaryButton(
                    onClick = onGenerateNew,
                    enabled = uiState.createEditPrompt.isNotBlank() && !isLoading,
                    minHeight = 54.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_image_prompt_button")
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Metinden Oluştur", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                ProminentSecondaryButton(
                    onClick = onEditSource,
                    enabled = uiState.sourceBitmap != null && uiState.createEditPrompt.isNotBlank() && !isLoading,
                    accentBorder = true,
                    minHeight = 54.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_image_prompt_button")
                ) {
                    Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Seçiliyi Düzenle", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CreateEditOutputPane(
    uiState: PhotoRestoreUiState,
    onUseGeneratedAsSource: () -> Unit
) {
    val isLoading = uiState.aiCreateEditState is AiState.Loading
    val generatedBitmap = (uiState.aiCreateEditState as? AiState.Success)?.data

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Nano Banana 2.1 görseli işliyor (15–45 sn)...",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (uiState.aiCreateEditState is AiState.Error) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_create_edit_error")
            ) {
                Text(
                    text = "Görsel İşlem Hatası: ${uiState.aiCreateEditState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (generatedBitmap != null) {
            if (uiState.sourceBitmap != null) {
                BeforeAfterPhotoComparison(
                    beforeBitmap = uiState.sourceBitmap,
                    afterBitmap = generatedBitmap,
                    isProcessing = false
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    val aspect = (generatedBitmap.width.toFloat() / generatedBitmap.height.coerceAtLeast(1).toFloat())
                        .coerceIn(0.6f, 1.9f)
                    Image(
                        bitmap = generatedBitmap.asImageBitmap(),
                        contentDescription = "Nano Banana 2.1 ile oluşturulan görsel",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                    )
                }
            }

            ProminentSecondaryButton(
                onClick = onUseGeneratedAsSource,
                accentBorder = true,
                minHeight = 52.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("use_generated_as_source_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bu Görseli Üstüne Yeni Düzenleme Yap", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PhotoExportActionRow(
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProminentSuccessButton(
            onClick = onSaveToGallery,
            minHeight = 52.dp,
            modifier = Modifier
                .weight(1f)
                .testTag("save_photo_gallery_button")
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Kaydetme Seçenekleri", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        ProminentSecondaryButton(
            onClick = onShare,
            minHeight = 52.dp,
            modifier = Modifier
                .weight(1f)
                .testTag("share_photo_button")
        ) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Paylaş", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
