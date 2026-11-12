package com.example.ui.adaptive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.flowOf

enum class AppWindowWidthClass {
    COMPACT,  // < 600.dp (portrait phone)
    MEDIUM,   // 600.dp .. 839.dp (landscape phone, portrait tablet, half-unfolded)
    EXPANDED  // >= 840.dp (tablet, unfolded large foldable, desktop)
}

enum class FoldPosture {
    FLAT,
    BOOK,      // Half-opened with vertical hinge -> natural two-pane side-by-side
    TABLETOP   // Half-opened with horizontal hinge -> top preview / bottom controls
}

data class AdaptiveWindowState(
    val widthClass: AppWindowWidthClass,
    val availableWidth: Dp,
    val availableHeight: Dp,
    val posture: FoldPosture,
    val isDualPaneRecommended: Boolean
)

@Composable
fun rememberAdaptiveWindowState(
    availableWidth: Dp,
    availableHeight: Dp
): AdaptiveWindowState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val layoutInfoFlow = remember(activity) {
        if (activity != null) {
            runCatching { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
                .getOrElse { flowOf(WindowLayoutInfo(emptyList())) }
        } else {
            flowOf(WindowLayoutInfo(emptyList()))
        }
    }
    val layoutInfo by layoutInfoFlow.collectAsState(initial = WindowLayoutInfo(emptyList()))

    val widthClass = when {
        availableWidth >= 840.dp -> AppWindowWidthClass.EXPANDED
        availableWidth >= 600.dp -> AppWindowWidthClass.MEDIUM
        else -> AppWindowWidthClass.COMPACT
    }

    val foldFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
    val posture = when {
        foldFeature == null -> FoldPosture.FLAT
        foldFeature.state == FoldingFeature.State.HALF_OPENED &&
            foldFeature.orientation == FoldingFeature.Orientation.VERTICAL -> FoldPosture.BOOK
        foldFeature.state == FoldingFeature.State.HALF_OPENED &&
            foldFeature.orientation == FoldingFeature.Orientation.HORIZONTAL -> FoldPosture.TABLETOP
        foldFeature.isSeparating && foldFeature.orientation == FoldingFeature.Orientation.VERTICAL -> FoldPosture.BOOK
        else -> FoldPosture.FLAT
    }

    val isDualPane = widthClass == AppWindowWidthClass.EXPANDED ||
        (widthClass == AppWindowWidthClass.MEDIUM && posture == FoldPosture.BOOK)

    return AdaptiveWindowState(
        widthClass = widthClass,
        availableWidth = availableWidth,
        availableHeight = availableHeight,
        posture = posture,
        isDualPaneRecommended = isDualPane
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
