package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.model.VideoMetadataInfo

@Composable
fun VideoSelectionAndPreviewCard(
    selectedVideo: VideoMetadataInfo?,
    enhancedUri: Uri?,
    showAfterPreview: Boolean,
    targetResolutionLabel: String,
    isLoadingVideo: Boolean,
    isCreatingSample: Boolean,
    isProcessing: Boolean,
    onPickFromGallery: () -> Unit,
    onPickFromFiles: () -> Unit,
    onCreateSample: () -> Unit,
    onClearVideo: () -> Unit,
    onToggleShowAfter: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        if (isLoadingVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Video ve kare önizlemeleri hazırlanıyor...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (selectedVideo == null) {
            VideoGallerySelectionDropzone(
                isCreatingSample = isCreatingSample,
                isProcessing = isProcessing,
                onPickFromGallery = onPickFromGallery,
                onPickFromFiles = onPickFromFiles,
                onCreateSample = onCreateSample
            )
        } else {
            SelectedVideoInteractivePreview(
                video = selectedVideo,
                enhancedUri = enhancedUri,
                showAfter = showAfterPreview,
                targetResolutionLabel = targetResolutionLabel,
                isProcessing = isProcessing,
                onToggleShowAfter = onToggleShowAfter,
                onChangeVideo = onPickFromGallery,
                onClearVideo = onClearVideo
            )
        }
    }
}

@Composable
private fun VideoGallerySelectionDropzone(
    isCreatingSample: Boolean,
    isProcessing: Boolean,
    onPickFromGallery: () -> Unit,
    onPickFromFiles: () -> Unit,
    onCreateSample: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "1. Video Seçimi & Yükleme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                        style = Stroke(
                            width = 1.6.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                        )
                    )
                }
                .clickable(enabled = !isProcessing, onClick = onPickFromGallery)
                .padding(horizontal = 18.dp, vertical = 22.dp)
                .testTag("video_picker_dropzone"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoCall,
                        contentDescription = "Galeriden video seç",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Text(
                    text = "Galerinizden Bir Video Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MP4, MOV, MKV • Android Photo Picker ile güvenli ve izinsiz seçim",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                ProminentPrimaryButton(
                    onClick = onPickFromGallery,
                    enabled = !isProcessing,
                    minHeight = 52.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Galeriden Video Seç", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProminentSecondaryButton(
                onClick = onPickFromFiles,
                enabled = !isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_video_files_button")
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dosyalardan", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            ProminentSecondaryButton(
                onClick = onCreateSample,
                enabled = !isProcessing && !isCreatingSample,
                accentBorder = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("load_sample_video_button")
            ) {
                if (isCreatingSample) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isCreatingSample) "Hazırlanıyor" else "Örnek VHS",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SelectedVideoInteractivePreview(
    video: VideoMetadataInfo,
    enhancedUri: Uri?,
    showAfter: Boolean,
    targetResolutionLabel: String,
    isProcessing: Boolean,
    onToggleShowAfter: (Boolean) -> Unit,
    onChangeVideo: () -> Unit,
    onClearVideo: () -> Unit
) {
    val context = LocalContext.current
    val activeUri = if (showAfter && enhancedUri != null) enhancedUri else video.uri
    var selectedFrameIdx by remember(video.uri) { mutableIntStateOf(0) }
    var useSideBySide by remember(video.uri) { mutableStateOf(false) }

    LaunchedEffect(enhancedUri, isProcessing) {
        if (enhancedUri != null || isProcessing) {
            useSideBySide = true
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(activeUri, useSideBySide) {
        if (!useSideBySide) {
            exoPlayer.setMediaItem(MediaItem.fromUri(activeUri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row with title & quick change/clear actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ön İzleme Hazır • Kare şeridine dokunarak zaman atlayabilirsiniz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ProminentSecondaryButton(
                onClick = onChangeVideo,
                enabled = !isProcessing,
                minHeight = 48.dp,
                modifier = Modifier.testTag("change_video_button")
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Değiştir", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(
                onClick = onClearVideo,
                enabled = !isProcessing,
                modifier = Modifier.testTag("clear_video_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Videoyu kaldır",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Video Player Surface OR Side-by-Side Comparison View
        if (useSideBySide) {
            SideBySideVideoComparisonView(
                originalUri = video.uri,
                enhancedUri = enhancedUri,
                sourceResolutionLabel = video.resolutionLabel,
                targetResolutionLabel = targetResolutionLabel,
                isProcessing = isProcessing
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
                    .testTag("video_preview_player")
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        if (showAfter && enhancedUri != null) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = if (showAfter && enhancedUri != null) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (showAfter && enhancedUri != null) {
                                "SONRASI • $targetResolutionLabel"
                            } else {
                                "ORİJİNAL • ${video.resolutionLabel}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Interactive Filmstrip Timeline Thumbnails
        if (video.timelineThumbnails.isNotEmpty()) {
            val stripRatios = listOf(0.05f, 0.25f, 0.50f, 0.75f, 0.92f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                video.timelineThumbnails.forEachIndexed { idx, frameBmp ->
                    val isSelected = selectedFrameIdx == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(7.dp))
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .clickable {
                                selectedFrameIdx = idx
                                val ratio = stripRatios.getOrElse(idx) { 0f }
                                val targetMs = (video.durationMs * ratio).toLong().coerceAtLeast(0L)
                                exoPlayer.seekTo(targetMs)
                            }
                            .testTag("video_filmstrip_frame_$idx")
                    ) {
                        Image(
                            bitmap = frameBmp.asImageBitmap(),
                            contentDescription = "Video kare önizlemesi ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Metadata Specs Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoSpecPill(Icons.Rounded.AspectRatio, video.resolutionLabel, Modifier.weight(1f))
            VideoSpecPill(Icons.Rounded.Timer, video.durationLabel, Modifier.weight(1f))
            VideoSpecPill(Icons.Rounded.Speed, "${video.frameRate} FPS", Modifier.weight(1f))
            VideoSpecPill(Icons.Rounded.Storage, video.fileSizeLabel, Modifier.weight(1f))
        }

        // 3-Way Comparison Mode Bar: Yan Yana | Öncesi | Sonrası
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreviewToggleSmall(
                label = "Yan Yana",
                selected = useSideBySide,
                onClick = { useSideBySide = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("video_side_by_side_toggle")
            )
            PreviewToggleSmall(
                label = "Öncesi",
                selected = !useSideBySide && (!showAfter || enhancedUri == null),
                onClick = {
                    useSideBySide = false
                    onToggleShowAfter(false)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("video_before_toggle")
            )
            if (enhancedUri != null) {
                PreviewToggleSmall(
                    label = "Sonrası ($targetResolutionLabel)",
                    selected = !useSideBySide && showAfter,
                    onClick = {
                        useSideBySide = false
                        onToggleShowAfter(true)
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("video_after_toggle")
                )
            }
        }
    }
}

@Composable
private fun VideoSpecPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreviewToggleSmall(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) null else BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
