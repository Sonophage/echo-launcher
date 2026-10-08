package com.echo.feature.crossbar.ui.detail

enum class ArtworkType { ICON, BACKGROUND }

val ArtworkType.displayLabel: String
    get() = when (this) {
        ArtworkType.ICON       -> "Game Icon"
        ArtworkType.BACKGROUND -> "Background"
    }

data class DetailMedia(val uri: String, val isVideo: Boolean)

