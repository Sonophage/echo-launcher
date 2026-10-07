package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.GlowSide
import com.echo.core.ui.design.GlowMaskedWave
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.feature.crossbar.viewmodel.NoticeChip
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.common.format.playTimeLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Velocity
import com.echo.feature.crossbar.viewmodel.PANEL_SETTINGS
import com.echo.feature.crossbar.viewmodel.SETTINGS_GRID_COLUMNS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PANEL_CARD_RADIUS
import com.echo.core.ui.design.PanelCardFill
import com.echo.core.ui.design.panelBackdrop
import com.echo.core.ui.design.panelSectionTint
import com.echo.feature.settings.ui.icon
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.feature.crossbar.viewmodel.LibraryChip
import com.echo.feature.crossbar.viewmodel.NoticeFocus
import com.echo.feature.crossbar.viewmodel.PanelEntry
import com.echo.feature.crossbar.viewmodel.PanelStage
import com.echo.feature.crossbar.viewmodel.PanelTab
import com.echo.feature.crossbar.viewmodel.ProfileData
import com.echo.feature.crossbar.viewmodel.ProfileFocus
import com.echo.feature.crossbar.viewmodel.ProfileSpot
import com.echo.feature.crossbar.viewmodel.QuickSetting
import com.echo.core.common.format.relativeTime
import com.echo.core.ui.design.panelDesignUnits

data class QuickSettingsState(
    val wave: com.echo.core.ui.wave.WaveStyle,
    val backdropOn: Boolean,
    val rowCoverArt: Boolean,
    val recentAppsOn: Boolean,
    val chips: List<LibraryChip>,
    val secondDisplay: Boolean = false,
    val secondScreenOn: Boolean = true,
)

// how far each tab's content moves down to sit under the tab row
private val PanelTabRowDrop = 48.dp

@Composable
fun CrossbarNotificationBar(
    open: Boolean,
    tab: PanelTab,
    entries: List<PanelEntry>,
    stage: PanelStage,
    focus: NoticeFocus?,
    chip: NoticeChip,
    allCount: Int,
    onChipTapped: (NoticeChip) -> Unit,
    androidAccessGranted: Boolean,
    quick: QuickSettingsState?,
    quickFocus: QuickSetting,
    chipFocus: Int,
    accent: Color,
    onRowTapped: (NoticeFocus) -> Unit,
    onFocusedTapped: () -> Unit,
    settingFocus: Int,
    onQuickTapped: (QuickSetting, Int) -> Unit,
    onSettingTapped: (Int) -> Unit,
    onGrantAndroidAccess: () -> Unit,
    profile: ProfileData,
    profileName: String,
    profileAvatar: String?,
    profileFocus: ProfileFocus,
    onProfileTapped: (ProfileSpot, Int) -> Unit,
    pull: PanelPull,
    onOpened: () -> Unit,
    onClosed: () -> Unit,
    onTabTapped: (PanelTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pull.progress.value <= 0f && !open) return
    Box(modifier.fillMaxSize().graphicsLayer { translationY = -(1f - pull.progress.value) * size.height }) {
        val stageIcon = rememberAppIcon(stagePackage(stage, LocalContext.current.packageName))
        val tabTint = when (tab) {
            PanelTab.SETTINGS -> panelSectionTint(PANEL_SETTINGS.getOrNull(settingFocus))
            PanelTab.PROFILE -> ProfileTint
            else -> stageTint(stage, stageIcon?.color, accent)
        }
        val tint by animateColorAsState(tabTint, tween(500), label = "panelTint")
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .nestedScroll(remember(pull) { pull.listOverscroll(onOpened, onClosed) })
                .panelBackdrop(tint)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .panelPullGesture(pull, onOpened, onClosed),
        ) {
            GlowMaskedWave(GlowSide.LEFT)
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            when (tab) {
                // kit 11: chips, the focused notice large on the left, the rest on the right
                PanelTab.NOTIFICATIONS -> Column(Modifier.fillMaxSize().padding(start = u.dp(80), end = u.dp(56), top = u.dp(96) + PanelTabRowDrop, bottom = u.dp(80))) {
                    NoticeChips(chip, allCount, u, onChipTapped)
                    val others = entries.filter { it.focus != focus }
                    if (u.square) {
                        Column(Modifier.fillMaxSize().padding(top = u.dp(24)), verticalArrangement = Arrangement.spacedBy(u.dp(20))) {
                            Box(Modifier.fillMaxWidth().weight(1f)) { FocusedNotice(stage, stageIcon?.bitmap, tint, u, onFocusedTapped) }
                            NoticeList(others, androidAccessGranted, u, onRowTapped, onGrantAndroidAccess, Modifier.fillMaxWidth().weight(1f))
                        }
                    } else {
                        Row(Modifier.fillMaxSize().padding(top = u.dp(24)), horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
                            Box(Modifier.weight(1.15f).fillMaxHeight()) { FocusedNotice(stage, stageIcon?.bitmap, tint, u, onFocusedTapped) }
                            NoticeList(others, androidAccessGranted, u, onRowTapped, onGrantAndroidAccess, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
                PanelTab.PROFILE -> ProfilePanel(profile, profileName, profileAvatar, profileFocus, u, onProfileTapped, top = u.dp(96) + PanelTabRowDrop)
                PanelTab.QUICK -> quick?.let {
                    QuickTiles(it, quickFocus, u, onQuickTapped, Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(200) + PanelTabRowDrop))
                }
                PanelTab.LIBRARIES -> quick?.let {
                    LibraryTiles(it.chips, chipFocus, u, onQuickTapped, Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(180) + PanelTabRowDrop))
                }
                PanelTab.SETTINGS -> SettingsTiles(settingFocus, u, onSettingTapped,
                    Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(110) + PanelTabRowDrop))
            }
            // owner, 2026-10-07: the panel's tabs are a tab row along the top, as in Settings
            com.echo.core.ui.components.EchoTabRow(
                labels = PanelTab.entries.map { it.label },
                current = tab.ordinal,
                u = u,
                onBack = onBack,
                onPick = { onTabTapped(PanelTab.entries[it]) },
                modifier = Modifier.padding(top = com.echo.core.ui.components.StatusStripHeight),
            )
        }
    }
}

@Composable
private fun NoticeChips(chip: NoticeChip, allCount: Int, u: DesignUnits, onTapped: (NoticeChip) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
        if (LocalPadPrompts.current) ControllerPrompt(GamepadAction.NAVIGATE_LEFT, "", glyphSize = u.dp(22), spacing = 0.dp)
        NoticeChip.entries.forEach { c ->
            val on = c == chip
            Text(
                if (c == NoticeChip.ALL) "${c.label} · $allCount" else c.label,
                color = if (on) Color(0xFF0A0A0A) else Color.White,
                fontSize = u.sp(15),
                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else Color.White.copy(alpha = 0.10f))
                    .clickable { onTapped(c) }
                    .padding(horizontal = u.dp(18), vertical = u.dp(8)),
            )
        }
        if (LocalPadPrompts.current) ControllerPrompt(GamepadAction.NAVIGATE_RIGHT, "", glyphSize = u.dp(22), spacing = 0.dp)
    }
}

// the focused notice shown large, as a card tinted by its app
@Composable
private fun FocusedNotice(stage: PanelStage, icon: ImageBitmap?, tint: Color, u: DesignUnits, onTapped: () -> Unit) {
    val now = System.currentTimeMillis()
    val (app, sub, title, text) = when (stage) {
        is PanelStage.Android -> NoticeCardText(stage.notice.appLabel, relativeTime(now, stage.notice.postedAt), stage.notice.title ?: stage.notice.appLabel, stage.notice.text)
        is PanelStage.Launcher -> NoticeCardText("Launcher", relativeTime(now, stage.toast.postedAt), stage.toast.title, stage.toast.message)
        else -> {
            Text("You're all caught up", color = Color.White, fontSize = u.sp(40), fontWeight = FontWeight.ExtraLight,
                modifier = Modifier.padding(top = u.dp(40)))
            return
        }
    }
    val shape = RoundedCornerShape(u.dp(24))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.25f))))
            .border(u.dp(2), Color.White, shape)
            // owner, 2026-10-05: tapping the notification opens its app
            .clickable(onClick = onTapped)
            .padding(u.dp(30)),
        verticalArrangement = Arrangement.spacedBy(u.dp(18)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
            AppIcon(icon, u.dp(52), u.dp(14))
            Column(verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
                Text(app, color = Color.White, fontSize = u.sp(17), fontWeight = FontWeight.Medium, maxLines = 1)
                Text(sub, color = Faint, fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
            }
        }
        Text(title, color = Color.White, fontSize = u.sp(32), fontWeight = FontWeight.Light, lineHeight = u.sp(38),
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        text?.takeIf { it.isNotBlank() }?.let { Meta("\u201C$it\u201D", u.sp(16), maxLines = 3) }
    }
}

private data class NoticeCardText(val app: String, val sub: String, val title: String, val text: String?)

@Composable
private fun NoticeList(
    entries: List<PanelEntry>,
    androidAccessGranted: Boolean,
    u: DesignUnits,
    onRowTapped: (NoticeFocus) -> Unit,
    onGrantAndroidAccess: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(u.dp(10)),
    ) {
        if (!androidAccessGranted) {
            item(key = "grant") {
                Text("Turn on Notification access to see other apps here", color = Faint, fontSize = u.sp(14),
                    modifier = Modifier.clip(RoundedCornerShape(u.dp(14))).clickable(onClick = onGrantAndroidAccess).padding(u.dp(12)))
            }
        }
        items(entries, key = { it.focus.toString() }) { entry ->
            NoticeRow(entry, u) { onRowTapped(entry.focus) }
        }
    }
}

@Composable
private fun NoticeRow(entry: PanelEntry, u: DesignUnits, onClick: () -> Unit) {
    val context = LocalContext.current
    val (pkg, title, app, postedAt) = when (entry) {
        is PanelEntry.Android -> RowText(entry.notice.packageName, entry.notice.title ?: entry.notice.appLabel, entry.notice.appLabel, entry.notice.postedAt)
        is PanelEntry.Launcher -> RowText(context.packageName, entry.toast.title, "Launcher", entry.toast.postedAt)
    }
    val icon = rememberAppIcon(pkg)
    val shape = RoundedCornerShape(u.dp(PANEL_CARD_RADIUS))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PanelCardFill)
            .border(u.dp(1), Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(start = u.dp(16), end = u.dp(16), top = u.dp(14), bottom = u.dp(14)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        AppIcon(icon?.bitmap, u.dp(38), u.dp(10))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            Text("$app · ${relativeTime(System.currentTimeMillis(), postedAt)}", color = Color.White.copy(alpha = 0.6f),
                fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
            Text(title, color = Color.White, fontSize = u.sp(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class RowText(val pkg: String, val title: String, val app: String, val postedAt: Long)

@Composable
private fun QuickTiles(quick: QuickSettingsState, focus: QuickSetting, u: DesignUnits, onTapped: (QuickSetting, Int) -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(22))) {
        com.echo.feature.crossbar.viewmodel.quickSettingsFor(quick.secondDisplay).forEach { setting ->
            val (label, value) = when (setting) {
                QuickSetting.WAVE -> "Wave" to quick.wave.label
                // owner, 2026-10-05: "Crossbar shows Art / Wallpaper" was unclear; this is what fills the background
                QuickSetting.BACKDROP -> "Background" to if (quick.backdropOn) "Game art" else "Your theme"
                QuickSetting.ROW_ART -> "Game rows show" to if (quick.rowCoverArt) "Cover art" else "Icons"
                QuickSetting.RECENT_APPS -> "Apps in Recent" to if (quick.recentAppsOn) "On" else "Off"
                QuickSetting.SECOND_SCREEN -> "Screens" to if (quick.secondScreenOn) "Dual" else "Single"
                QuickSetting.ANDROID_SETTINGS -> "Android settings" to "Open"
                QuickSetting.LIBRARIES -> "" to ""
            }
            Tile(setting == focus, u.dp(300), u.dp(22), u.dp(28), u, Modifier.weight(1f), { onTapped(setting, 0) }) {
                Icon(quickIcon(setting), null, tint = Color.White, modifier = Modifier.size(u.dp(34)))
                Column {
                    Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(17), lineHeight = u.sp(17) * 1.2f, fontWeight = FontWeight.Light)
                    // five tiles share the row, so a long value ("Reduced + Static", "Cover art") takes two lines
                    Text(value, color = Color.White, fontSize = u.sp(26), lineHeight = u.sp(26) * 1.1f, fontWeight = FontWeight.ExtraLight, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun LibraryTiles(chips: List<LibraryChip>, focus: Int, u: DesignUnits, onTapped: (QuickSetting, Int) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(u.dp(16))) {
        Text("${chips.count { it.visible }} of ${chips.size} on the crossbar", color = Color.White.copy(alpha = 0.6f),
            fontSize = u.sp(14), fontWeight = FontWeight.Light)
        chips.withIndex().chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(16))) {
                row.forEach { (i, chip) ->
                    Tile(i == focus, u.dp(170), u.dp(20), u.dp(22), u,
                        Modifier.weight(1f).graphicsLayer(alpha = if (chip.visible) 1f else 0.45f), { onTapped(QuickSetting.LIBRARIES, i) }) {
                        Icon(libraryIcon(chip.id), null, tint = Color.White, modifier = Modifier.size(u.dp(30)))
                        Column {
                            Text(chip.name, color = Color.White, fontSize = u.sp(22), fontWeight = FontWeight.Light)
                            Text(if (chip.visible) "On crossbar" else "Hidden from crossbar", color = Color.White.copy(alpha = 0.6f),
                                fontSize = u.sp(13), fontWeight = FontWeight.Light)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SettingsTiles(focus: Int, u: DesignUnits, onTapped: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(u.dp(30))) {
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(18))) {
            PANEL_SETTINGS.withIndex().chunked(SETTINGS_GRID_COLUMNS).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(18))) {
                    row.forEach { (i, section) ->
                        Tile(i == focus, u.dp(200), u.dp(22), u.dp(24), u, Modifier.weight(1f), { onTapped(i) }) {
                            Icon(section.icon(), null, tint = Color.White, modifier = Modifier.size(u.dp(32)))
                            Column(verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                                Text(section.title, color = Color.White, fontSize = u.sp(20), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(section.subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13), fontWeight = FontWeight.Light,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    repeat(SETTINGS_GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Tile(
    focused: Boolean,
    height: Dp,
    radius: Dp,
    pad: Dp,
    u: DesignUnits,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val grow by animateFloatAsState(if (focused) 1.04f else 1f, tween(250), label = "tile")
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .scale(grow)
            .height(height)
            .clip(shape)
            .background(Color.White.copy(alpha = if (focused) 0.14f else 0.07f))
            .then(if (focused) Modifier.border(u.dp(2.5f), Color.White, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(pad),
        verticalArrangement = Arrangement.SpaceBetween,
    ) { content() }
}

@Composable
internal fun Eyebrow(text: String, u: DesignUnits) {
    Text(text.uppercase(), style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(13), letterSpacing = 0.18.em))
}

@Composable
internal fun Headline(text: String, size: androidx.compose.ui.unit.TextUnit, lines: Int) {
    Text(text, color = Color.White, fontSize = size, lineHeight = size * 1.05f, fontWeight = FontWeight.ExtraLight,
        letterSpacing = (-0.025).em, maxLines = lines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun Meta(text: String, size: androidx.compose.ui.unit.TextUnit, maxLines: Int = 1) {
    if (text.isBlank()) return
    Text(text, color = Color.White.copy(alpha = 0.72f), fontSize = size, fontWeight = FontWeight.Light, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}


@Composable
internal fun Art(uri: Any?, width: Dp, height: Dp, radius: Dp, fallback: ImageVector, u: DesignUnits) {
    Box(
        Modifier.width(width).height(height).clip(RoundedCornerShape(radius)).background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(fallback, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(minOf(width, height) * 0.4f))
        if (uri != null && uri.toString().isNotBlank()) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun AppIcon(bitmap: ImageBitmap?, size: Dp, radius: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(Color.White.copy(alpha = 0.08f))) {
        bitmap?.let { Image(it, null, modifier = Modifier.fillMaxSize()) }
    }
}

internal fun stagePackage(stage: PanelStage, launcher: String): String? = when (stage) {
    is PanelStage.Android -> stage.notice.packageName
    is PanelStage.Launcher -> launcher
    is PanelStage.App -> stage.packageName
    is PanelStage.Music -> stage.packageName
    else -> null
}

internal fun stageTint(stage: PanelStage, iconColor: Color?, accent: Color): Color = when (stage) {
    is PanelStage.Music -> if (stage.packageName != null) iconColor ?: MusicTint else MusicTint
    is PanelStage.Video -> VideoTint
    is PanelStage.Book -> BookTint
    is PanelStage.Game -> GameTint
    is PanelStage.App, is PanelStage.Android -> iconColor ?: accent
    is PanelStage.Launcher -> accent
    PanelStage.Empty -> EmptyTint
}


internal fun stageGlyph(stage: PanelStage): ImageVector = when (stage) {
    is PanelStage.Music -> Icons.Outlined.MusicNote
    is PanelStage.Video -> Icons.Outlined.Movie
    is PanelStage.Book -> Icons.AutoMirrored.Outlined.MenuBook
    is PanelStage.Game -> Icons.Outlined.Games
    else -> Icons.Outlined.Apps
}

private fun quickIcon(setting: QuickSetting): ImageVector = when (setting) {
    QuickSetting.WAVE -> Icons.Outlined.Waves
    QuickSetting.BACKDROP -> Icons.Outlined.Image
    QuickSetting.ROW_ART -> Icons.Outlined.Games
    QuickSetting.RECENT_APPS -> Icons.Outlined.History
    QuickSetting.SECOND_SCREEN -> Icons.Outlined.Devices
    QuickSetting.ANDROID_SETTINGS -> Icons.Outlined.Settings
    QuickSetting.LIBRARIES -> Icons.Outlined.GridView
}

private fun libraryIcon(id: String): ImageVector = when (id) {
    com.echo.core.domain.model.BuiltInCategory.GAMES -> Icons.Outlined.Games
    com.echo.core.domain.model.BuiltInCategory.MUSIC -> Icons.Outlined.MusicNote
    com.echo.core.domain.model.BuiltInCategory.VIDEO -> Icons.Outlined.Movie
    com.echo.core.domain.model.BuiltInCategory.LIBRARY -> Icons.AutoMirrored.Outlined.MenuBook
    com.echo.core.domain.model.BuiltInCategory.PHOTO -> Icons.Outlined.PhotoLibrary
    else -> Icons.Outlined.Language
}

@Stable
class PanelPull internal constructor(private val scope: CoroutineScope) {
    val progress = Animatable(0f)
    var heightPx = 1f
    var isOpen = false
    private var dragging = false
    private var raw = 0f

    fun drag(dy: Float) {
        if (!dragging) { dragging = true; raw = progress.value }
        raw = (raw + dy / heightPx).coerceIn(0f, 1f)
        scope.launch { progress.snapTo(raw) }
    }

    fun release(velocityY: Float, onOpened: () -> Unit, onClosed: () -> Unit) {
        if (!dragging) return
        dragging = false
        val wantOpen = pullSettlesOpen(isOpen, raw, velocityY)
        if (wantOpen != isOpen) { if (wantOpen) onOpened() else onClosed() }
        scope.launch { progress.animateTo(if (wantOpen) 1f else 0f, tween(PULL_SETTLE_MS)) }
    }

    internal fun settle(open: Boolean) {
        isOpen = open
        if (!dragging) scope.launch { progress.animateTo(if (open) 1f else 0f, tween(PULL_SETTLE_MS)) }
    }

    internal fun listOverscroll(onOpened: () -> Unit, onClosed: () -> Unit) = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (dragging && available.y > 0f && raw < 1f) { drag(available.y); return Offset(0f, available.y) }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y < 0f) { drag(available.y); return Offset(0f, available.y) }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            release(available.y, onOpened, onClosed)
            return available
        }
    }
}

@Composable
fun rememberPanelPull(open: Boolean): PanelPull {
    val scope = rememberCoroutineScope()
    val pull = remember { PanelPull(scope) }
    pull.heightPx = LocalWindowInfo.current.containerSize.height.toFloat().coerceAtLeast(1f)
    LaunchedEffect(open) { pull.settle(open) }
    return pull
}

fun Modifier.panelPullGesture(pull: PanelPull, onOpened: () -> Unit, onClosed: () -> Unit): Modifier = pointerInput(pull) {
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = { tracker.resetTracking() },
        onVerticalDrag = { change, dy ->
            tracker.addPosition(change.uptimeMillis, change.position)
            pull.drag(dy)
        },
        onDragEnd = { pull.release(tracker.calculateVelocity().y, onOpened, onClosed) },
        onDragCancel = { pull.release(0f, onOpened, onClosed) },
    )
}

internal fun pullSettlesOpen(wasOpen: Boolean, progress: Float, velocityY: Float): Boolean = when {
    velocityY > PULL_FLING_PX_PER_S -> true
    velocityY < -PULL_FLING_PX_PER_S -> false
    wasOpen -> progress > 1f - PULL_COMMIT
    else -> progress > PULL_COMMIT
}

private const val PULL_COMMIT = 0.33f
private const val PULL_FLING_PX_PER_S = 1200f
private const val PULL_SETTLE_MS = 220


private val Faint = Color.White.copy(alpha = 0.65f)

private val MusicTint = Color(0xFFC0632A)
private val VideoTint = Color(0xFFB5303C)
private val BookTint = Color(0xFF8D6A3A)
private val GameTint = Color(0xFF2C7A55)
private val EmptyTint = Color(0xFF222222)
