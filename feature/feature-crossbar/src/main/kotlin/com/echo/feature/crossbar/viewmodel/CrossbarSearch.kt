package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.MusicTrack
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarSearch(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun librarySearchItem(scope: SearchScope): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.SEARCH_ITEM_ID,
        title    = scope.label,
        subtitle = hintFor(scope),
        type     = CrossbarItemType.SEARCH,
    )

    private fun kindsShown(): Set<SearchKind> = searchKindsShown(uiState.value.categories.map { it.id })

    private fun hintFor(scope: SearchScope): String = if (scope == SearchScope.ALL) searchAllHint(kindsShown()) else scope.hint

    internal fun quickSearchItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.QUICK_SEARCH_ITEM_ID,
        title    = "Quick Search",
        subtitle = "Search the web, or type an address",
        type     = CrossbarItemType.SEARCH,
    )

    private var searchGames: List<com.echo.core.domain.model.Game> = emptyList()

    private var searchApps: List<com.echo.feature.appbar.InstalledApp> = emptyList()
    private var searchVideos: List<com.echo.core.domain.model.Video> = emptyList()
    private var searchPhotos: List<com.echo.core.domain.model.Photo> = emptyList()
    private var searchBooks: List<com.echo.core.domain.model.Book> = emptyList()
    private var searchTracks: List<com.echo.core.domain.model.MusicTrack> = emptyList()

    fun typeToSearchAllowed(): Boolean {
        val state = uiState.value
        return state.search == null && state.stripShowsCrossbarContext
    }

    fun openSearchTyping(query: String) {
        openSearch(SearchScope.ALL)
        onSearchQueryChange(query)
    }

    fun openAppSearch(initialQuery: String) {
        openSearch(SearchScope.APPS)
        if (initialQuery.isNotEmpty()) onSearchQueryChange(initialQuery)
    }

    fun openSearch(scope: SearchScope) {
        menuSound.play(MenuSound.SELECT)
        uiState.update { it.copy(search = SearchState(scope = scope)) }
        this@CrossbarSearch.scope.launch {
            val shown = kindsShown()
            val wantsGames = (scope == SearchScope.ALL && SearchKind.GAMES in shown) || scope == SearchScope.GAMES
            val wantsVideos = (scope == SearchScope.ALL && SearchKind.VIDEO in shown) || scope == SearchScope.VIDEOS
            val wantsPhotos = (scope == SearchScope.ALL && SearchKind.PHOTOS in shown) || scope == SearchScope.PHOTOS
            val wantsBooks = (scope == SearchScope.ALL && SearchKind.BOOKS in shown) || scope == SearchScope.BOOKS
            searchGames = if (wantsGames) vm.gameRepository.observeAllGames().first() else emptyList()
            searchVideos = if (wantsVideos) vm.videoRepository.observeAllVideos().first() else emptyList()
            searchPhotos = if (wantsPhotos) vm.photoRepository.observeAllPhotos().first() else emptyList()
            searchBooks = if (wantsBooks) vm.bookRepository.observeAllBooks().first() else emptyList()

            val wantsTracks = (scope == SearchScope.ALL && SearchKind.MUSIC in shown) || scope == SearchScope.MUSIC
            searchTracks = if (wantsTracks) vm.musicRepository.observeAllTracks().first() else emptyList()

            val wantsApps = scope == SearchScope.ALL || scope == SearchScope.APPS
            searchApps = if (wantsApps) vm.appCategoryRepository.visibleInstalledApps() else emptyList()
            uiState.update { it.copy(search = it.search?.copy(loaded = true)) }
            centreNext = true
            rebuildSearchRows()
        }
    }

    fun onSearchQueryChange(query: String) {
        vm.markTouchInput()
        val state = uiState.value.search ?: return
        uiState.update { it.copy(search = it.search?.copy(
            query = query,
            selectedIndex = 0,
            scrollToTopToken = state.scrollToTopToken + 1,
        )) }
        centreNext = true
        rebuildSearchRows()
    }

    fun closeSearch() {
        menuSound.play(MenuSound.BACK)

        searchGames = emptyList(); searchVideos = emptyList(); searchPhotos = emptyList()
        searchApps = emptyList()
        searchBooks = emptyList(); searchTracks = emptyList()
        uiState.update { it.copy(search = null) }
    }

    // the next rebuild puts the likeliest result in the middle and selects it: a new query, a new filter, a fresh open
    private var centreNext = true

    private fun rebuildSearchRows() {
        val state = uiState.value.search ?: return
        val q = state.query
        if (state.scope == SearchScope.WEB) {
            val row = quickSearchRow(q) ?: searchNoticeItem(state.scope.emptyTitle, state.scope.emptyHint)
            uiState.update { it.copy(search = it.search?.copy(rows = listOf(row), selectedIndex = 0)) }
            return
        }
        val libraryCovers = com.echo.core.domain.model.appCovers(searchGames)
        val found = buildList {
            searchApps.filter { matchesSearch(q, it.label, it.packageName) }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { app ->
                    add(
                        CrossbarItem(
                            id = "searchapp_${app.packageName}",
                            title = app.label,
                            subtitle = "App",
                            packageName = app.packageName,
                            // an app that is also in the game library shows its cover (owner, 2026-10-05)
                            coverUri = libraryCovers[app.packageName],
                            lastOpenedAt = app.lastUsedAt.takeIf { it > 0L },

                            isAndroidApp = true,
                        ),
                    )
                }

            searchGames.filter {
                matchesSearch(q, it.title, it.developer, it.publisher, vm.platformCache[it.platformId]?.name)
            }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { add(it.toSearchRow(vm.platformCache[it.platformId]?.name)) }
            searchVideos.filter { matchesSearch(q, it.displayTitle, it.displayName) }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { add(it.toSearchRow()) }
            searchPhotos.filter { matchesSearch(q, it.displayName, it.relativePath) }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { add(it.toSearchRow()) }
            searchBooks.filter { matchesSearch(q, it.displayTitle, it.author, it.seriesName) }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { add(it.toSearchRow()) }
            searchTracks.filter { matchesSearch(q, it.displayTitle, it.artist, it.album) }
                .take(CrossbarViewModel.SEARCH_RESULTS_PER_LIBRARY)
                .forEach { add(it.toSearchRow()) }
        }

        val rows = found.sortedWith(compareBy<CrossbarItem>({ searchRank(q, it.title) }, { -(it.lastOpenedAt ?: 0L) }))
        val counts = SearchKind.entries.mapNotNull { k -> rows.count { searchKindOf(it) == k }.takeIf { it > 0 }?.let { k to it } }
        val kind = state.kind?.takeIf { k -> counts.any { it.first == k } }
        val shown = centreOut(if (kind == null) rows else rows.filter { searchKindOf(it) == kind })
        val anyContent = searchGames.isNotEmpty() || searchVideos.isNotEmpty() ||
            searchPhotos.isNotEmpty() || searchBooks.isNotEmpty() || searchTracks.isNotEmpty() ||
            searchApps.isNotEmpty()
        val display = when {
            shown.isNotEmpty() -> shown
            else -> when (searchEmptyState(state.loaded, q, anyContent)) {
                SearchEmptyState.LOADING -> searchNoticeItem("Reading your libraries", "One moment.")
                SearchEmptyState.EMPTY_LIBRARY -> searchNoticeItem(state.scope.emptyTitle, state.scope.emptyHint)
                SearchEmptyState.PROMPT -> searchNoticeItem("Type to search", hintFor(state.scope))
                SearchEmptyState.NO_MATCHES -> searchNoticeItem("No matches", "Nothing here matches that.")
            }.let(::listOf)
        }
        uiState.update { it.copy(search = it.search?.copy(
            rows = display,
            selectedIndex = if (centreNext) centreOutIndex(display.size) else state.selectedIndex.coerceIn(0, (display.size - 1).coerceAtLeast(0)),
            kind = kind,
            kindCounts = counts,
            total = rows.size,
        )) }
        if (shown.isNotEmpty()) centreNext = false
    }

    private fun searchNoticeItem(title: String, subtitle: String): CrossbarItem =
        CrossbarItem(id = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID, title = title, subtitle = subtitle, type = CrossbarItemType.EMPTY)

    fun onSearchFocusedAt(index: Int) {
        vm.markTouchInput()
        moveSearch(index - (uiState.value.search?.selectedIndex ?: return))
    }

    private fun stepKind(delta: Int) {
        val state = uiState.value.search ?: return
        val next = stepSearchKind(state.kind, state.kindCounts.map { it.first }, delta)
        if (next == state.kind) return
        pickSearchKind(next)
    }

    fun pickSearchKind(kind: SearchKind?) {
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(search = it.search?.copy(kind = kind, selectedIndex = 0)) }
        centreNext = true
        rebuildSearchRows()
    }

    internal fun moveSearch(delta: Int) {
        val state = uiState.value.search ?: return
        if (state.rows.isEmpty()) return
        val next = (state.selectedIndex + delta).coerceIn(0, state.rows.lastIndex)
        if (next == state.selectedIndex) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(search = it.search?.copy(selectedIndex = next)) }
    }

    fun onSearchActivatedAt(index: Int) {
        val state = uiState.value.search ?: return
        val row = state.rows.getOrNull(index) ?: return
        if (row.type == CrossbarItemType.EMPTY) return
        if (state.scope == SearchScope.WEB) {
            closeSearch()
            vm.runQuickSearch(state.query)
            return
        }
        uiState.update { it.copy(search = it.search?.copy(selectedIndex = index)) }

        val appPackage = row.packageName?.takeIf { row.isInstalledApp }
        if (appPackage != null) {
            closeSearch()
            vm.launching.launchAppWithDisc(appPackage, row.shelfCoverArt)
            return
        }

        // resolved before closeSearch, which empties the lists a photo or track is found in
        val target = searchOpenTarget(row, searchPhotos, searchTracks)
        val categoryId = searchRowCategory(row) ?: return
        closeSearch()
        vm.selectCategoryById(categoryId)
        when (row.type) {
            CrossbarItemType.VIDEO_FILE ->
                uiState.update { it.copy(activeVideoId = row.id.removePrefix("vid_"), activeVideoAutoPlay = true) }
            CrossbarItemType.PHOTO_FILE -> (target as? SearchOpen.Photo)?.let { openSearchedPhoto(it.photo) }
            CrossbarItemType.LIBRARY_BOOK -> vm.bookshelf.openBook(row.id.removePrefix("book_"))
            CrossbarItemType.MUSIC_TRACK -> (target as? SearchOpen.Track)?.let { openSearchedTrack(it.track) }

            else -> row.gameId?.let { id -> vm.launching.launchGameDirectly(id) }
        }
    }

    // the banner's Options (owner, 2026-10-05): the row's own menu, drawn over the search
    fun onSearchOptionsAt(index: Int) {
        val row = uiState.value.search?.takeUnless { it.scope == SearchScope.WEB }?.rows?.getOrNull(index)?.takeUnless { it.type == CrossbarItemType.EMPTY } ?: return
        uiState.update { it.copy(search = it.search?.copy(selectedIndex = index)) }
        vm.openContextMenuFor(row)
    }

    private fun searchRowCategory(row: CrossbarItem): String? = row.owningCategory()

    private fun openSearchedPhoto(photo: com.echo.core.domain.model.Photo) {
        val libraryId = photo.libraryId
        val name = uiState.value.photoLibraries.firstOrNull { it.id == libraryId }?.displayName.orEmpty()
        uiState.update { it.copy(photoNav = PhotoNav.Library(libraryId, name)) }
        vm.gallery.openPhoto(photo.id)
    }

    private fun openSearchedTrack(track: MusicTrack) {
        scope.launch {
            vm.launching.awaitDiscHandOff(track.artUri)
            vm.musicPlayer.setQueue(listOf(track), 0)
            vm.showMusicPlayer()
        }
    }

    internal fun onButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_LEFT,
            GamepadAction.NAVIGATE_RIGHT -> moveSearch(searchStep(action))
            // the kinds are filters, so the triggers (owner, 2026-10-06)
            GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY -> stepKind(if (action == GamepadAction.PREV_CATEGORY) -1 else 1)
            GamepadAction.SELECT        -> onSearchActivatedAt(state.search?.selectedIndex ?: return)
            GamepadAction.OPEN_CONTEXT_MENU -> onSearchOptionsAt(state.search?.selectedIndex ?: return)
            GamepadAction.BACK          -> closeSearch()
            else -> Unit
        }
    }
}

internal sealed interface SearchOpen {
    data class Photo(val photo: com.echo.core.domain.model.Photo) : SearchOpen
    data class Track(val track: MusicTrack) : SearchOpen
}

// the photo or track a search row opens, looked up while the results are still held; null for rows
// that open from their own id (games, apps, videos, books)
internal fun searchOpenTarget(
    row: CrossbarItem,
    photos: List<com.echo.core.domain.model.Photo>,
    tracks: List<MusicTrack>,
): SearchOpen? = when (row.type) {
    CrossbarItemType.PHOTO_FILE -> photos.firstOrNull { it.id == row.id.removePrefix("pho_") }?.let(SearchOpen::Photo)
    CrossbarItemType.MUSIC_TRACK -> tracks.firstOrNull { it.id == row.id.removePrefix("mt_") }?.let(SearchOpen::Track)
    else -> null
}

internal fun com.echo.core.domain.model.Game.toSearchRow(platformName: String?): CrossbarItem = CrossbarItem(
    id = "search_game_$id",
    title = title,
    subtitle = listOfNotNull("Game", platformName).joinToString("  ·  "),

    coverUri = artworkUri,
    iconUri = iconUri,
    metadataLine = gameMetadataLine(releaseYear, genre, developer, players),
    description = description,
    totalPlayTimeMillis = totalPlayTimeMillis,
    lastOpenedAt = lastPlayedAt,
    gameId = id,
    platformId = platformId,
    type = CrossbarItemType.STANDARD,
)

internal fun com.echo.core.domain.model.Video.toSearchRow(): CrossbarItem = CrossbarItem(
    id = "vid_$id",
    title = displayTitle,
    subtitle = listOfNotNull("Video", videoRowSubtitle(durationMs, resolutionLabel, sizeBytes)).joinToString("  ·  "),
    coverUri = effectiveThumbnailUri,
    mediaUri = uri,
    mimeType = mimeType,
    type = CrossbarItemType.VIDEO_FILE,
)

internal fun com.echo.core.domain.model.Photo.toSearchRow(): CrossbarItem = CrossbarItem(
    id = "pho_$id",
    title = displayName,
    subtitle = listOfNotNull("Photo", relativePath).joinToString("  ·  "),
    coverUri = thumbnailUri ?: uri,
    mediaUri = uri,
    type = CrossbarItemType.PHOTO_FILE,
)

internal fun com.echo.core.domain.model.Book.toSearchRow(): CrossbarItem = CrossbarItem(
    id = "book_$id",
    title = displayTitle,
    subtitle = listOfNotNull("Book", bookRowSubtitle(author, seriesName, seriesIndex)).joinToString("  ·  "),
    coverUri = coverUri,
    type = CrossbarItemType.LIBRARY_BOOK,
)

internal fun com.echo.core.domain.model.MusicTrack.toSearchRow(): CrossbarItem = CrossbarItem(
    id = "mt_$id",
    title = displayTitle,
    subtitle = listOfNotNull("Music", musicRowSubtitle(artist, album, durationMs)).joinToString("  ·  "),
    coverUri = artUri,
    mediaUri = uri,
    mimeType = mimeType,
    type = CrossbarItemType.MUSIC_TRACK,
)
