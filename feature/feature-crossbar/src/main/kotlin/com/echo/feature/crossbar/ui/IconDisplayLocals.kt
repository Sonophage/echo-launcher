package com.echo.feature.crossbar.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.echo.core.domain.model.VideoSnapPlacement

data class FocusedGameVideo(
    val gameId: Long,
    val uri: String,
    val placement: VideoSnapPlacement = VideoSnapPlacement.ICON,
)

enum class SnapSite { TILE, BACKGROUND }

fun snapSiteFor(placement: VideoSnapPlacement): SnapSite =
    if (placement == VideoSnapPlacement.BACKGROUND) SnapSite.BACKGROUND else SnapSite.TILE

val LocalFocusedGameVideo = compositionLocalOf<FocusedGameVideo?> { null }

val LocalCrossbarHorizontalShift = compositionLocalOf { 0.dp }

// the applied theme's crossbar sizes (owner, 2026-10-09); the default is ECHO's own
val LocalCrossbarLayout = staticCompositionLocalOf { com.echo.themekit.CrossbarLayoutSpec.DEFAULT }
