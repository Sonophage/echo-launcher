package com.echo.feature.appbar

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.echo.core.domain.model.PlatformIds.ANDROID as ANDROID_PLATFORM_ID

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.sound.MenuSound
import com.echo.core.ui.sound.MenuSoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.echo.core.ui.components.moved
import com.echo.core.ui.components.back
import com.echo.core.ui.components.chose
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.components.initialOf
import com.echo.core.ui.components.letterMenuFor


private const val ROM_KEY_PREFIX = "rom:"

// the drawer's name for the PC games GameNative launches (owner, 2026-10-05)
private const val STEAM_GAMES_LABEL = "Steam Games"

enum class AppFilter(val label: String, val subtitle: String) {
    RECENT("Recently Used", "Apps you've used lately"),
    APPS("Apps", "Everything that is not a game or an emulator"),
    EMULATORS("Emulators", "RetroArch, PPSSPP, Dolphin and more"),
    GAMES("Games", "Apps categorized as games"),
    MUSIC("Music", "Your albums"),
    VIDEOS("Videos", "Your videos"),
    BOOKS("Books", "Your books");

    fun matches(app: InstalledApp): Boolean = when (this) {
        APPS -> app.media == null && !app.isGame && !app.isEmulator
        GAMES -> app.isGame
        EMULATORS -> app.isEmulator
        RECENT -> app.media == null && app.lastUsedAt > 0L
        MUSIC -> app.media?.kind == MediaKind.MUSIC
        VIDEOS -> app.media?.kind == MediaKind.VIDEO
        BOOKS -> app.media?.kind == MediaKind.BOOK
    }

    val isMedia: Boolean get() = this == MUSIC || this == VIDEOS || this == BOOKS

    fun stepped(delta: Int, shown: List<AppFilter>): AppFilter {
        val here = shown.indexOf(this)
        return if (here < 0) shown.first() else shown[(here + delta).mod(shown.size)]
    }

    companion object {
        val DEFAULT = RECENT

        // owner, 2026-10-05: a section with nothing in it is not shown. Recently Used stays while usage
        // access is off, because its empty page is where that access is granted
        fun shown(counts: Map<AppFilter, Int>, hasUsageAccess: Boolean): List<AppFilter> =
            entries.filter { (counts[it] ?: 0) > 0 || (it == RECENT && !hasUsageAccess) }.ifEmpty { listOf(DEFAULT) }
    }
}

enum class AppMenuAction(val label: String, val group: MenuGroup) {
    ADD_TO_CROSS_BAR("Add to Cross Bar", MenuGroup.MAIN),
    APP_INFO("App Info", MenuGroup.SETTINGS),
    MARK_GAME("Mark as Game", MenuGroup.LIBRARY),
    UNMARK_GAME("Unmark as Game", MenuGroup.LIBRARY),
    // owner, 2026-10-05: hide an app that is never used from the whole layout; undone in Settings > Library > Hidden Items
    HIDE_EVERYWHERE("Hide Everywhere", MenuGroup.REMOVE),
    UNINSTALL("Uninstall", MenuGroup.REMOVE),
}

// achievements: "12/40" once the game's set has synced (owner, 2026-10-08: shown under the details)
// recentBadges: the icons of the last achievements earned, newest first (owner, 2026-10-08: one row of them)
data class GameDetails(
    val gameId: Long,
    val facts: String?,
    val description: String?,
    val achievements: String? = null,
    val recentBadges: List<String> = emptyList(),
)

// the line under a game's title: year, genre, developer, players, as many as are known
fun gameFacts(game: com.echo.core.domain.model.Game): String? =
    listOfNotNull(game.releaseDate?.take(4), game.genre, game.developer, game.players?.let { if (it.trim() == "1") "1 player" else "$it players" })
        .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null }

data class AppDrawerUiState(
    val allApps: List<InstalledApp> = emptyList(),

    // the app whose launch ring is filling while A is held
    val holdingPackage: String? = null,

    // the focused game's details for the info band: year, genre, developer and its description
    val gameDetails: GameDetails? = null,

    val visibleApps: List<InstalledApp> = emptyList(),

    val activeFilter: AppFilter = AppFilter.DEFAULT,
    val isLoading: Boolean = true,
    val selectedIndex: Int = 0,

    val usingTouch: Boolean = false,
    val hasUsageAccess: Boolean = false,

    val menuApp: InstalledApp? = null,
    val appMenu: MenuState<AppMenuAction>? = null,

    val confirmUninstall: InstalledApp? = null,

    val uninstallConfirmFocused: Boolean = false,

    val menuAppIsGame: Boolean = false,

    val filterCounts: Map<AppFilter, Int> = emptyMap(),

    val sections: List<AppFilter> = AppFilter.entries,

    val letterMenu: List<Char> = emptyList(),

    val letterCursor: Int? = null,

    val letterFilter: Char? = null,

    // a library game's Options: the crossbar's own game menu, opened over the drawer (owner, 2026-10-05)
    val pendingGameMenu: Long? = null,
    val pendingRomLaunch: Long? = null,

    val pendingCrossBarAdd: String? = null,

    val systemChips: List<SystemChip> = emptyList(),

    val systemFilter: String? = null,

    // a media case asked to open; the crossbar opens it as its own column does
    val pendingMediaOpen: DrawerMedia? = null,
    val pendingMediaMenu: Pair<DrawerMedia, String>? = null,

    // Music's and Books' buttons are genres (X), else artists or authors
    val mediaGrouping: MediaGrouping = MediaGrouping.MAKER,

    // the crossbar's genre filter, which the Games section follows (owner, 2026-10-08)
    val genreFilter: com.echo.core.domain.model.GameGenre? = null,
    // the Game column is grouped by genre, so the Games section's buttons are genres
    val chipsByGenre: Boolean = false,
    // owner, 2026-10-09: X's third step in Games, after systems and genres: the rail shows letters
    val gamesByLetter: Boolean = false,

) {
    // owner, 2026-10-09: the side rail is the section's groups when it has them (systems or genres in Games;
    // artists, albums or genres in Music; authors in Books), else its letters. Either way it filters
    val railByGroup: Boolean get() = showSystemChips && !(activeFilter == AppFilter.GAMES && gamesByLetter)
    val railRungs: List<com.echo.core.ui.components.RailRung> get() =
        if (railByGroup) systemChips.map { com.echo.core.ui.components.RailRung(if (it.id == null) ALL_GLYPH else initialOf(it.label).toString(), it.label) }
        else com.echo.core.ui.components.letterRungs(letterMenu)
    // the rung of the filter in use
    val railPick: Int get() =
        if (railByGroup) systemChips.indexOfFirst { it.id == systemFilter }.coerceAtLeast(0)
        else letterMenu.indexOf(letterFilter).coerceAtLeast(0)

    // owner, 2026-10-08: grouped by genre the buttons show even with one genre, so X visibly does something;
    // systems, artists and authors still need two to be worth a row
    val showSystemChips: Boolean get() = when (activeFilter) {
        AppFilter.GAMES -> systemChips.size > if (chipsByGenre) 1 else 2
        AppFilter.MUSIC, AppFilter.BOOKS -> systemChips.size > if (mediaGrouping == MediaGrouping.GENRE) 1 else 2
        else -> false
    }

    val menuActions: List<AppMenuAction>
        get() = buildList {
            if (menuApp?.gameId != null) return@buildList
            add(AppMenuAction.ADD_TO_CROSS_BAR)
            add(AppMenuAction.APP_INFO)
            add(if (menuAppIsGame) AppMenuAction.UNMARK_GAME else AppMenuAction.MARK_GAME)
            add(AppMenuAction.HIDE_EVERYWHERE)
            if (menuApp?.isSystemApp == false) add(AppMenuAction.UNINSTALL)
        }

    internal fun menuStateFor(app: InstalledApp): MenuState<AppMenuAction> = MenuState(
        title = app.label,
        rows = menuActions.map {
            MenuRow(
                action = it,
                label = it.label,
                group = it.group,
                isDestructive = it == AppMenuAction.UNINSTALL,
                confirms = false,
            )
        },
        selectedIndex = 0,
    )
}

// X's next grouping in Music or Books: the next one with groups to show, so the hint never names a grouping the rail
// cannot draw; with none to show it simply steps on
internal fun nextMediaGrouping(state: AppDrawerUiState, cases: List<InstalledApp>): MediaGrouping =
    generateSequence(state.mediaGrouping.next) { it.next }.take(MediaGrouping.entries.size)
        .firstOrNull { by -> state.copy(mediaGrouping = by, systemChips = mediaChips(cases, by)).showSystemChips }
        ?: state.mediaGrouping.next

// the rail's filter in use: a group's id, or a letter
internal val AppDrawerUiState.railFilter: Any? get() = if (railByGroup) systemFilter else letterFilter

// the filter in use as words, for the footer beside the section; null when the rail shows everything
internal val AppDrawerUiState.railFilterName: String? get() =
    if (railByGroup) systemChips.firstOrNull { it.id != null && it.id == systemFilter }?.label else letterFilter?.toString()

internal fun AppDrawerUiState.withRailFilter(value: Any?): AppDrawerUiState =
    if (railByGroup) copy(systemFilter = value as String?) else copy(letterFilter = value as Char?)

internal fun AppDrawerUiState.withRailRung(rung: Int): AppDrawerUiState =
    if (railByGroup) copy(systemFilter = systemChips.getOrNull(rung)?.id) else copy(letterFilter = letterMenu.getOrNull(rung))

// owner, 2026-10-09: the rail lists groups A to Z by name, All first, so its letters run in order
internal fun byName(chips: List<SystemChip>): List<SystemChip> =
    chips.filter { it.id == null } + chips.filter { it.id != null }.sortedBy { it.label.lowercase() }

// the All rung's mark on the rail
internal const val ALL_GLYPH = "•"

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: InstalledAppRepository,
    private val menuSound: MenuSoundPlayer,
    private val gameRepository: com.echo.core.domain.repository.GameRepository,
    private val memoryCardRepository: com.echo.core.data.repository.MemoryCardRepository,
    private val mediaLaunchGate: com.echo.core.data.launch.MediaLaunchGate,
    private val platformDao: com.echo.core.data.database.dao.PlatformDao,
    private val appCategoryRepository: AppCategoryRepository,
    private val musicRepository: com.echo.core.domain.repository.MusicRepository,
    private val videoRepository: com.echo.core.domain.repository.VideoRepository,
    private val bookRepository: com.echo.core.domain.repository.BookRepository,
    private val achievementSets: com.echo.core.data.database.dao.AccountAchievementSetDao,
    private val achievementCoins: com.echo.core.data.database.dao.AccountAchievementDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val hasUsageAccess = appRepository.hasUsageAccess()
            val hidden = appCategoryRepository.hiddenEverywhere()
            // owner, 2026-10-05: an installed app that is also in the game library shows its cover
            val covers = libraryCovers()
            val apps = appRepository.getInstalledApps().filterNot { it.packageName in hidden }.map { app -> covers[app.packageName]?.let { app.copy(art = it) } ?: app } + romsInLibrary() + mediaInLibrary()
            _uiState.update {
                it.copy(
                    allApps = apps.sortedBy { app -> app.label.lowercase() },
                    isLoading = false,
                    hasUsageAccess = hasUsageAccess,
                )
            }
            applyFilter()
        }
    }

    private suspend fun libraryCovers(): Map<String, String> =
        com.echo.core.domain.model.appCovers(gameRepository.observeAllGames().first())

    private suspend fun mediaInLibrary(): List<InstalledApp> = runCatching {
        albumCases(musicRepository.observeAllTracks().first()) +
            videoCases(videoRepository.observeAllVideos().first()) +
            bookCases(bookRepository.observeAllBooks().first())
    }.getOrElse { timber.log.Timber.w(it, "Media for the drawer failed to load"); emptyList() }

    private suspend fun romsInLibrary(): List<InstalledApp> {
        // a game a launcher app opens with its own shortcut (owner, 2026-10-05: the Steam games GameNative runs)
        // counts as a library game even though it names that app
        val roms = gameRepository.observeAllGames().first().filter { it.packageName == null || it.launchIntentUri != null }
        val names = roms.map { it.platformId }.distinct().associateWith { id ->
            if (id == com.echo.core.domain.model.PlatformIds.WINDOWS) STEAM_GAMES_LABEL
            else runCatching { platformDao.getById(id)?.shortName }.getOrNull() ?: id.uppercase()
        }
        return roms.map { game ->
                InstalledApp(
                    packageName = "$ROM_KEY_PREFIX${game.id}",
                    label = game.title,
                    icon = null,
                    isGame = true,
                    isEmulator = false,
                    lastUsedAt = game.lastPlayedAt ?: 0L,
                    gameId = game.id,
                    art = com.echo.core.domain.model.coverArtOf(game.iconUri, game.artworkUri),
                    playTimeMillis = game.totalPlayTimeMillis,
                    platformId = game.platformId,
                    platformName = names[game.platformId],
                    genre = com.echo.core.domain.model.effectiveGenre(game.genre, game.genreOverride),
                )
            }
    }

    fun setChipsByGenre(on: Boolean) {
        if (on == _uiState.value.chipsByGenre) return
        _uiState.update { it.copy(chipsByGenre = on, systemFilter = null, selectedIndex = 0) }
        applyFilter()
    }

    fun setGenreFilter(genre: com.echo.core.domain.model.GameGenre?) {
        if (genre == _uiState.value.genreFilter) return
        _uiState.update { it.copy(genreFilter = genre, selectedIndex = 0) }
        applyFilter()
    }

    fun setFilter(filter: AppFilter) {
        if (filter != _uiState.value.activeFilter) menuSound.play(MenuSound.SYSTEM_BROWSE)
        _uiState.update { it.copy(activeFilter = filter, selectedIndex = 0, letterFilter = null, systemFilter = null) }
        applyFilter()
    }

    fun onAppTapped(index: Int) {
        if (index != _uiState.value.selectedIndex) menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(selectedIndex = index, usingTouch = true) }
    }

    fun launchApp(packageName: String) {
        val app = _uiState.value.visibleApps.firstOrNull { it.packageName == packageName }
        app?.media?.let { media ->
            menuSound.play(MenuSound.SELECT)
            _uiState.update { it.copy(pendingMediaOpen = media) }
            return
        }
        menuSound.play(MenuSound.LAUNCH)

        if (app?.gameId != null) {
            _uiState.update { it.copy(pendingRomLaunch = app.gameId) }
            return
        }
        viewModelScope.launch {
            mediaLaunchGate.awaitHandOff(app?.icon)
            appRepository.launchApp(packageName)
        }
    }

    fun onRomLaunchHandled() = _uiState.update { it.copy(pendingRomLaunch = null) }

    fun onMediaOpenHandled() = _uiState.update { it.copy(pendingMediaOpen = null) }

    fun onMediaMenuHandled() = _uiState.update { it.copy(pendingMediaMenu = null) }

    // X in Music or Books: artists or authors, then album or title letters, then genres
    // X in Games: systems, then genres, then A to Z. Systems and genres are the Game column's grouping, which
    // [toggleColumn] flips (owner, 2026-10-08); A to Z is the drawer's own
    fun cycleGamesGrouping(toggleColumn: () -> Unit) {
        menuSound.play(MenuSound.SELECT)
        val state = _uiState.value
        when {
            state.gamesByLetter -> { _uiState.update { it.copy(gamesByLetter = false, letterFilter = null, selectedIndex = 0) }; toggleColumn() }
            state.chipsByGenre -> _uiState.update { it.copy(gamesByLetter = true, systemFilter = null, selectedIndex = 0) }
            else -> toggleColumn()
        }
        applyFilter()
    }

    // owner, 2026-10-09: X skips a grouping with nothing to show (no genre tags), so the hint always names what the
    // rail shows; with none to show, the rail falls back to letters and the hint said By Genre over them
    fun toggleMediaGrouping() {
        menuSound.play(MenuSound.SELECT)
        val next = nextMediaGrouping(_uiState.value, tabApps(_uiState.value))
        _uiState.update { it.copy(mediaGrouping = next, systemFilter = null, selectedIndex = 0) }
        applyFilter()
    }

    // the section's cases before any rail filter; the genre narrows Games first, so each system counts what it shows
    private fun tabApps(state: AppDrawerUiState): List<InstalledApp> =
        state.allApps.filter { app -> state.activeFilter.matches(app) }
            .let { if (state.activeFilter == AppFilter.GAMES) it.ofGenre(state.genreFilter) else it }

    fun onGameMenuHandled() = _uiState.update { it.copy(pendingGameMenu = null) }

    fun onCrossBarAddHandled() = _uiState.update { it.copy(pendingCrossBarAdd = null) }

    fun refresh() {
        loadApps()
    }

    fun openAppMenu(app: InstalledApp) {
        // an album, video or book: the crossbar's own menu for it
        app.media?.let { media ->
            menuSound.play(MenuSound.SELECT)
            _uiState.update { it.copy(pendingMediaMenu = media to app.label) }
            return
        }
        if (app.gameId != null) {
            menuSound.play(MenuSound.SELECT)
            _uiState.update { it.copy(pendingGameMenu = app.gameId) }
            return
        }
        menuSound.play(MenuSound.SELECT)
        _uiState.update { it.copy(menuApp = app, menuAppIsGame = false).let { s -> s.copy(appMenu = s.menuStateFor(app)) } }

        viewModelScope.launch {
            val entry = gameRepository.getAppEntry(app.packageName)
            val isGame = entry != null &&
                entry.platformId == ANDROID_PLATFORM_ID &&
                entry.contentType == com.echo.core.domain.model.GameContentType.GAME
            _uiState.update {
                if (it.menuApp?.packageName == app.packageName) it.copy(menuAppIsGame = isGame) else it
            }
        }
    }

    fun openAppMenuForSelected() {
        val app = _uiState.value.visibleApps.getOrNull(_uiState.value.selectedIndex) ?: return
        openAppMenu(app)
    }

    fun closeAppMenu() = _uiState.update { it.copy(menuApp = null, appMenu = null) }

    private fun moveAppMenu(delta: Int) = _uiState.update { s ->
        s.copy(appMenu = s.appMenu?.moved(delta))
    }

    private fun backOutOfAppMenu() {
        val parent = _uiState.value.appMenu?.back()
        if (parent == null) closeAppMenu() else _uiState.update { it.copy(appMenu = parent) }
    }

    fun onMenuRowActivated(index: Int) {
        when (val chosen = _uiState.value.appMenu?.chose(index)) {
            is MenuSelect.Replace -> _uiState.update { it.copy(appMenu = chosen.state) }
            is MenuSelect.Run -> onMenuAction(chosen.action)
            else -> Unit
        }
    }

    fun onMenuAction(action: AppMenuAction) {
        val app = _uiState.value.menuApp ?: return
        // owner, 2026-10-05: the menu goes once a row is chosen; the branches below only cleared menuApp,
        // which left the menu drawn after Hide Everywhere, Mark as Game and the rest
        _uiState.update { it.copy(appMenu = null) }
        when (action) {
            AppMenuAction.APP_INFO -> {
                appRepository.openAppInfo(app.packageName)
                _uiState.update { it.copy(menuApp = null) }
            }
            AppMenuAction.MARK_GAME   -> { setMarkedAsGame(app, marked = true);  _uiState.update { it.copy(menuApp = null) } }
            AppMenuAction.UNMARK_GAME -> { setMarkedAsGame(app, marked = false); _uiState.update { it.copy(menuApp = null) } }

            AppMenuAction.UNINSTALL -> _uiState.update { it.copy(menuApp = null, confirmUninstall = app, uninstallConfirmFocused = false) }

            AppMenuAction.ADD_TO_CROSS_BAR -> _uiState.update { it.copy(menuApp = null, pendingCrossBarAdd = app.packageName) }

            AppMenuAction.HIDE_EVERYWHERE -> {
                _uiState.update { it.copy(menuApp = null) }
                viewModelScope.launch {
                    appCategoryRepository.setHidden(app.packageName, true)
                    loadApps()
                }
            }
        }
    }

    private fun setMarkedAsGame(app: InstalledApp, marked: Boolean) {
        viewModelScope.launch {
            val existing = gameRepository.getAppEntry(app.packageName)
            if (marked) {
                if (existing == null) {
                    gameRepository.upsert(
                        com.echo.core.domain.model.Game(
                            title         = app.label,
                            platformId    = ANDROID_PLATFORM_ID,
                            packageName   = app.packageName,
                            isManualEntry = true,
                            contentType   = com.echo.core.domain.model.GameContentType.GAME,
                        )
                    )
                } else {
                    gameRepository.upsert(existing.copy(
                        platformId  = ANDROID_PLATFORM_ID,
                        contentType = com.echo.core.domain.model.GameContentType.GAME,
                    ))
                }
            } else if (existing != null) {
                gameRepository.upsert(existing.copy(
                    platformId  = com.echo.core.domain.model.PlatformIds.APP_SHORTCUT,
                    contentType = com.echo.core.domain.model.GameContentType.ANDROID_APP,
                ))
            }
            memoryCardRepository.recountGames(ANDROID_PLATFORM_ID)
        }
    }

    fun confirmUninstall() {
        val app = _uiState.value.confirmUninstall ?: return
        appRepository.uninstallApp(app.packageName)
        _uiState.update { it.copy(confirmUninstall = null, uninstallConfirmFocused = false) }
    }

    fun cancelUninstall() = _uiState.update { it.copy(confirmUninstall = null, uninstallConfirmFocused = false) }

    fun openUsageAccessSettings() {
        appRepository.openUsageAccessSettings()
    }

    fun openLetterJump() {
        val state = _uiState.value
        if (state.menuApp != null || state.confirmUninstall != null || state.letterCursor != null) return
        if (state.railRungs.isEmpty()) return
        filterAtGestureStart = state.railFilter
        landOn(state.railPick)
    }

    fun closeLetterJump() = _uiState.update { it.copy(letterCursor = null) }

    fun onLetterRailTouch(rung: Int) {
        val state = _uiState.value
        if (state.menuApp != null || state.confirmUninstall != null) return
        if (rung !in state.railRungs.indices) return
        if (state.letterCursor == null) filterAtGestureStart = state.railFilter
        if (state.letterCursor == rung) return
        landOn(rung)
    }

    fun onLetterRailReleased() {
        val state = _uiState.value
        if (state.letterCursor == null) return
        // landing where the gesture began takes the filter off
        val landed = state.railFilter
        val clear = landed != null && landed == filterAtGestureStart
        _uiState.update { if (clear) it.withRailFilter(null).copy(letterCursor = null) else it.copy(letterCursor = null) }
        applyFilter()
    }

    fun clearLetterFilter() {
        if (_uiState.value.railFilter == null) return
        menuSound.play(MenuSound.BACK)
        _uiState.update { it.withRailFilter(null).copy(selectedIndex = 0) }
        applyFilter()
    }

    private fun moveLetterJump(delta: Int) {
        val state = _uiState.value
        val cursor = state.letterCursor ?: return
        val next = (cursor + delta).coerceIn(0, state.railRungs.lastIndex)
        if (next == cursor) return
        landOn(next)
    }

    private fun landOn(rung: Int) {
        menuSound.play(MenuSound.SCROLL)
        _uiState.update {
            it.withRailRung(rung).copy(
                letterCursor = rung,
                selectedIndex = 0,
                usingTouch = false,
            )
        }
        applyFilter()
    }

    private var filterAtGestureStart: Any? = null

    private var launchHold: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            _uiState.map { it.visibleApps.getOrNull(it.selectedIndex)?.gameId }
                .distinctUntilChanged()
                .collectLatest { gameId ->
                    val game = gameId?.let { runCatching { gameRepository.getById(it) }.getOrNull() }
                    val set = gameId?.let { runCatching { achievementSets.observeSetForGame(it).first() }.getOrNull() }
                    val coins = set?.let { s -> runCatching { achievementCoins.getForSet(s.provider, s.providerGameId) }.getOrNull() }.orEmpty()
                    _uiState.update {
                        it.copy(gameDetails = game?.let { g ->
                            GameDetails(
                                g.id, gameFacts(g), g.description?.takeIf { d -> d.isNotBlank() }, achievementsLabel(set?.unlocked, set?.total),
                                recentBadges(coins.map { c -> EarnedBadge(c.iconUrl, c.isEarned, c.earnedAt) }),
                            )
                        })
                    }
                }
        }
    }

    // A must be held before an app opens, as everywhere in ECHO; the crossbar reports A coming up
    fun onSelectReleased() = cancelLaunchHold()

    private fun cancelLaunchHold() {
        launchHold?.cancel()
        launchHold = null
        if (_uiState.value.holdingPackage != null) _uiState.update { it.copy(holdingPackage = null) }
    }

    private fun holdToLaunch(packageName: String) {
        cancelLaunchHold()
        _uiState.update { it.copy(holdingPackage = packageName) }
        launchHold = viewModelScope.launch {
            kotlinx.coroutines.delay(com.echo.core.ui.design.LAUNCH_HOLD_MS)
            launchHold = null
            _uiState.update { it.copy(holdingPackage = null) }
            launchApp(packageName)
        }
    }

    fun handleGamepadAction(action: GamepadAction) {
        if (action != GamepadAction.SELECT) cancelLaunchHold()
        val state = _uiState.value

        if (state.letterCursor != null) {
            when (action) {
                GamepadAction.NAVIGATE_UP   -> moveLetterJump(-1)
                GamepadAction.NAVIGATE_DOWN -> moveLetterJump(+1)
                GamepadAction.BACK -> {
                    _uiState.update { it.withRailFilter(filterAtGestureStart).copy(letterCursor = null) }
                    applyFilter()
                }
                else -> Unit
            }
            return
        }

        state.confirmUninstall?.let {
            when (action) {
                GamepadAction.SELECT ->
                    if (state.uninstallConfirmFocused) confirmUninstall() else cancelUninstall()
                GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN ->
                    _uiState.update { s -> s.copy(uninstallConfirmFocused = !s.uninstallConfirmFocused) }
                GamepadAction.BACK -> cancelUninstall()

                else -> Unit
            }
            return
        }

        state.menuApp?.let {
            val actions = state.menuActions

            if (actions.isEmpty()) return
            when (action) {
                GamepadAction.NAVIGATE_UP   -> moveAppMenu(-1)
                GamepadAction.NAVIGATE_DOWN -> moveAppMenu(+1)
                GamepadAction.SELECT        -> onMenuRowActivated(state.appMenu?.selectedIndex ?: 0)

                GamepadAction.BACK               -> backOutOfAppMenu()
                GamepadAction.OPEN_CONTEXT_MENU  -> closeAppMenu()
                GamepadAction.CHANGE_SORT        -> closeAppMenu()
                else -> Unit
            }
            return
        }

        if (action == GamepadAction.PREV_CATEGORY || action == GamepadAction.NEXT_CATEGORY) {
            setFilter(state.activeFilter.stepped(if (action == GamepadAction.PREV_CATEGORY) -1 else 1, state.sections))
            return
        }

        val size  = state.visibleApps.size
        if (size == 0) return

        if (state.usingTouch) _uiState.update { it.copy(usingTouch = false) }
        val cur = state.selectedIndex
        when (action) {
            // Menu is Options everywhere; the drawer has nothing to sort, so X does nothing (owner, 2026-10-04)
            GamepadAction.OPEN_CONTEXT_MENU -> openAppMenuForSelected()
            GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT,
            GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN -> {
                val cells = wallLayout(size)
                val next = wallMove(action, cur, cells)

                if (next != cur) {
                    _uiState.update { it.copy(selectedIndex = next) }
                    menuSound.play(MenuSound.SCROLL)
                }
            }
            GamepadAction.SELECT -> {
                val app = state.visibleApps.getOrNull(cur)
                // media plays inside ECHO, so it acts at once; a launch out of ECHO is a hold
                if (app?.media != null) launchApp(app.packageName)
                else if (app != null) holdToLaunch(app.packageName)
            }

            else -> Unit
        }
    }

    private fun applyFilter() {
        val counts = AppFilter.entries.associateWith { filter -> _uiState.value.allApps.count(filter::matches) }
        // nothing is counted until the apps have loaded, so every section stays until then
        val sections = if (_uiState.value.allApps.isEmpty()) AppFilter.entries else AppFilter.shown(counts, _uiState.value.hasUsageAccess)
        // the section that was showing has emptied: move to the first one left
        if (_uiState.value.activeFilter !in sections) _uiState.update { it.copy(activeFilter = sections.first(), selectedIndex = 0) }
        val state = _uiState.value

        // the genre narrows the Games section first, so each system chip counts what it will show
        val tabApps = tabApps(state)
        val chips = when (state.activeFilter) {
            AppFilter.GAMES -> if (state.chipsByGenre) genreChips(tabApps) else systemChips(tabApps)
            AppFilter.MUSIC, AppFilter.BOOKS -> mediaChips(tabApps, state.mediaGrouping)
            else -> emptyList()
        }.let(::byName)
        val grouped = state.copy(systemChips = chips).railByGroup
        val system = state.systemFilter?.takeIf { id -> grouped && chips.any { it.id == id } }

        val inTab = if (state.activeFilter.isMedia) tabApps.ofMediaChip(system, state.mediaGrouping)
            else tabApps.ofChip(system, state.chipsByGenre)
            .let { apps ->
                if (state.activeFilter == AppFilter.RECENT) {
                    apps.sortedByDescending { it.lastUsedAt }
                } else {
                    apps
                }
            }

        // owner, 2026-10-09: no letters where the rail shows groups
        val letters = if (grouped) emptyList() else letterMenuFor(inTab.map { it.label })
        val pick = state.letterFilter?.takeIf { it in letters }
        val kept = { app: InstalledApp -> pick == null || initialOf(app.label) == pick }

        _uiState.update {
            it.copy(
                visibleApps = inTab.filter(kept),
                filterCounts = counts,
                sections = sections,
                letterMenu = letters,
                letterFilter = pick,
                systemChips = chips,
                systemFilter = system,
            )
        }
    }
}
