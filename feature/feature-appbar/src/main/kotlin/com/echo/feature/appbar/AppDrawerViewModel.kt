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
    GAMES("Games", "Apps categorized as games");

    fun matches(app: InstalledApp): Boolean = when (this) {
        APPS -> !app.isGame && !app.isEmulator
        GAMES -> app.isGame
        EMULATORS -> app.isEmulator
        RECENT -> app.lastUsedAt > 0L
    }

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

data class GameDetails(val gameId: Long, val facts: String?, val description: String?)

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

    // the crossbar's genre filter, which the Games section follows (owner, 2026-10-08)
    val genreFilter: com.echo.core.domain.model.GameGenre? = null,
    // the Game column is grouped by genre, so the Games section's buttons are genres
    val chipsByGenre: Boolean = false,

    val chipFocus: Boolean = false,

) {
    val showSystemChips: Boolean get() = activeFilter == AppFilter.GAMES && systemChips.size > 2

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

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: InstalledAppRepository,
    private val menuSound: MenuSoundPlayer,
    private val gameRepository: com.echo.core.domain.repository.GameRepository,
    private val memoryCardRepository: com.echo.core.data.repository.MemoryCardRepository,
    private val mediaLaunchGate: com.echo.core.data.launch.MediaLaunchGate,
    private val platformDao: com.echo.core.data.database.dao.PlatformDao,
    private val appCategoryRepository: AppCategoryRepository,
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
            val apps = appRepository.getInstalledApps().filterNot { it.packageName in hidden }.map { app -> covers[app.packageName]?.let { app.copy(art = it) } ?: app } + romsInLibrary()
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
        _uiState.update { it.copy(activeFilter = filter, selectedIndex = 0, letterFilter = null, systemFilter = null, chipFocus = false) }
        applyFilter()
    }

    fun selectSystem(id: String?) {
        if (id != _uiState.value.systemFilter) menuSound.play(MenuSound.SYSTEM_BROWSE)
        _uiState.update { it.copy(systemFilter = id, selectedIndex = 0) }
        applyFilter()
    }

    // owner, 2026-10-04: LB/RB walk the Games tab's system filters
    fun onSystemChipTapped(id: String?) {
        _uiState.update { it.copy(usingTouch = true) }
        selectSystem(id)
    }

    private fun handleChipRow(action: GamepadAction) {
        val state = _uiState.value
        val chips = state.systemChips
        val at = chips.indexOfFirst { it.id == state.systemFilter }.coerceAtLeast(0)
        when (action) {
            GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT -> {
                val next = (at + if (action == GamepadAction.NAVIGATE_LEFT) -1 else 1).coerceIn(0, chips.lastIndex)
                if (next != at) selectSystem(chips[next].id)
            }
            GamepadAction.NAVIGATE_DOWN, GamepadAction.SELECT -> {
                menuSound.play(MenuSound.SCROLL)
                _uiState.update { it.copy(chipFocus = false) }
            }
            else -> Unit
        }
    }

    fun onAppTapped(index: Int) {
        if (index != _uiState.value.selectedIndex) menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(selectedIndex = index, usingTouch = true, chipFocus = false) }
    }

    fun launchApp(packageName: String) {
        val app = _uiState.value.visibleApps.firstOrNull { it.packageName == packageName }
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

    fun onGameMenuHandled() = _uiState.update { it.copy(pendingGameMenu = null) }

    fun onCrossBarAddHandled() = _uiState.update { it.copy(pendingCrossBarAdd = null) }

    fun refresh() {
        loadApps()
    }

    fun openAppMenu(app: InstalledApp) {
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
        if (state.letterMenu.isEmpty()) return
        val start = state.letterMenu.indexOf(state.letterFilter).coerceAtLeast(0)
        filterAtGestureStart = state.letterFilter
        landOn(start)
    }

    fun closeLetterJump() = _uiState.update { it.copy(letterCursor = null) }

    fun onLetterRailTouch(rung: Int) {
        val state = _uiState.value
        if (state.menuApp != null || state.confirmUninstall != null) return
        if (rung !in state.letterMenu.indices) return
        if (state.letterCursor == null) filterAtGestureStart = state.letterFilter
        if (state.letterCursor == rung) return
        landOn(rung)
    }

    fun onLetterRailReleased() {
        val state = _uiState.value
        if (state.letterCursor == null) return
        val landed = state.letterFilter
        val keep = if (landed != null && landed == filterAtGestureStart) null else landed
        _uiState.update { it.copy(letterCursor = null, letterFilter = keep) }
        applyFilter()
    }

    fun clearLetterFilter() {
        if (_uiState.value.letterFilter == null) return
        menuSound.play(MenuSound.BACK)
        _uiState.update { it.copy(letterFilter = null, selectedIndex = 0) }
        applyFilter()
    }

    private fun moveLetterJump(delta: Int) {
        val state = _uiState.value
        val cursor = state.letterCursor ?: return
        val next = (cursor + delta).coerceIn(0, state.letterMenu.lastIndex)
        if (next == cursor) return
        landOn(next)
    }

    private fun landOn(rung: Int) {
        menuSound.play(MenuSound.SCROLL)
        _uiState.update {
            it.copy(
                letterCursor = rung,
                letterFilter = it.letterMenu.getOrNull(rung),
                selectedIndex = 0,
                usingTouch = false,
            )
        }
        applyFilter()
    }

    private var filterAtGestureStart: Char? = null

    private var launchHold: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            _uiState.map { it.visibleApps.getOrNull(it.selectedIndex)?.gameId }
                .distinctUntilChanged()
                .collectLatest { gameId ->
                    val game = gameId?.let { runCatching { gameRepository.getById(it) }.getOrNull() }
                    _uiState.update {
                        it.copy(gameDetails = game?.let { g -> GameDetails(g.id, gameFacts(g), g.description?.takeIf { d -> d.isNotBlank() }) })
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
                    _uiState.update { it.copy(letterCursor = null, letterFilter = filterAtGestureStart) }
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

        if (state.chipFocus) {
            if (state.usingTouch) _uiState.update { it.copy(usingTouch = false) }
            handleChipRow(action)
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

                if (next == cur && action == GamepadAction.NAVIGATE_UP && state.showSystemChips && cells[cur].row == 0) {
                    menuSound.play(MenuSound.SCROLL)
                    _uiState.update { it.copy(chipFocus = true) }
                } else if (next != cur) {
                    _uiState.update { it.copy(selectedIndex = next) }
                    menuSound.play(MenuSound.SCROLL)
                }
            }
            GamepadAction.SELECT -> {
                val app = state.visibleApps.getOrNull(cur)
                if (app != null) holdToLaunch(app.packageName)
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
        val tabApps = state.allApps.filter { app -> state.activeFilter.matches(app) }
            .let { if (state.activeFilter == AppFilter.GAMES) it.ofGenre(state.genreFilter) else it }
        val chips = when {
            state.activeFilter != AppFilter.GAMES -> emptyList()
            state.chipsByGenre -> genreChips(tabApps)
            else -> systemChips(tabApps)
        }
        val system = state.systemFilter?.takeIf { id -> chips.any { it.id == id } }

        val inTab = tabApps.ofChip(system, state.chipsByGenre)
            .let { apps ->
                if (state.activeFilter == AppFilter.RECENT) {
                    apps.sortedByDescending { it.lastUsedAt }
                } else {
                    apps
                }
            }

        val letters = letterMenuFor(inTab.map { it.label })
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
                chipFocus = it.chipFocus && it.copy(systemChips = chips).showSystemChips,
            )
        }
    }
}
