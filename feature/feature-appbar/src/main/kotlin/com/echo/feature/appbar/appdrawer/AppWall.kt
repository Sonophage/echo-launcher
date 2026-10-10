package com.echo.feature.appbar.appdrawer

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.echo.core.ui.design.ShelfRoom
import com.echo.core.ui.design.DRAWER_LOGO_SHARE
import com.echo.core.ui.design.DRAWER_MARK_SHARE
import com.echo.core.ui.design.VhsAppFace
import com.echo.core.ui.design.VhsCase
import com.echo.core.ui.design.VhsCoverArt
import com.echo.core.ui.design.caseShape
import com.echo.core.ui.design.roomGlow
import com.echo.core.ui.design.vignette
import com.echo.core.ui.design.wallStripes
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.common.format.playTimeLabel
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.echo.core.ui.icons.AppIconArt
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.feature.appbar.InstalledApp
import com.echo.feature.appbar.SystemChip
import com.echo.feature.appbar.WALL_COLUMNS
import com.echo.feature.appbar.WALL_ROWS
import androidx.compose.ui.unit.Dp
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.IsTitan2

// owner, 2026-10-05: the drawer is the "Drawer and Search Variations" design's 6a: a dark room lit by the
// selected app's colour, its big icon faint on the wall, and the apps standing as cases in three columns
@Composable
internal fun WallBackdrop(app: InstalledApp?, icon: AppIconArt?, u: DesignUnits, wallpaper: (@Composable () -> Unit)? = null) {
    val tint by animateColorAsState(icon?.color ?: NeutralTint, tween(500), label = "wallTint")
    Box(Modifier.fillMaxSize()) {
        // owner, 2026-10-09: the app's colour is a tint over the wallpaper, which still shows; without one (the
        // second screen's drawer) the room stays solid
        if (wallpaper != null) {
            wallpaper()
            Box(Modifier.fillMaxSize().background(ShelfRoom.copy(alpha = WALL_VEIL)).background(tint.copy(alpha = WALL_TINT)))
        } else {
            Box(Modifier.fillMaxSize().background(ShelfRoom))
        }
        // over a wallpaper the tint is the app's colour; the room's glow would cover the wallpaper again
        Box(Modifier.fillMaxSize().then(if (wallpaper == null) Modifier.roomGlow(tint) else Modifier).wallStripes()) {
            when {
                app?.art != null -> AsyncImage(rememberBlurSourceModel(app.art), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(u.dp(28)).graphicsLayer(alpha = 0.25f))
                // owner, 2026-10-09: the app's icon, large and faint, in the bottom left corner
                icon != null -> Image(icon.bitmap, null, Modifier.align(Alignment.BottomStart).offset(u.dp(-90), u.dp(110))
                    .size(u.dp(520)).rotate(-12f).graphicsLayer(alpha = 0.09f))
            }
            Box(Modifier.fillMaxSize().vignette())
        }
    }
}

// how much of the room's dark and of the app's colour lie over the wallpaper
private const val WALL_VEIL = 0.40f
private const val WALL_TINT = 0.22f

// what a case's spine says: the console for a game in the library, else the kind of app
internal fun caseLabel(app: InstalledApp): String = when {
    app.media != null -> app.media.kind.label
    app.platformName != null -> app.platformName
    app.isEmulator -> "Emulator"
    app.isGame || app.gameId != null -> "Game"
    else -> when (app.systemCategory) {
        ApplicationInfo.CATEGORY_AUDIO -> "Music"
        ApplicationInfo.CATEGORY_VIDEO -> "Video"
        ApplicationInfo.CATEGORY_IMAGE -> "Photos"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "News"
        ApplicationInfo.CATEGORY_MAPS -> "Maps"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Tools"
        else -> "App"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppWall(
    apps: List<InstalledApp>,
    selectedIndex: Int,
    usingTouch: Boolean,
    u: DesignUnits,
    onAppTapped: (Int) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    // a new tab or filter brings a new list with the same index, so the list itself is a key too
    LaunchedEffect(selectedIndex, usingTouch, apps) {
        if (usingTouch || selectedIndex !in apps.indices) return@LaunchedEffect
        val shown = gridState.layoutInfo.visibleItemsInfo
        val fits = shown.any { it.index == selectedIndex && it.offset.y >= 0 && it.offset.y + it.size.height <= gridState.layoutInfo.viewportEndOffset }
        if (!fits) gridState.animateScrollToItem((selectedIndex - WALL_COLUMNS).coerceAtLeast(0))
    }
    // owner, 2026-10-05: no row is cut off at the bottom. The cases are as tall as fits WALL_ROWS whole rows in
    // the space, and the d-pad scrolls a row at a time, so the rows always land whole
    BoxWithConstraints(modifier) {
        // the Titan 2's cases are big enough that the chosen one, lifted and grown, needs more room above and beside it
        val edge = if (IsTitan2) u.dp(32) else u.dp(16)
        val side = if (IsTitan2) u.dp(16) else 0.dp
        val caseHeight = (maxHeight - edge - u.dp(16) - u.dp(24) * (WALL_ROWS - 1)) / WALL_ROWS
        LazyVerticalGrid(
            columns = GridCells.Fixed(WALL_COLUMNS),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(u.dp(28)),
            verticalArrangement = Arrangement.spacedBy(u.dp(24)),
            // room for the chosen case to rise
            contentPadding = PaddingValues(top = edge, bottom = u.dp(16), start = side, end = side),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName + (app.gameId ?: "") }) { index, app ->
                AppCase(
                    app = app,
                    height = caseHeight,
                    focused = index == selectedIndex,
                    dimmed = selectedIndex in apps.indices && index != selectedIndex,
                    u = u,
                    // a tap only picks the app; it opens by holding the launch button (owner, 2026-10-04)
                    onClick = { onAppTapped(index) },
                    onLongClick = { onAppTapped(index); onAppMenu(app) },
                )
            }
        }
    }
}

// one app as the shared VHS case (VhsCase): a game's own art keeps its shape on the ribbed plastic, an app's cover
// is its colour, rings and icon
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCase(app: InstalledApp, height: Dp, focused: Boolean, dimmed: Boolean, u: DesignUnits, onClick: () -> Unit, onLongClick: () -> Unit) {
    val lift by animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "caseLift")
    // owner, 2026-10-05: the chosen case stands a little bigger than the rest, and the rest step back
    val shade by animateFloatAsState(if (dimmed) 0.6f else 1f, tween(200), label = "caseShade")
    val icon = if (app.art == null) rememberAppIcon(app.packageName.takeIf { app.gameId == null }) else null
    val tint = icon?.color ?: NeutralTint
    val shape = caseShape(u)
    val rise = with(LocalDensity.current) { u.dp(10).toPx() }
    VhsCase(
        label = caseLabel(app), tint = tint, u = u,
        modifier = Modifier
            .zIndex(if (focused) 1f else 0f)
            .height(height)
            .graphicsLayer {
                translationY = -rise * lift
                scaleX = 1f + 0.07f * lift
                scaleY = 1f + 0.07f * lift
                alpha = shade
            }
            .shadow(u.dp(if (focused) 18 else 10), shape)
            .then(if (focused) Modifier.border(u.dp(3), Color.White, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (app.art != null) VhsCoverArt(app.art, u)
        else VhsAppFace(app.label, icon, tint, u, grows = true, logoShare = DRAWER_LOGO_SHARE, markShare = DRAWER_MARK_SHARE, nameGrows = false) {
            Text(initialOf(app.label).toString(), color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(40), fontWeight = FontWeight.ExtraLight)
        }
    }
}

// the left column: the app's tile, name and kind, a pill with what it is, what is known about it, when it was
// last used, and its buttons
@Composable
internal fun WallInfo(
    app: InstalledApp,
    icon: AppIconArt?,
    u: DesignUnits,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    holding: Boolean = false,
    details: com.echo.feature.appbar.GameDetails? = null,
) {
    val game = app.isGame || app.gameId != null
    val kind = when {
        app.media != null -> app.media.kind.label
        app.isEmulator -> "Emulator"
        game -> "Game"
        else -> "App"
    }
    val tint = icon?.color ?: NeutralTint
    Column(modifier, verticalArrangement = Arrangement.spacedBy(u.dp(12))) {
        Box(
            Modifier.size(u.dp(84)).shadow(u.dp(14), RoundedCornerShape(u.dp(22))).clip(RoundedCornerShape(u.dp(22))).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            when {
                app.art != null -> AsyncImage(app.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                icon != null -> Image(icon.bitmap, null, Modifier.size(u.dp(50)))
                else -> Text(initialOf(app.label).toString(), color = Color.White, fontSize = u.sp(36), fontWeight = FontWeight.ExtraLight)
            }
        }
        Text(app.label, color = Color.White, fontSize = u.sp(30), lineHeight = u.sp(33), fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(kind.uppercase(), style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(12), letterSpacing = 0.2.em))
        // an album's artist, a book's author, and its genre
        app.media?.let { m -> com.echo.feature.appbar.mediaByline(m) }?.let {
            Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = u.sp(16), fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val pillRight = when {
            app.playTimeMillis > 0L -> playTimeLabel(app.playTimeMillis)
            else -> null
        }
        // the kind pill names the system or kind; a media case says its kind above already
        if (app.media == null) Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(u.dp(20))).background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(u.dp(20))).padding(horizontal = u.dp(16), vertical = u.dp(8)),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(caseLabel(app), color = Color.White, fontSize = u.sp(14), fontWeight = FontWeight.SemiBold, maxLines = 1)
            pillRight?.let { Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(14), maxLines = 1) }
        }
        (details?.description ?: details?.facts)?.let {
            Text("ABOUT", style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(12), letterSpacing = 0.2.em))
            Text(it, color = Color.White, fontSize = u.sp(15), lineHeight = u.sp(22), fontWeight = FontWeight.Medium, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
        // owner, 2026-10-08: achievements under the details
        details?.achievements?.let {
            Text("ACHIEVEMENTS  $it", style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.8f), fontSize = u.sp(13), letterSpacing = 0.15.em))
        }
        // the last badges earned, in one row (owner, 2026-10-08)
        details?.recentBadges?.takeIf { it.isNotEmpty() }?.let { badges ->
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                badges.forEach { url ->
                    AsyncImage(url, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(u.dp(44)).clip(RoundedCornerShape(u.dp(10))).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
        if (app.lastUsedAt > 0L) {
            Text("${if (game) "Played" else "Used"} ${relativeTime(System.currentTimeMillis(), app.lastUsedAt).lowercase()}",
                color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(13))
        }
        Row(Modifier.padding(top = u.dp(4)), horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
            PanelButton(GamepadAction.SELECT, actionLabel(app), u, com.echo.core.ui.design.LAUNCH_HOLD_MS, holding, onClick = onLaunch)
            PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", u, onClick = onOptions)
        }
    }
}

// owner, 2026-10-09: the Titan 2's details strip: the app's tile, then its name, kind and when it was last used, and
// its buttons on the right, larger, level with the name
@Composable
internal fun WallInfoStrip(
    app: InstalledApp,
    icon: AppIconArt?,
    u: DesignUnits,
    onLaunch: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    holding: Boolean = false,
    details: com.echo.feature.appbar.GameDetails? = null,
) {
    val game = app.isGame || app.gameId != null
    val tint = icon?.color ?: NeutralTint
    val bu = DesignUnits(u.scale * TITAN_STRIP_BUTTON_GROW, LocalDensity.current, u.square)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(u.dp(24)), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(u.dp(110)).shadow(u.dp(14), RoundedCornerShape(u.dp(26))).clip(RoundedCornerShape(u.dp(26))).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            when {
                app.art != null -> AsyncImage(app.art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                icon != null -> Image(icon.bitmap, null, Modifier.size(u.dp(66)))
                else -> Text(initialOf(app.label).toString(), color = Color.White, fontSize = u.sp(44), fontWeight = FontWeight.ExtraLight)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
            Text(app.label, color = Color.White, fontSize = u.sp(30), lineHeight = u.sp(33), fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val facts = listOfNotNull(
                caseLabel(app),
                app.media?.let { m -> com.echo.feature.appbar.mediaByline(m) },
                app.playTimeMillis.takeIf { it > 0L }?.let(::playTimeLabel),
                details?.achievements?.let { "Achievements $it" },
            ).joinToString(" · ")
            Text(facts.uppercase(), style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(12), letterSpacing = 0.18.em),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (app.lastUsedAt > 0L) {
                Text("${if (game) "Played" else "Used"} ${relativeTime(System.currentTimeMillis(), app.lastUsedAt).lowercase()}",
                    color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(13))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(bu.dp(10))) {
            PanelButton(GamepadAction.SELECT, actionLabel(app), bu, com.echo.core.ui.design.LAUNCH_HOLD_MS, holding, onClick = onLaunch)
            PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", bu, onClick = onOptions)
        }
    }
}

private const val TITAN_STRIP_BUTTON_GROW = 1.35f

internal fun actionLabel(app: InstalledApp): String = when {
    app.media?.kind == com.echo.feature.appbar.MediaKind.BOOK -> "Read"
    app.media != null || app.isGame || app.gameId != null -> "Play"
    else -> "Open"
}

private val NeutralTint = Color(0xFF222838)


