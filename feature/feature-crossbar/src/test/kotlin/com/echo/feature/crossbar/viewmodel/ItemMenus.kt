package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.domain.model.GameGenre
import com.echo.core.domain.model.HideLocationType
import com.echo.core.domain.model.Photo
import com.echo.core.domain.model.PlatformIds
import com.echo.core.domain.model.Video
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.ui.components.MenuRow
import com.echo.feature.crossbar.music.MusicPlaybackState
import com.echo.feature.crossbar.ui.detail.DetailPanelContent
import com.echo.feature.photos.PhotoViewerUiState
import com.echo.feature.video.VideoDetailUiState

// the one list of item menu builders (spec #2), each called once per state that changes its rows; a new builder is
// added here. MenuLabelsTest reads every label on it, and CompanionMenuTest compares each kind's Companion menu
// with its Main Screen menu (spec #32)
internal object ItemMenus {
    private val games = Category(id = BuiltInCategory.GAMES, name = "Games", iconKey = "games", type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true)
    private val retro = games.copy(id = "retro", name = "Retro", iconKey = "retro", type = CategoryType.SMART)
    private val other = retro.copy(id = "other", name = "Other")
    private fun game(platformId: String = "psp", packageName: String? = null, pinned: Boolean = false, favorite: Boolean = false) = CrossbarItem(
        id = "g1", title = "Crisis Core", gameId = 1L, platformId = platformId, packageName = packageName,
        genre = GameGenre.entries.first(), pinnedInCategory = pinned, isFavorite = favorite,
        romPath = if (packageName == null) "/storage/emulated/0/ROMs/psp/Crisis Core.iso" else null,
    )
    private val inRetro = CrossbarUiState(categories = listOf(games, retro, other), selectedCategoryIndex = 1, secondDisplayPresent = true)
    private val hide = Triple(HideLocationType.CATEGORY, "retro", "Retro")
    private val gameInfoOpen = inRetro.copy(gameInfo = GameInfoState(
        game(), content = DetailPanelContent(title = "Crisis Core", platformName = "PSP"), videoUri = "/v.mp4", manualPath = "/m.pdf",
    ))
    private val video = Video(id = "v", libraryId = "l", uri = "content://v", displayName = "v.mp4", resumePositionMs = 5)
    val photo = Photo(id = "p", libraryId = "l", uri = "content://p", displayName = "p.jpg")

    val all: List<Pair<String, List<MenuRow<*>>>> = listOf(
        "game" to gameContextMenuItems(game(), inRetro, 2, true, hide) +
            gameContextMenuItems(game(pinned = true, favorite = true), inRetro.copy(genreFilter = GameGenre.entries.first(), recentPins = listOf("g:1")), 1, false, null) +
            gameContextMenuItems(game(), inRetro.copy(selectedPlatformId = CrossbarViewModel.MISSING_PLATFORM_ID), 1, false, null),
        "game with Game Info open" to gameContextMenuItems(game(), gameInfoOpen, 1, false, null),
        "Android game" to gameContextMenuItems(game(PlatformIds.ANDROID, "com.game"), inRetro, 1, false, null),
        "PC game" to gameContextMenuItems(game(PlatformIds.WINDOWS), inRetro.copy(categories = listOf(games), selectedCategoryIndex = 0), 1, false, null),
        "app" to appContextMenuItems(inRetro, "retro", onRecentShelf = true, packageName = "com.app") +
            appContextMenuItems(inRetro, "retro", onRecentShelf = false, packageName = "com.app", isFavorite = true, pinnedInCategory = true, isGame = true),
        "video file" to videoFileContextMenuItems(isFavorite = true, resumePositionMs = 5, hasWatchStamp = true, inPlaylist = true) +
            videoFileContextMenuItems(isFavorite = false, resumePositionMs = 0, hasWatchStamp = false, inPlaylist = false),
        "Shelves" to shelvesPickerItems(current = null),
        "video library" to videoLibraryContextMenuItems(),
        "video playlist" to videoPlaylistContextMenuItems(),
        "photo" to photoFileContextMenuItems() + photoFileContextMenuItems(isFavorite = true),
        "photo library" to photoLibraryContextMenuItems(),
        "book" to bookContextMenuItems(hasOpenStamp = true),
        "album" to albumContextMenuItems(),
        "recent album" to recentAlbumContextMenuItems(),
        "track" to musicTrackContextMenuItems(playlistId = 1L, hasPlayStamp = true),
        "playlist" to playlistRowContextMenuItems(),
        "music player" to musicPlayerMenuItems(MusicPlaybackState()),
        "Now Playing" to nowPlayingContextMenuItems(isPlaying = true) + nowPlayingContextMenuItems(isPlaying = false),
        "system" to platformContextMenuItems("psp", pinned = true, emulatorLabel = "PPSSPP", overrideCount = 2, romDirectory = "ROMs") +
            platformContextMenuItems(PlatformIds.WINDOWS, pinned = false) + platformContextMenuItems(PlatformIds.ANDROID, pinned = false),
        "All Games" to allGamesContextMenuItems(GameGrouping.SYSTEM) + allGamesContextMenuItems(GameGrouping.GENRE),
        "media root" to MediaRootKind.entries.flatMap { mediaRootContextMenuItems(linked = true, kind = it) },
        "media folders" to MediaRootKind.entries.flatMap { mediaFoldersContextMenuItems(it) },
        "photo viewer Options" to PhotoViewerUiState(photos = listOf(photo)).optionsMenu.rows,
        "video detail Options" to VideoDetailUiState(video = video).optionsMenu.rows +
            VideoDetailUiState(video = video.copy(isFavorite = true, resumePositionMs = 0)).optionsMenu.rows,
    )
}
