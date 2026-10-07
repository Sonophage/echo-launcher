package com.echo.feature.crossbar.ui

import com.echo.core.ui.components.StatusStripHeight
import androidx.compose.ui.zIndex
import com.echo.feature.crossbar.viewmodel.gameInfoSectionLabel
import com.echo.feature.crossbar.viewmodel.BadgeFilter
import com.echo.feature.crossbar.viewmodel.badgesInView
import com.echo.feature.crossbar.viewmodel.filterBadges
import com.echo.feature.crossbar.viewmodel.RarityTier
import com.echo.feature.crossbar.viewmodel.rarityTier
import com.echo.core.ui.design.PanelButton
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.echo.feature.crossbar.viewmodel.gameInfoSections
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.EchoHintBar
import com.echo.feature.crossbar.viewmodel.holdMsFor
import com.echo.core.ui.theme.EchoTextStyle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import coil3.compose.AsyncImage
import com.echo.core.common.format.relativeTime
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.PanelCardFill
import com.echo.core.ui.design.PanelCardFocusFill
import com.echo.core.ui.design.PanelFocusRing
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.notification.AndroidNotice
import com.echo.feature.crossbar.ui.detail.DetailPanelContent
import com.echo.feature.crossbar.viewmodel.GameInfoAction
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.GameInfoStat
import com.echo.feature.crossbar.viewmodel.gameInfoStats
import com.echo.feature.crossbar.viewmodel.notices
import com.echo.core.ui.design.PANEL_DESIGN_HEIGHT
import com.echo.core.ui.design.panelDesignUnits


@Composable
fun GameInfoScreen(
    info: GameInfoState,
    androidNotices: List<AndroidNotice>,
    onAction: (GamepadAction) -> Unit,
    onCardFocused: (Int) -> Unit,
    onNoticeTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit = {},
    onScrollMax: (Int) -> Unit = {},

    launchHold: String? = null,
    onSectionPicked: (GameInfoAction?) -> Unit = {},
    onAchievementFilter: (BadgeFilter) -> Unit = {},
    // on the bottom screen, which is touch only and has its own bar: no controller footer or shoulders
    companion: Boolean = false,
) {
    val item = info.item
    val now = System.currentTimeMillis()
    val notices = info.notices(androidNotices)
    val stats = gameInfoStats(info, notices.size, now)
    val appIcon = rememberAppIcon(item.packageName.takeIf { info.isApp })

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(PanelBase)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        val drop = if (u.square) (maxHeight - u.dp(PANEL_DESIGN_HEIGHT)) / 2 else 0.dp
        Box(Modifier.fillMaxWidth().height(u.dp(430) + drop)) {
            val art = item.backdropArt.firstOrNull()
            when {
                info.isApp -> Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf((appIcon?.color ?: Color.White).copy(alpha = 0.45f), PanelBase)),
                    ),
                )
                art != null -> AsyncImage(
                    model = rememberArtworkModel(art),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.3f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            info.content?.videoUri?.takeIf { info.open != GameInfoAction.VIDEO }?.let { Icon1VideoOverlay(videoUri = it, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.58f to Color.Transparent, 1f to PanelBase)))
        }

        Row(
            Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(280) + drop).fillMaxWidth().height(u.dp(130)),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(u.dp(48)),
        ) {
            Box(Modifier.weight(1f, fill = false)) {
                if (info.isApp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(20))) {
                        AppIcon(appIcon?.bitmap, u.dp(96), u.dp(24))
                        Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
                            Headline(item.title, u.sp(40), 1)
                            Text("Android app", color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13), fontWeight = FontWeight.Light)
                        }
                    }
                } else {
                    val logo = info.content?.logoUri ?: item.logoUri?.takeIf { item.hasVisibleLogo }
                    if (logo != null) {
                        AsyncImage(
                            model = rememberArtworkModel(logo),
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomStart,
                            modifier = Modifier.width(u.dp(300)).height(u.dp(120)),
                        )
                    } else {
                        Headline(item.title, u.sp(48), 2)
                    }
                }
            }
            Row(Modifier.weight(1f).padding(bottom = u.dp(8)), horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
                stats.forEach { stat ->
                    if (stat.label == "Achievements") {
                        Box(Modifier.clip(RoundedCornerShape(u.dp(8))).clickable { onAction(GamepadAction.CHANGE_SORT) }) { BandStat(stat, u, GamepadAction.CHANGE_SORT) }
                    } else {
                        BandStat(stat, u)
                    }
                }
            }
        }

        // owner, 2026-10-06: the view LT/RT pick (achievements, the info, the manual) takes the screenshots' place
        // under the band; only the video plays over the whole screen
        Column(
            Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(440) + drop, bottom = u.dp(80)).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            val media = info.content?.media.orEmpty()
            val description = info.content?.description?.takeIf { it.isNotBlank() }
            when {
                info.open == GameInfoAction.ACHIEVEMENTS -> AchievementsView(info, u, onOpenAll = { onAction(GamepadAction.SELECT) }, onFilter = onAchievementFilter)
                info.open == GameInfoAction.INFO -> info.content?.let { InfoView(info, it, u, onScrollMax) }
                info.open == GameInfoAction.MANUAL -> ManualView(u) { onAction(GamepadAction.SELECT) }
                info.isApp && notices.isNotEmpty() -> {
                    SectionLabel("Recent from ${item.title}", u)
                    CardRow(notices.size, info.cursor, u, onCardFocused) { index, focused ->
                        NoticeCard(notices[index], focused, now, u) { onNoticeTapped(notices[index].key) }
                    }
                }
                media.isNotEmpty() -> {
                    SectionLabel("Screenshots", u)
                    CardRow(media.size, info.cursor, u, onCardFocused) { index, focused ->
                        Shot(media[index].uri, focused, u) { onCardFocused(index) }
                    }
                }
                !info.isApp && description != null -> {
                    SectionLabel("About", u)
                    Meta(description, u.sp(15), maxLines = 5)
                }
            }
        }

        // the views LT/RT walk
        val sections = gameInfoSections(info)
        val walk = !info.isApp && sections.size > 1
        // kit screens 3-4: Back and the view on the left (owner, 2026-10-06: no Home hint, and the view as LT/RT and
        // its word), the action orb in the centre, the screen's actions on the right
        if (!companion) EchoHintBar(
            filter = if (walk) {
                {
                    val at = sections.indexOf(info.open).coerceAtLeast(0)
                    com.echo.core.ui.components.TriggerFilter(gameInfoSectionLabel(sections[at])) { onSectionPicked(sections[(at + 1) % sections.size]) }
                }
            } else null,
            items = listOfNotNull(
                ControllerPromptItem(GamepadAction.BACK, "Back"),
                ControllerPromptItem(GamepadAction.CHANGE_SORT, if (info.open == GameInfoAction.ACHIEVEMENTS) "Filter" else "Achievements")
                    .takeIf { info.achievementsStat != null },
                ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
            onAction = onAction,
            primary = HintAction(
                GamepadAction.SELECT,
                when {
                    info.isApp -> "Open"
                    item.lastOpenedAt != null -> "Continue"
                    else -> "Play"
                },
                item.title,
                holdMs = holdMsFor(item),
                holding = launchHold == item.id,
            ),
        )

        // the bottom screen has no footer and is touch only: its views stay a row of tabs to tap
        if (companion && walk) {
            StripSections(
                labels = sections.map(::gameInfoSectionLabel),
                selected = sections.indexOf(info.open).coerceAtLeast(0),
                onTapped = { onSectionPicked(sections[it]) },
                u = u,
                shoulders = false,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = StatusStripHeight + u.dp(8)).zIndex(2f),
            )
        }

        when (info.open) {
            GameInfoAction.VIDEO -> info.videoUri?.let { uri ->
                Box(
                    Modifier.fillMaxSize().background(Color.Black)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClosePanel),
                ) {
                    OneShotVideoLayer(uri, C.TIME_END_OF_SOURCE, onEnded = onClosePanel, onFailed = onClosePanel, modifier = Modifier.fillMaxSize())
                }
            }
            else -> Unit
        }
    }
}


// the about text, scrolled with up and down, and the facts under it
@Composable
private fun ColumnScope.InfoView(info: GameInfoState, content: DetailPanelContent, u: DesignUnits, onScrollMax: (Int) -> Unit) {
    val scroll = rememberScrollState()
    val step = with(LocalDensity.current) { u.dp(120).roundToPx() }
    LaunchedEffect(scroll.maxValue, step) { onScrollMax(scroll.maxValue / step + if (scroll.maxValue % step > 0) 1 else 0) }
    LaunchedEffect(info.infoScroll) { scroll.animateScrollTo(info.infoScroll * step) }
    // the band above already shows the play time, the platform and when it was last played
    val facts = listOfNotNull(content.fileName?.let { GameInfoStat("File", it) })
    SectionLabel("About", u)
    content.metaLine?.let { Meta(it, u.sp(15), maxLines = 1) }
    Text(
        content.description?.takeIf { it.isNotBlank() } ?: "No description available.",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = u.sp(15),
        lineHeight = u.sp(22),
        fontWeight = FontWeight.Light,
        modifier = Modifier.weight(1f, fill = false).verticalScroll(scroll),
    )
    if (facts.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
            facts.forEach { fact ->
                Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
                    Text(fact.value, color = Color.White, fontSize = u.sp(18), fontWeight = FontWeight.ExtraLight,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(fact.label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
                }
            }
        }
    }
}

// the game's achievements: the tiers, the filters, and the badges, latest unlocked first. The d-pad hovers a
// badge, whose details stand where the ring is; X filters; A on a badge, or a tap, opens the full wall
@Composable
private fun AchievementsView(info: GameInfoState, u: DesignUnits, onOpenAll: () -> Unit, onFilter: (BadgeFilter) -> Unit) {
    val set = info.achievementSet ?: return
    val all = info.achievements
    val shown = info.badgesInView
    val hovered = info.cursor?.let { shown.getOrNull(it) }
    val row = rememberLazyListState()
    LaunchedEffect(info.cursor) { info.cursor?.takeIf { it in shown.indices }?.let { row.animateScrollToItem((it - 2).coerceAtLeast(0)) } }
    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(24))) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(12))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(18))) {
                SectionLabel("Achievements", u)
                RarityTier.entries.forEach { tier ->
                    val inTier = all.filter { rarityTier(it.globalPercent) == tier }
                    if (inTier.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
                            Box(Modifier.size(u.dp(8)).clip(CircleShape).background(tierColor(tier)))
                            Text("${tier.label} ${inTier.count { it.isUnlocked }}/${inTier.size}", color = Color.White.copy(alpha = 0.75f),
                                fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
                BadgeFilter.entries.forEach { f ->
                    val n = filterBadges(all, f).size
                    val on = f == info.achievementFilter
                    Column(
                        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(u.dp(8))).clickable { onFilter(f) }
                            .padding(horizontal = u.dp(10), vertical = u.dp(4)),
                        verticalArrangement = Arrangement.spacedBy(u.dp(4)),
                    ) {
                        Text("${f.label} $n", color = Color.White.copy(alpha = if (on) 1f else 0.5f), fontSize = u.sp(14),
                            fontWeight = if (on) FontWeight.Medium else FontWeight.Light, maxLines = 1)
                        Box(Modifier.width(if (on) u.dp(18) else 0.dp).height(u.dp(2)).background(Color.White))
                    }
                }
            }
            LazyRow(state = row, horizontalArrangement = Arrangement.spacedBy(u.dp(12)), contentPadding = PaddingValues(u.dp(4))) {
                items(shown.size) { i -> Badge(shown[i], focused = i == info.cursor, u = u, onClick = onOpenAll) }
            }
        }
        Box(Modifier.width(u.dp(440)), contentAlignment = Alignment.CenterEnd) {
            if (hovered != null) DetailCard(hovered, u) else Ring(set, u)
        }
    }
}

// the manual opens in its own viewer
@Composable
private fun ManualView(u: DesignUnits, onOpen: () -> Unit) {
    SectionLabel("Manual", u)
    Meta("The game's manual, in the viewer.", u.sp(15), maxLines = 1)
    PanelButton(GamepadAction.SELECT, "Open the manual", u, onClick = onOpen)
}

@Composable
private fun BandStat(stat: GameInfoStat, u: DesignUnits, button: GamepadAction? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
        Text(stat.value, color = Color.White, fontSize = u.sp(26), fontWeight = FontWeight.ExtraLight, maxLines = 1)
        val labelStyle = EchoTextStyle.copy(fontSize = u.sp(12), fontWeight = FontWeight.Light)
        if (button != null && LocalPadPrompts.current) {
            ControllerPrompt(button, stat.label, labelColor = Color.White.copy(alpha = 0.55f), labelStyle = labelStyle, glyphSize = u.dp(18), spacing = u.dp(6))
        } else {
            Text(stat.label, color = Color.White.copy(alpha = 0.55f), style = labelStyle, maxLines = 1)
        }
    }
}

@Composable
private fun CardRow(
    count: Int,
    cursor: Int?,
    u: DesignUnits,
    onCardFocused: (Int) -> Unit,
    card: @Composable (Int, Boolean) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(cursor) { cursor?.let { state.animateScrollToItem((it - 1).coerceAtLeast(0)) } }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
        contentPadding = PaddingValues(vertical = u.dp(4)),
    ) {
        items(count) { index ->
            Box(Modifier.width(u.dp(250)).height(u.dp(132))) { card(index, index == cursor) }
        }
    }
}

private fun Modifier.cardFrame(focused: Boolean, u: DesignUnits, fill: Color, onClick: () -> Unit): Modifier {
    val shape = RoundedCornerShape(u.dp(14))
    return fillMaxSize()
        .clip(shape)
        .background(fill)
        .then(if (focused) Modifier.border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape) else Modifier)
        .clickable(onClick = onClick)
}

@Composable
private fun Shot(uri: String, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
    Box(Modifier.cardFrame(focused, u, PanelCardFill, onClick)) {
        AsyncImage(rememberArtworkModel(uri), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun NoticeCard(notice: AndroidNotice, focused: Boolean, now: Long, u: DesignUnits, onClick: () -> Unit) {
    Column(
        Modifier.cardFrame(focused, u, if (focused) PanelCardFocusFill else PanelCardFill, onClick)
            .padding(horizontal = u.dp(18), vertical = u.dp(16)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(relativeTime(now, notice.postedAt), color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12), fontWeight = FontWeight.Light)
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
            Text(notice.title ?: notice.appLabel, color = Color.White, fontSize = u.sp(15), lineHeight = u.sp(19),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            notice.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
