package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.RecentKind
import com.echo.feature.crossbar.viewmodel.recentKind
import com.echo.feature.launcher.OpenSession
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
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

// keeps the Companion Screen's state current from the Main Screen's, and acts on its taps. Nothing is loaded
// while no Companion Screen is on show, so a one-screen device pays nothing until Swap shows it.
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CrossbarBottomScreen(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val link: BottomScreenLink,
    private val lastLaunch: StateFlow<OpenSession?>,
) {
    // a Companion Screen is on show: attached on a second display, or shown in place by Swap on one
    private val companionShown = combine(link.attached, uiState.map { it.companionInPlace }) { attached, inPlace -> attached || inPlace }
        .distinctUntilChanged()

    fun observe() {
        scope.launch {
            companionShown.flatMapLatest { on ->
                if (!on) flowOf(null)
                // the cursor settles before the info is read, so scrolling a column reads only where it stops
                else uiState.map { mainScreenHover(it)?.takeIf(::companionShows) }
                    .distinctUntilChanged { a, b -> a?.id == b?.id }
                    .debounce(SETTLE_MS)
            }.collectLatest { item ->
                val info = item?.let { vm.gameDetail.load(GameInfoState(it)) }
                link.update { it.copy(hovered = info) }
            }
        }
        // the App Drawer is on the Main Screen (ADR 0001) and the Companion Screen shows the app hovered in it, with a
        // game's details and achievements; it loads once the cursor settles, as Info does
        scope.launch {
            companionShown.flatMapLatest { on -> if (on) link.drawerFocus else flowOf(null) }
                .distinctUntilChanged { a, b -> a?.packageName == b?.packageName && a?.gameId == b?.gameId }
                .debounce(SETTLE_MS)
                .collectLatest { app ->
                    val item = app?.let { a -> a.gameId?.let { itemFor(it) } ?: drawerCaseItem(a) }
                    link.update { it.copy(drawer = item?.let { GameInfoState(it) }) }
                    item?.let { i -> vm.gameDetail.load(GameInfoState(i)).let { info -> link.update { it.copy(drawer = info) } } }
                }
        }
        // Search, a menu, Settings and the colour scheme picker open on the Main Screen, which the Companion rule reads
        scope.launch {
            uiState.map { s ->
                BottomScreenState(
                    searching = s.search != null,
                    menuOpen = s.activeContextMenu != null,
                    settingsOpen = s.activeSettingsScreen != null,
                    colorScheme = s.colorSchemePicker?.let { it.options.getOrNull(it.selectedIndex) },
                )
            }.distinctUntilChanged().collect { main ->
                link.update {
                    it.copy(searching = main.searching, menuOpen = main.menuOpen, settingsOpen = main.settingsOpen, colorScheme = main.colorScheme)
                }
            }
        }
        scope.launch {
            combine(companionShown, link.hostShown, lastLaunch) { on, shown, last ->
                if (on) playingGameId(shown, last?.gameId)?.let { it to last?.launchedAt } else null
            }.distinctUntilChanged().collectLatest { session ->
                val info = session?.first?.let { itemFor(it) }?.let { vm.gameDetail.load(GameInfoState(it)) }
                link.update { it.copy(playing = info, playingSince = session?.second.takeIf { info != null }) }
            }
        }
        // the Crossbar knows while a Companion Screen is attached
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

    // Resume on the Companion Screen, which brings back the game ECHO launched last
    fun resume() {
        lastLaunch.value?.gameId?.let(vm.launching::resumeGame)
    }

    private suspend fun itemFor(gameId: Long): CrossbarItem? =
        runCatching { vm.gameRepository.getById(gameId) }.getOrNull()?.let { game -> with(vm) { listOf(game).toCrossbarItems() }.first() }

    fun drawerFocused(app: com.echo.feature.appbar.InstalledApp?) = link.drawerFocused(app)

    // the setting focused in Settings, and the theme focused in the Theme Store, which Settings reports while a
    // Companion Screen is on show
    fun settingFocused(info: com.echo.feature.settings.ui.SettingsFocusInfo?) = link.update { it.copy(setting = info) }

    fun themeFocused(preview: com.echo.feature.settings.ui.StorePreview?) = link.update { it.copy(theme = preview) }

    // Swap (the Swap button, L3 or R3), as swap() decides: two displays change places and it is remembered, and the
    // controller stays with the Main Screen; one display shows the Companion Screen in place, in memory only
    fun toggleSwap() {
        when (val next = swap(uiState.value)) {
            is Swap.Displays -> scope.launch { vm.context.echoDataStore.edit { it[KEY_SWAP_SCREENS] = next.swapped } }
            is Swap.InPlace -> uiState.update { it.copy(companionInPlace = next.shown) }
        }
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
