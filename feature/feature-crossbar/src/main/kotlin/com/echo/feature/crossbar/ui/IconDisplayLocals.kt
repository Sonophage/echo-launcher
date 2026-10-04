package com.echo.feature.crossbar.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import com.echo.core.domain.model.VideoSnapPlacement

data class FocusedGameVideo(
    val gameId: Long,
    val uri: String,
    val placement: VideoSnapPlacement = VideoSnapPlacement.ICON,
)

enum class SnapSite { TILE, BACKGROUND, PANEL }

fun snapSiteFor(
    placement: VideoSnapPlacement,
    panelShowingVideo: Boolean,
): SnapSite? = when {
    panelShowingVideo -> SnapSite.PANEL
    placement == VideoSnapPlacement.BACKGROUND -> SnapSite.BACKGROUND
    else -> SnapSite.TILE
}

val LocalFocusedGameVideo = compositionLocalOf<FocusedGameVideo?> { null }

val LocalPanelShowingVideo = compositionLocalOf { false }

val LocalCrossbarHorizontalShift = compositionLocalOf { 0.dp }
