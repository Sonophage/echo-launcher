package com.echo.feature.crossbar.bottomscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.LAUNCH_HOLD_MS
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.PanelButton
import com.echo.core.ui.design.panelDesignUnits
import com.echo.feature.appbar.AppDrawerScreen
import com.echo.feature.appbar.AppFilter
import com.echo.feature.crossbar.ui.DrawerSectionRow
import com.echo.feature.crossbar.ui.GameInfoScreen
import com.echo.feature.crossbar.ui.LastPlayedPage
import com.echo.feature.crossbar.ui.RecentFilterRow
import androidx.compose.foundation.lazy.rememberLazyListState
import com.echo.feature.crossbar.ui.SearchScreen
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.firstSection
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.settings.ui.SettingsNavHost
import androidx.compose.runtime.collectAsState

enum class BottomPage(val label: String) { INFO("Info"), RECENT("Recent"), MUSIC("Music") }

// what the bottom screen shows over its own pages: the screens a second screen takes from the top one
// (owner, 2026-10-06), whatever opened them
internal enum class LockedScreen { NONE, SETTINGS, APPS, SEARCH }

// stacked as CrossbarShell stacks them: Search over the drawer (the drawer's own search), the drawer over Settings
internal fun lockedScreen(s: CrossbarUiState): LockedScreen = when {
    s.search != null -> LockedScreen.SEARCH
    s.activeAppDrawerFilter != null -> LockedScreen.APPS
    s.activeSettingsScreen != null && s.activeSettingsScreen !in CrossbarViewModel.WIZARD_SCREEN_IDS -> LockedScreen.SETTINGS
    else -> LockedScreen.NONE
}

// the bottom screen: Info (the running game, else the cursor's item) or the Recent shelf, and, over
// them, the App Drawer, Search and Settings, which open here on a device with a second screen. Touch
// drives it, and the controller drives the locked screens through the crossbar as it does on top.
@Composable
fun BottomScreen(
    state: BottomScreenState,
    crossbar: CrossbarViewModel,
    modifier: Modifier = Modifier,
    // true while a locked screen is open here, so its text fields can take the keyboard
    onLocked: (LockedScreenOpen) -> Unit = {},
) {
    val ui by crossbar.uiState.collectAsState()
    val page = state.shownPage()
    val locked = lockedScreen(ui)
    LaunchedEffect(locked) { onLocked(LockedScreenOpen(open = locked != LockedScreen.NONE, typing = locked == LockedScreen.SEARCH)) }
    val info = state.shownInfo().takeIf { page == BottomPage.INFO }
    // the screen the controller drives is outlined (owner, 2026-10-06: a tap gives the controller to the screen tapped)
    val driven = ui.secondScreen && ui.companionActive
    val recentList = rememberLazyListState()
    CompositionLocalProvider(LocalPadPrompts provides false) {
        BoxWithConstraints(
            modifier.fillMaxSize().background(PanelBase)
                .then(if (driven) Modifier.border(3.dp, Color.White.copy(alpha = 0.55f)) else Modifier),
        ) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            when (locked) {
                LockedScreen.SETTINGS -> {
                    SettingsHere(ui, crossbar)
                    return@BoxWithConstraints
                }
                LockedScreen.APPS -> {
                    AppsHere(ui, crossbar, u)
                    return@BoxWithConstraints
                }
                LockedScreen.SEARCH -> {
                    ui.search?.let { search ->
                        SearchScreen(
                            state = search,
                            onQueryChange = crossbar.librarySearch::onSearchQueryChange,
                            onActivateAt = crossbar.librarySearch::onSearchActivatedAt,
                            onBack = crossbar.librarySearch::closeSearch,
                            onFocusAt = crossbar.librarySearch::onSearchFocusedAt,
                            onOptionsAt = crossbar.librarySearch::onSearchOptionsAt,
                            onKindPicked = crossbar.librarySearch::pickSearchKind,
                            // the keyboard and field here, the results on the crossbar's screen (owner, 2026-10-08)
                            part = com.echo.feature.crossbar.ui.SearchPart.FIELD,
                        )
                    }
                    return@BoxWithConstraints
                }
                LockedScreen.NONE -> Unit
            }
            if (page == BottomPage.MUSIC) {
                // the music remote: the player itself, so the top screen stays free to browse (owner, 2026-10-08)
                // the scrubber reads the playing position, which only the crossbar's window provided
                androidx.compose.runtime.CompositionLocalProvider(
                    com.echo.feature.crossbar.ui.LocalPlaybackPositions provides
                        remember(crossbar) { com.echo.feature.crossbar.ui.PlaybackPositions(crossbar.musicPositionMs, crossbar.externalPositionMs) },
                ) {
                com.echo.feature.crossbar.ui.MusicPlayerScreen(
                    state = ui.musicPlayback,
                    onPlayPause = crossbar.music::musicPlayPause,
                    onPrev = crossbar.music::musicPrev,
                    onNext = crossbar.music::musicNext,
                    onSeekTo = crossbar.music::musicSeekTo,
                    onShuffle = crossbar.music::musicToggleShuffle,
                    onRepeat = crossbar.music::musicCycleRepeat,
                    accentArgb = ui.musicAccentArgb,
                    onBack = { crossbar.bottomScreen.showPage(BottomPage.RECENT) },
                    showFooter = false,
                    modifier = Modifier.fillMaxSize().padding(bottom = u.dp(84)),
                )
                }
            } else if (info != null) {
                // Info's sections (the sheet, the video) open here without touching the top screen
                // it opens on achievements, as the top screen's Game Info does
                var open by remember(info.item.id, info.achievementSet, info.content != null) { mutableStateOf(info.firstSection()) }
                GameInfoScreen(
                    info = info.copy(open = open),
                    androidNotices = emptyList(),
                    onAction = {},
                    onCardFocused = {},
                    onNoticeTapped = {},
                    onClosePanel = { open = null },
                    onSectionPicked = { open = it },
                    companion = true,
                )
                if (state.playing != null) {
                    // the session clock ticks each minute while the game runs
                    val now by androidx.compose.runtime.produceState(System.currentTimeMillis(), state.playingSince) {
                        while (true) { value = System.currentTimeMillis(); kotlinx.coroutines.delay(30_000L) }
                    }
                    Text(
                        sessionLabel(state.playingSince, now),
                        style = u.eyebrow(),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = u.dp(40), top = u.dp(28)),
                    )
                }
            } else {
                // the Last Played screen, as the top screen draws it, with its filters above it
                LastPlayedPage(
                    items = state.recent.ifEmpty { listOf(crossbar.recents.emptyRecentItem()) },
                    selectedIndex = state.recentSelected,
                    listState = recentList,
                    filter = state.recentFilter,
                    railVisible = state.recentListOpen,
                    onCardTapped = crossbar.bottomScreen::tapRecent,
                    onCardPressed = crossbar.bottomScreen::pressRecent,
                    modifier = Modifier.fillMaxSize(),
                )
                RecentFilterRow(
                    filter = state.recentFilter,
                    u = u,
                    filters = state.recentFilters,
                    onFilterTapped = crossbar.bottomScreen::pickRecentFilter,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = u.dp(12)),
                )
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = u.dp(40), vertical = u.dp(24)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
            ) {
                // the page in view is drawn as the primary button
                // Info only when there is something to show: on a shelf or a folder it did nothing when tapped
                state.pages().forEach { p ->
                    PanelButton(if (p == page) GamepadAction.SELECT else GamepadAction.OPEN_CONTEXT_MENU, p.label, u) { crossbar.bottomScreen.showPage(p) }
                }
                Spacer(Modifier.weight(1f))
                // the remote's way out: Y's options hold Stop & Close; B, or a tap on the player, goes back to Recent
                if (page == BottomPage.MUSIC) PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", u) { crossbar.openMusicOptionsOnCompanion() }
                // Resume launches the game again, so it is a hold like every launch
                if (state.playing != null) PanelButton(GamepadAction.SELECT, "Resume", u, holdMs = LAUNCH_HOLD_MS) { crossbar.bottomScreen.resume() }
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Apps", u) { crossbar.onOpenAppDrawer() }
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Search", u) { crossbar.librarySearch.openSearch(SearchScope.ALL) }
                // the XMB and this screen change places (owner, 2026-10-06)
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Swap", u) { crossbar.bottomScreen.toggleSwap() }
            }
            // a menu asked for here (Y on Recent) opens here, over the page
            ui.activeContextMenu?.takeIf { ui.menuOnCompanion }?.let { menu ->
                com.echo.core.ui.components.EchoContextMenuOverlay(
                    state = menu.state,
                    onRowActivated = crossbar::onContextMenuItemActivatedAt,
                    onDismiss = crossbar::closeContextMenu,
                )
            }
        }
    }
}

data class LockedScreenOpen(val open: Boolean, val typing: Boolean)

// the App Drawer as the top screen hosts it (CrossbarShell), with its sections drawn above it
@Composable
private fun AppsHere(ui: CrossbarUiState, crossbar: CrossbarViewModel, u: DesignUnits) {
    val filter = runCatching { AppFilter.valueOf(ui.activeAppDrawerFilter.orEmpty()) }.getOrDefault(AppFilter.DEFAULT)
    var active by remember { mutableStateOf<AppFilter?>(null) }
    var sections by remember { mutableStateOf<List<AppFilter>>(emptyList()) }
    var pick by remember { mutableStateOf<AppFilter?>(null) }
    Box(Modifier.fillMaxSize()) {
        AppDrawerScreen(
            initialFilter = filter,
            onBack = crossbar::onCloseAppDrawer,
            pendingGamepadAction = ui.pendingDrawerAction,
            selectReleases = ui.drawerSelectReleases,
            typedChar = ui.pendingDrawerTypedChar,
            onTypedCharConsumed = crossbar::onDrawerTypedCharConsumed,
            onGamepadActionConsumed = crossbar::consumeDrawerAction,
            letterRailHeld = ui.drawerLetterRailHeld,
            onTouchInteraction = crossbar::markTouchInput,
            onAddToCrossBar = crossbar::addAppToOpenCategory,
            onLaunchRom = crossbar.launching::launchGameFromDrawer,
            onGameMenu = crossbar::openGameMenu,
            onOpenAppSearch = crossbar.librarySearch::openAppSearch,
            onTabsShown = { a, s -> active = a; sections = s },
            tabPick = pick,
            onTabPickConsumed = { pick = null },
            onOpenPermissions = { crossbar.onOpenSettingsScreen("settings_permissions") },
            heroOnOtherScreen = true,
            genreFilter = ui.genreFilter,
            onFocusedApp = { app, launch, options -> crossbar.bottomScreen.drawerFocused(app?.let { DrawerFocus(it, launch, options) }) },
            modifier = Modifier.fillMaxSize(),
        )
        active?.let { a ->
            DrawerSectionRow(a, sections, u, Modifier.align(Alignment.TopCenter).padding(top = u.dp(12))) { pick = it }
        }
    }
}

// Settings as the top screen hosts it (CrossbarShell)
@Composable
private fun SettingsHere(ui: CrossbarUiState, crossbar: CrossbarViewModel) {
    val screenId = ui.activeSettingsScreen ?: return
    // the store reports its focused theme, which the crossbar's screen shows large
    CompositionLocalProvider(com.echo.feature.settings.ui.LocalStorePreview provides crossbar.bottomScreen::storePreviewed) {
    SettingsNavHost(
        screenId = screenId,
        onBack = crossbar::onSettingsBack,
        pendingGamepadAction = ui.pendingSettingsAction,
        onGamepadActionConsumed = crossbar::consumeSettingsAction,
        onPromptTapped = crossbar::onPromptTapped,
        showControllerHint = ui.showSettingsHint,
        leftBacksOut = ui.leftBacksOut,
        lastInputWasTouch = ui.lastInputWasTouch,
        onTouchInteraction = crossbar::markTouchInput,
        onOpenColorSchemePicker = crossbar.look::openColorSchemePicker,
        onOpenCustomIcons = crossbar::openCustomIcons,
        onPreviewBootSequence = crossbar.launching::previewBootSequence,
        onPreviewGameBoot = crossbar.launching::previewGameBoot,
        onAddAndroidApps = crossbar::openAndroidLibraryPicker,
        onGoToLibrary = crossbar::goToLibrary,
        onOpenScreen = crossbar::onOpenSettingsScreen,
        modifier = Modifier.fillMaxSize(),
    )
    }
}
