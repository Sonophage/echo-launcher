package com.echo.feature.crossbar.ui

import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.common.format.playTimeLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.EchoSearchField
import com.echo.core.ui.components.SearchFieldHeight
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PANEL_CARD_RADIUS
import com.echo.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.echo.core.ui.design.PANEL_UNFOCUSED_ALPHA
import com.echo.core.ui.design.PanelCardFill
import com.echo.core.ui.design.PanelCardFocusFill
import com.echo.core.ui.design.PanelFocusRing
import com.echo.core.ui.design.panelBackdrop
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.core.ui.theme.deriveStorefrontColors
import com.echo.feature.crossbar.viewmodel.SearchState
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.isInstalledApp
import com.echo.feature.crossbar.viewmodel.primaryVerbFor
import com.echo.core.common.format.relativeTime
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.design.HeroBanner
import com.echo.core.ui.design.PanelButton
import com.echo.core.ui.design.HERO_BANNER_SIDE
import com.echo.feature.crossbar.viewmodel.SEARCH_COLUMNS
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onActivateAt: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusAt: (Int) -> Unit = {},
    onOptionsAt: (Int) -> Unit = {},
) {
    val listState = rememberLazyGridState()
    LaunchedEffect(state.selectedIndex, state.scrollToTopToken) {
        if (state.rows.isNotEmpty()) listState.animateScrollToItem((state.selectedIndex - SEARCH_COLUMNS).coerceAtLeast(0))
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val empty = state.rows.singleOrNull()?.takeIf { it.type == CrossbarItemType.EMPTY }
    val focused = state.rows.getOrNull(state.selectedIndex)?.takeIf { empty == null }
    val icon = rememberAppIcon(focused?.packageName?.takeIf { focused.isInstalledApp })
    val art = focused?.takeUnless { it.isInstalledApp }?.let { it.backdropArt.firstOrNull() ?: it.shelfCoverArt }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .panelBackdrop(icon?.color ?: SearchTint)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        if (art != null) {
            AsyncImage(
                model = rememberBlurSourceModel(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(24)).graphicsLayer(alpha = 0.45f),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        val fieldTop = StatusStripHeight + u.dp(12)
        val belowField = fieldTop + SearchFieldHeight + u.dp(16)
        EchoSearchField(
            query = state.query,
            active = true,
            focusRequester = focusRequester,
            placeholder = state.scope.label,
            onActivate = {},
            onQueryChange = onQueryChange,
            onDone = {},
            colors = deriveStorefrontColors().copy(
                searchField = Color.White.copy(alpha = 0.10f),
                searchBorder = Color.White.copy(alpha = 0.14f),
                textPrimary = Color.White,
                textSecondary = Color.White,
            ),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = fieldTop).width(u.dp(380)),
        )

        // owner, 2026-10-04: search takes the whole screen; the highlighted result is a hero banner across
        // the top and the results run below it; moving through them changes the hero.
        // owner, 2026-10-05: the banner is the App Drawer's, at the drawer's margins, and the results are two columns
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = u.dp(HERO_BANNER_SIDE), end = u.dp(HERO_BANNER_SIDE), top = belowField, bottom = if (imeUp) 10.dp else HintBarHeight)
                .then(if (imeUp) Modifier.imePadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            when {
                empty != null -> EmptyNotice(empty, u)
                focused != null -> {
                    Hero(focused, u, short = imeUp, onOpen = { onActivateAt(state.selectedIndex) }, onOptions = { onOptionsAt(state.selectedIndex) })
                    ResultList(state, listState, u, onActivateAt, onFocusAt)
                }
            }
        }

        if (!imeUp) {
            EchoHintBar(
                items = listOf(
                    ControllerPromptItem(GamepadAction.BACK, "Close"),
                    ControllerPromptItem(GamepadAction.SELECT, "Open"),
                    ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
                primary = focused?.let { HintAction(GamepadAction.SELECT, primaryVerbFor(it) ?: "Open", listOfNotNull(it.title, it.subtitle).filter { t -> t.isNotBlank() }.joinToString(" · ")) },
                onAction = { action ->
                    when (action) {
                        GamepadAction.BACK -> onBack()
                        GamepadAction.SELECT -> state.selectedIndex.takeIf { focused != null }?.let(onActivateAt)
                        GamepadAction.OPEN_CONTEXT_MENU -> state.selectedIndex.takeIf { focused != null }?.let(onOptionsAt)
                        else -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun ResultList(
    state: SearchState,
    listState: LazyGridState,
    u: DesignUnits,
    onActivateAt: (Int) -> Unit,
    onFocusAt: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(SEARCH_COLUMNS),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(u.dp(4)),
        horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
            val selected = index == state.selectedIndex
            ResultRow(row, selected, u) { if (selected) onActivateAt(index) else onFocusAt(index) }
        }
    }
}

// the highlighted result across the top: its art filling the banner, what it is, and its facts
@Composable
private fun Hero(row: CrossbarItem, u: DesignUnits, short: Boolean, onOpen: () -> Unit, onOptions: () -> Unit) {
    val (kind, detail) = kindAndDetail(row)
    val icon = rememberAppIcon(row.packageName?.takeIf { row.isInstalledApp })
    val art = (row.backdropArt.firstOrNull() ?: row.shelfCoverArt).takeUnless { row.isInstalledApp }
    // an app has no art, so its banner takes the icon's colour
    HeroBanner(
        u,
        tint = icon?.color,
        short = short,
        art = {
            when {
                art != null -> AsyncImage(rememberArtworkModel(art), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                // as in the App Drawer, an app's icon stands on the right of its banner
                row.isInstalledApp -> Box(Modifier.align(Alignment.Center)) { AppIcon(icon?.bitmap, u.dp(if (short) 64 else 150), u.dp(if (short) 16 else 34)) }
            }
        },
    ) {
        Row(
            Modifier.align(Alignment.BottomStart).padding(u.dp(if (short) 16 else 26)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(20)),
        ) {
            when {
                art == null && !row.isInstalledApp -> Icon(kindGlyph(row), null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(u.dp(if (short) 48 else 72)))
            }
            Column(verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                Eyebrow(kind, u)
                Headline(row.title, u.sp(if (short) 22 else 36), if (short) 1 else 2)
                (row.metadataLine?.takeIf { it.isNotBlank() } ?: detail).takeIf { it.isNotBlank() }?.let { Meta(it, u.sp(14), 1) }
                if (!short && row.gameId != null && (row.lastOpenedAt != null || row.totalPlayTimeMillis > 0)) {
                    Row(Modifier.padding(top = u.dp(6)), horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
                        row.lastOpenedAt?.let { Stat("Last played", relativeTime(System.currentTimeMillis(), it), u) }
                        if (row.totalPlayTimeMillis > 0) Stat("Play time", playTimeLabel(row.totalPlayTimeMillis), u)
                    }
                }
                if (!short) HeroButtons(row, u, onOpen, onOptions, Modifier.padding(top = u.dp(6)))
            }
        }
        // with the keyboard up the banner is short, so its buttons stand at the right end
        if (short) HeroButtons(row, u, onOpen, onOptions, Modifier.align(Alignment.CenterEnd).padding(end = u.dp(26)))
    }
}

// owner, 2026-10-05: the App Drawer's buttons; search acts at once, so Open takes no hold
@Composable
private fun HeroButtons(row: CrossbarItem, u: DesignUnits, onOpen: () -> Unit, onOptions: () -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
        PanelButton(GamepadAction.SELECT, primaryVerbFor(row) ?: "Open", u, onClick = onOpen)
        PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", u, onClick = onOptions)
    }
}

@Composable
private fun EmptyNotice(row: CrossbarItem, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
        Headline(row.title, u.sp(30), 2)
        row.subtitle?.let { Meta(it, u.sp(15), 2) }
    }
}

@Composable
private fun ResultRow(row: CrossbarItem, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
    val (kind, detail) = kindAndDetail(row)
    val shape = RoundedCornerShape(u.dp(PANEL_CARD_RADIUS))
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (focused) 1f else PANEL_UNFOCUSED_ALPHA)
            .clip(shape)
            .background(if (focused) PanelCardFocusFill else PanelCardFill)
            .then(if (focused) Modifier.border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = u.dp(8), end = u.dp(14), top = u.dp(8), bottom = u.dp(8)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        if (row.isInstalledApp) {
            AppIcon(rememberAppIcon(row.packageName)?.bitmap, u.dp(46), u.dp(11))
        } else {
            Art(row.shelfCoverArt, u.dp(46), u.dp(46), u.dp(11), kindGlyph(row), u)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(3))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                Text(row.title, color = Color.White, fontSize = u.sp(14), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    kind.uppercase(),
                    style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(9), fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em),
                    modifier = Modifier.clip(RoundedCornerShape(u.dp(5))).background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = u.dp(7), vertical = u.dp(3)),
                )
            }
            if (detail.isNotBlank()) {
                Text(detail, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(11), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            row.lastOpenedAt?.let {
                Text("Played ${relativeTime(System.currentTimeMillis(), it)}", color = Color.White.copy(alpha = 0.45f), fontSize = u.sp(11),
                    fontWeight = FontWeight.Light, maxLines = 1)
            }
        }
    }
}

private fun kindAndDetail(row: CrossbarItem): Pair<String, String> {
    val parts = row.subtitle.orEmpty().split("  ·  ", limit = 2)
    return parts[0] to parts.getOrElse(1) { "" }
}

private fun kindGlyph(row: CrossbarItem): ImageVector = when (row.type) {
    CrossbarItemType.VIDEO_FILE -> Icons.Outlined.Movie
    CrossbarItemType.PHOTO_FILE -> Icons.Outlined.Image
    CrossbarItemType.LIBRARY_BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    CrossbarItemType.MUSIC_TRACK -> Icons.Outlined.MusicNote
    else -> if (row.isInstalledApp) Icons.Outlined.Apps else Icons.Outlined.Games
}

private val SearchTint = Color(0xFF2B3654)
