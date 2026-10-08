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
    // the stored pins, for the menus to know what is pinned
    private val pins: Flow<List<String>> =
        vm.context.echoDataStore.data.map { parsePins(it[CrossbarViewModel.KEY_RECENT_PINS]) }.distinctUntilChanged()

    init {
        scope.launch { pins.collect { list -> uiState.update { it.copy(recentPins = list) } } }
    }

    // pins or unpins a game or app under Recent (owner, 2026-10-06)
    internal fun togglePinned(key: String) {
        menuSound.play(MenuSound.SELECT)
        scope.launch {
            vm.context.echoDataStore.edit { prefs ->
                prefs[CrossbarViewModel.KEY_RECENT_PINS] = togglePin(parsePins(prefs[CrossbarViewModel.KEY_RECENT_PINS]), key).joinToString("\n")
            }
        }
    }

    // each pin as a row: a game as the library shows it, an app by its label; a pin whose game or app is gone
    // is left out
    private fun pinnedRows(): Flow<List<CrossbarItem>> = combine(pins, vm.appCategoryRepository.changes().onStart { emit(Unit) }) { list, _ -> list }
        .map { list ->
            val apps = if (list.any { it.startsWith("a:") }) vm.appCategoryRepository.visibleInstalledApps().associateBy { it.packageName } else emptyMap()
            list.mapNotNull { key ->
                val row = when {
                    key.startsWith("g:") -> key.removePrefix("g:").toLongOrNull()
                        ?.let { id -> runCatching { vm.gameRepository.getById(id) }.getOrNull() }
                        ?.let { game -> with(vm) { listOf(game).toCrossbarItems() }.firstOrNull() }
                    key.startsWith("a:") -> apps[key.removePrefix("a:")]?.let { app ->
                        CrossbarItem(id = key, title = app.label, subtitle = "App", packageName = app.packageName, isAndroidApp = true)
                    }
                    else -> null
                }
                row?.copy(id = pinnedRowId(key), pinnedToRecent = true)
            }
        }

    internal fun dismissAppFromRecents(packageName: String) = dismissFromRecents(packageName)

    internal fun dismissGameFromRecents(gameId: Long) = dismissFromRecents(gameDismissalKey(gameId))

    // an app by its package, a game by gameDismissalKey; both come back once used again
    private fun dismissFromRecents(key: String) {
        scope.launch {
            vm.context.echoDataStore.edit { prefs ->
                prefs[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS] = withRecentDismissal(
                    prefs[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS].orEmpty(), key, System.currentTimeMillis(),
                )
            }
        }
    }

    private fun recentDismissals(): Flow<Map<String, Long>> =
        vm.context.echoDataStore.data
            .map { parseRecentDismissals(it[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS].orEmpty()) }
            .distinctUntilChanged()

    // the games Last Played shows: played, and not taken off it since
    private fun recentGames(): Flow<List<com.echo.core.domain.model.Game>> =
        combine(vm.gameRepository.observeRecentlyPlayed(CrossbarViewModel.RECENTLY_PLAYED_LIMIT), recentDismissals(), ::notDismissedGames)

    internal fun emptyRecentItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "Nothing watched yet",
        subtitle = "Videos you play show up here",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun recentFilterAndApps(filter: Flow<RecentFilter>): Flow<Triple<RecentFilter, List<Pair<Long, CrossbarItem>>, Int>> =
        combine(
            filter,
            recentAppRows(),
            uiState.map { it.interfaceChoices.lastPlayedSize }.distinctUntilChanged(),
        ) { filter, rows, limit -> Triple(filter, rows, limit) }

    private fun recentAppRows(): Flow<List<Pair<Long, CrossbarItem>>> =
        combine(
            uiState.map { it.recentsIncludeApps }.distinctUntilChanged(),
            combine(vm.appCategoryRepository.changes().onStart { emit(Unit) }, vm.appCategoryRepository.lastUsedChanges()) { _, _ -> },
            vm.context.echoDataStore.data
                .map { it[CrossbarViewModel.KEY_RECENT_APP_DISMISSALS].orEmpty() }
                .distinctUntilChanged(),
        ) { includeApps, _, dismissals -> includeApps to dismissals }
            .map { (includeApps, dismissals) ->
                if (!includeApps) return@map emptyList()
                val dismissedAt = parseRecentDismissals(dismissals)
                vm.appCategoryRepository.visibleInstalledApps()
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
        setRecentFilter(uiState.value.let { it.recentFilter.step(delta, it.recentFilters) })

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
            item.isRecentAlbum -> vm.music.removeAlbumFromRecent(item.musicGroupKey!!)
            item.gameId != null -> dismissGameFromRecents(item.gameId)
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
                recentGames(),
                vm.musicRepository.observeRecentlyPlayedTracks(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
                vm.bookshelf.observeRecentBookRows(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
                vm.videoRepository.observeRecentlyWatched(),
                recentAppRows(),
            ) { games, tracks, books, videos, appRows ->
                val visibleGames = with(vm) { games.notHiddenAt(HideLocationType.RECENTS) }
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
                uiState.update { it.copy(activeVideoId = item.id.removePrefix("vid_"), activeVideoAutoPlay = true) }
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
                vm.music.openAlbumOnCrossbar(item.title, it)
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

    private var touchHolding = false

    // owner, 2026-10-05: holding a game or app's card or row launches it, the same hold as A; a tap only picks it
    fun onRecentCardPress(index: Int, down: Boolean) {
        if (!down) {
            if (touchHolding) vm.releaseLaunchHold()
            touchHolding = false
            return
        }
        vm.markTouchInput()
        val s = uiState.value
        val item = s.currentItems.getOrNull(index)
        if (s.hasBlockingOverlay || item == null || !item.launchesOut()) return
        uiState.update { it.copy(selectedItemIndex = index) }
        touchHolding = true
        vm.startLaunchHold(item) {
            touchHolding = false
            // by id, in case the column changed under the finger during the hold
            uiState.value.currentItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }?.let(vm::onItemSelected)
        }
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
    internal class RecentRows(val filters: List<RecentFilter>, val items: List<CrossbarItem>, val tracks: List<com.echo.core.domain.model.MusicTrack>)

    // the Last Played rows for [filter]: the column on the top screen, and on a device with a second
    // screen the bottom screen's Recent page (owner, 2026-10-06), so both read the same list
    internal fun recentRows(filter: Flow<RecentFilter>): Flow<RecentRows> =
        combine(
            recentGames(),
            vm.musicRepository.observeRecentlyPlayedTracks(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
            vm.bookshelf.observeRecentBookRows(CrossbarViewModel.RECENTLY_PLAYED_LIMIT),
            vm.videoRepository.observeRecentlyWatched(),

            recentFilterAndApps(filter),
        ) { games, tracks, books, videos, filterAndApps -> filterAndApps.first to Triple(games, tracks, Triple(books, videos, filterAndApps)) }
            .combine(pinnedRows()) { (shown, rest), pinned -> Triple(shown, rest, pinned) }
            .map { (shownFilter, rest, pinned) ->
            val (games, tracks, more) = rest
            val (books, videos, filterAndApps) = more
            val (_, appRows, limit) = filterAndApps

            val visibleGames = with(vm) { games.notHiddenAt(HideLocationType.RECENTS) }
            val music = tracks.recentMusicRows()
            val filters = RecentFilter.shown(stockedRecentFilters(visibleGames, music, books, videos, appRows))
            // the pinned rows lead (owner, 2026-10-07), so the cursor meets them in the order the rail draws them
            RecentRows(filters, pinnedForFilter(pinned, shownFilter) + mergeRecents(
                games  = visibleGames.map { it.lastPlayedAt ?: 0L }.zip(with(vm) { visibleGames.toCrossbarItems() }),

                music  = music,
                books  = books,
                videos = videos.map { it.lastWatchedAt ?: 0L }.zip(videos.toVideoItems()),
                apps   = appRows,
                filter = shownFilter,
                limit  = limit,
            ), tracks)
        }

    internal suspend fun loadColumn(keepCursorOnRow: Boolean) {
        var keepCursor = keepCursorOnRow
        recentRows(uiState.map { it.recentFilter }.distinctUntilChanged()).map { rows ->
            vm.music.currentMusicTracks = rows.tracks
            rows.filters to rows.items
        }.collect { (filters, items) ->
            // the filter that was showing has just emptied: go back to All, which reloads the column
            if (uiState.value.recentFilter !in filters) {
                uiState.update { it.copy(recentFilters = filters, recentFilter = RecentFilter.ALL, selectedItemIndex = 0) }
                return@collect
            }
            uiState.update { it.copy(recentFilters = filters) }
            vm.publishGameItems(items, keepCursor)
            keepCursor = true
        }
    }
}
