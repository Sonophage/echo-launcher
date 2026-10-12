package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.GameInfoState

// what the Companion Screen shows (ADR 0001). Settings on the Companion Screen adds its own case here.
sealed interface CompanionContent {
    // an item's Details; while it is the game ECHO launched and that game runs, with the session clock and Resume
    data class Details(val info: GameInfoState, val playing: Boolean = false, val playingSince: Long? = null) : CompanionContent
}

// the Companion rule: the running game, else the item an open menu belongs to (the one shown when it opened),
// else the hovered item (the App Drawer's over the Crossbar's, Search's result over both), else what it showed
// [last], so it never goes blank. A game kept after it ends loses its clock and Resume.
fun companionContent(main: BottomScreenState, last: CompanionContent?): CompanionContent? {
    main.playing?.let { return CompanionContent.Details(it, playing = true, playingSince = main.playingSince) }
    val kept = (last as? CompanionContent.Details)?.copy(playing = false, playingSince = null) ?: last
    // ponytail: the owner is what was shown when the menu opened; a menu opened within the 150 ms the cursor
    // takes to settle keeps the item before it, until the menu closes. Track the menu's own item if that shows.
    if (main.menuOpen && kept != null) return kept
    val hovered = if (main.searching) main.hovered else main.drawer ?: main.hovered
    return hovered?.let { CompanionContent.Details(it) } ?: kept
}

// what the Main Screen has hovered, besides the App Drawer (which reports its own): Search's focused result while
// Search is open, else the Crossbar's cursor
fun mainScreenHover(s: CrossbarUiState): CrossbarItem? =
    s.search?.let { it.rows.getOrNull(it.selectedIndex) } ?: s.focusedItem.takeIf { s.search == null }

// the items with Details on the Companion Screen: games, installed apps, songs, videos, books, photos and a
// system's card. A settings row or a folder has none, so hovering one keeps the last thing shown
internal fun companionShows(item: CrossbarItem): Boolean = when {
    item.gameId != null -> true
    item.isAndroidApp && item.packageName != null -> true
    item.type == CrossbarItemType.MEMORY_CARD -> item.platformId != null
    else -> item.type in MEDIA_TYPES
}

private val MEDIA_TYPES = setOf(
    CrossbarItemType.MUSIC_TRACK, CrossbarItemType.MUSIC_GROUP, CrossbarItemType.VIDEO_FILE,
    CrossbarItemType.LIBRARY_BOOK, CrossbarItemType.PHOTO_FILE,
)

// a game or app is drawn as Game Info; a song, video, book, photo or system as Recent draws an item in full view
internal fun showsGameInfo(item: CrossbarItem): Boolean = item.gameId != null || item.isAndroidApp
