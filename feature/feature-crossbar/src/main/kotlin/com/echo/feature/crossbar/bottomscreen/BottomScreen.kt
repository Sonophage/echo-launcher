package com.echo.feature.crossbar.bottomscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.echo.core.ui.design.LAUNCH_HOLD_MS
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.PanelButton
import com.echo.core.ui.design.panelDesignUnits
import com.echo.feature.crossbar.ui.GameInfoScreen
import com.echo.feature.crossbar.ui.LastPlayedPage
import androidx.compose.foundation.lazy.rememberLazyListState
import com.echo.feature.crossbar.ui.SearchScreen
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.firstSection
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.settings.ui.SettingsNavHost
import androidx.compose.runtime.collectAsState

// what the bottom screen shows over its own pages: the screens a second screen takes from the top one
// (owner, 2026-10-06), whatever opened them
internal enum class LockedScreen { NONE, SETTINGS, SEARCH }

// stacked as CrossbarShell stacks them: Search over the drawer (the drawer's own search), the drawer over Settings.
// The App Drawer is on the Main Screen (ADR 0001), so while it is open this screen shows its hovered app
internal fun lockedScreen(s: CrossbarUiState): LockedScreen = when {
    s.search != null -> LockedScreen.SEARCH
    s.activeAppDrawerFilter != null -> LockedScreen.NONE
    s.activeSettingsScreen != null && s.activeSettingsScreen !in CrossbarViewModel.WIZARD_SCREEN_IDS -> LockedScreen.SETTINGS
    else -> LockedScreen.NONE
}

// the Companion Screen: the Details the Companion rule picks (companionContent), with Swap, and Resume while a
// game runs; over them, Search and Settings while they still open here. It takes touch only: a tap never moves
// the controller from the Main Screen (ADR 0001).
@Composable
fun BottomScreen(
    state: BottomScreenState,
    crossbar: CrossbarViewModel,
    modifier: Modifier = Modifier,
    // true while a locked screen is open here, so its text fields can take the keyboard
    onLocked: (LockedScreenOpen) -> Unit = {},
) {
    val ui by crossbar.uiState.collectAsState()
    val locked = lockedScreen(ui)
    LaunchedEffect(locked) { onLocked(LockedScreenOpen(open = locked != LockedScreen.NONE, typing = locked == LockedScreen.SEARCH)) }
    val shown = state.shown as? CompanionContent.Details
    CompositionLocalProvider(LocalPadPrompts provides false) {
        BoxWithConstraints(modifier.fillMaxSize().background(PanelBase)) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            // a menu asked for here (Y on Recent) opens here, over the page
            val companionMenu: @Composable () -> Unit = {
                ui.activeContextMenu?.takeIf { ui.menuOnCompanion }?.let { menu ->
                    com.echo.core.ui.components.EchoContextMenuOverlay(
                        state = menu.state,
                        onRowActivated = crossbar::onContextMenuItemActivatedAt,
                        onDismiss = crossbar::closeContextMenu,
                    )
                }
            }
            when (locked) {
                LockedScreen.SETTINGS -> {
                    SettingsHere(ui, crossbar)
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
            shown?.let { details ->
                val info = details.info
                if (showsGameInfo(info.item)) {
                    // Info's sections (the sheet, the video) open here without touching the Main Screen
                    // it opens on achievements, as the Main Screen's Game Info does
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
                } else {
                    // a song, video, book, photo or system as Recent draws an item in full view; a tap does nothing
                    LastPlayedPage(
                        items = listOf(info.item),
                        selectedIndex = 0,
                        listState = rememberLazyListState(),
                        filter = RecentFilter.ALL,
                        railVisible = false,
                        onCardTapped = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (details.playing) {
                    // the session clock ticks each minute while the game runs
                    val now by androidx.compose.runtime.produceState(System.currentTimeMillis(), details.playingSince) {
                        while (true) { value = System.currentTimeMillis(); kotlinx.coroutines.delay(30_000L) }
                    }
                    Text(
                        sessionLabel(details.playingSince, now),
                        style = u.eyebrow(),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = u.dp(40), top = u.dp(28)),
                    )
                }
            }
            // the Companion Screen's only controls: Resume while a game runs, and Swap
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = u.dp(40), vertical = u.dp(24)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
            ) {
                Spacer(Modifier.weight(1f))
                // Resume launches the game again, so it is a hold like every launch
                if (shown?.playing == true) PanelButton(GamepadAction.SELECT, "Resume", u, holdMs = LAUNCH_HOLD_MS) { crossbar.bottomScreen.resume() }
                // the Main Screen and this screen change displays
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Swap", u) { crossbar.bottomScreen.toggleSwap() }
            }
            companionMenu()
        }
    }
}

data class LockedScreenOpen(val open: Boolean, val typing: Boolean)

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
