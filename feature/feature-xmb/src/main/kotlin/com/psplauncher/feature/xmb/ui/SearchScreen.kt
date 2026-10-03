package com.psplauncher.feature.xmb.ui

import com.psplauncher.core.common.format.playTimeLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPromptItem
import com.psplauncher.core.ui.components.HintAction
import com.psplauncher.core.ui.components.HintBarHeight
import com.psplauncher.core.ui.components.PfpHintBar
import com.psplauncher.core.ui.components.PfpSearchField
import com.psplauncher.core.ui.components.StatusStripHeight
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.PANEL_CARD_RADIUS
import com.psplauncher.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.psplauncher.core.ui.design.PANEL_UNFOCUSED_ALPHA
import com.psplauncher.core.ui.design.PanelCardFill
import com.psplauncher.core.ui.design.PanelCardFocusFill
import com.psplauncher.core.ui.design.PanelFocusRing
import com.psplauncher.core.ui.design.panelBackdrop
import com.psplauncher.core.ui.image.rememberArtworkModel
import com.psplauncher.core.ui.theme.deriveStorefrontColors
import com.psplauncher.feature.xmb.viewmodel.SearchState
import com.psplauncher.feature.xmb.viewmodel.XMBItem
import com.psplauncher.feature.xmb.viewmodel.XMBItemType
import com.psplauncher.feature.xmb.viewmodel.isInstalledApp
import com.psplauncher.feature.xmb.viewmodel.primaryVerbFor
import com.psplauncher.core.common.format.relativeTime
import com.psplauncher.core.ui.design.PanelButton
import com.psplauncher.core.ui.icons.rememberAppIcon

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

    val empty = state.rows.singleOrNull()?.takeIf { it.type == XMBItemType.EMPTY }
    val focused = state.rows.getOrNull(state.selectedIndex)?.takeIf { empty == null }
    val icon = rememberAppIcon(focused?.packageName?.takeIf { focused.isInstalledApp })
    val art = focused?.takeUnless { it.isInstalledApp }?.let { it.backdropArt.firstOrNull() ?: it.shelfCoverArt }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .panelBackdrop(icon?.color ?: SearchTint)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val u = DesignUnits(minOf(maxWidth.value / PANEL_DESIGN_WIDTH, maxHeight.value / PANEL_DESIGN_HEIGHT), LocalDensity.current)
        val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        if (art != null) {
            AsyncImage(
                model = rememberArtworkModel(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(24)).graphicsLayer(alpha = 0.45f),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        Column(
            Modifier
                .fillMaxHeight()
                .padding(start = u.dp(64), top = StatusStripHeight + u.dp(12), bottom = if (imeUp) 10.dp else HintBarHeight)
                .width(u.dp(400))
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(u.dp(16)),
        ) {
            PfpSearchField(
                query = state.query,
                active = true,
                focusRequester = focusRequester,
                placeholder = state.scope.label,
                onActivate = {},
                onQueryChange = onQueryChange,
                onDone = {},
                colors = deriveStorefrontColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (empty != null && imeUp) {
                EmptyNotice(empty, u)
            } else if (empty == null) {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(u.dp(4)), modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
                        val selected = index == state.selectedIndex
                        ResultRow(row, selected, u) { if (selected) onActivateAt(index) else onFocusAt(index) }
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxHeight()
                .padding(
                    start = u.dp(520),
                    end = u.dp(80),
                    top = if (imeUp) StatusStripHeight + u.dp(12) else u.dp(96),
                    bottom = if (imeUp) 10.dp else HintBarHeight,
                )
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(u.dp(if (imeUp) 12 else 20)),
        ) {
            when {
                empty != null && !imeUp -> Box(Modifier.padding(top = u.dp(200))) { EmptyNotice(empty, u) }
                focused != null -> Preview(focused, icon?.bitmap, u, compact = imeUp) { onActivateAt(state.selectedIndex) }
            }
        }

        if (!imeUp) {
            PfpHintBar(
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
private fun EmptyNotice(row: XMBItem, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
        Headline(row.title, u.sp(30), 2)
        row.subtitle?.let { Meta(it, u.sp(15), 2) }
    }
}

@Composable
private fun ResultRow(row: XMBItem, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
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
                    style = TextStyle(color = Color.White, fontSize = u.sp(9), fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em),
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

@Composable
private fun ColumnScope.Preview(row: XMBItem, icon: ImageBitmap?, u: DesignUnits, compact: Boolean, onActivate: () -> Unit) {
    val (kind, detail) = kindAndDetail(row)
    val game = row.gameId != null
    Box(
        Modifier
            .weight(1f, fill = false)
            .widthIn(max = u.dp(480))
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(u.dp(22)))
            .background(Color.White.copy(alpha = 0.07f)),
        contentAlignment = Alignment.Center,
    ) {
        if (row.isInstalledApp) {
            AppIcon(icon, u.dp(120), u.dp(30))
        } else {
            Icon(kindGlyph(row), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(u.dp(96)))
            (row.backdropArt.firstOrNull() ?: row.shelfCoverArt)?.let {
                AsyncImage(rememberArtworkModel(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
        Eyebrow(kind, u)
        Headline(row.title, u.sp(if (compact) 32 else 46), if (compact) 1 else 2)
        Meta(row.metadataLine?.takeIf { it.isNotBlank() } ?: detail, u.sp(15))
    }
    if (!compact && game && (row.lastOpenedAt != null || row.totalPlayTimeMillis > 0 || detail.isNotBlank())) {
        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
            row.lastOpenedAt?.let { Stat("Last played", relativeTime(System.currentTimeMillis(), it), u) }
            if (row.totalPlayTimeMillis > 0) Stat("Play time", playTimeLabel(row.totalPlayTimeMillis), u)
            if (detail.isNotBlank()) Stat("Platform", detail, u)
        }
    }
    Row(Modifier.padding(top = u.dp(4))) {
        PanelButton(GamepadAction.SELECT, if (game) "Play" else "Open", u, onActivate)
    }
}

private fun kindAndDetail(row: XMBItem): Pair<String, String> {
    val parts = row.subtitle.orEmpty().split("  ·  ", limit = 2)
    return parts[0] to parts.getOrElse(1) { "" }
}

private fun kindGlyph(row: XMBItem): ImageVector = when (row.type) {
    XMBItemType.VIDEO_FILE -> Icons.Outlined.Movie
    XMBItemType.PHOTO_FILE -> Icons.Outlined.Image
    XMBItemType.LIBRARY_BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    XMBItemType.MUSIC_TRACK -> Icons.Outlined.MusicNote
    else -> if (row.isInstalledApp) Icons.Outlined.Apps else Icons.Outlined.Games
}

private val SearchTint = Color(0xFF2B3654)
