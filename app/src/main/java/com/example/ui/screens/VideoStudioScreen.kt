package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AiState
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.TargetResolution
import com.example.domain.model.VideoEnhancementPreset
import com.example.domain.model.VideoProcessingStage
import com.example.ui.adaptive.AdaptiveWindowState
import com.example.ui.adaptive.FoldPosture
import com.example.ui.components.AiDiagnosticCard
import com.example.ui.components.AiDiagnosticLoadingCard
import com.example.ui.components.EnhancementProgressCard
import com.example.ui.components.ProminentPrimaryButton
import com.example.ui.components.ProminentSecondaryButton
import com.example.ui.components.ProminentSuccessButton
import com.example.ui.components.StudioHeroBanner
import com.example.ui.components.VideoEnhancementSettingsPanel
import com.example.ui.components.VideoSelectionAndPreviewCard
import com.example.ui.viewmodel.VideoStudioUiState
import com.example.ui.viewmodel.VideoStudioViewModel
import java.io.File
import kotlin.math.roundToInt

@Composable
fun VideoStudioScreen(
    viewModel: VideoStudioViewModel,
    adaptiveState: AdaptiveWindowState,
    contentPadding: PaddingValues,
    onOpenBatchMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdvancedSliders by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.onVideoSelected(it) } }
    )

    val documentVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.onVideoSelected(it) } }
    )

    val enhancedUri = uiState.processingProgress.outputFilePath?.let { path ->
        val file = File(path)
        if (file.exists()) Uri.fromFile(file) else null
    }

    val onPickFromGallery: () -> Unit = {
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
    val onPickFromFiles: () -> Unit = {
        documentVideoPickerLauncher.launch(arrayOf("video/*"))
    }

    val topPad = contentPadding.calculateTopPadding() + 12.dp
    val bottomPad = contentPadding.calculateBottomPadding() + 24.dp

    if (adaptiveState.posture == FoldPosture.TABLETOP) {
        // Foldable Tabletop Posture: Video Preview on top half above the hinge, Controls below
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
                    VideoSelectionAndPreviewCard(
                        selectedVideo = uiState.selectedVideo,
                        enhancedUri = enhancedUri,
                        showAfterPreview = uiState.showAfterPreview,
                        targetResolutionLabel = uiState.qualitySettings.targetResolution.labelTr,
                        isLoadingVideo = uiState.isLoadingVideo,
                        isCreatingSample = uiState.isCreatingSample,
                        isProcessing = uiState.processingProgress.isRunning,
                        onPickFromGallery = onPickFromGallery,
                        onPickFromFiles = onPickFromFiles,
                        onCreateSample = viewModel::createAndLoadSampleVideo,
                        onClearVideo = viewModel::clearSelectedVideo,
                        onToggleShowAfter = viewModel::toggleBeforeAfterPreview
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.selectedVideo != null) {
                    item {
                        VideoAiDiagnosticSection(
                            uiState = uiState,
                            onAnalyze = { viewModel.analyzeVideoWithAi(autoApply = true) },
                            onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
                        )
                    }
                }
                item {
                    VideoSettingsAndActionsContent(
                        uiState = uiState,
                        showAdvancedSliders = showAdvancedSliders,
                        onToggleAdvancedSliders = { showAdvancedSliders = !showAdvancedSliders },
                        viewModel = viewModel
                    )
                }
            }
        }
    } else if (adaptiveState.isDualPaneRecommended) {
        // Tablet / Unfolded Foldable Dual-Pane Layout
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StudioHeroBanner(
                        title = "Video İyileştirme Stüdyosu",
                        subtitle = "Eski, bulanık ve düşük çözünürlüklü videoları yapay zeka ile netleştirip 4K'ya yükseltin.",
                        badgeText = "Gemini + Media3 Süper Çözünürlük"
                    )
                }

                item {
                    VideoSelectionAndPreviewCard(
                        selectedVideo = uiState.selectedVideo,
                        enhancedUri = enhancedUri,
                        showAfterPreview = uiState.showAfterPreview,
                        targetResolutionLabel = uiState.qualitySettings.targetResolution.labelTr,
                        isLoadingVideo = uiState.isLoadingVideo,
                        isCreatingSample = uiState.isCreatingSample,
                        isProcessing = uiState.processingProgress.isRunning,
                        onPickFromGallery = onPickFromGallery,
                        onPickFromFiles = onPickFromFiles,
                        onCreateSample = viewModel::createAndLoadSampleVideo,
                        onClearVideo = viewModel::clearSelectedVideo,
                        onToggleShowAfter = viewModel::toggleBeforeAfterPreview
                    )
                }

                item {
                    ProminentSecondaryButton(
                        onClick = onOpenBatchMode,
                        accentBorder = true,
                        minHeight = 48.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_open_batch_button")
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Birden Fazla Video Seç (Toplu İşlem Kuyruğu)", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (uiState.selectedVideo != null) {
                    item {
                        VideoAiDiagnosticSection(
                            uiState = uiState,
                            onAnalyze = { viewModel.analyzeVideoWithAi(autoApply = true) },
                            onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    VideoSettingsAndActionsContent(
                        uiState = uiState,
                        showAdvancedSliders = showAdvancedSliders,
                        onToggleAdvancedSliders = { showAdvancedSliders = !showAdvancedSliders },
                        viewModel = viewModel
                    )
                }
            }
        }
    } else {
        // Compact / Standard Single-Column Layout
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
                    title = "Video İyileştirme Stüdyosu",
                    subtitle = "Eski, bulanık ve düşük çözünürlüklü videoları yapay zeka ile netleştirip 4K'ya yükseltin.",
                    badgeText = "Gemini + Media3 Süper Çözünürlük"
                )
            }

            item {
                VideoSelectionAndPreviewCard(
                    selectedVideo = uiState.selectedVideo,
                    enhancedUri = enhancedUri,
                    showAfterPreview = uiState.showAfterPreview,
                    targetResolutionLabel = uiState.qualitySettings.targetResolution.labelTr,
                    isLoadingVideo = uiState.isLoadingVideo,
                    isCreatingSample = uiState.isCreatingSample,
                    isProcessing = uiState.processingProgress.isRunning,
                    onPickFromGallery = onPickFromGallery,
                    onPickFromFiles = onPickFromFiles,
                    onCreateSample = viewModel::createAndLoadSampleVideo,
                    onClearVideo = viewModel::clearSelectedVideo,
                    onToggleShowAfter = viewModel::toggleBeforeAfterPreview
                )
            }

            item {
                ProminentSecondaryButton(
                    onClick = onOpenBatchMode,
                    accentBorder = true,
                    minHeight = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_open_batch_button")
                ) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Birden Fazla Video Seç (Toplu İşlem Kuyruğu)", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (uiState.selectedVideo != null) {
                item {
                    VideoAiDiagnosticSection(
                        uiState = uiState,
                        onAnalyze = { viewModel.analyzeVideoWithAi(autoApply = true) },
                        onApplyRecommendations = viewModel::applyAiDiagnosticRecommendations
                    )
                }
            }

            item {
                VideoSettingsAndActionsContent(
                    uiState = uiState,
                    showAdvancedSliders = showAdvancedSliders,
                    onToggleAdvancedSliders = { showAdvancedSliders = !showAdvancedSliders },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun VideoAiDiagnosticSection(
    uiState: VideoStudioUiState,
    onAnalyze: () -> Unit,
    onApplyRecommendations: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val diagState = uiState.aiDiagnosticState) {
            is AiState.Loading -> {
                AiDiagnosticLoadingCard(mediaTypeLabel = "video")
            }
            is AiState.Success -> {
                val diag = diagState.data
                val presetLabel = VideoEnhancementPreset.entries
                    .firstOrNull { it.name == diag.recommendedPreset }?.titleTr
                    ?: "Eski Video Onarımı"
                val levelLabel = EnhancementLevel.entries
                    .firstOrNull { it.name == diag.recommendedLevel }?.labelTr
                    ?: "Orta"
                val resLabel = TargetResolution.entries
                    .firstOrNull { it.name == diag.recommendedResolution }?.labelTr
                    ?: "1080p Full HD"
                val brightnessDelta = diag.suggestedBrightness.roundToInt()
                val brightnessLabel = if (brightnessDelta >= 0) "+$brightnessDelta" else "$brightnessDelta"

                AiDiagnosticCard(
                    diagnostic = diag,
                    isApplied = uiState.diagnosticApplied,
                    recommendedSummaryTr = "$presetLabel • $levelLabel • $resLabel • Parlaklık $brightnessLabel",
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
                    text = "AI Analiz Hatası: ${diagState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp)
                )
            }
            is AiState.Idle -> Unit
        }

        ProminentSecondaryButton(
            onClick = onAnalyze,
            enabled = uiState.aiDiagnosticState !is AiState.Loading && !uiState.processingProgress.isRunning,
            accentBorder = true,
            minHeight = 52.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("analyze_video_ai_button")
        ) {
            if (uiState.aiDiagnosticState is AiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(10.dp))
                Text("Gemini Video Karelerini İnceliyor...", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (uiState.aiDiagnosticState is AiState.Success) {
                        "Yeniden Analiz Et & Otomatik Uygula"
                    } else {
                        "AI Akıllı Video Analizi & Otomatik Ayar"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun VideoSettingsAndActionsContent(
    uiState: VideoStudioUiState,
    showAdvancedSliders: Boolean,
    onToggleAdvancedSliders: () -> Unit,
    viewModel: VideoStudioViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VideoEnhancementSettingsPanel(
            qualitySettings = uiState.qualitySettings,
            selectedVideo = uiState.selectedVideo,
            showAdvancedSliders = showAdvancedSliders,
            isProcessing = uiState.processingProgress.isRunning,
            onSelectLevel = viewModel::selectEnhancementLevel,
            onSelectResolution = viewModel::selectResolution,
            onSelectPreset = viewModel::selectPreset,
            onToggleAdvancedSliders = onToggleAdvancedSliders,
            onSharpnessChange = viewModel::updateSharpness,
            onDenoiseChange = viewModel::updateDenoise,
            onContrastChange = viewModel::updateContrast,
            onSaturationChange = viewModel::updateSaturation,
            onBrightnessChange = viewModel::updateBrightness
        )

        ProminentPrimaryButton(
            onClick = { viewModel.startEnhancement() },
            enabled = uiState.selectedVideo != null && !uiState.processingProgress.isRunning,
            minHeight = 58.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("start_enhancement_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (uiState.processingProgress.isRunning) {
                    "İyileştirme Sürüyor..."
                } else {
                    "İyileştirmeyi Başlat (${uiState.qualitySettings.enhancementLevel.labelTr} • ${uiState.qualitySettings.targetResolution.shortRouteTr})"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (uiState.processingProgress.stage != VideoProcessingStage.IDLE) {
            EnhancementProgressCard(
                progress = uiState.processingProgress,
                onCancel = { viewModel.cancelEnhancement() }
            )
        }

        if (uiState.processingProgress.stage == VideoProcessingStage.COMPLETED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProminentSuccessButton(
                    onClick = { viewModel.openExportOptions() },
                    minHeight = 52.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_video_gallery_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kaydetme Seçenekleri", style = MaterialTheme.typography.labelLarge)
                }

                ProminentSecondaryButton(
                    onClick = { viewModel.shareEnhancedVideo() },
                    minHeight = 52.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_video_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Paylaş", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SettingSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
