package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.HideLocationType
import com.echo.feature.crossbar.viewmodel.currentCategoryOrNull
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.crossbar.viewmodel.RecentKind
import com.echo.feature.crossbar.viewmodel.launchesOut
import com.echo.feature.crossbar.viewmodel.recentKind
import com.echo.feature.launcher.OpenSession
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.GamepadAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// keeps the bottom screen's state current from the crossbar, and acts on its taps. Nothing is loaded
// while no bottom screen is attached, so a one-screen device pays nothing.
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CrossbarBottomScreen(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val link: BottomScreenLink,
    private val lastLaunch: StateFlow<OpenSession?>,
) {
    fun observe() {
        scope.launch {
            link.attached.flatMapLatest { on ->
                if (!on) flowOf(null)
                // the cursor settles before the info is read, so scrolling a column reads only where it stops
                else uiState.map { it.focusedItem?.takeIf(::hasInfo) }
                    .distinctUntilChanged { a, b -> a?.id == b?.id }
                    .debounce(SETTLE_MS)
            }.collectLatest { item ->
                val info = item?.let { vm.gameDetail.load(GameInfoState(it)) }
                link.update { it.copy(focused = info) }
            }
        }
        // owner, 2026-10-08: the drawer's focused item is drawn on the crossbar's screen as Recent draws an item, so
        // a game's details and achievements show there; it loads once the cursor settles, as Info does
        scope.launch {
            link.drawerFocus.map { it?.app }.distinctUntilChanged { a, b -> a?.packageName == b?.packageName && a?.gameId == b?.gameId }
                .debounce(SETTLE_MS)
                .collectLatest { app ->
                    val item = app?.let { a -> a.gameId?.let { itemFor(it) } ?: drawerAppItem(a) }
                    link.drawerInfoLoaded(item?.let { GameInfoState(it) })
                    item?.let { link.drawerInfoLoaded(vm.gameDetail.load(GameInfoState(it))) }
                }
        }
        // the companion follows the crossbar's column: Recent narrows to its kind, and the Music column shows the
        // remote while something plays
        scope.launch {
            uiState.map { it.currentCategoryOrNull() }.distinctUntilChanged { a, b -> a?.id == b?.id }.collect { category ->
                val state = link.state.value
                pickRecentFilter(recentFilterFor(category?.id, category?.isGamingCategory == true, state.recentFilters))
                if (category?.id == com.echo.core.domain.model.BuiltInCategory.MUSIC && state.music) showPage(BottomPage.MUSIC)
            }
        }
        // music starting turns the companion into its remote; music ending takes the page away
        scope.launch {
            uiState.map { it.musicPlayback.track != null }.distinctUntilChanged().collect { on ->
                link.update { it.copy(music = on, page = if (on && !it.music) BottomPage.MUSIC else it.page) }
            }
        }
        scope.launch {
            combine(link.attached, link.hostShown, lastLaunch) { on, shown, last ->
                if (on) playingGameId(shown, last?.gameId)?.let { it to last?.launchedAt } else null
            }.distinctUntilChanged().collectLatest { session ->
                val info = session?.first?.let { itemFor(it) }?.let { vm.gameDetail.load(GameInfoState(it)) }
                link.update { it.copy(playing = info, playingSince = session?.second.takeIf { info != null }) }
            }
        }
        scope.launch {
            link.attached.flatMapLatest { on ->
                if (on) vm.recents.recentRows(recentFilter) else flowOf(null)
            }.collect { rows ->
                if (rows == null) return@collect
                // a filter that has emptied goes back to All, as the column does
                if (recentFilter.value !in rows.filters) recentFilter.value = RecentFilter.ALL
                link.update {
                    it.copy(
                        recent = rows.items,
                        recentFilters = rows.filters,
                        recentFilter = recentFilter.value,
                        recentSelected = it.recentSelected.coerceIn(0, (rows.items.size - 1).coerceAtLeast(0)),
                    )
                }
            }
        }
        // Last Played leaves the top screen while this one shows it
        scope.launch {
            combine(link.attached, uiState.map { it.categories to it.selectedCategoryIndex }.distinctUntilChanged()) { on, _ -> on }
                .collect { on -> if (on) vm.leaveUnreachableCategory() }
        }
        // the top screen hands the App Drawer, Search and Settings to this one while it is attached
        scope.launch {
            link.attached.collect { on -> uiState.update { it.copy(secondScreen = on) } }
        }
        scope.launch {
            vm.context.echoDataStore.data.map { (it[KEY_SWAP_SCREENS] == true) to (it[KEY_SECOND_SCREEN] != false) }.distinctUntilChanged()
                .collect { (swapped, enabled) -> uiState.update { it.copy(screensSwapped = swapped, secondScreenEnabled = enabled) } }
        }
        link.bind(vm)
        vm.addCloseable { link.unbind(vm) }
    }

    private val recentFilter = MutableStateFlow(RecentFilter.ALL)

    fun pickRecentFilter(filter: RecentFilter) {
        recentFilter.value = filter
        link.update { it.copy(recentFilter = filter, recentSelected = 0) }
    }

    // as on the column: a game or app is only picked by a tap and launches by a hold; a track, video
    // or book opens at once
    fun tapRecent(index: Int) {
        vm.markTouchInput()
        val item = link.state.value.recent.getOrNull(index) ?: return
        link.update { it.copy(recentSelected = index) }
        if (!item.launchesOut()) vm.openItem(item)
    }

    private var holding = false

    fun pressRecent(index: Int, down: Boolean) {
        if (!down) {
            if (holding) vm.releaseLaunchHold()
            holding = false
            return
        }
        val item = link.state.value.recent.getOrNull(index)?.takeIf { it.launchesOut() } ?: return
        link.update { it.copy(recentSelected = index) }
        holding = true
        vm.startLaunchHold(item) {
            holding = false
            vm.openItem(item)
        }
    }

    // Resume on the bottom screen, which brings back the game ECHO launched last
    fun resume() {
        lastLaunch.value?.gameId?.let(vm.launching::resumeGame)
    }

    private fun drawerAppItem(app: com.echo.feature.appbar.InstalledApp) = CrossbarItem(
        id = app.packageName, title = app.label, packageName = app.packageName, isAndroidApp = true,
        lastOpenedAt = app.lastUsedAt.takeIf { it > 0L }, totalPlayTimeMillis = app.playTimeMillis,
    )

    val drawerInfo get() = link.drawerInfo

    private suspend fun itemFor(gameId: Long): CrossbarItem? =
        runCatching { vm.gameRepository.getById(gameId) }.getOrNull()?.let { game -> with(vm) { listOf(game).toCrossbarItems() }.first() }

    // the Swap button: the XMB and the companion change screens, and the controller follows the XMB's
    // cursor to its new screen (owner, 2026-10-06)
    fun drawerFocused(focus: DrawerFocus?) = link.drawerFocused(focus)

    val drawerFocus get() = link.drawerFocus

    fun storePreviewed(preview: com.echo.feature.settings.ui.StorePreview?) = link.storePreviewed(preview)

    val storePreview get() = link.storePreview

    fun toggleSwap() {
        setCompanionActive(false)
        scope.launch { vm.context.echoDataStore.edit { it[KEY_SWAP_SCREENS] = it[KEY_SWAP_SCREENS] != true } }
    }

    // a tap gives the controller to the screen tapped: true for the companion's, false for the XMB's
    fun setCompanionActive(on: Boolean) {
        if (uiState.value.companionActive != on) uiState.update { it.copy(companionActive = on) }
    }

    fun showPage(page: BottomPage) = link.update { it.copy(page = page) }

    // the controller on the companion: left and right change page (on Recent, first close or open its list), up and down walk Recent, LT and RT
    // its filters, A opens (a hold for what leaves ECHO, as everywhere), B hands the controller back
    // to the XMB. False lets the XMB have the press.
    fun onButton(action: GamepadAction): Boolean {
        val state = link.state.value
        val page = state.shownPage()
        if (page == BottomPage.MUSIC) when (musicRemoteKey(action)) {
            MusicRemoteKey.PLAY_PAUSE -> return true.also { vm.music.musicPlayPause() }
            MusicRemoteKey.PREV -> return true.also { vm.music.musicPrev() }
            MusicRemoteKey.NEXT -> return true.also { vm.music.musicNext() }
            MusicRemoteKey.LEAVE -> return true.also { showPage(BottomPage.RECENT) }
            MusicRemoteKey.OPTIONS -> return true.also { vm.openMusicOptionsOnCompanion() }
            null -> Unit
        }
        when (action) {
            GamepadAction.BACK -> setCompanionActive(false)
            GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT -> link.update { it.sideStep(action) }

            GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN -> if (page == BottomPage.RECENT) {
                val step = if (action == GamepadAction.NAVIGATE_UP) -1 else 1
                val next = (state.recentSelected + step).coerceIn(0, (state.recent.size - 1).coerceAtLeast(0))
                link.update { it.copy(recentSelected = next) }
            }
            GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY -> if (page == BottomPage.RECENT) {
                val filters = state.recentFilters
                val at = filters.indexOf(state.recentFilter) + if (action == GamepadAction.PREV_CATEGORY) -1 else 1
                filters.getOrNull(at)?.let(::pickRecentFilter)
            }
            GamepadAction.SELECT -> if (page == BottomPage.RECENT) {
                val item = state.recent.getOrNull(state.recentSelected) ?: return true
                if (!vm.holdToLaunch(item) { vm.openItem(item) }) vm.openItem(item)
            }
            // Y on Recent opens that item's Options (Artwork, App Info, Uninstall for an app), not the crossbar's
            GamepadAction.OPEN_CONTEXT_MENU -> {
                if (page != BottomPage.RECENT) return false
                state.recent.getOrNull(state.recentSelected)?.let(vm::openRecentItemMenu)
            }
            else -> return false
        }
        return true
    }

    // dual or single screen: off, the second screen is left to Android and ECHO uses one screen
    fun setSecondScreenEnabled(on: Boolean) {
        scope.launch { vm.context.echoDataStore.edit { it[KEY_SECOND_SCREEN] = on } }
    }

    internal companion object {
        val KEY_SWAP_SCREENS = booleanPreferencesKey("display_swap_screens")
        val KEY_SECOND_SCREEN = booleanPreferencesKey("display_second_screen")
        const val SETTLE_MS = 150L
    }
}

// only games and installed apps have an info screen; a settings row or a folder does not
internal fun hasInfo(item: CrossbarItem): Boolean =
    recentKind(item) == RecentKind.GAME || (item.isAndroidApp && item.packageName != null)
