package com.echo.feature.crossbar.ui

import androidx.compose.material.icons.outlined.VideogameAsset
import com.echo.feature.appbar.AppFilter
import com.echo.core.ui.design.RailPanelFill
import com.echo.core.ui.design.RAIL_PANEL_WIDTH
import androidx.compose.foundation.border
import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.rotate
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxScope
import com.echo.core.ui.design.IsTitan2
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.design.PanelButton
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
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.RailEdgeGap
import com.echo.core.ui.components.RailRowGap
import com.echo.core.ui.components.contextMenuDim
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.feature.crossbar.viewmodel.RecentDay
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.crossbar.viewmodel.RecentKind
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.groupRecentsByDay
import com.echo.feature.crossbar.viewmodel.recentKind
import com.echo.core.common.format.playTimeLabel
import com.echo.core.common.format.relativeTime
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
    modifier: Modifier = Modifier,

    // a finger down (true) and up (false) on a card or row, so holding it launches like holding A (owner, 2026-10-05)
    onCardPressed: (Int, Boolean) -> Unit = { _, _ -> },

    // the crossbar wave, drawn over the backdrop art and under icons, panels and words (owner, 2026-10-05)
    wave: (@Composable () -> Unit)? = null,

    // the focused game's achievements, "12/40", once loaded (owner, 2026-10-08)
    achievements: String? = null,
) {
    val focused = items.getOrNull(selectedIndex)
    val now = System.currentTimeMillis()
    val empty = if (filter == RecentFilter.ALL) "Nothing played yet." else "No recent ${filter.label.lowercase()}."

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Crossfade(railVisible, animationSpec = tween(220), label = "recentRail") { rail ->
            if (rail) {
                RecentList(items, selectedIndex, focused, listState, filter, now, empty, u, onCardTapped, onCardPressed, wave)
            } else {
                Letterbox(focused, now, empty, u, wave, achievements, { onCardPressed(selectedIndex, it) }, { onCardTapped(selectedIndex) }) {
                    // owner, 2026-10-09: the Titan 2's square screen has room for the rest of Recent under the icon
                    if (IsTitan2 && items.size > 1) RecentStrip(items, selectedIndex, u, onCardTapped, onCardPressed, Modifier.align(BiasAlignment(0f, 0.12f)))
                }
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
    wave: (@Composable () -> Unit)?,
    achievements: String?,
    onArtPressed: (Boolean) -> Unit,
    onArtTapped: () -> Unit,
    extra: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxSize()
            .tapOrHold(onArtTapped, onArtPressed),
    ) {
        val kind = item?.let(::recentKind)
        if (item != null && (kind == RecentKind.MUSIC || kind == RecentKind.VIDEO || kind == RecentKind.BOOK)) {
            MediaStage(item, kind, now, u, wave)
            return@Box
        }
        BackdropArt(item, BiasAlignment(0f, -0.2f))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f))))
        wave?.invoke()
        // on the Titan 2 the icon is larger and higher, so the strip of recents fits under it
        if (IsTitan2) AppIconArt(item, u.dp(230), BiasAlignment(0f, -0.42f)) else AppIconArt(item, u.dp(150))
        extra()
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = HintBarHeight + u.dp(if (u.square) SQUARE_FOOT else 40)),
        ) {
            item?.let { Box(Modifier.padding(start = u.dp(80), bottom = u.dp(18))) { Eyebrow(it, u) } }
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
                Column(
                    Modifier.padding(top = u.dp(25), start = u.dp(80), end = u.dp(80)).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(u.dp(6)),
                ) {
                    Text(detailLine(item, now), color = Color.White, fontSize = u.sp(20), fontWeight = FontWeight.Light,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    subLine(item)?.let {
                        Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13), fontWeight = FontWeight.Light,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // a game's year, genre, developer and players, and its achievements
                    factsLine(item, achievements)?.let {
                            Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13), fontWeight = FontWeight.Light,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onCardPressed: (Int, Boolean) -> Unit,
    wave: (@Composable () -> Unit)?,
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
        Box(Modifier.fillMaxSize().padding(start = u.dp(RAIL_PANEL_WIDTH))) {
            BackdropArt(focused, Alignment.Center)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PanelBase.copy(alpha = 0.85f), 0.3f to Color.Transparent)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to PanelBase.copy(alpha = 0.92f))))
        }
        wave?.invoke()
        Box(Modifier.fillMaxSize().padding(start = u.dp(RAIL_PANEL_WIDTH))) { AppIconArt(focused, u.dp(150)) }
        Box(Modifier.fillMaxHeight().width(u.dp(RAIL_PANEL_WIDTH)).background(RailPanelFill))

        Column(
            Modifier
                .padding(start = RailEdgeGap, top = stripBandHeight(rememberStripUnits()) + 8.dp + if (u.square) StripHeight else 0.dp, bottom = u.dp(70))
                .width(u.dp(380))
                .fillMaxHeight(),
        ) {
            // kit 06: a small label, then slim rows, so the rail stays a list and not a wall
            Text(
                "Recent · ${filter.label}".uppercase(),
                style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(11), letterSpacing = 0.18.em),
                modifier = Modifier.padding(start = u.dp(4), bottom = u.dp(10)),
            )
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(RailRowGap),
                contentPadding = PaddingValues(bottom = RailRowGap),
            ) {
                items(rows, key = { row -> (row.item?.value?.id ?: row.day.name) }) { row ->
                    val entry = row.item
                    if (entry != null) {
                        val dim = contextMenuDim(abs(entry.index - selectedIndex), items.lastIndex)
                        RecentRow(entry.value, entry.index == selectedIndex, dim, now, u, { onCardPressed(entry.index, it) }) { onCardTapped(entry.index) }
                    } else {
                        Text(
                            row.day.label.uppercase(),
                            style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.4f), fontSize = u.sp(10), letterSpacing = 0.18.em),
                            modifier = Modifier.padding(start = u.dp(14), top = u.dp(8)),
                        )
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = u.dp(RAIL_PANEL_WIDTH + 60), end = u.dp(80),
                // on the Titan 2 the footer is taller than 80 design units, so the words ran into it
                bottom = if (IsTitan2) HintBarHeight + u.dp(40) else u.dp(80)),
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
            }
        }

    }
}

// a tap acts as a click; the finger's down and up are reported too, so the caller can time a hold
private fun Modifier.tapOrHold(onTap: () -> Unit, onPressed: (Boolean) -> Unit): Modifier =
    semantics { role = Role.Button; onClick { onTap(); true } }
        .pointerInput(onTap, onPressed) {
            detectTapGestures(
                onPress = {
                    onPressed(true)
                    tryAwaitRelease()
                    onPressed(false)
                },
                onTap = { onTap() },
            )
        }

// owner, 2026-10-09: the Titan 2's strip of recents: each one's icon or art in a row, the chosen one ringed. A tap
// picks it and holding launches it, as on its row in the rail
@Composable
private fun RecentStrip(
    items: List<CrossbarItem>,
    selectedIndex: Int,
    u: DesignUnits,
    onTapped: (Int) -> Unit,
    onPressed: (Int, Boolean) -> Unit,
    modifier: Modifier,
) {
    val state = rememberLazyListState()
    LaunchedEffect(selectedIndex) { if (selectedIndex in items.indices) state.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0)) }
    val tile = u.dp(96)
    val shape = RoundedCornerShape(u.dp(22))
    LazyRow(
        state = state,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(u.dp(18), Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = u.dp(80), vertical = u.dp(12)),
    ) {
        itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
            val on = index == selectedIndex
            Box(
                Modifier
                    .size(tile)
                    .graphicsLayer(alpha = if (on) 1f else 0.55f, scaleX = if (on) 1.08f else 1f, scaleY = if (on) 1.08f else 1f)
                    .clip(shape)
                    .then(if (on) Modifier.border(u.dp(3), Color.White, shape) else Modifier)
                    .tapOrHold({ onTapped(index) }, { onPressed(index, it) }),
                contentAlignment = Alignment.Center,
            ) {
                val art = item.tileArt ?: item.shelfCoverArt ?: item.backdropArt.firstOrNull()
                when {
                    item.packageName != null && art == null -> AndroidAppIcon(packageName = item.packageName, title = item.title, size = tile * 0.82f)
                    art != null -> AsyncImage(rememberArtworkModel(art), item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else -> Icon(kindGlyph(item), item.title, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(tile * 0.5f))
                }
            }
        }
    }
}

private class RailRow(val day: RecentDay, val item: IndexedValue<CrossbarItem>?)

@Composable
private fun RecentRow(item: CrossbarItem, focused: Boolean, dim: Float, now: Long, u: DesignUnits, onPressed: (Boolean) -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u.dp(12))
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (focused) 1f else dim.coerceAtLeast(0.55f))
            .clip(shape)
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.10f)).border(u.dp(2), Color.White, shape) else Modifier)
            .tapOrHold(onClick, onPressed)
            .padding(horizontal = u.dp(14), vertical = u.dp(10)),
        verticalArrangement = Arrangement.spacedBy(u.dp(6)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, color = Color.White, fontSize = u.sp(15), fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(listOfNotNull(kindLabel(item), item.lastOpenedAt?.let { relativeTime(now, it) }).joinToString(" · "),
                color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(11), fontWeight = FontWeight.Light, maxLines = 1,
                modifier = Modifier.padding(start = u.dp(10)))
        }
        item.progressFraction?.let { ProgressBar(it, u.dp(3), Modifier.fillMaxWidth()) }
    }
}


// the art fills the page like a wallpaper, so the wave draws over it. An app with no art gets the crossbar's
// own app backdrop, its icon's colour, rather than black (owner, 2026-10-08)
@Composable
private fun BackdropArt(item: CrossbarItem?, alignment: Alignment) {
    val art = item?.backdropArt?.firstOrNull()
    if (art == null) {
        item?.takeIf(::isAppWithoutArt)?.packageName?.let { CrossbarAppIconBackdrop(it, fallbackAccent = PanelBase) }
        return
    }
    AsyncImage(
        model = rememberArtworkModel(art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier = Modifier.fillMaxSize(),
    )
}

// an app with no art shows its icon, which sits above the wave. Anything with a package is an Android app or
// an Android game, so an app marked as a game (it has a game row) keeps its icon too
internal fun isAppWithoutArt(item: CrossbarItem): Boolean = item.backdropArt.isEmpty() && item.packageName != null

// owner, 2026-10-09: the app's icon again, large and faint in the bottom corner, as on its App Drawer case
@Composable
private fun AppIconArt(item: CrossbarItem?, iconSize: Dp, alignment: Alignment = Alignment.Center) {
    if (item == null || !isAppWithoutArt(item)) return
    val mark = iconSize * APP_MARK_GROW
    AndroidAppIcon(
        packageName = item.packageName, title = item.title, size = mark,
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd, unbounded = true)
            .offset(mark * 0.23f, mark * 0.14f).rotate(-14f).graphicsLayer(alpha = 0.16f),
    )
    Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
        AndroidAppIcon(packageName = item.packageName, title = item.title, size = iconSize)
    }
}

private const val APP_MARK_GROW = 2.6f

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

// a game's year, genre, developer and players, then its achievements; null when nothing is known
internal fun factsLine(item: CrossbarItem, achievements: String?): String? =
    listOfNotNull(item.metadataLine?.takeIf { it.isNotBlank() }, achievements?.let { "Achievements $it" })
        .joinToString("  ·  ").ifEmpty { null }

private fun subLine(item: CrossbarItem): String? =
    (item.progressLabel ?: item.subtitle)?.takeIf { it.isNotBlank() && it != kindLabel(item) }

@Composable
fun RecentFilterRow(
    filter: RecentFilter,
    u: DesignUnits,
    modifier: Modifier = Modifier,

    onFilterTapped: (RecentFilter) -> Unit = {},

    filters: List<RecentFilter> = listOf(RecentFilter.ALL),
) {
    StripSections(
        labels = filters.map { it.label },
        selected = filters.indexOf(filter),
        onTapped = { onFilterTapped(filters[it]) },
        u = u,
        shoulders = true,
        modifier = modifier,
    ) { i, tint, m -> Icon(filterGlyph(filters[i]), null, m, tint = tint) }
}

// the app drawer's sections as icons in the top bar, LT and RT at the ends (owner, 2026-10-04)
@Composable
fun DrawerSectionRow(active: AppFilter, sections: List<AppFilter>, u: DesignUnits, modifier: Modifier = Modifier, onTapped: (AppFilter) -> Unit) {
    StripSections(
        labels = sections.map { it.label },
        selected = sections.indexOf(active),
        onTapped = { onTapped(sections[it]) },
        u = u,
        shoulders = true,
        modifier = modifier,
    ) { i, tint, m -> Icon(drawerGlyph(sections[i]), null, m, tint = tint) }
}

private fun drawerGlyph(section: AppFilter): ImageVector = when (section) {
    AppFilter.RECENT -> Icons.Outlined.History
    AppFilter.APPS -> Icons.Outlined.Apps
    AppFilter.EMULATORS -> Icons.Outlined.VideogameAsset
    AppFilter.GAMES -> Icons.Outlined.Games
    AppFilter.MUSIC -> Icons.Outlined.MusicNote
    AppFilter.VIDEOS -> Icons.Outlined.Movie
    AppFilter.BOOKS -> Icons.AutoMirrored.Outlined.MenuBook
}

private fun filterGlyph(filter: RecentFilter): ImageVector = when (filter) {
    RecentFilter.ALL -> Icons.Outlined.History
    RecentFilter.GAMES -> Icons.Outlined.Games
    RecentFilter.MUSIC -> Icons.Outlined.MusicNote
    RecentFilter.BOOKS -> Icons.AutoMirrored.Outlined.MenuBook
    RecentFilter.VIDEO -> Icons.Outlined.Movie
    RecentFilter.APPS -> Icons.Outlined.Apps
}

// false where the page shows something that is not from Recent: the App Drawer's focus on the other screen
internal val LocalFromRecent = androidx.compose.runtime.staticCompositionLocalOf { true }

@Composable
private fun Eyebrow(item: CrossbarItem, u: DesignUnits) {
    Text(
        (if (LocalFromRecent.current) "From recent · ${kindLabel(item)}" else kindLabel(item)).uppercase(),
        style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(13), letterSpacing = 0.18.em),
        maxLines = 1,
    )
}

// kit 07-09: the art takes the shape of its kind (square record, wide frame, portrait cover) with the details beside it
@Composable
private fun MediaStage(item: CrossbarItem, kind: RecentKind, now: Long, u: DesignUnits, wave: (@Composable () -> Unit)?) {
    val art = item.shelfCoverArt ?: item.tileArt ?: item.backdropArt.firstOrNull()
    art?.let {
        AsyncImage(
            model = rememberBlurSourceModel(it),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(u.dp(40)).graphicsLayer(alpha = 0.35f),
        )
    }
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.2f), 1f to Color.Black.copy(alpha = 0.8f))))
    wave?.invoke()
    val (w, h) = when (kind) {
        RecentKind.MUSIC -> 375 to 375
        RecentKind.VIDEO -> 560 to 315
        else -> 250 to 375
    }
    Row(
        Modifier
            .fillMaxSize()
            .padding(start = u.dp(60), end = u.dp(60), top = stripBandHeight(rememberStripUnits()), bottom = HintBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(50)),
    ) {
        Box(
            Modifier.size(u.dp(w), u.dp(h)).clip(RoundedCornerShape(u.dp(16))).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(kindGlyph(item), null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(u.dp(72)))
            art?.let { AsyncImage(rememberArtworkModel(it), item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
            Eyebrow(item, u)
            Headline(item.title, u.sp(50), 2)
            // the progress label sits under the bar, so this line is the artist, season or chapter
            (item.subtitle?.takeIf { it.isNotBlank() && it != kindLabel(item) } ?: detailLine(item, now).takeIf { it.isNotBlank() })?.let {
                Text(it, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(18), fontWeight = FontWeight.Light,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            item.progressFraction?.let { p ->
                Column(Modifier.width(u.dp(450)).padding(top = u.dp(16)), verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
                    ProgressBar(p, u.dp(4), Modifier.fillMaxWidth())
                    item.progressLabel?.let {
                        Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(13), fontWeight = FontWeight.Light, maxLines = 1)
                    }
                }
            }
        }
    }
}

// owner, 2026-10-09: Last Played with nothing played yet: the ECHO mark, a line, and the two ways to something to
// play, the Game column and the App Drawer. The buttons name the pad's own way there: Right, and LB
@Composable
internal fun LastPlayedEmpty(onGames: (() -> Unit)?, onAppDrawer: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = u.dp(120), top = u.dp(90)),
            verticalArrangement = Arrangement.spacedBy(u.dp(16)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.echo.core.ui.R.drawable.echo_logo), null,
                    Modifier.size(u.dp(40)),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                )
                Text("ECHO", style = u.eyebrow(Color.White.copy(alpha = 0.8f)))
            }
            Text("Start something.", color = Color.White, fontSize = u.sp(48), fontWeight = FontWeight.Light)
            Text("What you play shows up here.", color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(20), fontWeight = FontWeight.Light)
            Row(Modifier.padding(top = u.dp(12)), horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
                onGames?.let { PanelButton(GamepadAction.NAVIGATE_RIGHT, "Games", u, onClick = it) }
                PanelButton(GamepadAction.PREV_PAGE, "App Drawer", u, onClick = onAppDrawer)
            }
        }
    }
}
