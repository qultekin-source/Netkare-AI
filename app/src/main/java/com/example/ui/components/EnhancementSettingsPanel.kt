package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.TargetResolution
import com.example.domain.model.VideoEnhancementPreset
import com.example.domain.model.VideoMetadataInfo
import com.example.domain.model.VideoQualitySettings
import com.example.ui.screens.SettingSliderRow
import kotlin.math.roundToInt

@Composable
fun EnhancementLevelSelectorRow(
    selectedLevel: EnhancementLevel,
    onSelectLevel: (EnhancementLevel) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "İyileştirme Seviyesi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "${selectedLevel.labelTr} • ${selectedLevel.strengthPercentLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnhancementLevel.entries.forEach { level ->
                val isSelected = selectedLevel == level
                val activeColor = when (level) {
                    EnhancementLevel.LOW -> MaterialTheme.colorScheme.secondary
                    EnhancementLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                    EnhancementLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) {
                        activeColor.copy(alpha = 0.24f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.2.dp,
                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) { onSelectLevel(level) }
                        .testTag("enhancement_level_${level.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = level.labelTr,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (bar in 1..3) {
                                val lit = bar <= level.tierCount
                                Box(
                                    modifier = Modifier
                                        .width(15.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                lit && isSelected -> activeColor
                                                lit -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = selectedLevel.subtitleTr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoEnhancementSettingsPanel(
    qualitySettings: VideoQualitySettings,
    selectedVideo: VideoMetadataInfo?,
    showAdvancedSliders: Boolean,
    isProcessing: Boolean,
    onSelectLevel: (EnhancementLevel) -> Unit,
    onSelectResolution: (TargetResolution) -> Unit,
    onSelectPreset: (VideoEnhancementPreset) -> Unit,
    onToggleAdvancedSliders: () -> Unit,
    onSharpnessChange: (Float) -> Unit,
    onDenoiseChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_enhancement_settings_panel"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. Ayarlar & Ölçeklendirme Paneli",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "İyileştirme yoğunluğunu ve hedef çözünürlük yükseltmesini seçin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${qualitySettings.enhancementLevel.labelTr} • ${qualitySettings.targetResolution.badge}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // A) Enhancement Level Selector (Düşük / Orta / Yüksek)
            EnhancementLevelSelectorRow(
                selectedLevel = qualitySettings.enhancementLevel,
                onSelectLevel = onSelectLevel,
                enabled = !isProcessing
            )

            // B) Resolution Upscaling Options (SD'den HD'ye ... HD'den 4K'ya)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Çözünürlük Ölçeklendirme Seçenekleri",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${qualitySettings.targetResolution.bitrateMbps} Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Live input -> output upscale summary pill when a video is loaded
                if (selectedVideo != null && selectedVideo.height > 0) {
                    val scaleFactor = qualitySettings.targetResolution.heightPx.toFloat() /
                        selectedVideo.height.toFloat()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ölçek Yolu: ${selectedVideo.resolutionLabel} → ${qualitySettings.targetResolution.labelTr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = String.format("%.1fx", scaleFactor),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 2x2 Grid of Upscale Route Cards
                val rows = TargetResolution.entries.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { res ->
                                val selected = qualitySettings.targetResolution == res
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.2.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 72.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(enabled = !isProcessing) { onSelectResolution(res) }
                                        .testTag("resolution_chip_${res.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = res.upscaleRouteTr,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Text(
                                                    text = res.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${res.labelTr} (${res.pixelDimsLabel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C) AI Restoration Preset Modes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Yapay Zeka İyileştirme Modu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoEnhancementPreset.entries.forEach { preset ->
                        val selected = qualitySettings.preset == preset
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.2.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(enabled = !isProcessing) { onSelectPreset(preset) }
                                .testTag("video_preset_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoFixHigh,
                                    contentDescription = null,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.titleTr,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.subtitleTr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // D) Collapsible Fine-Tuning Sliders
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onToggleAdvancedSliders)
                    .testTag("toggle_video_advanced_sliders")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp),
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
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "İnce Ayarlar (Parlaklık, Keskinlik, Gürültü, Renk)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedSliders) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (showAdvancedSliders) "Daralt" else "Genişlet"
                    )
                }
            }

            AnimatedVisibility(visible = showAdvancedSliders) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val brightnessRounded = qualitySettings.brightness.roundToInt()
                    SettingSliderRow(
                        label = "Parlaklık & Işık Dengesi",
                        valueText = if (brightnessRounded >= 0) "+$brightnessRounded" else "$brightnessRounded",
                        value = qualitySettings.brightness,
                        range = -25f..35f,
                        onValueChange = onBrightnessChange,
                        testTag = "slider_video_brightness"
                    )
                    SettingSliderRow(
                        label = "Keskinlik & Netleştirme",
                        valueText = "%${(qualitySettings.sharpness * 100).roundToInt()}",
                        value = qualitySettings.sharpness,
                        range = 0f..1f,
                        onValueChange = onSharpnessChange,
                        testTag = "slider_video_sharpness"
                    )
                    SettingSliderRow(
                        label = "Gürültü & Gren Azaltma",
                        valueText = "%${(qualitySettings.denoise * 100).roundToInt()}",
                        value = qualitySettings.denoise,
                        range = 0f..1f,
                        onValueChange = onDenoiseChange,
                        testTag = "slider_video_denoise"
                    )
                    SettingSliderRow(
                        label = "Kontrast Güçlendirme",
                        valueText = "+${(qualitySettings.contrast * 100).roundToInt()}%",
                        value = qualitySettings.contrast,
                        range = 0f..0.6f,
                        onValueChange = onContrastChange,
                        testTag = "slider_video_contrast"
                    )
                    SettingSliderRow(
                        label = "Renk Canlılığı",
                        valueText = "${qualitySettings.saturation.roundToInt()}",
                        value = qualitySettings.saturation,
                        range = -10f..45f,
                        onValueChange = onSaturationChange,
                        testTag = "slider_video_saturation"
                    )
                }
            }
        }
    }
}
