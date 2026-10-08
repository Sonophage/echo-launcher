package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.Game

const val FAN_COVER_COUNT = 3

const val GRID_COVER_COUNT = 4

const val INSIDE_COVER_COUNT = 4

fun fanCoversOf(games: List<Game>, limit: Int = INSIDE_COVER_COUNT): List<String> = games
    .sortedWith(compareByDescending<Game> { it.dateAdded ?: 0L }.thenByDescending { it.id })
    .mapNotNull { it.artworkUri }
    .take(limit)

internal fun fanCoversToDraw(insideCovers: List<String>, cardArtGrid: Boolean, hasOwnArt: Boolean = false): List<String> =
    if (cardArtGrid && !hasOwnArt) insideCovers.take(FAN_COVER_COUNT) else emptyList()

// the Art slot of a system row or All Games, whose image replaces the covers from inside it
fun systemArtSlotFor(item: CrossbarItem?): String? {
    val id = when {
        item == null -> null
        // a genre folder is drawn as All Games, but All Games' own art is not its
        item.id in genreItemIds -> null
        item.type == CrossbarItemType.ALL_GAMES -> "allgames"
        item.type == CrossbarItemType.MEMORY_CARD -> item.platformId
        else -> null
    }
    return id?.takeIf { it in com.echo.themekit.SYSICON_PLATFORM_IDS }?.let { "sysart_$it" }
}

const val MEDIA_COVER_POOL = GRID_COVER_COUNT * 6

fun List<String>.gridSliceAt(index: Int): List<String> =
    drop(index * GRID_COVER_COUNT).take(GRID_COVER_COUNT)
