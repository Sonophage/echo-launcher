package com.echo.feature.crossbar.viewmodel

import com.echo.core.data.repository.MediaRootKind
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarVideo(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val videoRepository: com.echo.core.domain.repository.VideoRepository,
    private val videoScanner: com.echo.feature.library.scanner.VideoScanner,
    private val videoIntentResolver: com.echo.core.data.video.VideoIntentResolver,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun observeVideo() {
        scope.launch {
            videoRepository.observeRecentlyWatched().collect { videos ->
                val resumable = videos.firstOrNull { v ->
                    val total = v.durationMs ?: 0L
                    total > 0L && v.resumePositionMs > 0L &&
                        v.resumePositionMs.toFloat() / total < CrossbarViewModel.RESUME_DONE_FRACTION
                }
                if (uiState.value.resumeVideo?.id != resumable?.id) {
                    uiState.update { it.copy(resumeVideo = resumable) }
                    if (vm.currentCategory()?.id == BuiltInCategory.VIDEO &&
                        uiState.value.videoNav == VideoNav.Root
                    ) {
                        vm.loadItemsForCategory(vm.currentCategory(), keepCursorOnRow = true)
                    }
                }
            }
        }
        scope.launch {
            videoRepository.observeLibraries().collect { libraries ->
                uiState.update { it.copy(videoLibraries = libraries) }
                if (vm.currentCategory()?.id == BuiltInCategory.VIDEO &&
                    uiState.value.videoNav == VideoNav.Root
                ) {
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
        }
    }

    internal fun videoAddActions(): List<CrossbarItem> = buildList {
        if (uiState.value.videoLibraries.none { it.lastScannedAt != null }) add(addVideosItem())
        add(addVideoAppsItem())
    }

    internal suspend fun videoRootItems(): List<CrossbarItem> =
        vm.libraryColumn(
            mediaColumn(uiState.value.videoRootSections(), vm.videoAppItems(), videoAddActions()),
            SearchScope.VIDEOS,
        )

    private fun addVideosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_VIDEOS_ITEM_ID,
        title    = "Add Videos",
        subtitle = "Set your Video root folder in Settings to get started",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun videoCollectionsItems(): List<CrossbarItem> = listOf(
        CrossbarItem(
            id       = CrossbarViewModel.RECENTLY_WATCHED_ITEM_ID,
            title    = "Recently Watched",
            subtitle = "Pick up where you left off",
            type     = CrossbarItemType.VIDEO_RECENT,
        ),
        CrossbarItem(
            id       = CrossbarViewModel.FAVORITE_VIDEOS_ITEM_ID,
            title    = "Favorites",
            subtitle = "Your starred videos",
            type     = CrossbarItemType.VIDEO_FAVORITES,
        ),
        CrossbarItem(
            id       = CrossbarViewModel.VIDEO_PLAYLISTS_ITEM_ID,
            title    = "Playlists",
            subtitle = "Build and play your own lists",
            type     = CrossbarItemType.PLAYLIST,
        ),
    )

    internal fun videoLibraryItems(libraries: List<com.echo.core.domain.model.VideoLibrary>): List<CrossbarItem> {
        val rows = libraries.map { lib ->
            CrossbarItem(
                id       = "vlib_${lib.id}",
                title    = lib.displayName,
                subtitle = countLabel(lib.videoCount, "video", "videos"),
                coverUri = lib.artworkUri,
                type     = CrossbarItemType.VIDEO_FOLDER,
            )
        }
        return rows.ifEmpty {
            listOf(
                CrossbarItem(
                    id = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
                    title = "No video libraries yet",
                    subtitle = "Add a folder from the Folders row",
                    type = CrossbarItemType.EMPTY,
                ),
            )
        }
    }

    private fun addVideoAppsItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_VIDEO_APPS_ITEM_ID,
        title    = "Add Video Apps",
        subtitle = "Pick installed apps to show here",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun setVideoItems(
        videos: List<com.echo.core.domain.model.Video>,
        emptyItem: CrossbarItem,
        sortable: Boolean = true,
    ) {
        val ordered = if (sortable) videos.videoSorted(uiState.value.videoSortMode) else videos
        val items = if (ordered.isEmpty()) listOf(emptyItem) else ordered.toVideoItems()
        uiState.update { it.copy(currentItems = items) }
    }

    internal fun videoPlaylistItems(playlists: List<com.echo.core.domain.model.VideoPlaylist>): List<CrossbarItem> {
        val rows = playlists.map { pl ->
            CrossbarItem(
                id         = "vpl_${pl.id}",
                title      = pl.name,
                subtitle   = countLabel(pl.videoCount, "video", "videos"),
                playlistId = pl.id,
                type       = CrossbarItemType.PLAYLIST,
            )
        }
        return rows + CrossbarItem(
            id       = CrossbarViewModel.CREATE_VIDEO_PLAYLIST_ITEM_ID,
            title    = "Create Playlist",
            subtitle = "Start a new video playlist",
            type     = CrossbarItemType.ADD_ACTION,
        )
    }

    internal fun emptyAllVideosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No videos found",
        subtitle = "Add a video folder from the Folders row",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun emptyFavoriteVideosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No favorites yet",
        subtitle = "Star a video from its ⚙ Options menu",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun emptyPlaylistVideosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_PLAYLIST_ITEM_ID,
        title    = "This playlist is empty",
        subtitle = "Add videos from a video's ⚙ Options menu",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun handleVideoSelection(item: CrossbarItem): Boolean = when {
        item.id == CrossbarViewModel.SEARCH_ITEM_ID -> { vm.librarySearch.openSearch(SearchScope.VIDEOS); true }
        item.id == CrossbarViewModel.ADD_MENU_ITEM_ID -> { menuSound.play(MenuSound.SELECT); vm.openAddMenu(); true }
        item.type == CrossbarItemType.EMPTY -> true
        item.id == CrossbarViewModel.ALL_VIDEOS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.AllVideos); true }
        item.id == CrossbarViewModel.VIDEO_COLLECTIONS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.Collections); true }
        item.id == CrossbarViewModel.RECENTLY_WATCHED_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.RecentlyWatched); true }
        item.id == CrossbarViewModel.FAVORITE_VIDEOS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.Favorites); true }
        item.id == CrossbarViewModel.VIDEO_PLAYLISTS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.Playlists); true }
        item.id == CrossbarViewModel.CREATE_VIDEO_PLAYLIST_ITEM_ID -> { menuSound.play(MenuSound.SELECT); promptCreateVideoPlaylist(); true }
        item.id.startsWith("vpl_") && item.playlistId != null -> {
            menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.Playlist(item.playlistId, item.title)); true
        }
        item.id == CrossbarViewModel.VIDEO_LIBRARIES_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openVideoView(VideoNav.Libraries); true }
        item.id == CrossbarViewModel.ADD_VIDEOS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.openMediaFolders(MediaRootKind.VIDEO)
            true
        }
        item.id == CrossbarViewModel.ADD_VIDEO_APPS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.openAppPicker(AppPickerTarget.CategoryShortcuts(CrossbarViewModel.VIDEO_APPS_CATEGORY_ID), "Add Video Apps")
            true
        }
        item.id.startsWith("vlib_") -> {
            menuSound.play(MenuSound.SELECT)
            val libId = item.id.removePrefix("vlib_")
            openVideoView(VideoNav.Library(libId, item.title))
            true
        }
        item.type == CrossbarItemType.VIDEO_FILE -> {
            menuSound.play(MenuSound.SELECT)
            uiState.update { it.copy(activeVideoId = item.id.removePrefix("vid_")) }
            true
        }

        item.packageName != null -> {
            menuSound.play(MenuSound.LAUNCH)
            vm.launchAppWithDisc(item.packageName, item.shelfCoverArt)
            true
        }
        else -> false
    }

    internal fun videoNavKey(nav: VideoNav): String = when (nav) {
        VideoNav.Folders         -> "folders"
        VideoNav.Root            -> "root"
        VideoNav.AllVideos       -> "all"
        VideoNav.Collections     -> "collections"
        VideoNav.RecentlyWatched -> "recent"
        VideoNav.Favorites       -> "favorites"
        VideoNav.Playlists       -> "playlists"
        is VideoNav.Playlist     -> "playlist_${nav.id}"
        VideoNav.Libraries       -> "libraries"
        is VideoNav.Library      -> "library_${nav.id}"
    }

    internal fun openVideoView(nav: VideoNav) = vm.navigateRememberingCursor { it.copy(videoNav = nav) }

    internal fun closeVideoView() = openVideoView(VideoNav.Root)

    fun onCloseVideoDetail() {
        uiState.update { it.copy(activeVideoId = null, activeVideoAutoPlay = false, pendingVideoDetailAction = null) }
    }

    fun consumeVideoDetailAction() {
        uiState.update { it.copy(pendingVideoDetailAction = null) }
    }

    internal fun promptCreateVideoPlaylist(forVideoId: String? = null) {
        uiState.update { it.copy(
            playlistNameDialog = PlaylistNameDialogState(title = "New Video Playlist", videoContext = true, forVideoId = forVideoId)
        )}
    }

    private fun promptRenameVideoPlaylist(playlistId: Long) {
        val name = uiState.value.currentItems.firstOrNull { it.playlistId == playlistId }?.title.orEmpty()
        uiState.update { it.copy(
            playlistNameDialog = PlaylistNameDialogState(
                title = "Rename Playlist",
                initialText = name,
                renamePlaylistId = playlistId,
                videoContext = true,
            )
        )}
    }

    private fun openVideoPlaylistContextMenu(playlistId: Long, name: String) {
        val items = videoPlaylistContextMenuItems()
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = name, rows = items), videoPlaylistId = playlistId)) }
    }

    internal fun openVideoContextMenu(item: CrossbarItem): Boolean {
        if (item.menuHostCategory(vm.currentCategory()?.id) != BuiltInCategory.VIDEO) return false
        return when {
            item.type == CrossbarItemType.VIDEO_FILE && item.id.startsWith("vid_") -> {
                openVideoFileContextMenu(item.id.removePrefix("vid_"), item.title); true
            }
            item.type == CrossbarItemType.VIDEO_FOLDER && item.id.startsWith("vlib_") -> {
                openVideoLibraryContextMenu(item.id.removePrefix("vlib_"), item.title); true
            }
            item.type == CrossbarItemType.PLAYLIST && item.playlistId != null -> {
                openVideoPlaylistContextMenu(item.playlistId, item.title); true
            }
            item.packageName != null -> {
                vm.openAppContextMenu(item, categoryIdOverride = CrossbarViewModel.VIDEO_APPS_CATEGORY_ID); true
            }
            else -> false
        }
    }

    private fun openVideoFileContextMenu(videoId: String, title: String) {
        scope.launch {
            val video = videoRepository.getVideo(videoId) ?: return@launch
            val inPlaylist = uiState.value.videoNav is VideoNav.Playlist
            val items = videoFileContextMenuItems(
                isFavorite = video.isFavorite,
                resumePositionMs = video.resumePositionMs,
                hasWatchStamp = video.lastWatchedAt != null,
                inPlaylist = inPlaylist,
            )
            uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = title, rows = items), videoFileId = videoId)) }
        }
    }

    internal fun handleVideoFileAction(videoId: String, itemId: String) {
        when (itemId) {
            "video_play", "video_resume", "video_details" ->
                uiState.update { it.copy(activeVideoId = videoId) }
            "video_favorite" -> vm.appAction {
                val v = videoRepository.getVideo(videoId) ?: return@appAction
                videoRepository.setFavorite(videoId, !v.isFavorite)
            }
            "video_add_playlist" -> openVideoPlaylistPicker(videoId)
            "video_remove_playlist" -> (uiState.value.videoNav as? VideoNav.Playlist)?.let { nav ->
                vm.appAction { videoRepository.removeVideoFromPlaylist(nav.id, videoId) }
            }

            "video_remove_recent" -> vm.appAction { videoRepository.clearLastWatched(videoId) }
            "video_remove" -> vm.appAction { videoRepository.removeVideo(videoId) }
        }
    }

    internal fun openVideoPlaylistPicker(videoId: String, selectIndex: Int? = 0) {
        scope.launch {
            val playlists = videoRepository.observePlaylists().first()
            val memberOf = videoRepository.getPlaylistIdsForVideo(videoId).toSet()
            val items = buildList {
                playlists.forEach { pl -> add(CrossbarContextMenuItem("vpl_${pl.id}", pl.name, checked = pl.id in memberOf)) }
                add(CrossbarContextMenuItem("vpl_new", "Create New Playlist"))
            }
            uiState.update { it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Add to Playlist", rows = items, selectedIndex = selectIndex?.coerceIn(0, items.lastIndex.coerceAtLeast(0))), videoPlaylistPickerVideoId = videoId)
            )}
        }
    }

    private fun openVideoLibraryContextMenu(libraryId: String, name: String) {
        val items = videoLibraryContextMenuItems()
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = name, rows = items), videoLibraryId = libraryId)) }
    }

    internal fun handleVideoLibraryAction(libraryId: String, itemId: String) {
        when (itemId) {
            "video_lib_open" -> {
                val name = uiState.value.currentItems.firstOrNull { it.id == "vlib_$libraryId" }?.title.orEmpty()
                openVideoView(VideoNav.Library(libraryId, name))
            }
            "video_lib_manage" -> vm.openMediaFolders(MediaRootKind.VIDEO)
        }
    }

    internal fun handleVideoPlaylistRowAction(playlistId: Long, itemId: String) {
        when (itemId) {
            "open_video_playlist" -> {
                val name = uiState.value.currentItems.firstOrNull { it.playlistId == playlistId }?.title.orEmpty()
                openVideoView(VideoNav.Playlist(playlistId, name))
            }
            "rename_video_playlist" -> promptRenameVideoPlaylist(playlistId)
            "delete_video_playlist" -> vm.appAction {
                videoRepository.deletePlaylist(playlistId)
                if ((uiState.value.videoNav as? VideoNav.Playlist)?.id == playlistId) openVideoView(VideoNav.Playlists)
            }
        }
    }

    internal suspend fun scanVideoLibrary(libraryId: String, deep: Boolean = false) {
        val library = videoRepository.getLibrary(libraryId) ?: return
        val taskId = "video_scan_$libraryId"
        vm.addBackgroundTask(BackgroundTaskInfo(taskId, "Scanning ${library.displayName}", null))
        val existing = videoRepository.getVideosForLibrary(libraryId)
        videoScanner.scan(library, deep = deep, existing = existing).collect { result ->
            when (result) {
                is com.echo.feature.library.scanner.VideoScanResult.Progress -> Unit
                is com.echo.feature.library.scanner.VideoScanResult.Complete -> {
                    videoRepository.replaceVideosForLibrary(result.libraryId, result.videos, System.currentTimeMillis())
                    vm.completeBackgroundTask(taskId, "${result.videos.size} videos")
                }
                is com.echo.feature.library.scanner.VideoScanResult.Error ->
                    vm.failBackgroundTask(taskId, result.message)
            }
        }
    }

    internal fun videoLibrarySiblings(): List<CrossbarItem> =
        uiState.value.videoLibraries.map { CrossbarItem(id = "vlib_${it.id}", title = it.displayName, type = CrossbarItemType.VIDEO_FOLDER) }

    internal fun videoPlaylistSiblings(): List<CrossbarItem> =
        uiState.value.videoPlaylists.map { CrossbarItem(id = "vpl_${it.id}", title = it.name, playlistId = it.id, type = CrossbarItemType.PLAYLIST) }
}

internal fun List<com.echo.core.domain.model.Video>.toVideoItems(): List<CrossbarItem> =
    map { it.toCrossbarRow() }
