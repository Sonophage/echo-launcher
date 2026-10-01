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

@Composable
fun XmbNotificationBar(
    open: Boolean,
    items: List<SystemToast>,

    android: List<AndroidNotice> = emptyList(),

    androidAccessGranted: Boolean = true,

    media: NoticeMedia? = null,

    focus: NoticeFocus? = null,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(RowGap),
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to SheetScrim,
                            ScrimHold to SheetScrim,
                            1f to SheetScrim.copy(alpha = ScrimTail),
                        ),
                    )

                    .padding(top = StripHeight + 10.dp),
            ) {
                media?.let {
                    MediaRow(
                        media = it,
                        focused = focus == NoticeFocus.Media,
                        onPrimary = onMediaPrimary,
                        onPrev = onMediaPrev,
                        onNext = onMediaNext,
                        modifier = Modifier.padding(horizontal = EdgeGap),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ColumnGap),
                    modifier = Modifier.weight(1f).padding(horizontal = EdgeGap),
                ) {
                    NoticeColumn(label = "System", modifier = Modifier.weight(1f)) {
                        when {
                            !androidAccessGranted -> EmptyNote(
                                "Turn on Notification access to see these here",
                                onClick = onGrantAndroidAccess,
                            )
                            android.isEmpty() -> EmptyNote("Nothing from other apps")
                            else -> {
                                val focusedKey = (focus as? NoticeFocus.Notice)?.key
                                val listState = rememberLazyListState()
                                LaunchedEffect(focusedKey, android) {
                                    val at = android.indexOfFirst { it.key == focusedKey }
                                    if (at >= 0) listState.animateScrollToItem(at)
                                }
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(android, key = { it.key }) { notice ->
                                        NoticeCard(
                                            lead = notice.appLabel,
                                            title = notice.title ?: notice.appLabel,
                                            detail = notice.text,
                                            accent = null,
                                            focused = focusedKey == notice.key,

                                            onClick = if (notice.canOpen) ({ onNoticeTapped(notice.key) }) else null,
                                            onDismiss = if (notice.canDismiss) ({ onNoticeDismissTapped(notice.key) }) else null,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NoticeColumn(label = "Launcher", modifier = Modifier.weight(1f)) {
                        if (items.isEmpty()) {
                            EmptyNote("Nothing has happened yet")
                        } else {
                            items.take(ColumnRows).forEach { toast ->
                                NoticeCard(
                                    lead = if (toast.kind == ToastKind.ERROR) "!" else "\u2713",
                                    title = toast.title,
                                    detail = toast.message,
                                    accent = if (toast.kind == ToastKind.ERROR) ErrorTint else SuccessTint,
                                )
                            }
                            Text(
                                "Clear",
                                color = Muted,
                                fontSize = DetailSize,
                                lineHeight = DetailSize * 1.3f,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onClear)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeColumn(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Text(
            label,
            color = Muted,
            fontSize = DetailSize,
            lineHeight = DetailSize * 1.3f,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun EmptyNote(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = Muted,
        fontSize = DetailSize,
        lineHeight = DetailSize * 1.3f,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun NoticeCard(
    lead: String,
    title: String,
    detail: String?,
    accent: Color?,
    focused: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotificationChipCorner))

            .background(
                if (focused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(GlyphSlot)
                .clip(RoundedCornerShape(5.dp))
                .background((accent ?: Color.White).copy(alpha = if (accent != null) 0.18f else 0.12f)),
        ) {
            Text(
                text = lead.trim().firstOrNull()?.uppercase() ?: "?",
                color = accent ?: Color.White,
                fontSize = DetailSize,
                lineHeight = DetailSize * 1.3f,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                title,
                color = Color.White,
                fontSize = TitleSize,
                lineHeight = TitleSize * 1.3f,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.let {
                Text(
                    it,
                    color = Muted,
                    fontSize = DetailSize,
                    lineHeight = DetailSize * 1.3f,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        onDismiss?.let {
            Spacer(Modifier.weight(1f))
            Text(
                "\u00d7",
                color = if (focused) Color.White else Muted,
                fontSize = TitleSize,
                lineHeight = TitleSize * 1.3f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = it)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun MediaRow(
    media: NoticeMedia,
    focused: Boolean,
    onPrimary: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotificationChipCorner))
            .background(if (focused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            media.artUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(MediaArt).clip(RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    media.title,
                    color = Color.White,
                    fontSize = TitleSize,
                    lineHeight = TitleSize * 1.3f,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                (media.elapsed ?: media.detail)?.let {
                    Text(
                        it,
                        color = Muted,
                        fontSize = DetailSize,
                        lineHeight = DetailSize * 1.3f,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (media.hasTransport) {
                TransportButton(Icons.Filled.SkipPrevious, "Previous", onPrev)
                Spacer(Modifier.width(6.dp))
                TransportButton(
                    if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (media.isPlaying) "Pause" else "Play",
                    onPrimary,
                    primary = true,
                )
                Spacer(Modifier.width(6.dp))
                TransportButton(Icons.Filled.SkipNext, "Next", onNext)
            } else {
                TransportLabel(media.primaryLabel, onPrimary)
            }
        }
        media.progress?.let { fraction ->
            Spacer(Modifier.padding(top = 7.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val diameter = if (primary) 32.dp else 26.dp
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (primary) menuCursorEdge() else Color.White.copy(alpha = 0.13f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (primary) Color(0xFF0B0B12) else Color.White,
            modifier = Modifier.size(if (primary) 18.dp else 14.dp),
        )
    }
}

@Composable
private fun TransportLabel(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(NotificationChipCorner))
            .background(menuCursorEdge())
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF0B0B12),
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = Color(0xFF0B0B12),
            fontSize = DetailSize,
            lineHeight = DetailSize * 1.3f,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private val Muted = Color(0x99FFFFFF)
private val SuccessTint = Color(0xFF6FD08C)
private val ErrorTint = Color(0xFFE2606A)
private val GlyphSlot = 22.dp
private val MediaArt = 34.dp
private val RowGap = 8.dp
private val ColumnGap = 22.dp
private val EdgeGap = 20.dp

private const val ColumnRows = 5

private val SheetScrim = Color(0xFA050200)

private const val ScrimHold = 0.78f

private const val ScrimTail = 0.55f

private val TitleSize = NotificationBarStyle.TitleSp.sp
private val DetailSize = NotificationBarStyle.DetailSp.sp

object NotificationBarStyle {
    const val LegibilityFloorPx = LEGIBILITY_FLOOR_PX

    const val PanelDensity = 2.3375f

    const val TitleSp = 13f
    const val DetailSp = 12f
}

private val NotificationChipCorner = 7.dp
