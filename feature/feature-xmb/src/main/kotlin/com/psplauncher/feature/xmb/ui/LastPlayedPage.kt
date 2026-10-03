package com.psplauncher.feature.xmb.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPrompt
import com.psplauncher.core.ui.components.LocalPadPrompts
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.PANEL_CARD_RADIUS
import com.psplauncher.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.psplauncher.core.ui.design.PANEL_UNFOCUSED_ALPHA
import com.psplauncher.core.ui.design.PanelBase
import com.psplauncher.core.ui.design.PanelCardFocusFill
import com.psplauncher.core.ui.design.PanelFocusRing
import com.psplauncher.core.ui.image.rememberArtworkModel
import com.psplauncher.feature.xmb.ui.detail.panelPlayTime
import com.psplauncher.feature.xmb.viewmodel.RecentDay
import com.psplauncher.feature.xmb.viewmodel.RecentFilter
import com.psplauncher.feature.xmb.viewmodel.RecentKind
import com.psplauncher.feature.xmb.viewmodel.XMBItem
import com.psplauncher.feature.xmb.viewmodel.groupRecentsByDay
import com.psplauncher.feature.xmb.viewmodel.isInstalledApp
import com.psplauncher.feature.xmb.viewmodel.recentKind
import com.psplauncher.feature.xmb.viewmodel.relativeTime
import com.psplauncher.feature.xmb.viewmodel.removableFromRecent

private const val DESIGN_WIDTH = 1200f
private const val DESIGN_HEIGHT = 752f
private const val LETTERBOX_RATIO = 2.39f

@Composable
fun LastPlayedPage(
    items: List<XMBItem>,
    selectedIndex: Int,
    listState: LazyListState,

    filter: RecentFilter,

    railVisible: Boolean,
    onCardTapped: (Int) -> Unit,
    onAction: (GamepadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focused = items.getOrNull(selectedIndex)
    val now = System.currentTimeMillis()
    val empty = if (filter == RecentFilter.ALL) "Nothing played yet." else "No recent ${filter.label.lowercase()}."

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val u = DesignUnits(minOf(maxWidth.value / DESIGN_WIDTH, maxHeight.value / DESIGN_HEIGHT), LocalDensity.current)
        Crossfade(railVisible, animationSpec = tween(220), label = "recentRail") { rail ->
            if (rail) {
                RecentList(items, selectedIndex, focused, listState, now, empty, u, onCardTapped, onAction)
            } else {
                val stripTop = maxOf(u.dp(80), StripHeight + 8.dp)
                val stripHeight = minOf(maxWidth / LETTERBOX_RATIO, maxHeight - stripTop - u.dp(180))
                Letterbox(focused, stripTop, stripHeight, now, empty, u, { onCardTapped(selectedIndex) }, onAction)
            }
        }
    }
}

@Composable
private fun Letterbox(
    item: XMBItem?,
    stripTop: Dp,
    stripHeight: Dp,
    now: Long,
    empty: String,
    u: DesignUnits,
    onArtTapped: () -> Unit,
    onAction: (GamepadAction) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .padding(top = stripTop)
                .fillMaxWidth()
                .height(stripHeight)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onArtTapped),
        ) {
            ItemArt(item, u.dp(150), BiasAlignment(0f, -0.2f))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.65f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
            Box(Modifier.align(Alignment.BottomStart).padding(start = u.dp(80), end = u.dp(80), bottom = u.dp(14))) {
                val logo = item?.logoUri?.takeIf { item.hasVisibleLogo }
                if (logo != null) {
                    AsyncImage(
                        model = rememberArtworkModel(logo),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier.width(u.dp(380)).height(u.dp(130)),
                    )
                } else {
                    Headline(item?.title ?: empty, if (item != null) u.sp(64) else u.sp(40), 2)
                }
            }
        }
        item?.progressFraction?.let { p ->
            Box(
                Modifier.padding(top = stripTop + stripHeight, start = u.dp(80), end = u.dp(80))
                    .fillMaxWidth().height(u.dp(3)).background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
            }
        }
        if (item != null) {
            Row(
                Modifier.padding(top = stripTop + stripHeight + u.dp(28), start = u.dp(80), end = u.dp(80)).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(30)),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                    Text(detailLine(item, now), color = Color.White, fontSize = u.sp(20), fontWeight = FontWeight.Light,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    subLine(item)?.let {
                        Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13), fontWeight = FontWeight.Light,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
                    ActionPill(GamepadAction.SELECT, primaryLabel(item), true, u) { onAction(GamepadAction.SELECT) }
                    infoLabel(item)?.let { ActionPill(GamepadAction.CHANGE_SORT, it, false, u) { onAction(GamepadAction.CHANGE_SORT) } }
                }
            }
        }
    }
}

@Composable
private fun RecentList(
    items: List<XMBItem>,
    selectedIndex: Int,
    focused: XMBItem?,
    listState: LazyListState,
    now: Long,
    empty: String,
    u: DesignUnits,
    onCardTapped: (Int) -> Unit,
    onAction: (GamepadAction) -> Unit,
) {
    val groups = remember(items, now / 60_000L) { groupRecentsByDay(items, now) }
    val rows = remember(groups) {
        groups.flatMap { (day, members) -> listOf(RailRow(day, null)) + members.map { RailRow(day, it) } }
    }
    LaunchedEffect(selectedIndex, rows) {
        val at = rows.indexOfFirst { it.item?.index == selectedIndex }
        if (at >= 0) listState.animateScrollToItem((at - 1).coerceAtLeast(0))
    }
    val art = focused?.backdropArt?.firstOrNull()
    Box(Modifier.fillMaxSize().background(PanelBase)) {
        if (art != null) {
            AsyncImage(
                model = rememberArtworkModel(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(24)).graphicsLayer(alpha = 0.4f),
            )
        }
        Box(Modifier.fillMaxSize().padding(start = u.dp(480))) {
            ItemArt(focused, u.dp(150), Alignment.Center)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PanelBase.copy(alpha = 0.85f), 0.3f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to PanelBase.copy(alpha = 0.92f))))
        }
        Box(Modifier.fillMaxHeight().width(u.dp(480)).background(Color.Black.copy(alpha = 0.45f)))

        Row(
            Modifier.padding(start = u.dp(80), top = StripHeight + u.dp(12)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
        ) {
            Icon(Icons.Outlined.History, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(u.dp(22)))
            Text("Recently opened", color = Color.White, fontSize = u.sp(22), fontWeight = FontWeight.Light)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(start = u.dp(64), top = StripHeight + u.dp(64), bottom = u.dp(70))
                .width(u.dp(380))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(u.dp(4)),
            contentPadding = PaddingValues(vertical = u.dp(4)),
        ) {
            items(rows, key = { row -> (row.item?.value?.id ?: row.day.name) }) { row ->
                val entry = row.item
                if (entry != null) {
                    RecentRow(entry.value, entry.index == selectedIndex, now, u) { onCardTapped(entry.index) }
                } else {
                    Text(
                        row.day.label.uppercase(),
                        style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), letterSpacing = 0.16.em),
                        modifier = Modifier.padding(start = u.dp(16), top = u.dp(14), bottom = u.dp(6)),
                    )
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = u.dp(540), end = u.dp(80), bottom = u.dp(80)),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            if (focused == null) {
                Headline(empty, u.sp(40), 2)
            } else {
                val opened = focused.lastOpenedAt?.let { relativeTime(now, it) }
                Text(
                    listOfNotNull(kindLabel(focused), opened).joinToString(" · ").uppercase(),
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(13), letterSpacing = 0.18.em),
                )
                Headline(focused.title, u.sp(64), 2)
                (subLine(focused) ?: detailLine(focused, now).takeIf { it.isNotBlank() })?.let {
                    Text(it, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(16), fontWeight = FontWeight.Light,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                focused.progressFraction?.let { p ->
                    Row(Modifier.width(u.dp(420)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
                        ProgressBar(p, u.dp(4), Modifier.weight(1f))
                        focused.progressLabel?.let {
                            Text(it, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(13), fontWeight = FontWeight.Light, maxLines = 1)
                        }
                    }
                }
                Row(Modifier.padding(top = u.dp(8)), horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
                    ActionPill(GamepadAction.SELECT, primaryLabel(focused), true, u) { onAction(GamepadAction.SELECT) }
                    if (focused.removableFromRecent) {
                        ActionPill(GamepadAction.CHANGE_SORT, "Remove", false, u) { onAction(GamepadAction.CHANGE_SORT) }
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomStart).padding(start = u.dp(68), bottom = u.dp(14))
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(u.dp(20)))
                .clickable { onAction(GamepadAction.BACK) }
                .padding(horizontal = u.dp(12)),
            contentAlignment = Alignment.Center,
        ) {
            val style = TextStyle(fontSize = u.sp(13), fontWeight = FontWeight.Light)
            if (LocalPadPrompts.current) {
                ControllerPrompt(GamepadAction.BACK, "Back", labelStyle = style, glyphSize = u.dp(22), spacing = u.dp(8))
            } else {
                Text("Back", color = Color.White.copy(alpha = 0.75f), style = style)
            }
        }
    }
}

private class RailRow(val day: RecentDay, val item: IndexedValue<XMBItem>?)

@Composable
private fun RecentRow(item: XMBItem, focused: Boolean, now: Long, u: DesignUnits, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u.dp(PANEL_CARD_RADIUS))
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (focused) 1f else PANEL_UNFOCUSED_ALPHA)
            .clip(shape)
            .background(if (focused) PanelCardFocusFill else Color.Transparent)
            .then(if (focused) Modifier.border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = u.dp(10), end = u.dp(16), top = u.dp(10), bottom = u.dp(10)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        Thumb(item, u.dp(44), u.dp(11))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
            Text(item.title, color = Color.White, fontSize = u.sp(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(kindLabel(item), item.lastOpenedAt?.let { relativeTime(now, it) }).joinToString(" · "),
                color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1,
            )
            item.progressFraction?.let { ProgressBar(it, u.dp(3), Modifier.fillMaxWidth().padding(top = u.dp(2))) }
        }
    }
}

@Composable
private fun Thumb(item: XMBItem, size: Dp, radius: Dp) {
    if (item.isInstalledApp) {
        AndroidAppIcon(packageName = item.packageName, title = item.title, size = size)
        return
    }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(kindGlyph(item), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(size * 0.45f))
        item.tileArt?.let {
            AsyncImage(rememberArtworkModel(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ItemArt(item: XMBItem?, iconSize: Dp, alignment: Alignment) {
    val art = item?.backdropArt?.firstOrNull()
    when {
        art != null -> AsyncImage(
            model = rememberArtworkModel(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            modifier = Modifier.fillMaxSize(),
        )
        item?.isInstalledApp == true -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidAppIcon(packageName = item.packageName, title = item.title, size = iconSize)
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, height: Dp, modifier: Modifier) {
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
    }
}

@Composable
private fun ActionPill(button: GamepadAction, label: String, primary: Boolean, u: DesignUnits, onClick: () -> Unit) {
    val ink = if (primary) Color(0xFF0A0A0A) else Color.White
    Box(
        Modifier
            .height(u.dp(56))
            .clip(RoundedCornerShape(u.dp(28)))
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = u.dp(if (primary) 32 else 22)),
        contentAlignment = Alignment.Center,
    ) {
        val labelStyle = TextStyle(fontSize = u.sp(16), fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal)
        if (LocalPadPrompts.current) {
            ControllerPrompt(button, label, labelColor = ink, labelStyle = labelStyle, glyphSize = u.dp(20), spacing = u.dp(10))
        } else {
            Text(label, color = ink, style = labelStyle)
        }
    }
}

@Composable
private fun Headline(text: String, size: androidx.compose.ui.unit.TextUnit, lines: Int) {
    Text(text, color = Color.White, fontSize = size, lineHeight = size * 1.05f, fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-0.035).em, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

private fun kindLabel(item: XMBItem): String = recentKind(item).name.lowercase().replaceFirstChar { it.uppercase() }

private fun kindGlyph(item: XMBItem): ImageVector = when (recentKind(item)) {
    RecentKind.GAME -> Icons.Outlined.Games
    RecentKind.MUSIC -> Icons.Outlined.MusicNote
    RecentKind.VIDEO -> Icons.Outlined.Movie
    RecentKind.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    RecentKind.APP -> Icons.Outlined.Apps
}

private fun primaryLabel(item: XMBItem): String = when (recentKind(item)) {
    RecentKind.GAME, RecentKind.BOOK -> "Continue"
    RecentKind.VIDEO -> if (item.progressFraction != null) "Resume" else "Play"
    RecentKind.MUSIC -> "Play"
    RecentKind.APP -> "Open"
}

private fun infoLabel(item: XMBItem): String? = when (recentKind(item)) {
    RecentKind.GAME -> "Game info"
    RecentKind.APP -> "App info"
    else -> null
}

private fun detailLine(item: XMBItem, now: Long): String {
    val verb = when (recentKind(item)) {
        RecentKind.GAME, RecentKind.MUSIC -> "Played"
        RecentKind.VIDEO -> "Watched"
        RecentKind.BOOK -> "Opened"
        RecentKind.APP -> "Used"
    }
    val opened = item.lastOpenedAt?.let { relativeTime(now, it) }?.let { "$verb ${if (it == "Now") "just now" else it}" }
    val played = panelPlayTime(item.totalPlayTimeMillis)?.let { "$it played" }
    return listOfNotNull(opened, played).joinToString(" · ").ifBlank { kindLabel(item) }
}

private fun subLine(item: XMBItem): String? =
    (item.progressLabel ?: item.subtitle)?.takeIf { it.isNotBlank() && it != kindLabel(item) }

@Composable
fun RecentFilterRow(
    filter: RecentFilter,
    modifier: Modifier = Modifier,

    onFilterTapped: (RecentFilter) -> Unit = {},

    includeApps: Boolean = false,
) {
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        val pad = LocalPadPrompts.current
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = StripFontSize.value.dp * 1.6f, spacing = 0.dp)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,

                    onClick = { onFilterTapped(filter.next(includeApps)) },
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = filter.label,

                color = Color.White,

                fontSize = StripFontSize,
                lineHeight = StripFontSize * 1.25f,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(shadow = XmbTextShadow),
            )
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = StripFontSize.value.dp * 1.6f, spacing = 0.dp)
    }
}
