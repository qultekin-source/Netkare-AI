package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.AiRestorationService
import com.example.data.ai.InteractionsVideoService
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryRepository
import com.example.domain.model.CompletionMediaType
import com.example.ui.adaptive.AppWindowWidthClass
import com.example.ui.adaptive.rememberAdaptiveWindowState
import com.example.ui.components.EnhancementCompletionDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Surface
import com.example.ui.components.PhotoExportOptionsDialog
import com.example.ui.components.VideoExportOptionsDialog
import com.example.ui.screens.BatchStudioScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PhotoRestoreScreen
import com.example.ui.screens.VideoGenScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.ui.viewmodel.BatchStudioViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.PhotoRestoreViewModel
import com.example.ui.viewmodel.VideoGenViewModel
import com.example.ui.viewmodel.VideoStudioViewModel
import kotlinx.coroutines.launch
import java.io.File

enum class StudioTab(
    val labelTr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    VIDEO("İyileştir", Icons.Rounded.VideoSettings, Icons.Outlined.VideoSettings, "nav_tab_video"),
    PHOTO("Foto", Icons.Rounded.AutoFixHigh, Icons.Outlined.AutoFixHigh, "nav_tab_photo"),
    VIDEO_GEN("Video Üret", Icons.Rounded.MovieCreation, Icons.Outlined.MovieCreation, "nav_tab_video_gen"),
    CHAT("AI Sohbet", Icons.Rounded.Forum, Icons.Outlined.Forum, "nav_tab_chat"),
    HISTORY("Geçmiş", Icons.Rounded.History, Icons.Outlined.History, "nav_tab_history")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetKareApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val aiService = remember { AiRestorationService() }
    val interactionsService = remember { InteractionsVideoService() }
    val historyRepository = remember {
        HistoryRepository(AppDatabase.getInstance(application).enhancementDao())
    }

    val videoViewModel: VideoStudioViewModel = viewModel(
        factory = VideoStudioViewModel.provideFactory(application, aiService, historyRepository)
    )
    val photoViewModel: PhotoRestoreViewModel = viewModel(
        factory = PhotoRestoreViewModel.provideFactory(application, aiService, historyRepository)
    )
    val videoGenViewModel: VideoGenViewModel = viewModel(
        factory = VideoGenViewModel.provideFactory(application, interactionsService, historyRepository)
    )
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.provideFactory(aiService)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.provideFactory(application, historyRepository)
    )
    val batchViewModel: BatchStudioViewModel = viewModel(
        factory = BatchStudioViewModel.provideFactory(application, aiService, historyRepository)
    )

    var currentTab by rememberSaveable { mutableStateOf(StudioTab.VIDEO) }
    var showBatchScreen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val videoState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val photoState by photoViewModel.uiState.collectAsStateWithLifecycle()
    val videoGenState by videoGenViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val batchState by batchViewModel.uiState.collectAsStateWithLifecycle()

    val activeCompletionEvent = videoState.completionEvent
        ?: photoState.completionEvent
        ?: videoGenState.completionEvent

    fun shareForMediaType(type: CompletionMediaType) {
        when (type) {
            CompletionMediaType.VIDEO -> videoViewModel.shareEnhancedVideo()
            CompletionMediaType.PHOTO -> photoViewModel.shareRestoredPhoto()
            CompletionMediaType.GENERATED_VIDEO -> videoGenViewModel.shareGeneratedVideo()
        }
    }

    fun openSaveOptionsForType(type: CompletionMediaType) {
        when (type) {
            CompletionMediaType.VIDEO -> videoViewModel.openExportOptions()
            CompletionMediaType.PHOTO -> photoViewModel.openExportOptions()
            CompletionMediaType.GENERATED_VIDEO -> videoGenViewModel.openExportOptions()
        }
    }

    fun dismissEventForType(type: CompletionMediaType) {
        when (type) {
            CompletionMediaType.VIDEO -> videoViewModel.dismissCompletionEvent()
            CompletionMediaType.PHOTO -> photoViewModel.dismissCompletionEvent()
            CompletionMediaType.GENERATED_VIDEO -> videoGenViewModel.dismissCompletionEvent()
        }
    }

    LaunchedEffect(videoState.statusMessage) {
        videoState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            videoViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(photoState.statusMessage) {
        photoState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            photoViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(videoGenState.statusMessage) {
        videoGenState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            videoGenViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(historyState.statusMessage) {
        historyState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            historyViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(batchState.statusMessage) {
        batchState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            batchViewModel.clearStatusMessage()
        }
    }

    BackHandler(enabled = showBatchScreen || currentTab != StudioTab.VIDEO) {
        if (showBatchScreen) {
            showBatchScreen = false
        } else {
            currentTab = StudioTab.VIDEO
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val adaptiveState = rememberAdaptiveWindowState(
            availableWidth = maxWidth,
            availableHeight = maxHeight
        )
        val useNavigationRail = adaptiveState.widthClass != AppWindowWidthClass.COMPACT

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "NetKare AI",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        actions = {
                            Surface(
                                onClick = { showBatchScreen = !showBatchScreen },
                                shape = RoundedCornerShape(50),
                                color = if (showBatchScreen) {
                                    MaterialTheme.colorScheme.primary
                                } else if (batchState.isBatchRunning) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (showBatchScreen) {
                                        MaterialTheme.colorScheme.primary
                                    } else if (batchState.isBatchRunning) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                ),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .testTag("top_bar_batch_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Layers,
                                        contentDescription = "Toplu İşlem",
                                        tint = if (showBatchScreen) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else if (batchState.isBatchRunning) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = when {
                                            batchState.isBatchRunning -> "Toplu (%${(batchState.overallProgressFraction * 100).toInt()})"
                                            batchState.totalCount > 0 -> "Toplu (${batchState.totalCount})"
                                            else -> "Toplu İşlem"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (showBatchScreen) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            StudioTab.entries.forEach { tab ->
                                val selected = !showBatchScreen && currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        showBatchScreen = false
                                        currentTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelTr
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelTr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier
                            .displayCutoutPadding()
                            .navigationBarsPadding()
                            .padding(top = innerPadding.calculateTopPadding(), bottom = 12.dp)
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = !showBatchScreen && currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    showBatchScreen = false
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelTr
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.labelTr,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val effectivePadding = if (useNavigationRail) {
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                    } else {
                        innerPadding
                    }

                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (adaptiveState.isDualPaneRecommended) 1280.dp else 760.dp)

                    if (showBatchScreen) {
                        BatchStudioScreen(
                            viewModel = batchViewModel,
                            adaptiveState = adaptiveState,
                            contentPadding = effectivePadding,
                            modifier = contentModifier
                        )
                    } else {
                        when (currentTab) {
                            StudioTab.VIDEO -> VideoStudioScreen(
                                viewModel = videoViewModel,
                                adaptiveState = adaptiveState,
                                contentPadding = effectivePadding,
                                onOpenBatchMode = { showBatchScreen = true },
                                modifier = contentModifier
                            )
                            StudioTab.PHOTO -> PhotoRestoreScreen(
                                viewModel = photoViewModel,
                                adaptiveState = adaptiveState,
                                contentPadding = effectivePadding,
                                onOpenBatchMode = { showBatchScreen = true },
                                modifier = contentModifier
                            )
                            StudioTab.VIDEO_GEN -> VideoGenScreen(
                                viewModel = videoGenViewModel,
                                adaptiveState = adaptiveState,
                                onSendToUpscaler = { uri ->
                                    videoViewModel.onVideoSelected(uri)
                                    currentTab = StudioTab.VIDEO
                                },
                                contentPadding = effectivePadding,
                                modifier = contentModifier
                            )
                            StudioTab.CHAT -> ChatScreen(
                                viewModel = chatViewModel,
                                adaptiveState = adaptiveState,
                                contentPadding = effectivePadding,
                                modifier = contentModifier
                            )
                            StudioTab.HISTORY -> HistoryScreen(
                                viewModel = historyViewModel,
                                adaptiveState = adaptiveState,
                                onOpenInStudio = { record ->
                                    val file = File(record.outputPath)
                                    if (file.exists()) {
                                        val fileUri = Uri.fromFile(file)
                                        if (record.mediaType == "VIDEO") {
                                            videoViewModel.onVideoSelected(fileUri)
                                            currentTab = StudioTab.VIDEO
                                        } else {
                                            photoViewModel.onPhotoSelected(fileUri)
                                            currentTab = StudioTab.PHOTO
                                        }
                                    }
                                },
                                onNavigateToVideoStudio = { currentTab = StudioTab.VIDEO },
                                onNavigateToPhotoStudio = { currentTab = StudioTab.PHOTO },
                                contentPadding = effectivePadding,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }

        activeCompletionEvent?.let { event ->
            EnhancementCompletionDialog(
                event = event,
                onShare = {
                    dismissEventForType(event.mediaType)
                    shareForMediaType(event.mediaType)
                },
                onSaveToGallery = {
                    dismissEventForType(event.mediaType)
                    openSaveOptionsForType(event.mediaType)
                },
                onDismiss = {
                    val mediaType = event.mediaType
                    dismissEventForType(mediaType)
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "${event.titleTr} Paylaşıma hazır.",
                            actionLabel = "Kaydet",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            openSaveOptionsForType(mediaType)
                        }
                    }
                }
            )
        }

        if (videoState.exportState.isDialogVisible) {
            val resLabel = videoState.qualitySettings.targetResolution.labelTr
            val presetLabel = videoState.qualitySettings.preset.titleTr
            VideoExportOptionsDialog(
                sourceSpecSummary = "$resLabel • $presetLabel",
                exportState = videoState.exportState,
                onSaveToDevice = { format, quality ->
                    videoViewModel.exportVideoWithOptions(format, quality, shareAfterExport = false)
                },
                onShareWithOptions = { format, quality ->
                    videoViewModel.exportVideoWithOptions(format, quality, shareAfterExport = true)
                },
                onDismiss = videoViewModel::dismissExportOptions
            )
        }

        if (photoState.exportState.isDialogVisible) {
            val afterBmp = photoState.activeAfterBitmap
            val dimLabel = afterBmp?.let { "${it.width}×${it.height}" } ?: "Yüksek Çözünürlük"
            PhotoExportOptionsDialog(
                sourceSpecSummary = "$dimLabel • ${photoState.selectedPreset.titleTr}",
                exportState = photoState.exportState,
                onSaveToDevice = { format, quality ->
                    photoViewModel.exportPhotoWithOptions(format, quality, shareAfterExport = false)
                },
                onShareWithOptions = { format, quality ->
                    photoViewModel.exportPhotoWithOptions(format, quality, shareAfterExport = true)
                },
                onDismiss = photoViewModel::dismissExportOptions
            )
        }

        if (videoGenState.exportState.isDialogVisible) {
            VideoExportOptionsDialog(
                sourceSpecSummary = "${videoGenState.resolution} • ${videoGenState.duration} • ${videoGenState.aspectRatio.apiValue}",
                exportState = videoGenState.exportState,
                onSaveToDevice = { format, quality ->
                    videoGenViewModel.exportGeneratedVideoWithOptions(format, quality, shareAfterExport = false)
                },
                onShareWithOptions = { format, quality ->
                    videoGenViewModel.exportGeneratedVideoWithOptions(format, quality, shareAfterExport = true)
                },
                onDismiss = videoGenViewModel::dismissExportOptions
            )
        }
    }
}
