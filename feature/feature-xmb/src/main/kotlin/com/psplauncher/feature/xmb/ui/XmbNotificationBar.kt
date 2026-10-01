package com.psplauncher.feature.xmb.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.psplauncher.core.ui.theme.menuCursorEdge
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.psplauncher.core.ui.notification.AndroidNotice
import com.psplauncher.feature.xmb.viewmodel.NoticeFocus
import com.psplauncher.core.ui.notification.SystemToast
import com.psplauncher.core.ui.notification.ToastKind
import com.psplauncher.feature.xmb.viewmodel.LibraryChip
import com.psplauncher.feature.xmb.viewmodel.QuickSetting
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle

data class NoticeMedia(
    val title: String,
    val detail: String?,
    val artUri: String?,

    val progress: Float?,

    val elapsed: String?,
    val isPlaying: Boolean,

    val hasTransport: Boolean,
    val primaryLabel: String,
)

data class QuickSettingsState(
    val waveOn: Boolean,
    val backdropOn: Boolean,
    val recentAppsOn: Boolean,
    val chips: List<LibraryChip>,
)

@Composable
fun XmbNotificationBar(
    open: Boolean,
    items: List<SystemToast>,

    android: List<AndroidNotice> = emptyList(),

    androidAccessGranted: Boolean = true,

    media: NoticeMedia? = null,

    focus: NoticeFocus? = null,
    quick: QuickSettingsState? = null,
    quickFocus: QuickSetting? = null,
    chipFocus: Int = 0,
    accent: Color = MediaDefaultAccent,
    onQuickTapped: (QuickSetting, Int) -> Unit = { _, _ -> },
    onGrantAndroidAccess: () -> Unit = {},
    onNoticeTapped: (String) -> Unit = {},
    onNoticeDismissTapped: (String) -> Unit = {},
    onMediaPrimary: () -> Unit = {},
    onMediaPrev: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (open) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInVertically(tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080605))
                    .clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
            ) {
                MediaDesignFrame { u ->
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                        listOf(mediaGlow(accent, 0.14f), Color.Transparent),
                        center = Offset(constraints.maxWidth * 0.75f, constraints.maxHeight * 0.2f),
                        radius = constraints.maxWidth * 0.8f,
                    )))

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(StripHeight + u.dp(92))
                            .background(Brush.horizontalGradient(listOf(mediaGlow(accent, 0.30f), Color(0xFF0B0807)))),
                    )
                    media?.let {
                        MediaBand(it, focus == NoticeFocus.Media, accent, u, onMediaPrimary, onMediaPrev, onMediaNext,
                            Modifier.padding(top = StripHeight).padding(horizontal = u.dp(32)).height(u.dp(92)))
                    }

                    val top = StripHeight + u.dp(if (media != null) 114 else 24)
                    quick?.let {
                        QuickColumn(it, quickFocus, chipFocus, accent, u, onQuickTapped,
                            Modifier.offset(u.dp(32), top).width(u.dp(330)))
                    }
                    Box(Modifier.offset(u.dp(392), top).width(u.dp(1)).fillMaxHeight().padding(bottom = u.dp(70))
                        .background(Color.White.copy(alpha = 0.08f)))

                    Row(
                        Modifier.offset(u.dp(424), top).fillMaxWidth().padding(end = u.dp(456)).padding(bottom = u.dp(70)),
                        horizontalArrangement = Arrangement.spacedBy(u.dp(36)),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(14))) {
                            Text("ANDROID  ·  ${android.size}", style = u.eyebrow())
                            when {
                                !androidAccessGranted -> EmptyNote("Turn on Notification access to see these here", u, onGrantAndroidAccess)
                                android.isEmpty() -> EmptyNote("Nothing from other apps", u)
                                else -> {
                                    val focusedKey = (focus as? NoticeFocus.Notice)?.key
                                    val listState = rememberLazyListState()
                                    LaunchedEffect(focusedKey, android) {
                                        val at = android.indexOfFirst { it.key == focusedKey }
                                        if (at >= 0) listState.animateScrollToItem(at)
                                    }
                                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                                        items(android, key = { it.key }) { notice ->
                                            NoticeRow(
                                                glyph = notice.appLabel.trim().firstOrNull()?.uppercase() ?: "?",
                                                glyphTint = Color.White,
                                                title = notice.title ?: notice.appLabel,
                                                detail = listOfNotNull(notice.appLabel.takeIf { notice.title != null }, notice.text).joinToString("  ·  "),
                                                focused = focusedKey == notice.key,
                                                accent = accent,
                                                u = u,
                                                onClick = if (notice.canOpen) ({ onNoticeTapped(notice.key) }) else null,
                                                onDismiss = if (notice.canDismiss) ({ onNoticeDismissTapped(notice.key) }) else null,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(14))) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("LAUNCHER  ·  ${items.size}", style = u.eyebrow())
                                if (items.isNotEmpty()) {
                                    Text("Clear", color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12),
                                        modifier = Modifier.clip(RoundedCornerShape(u.dp(6))).clickable(onClick = onClear).padding(horizontal = u.dp(6)))
                                }
                            }
                            if (items.isEmpty()) EmptyNote("Nothing has happened yet", u)
                            items.take(ColumnRows).forEachIndexed { i, toast ->
                                val error = toast.kind == ToastKind.ERROR
                                NoticeRow(
                                    glyph = if (error) "!" else "\u2713",
                                    glyphTint = if (error) ErrorTint else SuccessTint,
                                    title = toast.title,
                                    detail = toast.message,
                                    focused = false,
                                    accent = accent,
                                    u = u,
                                    modifier = Modifier.graphicsLayer(alpha = (1f - i * 0.15f).coerceAtLeast(0.5f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaBand(
    media: NoticeMedia,
    focused: Boolean,
    accent: Color,
    u: DesignUnits,
    onPrimary: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(u.dp(12)))
            .then(if (focused) Modifier.border(u.dp(2), accent, RoundedCornerShape(u.dp(12))) else Modifier)
            .padding(horizontal = u.dp(10)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(20)),
    ) {
        Box(Modifier.size(u.dp(70)).clip(RoundedCornerShape(u.dp(9))).background(Color(0xFF2A201E))) {
            media.artUri?.let { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.width(u.dp(320)), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            Text(media.title, color = Color.White, fontSize = u.sp(24), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(media.detail, media.elapsed).joinToString("  ·  ")
            if (sub.isNotBlank()) Text(sub, color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.weight(1f).height(u.dp(40)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
            val played = media.progress ?: 0f
            BAR_HEIGHTS.forEachIndexed { i, h ->
                val lit = media.progress != null && i < (played * BAR_HEIGHTS.size)
                Box(Modifier.width(u.dp(4)).height(u.dp(h)).clip(RoundedCornerShape(u.dp(2)))
                    .background(if (lit) accent else Color.White.copy(alpha = 0.2f)))
            }
        }
        if (media.hasTransport) {
            BandButton(Icons.Filled.SkipPrevious, "Previous track", u.dp(44), u.dp(22), Color.Transparent, Color.White, onPrev)
            BandButton(if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (media.isPlaying) "Pause" else "Play",
                u.dp(54), u.dp(20), Color.White, Color(0xFF111111), onPrimary)
            BandButton(Icons.Filled.SkipNext, "Next track", u.dp(44), u.dp(22), Color.Transparent, Color.White, onNext)
        } else {
            Row(
                Modifier.clip(RoundedCornerShape(u.dp(999))).background(Color.White).clickable(onClick = onPrimary)
                    .padding(horizontal = u.dp(16), vertical = u.dp(10)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF111111), modifier = Modifier.size(u.dp(20)))
                Spacer(Modifier.width(u.dp(6)))
                Text(media.primaryLabel, color = Color(0xFF111111), fontSize = u.sp(15), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BandButton(icon: ImageVector, label: String, box: androidx.compose.ui.unit.Dp, glyph: androidx.compose.ui.unit.Dp, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(box).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(glyph))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickColumn(
    quick: QuickSettingsState,
    focus: QuickSetting?,
    chipFocus: Int,
    accent: Color,
    u: DesignUnits,
    onTapped: (QuickSetting, Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
        Text("QUICK SETTINGS", style = u.eyebrow(), modifier = Modifier.padding(start = u.dp(14), bottom = u.dp(8)))
        QuickSetting.entries.forEach { setting ->
            val focused = setting == focus
            val (label, value, on) = when (setting) {
                QuickSetting.WAVE -> Triple("Wave", if (quick.waveOn) "On" else "Off", quick.waveOn)
                QuickSetting.BACKDROP -> Triple("Crossbar shows", if (quick.backdropOn) "Art" else "Wallpaper", quick.backdropOn)
                QuickSetting.RECENT_APPS -> Triple("Apps in Recent", if (quick.recentAppsOn) "On" else "Off", quick.recentAppsOn)
                QuickSetting.LIBRARIES -> Triple("Libraries", "${quick.chips.count { it.visible }} of ${quick.chips.size}", true)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(u.dp(12)))
                    .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.08f)).border(u.dp(2), accent, RoundedCornerShape(u.dp(12))) else Modifier)
                    .clickable { onTapped(setting, if (setting == QuickSetting.LIBRARIES) chipFocus else 0) }
                    .padding(horizontal = u.dp(14), vertical = u.dp(12)),
                verticalArrangement = Arrangement.spacedBy(u.dp(10)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
                    Icon(quickIcon(setting), null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(u.dp(20)))
                    Text(label, color = Color.White, fontSize = u.sp(17), fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text(value, color = when {
                        setting == QuickSetting.LIBRARIES -> Color.White.copy(alpha = 0.7f)
                        on -> accent
                        else -> Color.White.copy(alpha = 0.45f)
                    }, fontSize = u.sp(14), fontWeight = FontWeight.SemiBold)
                }
                if (setting == QuickSetting.LIBRARIES) {
                    FlowRow(Modifier.padding(start = u.dp(34)), horizontalArrangement = Arrangement.spacedBy(u.dp(6)), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                        quick.chips.forEachIndexed { i, chip ->
                            val chipFocused = focused && i == chipFocus
                            Text(
                                chip.name,
                                color = if (chip.visible) Color.White else Color.White.copy(alpha = 0.45f),
                                fontSize = u.sp(12),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(u.dp(999)))
                                    .then(when {
                                        chipFocused -> Modifier.border(u.dp(2), accent, RoundedCornerShape(u.dp(999)))
                                        chip.visible -> Modifier
                                        else -> Modifier.border(u.dp(1), Color.White.copy(alpha = 0.25f), RoundedCornerShape(u.dp(999)))
                                    })
                                    .background(if (chip.visible) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { onTapped(QuickSetting.LIBRARIES, i) }
                                    .padding(horizontal = u.dp(10), vertical = u.dp(4)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun quickIcon(setting: QuickSetting): ImageVector = when (setting) {
    QuickSetting.WAVE -> Icons.Outlined.Waves
    QuickSetting.BACKDROP -> Icons.Outlined.Image
    QuickSetting.RECENT_APPS -> Icons.Outlined.History
    QuickSetting.LIBRARIES -> Icons.AutoMirrored.Outlined.List
}

@Composable
private fun EmptyNote(text: String, u: DesignUnits, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = Muted,
        fontSize = u.sp(15),
        modifier = Modifier
            .clip(RoundedCornerShape(u.dp(6)))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = u.dp(2)),
    )
}

@Composable
private fun NoticeRow(
    glyph: String,
    glyphTint: Color,
    title: String,
    detail: String?,
    focused: Boolean,
    accent: Color,
    u: DesignUnits,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(u.dp(12)))
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.08f)).border(u.dp(2), accent, RoundedCornerShape(u.dp(12))) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = u.dp(8), vertical = u.dp(8)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(16)),
    ) {
        Box(Modifier.width(u.dp(44)), contentAlignment = Alignment.Center) {
            Text(glyph, color = glyphTint, fontSize = u.sp(22), fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            Text(title, color = Color.White, fontSize = u.sp(19), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(14), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        onDismiss?.let {
            Text("\u00d7", color = if (focused) Color.White else Muted, fontSize = u.sp(22), fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(u.dp(6))).clickable(onClick = it).padding(horizontal = u.dp(8)))
        }
    }
}

private val BAR_HEIGHTS = listOf(14, 26, 18, 34, 22, 30, 12, 24, 32, 16, 26, 20, 30, 14, 24, 34, 18, 28, 12, 22)

private val Muted = Color(0x99FFFFFF)
private val SuccessTint = Color(0xFF6FD08C)
private val ErrorTint = Color(0xFFF07C85)

private const val ColumnRows = 5
