package com.echo.feature.crossbar.bottomscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
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
import com.echo.feature.crossbar.ui.RecentFilterRow
import androidx.compose.foundation.lazy.rememberLazyListState
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.firstSection
import com.echo.feature.crossbar.viewmodel.SearchScope
import androidx.compose.runtime.collectAsState

enum class BottomPage(val label: String) { INFO("Info"), RECENT("Recent") }

// the bottom screen: Info (the running game, else the App Drawer's hovered app, else the cursor's item) or
// the Recent shelf. The App Drawer, Search and Settings are on the Main Screen (ADR 0001); while Search is open
// the keyboard, which the Thor puts here, shows over this screen's page. Touch drives it.
@Composable
fun BottomScreen(
    state: BottomScreenState,
    crossbar: CrossbarViewModel,
    modifier: Modifier = Modifier,
    // nothing is locked here now; told so on show, so keys taken for typing in Swap are handed back
    onLocked: (LockedScreenOpen) -> Unit = {},
) {
    val ui by crossbar.uiState.collectAsState()
    val page = state.shownPage()
    LaunchedEffect(Unit) { onLocked(LockedScreenOpen(open = false, typing = false)) }
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
            if (info != null) {
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
                val recentGameId = state.recent.getOrNull(state.recentSelected)?.gameId
                val recentAchievements by remember(recentGameId) { crossbar.gameDetail.achievementsStatFor(recentGameId) }
                    .collectAsState(initial = null)
                LastPlayedPage(
                    items = state.recent.ifEmpty { listOf(crossbar.recents.emptyRecentItem()) },
                    selectedIndex = state.recentSelected,
                    listState = recentList,
                    filter = state.recentFilter,
                    railVisible = state.recentListOpen,
                    onCardTapped = crossbar.bottomScreen::tapRecent,
                    onCardPressed = crossbar.bottomScreen::pressRecent,
                    achievements = recentAchievements,
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
                // Resume launches the game again, so it is a hold like every launch
                if (state.playing != null) PanelButton(GamepadAction.SELECT, "Resume", u, holdMs = LAUNCH_HOLD_MS) { crossbar.bottomScreen.resume() }
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Apps", u) { crossbar.onOpenAppDrawer() }
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Search", u) { crossbar.librarySearch.openSearch(SearchScope.ALL) }
                // the XMB and this screen change places (owner, 2026-10-06)
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Swap", u) { crossbar.bottomScreen.toggleSwap() }
            }
        }
    }
}

data class LockedScreenOpen(val open: Boolean, val typing: Boolean)
