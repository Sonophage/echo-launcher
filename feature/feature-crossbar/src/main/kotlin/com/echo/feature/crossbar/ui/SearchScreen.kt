package com.echo.feature.crossbar.ui

import com.echo.core.ui.components.chromeGutter
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onActivateAt: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusAt: (Int) -> Unit = {},
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedIndex, state.scrollToTopToken) {
        if (state.rows.isNotEmpty()) listState.animateScrollToItem((state.selectedIndex - 1).coerceAtLeast(0))
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
        // the top and the results run below it, full width; moving through them changes the hero
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = chromeGutter(), end = chromeGutter(end = true), top = belowField, bottom = if (imeUp) 10.dp else HintBarHeight)
                .then(if (imeUp) Modifier.imePadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            when {
                empty != null -> EmptyNotice(empty, u)
                focused != null -> {
                    Hero(focused, u, short = imeUp) { onActivateAt(state.selectedIndex) }
                    ResultList(state, listState, u, onActivateAt, onFocusAt)
                }
            }
        }

        if (!imeUp) {
            EchoHintBar(
                items = listOf(
                    ControllerPromptItem(GamepadAction.BACK, "Close"),
                    ControllerPromptItem(GamepadAction.SELECT, "Open"),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
                primary = focused?.let { HintAction(GamepadAction.SELECT, primaryVerbFor(it) ?: "Open", listOfNotNull(it.title, it.subtitle).filter { t -> t.isNotBlank() }.joinToString(" · ")) },
                onAction = { action ->
                    when (action) {
                        GamepadAction.BACK -> onBack()
                        GamepadAction.SELECT -> state.selectedIndex.takeIf { focused != null }?.let(onActivateAt)
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
    listState: androidx.compose.foundation.lazy.LazyListState,
    u: DesignUnits,
    onActivateAt: (Int) -> Unit,
    onFocusAt: (Int) -> Unit,
) {
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(u.dp(4)), modifier = Modifier.fillMaxSize()) {
        itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
            val selected = index == state.selectedIndex
            ResultRow(row, selected, u) { if (selected) onActivateAt(index) else onFocusAt(index) }
        }
    }
}

// the highlighted result across the top: its art filling the banner, what it is, and its facts
@Composable
private fun Hero(row: CrossbarItem, u: DesignUnits, short: Boolean, onClick: () -> Unit) {
    val (kind, detail) = kindAndDetail(row)
    val icon = rememberAppIcon(row.packageName?.takeIf { row.isInstalledApp })
    val art = (row.backdropArt.firstOrNull() ?: row.shelfCoverArt).takeUnless { row.isInstalledApp }
    val shape = RoundedCornerShape(u.dp(22))
    Box(
        Modifier
            .fillMaxWidth()
            .height(u.dp(if (short) 120 else 230))
            .clip(shape)
            // an app has no art, so its banner takes the icon's colour
            .background(icon?.color?.copy(alpha = 0.45f) ?: Color.White.copy(alpha = 0.07f))
            .border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape)
            .clickable(onClick = onClick),
    ) {
        art?.let { AsyncImage(rememberArtworkModel(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.78f), 0.65f to Color.Transparent)))
        Row(
            Modifier.align(Alignment.BottomStart).padding(u.dp(if (short) 16 else 26)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(20)),
        ) {
            when {
                row.isInstalledApp -> AppIcon(icon?.bitmap, u.dp(if (short) 64 else 96), u.dp(if (short) 16 else 24))
                art == null -> Icon(kindGlyph(row), null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(u.dp(if (short) 48 else 72)))
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
            }
        }
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
