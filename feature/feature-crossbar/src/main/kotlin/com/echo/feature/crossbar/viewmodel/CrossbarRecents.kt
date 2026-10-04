package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.HiddenPlacement
import com.echo.core.domain.model.HideLocationType
import com.echo.core.domain.model.PlayState
import com.echo.core.ui.components.at
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarRecents(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun dismissAppFromRecents(packageName: String) {
        scope.launch {
            vm.context.echoDataStore.edit { prefs ->
                prefs[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS] = withRecentDismissal(
                    prefs[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS].orEmpty(), packageName, System.currentTimeMillis(),
                )
            }
        }
    }

    internal fun emptyRecentItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "Nothing watched yet",
        subtitle = "Videos you play show up here",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun recentFilterAndApps(): Flow<Triple<RecentFilter, List<Pair<Long, CrossbarItem>>, Int>> =
        combine(
            uiState.map { it.recentFilter }.distinctUntilChanged(),
            recentAppRows(),
            uiState.map { it.interfaceChoices.lastPlayedSize }.distinctUntilChanged(),
        ) { filter, rows, limit -> Triple(filter, rows, limit) }

    private fun recentAppRows(): Flow<List<Pair<Long, CrossbarItem>>> =
        combine(
            uiState.map { it.recentsIncludeApps }.distinctUntilChanged(),
            vm.appCategoryRepository.changes().onStart { emit(Unit) },
            vm.context.echoDataStore.data
                .map { it[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS].orEmpty() }
                .distinctUntilChanged(),
        ) { includeApps, _, dismissals -> includeApps to dismissals }
            .map { (includeApps, dismissals) ->
                if (!includeApps) return@map emptyList()
                val dismissedAt = parseRecentDismissals(dismissals)
                vm.appCategoryRepository.allInstalledApps()
                    .filter { it.lastUsedAt > 0L }

                    .filterNot { dismissedFromRecents(it.lastUsedAt, dismissedAt[it.packageName]) }

                    .filterNot { vm.isHiddenAt(HiddenPlacement.appKey(it.packageName), HideLocationType.RECENTS) }
                    .sortedByDescending { it.lastUsedAt }
                    .take(CrossbarViewModel.RECENTLY_PLAYED_LIMIT)
                    .map { app ->
                        app.lastUsedAt to CrossbarItem(
                            id = recentAppId(app.packageName),
                            title = app.label,
                            subtitle = "App",
                            packageName = app.packageName,

                            isAndroidApp = true,
                        )
                    }
            }

    internal fun stepRecentFilter(delta: Int) =
        setRecentFilter(uiState.value.let { it.recentFilter.step(delta, it.recentsIncludeApps) })

    fun setRecentFilter(filter: RecentFilter) {
        menuSound.play(MenuSound.SYSTEM_BROWSE)
        uiState.update { it.copy(recentFilter = filter, selectedItemIndex = 0) }
    }


    internal fun removeFromRecent(item: CrossbarItem) {
        if (!item.removableFromRecent) return
        menuSound.play(MenuSound.SELECT)
        when {
            item.type == CrossbarItemType.VIDEO_FILE -> vm.video.handleVideoFileAction(item.id.removePrefix("vid_"), "video_remove_recent")
            item.type == CrossbarItemType.LIBRARY_BOOK -> vm.bookshelf.handleBookAction(item.id.removePrefix("book_"), "book_remove_recent")
            item.type == CrossbarItemType.MUSIC_TRACK -> vm.music.handleMusicTrackAction(item.id.removePrefix("mt_"), "remove_from_recent", null)
            item.gameId != null -> {
                val gid = item.gameId
                vm.appAction { vm.gameRepository.clearLastPlayed(gid) }
            }
            item.packageName != null -> dismissAppFromRecents(item.packageName)
        }
    }

    internal fun observeShelfCounts() {
        scope.launch {
            val marks = PlayState.entries

            combine(
                marks.map { vm.gameRepository.observeByPlayState(it) } +
                    vm.gameRepository.observeRecentlyAdded() +
                    vm.gameRepository.observeFavorites(),
            ) { lists ->
                val byState = marks.mapIndexed { i, state -> state to lists[i] }.toMap()
                Triple(byState, lists[marks.size], lists[marks.size + 1])
            }.collect { (byState, recentlyAdded, favorites) ->
                uiState.update { state ->
                    state.copy(
                        playStateCounts = byState.mapValues { (_, games) -> games.size },
                        recentlyAddedCount = recentlyAdded.size,

                        shelfFanCovers = buildMap {
                            put(SHELF_FAVORITES_ID, fanCoversOf(favorites))
                            put(SHELF_RECENT_ID, fanCoversOf(recentlyAdded))
                            byState.forEach { (mark, games) ->
                                put("$SHELF_MARKED_PREFIX${mark.name}", fanCoversOf(games))
                            }
                        },
                    )
                }
            }
        }
    }

    internal fun observeRecentTop() {
        scope.launch {
            combine(
                vm.gameRepository.observeRecentlyPlayed(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
                vm.musicRepository.observeRecentlyPlayedTracks(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
                vm.bookshelf.observeRecentBookRows(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
                vm.videoRepository.observeRecentlyWatched(),
                recentAppRows(),
            ) { games, tracks, books, videos, appRows ->
                val visibleGames = with(vm) { games.notHiddenAt(HideLocationType.ALL_GAMES) }
                val rows = listOf(
                    visibleGames.map { it.lastPlayedAt ?: 0L }.zip(with(vm) { visibleGames.toCrossbarItems() }),
                    tracks.recentMusicRows(),
                    books,
                    videos.map { it.lastWatchedAt ?: 0L }.zip(videos.toVideoItems()),
                    appRows,
                )
                val top = mergeRecents(
                    games  = rows[0],
                    music  = rows[1],
                    books  = rows[2],
                    videos = rows[3],
                    apps   = rows[4],
                    filter = RecentFilter.ALL,
                    limit  = CrossbarViewModel.RECENTLY_PLAYED_LIMIT,
                ).firstOrNull { recentLaunchFor(it) != null }
                top to top?.let { t -> rows.flatten().firstOrNull { it.second.id == t.id }?.first?.takeIf { it > 0L } }
            }.collect { (top, at) ->
                uiState.update { it.copy(recentTop = top, recentTopAt = at) }
            }
        }
    }

    fun launchRecentTop() {
        val item = uiState.value.recentTop ?: return
        uiState.update { it.copy(activeAppDrawerFilter = null, pendingDrawerAction = null) }

        when (recentLaunchFor(item)) {
            RecentLaunch.GAME  -> item.gameId?.let { vm.launching.launchGameDirectly(it) }
            RecentLaunch.STORED_INTENT -> item.launchIntentUri?.let { vm.launching.launchStoredIntent(it, item.title) }
            RecentLaunch.SHORTCUT -> {
                val pkg = item.packageName ?: return
                val shortcut = item.shortcutId ?: return
                vm.launching.launchHarvestedShortcut(pkg, shortcut)
            }
            RecentLaunch.APP   -> item.packageName?.let { vm.launching.launchAppWithDisc(it, item.shelfCoverArt) }
            RecentLaunch.VIDEO -> {
                menuSound.play(MenuSound.SELECT)
                uiState.update { it.copy(activeVideoId = item.id.removePrefix("vid_")) }
            }
            RecentLaunch.BOOK  -> {
                menuSound.play(MenuSound.SELECT)
                vm.bookshelf.openBook(item.id.removePrefix("book_"))
            }
            RecentLaunch.TRACK -> {
                menuSound.play(MenuSound.SELECT)
                vm.music.openMusicPlayerForItem(item)
            }
            RecentLaunch.ALBUM -> item.musicGroupKey?.let {
                menuSound.play(MenuSound.SELECT)
                vm.music.openMusicBrowser(MusicBrowserView.Album(item.title, it))
            }
            null -> Unit
        }
    }

    fun onRecentCardTap(index: Int) {
        vm.markTouchInput()
        val s = uiState.value
        if (s.hasBlockingOverlay || index !in s.currentItems.indices) return
        // a game or app launches only by holding the launch button; tapping its card just picks it
        if (s.currentItems[index].launchesOut()) uiState.update { it.copy(selectedItemIndex = index) } else vm.onItemSelected(index)
    }

    internal fun openShelf(cardId: String) {
        vm.navigateRememberingCursor {
            it.copy(selectedPlatformId = cardId)
        }
    }

    internal fun observeRecentTopAccent() {
        scope.launch {
            uiState
                .map { s -> s.recentTop?.takeIf { recentKind(it) != RecentKind.APP } }
                .distinctUntilChanged { a, b -> a?.backdropIdentity() == b?.backdropIdentity() && a?.shelfCoverArt == b?.shelfCoverArt }
                .collectLatest { top ->
                    val accent = top?.let { vm.artworkAccent.of(it.shelfCoverArt, *it.backdropArt.toTypedArray()) }
                    uiState.update { it.copy(recentTopAccentArgb = accent) }
                }
        }
    }
    internal suspend fun loadColumn(keepCursorOnRow: Boolean) {
        var keepCursor = keepCursorOnRow
        combine(
            vm.gameRepository.observeRecentlyPlayed(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
            vm.musicRepository.observeRecentlyPlayedTracks(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
            vm.bookshelf.observeRecentBookRows(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
            vm.videoRepository.observeRecentlyWatched(),

            recentFilterAndApps(),
        ) { games, tracks, books, videos, filterAndApps ->
            val (filter, appRows, limit) = filterAndApps

            vm.music.currentMusicTracks = tracks
            val visibleGames = with(vm) { games.notHiddenAt(HideLocationType.ALL_GAMES) }
            mergeRecents(
                games  = visibleGames.map { it.lastPlayedAt ?: 0L }.zip(with(vm) { visibleGames.toCrossbarItems() }),

                music  = tracks.recentMusicRows(),
                books  = books,
                videos = videos.map { it.lastWatchedAt ?: 0L }.zip(videos.toVideoItems()),
                apps   = appRows,
                filter = filter,
                limit  = limit,
            )
        }.collect { items ->

            vm.publishGameItems(items, keepCursor)
            keepCursor = true
        }
    }
}
