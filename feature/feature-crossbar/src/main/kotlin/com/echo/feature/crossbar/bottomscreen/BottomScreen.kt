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
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.firstSection
import com.echo.feature.crossbar.viewmodel.RecentFilter

// the Companion Screen: the Details the Companion rule picks (companionContent), with Swap, and Resume while a
// game runs. The App Drawer, Search and Settings are on the Main Screen (ADR 0001); while Search is open the
// keyboard, which the Thor puts here, shows over this screen. It takes touch only: a tap never moves the
// controller from the Main Screen (ADR 0001).
@Composable
fun BottomScreen(
    state: BottomScreenState,
    crossbar: CrossbarViewModel,
    modifier: Modifier = Modifier,
    // nothing is locked here now; told so on show, so keys taken for typing in Swap are handed back
    onLocked: (LockedScreenOpen) -> Unit = {},
) {
    LaunchedEffect(Unit) { onLocked(LockedScreenOpen(open = false, typing = false)) }
    val shown = state.shown as? CompanionContent.Details
    CompositionLocalProvider(LocalPadPrompts provides false) {
        BoxWithConstraints(modifier.fillMaxSize().background(PanelBase)) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
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
        }
    }
}

data class LockedScreenOpen(val open: Boolean, val typing: Boolean)
