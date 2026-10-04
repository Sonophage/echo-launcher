package com.echo.feature.appbar.appdrawer

import com.echo.core.common.format.playTimeLabel
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.echo.core.common.format.relativeTime
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.components.initialOf
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PanelButton
import com.echo.core.ui.design.panelBackdrop
import com.echo.core.ui.icons.AppIconArt
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.feature.appbar.AppFilter
import com.echo.feature.appbar.InstalledApp
import com.echo.feature.appbar.SystemChip
import com.echo.feature.appbar.WALL_COLUMNS
import com.echo.feature.appbar.wallLayout
import com.echo.core.ui.design.PanelBase

@Composable
internal fun WallBackdrop(app: InstalledApp?, icon: AppIconArt?, u: DesignUnits) {
    val tint by animateColorAsState(icon?.color ?: NeutralTint, tween(500), label = "wallTint")
    Box(Modifier.fillMaxSize().panelBackdrop(tint)) {
        app?.art?.let { AsyncImage(rememberBlurSourceModel(it), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(u.dp(26))) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.55f else 0.75f)))
    }
}

@Composable
internal fun WallHero(app: InstalledApp?, icon: AppIconArt?, u: DesignUnits, modifier: Modifier = Modifier) {
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(HeroFade, blendMode = BlendMode.DstIn)
            },
    ) {
        when {
            app == null -> Unit
            app.art != null -> AsyncImage(app.art, null, contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.2f, -0.3f), modifier = Modifier.fillMaxSize())
            else -> {
                val tint = icon?.color ?: NeutralTint
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.6f), tint.copy(alpha = 0.12f)))))
                TileGlyph(icon, app, 150, u, Modifier.align(Alignment.Center).padding(bottom = u.dp(180)))
            }
        }
    }
}

@Composable
internal fun WallShade() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to PanelBase.copy(alpha = 0.5f), 0.2f to Color.Transparent)))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.52f to Color.Transparent, 1f to PanelBase.copy(alpha = 0.92f))))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppWall(
    apps: List<InstalledApp>,
    filter: AppFilter,
    selectedIndex: Int,
    usingTouch: Boolean,
    u: DesignUnits,
    onAppTapped: (Int) -> Unit,
    onAppLaunched: (String) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = remember(apps.size) { wallLayout(apps.size) }
    val lines = remember(cells) {
        cells.indices.groupBy { i -> if (cells[i].row < 2) 0 else cells[i].row }.values.toList()
    }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, usingTouch, lines) {
        if (usingTouch) return@LaunchedEffect
        val line = lines.indexOfFirst { selectedIndex in it }
        if (line < 0) return@LaunchedEffect
        val item = line
        val shown = listState.layoutInfo.visibleItemsInfo
        val fits = shown.any { it.index == item && it.offset >= 0 && it.offset + it.size <= listState.layoutInfo.viewportEndOffset }
        if (!fits) listState.animateScrollToItem((item - 1).coerceAtLeast(0))
    }

    BoxWithConstraints(modifier) {
        val gap = u.dp(14)
        val cell = (maxWidth - gap * (WALL_COLUMNS - 1)) / WALL_COLUMNS
        val rowHeight = u.dp(112)
        val tile: @Composable (Int, Boolean) -> Unit = { index, big ->
            val app = apps[index]
            WallTile(
                app = app,
                focused = index == selectedIndex,
                eyebrow = if (big && filter == AppFilter.RECENT) "Last opened" else null,
                width = if (big) cell * 2 + gap else cell,
                height = if (big) rowHeight * 2 + gap else rowHeight,
                big = big,
                u = u,
                onClick = {
                    val chosen = index == selectedIndex
                    onAppTapped(index)
                    if (chosen) onAppLaunched(app.packageName)
                },
                onLongClick = { onAppTapped(index); onAppMenu(app) },
            )
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(vertical = u.dp(10)),
            modifier = Modifier.fillMaxSize(),
        ) {
            lines.forEachIndexed { line, indices ->
                item(key = indices.first()) {
                    if (line == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            tile(0, true)
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                indices.drop(1).groupBy { cells[it].row }.values.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) { row.forEach { tile(it, false) } }
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) { indices.forEach { tile(it, false) } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallTile(
    app: InstalledApp,
    focused: Boolean,
    eyebrow: String?,
    width: Dp,
    height: Dp,
    big: Boolean,
    u: DesignUnits,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val grow by animateFloatAsState(if (focused) 1.04f else 1f, tween(200), label = "wallTile")
    val shape = RoundedCornerShape(u.dp(16))
    val icon = if (app.gameId == null && app.art == null) rememberAppIcon(app.packageName) else null
    Box(
        Modifier
            .zIndex(if (focused) 1f else 0f)
            .size(width, height)
            .scale(grow)
            .clip(shape)
            .background(icon?.color?.copy(alpha = 0.35f) ?: Color.White.copy(alpha = 0.08f))
            .then(if (focused) Modifier.border(u.dp(2.5f), Color.White, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        when {
            app.art != null -> AsyncImage(app.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            big -> TileGlyph(icon, app, 104, u, Modifier.align(BiasAlignment(0f, -0.45f)))
            else -> Column(
                Modifier.align(Alignment.Center).padding(horizontal = u.dp(10)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.dp(8)),
            ) {
                TileGlyph(icon, app, 52, u, Modifier)
                Text(app.label, color = Color.White, fontSize = u.sp(13), fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
        if (big) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE604060C))))
                    .padding(horizontal = u.dp(16), vertical = u.dp(14)),
                verticalArrangement = Arrangement.spacedBy(u.dp(3)),
            ) {
                eyebrow?.let { Text(it.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(10), letterSpacing = 0.16.em)) }
                Text(app.label, color = Color.White, fontSize = u.sp(17), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TileGlyph(icon: AppIconArt?, app: InstalledApp, px: Int, u: DesignUnits, modifier: Modifier) {
    if (icon != null) {
        Image(icon.bitmap, null, modifier = modifier.size(u.dp(px)))
    } else {
        Box(modifier.size(u.dp(px)), contentAlignment = Alignment.Center) {
            Text(initialOf(app.label).toString(), color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(px * 0.6f), fontWeight = FontWeight.ExtraLight)
        }
    }
}

@Composable
internal fun WallInfo(app: InstalledApp, u: DesignUnits, onLaunch: () -> Unit, onOptions: () -> Unit, modifier: Modifier = Modifier) {
    val game = app.isGame || app.gameId != null
    val kind = when {
        app.isEmulator -> "Emulator"
        game -> "Game"
        else -> "App"
    }
    val eyebrow = if (app.lastUsedAt > 0L) {
        "$kind · ${if (game) "Played" else "Used"} ${relativeTime(System.currentTimeMillis(), app.lastUsedAt).lowercase()}"
    } else kind
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(u.dp(14))) {
        Text(eyebrow.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(12), letterSpacing = 0.16.em))
        Text(app.label, color = Color.White, fontSize = u.sp(52), lineHeight = u.sp(54), fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-0.03).em, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (app.playTimeMillis > 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(32))) {
                Stat(playTimeLabel(app.playTimeMillis), "Played", u)
            }
        }
        Row(Modifier.padding(top = u.dp(6)), horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
            PanelButton(GamepadAction.SELECT, actionLabel(app), u, onLaunch)
            if (app.gameId == null) PanelButton(GamepadAction.CHANGE_SORT, "Options", u, onOptions)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(3))) {
        Text(value, color = Color.White, fontSize = u.sp(18), fontWeight = FontWeight.Light)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(11), fontWeight = FontWeight.Light)
    }
}

internal fun actionLabel(app: InstalledApp): String = if (app.isGame || app.gameId != null) "Play" else "Open"

@Composable
internal fun WallHints(u: DesignUnits, action: String?, onAction: () -> Unit, onNextTab: () -> Unit, onSearch: () -> Unit, onBack: () -> Unit) {
    val pad = LocalPadPrompts.current
    Row(
        Modifier.fillMaxWidth().height(u.dp(72)),
        horizontalArrangement = Arrangement.spacedBy(u.dp(if (pad) 28 else 12)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pad && action != null) Hint(listOf(GamepadAction.SELECT), action, u, onAction)
        if (pad) Hint(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Tabs", u, onNextTab)
        Hint(listOf(GamepadAction.OPEN_CONTEXT_MENU), "Search", u, onSearch)
        Hint(listOf(GamepadAction.BACK), "Back", u, onBack)
    }
}

@Composable
private fun Hint(actions: List<GamepadAction>, label: String, u: DesignUnits, onClick: () -> Unit) {
    val style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(13), fontWeight = FontWeight.Light)
    if (LocalPadPrompts.current) {
        ControllerPrompt(actions, label, Modifier.clip(RoundedCornerShape(u.dp(8))).clickable(onClick = onClick),
            labelStyle = style, glyphSize = u.dp(22), spacing = u.dp(8))
    } else {
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onClick)
                .padding(horizontal = u.dp(22)),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = style.copy(color = Color.White)) }
    }
}

@Composable
internal fun SystemChipRow(
    chips: List<SystemChip>,
    selected: String?,
    focused: Boolean,
    u: DesignUnits,
    onChip: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val at = chips.indexOfFirst { it.id == selected }.coerceAtLeast(0)
    LaunchedEffect(at) { listState.animateScrollToItem((at - 1).coerceAtLeast(0)) }
    LazyRow(state = listState, modifier = modifier, horizontalArrangement = Arrangement.spacedBy(u.dp(8)),
        verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(chips, key = { _, chip -> chip.id ?: "" }) { index, chip ->
            val on = index == at
            val shape = RoundedCornerShape(22.dp)
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(shape)
                    .background(if (on) Color.White else Color.White.copy(alpha = 0.1f))
                    .then(if (on && focused) Modifier.border(u.dp(2.5f), Color.White.copy(alpha = 0.5f), shape) else Modifier)
                    .clickable { onChip(chip.id) }
                    .padding(horizontal = u.dp(18)),
                horizontalArrangement = Arrangement.spacedBy(u.dp(6)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ink = if (on) PanelBase else Color.White
                Text(chip.label, color = ink, fontSize = u.sp(14), fontWeight = if (on) FontWeight.Medium else FontWeight.Light, maxLines = 1)
                Text(chip.count.toString(), color = ink.copy(alpha = 0.6f), fontSize = u.sp(11), fontWeight = FontWeight.Light)
            }
        }
    }
}

private val NeutralTint = Color(0xFF222838)


private val HeroFade = Brush.horizontalGradient(
    0f to Color.Transparent,
    0.14f to Color.Black.copy(alpha = 0.35f),
    0.3f to Color.Black.copy(alpha = 0.8f),
    0.46f to Color.Black,
)
