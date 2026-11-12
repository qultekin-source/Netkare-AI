package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AiState
import com.example.domain.model.VideoAspectRatio
import com.example.ui.adaptive.AdaptiveWindowState
import com.example.ui.components.ProminentPrimaryButton
import com.example.ui.components.ProminentSecondaryButton
import com.example.ui.components.ProminentSuccessButton
import com.example.ui.components.StudioHeroBanner
import com.example.ui.components.VideoPreviewPlayerCard
import com.example.ui.viewmodel.VideoGenUiState
import com.example.ui.viewmodel.VideoGenViewModel
import java.io.File

@Composable
fun VideoGenScreen(
    viewModel: VideoGenViewModel,
    adaptiveState: AdaptiveWindowState,
    onSendToUpscaler: (Uri) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topPad = contentPadding.calculateTopPadding() + 12.dp
    val bottomPad = contentPadding.calculateBottomPadding() + 24.dp

    if (adaptiveState.isDualPaneRecommended) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StudioHeroBanner(
                        title = "Metinden Video Üretimi",
                        subtitle = "Hayalinizdeki sahneyi yazın, gemini-omni-1.2-flash Interactions API ile 16:9 veya 9:16 video oluşturun.",
                        badgeText = "Gemini Omni 1.2 Flash • Interactions API"
                    )
                }
                item {
                    VideoGenComposerSection(uiState = uiState, viewModel = viewModel)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    VideoGenPreviewAndOutputSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSendToUpscaler = onSendToUpscaler,
                        showStandbyPlaceholder = true
                    )
                }
            }
        }
    } else {
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
                    title = "Metinden Video Üretimi",
                    subtitle = "Hayalinizdeki sahneyi yazın, gemini-omni-1.2-flash Interactions API ile 16:9 veya 9:16 video oluşturun.",
                    badgeText = "Gemini Omni 1.2 Flash • Interactions API"
                )
            }
            item {
                VideoGenComposerSection(uiState = uiState, viewModel = viewModel)
            }
            item {
                VideoGenPreviewAndOutputSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSendToUpscaler = onSendToUpscaler,
                    showStandbyPlaceholder = false
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoGenComposerSection(
    uiState: VideoGenUiState,
    viewModel: VideoGenViewModel
) {
    val isLoading = uiState.generationState is AiState.Loading
    val samplePrompts = listOf(
        "Gün batımında İstanbul Boğazı üzerinde süzülen sinematik drone çekimi, altın saat ışığı",
        "Yağmurlu neon sokakta yürüyen nostaljik 1980'ler klasik otomobili, 35mm film dokusu",
        "Karlı dağ zirvelerinin arasından doğan güneş, ultra akıcı hava çekimi"
    )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sahne İstemi (Prompt)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.lastInteractionId != null) {
                        ProminentSecondaryButton(
                            onClick = { viewModel.clearInteractionChain() },
                            minHeight = 48.dp,
                            modifier = Modifier.testTag("reset_video_interaction_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Yeni Oturum", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = viewModel::updatePrompt,
                    placeholder = {
                        Text("Oluşturmak istediğiniz videoyu detaylı tarif edin...")
                    },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_gen_prompt_input"),
                    shape = MaterialTheme.shapes.medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePrompts.forEachIndexed { idx, sample ->
                        FilterChip(
                            selected = uiState.prompt == sample,
                            onClick = { viewModel.updatePrompt(sample) },
                            label = {
                                Text(
                                    text = sample,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("video_gen_sample_prompt_$idx")
                        )
                    }
                }

                Text(
                    text = "En-Boy Oranı (Zorunlu Seçim)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VideoAspectRatio.entries.forEach { ratio ->
                        val selected = uiState.aspectRatio == ratio
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectAspectRatio(ratio) },
                            enabled = !isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (ratio == VideoAspectRatio.LANDSCAPE_16_9) {
                                        Icons.Rounded.Tv
                                    } else {
                                        Icons.Rounded.Smartphone
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(ratio.labelTr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("video_aspect_${ratio.apiValue.replace(":", "_")}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Çözünürlük",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("720p", "1080p").forEach { res ->
                                FilterChip(
                                    selected = uiState.resolution == res,
                                    onClick = { viewModel.selectResolution(res) },
                                    enabled = !isLoading,
                                    label = { Text(res) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("video_gen_res_$res")
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Süre",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("5s", "10s").forEach { dur ->
                                FilterChip(
                                    selected = uiState.duration == dur,
                                    onClick = { viewModel.selectDuration(dur) },
                                    enabled = !isLoading,
                                    label = { Text(dur) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("video_gen_dur_$dur")
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProminentPrimaryButton(
                onClick = { viewModel.generateVideo(refinePrevious = false) },
                enabled = uiState.prompt.isNotBlank() && !isLoading,
                minHeight = 58.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("generate_video_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Video Oluşturuluyor...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Video Oluştur (${uiState.aspectRatio.apiValue})", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (uiState.lastInteractionId != null && !isLoading) {
                ProminentSecondaryButton(
                    onClick = { viewModel.generateVideo(refinePrevious = true) },
                    enabled = uiState.prompt.isNotBlank(),
                    accentBorder = true,
                    minHeight = 58.dp,
                    modifier = Modifier.testTag("refine_video_interaction_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Revize Et", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun VideoGenPreviewAndOutputSection(
    uiState: VideoGenUiState,
    viewModel: VideoGenViewModel,
    onSendToUpscaler: (Uri) -> Unit,
    showStandbyPlaceholder: Boolean
) {
    val isLoading = uiState.generationState is AiState.Loading
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_gen_progress_card"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gemini Omni Video Sentezi",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.progressStatusText.ifBlank { "İşleniyor..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.cancelGeneration() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("cancel_video_gen_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "İptal", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        when (val state = uiState.generationState) {
            is AiState.Idle -> {
                if (showStandbyPlaceholder) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MovieCreation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Video Ön İzleme Monitörü",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sol panelden bir sahne tarifi girip en-boy oranını seçtiğinizde üretilen video burada oynatılır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            is AiState.Error -> {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_gen_error_banner")
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            is AiState.Success -> {
                val videoUri = Uri.fromFile(File(state.data.filePath))
                VideoPreviewPlayerCard(
                    originalUri = videoUri,
                    enhancedUri = null,
                    showAfter = false,
                    onToggleShowAfter = {},
                    sourceResolutionLabel = "${state.data.resolution} • ${state.data.aspectRatio.apiValue}",
                    targetResolutionLabel = state.data.resolution
                )

                ProminentPrimaryButton(
                    onClick = { onSendToUpscaler(videoUri) },
                    minHeight = 52.dp,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_generated_video_to_upscaler")
                ) {
                    Icon(Icons.Rounded.Hd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Video Stüdyo'da 4K Yükselt & İyileştir", style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProminentSuccessButton(
                        onClick = { viewModel.openExportOptions() },
                        minHeight = 52.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_generated_video_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kaydetme Seçenekleri", style = MaterialTheme.typography.labelLarge)
                    }

                    ProminentSecondaryButton(
                        onClick = { viewModel.shareGeneratedVideo() },
                        minHeight = 52.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_generated_video_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paylaş", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            else -> Unit
        }
    }
}
