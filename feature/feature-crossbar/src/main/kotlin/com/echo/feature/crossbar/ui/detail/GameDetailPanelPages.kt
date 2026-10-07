package com.echo.feature.crossbar.ui.detail

import com.echo.core.common.format.playTimeLabel
import com.echo.core.domain.model.Game
import com.echo.feature.crossbar.viewmodel.gameMetadataLine

data class DetailPanelContent(
    val title: String,
    val platformName: String,
    val logoUri: String? = null,

    val posterFallbackUri: String? = null,
    val metaLine: String? = null,
    val description: String? = null,
    val fileName: String? = null,

    val playTime: String? = null,

    val videoUri: String? = null,
    val media: List<DetailMedia> = emptyList(),
)

fun panelFileName(romPath: String?): String? =
    romPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

fun detailPanelContentFor(
    game: Game,
    platformName: String,
    media: List<DetailMedia>,
    videoUri: String? = null,
): DetailPanelContent = DetailPanelContent(
    title = game.displayTitle,
    platformName = platformName,
    logoUri = game.logoUri,
    posterFallbackUri = game.artworkUri,
    metaLine = gameMetadataLine(game.releaseYear, game.genre, game.developer, game.players),
    description = game.description,

    fileName = panelFileName(game.romPath),
    playTime = game.totalPlayTimeMillis.takeIf { it > 0L }?.let(::playTimeLabel),
    videoUri = videoUri,
    media = media,
)
