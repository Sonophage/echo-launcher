package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.HideLocationType
import com.echo.core.domain.model.PlatformIds
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.ui.components.MenuGroup

internal fun CrossbarUiState.currentCategoryOrNull(): Category? =
    categories.getOrNull(selectedCategoryIndex)

internal fun CrossbarUiState.categoryDisplayNameOf(id: String): String = when (id) {
    CrossbarViewModel.MUSIC_APPS_CATEGORY_ID -> "Music Apps"
    CrossbarViewModel.VIDEO_APPS_CATEGORY_ID -> "Video Apps"
    else -> categories.firstOrNull { it.id == id }?.name ?: id
}

internal fun gameContextMenuItems(
    item: CrossbarItem,
    state: CrossbarUiState,
    discCount: Int,
    onRecentShelf: Boolean,

    hideLocation: Triple<HideLocationType, String, String>?,
): List<CrossbarContextMenuItem> {
    val currentCat = state.currentCategoryOrNull()
    val inGamingCategory = currentCat?.isGamingCategory == true
    val inMissingBucket = state.selectedPlatformId == CrossbarViewModel.MISSING_PLATFORM_ID

    return buildList {
        add(CrossbarContextMenuItem("play", "Play", hidden = true))
        if (state.gameInfo == null) add(CrossbarContextMenuItem("game_info", "Game Info"))
        // the kit's Game Info has no buttons, so its extras live in Options (owner, 2026-10-04)
        state.gameInfo?.takeIf { it.item.gameId == item.gameId }?.let { info ->
            val offered = gameInfoActions(info)
            if (GameInfoAction.INFO in offered) add(CrossbarContextMenuItem("info_about", "Info"))
            if (GameInfoAction.VIDEO in offered) add(CrossbarContextMenuItem("info_video", "Video"))
            if (GameInfoAction.MANUAL in offered) add(CrossbarContextMenuItem("info_manual", "Manual"))
        }

        if (discCount > 1) add(CrossbarContextMenuItem("choose_disc", "Choose Disc"))
        if (item.platformId == PlatformIds.WINDOWS) {
            add(CrossbarContextMenuItem("export_game", "Export Game", group = MenuGroup.SETTINGS))
        }

        add(CrossbarContextMenuItem("shelves", "Shelves", group = MenuGroup.LIBRARY, pinnedToRoot = true))
        // owner, 2026-10-08: filter the game lists by this game's genre, or clear the filter
        genreFilterRow(item.genre, state.genreFilter)?.let(::add)
        // owner, 2026-10-08: the drawer's Games buttons switch between systems and genres from here
        if (state.activeAppDrawerFilter != null) add(groupingRow(state.gameGrouping))
        pinKey(item)?.let { add(recentPinRow(it in state.recentPins)) }
        if (onRecentShelf) add(CrossbarContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

        if (inGamingCategory) {
            val hasOtherCustomCategory = state.categories.any {
                it.isGamingCategory && it.id != BuiltInCategory.GAMES && it.id != currentCat.id
            }

            if (currentCat.id == BuiltInCategory.GAMES) {
                if (hasOtherCustomCategory) add(CrossbarContextMenuItem("add_category", "Add to Category", group = MenuGroup.CATEGORY))
            } else {
                if (hasOtherCustomCategory) add(CrossbarContextMenuItem("move_category", "Move to Category", group = MenuGroup.CATEGORY))
                add(CrossbarContextMenuItem("remove_category", "Remove from Category", group = MenuGroup.CATEGORY))
                val pinned = item.pinnedInCategory
                add(
                    CrossbarContextMenuItem(
                        if (pinned) "unpin_category" else "pin_category",
                        if (pinned) "Unpin" else "Pin",
                        group = MenuGroup.CATEGORY,
                    ),
                )
            }
        }

        add(CrossbarContextMenuItem("detail_title", "Edit Title", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("detail_note", "Edit Note", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("edit_genre", "Edit Genre", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("detail_ARTWORK", "Artwork", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("detail_METADATA", "Update Metadata", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("detail_MANUAL", "Manual", group = MenuGroup.METADATA))
        add(CrossbarContextMenuItem("detail_REFRESH", "Refresh Artwork", group = MenuGroup.METADATA))

        if (!item.isAndroidApp) add(CrossbarContextMenuItem("change_emulator", "Change Emulator", group = MenuGroup.SETTINGS))
        add(CrossbarContextMenuItem("file_location", "View File Location", group = MenuGroup.SETTINGS))

        hideLocation?.let { (_, _, label) ->
            add(CrossbarContextMenuItem("hide_here", "Hide from $label", group = MenuGroup.REMOVE))
        }
        if (inMissingBucket) {
            add(CrossbarContextMenuItem("remove_missing", "Remove permanently", isDestructive = true, group = MenuGroup.REMOVE))
        } else if (item.platformId == PlatformIds.ANDROID && item.packageName != null) {
            add(CrossbarContextMenuItem("app_info", "App Info", group = MenuGroup.SETTINGS))
            add(CrossbarContextMenuItem("unmark_game", "Unmark as Game", group = MenuGroup.REMOVE))
            add(CrossbarContextMenuItem("remove_app", "Remove from Library", isDestructive = true, group = MenuGroup.REMOVE))
            add(CrossbarContextMenuItem("uninstall", "Uninstall", isDestructive = true, group = MenuGroup.REMOVE))
        } else {
            add(CrossbarContextMenuItem("remove_game", "Remove from Library", isDestructive = true, group = MenuGroup.REMOVE))
        }
    }
}

// owner, 2026-10-06: a game or app pins to, or unpins from, the list under Recent
internal fun recentPinRow(pinned: Boolean): CrossbarContextMenuItem =
    if (pinned) CrossbarContextMenuItem("unpin_recent", "Unpin from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true)
    else CrossbarContextMenuItem("pin_recent", "Pin to Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true)

internal fun appContextMenuItems(
    state: CrossbarUiState,
    categoryId: String?,
    onRecentShelf: Boolean,
    packageName: String? = null,
): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem("launch", "Launch"))

    add(CrossbarContextMenuItem("mark_game", "Mark as Game", group = MenuGroup.LIBRARY))
    add(CrossbarContextMenuItem("favorite", "Add to Favorites", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    packageName?.let { add(recentPinRow("a:$it" in state.recentPins)) }
    if (onRecentShelf) {
        // Remove clears it until it is used again; Hide keeps it off the panel until unhidden (owner, 2026-10-05)
        add(CrossbarContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))
        add(CrossbarContextMenuItem("hide_from_recent", "Hide from Recent", group = MenuGroup.REMOVE))
    }

    // owner, 2026-10-08: art through Artwork Studio, as a game's; the old app editor changed little but the title
    add(CrossbarContextMenuItem("app_artwork", "Artwork", group = MenuGroup.METADATA))
    add(CrossbarContextMenuItem("rename", "Rename Shortcut", group = MenuGroup.SETTINGS))
    if (packageName != null) add(CrossbarContextMenuItem("app_info", "App Info", group = MenuGroup.SETTINGS))

    add(CrossbarContextMenuItem("move", "Move to Category", group = MenuGroup.CATEGORY))
    add(CrossbarContextMenuItem("add", "Add to Category", group = MenuGroup.CATEGORY))
    if (categoryId != null) {
        add(CrossbarContextMenuItem("remove", "Remove from Category", group = MenuGroup.CATEGORY))
        add(CrossbarContextMenuItem("pin", "Pin to Category", group = MenuGroup.CATEGORY))

        if (!onRecentShelf) {
            add(CrossbarContextMenuItem("hide_from_category", "Hide from ${state.categoryDisplayNameOf(categoryId)}", group = MenuGroup.CATEGORY))
        }
    }
    add(CrossbarContextMenuItem("hide_everywhere", "Hide Everywhere", group = MenuGroup.REMOVE))
    if (packageName != null) add(CrossbarContextMenuItem("uninstall", "Uninstall", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun videoFileContextMenuItems(
    isFavorite: Boolean,
    resumePositionMs: Long,
    hasWatchStamp: Boolean,
    inPlaylist: Boolean,
): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem("video_play", "Play"))
    if (resumePositionMs > 0) add(CrossbarContextMenuItem("video_resume", "Resume"))
    add(CrossbarContextMenuItem("video_details", "Details"))
    // owner, 2026-10-08: a video can be the wallpaper; MotionWallpaper decides whether it fits its limits
    add(CrossbarContextMenuItem("video_wallpaper", "Set as Wallpaper"))

    add(CrossbarContextMenuItem("video_favorite", if (isFavorite) "Remove from Favorites" else "Add to Favorites", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    add(CrossbarContextMenuItem("video_add_playlist", "Add to Playlist", group = MenuGroup.LIBRARY))
    if (hasWatchStamp) add(CrossbarContextMenuItem("video_remove_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

    if (inPlaylist) {
        add(CrossbarContextMenuItem("video_remove_playlist", "Remove from this Playlist", isDestructive = true, confirms = false, group = MenuGroup.REMOVE))
    }
    add(CrossbarContextMenuItem("video_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun videoLibraryContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("video_lib_open", "Open"),
    CrossbarContextMenuItem("video_lib_manage", "Manage Folders", group = MenuGroup.SETTINGS),
)

internal fun videoPlaylistContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("open_video_playlist", "Open"),
    CrossbarContextMenuItem("rename_video_playlist", "Rename Playlist", group = MenuGroup.SETTINGS),
    CrossbarContextMenuItem("delete_video_playlist", "Delete Playlist", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun photoFileContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("photo_open", "Open"),
    CrossbarContextMenuItem("photo_set_wallpaper", "Set as Launcher Wallpaper", group = MenuGroup.SETTINGS),
    CrossbarContextMenuItem("photo_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun photoLibraryContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("photo_lib_open", "Open"),
    CrossbarContextMenuItem("photo_lib_scan", "Scan Album", group = MenuGroup.SETTINGS),
    CrossbarContextMenuItem("photo_lib_manage", "Manage Folders", group = MenuGroup.SETTINGS),
)

internal fun bookContextMenuItems(hasOpenStamp: Boolean): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem("book_open", "Read"))
    if (hasOpenStamp) add(CrossbarContextMenuItem("book_remove_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    add(CrossbarContextMenuItem("book_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun recentAlbumContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("open_album", "Open Album"),
    CrossbarContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true),
)

// every recent track of the album, not only the run the row was folded from
internal fun recentAlbumTrackIds(recentTracks: List<com.echo.core.domain.model.MusicTrack>, key: String): List<String> =
    recentTracks.filter { it.lastPlayedAt != null && it.album.musicGroupKey() == key }.map { it.id }

internal fun musicTrackContextMenuItems(
    playlistId: Long?,
    hasPlayStamp: Boolean,
): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem("play", "Play"))
    add(CrossbarContextMenuItem("play_background", "Play in Background"))

    add(CrossbarContextMenuItem("add_to_playlist", "Add to Playlist", group = MenuGroup.LIBRARY))
    if (hasPlayStamp) add(CrossbarContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

    if (playlistId != null) {
        add(CrossbarContextMenuItem("remove_from_playlist", "Remove from this Playlist", isDestructive = true, confirms = false, group = MenuGroup.REMOVE))
    }
    add(CrossbarContextMenuItem("remove_track", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun playlistRowContextMenuItems(): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("open_playlist", "Open"),
    CrossbarContextMenuItem("add_tracks", "Add Tracks", group = MenuGroup.LIBRARY),
    CrossbarContextMenuItem("rename_playlist", "Rename Playlist", group = MenuGroup.SETTINGS),
    CrossbarContextMenuItem("delete_playlist", "Delete Playlist", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun musicPlayerMenuItems(playback: com.echo.feature.crossbar.music.MusicPlaybackState): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("music_shuffle", if (playback.shuffle) "Shuffle: On" else "Shuffle: Off"),
    CrossbarContextMenuItem("music_repeat", when (playback.repeat) {
        com.echo.feature.crossbar.music.RepeatMode.OFF -> "Repeat: Off"
        com.echo.feature.crossbar.music.RepeatMode.ALL -> "Repeat: All"
        com.echo.feature.crossbar.music.RepeatMode.ONE -> "Repeat: One"
    }),
    CrossbarContextMenuItem("music_background", "Play in Background"),
    CrossbarContextMenuItem("music_close", "Stop & Close"),
)

internal fun nowPlayingContextMenuItems(isPlaying: Boolean): List<CrossbarContextMenuItem> = listOf(
    CrossbarContextMenuItem("music_playpause", if (isPlaying) "Pause" else "Resume"),
    CrossbarContextMenuItem("music_close", "Stop and Close"),
)

internal fun platformContextMenuItems(
    platformId: String,
    pinned: Boolean,
    emulatorLabel: String? = null,
    overrideCount: Int = 0,
    romDirectory: String? = null,
): List<CrossbarContextMenuItem> = buildList {
    if (platformId == PlatformIds.ANDROID) add(CrossbarContextMenuItem("find_games", "Find Games"))
    else add(CrossbarContextMenuItem("scan_roms", "Scan This Console"))

    if (platformId == PlatformIds.WINDOWS) {
        add(CrossbarContextMenuItem("import_pc_games", "Import PC Games"))
    }
    add(CrossbarContextMenuItem("update_metadata", "Update Metadata", group = MenuGroup.SETTINGS))
    add(CrossbarContextMenuItem("scrape_missing_artwork", "Scrape Missing Artwork", group = MenuGroup.SETTINGS))

    if (emulatorLabel != null) {
        add(CrossbarContextMenuItem("default_emulator", "Default Emulator ($emulatorLabel)", group = MenuGroup.SETTINGS))
    }
    if (overrideCount > 0) {
        add(
            CrossbarContextMenuItem(
                "clear_emulator_overrides",
                "Clear $overrideCount Game Override(s)",
                isDestructive = true,
                group = MenuGroup.SETTINGS,
            ),
        )
    }

    add(CrossbarContextMenuItem("rename_card", "Rename System", group = MenuGroup.SETTINGS))
    if (romDirectory != null) {
        add(CrossbarContextMenuItem("card_rom_directory", "ROM Folder ($romDirectory)", group = MenuGroup.SETTINGS))
    }
    add(CrossbarContextMenuItem("library_manager", "Open in Library Manager", group = MenuGroup.SETTINGS))

    if (pinned) add(CrossbarContextMenuItem("unpin", "Unpin", group = MenuGroup.CATEGORY))
    else add(CrossbarContextMenuItem("pin", "Pin To Top", group = MenuGroup.CATEGORY))

    add(CrossbarContextMenuItem("hide", "Hide From Games", group = MenuGroup.REMOVE))

    if (platformId != PlatformIds.WINDOWS) {
        add(CrossbarContextMenuItem("remove", "Remove System", isDestructive = true, group = MenuGroup.REMOVE))
    }
}

internal fun allGamesContextMenuItems(grouping: GameGrouping = GameGrouping.SYSTEM): List<CrossbarContextMenuItem> = listOf(
    // owner, 2026-10-08: the Game column's folders by system or by genre
    if (grouping == GameGrouping.GENRE) CrossbarContextMenuItem("group_by_system", "Group by System")
    else CrossbarContextMenuItem("group_by_genre", "Group by Genre"),
    CrossbarContextMenuItem("import_pc_games", "Import PC Games"),
    CrossbarContextMenuItem("library_manager", "Manage Library", group = MenuGroup.SETTINGS),
)

internal fun groupingRow(grouping: GameGrouping): CrossbarContextMenuItem =
    if (grouping == GameGrouping.GENRE) CrossbarContextMenuItem("group_by_system", "Group by System", group = MenuGroup.LIBRARY, pinnedToRoot = true)
    else CrossbarContextMenuItem("group_by_genre", "Group by Genre", group = MenuGroup.LIBRARY, pinnedToRoot = true)

enum class GameGrouping {
    SYSTEM, GENRE;

    companion object {
        fun fromName(name: String?): GameGrouping = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

internal const val GENRE_ITEM_PREFIX = "genre_folder_"
internal fun genreItemId(genre: com.echo.core.domain.model.GameGenre) = GENRE_ITEM_PREFIX + genre.name
internal val genreItemIds: Set<String> = com.echo.core.domain.model.GameGenre.entries.map(::genreItemId).toSet()
internal fun genreOfItemId(id: String?): com.echo.core.domain.model.GameGenre? =
    id?.takeIf { it.startsWith(GENRE_ITEM_PREFIX) }?.let { com.echo.core.domain.model.GameGenre.fromName(it.removePrefix(GENRE_ITEM_PREFIX)) }

// a folder per genre that has games, in the list's order; a genre folder opens All Games narrowed to it
internal fun genreFolderRows(counts: Map<com.echo.core.domain.model.GameGenre, Int>, covers: Map<String, List<String>>): List<CrossbarItem> =
    com.echo.core.domain.model.GameGenre.entries.mapNotNull { genre ->
        counts[genre]?.takeIf { it > 0 }?.let { n ->
            CrossbarItem(
                id = genreItemId(genre),
                title = genre.label,
                subtitle = countLabel(n, "game", "games"),
                insideCovers = covers[genreItemId(genre)].orEmpty(),
                type = CrossbarItemType.ALL_GAMES,
            )
        }
    }

internal const val MEDIA_APP_PREFIX = "media_app_"

enum class MediaRootAction { RESCAN, RESCAN_DEEP, RELINK, REMOVE }

enum class MediaFoldersAction { ADD_ROOT, RESCAN_ALL, RESCAN_ALL_DEEP, DEFAULT_APP, CLEAR_CACHE, PICK_APP }

internal fun mediaRootActionOf(itemId: String): MediaRootAction? = when (itemId) {
    "media_root_rescan"      -> MediaRootAction.RESCAN
    "media_root_rescan_deep" -> MediaRootAction.RESCAN_DEEP
    "media_root_relink"      -> MediaRootAction.RELINK
    "media_root_remove"      -> MediaRootAction.REMOVE
    else -> null
}

internal fun mediaFoldersActionOf(itemId: String): MediaFoldersAction? = when {
    itemId == "media_add_root"           -> MediaFoldersAction.ADD_ROOT
    itemId == "media_rescan_all"         -> MediaFoldersAction.RESCAN_ALL
    itemId == "media_rescan_all_deep"    -> MediaFoldersAction.RESCAN_ALL_DEEP
    itemId == "media_default_app"        -> MediaFoldersAction.DEFAULT_APP
    itemId == "media_clear_cache"        -> MediaFoldersAction.CLEAR_CACHE
    itemId.startsWith(MEDIA_APP_PREFIX)  -> MediaFoldersAction.PICK_APP
    else -> null
}

internal fun mediaKindFolderWord(kind: MediaRootKind): String = when (kind) {
    MediaRootKind.MUSIC -> "music"
    MediaRootKind.VIDEO -> "video"
    MediaRootKind.PHOTO -> "photo"
    MediaRootKind.BOOK  -> "book"
}

internal fun mediaKindLabel(kind: MediaRootKind): String = when (kind) {
    MediaRootKind.MUSIC -> "Music"
    MediaRootKind.VIDEO -> "Video"
    MediaRootKind.PHOTO -> "Photo"
    MediaRootKind.BOOK  -> "Books"
}

internal fun mediaRootContextMenuItems(linked: Boolean, kind: MediaRootKind): List<CrossbarContextMenuItem> = buildList {
    if (linked) add(CrossbarContextMenuItem("media_root_rescan", "Rescan This Folder"))
    if (linked && kind == MediaRootKind.BOOK) {
        add(CrossbarContextMenuItem("media_root_rescan_deep", "Deep Rescan", group = MenuGroup.SETTINGS))
    }
    add(CrossbarContextMenuItem("media_root_relink", "Relink Folder", group = MenuGroup.SETTINGS))
    add(
        CrossbarContextMenuItem(
            "media_root_remove", "Remove Folder",
            isDestructive = true, group = MenuGroup.REMOVE,
        ),
    )
}

internal fun mediaFoldersContextMenuItems(kind: MediaRootKind): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem("media_add_root", "Add Folder"))
    add(CrossbarContextMenuItem("media_rescan_all", "Rescan ${mediaKindLabel(kind)} Library", group = MenuGroup.LIBRARY))
    if (kind == MediaRootKind.BOOK) {
        add(CrossbarContextMenuItem("media_rescan_all_deep", "Deep Rescan", group = MenuGroup.LIBRARY))
    }
    when (kind) {
        MediaRootKind.MUSIC -> add(CrossbarContextMenuItem("media_default_app", "Default Music Player", group = MenuGroup.SETTINGS))
        MediaRootKind.VIDEO -> add(CrossbarContextMenuItem("media_default_app", "Default Video Player", group = MenuGroup.SETTINGS))
        MediaRootKind.BOOK  -> add(CrossbarContextMenuItem("media_default_app", "Default Reader", group = MenuGroup.SETTINGS))
        MediaRootKind.PHOTO -> add(CrossbarContextMenuItem("media_default_app", "Default Photo Viewer", group = MenuGroup.SETTINGS))
    }
    when (kind) {
        MediaRootKind.PHOTO -> add(CrossbarContextMenuItem("media_clear_cache", "Clear Thumbnail Cache", group = MenuGroup.SETTINGS))
        MediaRootKind.BOOK  -> add(CrossbarContextMenuItem("media_clear_cache", "Clear Cover Cache", group = MenuGroup.SETTINGS))
        else -> Unit
    }
}

internal fun genreFilterRow(itemGenre: com.echo.core.domain.model.GameGenre?, active: com.echo.core.domain.model.GameGenre?): CrossbarContextMenuItem? = when {
    active != null -> CrossbarContextMenuItem("genre_all", "Show All Genres", group = MenuGroup.LIBRARY, pinnedToRoot = true)
    itemGenre != null -> CrossbarContextMenuItem("genre_only", "Show Only ${itemGenre.label}", group = MenuGroup.LIBRARY, pinnedToRoot = true)
    else -> null
}

// a filtered game list keeps its other rows (headers, Add Games); a game shows when it is the genre
internal fun List<CrossbarItem>.withGenre(genre: com.echo.core.domain.model.GameGenre?): List<CrossbarItem> =
    if (genre == null) this else filter { it.gameId == null || it.genre == genre }

// a game's row: the column's own when the column holds it, else built from the library (a game opened from the
// App Drawer is seldom in the column)
internal suspend fun gameRowFor(gameId: Long, column: List<CrossbarItem>, fromLibrary: suspend () -> CrossbarItem?): CrossbarItem? =
    column.firstOrNull { it.gameId == gameId } ?: fromLibrary()
