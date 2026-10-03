package com.psplauncher.feature.xmb.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.psplauncher.core.common.format.relativeTime
import com.psplauncher.core.domain.achievement.Achievement
import com.psplauncher.core.domain.achievement.AchievementProvider
import com.psplauncher.core.domain.achievement.AchievementSet
import com.psplauncher.core.domain.discord.DiscordFriend
import com.psplauncher.core.domain.discord.DiscordPresence
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPrompt
import com.psplauncher.core.ui.components.LocalPadPrompts
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.psplauncher.core.ui.design.PanelBase
import com.psplauncher.core.ui.design.PanelButton
import com.psplauncher.core.ui.design.PanelCardFill
import com.psplauncher.core.ui.design.PanelCardFocusFill
import com.psplauncher.core.ui.design.PanelFocusRing
import com.psplauncher.core.ui.design.panelBackdrop
import com.psplauncher.core.ui.image.rememberArtworkModel
import com.psplauncher.feature.xmb.viewmodel.BADGE_COLUMNS
import com.psplauncher.feature.xmb.viewmodel.BadgeFilter
import com.psplauncher.feature.xmb.viewmodel.ProfileState
import com.psplauncher.feature.xmb.viewmodel.ProfileTab
import com.psplauncher.feature.xmb.viewmodel.RarityTier
import com.psplauncher.feature.xmb.viewmodel.groupFriends
import com.psplauncher.feature.xmb.viewmodel.rarityTier
import com.psplauncher.feature.xmb.viewmodel.setKey
import com.psplauncher.feature.xmb.viewmodel.showcase

@Composable
fun ProfileScreen(
    profile: ProfileState,
    name: String,
    avatar: String?,
    onAction: (GamepadAction) -> Unit,
    onTab: (ProfileTab) -> Unit,
    onSet: (Int) -> Unit,
    onBadge: (Int) -> Unit,
    onFilter: (BadgeFilter) -> Unit,
    onFriend: (Int) -> Unit,
    onEditName: () -> Unit,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = profile.data.recent.firstOrNull()?.let { it.artworkUri ?: it.iconUri }?.takeIf { it.isNotBlank() }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .panelBackdrop(ProfileTint)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val u = DesignUnits(minOf(maxWidth.value / PANEL_DESIGN_WIDTH, maxHeight.value / PANEL_DESIGN_HEIGHT), LocalDensity.current)
        if (backdrop != null) {
            AsyncImage(
                model = rememberArtworkModel(backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(28)).graphicsLayer(alpha = 0.45f),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to PanelBase.copy(alpha = 0.35f), 0.55f to PanelBase.copy(alpha = 0.85f), 1f to PanelBase)))
        }

        ProfileTabs(profile.tab, u, onTab, Modifier.align(Alignment.TopCenter).padding(top = u.dp(26)))

        when (profile.tab) {
            ProfileTab.OVERVIEW -> Overview(profile, name, avatar, u, onEditName, onPickAvatar)
            ProfileTab.ACHIEVEMENTS -> AchievementsWall(profile, u, onAction, onSet, onBadge, onFilter)
            ProfileTab.FRIENDS -> FriendsTab(profile, name, avatar, u, onAction, onFriend, onEditName, onPickAvatar)
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = u.dp(68), bottom = u.dp(14)),
            horizontalArrangement = Arrangement.spacedBy(u.dp(8)),
        ) {
            if (profile.tab == ProfileTab.ACHIEVEMENTS && profile.data.sets.isNotEmpty()) {
                Hint(GamepadAction.OPEN_CONTEXT_MENU, "Filter", u) { onAction(GamepadAction.OPEN_CONTEXT_MENU) }
            }
            Hint(GamepadAction.BACK, "Back", u) { onAction(GamepadAction.BACK) }
        }
    }
}

@Composable
private fun Hint(action: GamepadAction, label: String, u: DesignUnits, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(u.dp(20))).clickable(onClick = onClick).padding(horizontal = u.dp(12)),
        contentAlignment = Alignment.Center,
    ) {
        val style = TextStyle(fontSize = u.sp(13), fontWeight = FontWeight.Light)
        if (LocalPadPrompts.current) {
            ControllerPrompt(action, label, labelStyle = style, glyphSize = u.dp(22), spacing = u.dp(8))
        } else {
            Text(label, color = Color.White.copy(alpha = 0.75f), style = style)
        }
    }
}

@Composable
private fun ProfileTabs(tab: ProfileTab, u: DesignUnits, onTab: (ProfileTab) -> Unit, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
        val pad = LocalPadPrompts.current
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = u.dp(24), spacing = 0.dp)
        ProfileTab.entries.forEach { t ->
            val on = t == tab
            Column(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(u.dp(8))).clickable { onTab(t) }.padding(horizontal = u.dp(10), vertical = u.dp(4)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.dp(4)),
            ) {
                Text(t.label, color = Color.White.copy(alpha = if (on) 1f else 0.5f), fontSize = u.sp(17),
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Light, maxLines = 1)
                Box(Modifier.width(if (on) u.dp(22) else 0.dp).height(u.dp(2)).clip(RoundedCornerShape(1.dp)).background(Color.White))
            }
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(24), spacing = 0.dp)
    }
}

@Composable
private fun Header(profile: ProfileState, name: String, avatar: String?, u: DesignUnits, onEditName: () -> Unit, onPickAvatar: () -> Unit) {
    val data = profile.data
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(24))) {
        Box(
            Modifier.size(u.dp(110)).border(u.dp(2), Color.White, RoundedCornerShape(u.dp(4))).padding(u.dp(2))
                .background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onPickAvatar),
            contentAlignment = Alignment.Center,
        ) {
            if (avatar != null) {
                AsyncImage(avatar, name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Outlined.Image, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(u.dp(30)))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
            Box(Modifier.clickable(onClick = onEditName)) { Headline(name, u.sp(52), 1) }
            data.discordUser?.let { user ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                    Box(Modifier.size(u.dp(8)).clip(CircleShape).background(presenceColor(DiscordPresence.ONLINE)))
                    Text("Online on Discord as ${user.label}", color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(13),
                        fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val chips = listOfNotNull(
                "Steam".takeIf { data.steamLinked },
                "RetroAchievements".takeIf { data.raLinked },
                "Discord".takeIf { data.discordSignedIn },
            )
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                    chips.forEach {
                        Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = u.sp(12), maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(u.dp(12))).background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = u.dp(10), vertical = u.dp(4)))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stats(profile: ProfileState, u: DesignUnits) {
    val data = profile.data
    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(56))) {
        BigStat(data.games.toString(), "Games", u)
        BigStat((data.totals?.unlocked ?: 0).toString(), "Achievements", u)
        BigStat((data.playTimeMs / 3_600_000L).toString(), "Hours played", u)
        data.totals?.raPoints?.takeIf { it > 0 }?.let { BigStat(it.toString(), "RA points", u) }
    }
}

@Composable
private fun BigStat(value: String, label: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
        Text(value, color = Color.White, fontSize = u.sp(32), fontWeight = FontWeight.ExtraLight, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
    }
}

@Composable
private fun EditButtons(u: DesignUnits, onEditName: () -> Unit, onPickAvatar: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
        PanelButton(GamepadAction.CHANGE_SORT, "Edit name", u, onEditName)
        PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Picture", u, onPickAvatar)
    }
}

@Composable
private fun Overview(profile: ProfileState, name: String, avatar: String?, u: DesignUnits, onEditName: () -> Unit, onPickAvatar: () -> Unit) {
    val data = profile.data
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().padding(start = u.dp(80), end = u.dp(80), top = u.dp(110), bottom = u.dp(70))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Header(profile, name, avatar, u, onEditName, onPickAvatar) }
            EditButtons(u, onEditName, onPickAvatar)
        }
        Spacer(Modifier.height(u.dp(36)))
        Stats(profile, u)
        Box(Modifier.padding(vertical = u.dp(22)).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(u.dp(40))) {
            Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
                SectionLabel("Recently played", u)
                if (data.recent.isEmpty()) Meta("Nothing played yet", u.sp(14))
                data.recent.forEach { game ->
                    val set = data.sets.firstOrNull { it.gameId == game.id }
                    val detail = listOfNotNull(
                        set?.let { "${it.unlocked} of ${it.total}" },
                        game.lastPlayedAt?.let { relativeTime(now, it) },
                    ).joinToString(" · ")
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(u.dp(12))).background(PanelCardFill).padding(u.dp(8)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
                    ) {
                        Art(listOfNotNull(game.iconUri, game.artworkUri).firstOrNull { it.isNotBlank() }, u.dp(44), u.dp(44), u.dp(8), Icons.Outlined.Image, u)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(3))) {
                            Text(game.displayTitle, color = Color.White, fontSize = u.sp(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(detail, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(11), fontWeight = FontWeight.Light, maxLines = 1)
                            set?.takeIf { it.total > 0 }?.let { Bar(it.unlocked.toFloat() / it.total, u) }
                        }
                    }
                }
            }
            val shown = showcase(data.sets, data.badges)
            if (shown.isNotEmpty()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
                    SectionLabel("Showcase", u)
                    shown.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
                            pair.forEach { b ->
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(u.dp(12))).background(PanelCardFill).padding(u.dp(12)),
                                    verticalArrangement = Arrangement.spacedBy(u.dp(6)),
                                ) {
                                    BadgeIcon(b.achievement, u.dp(30), u)
                                    Text(b.achievement.name, color = Color.White, fontSize = u.sp(13), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${b.game} · ${percent(b.achievement.globalPercent)}", color = Color.White.copy(alpha = 0.55f),
                                        fontSize = u.sp(11), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (data.discordSignedIn) {
                Column(Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
                    SectionLabel("Friends · ${data.friends.count { it.presence.isOnline }} online", u)
                    groupFriends(data.friends).flatMap { it.second }.take(4).forEach { FriendRow(it, false, u) {} }
                }
            }
        }
    }
}

@Composable
private fun AchievementsWall(
    profile: ProfileState,
    u: DesignUnits,
    onAction: (GamepadAction) -> Unit,
    onSet: (Int) -> Unit,
    onBadge: (Int) -> Unit,
    onFilter: (BadgeFilter) -> Unit,
) {
    val data = profile.data
    val set = profile.focusedSet
    if (set == null) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = u.dp(160)),
            verticalArrangement = Arrangement.spacedBy(u.dp(16), Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.EmojiEvents, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(u.dp(48)))
            Headline("No achievements yet", u.sp(36), 1)
            Meta("Add RetroAchievements or Steam in Settings, System, Accounts.", u.sp(15), maxLines = 2)
            PanelButton(GamepadAction.SELECT, "Open Accounts", u) { onAction(GamepadAction.SELECT) }
        }
        return
    }
    val all = data.badges[setKey(set)].orEmpty()
    val badges = profile.visibleBadges
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().padding(start = u.dp(80), end = u.dp(80), top = u.dp(84), bottom = u.dp(64))) {
        CoverStrip(data.sets, profile.set, !profile.inGrid, u, onSet)
        Row(Modifier.fillMaxWidth().padding(top = u.dp(18)), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                val eyebrow = listOfNotNull(
                    providerLabel(set.provider),
                    set.gameId?.let { data.platforms[it] },
                    set.lastPlayedAt?.let { "Last played ${relativeTime(now, it)}" },
                ).joinToString(" · ")
                Text(eyebrow.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12), letterSpacing = 0.16.em), maxLines = 1)
                Headline(set.title, u.sp(44), 1)
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(18))) {
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
            }
            Ring(set, u)
        }
        Row(Modifier.padding(top = u.dp(14), bottom = u.dp(10)), horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
            BadgeFilter.entries.forEach { f ->
                val n = when (f) {
                    BadgeFilter.ALL -> all.size
                    BadgeFilter.UNLOCKED -> all.count { it.isUnlocked }
                    BadgeFilter.LOCKED -> all.count { !it.isUnlocked }
                }
                val on = f == profile.filter
                Column(
                    Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(u.dp(8))).clickable { onFilter(f) }.padding(horizontal = u.dp(10), vertical = u.dp(4)),
                    verticalArrangement = Arrangement.spacedBy(u.dp(4)),
                ) {
                    Text("${f.label} $n", color = Color.White.copy(alpha = if (on) 1f else 0.5f), fontSize = u.sp(14),
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Light, maxLines = 1)
                    Box(Modifier.width(if (on) u.dp(18) else 0.dp).height(u.dp(2)).background(Color.White))
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(u.dp(36))) {
            val grid = rememberLazyGridState()
            val focus = profile.badge.coerceIn(0, (badges.size - 1).coerceAtLeast(0))
            LaunchedEffect(focus, profile.inGrid) { if (profile.inGrid && badges.isNotEmpty()) grid.animateScrollToItem(focus) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(BADGE_COLUMNS),
                state = grid,
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
                verticalArrangement = Arrangement.spacedBy(u.dp(12)),
                contentPadding = PaddingValues(u.dp(4)),
            ) {
                items(badges.size) { i -> Badge(badges[i], profile.inGrid && i == focus, u) { onBadge(i) } }
            }
            Box(Modifier.weight(1f)) {
                badges.getOrNull(focus)?.let { DetailCard(it, u) }
                    ?: Meta(if (all.isEmpty()) "This set has not synced its achievements yet." else "Nothing here with this filter.", u.sp(14), maxLines = 2)
            }
        }
    }
}

@Composable
private fun CoverStrip(sets: List<AchievementSet>, at: Int, focused: Boolean, u: DesignUnits, onSet: (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(at) { state.animateScrollToItem((at - 2).coerceAtLeast(0)) }
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(u.dp(10)), contentPadding = PaddingValues(vertical = u.dp(4), horizontal = u.dp(4))) {
        items(sets.size) { i ->
            val on = i == at
            val shape = RoundedCornerShape(u.dp(12))
            Box(
                Modifier.size(u.dp(64)).graphicsLayer(alpha = if (on) 1f else 0.55f).clip(shape).background(PanelCardFill)
                    .then(if (on) Modifier.border(u.dp(if (focused) 2.5f else 1.5f), Color.White.copy(alpha = if (focused) 1f else 0.6f), shape) else Modifier)
                    .clickable { onSet(i) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(u.dp(24)))
                sets[i].iconUrl?.let { AsyncImage(it, sets[i].title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Text(if (sets[i].provider == AchievementProvider.STEAM) "Steam" else "RA", color = Color.White, fontSize = u.sp(9),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = u.dp(3)).clip(RoundedCornerShape(u.dp(4)))
                        .background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = u.dp(4)))
            }
        }
    }
}

@Composable
private fun Ring(set: AchievementSet, u: DesignUnits) {
    val fraction = if (set.total > 0) set.unlocked.toFloat() / set.total else 0f
    Box(Modifier.size(u.dp(120)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.06f
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawCircle(Color.Black.copy(alpha = 0.35f))
            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(Color.White, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(fraction * 100).toInt()}%", color = Color.White, fontSize = u.sp(26), fontWeight = FontWeight.ExtraLight, maxLines = 1)
            Text("${set.unlocked} of ${set.total}", color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(11), maxLines = 1)
        }
    }
}

@Composable
private fun Badge(a: Achievement, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u.dp(16))
    val tier = tierColor(rarityTier(a.globalPercent))
    Box(
        Modifier.size(u.dp(78)).clip(shape).background(if (focused) PanelCardFocusFill else PanelCardFill)
            .border(
                if (focused) u.dp(PANEL_FOCUS_RING_WIDTH + 0.5f) else u.dp(1.5f),
                if (focused) PanelFocusRing else if (a.isUnlocked) tier.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BadgeIcon(a, u.dp(48), u)
    }
}

@Composable
private fun BadgeIcon(a: Achievement, size: Dp, u: DesignUnits) {
    Box(Modifier.size(size).clip(RoundedCornerShape(u.dp(8))), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = if (a.isUnlocked) 1f else 0.3f), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.EmojiEvents, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxSize(0.6f))
            a.iconUrl?.let { AsyncImage(it, a.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        if (!a.isUnlocked) Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.fillMaxSize(0.45f))
    }
}

@Composable
private fun DetailCard(a: Achievement, u: DesignUnits) {
    val tier = rarityTier(a.globalPercent)
    val secret = a.isHidden && !a.isUnlocked
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(u.dp(18))).background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(u.dp(18))).padding(u.dp(20)),
        verticalArrangement = Arrangement.spacedBy(u.dp(16)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(16))) {
            Box(Modifier.size(u.dp(72)).border(u.dp(2), Color.White.copy(alpha = 0.5f), RoundedCornerShape(u.dp(16))), contentAlignment = Alignment.Center) {
                BadgeIcon(a, u.dp(56), u)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
                Text(if (secret) "Hidden" else a.name, color = Color.White, fontSize = u.sp(20), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (secret) "Hidden until unlocked" else a.description, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12),
                    fontWeight = FontWeight.Light, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(24))) {
            DetailStat("Tier", tier.label, u)
            a.globalPercent?.let { DetailStat("Players", percent(it), u) }
            DetailStat("Status", a.unlockedAt?.takeIf { a.isUnlocked }?.let { "Unlocked ${unlockDate(it)}" } ?: if (a.isUnlocked) "Unlocked" else "Locked", u)
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
        Text(label.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(10), letterSpacing = 0.14.em), maxLines = 1)
        Text(value, color = Color.White, fontSize = u.sp(14), maxLines = 1)
    }
}

@Composable
private fun FriendsTab(
    profile: ProfileState,
    name: String,
    avatar: String?,
    u: DesignUnits,
    onAction: (GamepadAction) -> Unit,
    onFriend: (Int) -> Unit,
    onEditName: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val data = profile.data
    Column(Modifier.fillMaxHeight().padding(start = u.dp(80), top = u.dp(130)).width(u.dp(620)), verticalArrangement = Arrangement.spacedBy(u.dp(36))) {
        Header(profile, name, avatar, u, onEditName, onPickAvatar)
        Stats(profile, u)
    }
    Column(
        Modifier.fillMaxSize().padding(start = u.dp(800), end = u.dp(40), top = u.dp(84), bottom = u.dp(24))
            .clip(RoundedCornerShape(u.dp(24))).background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(u.dp(24))).padding(u.dp(18)),
        verticalArrangement = Arrangement.spacedBy(u.dp(12)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
            Text("Friends", color = Color.White, fontSize = u.sp(20), fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            if (data.discordSignedIn) {
                Text("Discord ${data.friends.size}", color = Color(0xFF0A0A0A), fontSize = u.sp(12), maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(u.dp(14))).background(Color.White).padding(horizontal = u.dp(12), vertical = u.dp(5)))
            }
        }
        if (!data.discordSignedIn) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(u.dp(14), Alignment.CenterVertically)) {
                Meta("Sign in to Discord to see your friends and what they are playing.", u.sp(14), maxLines = 3)
                PanelButton(GamepadAction.SELECT, "Sign in to Discord", u) { onAction(GamepadAction.SELECT) }
            }
            return@Column
        }
        if (data.friends.isEmpty()) {
            Meta("No friends to show yet.", u.sp(14))
            return@Column
        }
        val state = rememberLazyListState()
        val groups = groupFriends(data.friends)
        LaunchedEffect(profile.friend) {
            var seen = 0
            val item = groups.withIndex().firstNotNullOfOrNull { (g, pair) ->
                if (profile.friend < seen + pair.second.size) profile.friend + g + 1 else { seen += pair.second.size; null }
            } ?: 0
            state.animateScrollToItem((item - 2).coerceAtLeast(0))
        }
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
            var offset = 0
            groups.forEach { (group, friends) ->
                val start = offset
                item(key = group.name) {
                    Text("${group.label} · ${friends.size}".uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(11),
                        letterSpacing = 0.16.em), modifier = Modifier.padding(top = u.dp(10), bottom = u.dp(4)))
                }
                items(friends.size, key = { "f${friends[it].id}" }) { i ->
                    FriendRow(friends[i], start + i == profile.friend, u) { onFriend(start + i) }
                }
                offset += friends.size
            }
        }
    }
}

@Composable
private fun FriendRow(friend: DiscordFriend, focused: Boolean, u: DesignUnits, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u.dp(12))
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (focused) PanelCardFocusFill else Color.Transparent)
            .then(if (focused) Modifier.border(u.dp(PANEL_FOCUS_RING_WIDTH), PanelFocusRing, shape) else Modifier)
            .clickable(onClick = onClick).padding(u.dp(6)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
    ) {
        Box(Modifier.size(u.dp(40))) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text(friend.label.take(1).uppercase(), color = Color.White, fontSize = u.sp(15))
                if (friend.avatarUrl.isNotBlank()) AsyncImage(friend.avatarUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.align(Alignment.BottomEnd).size(u.dp(12)).clip(CircleShape).background(PanelBase).padding(u.dp(2))
                .clip(CircleShape).background(presenceColor(friend.presence)))
        }
        Column(Modifier.weight(1f)) {
            Text(friend.label, color = Color.White, fontSize = u.sp(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(friend.activity ?: presenceLabel(friend.presence), color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(11),
                fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionLabel(text: String, u: DesignUnits) {
    Text(text.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(12), letterSpacing = 0.16.em),
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun Bar(fraction: Float, u: DesignUnits) {
    Box(Modifier.fillMaxWidth().height(u.dp(3)).clip(RoundedCornerShape(u.dp(2))).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
    }
}

private fun providerLabel(p: AchievementProvider): String = when (p) {
    AchievementProvider.STEAM -> "Steam"
    AchievementProvider.RETRO_ACHIEVEMENTS -> "RetroAchievements"
}

private fun percent(p: Double?): String = p?.let { "%.1f%%".format(it) } ?: ""

private fun unlockDate(at: Long): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(at))

private fun tierColor(tier: RarityTier): Color = when (tier) {
    RarityTier.LEGENDARY -> Color(0xFFE8A93A)
    RarityTier.EPIC -> Color(0xFFA77BE8)
    RarityTier.RARE -> Color(0xFF4C8DF0)
    RarityTier.COMMON -> Color(0xFF9AA3B5)
    RarityTier.UNKNOWN -> Color.White.copy(alpha = 0.4f)
}

private fun presenceColor(p: DiscordPresence): Color = when (p) {
    DiscordPresence.ONLINE -> Color(0xFF3BA55D)
    DiscordPresence.IDLE -> Color(0xFFFAA61A)
    DiscordPresence.DND -> Color(0xFFED4245)
    DiscordPresence.STREAMING -> Color(0xFF8F5BD8)
    DiscordPresence.OFFLINE, DiscordPresence.UNKNOWN -> Color(0xFF747F8D)
}

private fun presenceLabel(p: DiscordPresence): String = when (p) {
    DiscordPresence.ONLINE -> "Online"
    DiscordPresence.IDLE -> "Idle"
    DiscordPresence.DND -> "Do not disturb"
    DiscordPresence.STREAMING -> "Streaming"
    DiscordPresence.OFFLINE, DiscordPresence.UNKNOWN -> "Offline"
}

private val ProfileTint = Color(0xFF2C5FD8)
