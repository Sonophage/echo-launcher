package com.psplauncher.feature.xmb.ui

import com.psplauncher.core.common.format.playTimeLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Velocity
import com.psplauncher.feature.xmb.viewmodel.PANEL_SETTINGS
import com.psplauncher.feature.xmb.viewmodel.SETTINGS_GRID_COLUMNS
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.PANEL_CARD_RADIUS
import com.psplauncher.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.psplauncher.core.ui.design.PANEL_UNFOCUSED_ALPHA
import com.psplauncher.core.ui.design.PanelButton
import com.psplauncher.core.ui.design.PanelCardFill
import com.psplauncher.core.ui.design.PanelCardFocusFill
import com.psplauncher.core.ui.design.PanelFocusRing
import com.psplauncher.core.ui.design.panelBackdrop
import com.psplauncher.core.ui.design.panelSectionTint
import com.psplauncher.feature.settings.ui.icon
import com.psplauncher.core.ui.icons.rememberAppIcon
import com.psplauncher.feature.xmb.viewmodel.LibraryChip
import com.psplauncher.feature.xmb.viewmodel.NoticeFocus
import com.psplauncher.feature.xmb.viewmodel.PANEL_QUICK_SETTINGS
import com.psplauncher.feature.xmb.viewmodel.PanelEntry
import com.psplauncher.feature.xmb.viewmodel.PanelStage
import com.psplauncher.feature.xmb.viewmodel.PanelTab
import com.psplauncher.feature.xmb.viewmodel.ProfileData
import com.psplauncher.feature.xmb.viewmodel.ProfileFocus
import com.psplauncher.feature.xmb.viewmodel.ProfileSpot
import com.psplauncher.feature.xmb.viewmodel.QuickSetting
import com.psplauncher.feature.xmb.viewmodel.StageAction
import com.psplauncher.feature.xmb.viewmodel.StageCommand
import com.psplauncher.feature.xmb.viewmodel.formatDuration
import com.psplauncher.core.common.format.relativeTime
import com.psplauncher.core.ui.design.panelDesignUnits

data class QuickSettingsState(
    val waveOn: Boolean,
    val backdropOn: Boolean,
    val recentAppsOn: Boolean,
    val chips: List<LibraryChip>,
)

@Composable
fun PanelTabsRow(tab: PanelTab, onTabTapped: (PanelTab) -> Unit, u: DesignUnits, tight: Boolean, modifier: Modifier = Modifier) {
    StripSections(
        labels = PanelTab.entries.map { it.label },
        selected = tab.ordinal,
        onTapped = { onTabTapped(PanelTab.entries[it]) },
        u = u,
        shoulders = true,
        modifier = modifier,
        icon = if (tight) { i, tint, m -> Icon(panelTabGlyph(PanelTab.entries[i]), null, m, tint = tint) } else null,
    )
}

private fun panelTabGlyph(tab: PanelTab): ImageVector = when (tab) {
    PanelTab.NOTIFICATIONS -> Icons.Outlined.Notifications
    PanelTab.PROFILE -> Icons.Outlined.Person
    PanelTab.QUICK -> Icons.Outlined.Tune
    PanelTab.LIBRARIES -> Icons.Outlined.VideoLibrary
    PanelTab.SETTINGS -> Icons.Outlined.Settings
}

@Composable
fun XmbNotificationBar(
    open: Boolean,
    tab: PanelTab,
    entries: List<PanelEntry>,
    stage: PanelStage,
    actions: List<StageAction>,
    recent: PanelStage?,
    focus: NoticeFocus?,
    androidAccessGranted: Boolean,
    quick: QuickSettingsState?,
    quickFocus: QuickSetting,
    chipFocus: Int,
    accent: Color,
    onRowTapped: (NoticeFocus) -> Unit,
    onActionTapped: (StageCommand) -> Unit,
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
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            when (tab) {
                PanelTab.NOTIFICATIONS -> {
                    Box(
                        Modifier.align(Alignment.CenterStart).fillMaxHeight()
                            .padding(start = u.dp(80), top = u.dp(96), bottom = u.dp(70)).width(u.dp(600)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Stage(stage, actions, stageIcon?.bitmap, u, onActionTapped)
                    }
                    NoticeList(
                        entries, recent, focus, androidAccessGranted, tint, u, onRowTapped, onGrantAndroidAccess,
                        Modifier.align(Alignment.TopEnd).fillMaxHeight()
                            .padding(end = u.dp(56), top = u.dp(118), bottom = u.dp(70)).width(u.dp(430)),
                    )
                }
                PanelTab.PROFILE -> ProfilePanel(profile, profileName, profileAvatar, profileFocus, u, onProfileTapped)
                PanelTab.QUICK -> quick?.let {
                    QuickTiles(it, quickFocus, u, onQuickTapped, Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(200)))
                }
                PanelTab.LIBRARIES -> quick?.let {
                    LibraryTiles(it.chips, chipFocus, u, onQuickTapped, Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(180)))
                }
                PanelTab.SETTINGS -> SettingsTiles(settingFocus, u, onSettingTapped,
                    Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(110)))
            }
        }
    }
}

@Composable
private fun LiveMusicProgress(stage: PanelStage.Music, u: DesignUnits) {
    val positionMs = stage.livePositionMs()
    Progress(positionMs.toFloat() / stage.durationMs, formatDuration(positionMs), formatDuration(stage.durationMs), u)
}

@Composable
private fun Stage(stage: PanelStage, actions: List<StageAction>, icon: ImageBitmap?, u: DesignUnits, onAction: (StageCommand) -> Unit) {
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(u.dp(20))) {
        when (stage) {
            is PanelStage.Music -> {
                Eyebrow(if (stage.loaded) "Now playing" else "From Recent · Music", u)
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(28)), verticalAlignment = Alignment.Bottom) {
                    Art(stage.art, u.dp(220), u.dp(220), u.dp(18), Icons.Outlined.MusicNote, u)
                    Column(Modifier.weight(1f).padding(bottom = u.dp(6)), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
                        if (stage.loaded) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                                Box(Modifier.size(u.dp(7)).clip(CircleShape).background(Color.White.copy(alpha = if (stage.playing) 1f else 0.5f)))
                                Text(if (stage.playing) "Playing" else "Paused", color = Faint, fontSize = u.sp(13), fontWeight = FontWeight.Light)
                            }
                        }
                        Headline(stage.title, u.sp(52), 3)
                        Meta(listOfNotNull(stage.artist, stage.album, stage.app).joinToString("  ·  "), u.sp(17))
                    }
                }
                if (stage.loaded && stage.durationMs > 0) LiveMusicProgress(stage, u)
            }
            is PanelStage.Video -> {
                Eyebrow("From Recent · Video", u)
                Box(Modifier.width(u.dp(440)).height(u.dp(248)).clip(RoundedCornerShape(u.dp(16)))) {
                    Art(stage.art, u.dp(440), u.dp(248), u.dp(0), Icons.Outlined.Movie, u)
                    stage.progress?.let { p ->
                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(u.dp(4)).background(Color.White.copy(alpha = 0.25f))) {
                            Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                    Headline(stage.title, u.sp(48), 2)
                    stage.detail?.let { Meta(it, u.sp(17)) }
                    stage.progressLabel?.let { Text(it, color = Faint, fontSize = u.sp(13), fontWeight = FontWeight.Light) }
                }
            }
            is PanelStage.Book -> {
                Eyebrow("From Recent · Book", u)
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(30)), verticalAlignment = Alignment.Bottom) {
                    Art(stage.cover, u.dp(170), u.dp(256), u.dp(6), Icons.AutoMirrored.Outlined.MenuBook, u)
                    Column(Modifier.weight(1f).padding(bottom = u.dp(4)), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
                        Headline(stage.title, u.sp(50), 3)
                        stage.detail?.let { Meta(it, u.sp(17)) }
                    }
                }
            }
            is PanelStage.Game -> {
                stage.art?.let { Art(it, u.dp(580), u.dp(186), u.dp(18), Icons.Outlined.Games, u) }
                Eyebrow("From Recent · Game", u)
                Headline(stage.title, u.sp(if (stage.art != null) 64 else 84), 2)
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(28))) {
                    stage.lastPlayedAt?.let { Stat("Last played", relativeTime(now, it), u) }
                    if (stage.playTimeMs > 0) Stat("Play time", playTimeLabel(stage.playTimeMs), u)
                }
            }
            is PanelStage.App -> {
                Eyebrow("From Recent", u)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(22))) {
                    AppIcon(icon, u.dp(80), u.dp(21))
                    Headline(stage.title, u.sp(80), 1)
                }
                Meta(listOfNotNull("App", stage.lastUsedAt?.let { "Last used ${relativeTime(now, it)}" }).joinToString("  ·  "), u.sp(17))
            }
            is PanelStage.Android -> NoticeStage(icon, stage.notice.appLabel, relativeTime(now, stage.notice.postedAt),
                stage.notice.title ?: stage.notice.appLabel, stage.notice.text, u)
            is PanelStage.Launcher -> NoticeStage(icon, "Launcher", relativeTime(now, stage.toast.postedAt), stage.toast.title, stage.toast.message, u)
            PanelStage.Empty -> Text("You're all caught up", color = Color.White, fontSize = u.sp(40), fontWeight = FontWeight.ExtraLight)
        }
        if (actions.isNotEmpty()) {
            Row(Modifier.padding(top = u.dp(8)), horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
                actions.forEach { PanelButton(it.button, it.label, u) { onAction(it.command) } }
            }
        }
    }
}

@Composable
private fun NoticeStage(icon: ImageBitmap?, app: String, `when`: String, title: String, text: String?, u: DesignUnits) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(16))) {
        AppIcon(icon, u.dp(64), u.dp(16))
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
            Text(app, color = Color.White, fontSize = u.sp(15))
            Text(`when`, color = Faint, fontSize = u.sp(13), fontWeight = FontWeight.Light)
        }
    }
    Text(title, color = Color.White, fontSize = u.sp(46), fontWeight = FontWeight.Light, lineHeight = u.sp(52),
        maxLines = 3, overflow = TextOverflow.Ellipsis)
    text?.takeIf { it.isNotBlank() }?.let { Meta(it, u.sp(17), maxLines = 4) }
}

@Composable
private fun NoticeList(
    entries: List<PanelEntry>,
    recent: PanelStage?,
    focus: NoticeFocus?,
    androidAccessGranted: Boolean,
    tint: Color,
    u: DesignUnits,
    onRowTapped: (NoticeFocus) -> Unit,
    onGrantAndroidAccess: () -> Unit,
    modifier: Modifier,
) {
    val state = rememberLazyListState()
    val lead = (if (recent != null) 1 else 0) + 1 + (if (androidAccessGranted) 0 else 1)
    LaunchedEffect(focus, entries.size) {
        val at = when (focus) {
            NoticeFocus.Media -> 0
            null -> return@LaunchedEffect
            else -> entries.indexOfFirst { it.focus == focus }.takeIf { it >= 0 }?.plus(lead) ?: return@LaunchedEffect
        }
        state.animateScrollToItem((at - 1).coerceAtLeast(0))
    }
    LazyColumn(
        state = state,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.04f to Color.Black, 0.88f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            },
        verticalArrangement = Arrangement.spacedBy(u.dp(6)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = u.dp(8), vertical = u.dp(12)),
    ) {
        recent?.let { r ->
            item(key = "recent") { RecentCard(r, focus == NoticeFocus.Media, tint, u) { onRowTapped(NoticeFocus.Media) } }
        }
        item(key = "header") {
            Text("NOTIFICATIONS  ·  ${entries.size}", style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(12),
                fontWeight = FontWeight.Light, letterSpacing = 0.14.em), modifier = Modifier.padding(start = u.dp(12), top = u.dp(14), bottom = u.dp(4)))
        }
        if (!androidAccessGranted) {
            item(key = "grant") {
                Text("Turn on Notification access to see other apps here", color = Faint, fontSize = u.sp(14),
                    modifier = Modifier.clip(RoundedCornerShape(u.dp(14))).clickable(onClick = onGrantAndroidAccess).padding(u.dp(12)))
            }
        }
        items(entries, key = { it.focus.toString() }) { entry ->
            NoticeRow(entry, entry.focus == focus, u) { onRowTapped(entry.focus) }
        }
    }
}

@Composable
private fun RecentCard(stage: PanelStage, focused: Boolean, tint: Color, u: DesignUnits, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val (title, detail) = when (stage) {
        is PanelStage.Music -> stage.title to listOfNotNull(if (stage.loaded) "Now playing" else "From Recent", stage.artist, stage.app).joinToString("  ·  ")
        is PanelStage.Video -> stage.title to listOfNotNull("From Recent", stage.progressLabel ?: stage.detail).joinToString("  ·  ")
        is PanelStage.Book -> stage.title to listOfNotNull("From Recent", stage.detail).joinToString("  ·  ")
        is PanelStage.Game -> stage.title to listOfNotNull("From Recent", stage.lastPlayedAt?.let { "Last played ${relativeTime(now, it)}" }).joinToString("  ·  ")
        is PanelStage.App -> stage.title to "From Recent"
        else -> return
    }
    val shape = RoundedCornerShape(u.dp(16))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.6f), tint.copy(alpha = 0.2f))))
            .border(if (focused) u.dp(2.5f) else u.dp(1), Color.White.copy(alpha = if (focused) 1f else 0.1f), shape)
            .clickable(onClick = onClick)
            .padding(start = u.dp(14), end = u.dp(16), top = u.dp(14), bottom = u.dp(14)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        val icon = rememberAppIcon((stage as? PanelStage.App)?.packageName)
        when {
            stage is PanelStage.App -> AppIcon(icon?.bitmap, u.dp(44), u.dp(12))
            else -> Art(stageArt(stage), u.dp(44), u.dp(44), u.dp(12), stageGlyph(stage), u)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = u.sp(15), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(u.dp(14)))
    }
}

@Composable
private fun NoticeRow(entry: PanelEntry, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
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
            .graphicsLayer(alpha = if (focused) 1f else PANEL_UNFOCUSED_ALPHA)
            .clip(shape)
            .background(if (focused) PanelCardFocusFill else PanelCardFill)
            .then(if (focused) Modifier.border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = u.dp(12), end = u.dp(16), top = u.dp(12), bottom = u.dp(12)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        AppIcon(icon?.bitmap, u.dp(38), u.dp(10))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = u.sp(14.5f), lineHeight = u.sp(19), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(app, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
        }
        Text(relativeTime(System.currentTimeMillis(), postedAt), color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12),
            fontWeight = FontWeight.Light, maxLines = 1)
    }
}

private data class RowText(val pkg: String, val title: String, val app: String, val postedAt: Long)

@Composable
private fun QuickTiles(quick: QuickSettingsState, focus: QuickSetting, u: DesignUnits, onTapped: (QuickSetting, Int) -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(22))) {
        PANEL_QUICK_SETTINGS.forEach { setting ->
            val (label, value) = when (setting) {
                QuickSetting.WAVE -> "Wave" to if (quick.waveOn) "On" else "Off"
                QuickSetting.BACKDROP -> "Crossbar shows" to if (quick.backdropOn) "Art" else "Wallpaper"
                QuickSetting.RECENT_APPS -> "Apps in Recent" to if (quick.recentAppsOn) "On" else "Off"
                QuickSetting.ANDROID_SETTINGS -> "Android settings" to "Open"
                QuickSetting.LIBRARIES -> "" to ""
            }
            Tile(setting == focus, u.dp(300), u.dp(22), u.dp(28), u, Modifier.weight(1f), { onTapped(setting, 0) }) {
                Icon(quickIcon(setting), null, tint = Color.White, modifier = Modifier.size(u.dp(34)))
                Column {
                    Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(17), lineHeight = u.sp(17) * 1.2f, fontWeight = FontWeight.Light)
                    Text(value, color = Color.White, fontSize = u.sp(30), lineHeight = u.sp(30) * 1.1f, fontWeight = FontWeight.ExtraLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Headline("Settings", u.sp(48), 1)
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
    Text(text.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(13), letterSpacing = 0.18.em))
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
internal fun Stat(label: String, value: String, u: DesignUnits) {
    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(14), fontWeight = FontWeight.Light)
        Text(value, color = Color.White, fontSize = u.sp(14), fontWeight = FontWeight.Light)
    }
}

@Composable
private fun Progress(fraction: Float, left: String, right: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
        Box(Modifier.fillMaxWidth().height(u.dp(4)).clip(RoundedCornerShape(u.dp(2))).background(Color.White.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(u.dp(2))).background(Color.White))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, color = Faint, fontSize = u.sp(12), fontWeight = FontWeight.Light)
            Text(right, color = Faint, fontSize = u.sp(12), fontWeight = FontWeight.Light)
        }
    }
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

private fun stageArt(stage: PanelStage): Any? = when (stage) {
    is PanelStage.Music -> stage.art
    is PanelStage.Video -> stage.art
    is PanelStage.Book -> stage.cover
    is PanelStage.Game -> stage.art
    else -> null
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
    QuickSetting.RECENT_APPS -> Icons.Outlined.History
    QuickSetting.ANDROID_SETTINGS -> Icons.Outlined.Settings
    QuickSetting.LIBRARIES -> Icons.Outlined.GridView
}

private fun libraryIcon(id: String): ImageVector = when (id) {
    com.psplauncher.core.domain.model.BuiltInCategory.GAMES -> Icons.Outlined.Games
    com.psplauncher.core.domain.model.BuiltInCategory.MUSIC -> Icons.Outlined.MusicNote
    com.psplauncher.core.domain.model.BuiltInCategory.VIDEO -> Icons.Outlined.Movie
    com.psplauncher.core.domain.model.BuiltInCategory.LIBRARY -> Icons.AutoMirrored.Outlined.MenuBook
    com.psplauncher.core.domain.model.BuiltInCategory.PHOTO -> Icons.Outlined.PhotoLibrary
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
