package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SideBySideVideoComparisonView(
    originalUri: Uri,
    enhancedUri: Uri?,
    sourceResolutionLabel: String,
    targetResolutionLabel: String,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    val beforePlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    val afterPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 1f
        }
    }

    DisposableEffect(originalUri) {
        beforePlayer.setMediaItem(MediaItem.fromUri(originalUri))
        beforePlayer.prepare()
        beforePlayer.playWhenReady = isPlaying
        onDispose { }
    }

    DisposableEffect(enhancedUri) {
        if (enhancedUri != null) {
            afterPlayer.setMediaItem(MediaItem.fromUri(enhancedUri))
            afterPlayer.prepare()
            afterPlayer.seekTo(beforePlayer.currentPosition.coerceAtLeast(0L))
            afterPlayer.playWhenReady = isPlaying
        } else {
            afterPlayer.stop()
            afterPlayer.clearMediaItems()
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            beforePlayer.release()
            afterPlayer.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_side_by_side_comparison"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Pane: ORİJİNAL (ÖNCE)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        MaterialTheme.shapes.medium
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = beforePlayer
                                useController = false
                            }
                        },
                        update = { view -> view.player = beforePlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "ÖNCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Orijinal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceResolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Pane: İŞLENMİŞ (SONRA)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(
                        1.5.dp,
                        if (enhancedUri != null) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        MaterialTheme.shapes.medium
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (enhancedUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = afterPlayer
                                    useController = false
                                }
                            },
                            update = { view -> view.player = afterPlayer },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ProcessingSidePlaceholder(
                            label = if (isProcessing) "İşleniyor..." else "Önizleme Bekliyor",
                            targetLabel = targetResolutionLabel,
                            isProcessing = isProcessing
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (enhancedUri != null) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "SONRA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (enhancedUri != null) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (enhancedUri != null) "İşlenmiş" else "Hedef",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = targetResolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Synchronized Playback Controls for Side-by-Side Video Comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProminentSecondaryButton(
                onClick = {
                    val nextPlay = !isPlaying
                    isPlaying = nextPlay
                    beforePlayer.playWhenReady = nextPlay
                    if (enhancedUri != null) {
                        afterPlayer.seekTo(beforePlayer.currentPosition.coerceAtLeast(0L))
                        afterPlayer.playWhenReady = nextPlay
                    }
                },
                accentBorder = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("video_compare_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "Eşzamanlı Duraklat" else "Eşzamanlı Oynat",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            ProminentSecondaryButton(
                onClick = {
                    beforePlayer.seekTo(0L)
                    if (enhancedUri != null) {
                        afterPlayer.seekTo(0L)
                    }
                    isPlaying = true
                    beforePlayer.playWhenReady = true
                    if (enhancedUri != null) {
                        afterPlayer.playWhenReady = true
                    }
                },
                modifier = Modifier.testTag("video_compare_restart")
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = "Başa sar", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Başa Sar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SideBySidePhotoComparisonView(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val aspect = (beforeBitmap.width.toFloat() / beforeBitmap.height.coerceAtLeast(1).toFloat())
        .coerceIn(0.65f, 1.55f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("photo_side_by_side_comparison"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left: ORİJİNAL (ÖNCE)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    MaterialTheme.shapes.medium
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = beforeBitmap.asImageBitmap(),
                    contentDescription = "Orijinal fotoğraf",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "ÖNCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Orijinal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${beforeBitmap.width}×${beforeBitmap.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Right: İŞLENMİŞ (SONRA)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    1.5.dp,
                    if (afterBitmap != null) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    MaterialTheme.shapes.medium
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (afterBitmap != null) {
                    Image(
                        bitmap = afterBitmap.asImageBitmap(),
                        contentDescription = "İşlenmiş fotoğraf",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ProcessingSidePlaceholder(
                        label = if (isProcessing) "AI İyileştiriyor..." else "Henüz İşlenmedi",
                        targetLabel = if (isProcessing) "Netleştirme" else "Hazır",
                        isProcessing = isProcessing
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (afterBitmap != null) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SONRA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (afterBitmap != null) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (afterBitmap != null) "İşlenmiş" else "Hedef",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = afterBitmap?.let { "${it.width}×${it.height}" } ?: "AI HD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ProcessingSidePlaceholder(
    label: String,
    targetLabel: String,
    isProcessing: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "side_shimmer")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = if (isProcessing) alphaAnim else 0.22f
                        )
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
