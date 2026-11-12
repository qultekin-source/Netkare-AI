package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.ExportQualityTier
import com.example.domain.model.ExportUiState
import com.example.domain.model.PhotoExportFormat
import com.example.domain.model.VideoExportFormat

@Composable
fun VideoExportOptionsDialog(
    sourceSpecSummary: String,
    exportState: ExportUiState,
    onSaveToDevice: (VideoExportFormat, ExportQualityTier) -> Unit,
    onShareWithOptions: (VideoExportFormat, ExportQualityTier) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by rememberSaveable { mutableStateOf(VideoExportFormat.MP4_H264) }
    var selectedQuality by rememberSaveable { mutableStateOf(ExportQualityTier.HIGH) }

    ExportOptionsDialogShell(
        mediaTypeBadge = "VİDEO",
        sourceSpecSummary = sourceSpecSummary,
        selectedSummaryTr = "${selectedFormat.labelTr} (.${selectedFormat.extension}) • ${selectedQuality.labelTr}",
        exportState = exportState,
        onDismiss = onDismiss,
        formatContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Video Formatı",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                VideoExportFormat.entries.forEach { format ->
                    ExportChoiceRow(
                        title = format.labelTr,
                        badge = format.codecBadge,
                        subtitle = format.descriptionTr,
                        selected = selectedFormat == format,
                        enabled = !exportState.isExporting,
                        onClick = { selectedFormat = format },
                        testTag = "export_video_format_${format.name.lowercase()}"
                    )
                }
            }
        },
        qualityContent = {
            QualityTierSelectorSection(
                selectedQuality = selectedQuality,
                enabled = !exportState.isExporting,
                onSelectQuality = { selectedQuality = it }
            )
        },
        onSaveClick = { onSaveToDevice(selectedFormat, selectedQuality) },
        onShareClick = { onShareWithOptions(selectedFormat, selectedQuality) }
    )
}

@Composable
fun PhotoExportOptionsDialog(
    sourceSpecSummary: String,
    exportState: ExportUiState,
    onSaveToDevice: (PhotoExportFormat, ExportQualityTier) -> Unit,
    onShareWithOptions: (PhotoExportFormat, ExportQualityTier) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by rememberSaveable { mutableStateOf(PhotoExportFormat.JPEG) }
    var selectedQuality by rememberSaveable { mutableStateOf(ExportQualityTier.HIGH) }

    ExportOptionsDialogShell(
        mediaTypeBadge = "FOTOĞRAF",
        sourceSpecSummary = sourceSpecSummary,
        selectedSummaryTr = "${selectedFormat.labelTr} (.${selectedFormat.extension}) • ${selectedQuality.labelTr}",
        exportState = exportState,
        onDismiss = onDismiss,
        formatContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Görsel Formatı",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                PhotoExportFormat.entries.forEach { format ->
                    ExportChoiceRow(
                        title = format.labelTr,
                        badge = format.badge,
                        subtitle = format.descriptionTr,
                        selected = selectedFormat == format,
                        enabled = !exportState.isExporting,
                        onClick = { selectedFormat = format },
                        testTag = "export_photo_format_${format.name.lowercase()}"
                    )
                }
            }
        },
        qualityContent = {
            QualityTierSelectorSection(
                selectedQuality = selectedQuality,
                enabled = !exportState.isExporting,
                onSelectQuality = { selectedQuality = it }
            )
        },
        onSaveClick = { onSaveToDevice(selectedFormat, selectedQuality) },
        onShareClick = { onShareWithOptions(selectedFormat, selectedQuality) }
    )
}

@Composable
private fun ExportOptionsDialogShell(
    mediaTypeBadge: String,
    sourceSpecSummary: String,
    selectedSummaryTr: String,
    exportState: ExportUiState,
    onDismiss: () -> Unit,
    formatContent: @Composable () -> Unit,
    qualityContent: @Composable () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!exportState.isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .widthIn(max = 540.dp)
                .testTag("export_options_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SaveAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Kaydetme Seçenekleri",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = mediaTypeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = sourceSpecSummary.ifBlank { "Dışa aktarma formatını ve kalitesini seçin" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !exportState.isExporting,
                        modifier = Modifier.testTag("export_options_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                formatContent()

                qualityContent()

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Çıktı Özeti: $selectedSummaryTr",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedVisibility(visible = exportState.isExporting) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exportState.statusDetailTr.ifBlank { "Dışa aktarılıyor..." },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "%${exportState.progressPercent}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (exportState.progressPercent.coerceIn(0, 100) / 100f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProminentSuccessButton(
                        onClick = onSaveClick,
                        enabled = !exportState.isExporting,
                        minHeight = 54.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_save_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cihaza Kaydet", style = MaterialTheme.typography.titleMedium)
                    }

                    ProminentSecondaryButton(
                        onClick = onShareClick,
                        enabled = !exportState.isExporting,
                        accentBorder = true,
                        minHeight = 50.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_share_format_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Bu Formatla Paylaş", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityTierSelectorSection(
    selectedQuality: ExportQualityTier,
    enabled: Boolean,
    onSelectQuality: (ExportQualityTier) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Dışa Aktarma Kalitesi",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        ExportQualityTier.entries.forEach { tier ->
            ExportChoiceRow(
                title = tier.labelTr,
                badge = tier.badgeTr,
                subtitle = tier.subtitleTr,
                selected = selectedQuality == tier,
                enabled = enabled,
                onClick = { onSelectQuality(tier) },
                testTag = "export_quality_${tier.name.lowercase()}"
            )
        }
    }
}

@Composable
private fun ExportChoiceRow(
    title: String,
    badge: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
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
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
