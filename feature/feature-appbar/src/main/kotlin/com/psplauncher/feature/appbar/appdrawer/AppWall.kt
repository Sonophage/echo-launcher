package com.psplauncher.feature.appbar.appdrawer

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.psplauncher.core.common.format.relativeTime
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPrompt
import com.psplauncher.core.ui.components.LocalPadPrompts
import com.psplauncher.core.ui.components.initialOf
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.PanelButton
import com.psplauncher.core.ui.design.panelBackdrop
import com.psplauncher.core.ui.icons.AppIconArt
import com.psplauncher.core.ui.icons.rememberAppIcon
import com.psplauncher.feature.appbar.AppFilter
import com.psplauncher.feature.appbar.InstalledApp
import com.psplauncher.feature.appbar.WALL_COLUMNS
import com.psplauncher.feature.appbar.wallLayout

@Composable
internal fun WallBackdrop(app: InstalledApp?, icon: AppIconArt?, u: DesignUnits) {
    val tint by animateColorAsState(icon?.color ?: NeutralTint, tween(500), label = "wallTint")
    Box(Modifier.fillMaxSize().panelBackdrop(tint)) {
        app?.art?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(u.dp(26))) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.55f else 0.75f)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppWall(
    apps: List<InstalledApp>,
    sectionCount: Int,
    filter: AppFilter,
    selectedIndex: Int,
    usingTouch: Boolean,
    u: DesignUnits,
    onAppTapped: (Int) -> Unit,
    onAppLaunched: (String) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    restHeading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = remember(sectionCount, apps.size) { wallLayout(sectionCount, apps.size) }
    val lines = remember(cells) {
        cells.indices.groupBy { i -> if (sectionCount > 0 && cells[i].row < 2) 0 else cells[i].row }.values.toList()
    }
    val restLine = if (sectionCount < apps.size) lines.indexOfFirst { it.first() >= sectionCount } else -1
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex, usingTouch, lines) {
        if (usingTouch) return@LaunchedEffect
        val line = lines.indexOfFirst { selectedIndex in it }
        if (line < 0) return@LaunchedEffect
        val item = line + if (restLine in 0..line) 1 else 0
        val shown = listState.layoutInfo.visibleItemsInfo
        val fits = shown.any { it.index == item && it.offset >= 0 && it.offset + it.size <= listState.layoutInfo.viewportEndOffset }
        if (!fits) listState.animateScrollToItem((item - 1).coerceAtLeast(0))
    }

    BoxWithConstraints(modifier) {
        val gap = u.dp(14)
        val cell = (maxWidth - gap * (WALL_COLUMNS - 1)) / WALL_COLUMNS
        val rowHeight = u.dp(132)
        val tile: @Composable (Int, Boolean) -> Unit = { index, big ->
            val app = apps[index]
            WallTile(
                app = app,
                focused = !usingTouch && index == selectedIndex,
                eyebrow = if (big && filter == AppFilter.RECENT) (if (app.isGame || app.gameId != null) "Last played" else "Recent") else null,
                width = if (big) cell * 2 + gap else cell,
                height = if (big) rowHeight * 2 + gap else rowHeight,
                big = big,
                u = u,
                onClick = { onAppTapped(index); onAppLaunched(app.packageName) },
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
                if (line == restLine) item(key = "rest") { restHeading() }
                item(key = indices.first()) {
                    if (line == 0 && sectionCount > 0) {
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
    val shape = RoundedCornerShape(u.dp(if (big) 20 else 16))
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
            big -> TileGlyph(icon, app, 120, u, Modifier.align(Alignment.Center))
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
                    .padding(horizontal = u.dp(20), vertical = u.dp(18)),
                verticalArrangement = Arrangement.spacedBy(u.dp(4)),
            ) {
                eyebrow?.let { Text(it.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(11), letterSpacing = 0.16.em)) }
                Text(app.label, color = Color.White, fontSize = u.sp(22), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
internal fun WallBand(app: InstalledApp, u: DesignUnits, onLaunch: () -> Unit, onOptions: () -> Unit) {
    val kind = when {
        app.isEmulator -> "Emulator"
        app.isGame || app.gameId != null -> "Game"
        else -> "App"
    }
    val played = app.gameId != null
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
        Row(
            Modifier.fillMaxWidth().padding(top = u.dp(20)),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(u.dp(24)),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                Text(kind.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(13), letterSpacing = 0.16.em))
                Text(app.label, color = Color.White, fontSize = u.sp(40), lineHeight = u.sp(42), fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-0.02).em, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (app.lastUsedAt > 0L) {
                    Text("${if (played) "Last played" else "Last used"} ${relativeTime(System.currentTimeMillis(), app.lastUsedAt)}",
                        color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(14), fontWeight = FontWeight.Light)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
                PanelButton(GamepadAction.SELECT, if (played || app.isGame) "Play" else "Open", u, onLaunch)
                if (app.gameId == null) PanelButton(GamepadAction.CHANGE_SORT, "Options", u, onOptions)
            }
        }
    }
}

@Composable
internal fun WallHints(u: DesignUnits, onNextTab: () -> Unit, onSearch: () -> Unit, onBack: () -> Unit) {
    val pad = LocalPadPrompts.current
    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(if (pad) 26 else 12)), verticalAlignment = Alignment.CenterVertically) {
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
internal fun WallHeading(text: String, u: DesignUnits) {
    Text(text.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), letterSpacing = 0.16.em),
        modifier = Modifier.padding(top = u.dp(6)))
}

private val NeutralTint = Color(0xFF222838)
