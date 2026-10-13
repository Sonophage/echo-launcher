package com.echo.feature.crossbar.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.echo.core.domain.model.resolve
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.LAUNCH_HOLD_MS
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.PanelButton
import com.echo.core.ui.design.panelDesignUnits
import com.echo.feature.crossbar.ui.GameInfoScreen
import com.echo.feature.crossbar.ui.LastPlayedPage
import com.echo.feature.crossbar.ui.LocalRecentWallpaper
import com.echo.feature.crossbar.ui.PlayingWallpaper
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.firstSection
import com.echo.feature.crossbar.viewmodel.RecentFilter

// the Companion Screen: the Details, or in Settings the focused setting, that the Companion rule picks
// (companionContent), with Swap, Options on Details (ADR 0002), and Resume while a
// game runs. The App Drawer, Search and Settings are on the Main Screen (ADR 0001); while Search is open the
// keyboard, which the Thor puts here, shows over this screen. It takes touch only: a tap never moves the
// controller from the Main Screen (ADR 0001).
@Composable
fun CompanionScreen(
    state: CompanionState,
    crossbar: CrossbarViewModel,
    modifier: Modifier = Modifier,
) {
    val shown = state.shown as? CompanionContent.Details
    val ui by crossbar.uiState.collectAsState()
    // an app's Details sit on the wallpaper, tinted, as Recent draws an app; here it moves, as nothing else does
    CompositionLocalProvider(
        LocalPadPrompts provides false,
        LocalRecentWallpaper provides { PlayingWallpaper(ui) },
    ) {
        BoxWithConstraints(modifier.fillMaxSize().background(PanelBase)) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            (state.shown as? CompanionContent.Setting)?.let { SettingView(it, u) }
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
            // the Companion Screen's only controls: Resume while a game runs, Options on Details, and Swap
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = u.dp(40), vertical = u.dp(24)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
            ) {
                Spacer(Modifier.weight(1f))
                // Resume launches the game again, so it is a hold like every launch
                if (shown?.playing == true) PanelButton(GamepadAction.SELECT, "Resume", u, holdMs = LAUNCH_HOLD_MS) { crossbar.companionScreen.resume() }
                // the item's menu, opened here by touch; it stays on its item while the Main Screen's focus moves
                shown?.let { details -> PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", u) { crossbar.companionScreen.openMenu(details.info.item) } }
                // the Main Screen and this screen change displays
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Swap", u) { crossbar.companionScreen.toggleSwap() }
            }
            // the Companion's own menu, over everything here and drawn on whichever display shows this screen
            ui.companionMenu?.let { menu ->
                EchoContextMenuOverlay(
                    state = menu.state,
                    onRowActivated = crossbar.companionScreen::onMenuRow,
                    onDismiss = crossbar.companionScreen::closeMenu,
                )
            }
        }
    }
}

// the setting focused in Settings on the Main Screen: its description in large type, over a preview of the focused
// theme (its wallpaper, or its page's screenshot) or colour scheme where there is one
@Composable
private fun SettingView(setting: CompanionContent.Setting, u: com.echo.core.ui.design.DesignUnits) {
    Box(Modifier.fillMaxSize()) {
        when (val preview = setting.preview) {
            is SettingPreview.Theme -> {
                preview.store.picture?.let {
                    coil3.compose.AsyncImage(it, preview.store.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 1f to PanelBase)))
            }
            is SettingPreview.Colors -> {
                val swatch = Color(preview.option.swatch)
                val palette = preview.option.scheme?.resolve(java.time.LocalDate.now().monthValue)
                val (top, bottom) = palette?.let { Color(it.backgroundTop) to Color(it.backgroundBottom) } ?: (swatch to PanelBase)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 1f to bottom)))
                Box(Modifier.align(Alignment.TopEnd).padding(u.dp(48)).size(u.dp(120)).clip(CircleShape).background(swatch))
            }
            null -> Unit
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = u.dp(48), end = u.dp(48), bottom = u.dp(110)),
            verticalArrangement = Arrangement.spacedBy(u.dp(16)),
        ) {
            Text(setting.info.label, color = Color.White, fontSize = u.sp(56), lineHeight = u.sp(60), fontWeight = FontWeight.ExtraLight)
            setting.info.value?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White, fontSize = u.sp(30), fontWeight = FontWeight.Light)
            }
            setting.info.sublabel?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = u.sp(30), lineHeight = u.sp(40), fontWeight = FontWeight.Light)
            }
        }
    }
}
