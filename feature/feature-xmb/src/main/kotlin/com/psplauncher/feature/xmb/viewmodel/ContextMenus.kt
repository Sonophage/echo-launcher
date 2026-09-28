package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.BuiltInCategory
import com.psplauncher.core.domain.model.Category
import com.psplauncher.core.domain.model.HideLocationType
import com.psplauncher.core.domain.model.PlatformIds
import com.psplauncher.core.data.repository.MediaRootKind
import com.psplauncher.core.ui.components.MenuGroup

internal fun XMBUiState.currentCategoryOrNull(): Category? =
    categories.getOrNull(selectedCategoryIndex)

internal fun XMBUiState.categoryDisplayNameOf(id: String): String = when (id) {
    XMBViewModel.MUSIC_APPS_CATEGORY_ID -> "Music Apps"
    XMBViewModel.VIDEO_APPS_CATEGORY_ID -> "Video Apps"
    else -> categories.firstOrNull { it.id == id }?.name ?: id
}

internal fun gameContextMenuItems(
    item: XMBItem,
    state: XMBUiState,
    discCount: Int,
    onRecentShelf: Boolean,

    hideLocation: Triple<HideLocationType, String, String>?,
): List<XMBContextMenuItem> {
    val currentCat = state.currentCategoryOrNull()
    val inGamingCategory = currentCat?.isGamingCategory == true
    val inMissingBucket = state.selectedPlatformId == XMBViewModel.MISSING_PLATFORM_ID

    return buildList {
        add(XMBContextMenuItem("play", "Play", hidden = true))

        if (discCount > 1) add(XMBContextMenuItem("choose_disc", "Choose Disc"))
        if (item.platformId == PlatformIds.WINDOWS) {
            add(XMBContextMenuItem("export_game", "Export Game", group = MenuGroup.SETTINGS))
        }

        add(XMBContextMenuItem("shelves", "Shelves", group = MenuGroup.LIBRARY, pinnedToRoot = true))
        if (onRecentShelf) add(XMBContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

        if (inGamingCategory) {
            val hasOtherCustomCategory = state.categories.any {
                it.isGamingCategory && it.id != BuiltInCategory.GAMES && it.id != currentCat.id
            }

            if (currentCat.id == BuiltInCategory.GAMES) {
                if (hasOtherCustomCategory) add(XMBContextMenuItem("add_category", "Add to Category", group = MenuGroup.CATEGORY))
            } else {
                if (hasOtherCustomCategory) add(XMBContextMenuItem("move_category", "Move to Category", group = MenuGroup.CATEGORY))
                add(XMBContextMenuItem("remove_category", "Remove from Category", group = MenuGroup.CATEGORY))
                val pinned = item.subtitle == "Pinned"
                add(
                    XMBContextMenuItem(
                        if (pinned) "unpin_category" else "pin_category",
                        if (pinned) "Unpin" else "Pin",
                        group = MenuGroup.CATEGORY,
                    ),
                )
            }
        }

        add(XMBContextMenuItem("detail_title", "Edit Title", group = MenuGroup.METADATA))
        add(XMBContextMenuItem("detail_note", "Edit Note", group = MenuGroup.METADATA))
        add(XMBContextMenuItem("detail_ARTWORK", "Artwork", group = MenuGroup.METADATA))
        add(XMBContextMenuItem("detail_METADATA", "Update Metadata", group = MenuGroup.METADATA))
        add(XMBContextMenuItem("detail_MANUAL", "Manual", group = MenuGroup.METADATA))
        add(XMBContextMenuItem("detail_REFRESH", "Refresh Artwork", group = MenuGroup.METADATA))

        if (!item.isAndroidApp) add(XMBContextMenuItem("change_emulator", "Change Emulator", group = MenuGroup.SETTINGS))
        add(XMBContextMenuItem("file_location", "View File Location", group = MenuGroup.SETTINGS))

        hideLocation?.let { (_, _, label) ->
            add(XMBContextMenuItem("hide_here", "Hide from $label", group = MenuGroup.REMOVE))
        }
        if (inMissingBucket) {
            add(XMBContextMenuItem("remove_missing", "Remove permanently", isDestructive = true, group = MenuGroup.REMOVE))
        } else if (item.platformId == PlatformIds.ANDROID && item.packageName != null) {
            add(XMBContextMenuItem("unmark_game", "Unmark as Game", group = MenuGroup.REMOVE))
            add(XMBContextMenuItem("remove_app", "Remove from Library", isDestructive = true, group = MenuGroup.REMOVE))
        } else {
            add(XMBContextMenuItem("remove_game", "Remove from Library", isDestructive = true, group = MenuGroup.REMOVE))
        }
    }
}

internal fun appContextMenuItems(
    state: XMBUiState,
    categoryId: String?,
    onRecentShelf: Boolean,
): List<XMBContextMenuItem> = buildList {
    add(XMBContextMenuItem("launch", "Launch"))

    add(XMBContextMenuItem("mark_game", "Mark as Game", group = MenuGroup.LIBRARY))
    add(XMBContextMenuItem("favorite", "Add to Favorites", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    if (onRecentShelf) add(XMBContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

    add(XMBContextMenuItem("edit_app", "Edit App Details", group = MenuGroup.SETTINGS))
    add(XMBContextMenuItem("rename", "Rename Shortcut", group = MenuGroup.SETTINGS))

    add(XMBContextMenuItem("move", "Move to Category", group = MenuGroup.CATEGORY))
    add(XMBContextMenuItem("add", "Add to Category", group = MenuGroup.CATEGORY))
    if (categoryId != null) {
        add(XMBContextMenuItem("remove", "Remove from Category", group = MenuGroup.CATEGORY))
        add(XMBContextMenuItem("pin", "Pin to Category", group = MenuGroup.CATEGORY))

        if (!onRecentShelf) {
            add(XMBContextMenuItem("hide_from_category", "Hide from ${state.categoryDisplayNameOf(categoryId)}", group = MenuGroup.CATEGORY))
        }
    }
    add(XMBContextMenuItem("hide_everywhere", "Hide Everywhere", group = MenuGroup.REMOVE))
}

internal fun videoFileContextMenuItems(
    isFavorite: Boolean,
    resumePositionMs: Long,
    hasWatchStamp: Boolean,
    inPlaylist: Boolean,
): List<XMBContextMenuItem> = buildList {
    add(XMBContextMenuItem("video_play", "Play"))
    if (resumePositionMs > 0) add(XMBContextMenuItem("video_resume", "Resume"))
    add(XMBContextMenuItem("video_details", "Details"))

    add(XMBContextMenuItem("video_favorite", if (isFavorite) "Remove from Favorites" else "Add to Favorites", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    add(XMBContextMenuItem("video_add_playlist", "Add to Playlist", group = MenuGroup.LIBRARY))
    if (hasWatchStamp) add(XMBContextMenuItem("video_remove_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

    if (inPlaylist) {
        add(XMBContextMenuItem("video_remove_playlist", "Remove from this Playlist", isDestructive = true, confirms = false, group = MenuGroup.REMOVE))
    }
    add(XMBContextMenuItem("video_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun videoLibraryContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("video_lib_open", "Open"),
    XMBContextMenuItem("video_lib_manage", "Manage Folders", group = MenuGroup.SETTINGS),
)

internal fun videoPlaylistContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("open_video_playlist", "Open"),
    XMBContextMenuItem("rename_video_playlist", "Rename Playlist", group = MenuGroup.SETTINGS),
    XMBContextMenuItem("delete_video_playlist", "Delete Playlist", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun photoFileContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("photo_open", "Open"),
    XMBContextMenuItem("photo_set_wallpaper", "Set as Launcher Wallpaper", group = MenuGroup.SETTINGS),
    XMBContextMenuItem("photo_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun photoLibraryContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("photo_lib_open", "Open"),
    XMBContextMenuItem("photo_lib_scan", "Scan Album", group = MenuGroup.SETTINGS),
    XMBContextMenuItem("photo_lib_manage", "Manage Folders", group = MenuGroup.SETTINGS),
)

internal fun bookContextMenuItems(hasOpenStamp: Boolean): List<XMBContextMenuItem> = buildList {
    add(XMBContextMenuItem("book_open", "Read"))
    if (hasOpenStamp) add(XMBContextMenuItem("book_remove_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))
    add(XMBContextMenuItem("book_remove", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun musicTrackContextMenuItems(
    playlistId: Long?,
    hasPlayStamp: Boolean,
): List<XMBContextMenuItem> = buildList {
    add(XMBContextMenuItem("play", "Play"))
    add(XMBContextMenuItem("play_background", "Play in Background"))

    add(XMBContextMenuItem("add_to_playlist", "Add to Playlist", group = MenuGroup.LIBRARY))
    if (hasPlayStamp) add(XMBContextMenuItem("remove_from_recent", "Remove from Recent", group = MenuGroup.LIBRARY, pinnedToRoot = true))

    if (playlistId != null) {
        add(XMBContextMenuItem("remove_from_playlist", "Remove from this Playlist", isDestructive = true, confirms = false, group = MenuGroup.REMOVE))
    }
    add(XMBContextMenuItem("remove_track", "Remove From Library", isDestructive = true, group = MenuGroup.REMOVE))
}

internal fun playlistRowContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("open_playlist", "Open"),
    XMBContextMenuItem("add_tracks", "Add Tracks", group = MenuGroup.LIBRARY),
    XMBContextMenuItem("rename_playlist", "Rename Playlist", group = MenuGroup.SETTINGS),
    XMBContextMenuItem("delete_playlist", "Delete Playlist", isDestructive = true, group = MenuGroup.REMOVE),
)

internal fun nowPlayingContextMenuItems(isPlaying: Boolean): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("music_playpause", if (isPlaying) "Pause" else "Resume"),
    XMBContextMenuItem("music_close", "Stop and Close"),
)

internal fun platformContextMenuItems(
    platformId: String,
    pinned: Boolean,
    emulatorLabel: String? = null,
    overrideCount: Int = 0,
    romDirectory: String? = null,
): List<XMBContextMenuItem> = buildList {
    if (platformId == PlatformIds.ANDROID) add(XMBContextMenuItem("find_games", "Find Games"))
    else add(XMBContextMenuItem("scan_roms", "Scan This Console"))

    if (platformId == PlatformIds.WINDOWS) {
        add(XMBContextMenuItem("import_pc_games", "Import PC Games"))
    }
    add(XMBContextMenuItem("update_metadata", "Update Metadata", group = MenuGroup.SETTINGS))
    add(XMBContextMenuItem("scrape_missing_artwork", "Scrape Missing Artwork", group = MenuGroup.SETTINGS))

    if (emulatorLabel != null) {
        add(XMBContextMenuItem("default_emulator", "Default Emulator ($emulatorLabel)", group = MenuGroup.SETTINGS))
    }
    if (overrideCount > 0) {
        add(
            XMBContextMenuItem(
                "clear_emulator_overrides",
                "Clear $overrideCount Game Override(s)",
                isDestructive = true,
                group = MenuGroup.SETTINGS,
            ),
        )
    }

    add(XMBContextMenuItem("rename_card", "Rename Memory Card", group = MenuGroup.SETTINGS))
    if (romDirectory != null) {
        add(XMBContextMenuItem("card_rom_directory", "ROM Folder ($romDirectory)", group = MenuGroup.SETTINGS))
    }
    add(XMBContextMenuItem("library_manager", "Open in Library Manager", group = MenuGroup.SETTINGS))

    if (pinned) add(XMBContextMenuItem("unpin", "Unpin", group = MenuGroup.CATEGORY))
    else add(XMBContextMenuItem("pin", "Pin To Top", group = MenuGroup.CATEGORY))
    add(XMBContextMenuItem("card_move_up", "Move Up", group = MenuGroup.CATEGORY))
    add(XMBContextMenuItem("card_move_down", "Move Down", group = MenuGroup.CATEGORY))

    add(XMBContextMenuItem("hide", "Hide From Games", group = MenuGroup.REMOVE))

    if (platformId != PlatformIds.WINDOWS) {
        add(XMBContextMenuItem("remove", "Remove Memory Card", isDestructive = true, group = MenuGroup.REMOVE))
    }
}

internal fun allGamesContextMenuItems(): List<XMBContextMenuItem> = listOf(
    XMBContextMenuItem("import_pc_games", "Import PC Games"),
    XMBContextMenuItem("library_manager", "Manage Library", group = MenuGroup.SETTINGS),
)

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

internal fun mediaRootContextMenuItems(linked: Boolean, kind: MediaRootKind): List<XMBContextMenuItem> = buildList {
    if (linked) add(XMBContextMenuItem("media_root_rescan", "Rescan This Folder"))
    if (linked && kind == MediaRootKind.BOOK) {
        add(XMBContextMenuItem("media_root_rescan_deep", "Deep Rescan", group = MenuGroup.SETTINGS))
    }
    add(XMBContextMenuItem("media_root_relink", "Relink Folder", group = MenuGroup.SETTINGS))
    add(
        XMBContextMenuItem(
            "media_root_remove", "Remove Folder",
            isDestructive = true, group = MenuGroup.REMOVE,
        ),
    )
}

internal fun mediaFoldersContextMenuItems(kind: MediaRootKind): List<XMBContextMenuItem> = buildList {
    add(XMBContextMenuItem("media_add_root", "Add Folder"))
    add(XMBContextMenuItem("media_rescan_all", "Rescan ${mediaKindLabel(kind)} Library", group = MenuGroup.LIBRARY))
    if (kind == MediaRootKind.BOOK) {
        add(XMBContextMenuItem("media_rescan_all_deep", "Deep Rescan", group = MenuGroup.LIBRARY))
    }
    when (kind) {
        MediaRootKind.MUSIC -> add(XMBContextMenuItem("media_default_app", "Default Music Player", group = MenuGroup.SETTINGS))
        MediaRootKind.VIDEO -> add(XMBContextMenuItem("media_default_app", "Default Video Player", group = MenuGroup.SETTINGS))
        MediaRootKind.BOOK  -> add(XMBContextMenuItem("media_default_app", "Default Reader", group = MenuGroup.SETTINGS))
        MediaRootKind.PHOTO -> Unit
    }
    when (kind) {
        MediaRootKind.PHOTO -> add(XMBContextMenuItem("media_clear_cache", "Clear Thumbnail Cache", group = MenuGroup.SETTINGS))
        MediaRootKind.BOOK  -> add(XMBContextMenuItem("media_clear_cache", "Clear Cover Cache", group = MenuGroup.SETTINGS))
        else -> Unit
    }
}
