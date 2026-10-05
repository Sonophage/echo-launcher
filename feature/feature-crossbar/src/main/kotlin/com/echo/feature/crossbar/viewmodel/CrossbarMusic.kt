package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.MusicTrack
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarMusic(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val musicRepository: com.echo.core.domain.repository.MusicRepository,
    private val musicPlayer: com.echo.feature.crossbar.music.MusicPlayerController,
    private val musicScanner: com.echo.feature.library.scanner.MusicScanner,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
    private val artworkAccent: com.echo.core.data.repository.ArtworkAccent,
) {
    internal var currentMusicTracks: List<MusicTrack> = emptyList()
    internal var currentMusicTracksRaw: List<MusicTrack> = emptyList()

    private var lastHadPlayingTrack = false

    @Volatile
    private var defaultMusicPlayer: String? = null

    private var musicBrowserJob: Job? = null
    private var browserRawTracks: List<MusicTrack> = emptyList()
    private var browserRawPlaylists: List<com.echo.core.domain.model.Playlist> = emptyList()
    internal fun observeMusic() {
        scope.launch {
            musicRepository.observeFolders().collect { folders ->
                uiState.update { it.copy(musicFolders = folders) }
                if (vm.currentCategory()?.id == BuiltInCategory.MUSIC &&
                    uiState.value.musicNav == MusicNav.Root
                ) {
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
        }
        scope.launch {
            musicRepository.observeDefaultPlayerPackage().collect { defaultMusicPlayer = it }
        }
        scope.launch {
            musicPlayer.state
                .map { it.track?.artUri }
                .distinctUntilChanged()
                .collectLatest { art ->
                    val accent = art?.let { artworkAccent.of(it) }
                    uiState.update { it.copy(musicAccentArgb = accent) }
                }
        }
        scope.launch {
            musicPlayer.state.collect { playback ->
                uiState.update { it.copy(musicPlayback = playback) }

                val hasTrack = playback.track != null
                if (hasTrack != lastHadPlayingTrack) {
                    lastHadPlayingTrack = hasTrack
                    if (vm.currentCategory()?.id == BuiltInCategory.MUSIC &&
                        uiState.value.musicNav == MusicNav.Root
                    ) {
                        refreshMusicRootPreservingCursor()
                    }
                }
            }
        }
    }

    private suspend fun refreshMusicRootPreservingCursor() {
        val s = uiState.value
        val selectedId = s.currentItems.getOrNull(s.selectedItemIndex)?.id
        clearMusicTrackCache()
        val items = musicRootItems()
        val restored = selectedId
            ?.let { id -> items.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: s.selectedItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        uiState.update { it.copy(currentItems = items, selectedItemIndex = restored) }
    }

    internal fun musicAddActions(): List<CrossbarItem> = buildList {
        if (uiState.value.musicFolders.none { it.lastScannedAt != null }) add(addMusicFolderItem())
        add(addMusicAppsItem())
    }

    internal suspend fun musicRootItems(): List<CrossbarItem> =
        vm.mediaRootColumn(uiState.value.musicFolders.isNotEmpty(), uiState.value.musicRootSections(), vm.musicAppItems(), musicAddActions(), SearchScope.MUSIC)

    private fun addMusicFolderItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_MUSIC_FOLDER_ITEM_ID,
        title    = "Add Music Folder",
        subtitle = "Add a music folder here or in Settings ▸ Library",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun playlistRootItems(playlists: List<com.echo.core.domain.model.Playlist>): List<CrossbarItem> {
        val rows = playlists.map { pl ->
            CrossbarItem(
                id         = "pl_${pl.id}",
                title      = pl.name,
                subtitle   = countLabel(pl.trackCount, "track", "tracks"),
                playlistId = pl.id,
                type       = CrossbarItemType.PLAYLIST,
            )
        }
        return rows + CrossbarItem(
            id       = CrossbarViewModel.CREATE_PLAYLIST_ITEM_ID,
            title    = "Create Playlist",
            subtitle = "Start a new playlist",
            type     = CrossbarItemType.ADD_ACTION,
        )
    }

    private fun addMusicAppsItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_MUSIC_APPS_ITEM_ID,
        title    = "Add Music Apps",
        subtitle = "Pick installed apps to show here",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun addTracksItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_TRACKS_ITEM_ID,
        title    = "Add Tracks",
        subtitle = "Pick songs to add to this playlist",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun setMusicTrackItems(
        tracks: List<MusicTrack>,
        emptyItem: CrossbarItem,
        trailing: List<CrossbarItem> = emptyList(),
    ) {
        currentMusicTracksRaw = tracks
        val sorted = tracks.trackSorted(uiState.value.musicSortMode)
        currentMusicTracks = sorted
        val items = if (sorted.isEmpty()) listOf(emptyItem) else sorted.toMusicItems()
        uiState.update { it.copy(currentItems = items + trailing) }
    }

    internal fun clearMusicTrackCache() {
        currentMusicTracks = emptyList()
        currentMusicTracksRaw = emptyList()
    }

    internal fun emptyAllMusicItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No music found",
        subtitle = "Add a music folder from the Folders row",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun emptyPlaylistItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_PLAYLIST_ITEM_ID,
        title    = "This playlist is empty",
        subtitle = "Add tracks below or from a song's Options menu.",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun openMusicView(nav: MusicNav) = vm.navigateRememberingCursor { it.copy(musicNav = nav) }

    internal fun closeMusicView() = openMusicView(MusicNav.Root)

    internal fun musicNavKey(nav: MusicNav): String = when (nav) {
        MusicNav.Folders     -> "folders"
        MusicNav.Root        -> "root"
        MusicNav.AllMusic    -> "all"
        MusicNav.Playlists   -> "playlists"
        is MusicNav.Playlist -> "playlist_${nav.id}"
    }

    internal fun openMusicBrowser(view: MusicBrowserView) {
        musicBrowserJob?.cancel()
        val title = when (view) {
            MusicBrowserView.AllMusic    -> "Songs"
            MusicBrowserView.Playlists   -> "Playlists"
            MusicBrowserView.Artists     -> "Artists"
            MusicBrowserView.Albums      -> "Albums"
            is MusicBrowserView.Playlist -> view.name
            is MusicBrowserView.Artist   -> view.name
            is MusicBrowserView.Album    -> view.name
        }
        uiState.update { it.copy(musicBrowser = MusicBrowserState(view = view, title = title)) }
        musicBrowserJob = scope.launch {
            when (view) {
                MusicBrowserView.AllMusic -> musicRepository.observeAllTracks().collect { tracks ->
                    browserRawTracks = tracks; rebuildBrowserTrackRows()
                }
                is MusicBrowserView.Playlist -> musicRepository.observePlaylistTracks(view.id).collect { tracks ->
                    browserRawTracks = tracks; rebuildBrowserTrackRows()
                }
                MusicBrowserView.Playlists -> musicRepository.observePlaylists().collect { playlists ->
                    browserRawPlaylists = playlists; rebuildBrowserPlaylistRows()
                }

                MusicBrowserView.Artists, MusicBrowserView.Albums ->
                    musicRepository.observeAllTracks().collect { tracks ->
                        browserRawTracks = tracks; rebuildBrowserGroupRows()
                    }
                is MusicBrowserView.Artist -> musicRepository.observeAllTracks().collect { tracks ->

                    browserRawTracks = tracks.tracksByArtistKey(view.key)
                    rebuildBrowserTrackRows()
                }
                is MusicBrowserView.Album -> musicRepository.observeAllTracks().collect { tracks ->
                    browserRawTracks = tracks.filter { it.album.musicGroupKey() == view.key }
                    rebuildBrowserTrackRows()
                }
            }
        }
    }

    private fun MusicTrack.matchesQuery(q: String): Boolean =
        displayTitle.lowercase().contains(q) ||
            artist?.lowercase()?.contains(q) == true ||
            album?.lowercase()?.contains(q) == true

    internal fun rebuildBrowserTrackRows() {
        val state = uiState.value.musicBrowser ?: return
        val isPlaylist = state.view is MusicBrowserView.Playlist
        val q = state.query.trim().lowercase()
        val sorted = browserRawTracks.trackSorted(uiState.value.musicSortMode)
        val filtered = if (q.isBlank()) sorted else sorted.filter { it.matchesQuery(q) }
        currentMusicTracks = filtered
        val baseRows = when {
            filtered.isNotEmpty() -> filtered.toMusicItems()
            q.isNotBlank()        -> listOf(vm.browserNoResultsItem())
            isPlaylist            -> listOf(emptyPlaylistItem())
            else                  -> listOf(emptyAllMusicItem())
        }
        val rows = if (isPlaylist) baseRows + addTracksItem() else baseRows

        val label = uiState.value.musicSortMode.label
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(
            rows = rows,
            selectedIndex = state.selectedIndex.coerceIn(0, (rows.size - 1).coerceAtLeast(0)),
            sortLabel = label,
        )) }
    }

    private fun rebuildBrowserGroupRows() {
        val state = uiState.value.musicBrowser ?: return
        val q = state.query.trim().lowercase()
        val prefix = if (state.view == MusicBrowserView.Artists) "art" else "alb"
        val groups = if (state.view == MusicBrowserView.Artists) browserRawTracks.artistGroups()
                     else browserRawTracks.albumGroups()
        val filtered = if (q.isBlank()) groups else groups.filter { it.name.lowercase().contains(q) }
        val rows = when {
            filtered.isNotEmpty() -> filtered.map { it.toBrowserItem(prefix) }
            q.isNotBlank()        -> listOf(vm.browserNoResultsItem())
            else                  -> listOf(emptyAllMusicItem())
        }
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(
            rows = rows,
            selectedIndex = state.selectedIndex.coerceIn(0, (rows.size - 1).coerceAtLeast(0)),
            sortLabel = null,
        )) }
    }

    private fun MusicGroup.toBrowserItem(prefix: String): CrossbarItem = CrossbarItem(
        id            = "mg_${prefix}_$key",
        title         = name,
        subtitle      = subtitle,
        coverUri      = artUri,
        musicGroupKey = key,
        type          = CrossbarItemType.MUSIC_GROUP,
    )

    private fun rebuildBrowserPlaylistRows() {
        val state = uiState.value.musicBrowser ?: return
        val q = state.query.trim().lowercase()
        val filtered = if (q.isBlank()) browserRawPlaylists
                       else browserRawPlaylists.filter { it.name.lowercase().contains(q) }
        val rows = playlistRootItems(filtered)
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(
            rows = rows,
            selectedIndex = state.selectedIndex.coerceIn(0, (rows.size - 1).coerceAtLeast(0)),
            sortLabel = null,
        )) }
    }

    fun onMusicBrowserQueryChange(query: String) {
        vm.markTouchInput()
        val state = uiState.value.musicBrowser ?: return
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(
            query = query, selectedIndex = 0,
            scrollToTopToken = state.scrollToTopToken + 1,
        )) }
        when {
            state.view is MusicBrowserView.Playlists -> rebuildBrowserPlaylistRows()
            state.view.listsGroups -> rebuildBrowserGroupRows()
            else -> rebuildBrowserTrackRows()
        }
    }

    internal fun moveMusicBrowser(delta: Int) {
        val b = uiState.value.musicBrowser ?: return
        val next = (b.selectedIndex + delta).coerceIn(0, (b.rows.size - 1).coerceAtLeast(0))
        if (next != b.selectedIndex) {
            uiState.update { it.copy(musicBrowser = b.copy(selectedIndex = next)) }
            menuSound.play(MenuSound.SCROLL)
        }
    }

    internal fun activateMusicBrowser() {
        val b = uiState.value.musicBrowser ?: return
        handleMusicBrowserRow(b.rows.getOrNull(b.selectedIndex) ?: return)
    }

    fun onMusicBrowserActivatedAt(index: Int) {
        vm.markTouchInput()
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(selectedIndex = index)) }
        activateMusicBrowser()
    }

    private fun handleMusicBrowserRow(item: CrossbarItem) {
        when {
            item.type == CrossbarItemType.EMPTY -> Unit
            item.id == CrossbarViewModel.CREATE_PLAYLIST_ITEM_ID -> { menuSound.play(MenuSound.SELECT); promptCreatePlaylist() }
            item.id == CrossbarViewModel.ADD_TRACKS_ITEM_ID -> {
                menuSound.play(MenuSound.SELECT)
                (uiState.value.musicBrowser?.view as? MusicBrowserView.Playlist)?.let { openMusicTrackPicker(it.id) }
            }
            item.type == CrossbarItemType.PLAYLIST && item.playlistId != null -> {
                menuSound.play(MenuSound.SELECT)
                openMusicBrowser(MusicBrowserView.Playlist(item.playlistId, item.title))
            }
            item.type == CrossbarItemType.MUSIC_GROUP && item.musicGroupKey != null -> {
                menuSound.play(MenuSound.SELECT)
                openMusicBrowser(
                    if (uiState.value.musicBrowser?.view == MusicBrowserView.Artists)
                        MusicBrowserView.Artist(item.title, item.musicGroupKey)
                    else MusicBrowserView.Album(item.title, item.musicGroupKey)
                )
            }
            item.type == CrossbarItemType.MUSIC_TRACK -> { menuSound.play(MenuSound.SELECT); openMusicPlayerForItem(item) }
        }
    }

    internal fun openMusicBrowserContextMenu() {
        val b = uiState.value.musicBrowser ?: return
        val item = b.rows.getOrNull(b.selectedIndex) ?: return
        when {
            item.type == CrossbarItemType.MUSIC_TRACK -> openMusicTrackContextMenu(item)
            item.type == CrossbarItemType.PLAYLIST && item.playlistId != null ->
                openPlaylistRowContextMenu(item.playlistId, item.title)
        }
    }

    fun onMusicBrowserLongPressAt(index: Int) {
        vm.markTouchInput()
        uiState.update { it.copy(musicBrowser = it.musicBrowser?.copy(selectedIndex = index)) }
        openMusicBrowserContextMenu()
    }

    fun onMusicBrowserBack() {
        vm.markTouchInput()
        val b = uiState.value.musicBrowser ?: return
        menuSound.play(MenuSound.BACK)
        when (b.view) {
            is MusicBrowserView.Playlist -> openMusicBrowser(MusicBrowserView.Playlists)
            is MusicBrowserView.Artist -> openMusicBrowser(MusicBrowserView.Artists)
            is MusicBrowserView.Album -> openMusicBrowser(MusicBrowserView.Albums)
            else -> closeMusicBrowser()
        }
    }

    private fun closeMusicBrowser() {
        musicBrowserJob?.cancel(); musicBrowserJob = null
        val view = uiState.value.musicBrowser?.view
        browserRawTracks = emptyList(); browserRawPlaylists = emptyList()
        uiState.update { it.copy(musicBrowser = null) }

        if (vm.currentCategory()?.id == BuiltInCategory.MUSIC && uiState.value.musicNav == MusicNav.Root) {
            val targetId = when (view) {
                is MusicBrowserView.Playlists, is MusicBrowserView.Playlist -> CrossbarViewModel.PLAYLISTS_ITEM_ID
                is MusicBrowserView.Artists, is MusicBrowserView.Artist -> CrossbarViewModel.MUSIC_ARTISTS_ITEM_ID
                is MusicBrowserView.Albums, is MusicBrowserView.Album -> CrossbarViewModel.MUSIC_ALBUMS_ITEM_ID
                else -> CrossbarViewModel.ALL_MUSIC_ITEM_ID
            }
            val idx = uiState.value.currentItems.indexOfFirst { it.id == targetId }
            if (idx >= 0) uiState.update { it.copy(selectedItemIndex = idx) }
        }
    }

    fun onMusicBrowserSortTapped() {
        vm.markTouchInput()
        vm.cycleSort()
    }

    fun onMusicBrowserOptionsTapped() {
        vm.markTouchInput()
        openMusicBrowserContextMenu()
    }

    private fun currentPlaylistContextId(): Long? =
        (uiState.value.musicBrowser?.view as? MusicBrowserView.Playlist)?.id
            ?: (uiState.value.musicNav as? MusicNav.Playlist)?.id

    internal fun handleMusicSelection(item: CrossbarItem): Boolean = when {
        item.id == CrossbarViewModel.SEARCH_ITEM_ID -> { vm.librarySearch.openSearch(SearchScope.MUSIC); true }
        item.id == CrossbarViewModel.ADD_MENU_ITEM_ID -> { menuSound.play(MenuSound.SELECT); vm.openAddMenu(); true }
        item.type == CrossbarItemType.EMPTY -> true
        item.id == CrossbarViewModel.NOW_PLAYING_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            if (uiState.value.musicPlayback.track != null) uiState.update { it.copy(musicPlayerVisible = true) }
            true
        }

        item.id == CrossbarViewModel.PLAYLISTS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openMusicBrowser(MusicBrowserView.Playlists); true }
        item.id == CrossbarViewModel.ALL_MUSIC_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openMusicBrowser(MusicBrowserView.AllMusic); true }
        item.id == CrossbarViewModel.MUSIC_ARTISTS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openMusicBrowser(MusicBrowserView.Artists); true }
        item.id == CrossbarViewModel.MUSIC_ALBUMS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openMusicBrowser(MusicBrowserView.Albums); true }
        item.id == CrossbarViewModel.ADD_MUSIC_FOLDER_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.folders.openMediaFolders(MediaRootKind.MUSIC)
            true
        }
        item.id == CrossbarViewModel.CREATE_PLAYLIST_ITEM_ID -> { menuSound.play(MenuSound.SELECT); promptCreatePlaylist(); true }
        item.id == CrossbarViewModel.ADD_MUSIC_APPS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.appPickerSection.openAppPicker(AppPickerTarget.CategoryShortcuts(CrossbarViewModel.MUSIC_APPS_CATEGORY_ID), "Add Music Apps")
            true
        }
        item.id == CrossbarViewModel.ADD_TRACKS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            (uiState.value.musicNav as? MusicNav.Playlist)?.let { openMusicTrackPicker(it.id) }
            true
        }
        item.type == CrossbarItemType.MUSIC_TRACK -> { menuSound.play(MenuSound.SELECT); openMusicPlayerForItem(item); true }
        item.type == CrossbarItemType.PLAYLIST && item.playlistId != null -> {
            menuSound.play(MenuSound.SELECT); openMusicView(MusicNav.Playlist(item.playlistId, item.title)); true
        }

        item.packageName != null -> {
            menuSound.play(MenuSound.LAUNCH)
            vm.launching.launchAppWithDisc(item.packageName, item.shelfCoverArt)
            true
        }
        else -> false
    }

    internal fun openMusicPlayerForItem(item: CrossbarItem) {
        val trackId = item.id.removePrefix("mt_")
        val startIndex = currentMusicTracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
        if (currentMusicTracks.isEmpty()) return
        val track = currentMusicTracks[startIndex]
        scope.launch {
            vm.launching.awaitDiscHandOff(track.artUri)
            musicPlayer.setQueue(currentMusicTracks, startIndex)
            uiState.update { it.copy(musicPlayerVisible = true) }
        }
    }

    fun musicPlayPause() = musicPlayer.playPause()
    fun musicNext() = musicPlayer.next()
    fun musicPrev() = musicPlayer.prev()
    fun musicSeekTo(ms: Int) = musicPlayer.seekTo(ms)
    fun musicToggleShuffle() = musicPlayer.toggleShuffle()
    fun musicCycleRepeat() = musicPlayer.cycleRepeat()
    internal fun musicSeekBy(deltaMs: Int) = musicPlayer.seekBy(deltaMs)

    fun closeMusicPlayer() {
        uiState.update { it.copy(musicPlayerVisible = false) }
    }

    internal fun stopAndCloseMusicPlayer() {
        musicPlayer.stop()
        uiState.update { it.copy(musicPlayerVisible = false) }
    }

    internal fun openMusicPlayerOptions() {
        val title = musicPlayer.currentTrack()?.displayTitle ?: "Now Playing"
        uiState.update {
            it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = title, rows = musicPlayerMenuItems(it.musicPlayback)), musicTrackId = CrossbarViewModel.MUSIC_PLAYER_MENU_MARKER)
            )
        }
    }

    internal fun musicPlayInBackground() {
        if (musicPlayer.currentTrack() == null) return
        com.echo.feature.crossbar.music.MusicPlaybackService.start(vm.context)
        uiState.update { it.copy(musicPlayerVisible = false) }
    }

    private fun openMusicTrackContextMenu(item: CrossbarItem) {
        val playlistId = currentPlaylistContextId()

        val trackId = item.id.removePrefix("mt_")
        scope.launch {
            val onShelf = runCatching { musicRepository.getTrack(trackId) }
                .getOrNull()?.lastPlayedAt != null
            val items = musicTrackContextMenuItems(playlistId = playlistId, hasPlayStamp = onShelf)
            uiState.update { it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = items), musicTrackId = trackId, playlistId = playlistId)
            )}
        }
    }

    private fun openPlaylistRowContextMenu(playlistId: Long, name: String) {
        val items = playlistRowContextMenuItems()
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = name, rows = items), playlistId = playlistId)) }
    }

    internal fun openPlaylistPicker(trackId: String, selectIndex: Int? = 0) {
        scope.launch {
            val playlists = musicRepository.observePlaylists().first()
            val memberOf = musicRepository.getPlaylistIdsForTrack(trackId).toSet()
            val items = buildList {
                playlists.forEach { pl ->
                    add(CrossbarContextMenuItem("pl_${pl.id}", pl.name, checked = pl.id in memberOf))
                }
                add(CrossbarContextMenuItem("pl_new", "Create New Playlist"))
            }
            uiState.update { it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Add to Playlist", rows = items, selectedIndex = selectIndex?.coerceIn(0, items.lastIndex.coerceAtLeast(0))), playlistPickerTrackId = trackId)
            )}
        }
    }

    internal fun removeAlbumFromRecent(key: String) {
        val ids = recentAlbumTrackIds(currentMusicTracks, key)
        vm.appAction { ids.forEach { musicRepository.clearTrackLastPlayed(it) } }
    }

    internal fun openRecentAlbumContextMenu(item: CrossbarItem) {
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = recentAlbumContextMenuItems()), recentAlbum = item)) }
    }

    internal fun openMusicContextMenu(item: CrossbarItem): Boolean {
        if (item.menuHostCategory(vm.currentCategory()?.id) != BuiltInCategory.MUSIC) return false
        return when {
            item.id == CrossbarViewModel.NOW_PLAYING_ITEM_ID -> { vm.openNowPlayingContextMenu(); true }
            item.type == CrossbarItemType.MUSIC_TRACK -> { openMusicTrackContextMenu(item); true }
            item.type == CrossbarItemType.PLAYLIST && item.playlistId != null -> {
                openPlaylistRowContextMenu(item.playlistId, item.title); true
            }
            item.packageName != null -> {
                vm.openAppContextMenu(item, categoryIdOverride = CrossbarViewModel.MUSIC_APPS_CATEGORY_ID); true
            }
            else -> false
        }
    }

    internal fun promptCreatePlaylist(forTrackId: String? = null) {
        uiState.update { it.copy(
            playlistNameDialog = PlaylistNameDialogState(title = "New Playlist", forTrackId = forTrackId)
        )}
    }

    private fun promptRenamePlaylist(playlistId: Long) {
        val name = uiState.value.currentItems.firstOrNull { it.playlistId == playlistId }?.title.orEmpty()
        uiState.update { it.copy(
            playlistNameDialog = PlaylistNameDialogState(
                title = "Rename Playlist",
                initialText = name,
                renamePlaylistId = playlistId,
            )
        )}
    }

    fun onConfirmPlaylistName(name: String) {
        val dialog = uiState.value.playlistNameDialog ?: return
        uiState.update { it.copy(playlistNameDialog = null) }
        if (name.isBlank()) return
        scope.launch {
            val renameId = dialog.renamePlaylistId
            if (dialog.videoContext) {
                if (renameId != null) {
                    vm.videoRepository.renamePlaylist(renameId, name)
                } else {
                    val id = vm.videoRepository.createPlaylist(name)
                    dialog.forVideoId?.let { vm.videoRepository.addVideoToPlaylist(id, it) }
                }
            } else if (renameId != null) {
                musicRepository.renamePlaylist(renameId, name)
            } else {
                val id = musicRepository.createPlaylist(name)
                dialog.forTrackId?.let { musicRepository.addTrackToPlaylist(id, it) }
            }
        }
    }

    fun onCancelPlaylistName() {
        uiState.update { it.copy(playlistNameDialog = null) }
    }

    private fun openMusicTrackPicker(playlistId: Long) {
        scope.launch {
            val playlist = musicRepository.observePlaylists().first().firstOrNull { it.id == playlistId }

            val inPlaylist = musicRepository.observePlaylistTracks(playlistId).first().map { it.id }.toSet()
            val tracks = musicRepository.observeAllTracks().first()
                .filterNot { it.id in inPlaylist }
                .trackSorted(uiState.value.musicSortMode)
            uiState.update { it.copy(
                musicTrackPicker = MusicTrackPickerState(
                    playlistId   = playlistId,
                    playlistName = playlist?.name ?: "Playlist",
                    tracks       = tracks,
                )
            )}
        }
    }

    internal fun moveMusicTrackPicker(delta: Int) {
        val picker = uiState.value.musicTrackPicker ?: return
        val maxIndex = picker.tracks.size
        val next = (picker.selectedIndex + delta).coerceIn(0, maxIndex)
        uiState.update { it.copy(musicTrackPicker = picker.copy(selectedIndex = next)) }
    }

    internal fun activateMusicTrackPicker() {
        val picker = uiState.value.musicTrackPicker ?: return
        if (picker.selectedIndex == 0) {
            confirmMusicTrackPicker()
        } else {
            val track = picker.tracks.getOrNull(picker.selectedIndex - 1) ?: return
            val selected = if (track.id in picker.selected) picker.selected - track.id
                           else picker.selected + track.id
            uiState.update { it.copy(musicTrackPicker = picker.copy(selected = selected)) }
        }
    }

    fun onMusicTrackPickerActivatedAt(index: Int) {
        uiState.update { it.copy(musicTrackPicker = it.musicTrackPicker?.copy(selectedIndex = index)) }
        activateMusicTrackPicker()
    }

    fun onMusicTrackPickerConfirm() = confirmMusicTrackPicker()

    fun closeMusicTrackPicker() {
        uiState.update { it.copy(musicTrackPicker = null) }
    }

    internal fun confirmMusicTrackPicker() {
        val picker = uiState.value.musicTrackPicker ?: return
        val playlistId = picker.playlistId
        val trackIds = picker.tracks.map { it.id }.filter { it in picker.selected }
        closeMusicTrackPicker()
        if (trackIds.isEmpty()) return
        scope.launch {
            trackIds.forEach { musicRepository.addTrackToPlaylist(playlistId, it) }
        }
    }


    internal fun handleMusicFolderAction(folderId: String, itemId: String) {
        when (itemId) {
            "scan_folder" -> vm.appAction { scanMusicFolder(folderId) }
            "rename_folder" -> vm.folders.openMediaFolders(MediaRootKind.MUSIC)
            "enable_folder" -> vm.appAction { musicRepository.setFolderEnabled(folderId, true) }
            "disable_folder" -> vm.appAction { musicRepository.setFolderEnabled(folderId, false) }
            "remove_folder" -> vm.appAction { musicRepository.removeFolder(folderId) }
        }
    }

    internal fun handleMusicTrackAction(trackId: String, itemId: String, playlistId: Long?) {
        when (itemId) {
            "play" -> {
                val startIndex = currentMusicTracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
                if (currentMusicTracks.isNotEmpty()) {
                    musicPlayer.setQueue(currentMusicTracks, startIndex)
                    uiState.update { it.copy(musicPlayerVisible = true) }
                }
            }

            "play_background" -> {
                val startIndex = currentMusicTracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
                if (currentMusicTracks.isNotEmpty()) {
                    musicPlayer.setQueue(currentMusicTracks, startIndex)
                    com.echo.feature.crossbar.music.MusicPlaybackService.start(vm.context)
                }
            }
            "add_to_playlist" -> openPlaylistPicker(trackId)
            "remove_from_playlist" -> if (playlistId != null) {
                vm.appAction { musicRepository.removeTrackFromPlaylist(playlistId, trackId) }
            }

            "remove_from_recent" -> vm.appAction { musicRepository.clearTrackLastPlayed(trackId) }
            "remove_track" -> vm.appAction {
                val track = musicRepository.getTrack(trackId) ?: return@appAction
                removeSingleTrack(track.folderId, trackId)
            }
        }
    }

    internal fun handlePlaylistRowAction(playlistId: Long, itemId: String) {
        when (itemId) {
            "open_playlist"   -> {
                val name = uiState.value.currentItems.firstOrNull { it.playlistId == playlistId }?.title.orEmpty()
                openMusicView(MusicNav.Playlist(playlistId, name))
            }
            "add_tracks"      -> openMusicTrackPicker(playlistId)
            "rename_playlist" -> promptRenamePlaylist(playlistId)
            "delete_playlist" -> vm.appAction {
                musicRepository.deletePlaylist(playlistId)
                if ((uiState.value.musicNav as? MusicNav.Playlist)?.id == playlistId) closeMusicView()
            }
        }
    }

    internal suspend fun scanMusicFolder(folderId: String) {
        val folder = musicRepository.getFolder(folderId) ?: return
        val taskId = "music_scan_$folderId"
        vm.addBackgroundTask(BackgroundTaskInfo(taskId, "Scanning ${folder.displayName}", null))
        musicScanner.scan(folder).collect { result ->
            when (result) {
                is com.echo.feature.library.scanner.MusicScanResult.Progress -> Unit
                is com.echo.feature.library.scanner.MusicScanResult.Complete -> {
                    musicRepository.replaceTracksForFolder(result.folderId, result.tracks, System.currentTimeMillis())
                    vm.completeBackgroundTask(taskId, "${result.tracks.size} tracks")
                }
                is com.echo.feature.library.scanner.MusicScanResult.Error ->
                    vm.failBackgroundTask(taskId, result.message)
            }
        }
    }

    private suspend fun removeSingleTrack(folderId: String, trackId: String) {
        val tracks = musicRepository.observeTracksByFolder(folderId).first().filterNot { it.id == trackId }
        musicRepository.replaceTracksForFolder(folderId, tracks, System.currentTimeMillis())
    }

    internal fun musicPlaylistSiblings(): List<CrossbarItem> =
        uiState.value.musicPlaylists.map { CrossbarItem(id = "pl_${it.id}", title = it.name, playlistId = it.id, type = CrossbarItemType.PLAYLIST) }

    internal fun onTrackPickerButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP   -> moveMusicTrackPicker(-1)
            GamepadAction.NAVIGATE_DOWN -> moveMusicTrackPicker(+1)
            GamepadAction.SELECT        -> activateMusicTrackPicker()
            GamepadAction.HOME          -> confirmMusicTrackPicker()
            GamepadAction.BACK,
            GamepadAction.OPEN_CONTEXT_MENU    -> closeMusicTrackPicker()
            else -> Unit
        }
    }

    internal fun onPlayerButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.SELECT         -> musicPlayPause()
            GamepadAction.NAVIGATE_LEFT  -> musicPrev()
            GamepadAction.NAVIGATE_RIGHT -> musicNext()
            GamepadAction.NAVIGATE_UP    -> musicSeekBy(10_000)
            GamepadAction.NAVIGATE_DOWN  -> musicSeekBy(-10_000)
            GamepadAction.OPEN_CONTEXT_MENU     -> openMusicPlayerOptions()
            GamepadAction.BACK           -> closeMusicPlayer()
            else -> Unit
        }
    }

    internal fun onPlaylistNameButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.SELECT -> onConfirmPlaylistName(state.playlistNameDialog?.text ?: return)
            GamepadAction.BACK   -> onCancelPlaylistName()
            else                 -> Unit
        }
    }

    internal fun onBrowserButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP    -> moveMusicBrowser(-1)
            GamepadAction.NAVIGATE_DOWN  -> moveMusicBrowser(+1)
            GamepadAction.SELECT         -> activateMusicBrowser()
            GamepadAction.BACK           -> onMusicBrowserBack()
            GamepadAction.OPEN_CONTEXT_MENU     -> openMusicBrowserContextMenu()
            GamepadAction.CHANGE_SORT    -> vm.cycleSort()
            else -> Unit
        }
    }
    internal fun onPlaylistPickerItem(itemId: String, menu: CrossbarContextMenu) {
        val trackId = menu.playlistPickerTrackId ?: return
        val keepIndex = menu.selectedIndex
        when {
            itemId == "pl_new" -> {
                vm.closeContextMenu()
                promptCreatePlaylist(forTrackId = trackId)
            }
            itemId.startsWith("pl_") -> {
                val playlistId = itemId.removePrefix("pl_").toLongOrNull() ?: return
                scope.launch {
                    musicRepository.toggleTrackInPlaylist(playlistId, trackId)

                    openPlaylistPicker(trackId, keepIndex)
                }
            }
        }
    }
    internal suspend fun loadColumn() {
        when (val nav = uiState.value.musicNav) {
            MusicNav.Folders -> vm.mediaRootRepository.roots(MediaRootKind.MUSIC).collect {
                uiState.update { s -> s.copy(currentItems = vm.folders.mediaFolderItems(MediaRootKind.MUSIC)) }
            }
            MusicNav.Root -> {
                clearMusicTrackCache()
                uiState.update { it.copy(currentItems = musicRootItems()) }
            }
            MusicNav.AllMusic -> musicRepository.observeAllTracks().collect { tracks ->
                setMusicTrackItems(tracks, emptyAllMusicItem())
            }
            is MusicNav.Playlist -> musicRepository.observePlaylistTracks(nav.id).collect { tracks ->
                setMusicTrackItems(tracks, emptyPlaylistItem(), trailing = listOf(addTracksItem()))
            }
            MusicNav.Playlists -> {
                clearMusicTrackCache()
                musicRepository.observePlaylists().collect { playlists ->
                    uiState.update { it.copy(currentItems = playlistRootItems(playlists), musicPlaylists = playlists) }
                }
            }
        }
    }
}
