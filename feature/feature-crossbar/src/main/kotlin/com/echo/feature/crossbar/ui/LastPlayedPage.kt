package com.echo.feature.crossbar.ui

import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kotlin.math.abs
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ContextMenuEyebrow
import com.echo.core.ui.components.ContextMenuHeader
import com.echo.core.ui.components.ContextMenuRowLabel
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.RailCorner
import com.echo.core.ui.components.RailEdgeGap
import com.echo.core.ui.components.RailGap
import com.echo.core.ui.components.RailIcon
import com.echo.core.ui.components.RailRowGap
import com.echo.core.ui.components.RailSubtitleSize
import com.echo.core.ui.components.contextMenuDim
import com.echo.core.ui.components.contextMenuInk
import com.echo.core.ui.components.contextMenuRow
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.feature.crossbar.viewmodel.RecentDay
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.crossbar.viewmodel.RecentKind
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.groupRecentsByDay
import com.echo.feature.crossbar.viewmodel.holdMsFor
import com.echo.feature.crossbar.viewmodel.isInstalledApp
import com.echo.feature.crossbar.viewmodel.recentKind
import com.echo.core.common.format.playTimeLabel
import com.echo.core.common.format.relativeTime
import com.echo.core.ui.design.PanelButton
import com.echo.feature.crossbar.viewmodel.removableFromRecent
import com.echo.core.ui.design.panelDesignUnits

private const val SQUARE_FOOT = 64

@Composable
fun LastPlayedPage(
    items: List<CrossbarItem>,
    selectedIndex: Int,
    listState: LazyListState,

    filter: RecentFilter,

    railVisible: Boolean,
    onCardTapped: (Int) -> Unit,
    onAction: (GamepadAction) -> Unit,
    modifier: Modifier = Modifier,

    launchHold: String? = null,
) {
    val focused = items.getOrNull(selectedIndex)
    val now = System.currentTimeMillis()
    val empty = if (filter == RecentFilter.ALL) "Nothing played yet." else "No recent ${filter.label.lowercase()}."

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Crossfade(railVisible, animationSpec = tween(220), label = "recentRail") { rail ->
            if (rail) {
                RecentList(items, selectedIndex, focused, listState, filter, now, empty, u, onCardTapped, onAction, launchHold)
            } else {
                Letterbox(focused, now, empty, u, { onCardTapped(selectedIndex) }, onAction, launchHold)
            }
        }
    }
}

@Composable
private fun Letterbox(
    item: CrossbarItem?,
    now: Long,
    empty: String,
    u: DesignUnits,
    onArtTapped: () -> Unit,
    onAction: (GamepadAction) -> Unit,
    launchHold: String?,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onArtTapped),
    ) {
        ItemArt(item, u.dp(150), BiasAlignment(0f, -0.2f))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f))))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = HintBarHeight + u.dp(if (u.square) SQUARE_FOOT else 40)),
        ) {
            Box(Modifier.padding(start = u.dp(80), end = u.dp(80), bottom = u.dp(14))) {
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
            Box(Modifier.padding(start = u.dp(80), end = u.dp(80)).fillMaxWidth().height(u.dp(3))) {
                item?.progressFraction?.let { p ->
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.15f))) {
                        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
                    }
                }
            }
            if (item != null) {
                Row(
                    Modifier.padding(top = u.dp(25), start = u.dp(80), end = u.dp(80)).fillMaxWidth(),
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
                        PanelButton(GamepadAction.SELECT, primaryLabel(item), u, holdMsFor(item), launchHold == item.id) { onAction(GamepadAction.SELECT) }
                        infoLabel(item)?.let { PanelButton(GamepadAction.CHANGE_SORT, it, u) { onAction(GamepadAction.CHANGE_SORT) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentList(
    items: List<CrossbarItem>,
    selectedIndex: Int,
    focused: CrossbarItem?,
    listState: LazyListState,
    filter: RecentFilter,
    now: Long,
    empty: String,
    u: DesignUnits,
    onCardTapped: (Int) -> Unit,
    onAction: (GamepadAction) -> Unit,
    launchHold: String?,
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
                model = rememberBlurSourceModel(art),
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

        Column(
            Modifier
                .padding(start = RailEdgeGap, top = stripBandHeight(rememberStripUnits()) + 8.dp + if (u.square) StripHeight else 0.dp, bottom = u.dp(70))
                .width(u.dp(380))
                .fillMaxHeight(),
        ) {
            ContextMenuHeader("Recently opened", filter.label.uppercase(), textAlign = TextAlign.Start)
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(RailRowGap),
                contentPadding = PaddingValues(bottom = RailRowGap),
            ) {
                items(rows, key = { row -> (row.item?.value?.id ?: row.day.name) }) { row ->
                    val entry = row.item
                    if (entry != null) {
                        val dim = contextMenuDim(abs(entry.index - selectedIndex), items.lastIndex)
                        RecentRow(entry.value, entry.index == selectedIndex, dim, now) { onCardTapped(entry.index) }
                    } else {
                        ContextMenuEyebrow(row.day.label.uppercase(), textAlign = TextAlign.Start)
                    }
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
                    style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(13), letterSpacing = 0.18.em),
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
                    PanelButton(GamepadAction.SELECT, primaryLabel(focused), u, holdMsFor(focused), launchHold == focused.id) { onAction(GamepadAction.SELECT) }
                    if (focused.removableFromRecent) {
                        PanelButton(GamepadAction.CHANGE_SORT, "Remove", u) { onAction(GamepadAction.CHANGE_SORT) }
                    }
                }
            }
        }

    }
}

private class RailRow(val day: RecentDay, val item: IndexedValue<CrossbarItem>?)

@Composable
private fun RecentRow(item: CrossbarItem, focused: Boolean, dim: Float, now: Long, onClick: () -> Unit) {
    val ink = contextMenuInk(focused)
    Row(
        Modifier.fillMaxWidth().contextMenuRow(focused, dim, onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RailGap),
    ) {
        Column(Modifier.weight(1f)) {
            ContextMenuRowLabel(item.title, focused)
            Text(
                listOfNotNull(kindLabel(item), item.lastOpenedAt?.let { relativeTime(now, it) }).joinToString(" · "),
                color = ink.copy(alpha = 0.62f), fontSize = RailSubtitleSize, lineHeight = RailSubtitleSize * 1.2f, maxLines = 1,
            )
            item.progressFraction?.let { ProgressBar(it, 2.dp, Modifier.fillMaxWidth().padding(top = 2.dp), ink) }
        }
        Thumb(item, RailIcon, RailCorner)
    }
}

@Composable
private fun Thumb(item: CrossbarItem, size: Dp, radius: Dp) {
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
private fun ItemArt(item: CrossbarItem?, iconSize: Dp, alignment: Alignment) {
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
private fun ProgressBar(fraction: Float, height: Dp, modifier: Modifier, color: Color = Color.White) {
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(color.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

private fun kindLabel(item: CrossbarItem): String = recentKind(item).name.lowercase().replaceFirstChar { it.uppercase() }

private fun kindGlyph(item: CrossbarItem): ImageVector = when (recentKind(item)) {
    RecentKind.GAME -> Icons.Outlined.Games
    RecentKind.MUSIC -> Icons.Outlined.MusicNote
    RecentKind.VIDEO -> Icons.Outlined.Movie
    RecentKind.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    RecentKind.APP -> Icons.Outlined.Apps
}

private fun primaryLabel(item: CrossbarItem): String = when (recentKind(item)) {
    RecentKind.GAME, RecentKind.BOOK -> "Continue"
    RecentKind.VIDEO -> if (item.progressFraction != null) "Resume" else "Play"
    RecentKind.MUSIC -> "Play"
    RecentKind.APP -> "Open"
}

private fun infoLabel(item: CrossbarItem): String? = when (recentKind(item)) {
    RecentKind.GAME -> "Game info"
    RecentKind.APP -> "App info"
    else -> null
}

private fun detailLine(item: CrossbarItem, now: Long): String {
    val verb = when (recentKind(item)) {
        RecentKind.GAME, RecentKind.MUSIC -> "Played"
        RecentKind.VIDEO -> "Watched"
        RecentKind.BOOK -> "Opened"
        RecentKind.APP -> "Used"
    }
    val opened = item.lastOpenedAt?.let { relativeTime(now, it) }?.let { "$verb ${if (it == "Now") "just now" else it}" }
    val played = item.totalPlayTimeMillis.takeIf { it > 0L }?.let(::playTimeLabel)?.let { "$it played" }
    return listOfNotNull(opened, played).joinToString(" · ").ifBlank { kindLabel(item) }
}

private fun subLine(item: CrossbarItem): String? =
    (item.progressLabel ?: item.subtitle)?.takeIf { it.isNotBlank() && it != kindLabel(item) }

@Composable
fun RecentFilterRow(
    filter: RecentFilter,
    u: DesignUnits,
    modifier: Modifier = Modifier,

    onFilterTapped: (RecentFilter) -> Unit = {},

    includeApps: Boolean = false,

    onSearch: (() -> Unit)? = null,
) {
    val filters = RecentFilter.visible(includeApps)
    StripSections(
        labels = filters.map { it.label },
        selected = filters.indexOf(filter),
        onTapped = { onFilterTapped(filters[it]) },
        u = u,
        shoulders = true,
        modifier = modifier,
        onSearch = onSearch,
    ) { i, tint, m -> Icon(filterGlyph(filters[i]), null, m, tint = tint) }
}

private fun filterGlyph(filter: RecentFilter): ImageVector = when (filter) {
    RecentFilter.ALL -> Icons.Outlined.History
    RecentFilter.GAMES -> Icons.Outlined.Games
    RecentFilter.MUSIC -> Icons.Outlined.MusicNote
    RecentFilter.BOOKS -> Icons.AutoMirrored.Outlined.MenuBook
    RecentFilter.VIDEO -> Icons.Outlined.Movie
    RecentFilter.APPS -> Icons.Outlined.Apps
}
