package com.psplauncher.feature.xmb.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.psplauncher.core.common.format.relativeTime
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
import com.psplauncher.core.ui.icons.rememberAppIcon
import com.psplauncher.core.ui.image.rememberArtworkModel
import com.psplauncher.core.ui.notification.AndroidNotice
import com.psplauncher.feature.xmb.viewmodel.GameInfoState
import com.psplauncher.feature.xmb.viewmodel.GameInfoStat
import com.psplauncher.feature.xmb.viewmodel.gameInfoStats
import com.psplauncher.feature.xmb.viewmodel.notices

private const val DESIGN_WIDTH = 1200f
private const val DESIGN_HEIGHT = 752f

@Composable
fun GameInfoScreen(
    info: GameInfoState,
    androidNotices: List<AndroidNotice>,
    onAction: (GamepadAction) -> Unit,
    onCardFocused: (Int) -> Unit,
    onNoticeTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        val u = DesignUnits(minOf(maxWidth.value / DESIGN_WIDTH, maxHeight.value / DESIGN_HEIGHT), LocalDensity.current)
        Box(Modifier.fillMaxWidth().height(u.dp(430))) {
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
            info.content?.videoUri?.let { Icon1VideoOverlay(videoUri = it, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.58f to Color.Transparent, 1f to PanelBase)))
        }

        Row(
            Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(300)).fillMaxWidth().height(u.dp(130)),
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
                stats.forEach { BandStat(it, u) }
            }
            Row(Modifier.padding(bottom = u.dp(4)), horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
                val primary = when {
                    info.isApp -> "Open"
                    item.lastOpenedAt != null -> "Continue"
                    else -> "Play"
                }
                PanelButton(GamepadAction.SELECT, primary, u) { onAction(GamepadAction.SELECT) }
                PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "⋯", u) { onAction(GamepadAction.OPEN_CONTEXT_MENU) }
            }
        }

        Column(
            Modifier.padding(start = u.dp(80), end = u.dp(80), top = u.dp(470)).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            val media = info.content?.media.orEmpty()
            val description = info.content?.description?.takeIf { it.isNotBlank() }
            when {
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

@Composable
private fun BandStat(stat: GameInfoStat, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
        Text(stat.value, color = Color.White, fontSize = u.sp(26), fontWeight = FontWeight.ExtraLight, maxLines = 1)
        Text(stat.label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String, u: DesignUnits) {
    Text(text.uppercase(), style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(12), letterSpacing = 0.16.em),
        maxLines = 1, overflow = TextOverflow.Ellipsis)
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
