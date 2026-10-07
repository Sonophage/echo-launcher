package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.echoPulse
import androidx.compose.ui.unit.sp
import com.echo.feature.crossbar.viewmodel.StageCommand
import com.echo.core.ui.design.pressAndHold
import com.echo.core.ui.design.holdRing
import com.echo.core.ui.design.holdProgress
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.graphics.ImageBitmap
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.components.chromeGutter
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.mediaAccent
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.feature.crossbar.R
import com.echo.feature.crossbar.viewmodel.PanelStage
import com.echo.feature.crossbar.viewmodel.countLabel
import com.echo.feature.crossbar.viewmodel.islandProgress
import com.echo.feature.crossbar.viewmodel.timeLabel
import kotlinx.coroutines.delay
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.echo.core.ui.design.NeckFlare
import com.echo.core.ui.design.drawIslandNeck
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import com.echo.core.ui.design.MarkPose
import com.echo.core.ui.design.drawEchoMark
import java.util.Date
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.components.ChromeScrim

private val StripPrimary = Color(0xFFEEEEEE)
private val StripMuted   = Color(0xAAEEEEEE)

object CrossbarStatusIcons {
    @DrawableRes val bluetooth: Int = R.drawable.ic_status_bluetooth

    @DrawableRes fun battery(level: Int, charging: Boolean): Int =
        requireNotNull(forSlotKey(batterySlotKey(level, charging))) {
            "batterySlotKey produced a key outside the status strip"
        }

    fun batterySlotKey(level: Int, charging: Boolean): String = when {
        charging       -> "status_battery_charging"
        level >= 76    -> "status_battery_full"
        level >= 51    -> "status_battery_high"
        level >= 26    -> "status_battery_medium"
        else           -> "status_battery_low"
    }

    @DrawableRes fun forSlotKey(slotKey: String): Int? = when (slotKey) {
        "status_bluetooth"         -> R.drawable.ic_status_bluetooth
        "status_battery_charging"  -> R.drawable.ic_status_battery_charging
        "status_battery_full"      -> R.drawable.ic_status_battery_full
        "status_battery_high"      -> R.drawable.ic_status_battery_high
        "status_battery_medium"    -> R.drawable.ic_status_battery_medium
        "status_battery_low"       -> R.drawable.ic_status_battery_low
        else                       -> null
    }
}


data class StripLiveActivity(val art: Any?, val title: String, val detail: String?, val stage: PanelStage? = null, val accentArgb: Long? = null)

data class BatteryReading(val level: Int = 0, val charging: Boolean = false)

// one reading for the strip's percentage and the battery line along the bottom edge
@Composable
fun rememberBatteryReading(): BatteryReading {
    val context = LocalContext.current
    var reading by remember { mutableStateOf(BatteryReading()) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                reading = BatteryReading(
                    level = if (level >= 0 && scale > 0) (level * 100 / scale) else 0,
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                )
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return reading
}

data class StripHints(val shoulder: Boolean = false)

// the newest notification, for the right-hand island's peek
data class NoticePeek(val postedAt: Long, val title: String, val detail: String?, val packageName: String?)

// one row of the notification card
data class NoticeRow(val key: String, val title: String, val detail: String?, val packageName: String?, val canDismiss: Boolean)

// owner, 2026-10-05: a notification newer than any seen peeks from the right, once; one already there when
// the strip appears, or an older one left after a dismissal, does not
internal fun noticePeekDue(seenAt: Long, newest: NoticePeek?): Boolean = newest != null && newest.postedAt > seenAt


// the islands' cards grow out of their orb, down below the bar, and go back into it
private fun cardDrop(origin: TransformOrigin) =
    fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.3f, transformOrigin = origin)

private fun cardLift(origin: TransformOrigin) =
    fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.3f, transformOrigin = origin)



@Composable
fun CrossbarStatusStrip(
    // the XMB's sort as a Recent-style row (owner, 2026-10-04): the mode labels and the active one
    sortRow: Pair<List<String>, Int>? = null,
    onSortPicked: (Int) -> Unit = {},

    live: StripLiveActivity? = null,

    hints: StripHints = StripHints(),

    onLiveAreaTapped: (() -> Unit)? = null,

    noticeCount: Int = 0,
    noticePeek: NoticePeek? = null,
    // the card's rows, newest first, and the pad's row on it (null when the pad is not on the card)
    noticeRows: List<NoticeRow> = emptyList(),
    noticeCursor: Int? = null,
    onNoticeOpen: (String) -> Unit = {},
    onNoticeDismiss: (String) -> Unit = {},
    // the card is out (the view model times it); a press on the island, and a new notification arriving
    noticeCardOut: Boolean = false,
    onNoticeIslandPressed: () -> Unit = {},
    onNewNotice: () -> Unit = {},
    // the right-hand island with no notifications: the user's picture, else the ECHO mark
    profileAvatar: String? = null,

    // the top-left orb's level for the live activity (0 rest, 1 focused, 2 expanded); -1 keeps the plain island
    orbLevel: Int = -1,
    onOrbTapped: () -> Unit = {},
    onOrbTransport: (StageCommand) -> Unit = {},

    // over 0, the island's tap is a launch and must be held
    holdMs: Long = 0L,
    holding: Boolean = false,

    compact: Boolean = false,

    // the island rests as its small orb, so the bar has room for a screen's own row (owner, 2026-10-04:
    // the app drawer's sections); a tap on it does nothing, since the card's tap is a held launch
    minimized: Boolean = false,

    modifier: Modifier = Modifier,

    battery: BatteryReading = rememberBatteryReading(),

    centre: (@Composable BoxScope.(DesignUnits, Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val batteryLevel = battery.level
    var timeString     by remember { mutableStateOf(currentTimeString(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            timeString = currentTimeString(context)

            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val u = rememberStripUnits()
    val band = if (compact) StripHeight else stripBandHeight(u)

    val fallback = menuCursorEdge()
    val stage = live?.stage
    val stageIcon = rememberAppIcon(stage?.let { stagePackage(it, context.packageName) })
    val islandTint by animateColorAsState(
        live?.accentArgb?.let(::mediaAccent) ?: stage?.let { stageTint(it, stageIcon?.color, fallback) } ?: fallback,
        tween(500),
        label = "islandTint",
    )
    val islandGlow = lerp(islandTint, Color.White, 0.3f)

    Box(
        modifier
            .fillMaxWidth()
            .height(StripHeight),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(if (compact) StripHeight else u.dp(110))
                .background(Brush.verticalGradient(0f to ChromeScrim, 1f to Color.Transparent)),
        )

        // the card comes in rather than appearing (owner, 2026-10-04); the last one stays for its exit
        var shownLive by remember { mutableStateOf(live) }
        if (live != null) shownLive = live
        val islandMode = when {
            live == null -> IslandMode.NONE
            minimized -> IslandMode.ORB
            // at rest it is the orb on every screen: over Game Info or the achievements the card covered the screen
            // (owner, 2026-10-06)
            orbLevel == 0 -> IslandMode.ORB
            else -> IslandMode.CARD
        }
        // owner, 2026-10-05: the orb stays in the bar, and the card drops below the bar from it, as the
        // notification island's card does on the right. A bridge in the card's colour joins the two, so the
        // orb and its card read as one shape
        // just under each orb (owner, 2026-10-05: closer to the icon). The left orb hangs 10 from the top; the
        // right one is centred in the band
        val cardTop = u.dp(10 + 44 + 2)
        val noticeCardTop = band / 2 + u.dp(22 + 2)
        androidx.compose.animation.AnimatedVisibility(
            visible = islandMode == IslandMode.CARD,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopStart).padding(start = chromeGutter()).wrapContentHeight(Alignment.Top, unbounded = true),
        ) { IslandBridge(islandCardFill(islandTint), orbCentreY = u.dp(32), cardTop = cardTop, end = false, u = u) }
        androidx.compose.animation.AnimatedVisibility(
            visible = islandMode != IslandMode.NONE,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            shownLive?.let { activity ->
                RestOrb(
                    activity = activity,
                    tint = islandTint,
                    glow = islandGlow,
                    u = u,
                    stageIcon = stageIcon?.bitmap,
                    onTapped = when {
                        minimized -> ({})
                        islandMode == IslandMode.CARD && orbLevel < 0 -> onLiveAreaTapped ?: {}
                        else -> onOrbTapped
                    },
                    modifier = Modifier.padding(start = chromeGutter(), top = u.dp(10)),
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = islandMode == IslandMode.CARD,
            enter = cardDrop(TransformOrigin(0f, 0f)),
            exit = cardLift(TransformOrigin(0f, 0f)),
            modifier = Modifier.align(Alignment.TopStart).padding(start = chromeGutter(), top = cardTop)
                .wrapContentHeight(Alignment.Top, unbounded = true),
        ) {
            shownLive?.let { activity ->
                val music = orbLevel > 0 && activity.stage is PanelStage.Music
                IslandCard(
                    activity = activity,
                    tint = islandTint,
                    glow = islandGlow,
                    u = u,
                    compact = compact,
                    onTapped = if (music) onOrbTapped else onLiveAreaTapped,
                    stageIcon = stageIcon?.bitmap,
                    transport = if (music) onOrbTransport else null,
                    expanded = music && orbLevel == 2,
                    holdMs = holdMs,
                    holding = holding,
                )
            }
        }

        val centreSlot: @Composable (Boolean) -> Unit = { tight ->
            Box(contentAlignment = Alignment.Center) {
                when {
                    centre != null -> centre.invoke(this, u, tight)
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(u.dp(18)),
                    ) {
                        sortRow?.let { (labels, active) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
                                if (LocalPadPrompts.current) ControllerPrompt(GamepadAction.CHANGE_SORT, "", glyphSize = u.dp(22), spacing = 0.dp)
                                StripSections(labels = labels, selected = active, onTapped = onSortPicked, u = u, shoulders = false)
                            }
                        }
                        if (hints.shoulder) StripHint("LB  RB", u)
                    }
                }
            }
        }

        // owner, 2026-10-05: the right-hand island peeks a new notification, as the left one shows what is live.
        // A tap on the island brings the card out; a second tap (on the card) opens the notifications
        // owner, 2026-10-05: only what arrives after ECHO is up peeks; the notifications already waiting at boot,
        // which load a moment after the strip, do not
        var seenAt by remember { mutableStateOf(maxOf(noticePeek?.postedAt ?: 0L, System.currentTimeMillis())) }
        LaunchedEffect(noticePeek) {
            if (!noticePeekDue(seenAt, noticePeek)) return@LaunchedEffect
            seenAt = noticePeek?.postedAt ?: return@LaunchedEffect
            onNewNotice()
        }
        val peeking = noticeRows.takeIf { noticeCardOut && it.isNotEmpty() }

        val endGutter = chromeGutter(end = true)
        val statusSlot: @Composable (Boolean) -> Unit = { date ->
            Row(
                modifier = Modifier.padding(end = endGutter),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(22)),
            ) {
                // kit status corner: battery and time, then the notification island at the far right
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(14))) {
                    Text(
                        text       = "$batteryLevel%",
                        color      = StripPrimary.copy(alpha = 0.7f),
                        fontSize   = u.sp(13),
                        fontWeight = FontWeight.Light,
                        maxLines   = 1,
                    )
                    Text(
                        text       = timeString,
                        color      = StripPrimary,
                        fontSize   = u.sp(13),
                        fontWeight = FontWeight.Light,
                        maxLines   = 1,
                    )
                }
                NoticeOrb(noticeCount, profileAvatar, islandGlow, u, onNoticeIslandPressed, connected = peeking != null)
            }
        }

        // the card drops below the bar, so the bar only ever holds the orb
        val islandWidth = if (live == null) 0.dp else u.dp(44) + chromeGutter()
        androidx.compose.animation.AnimatedVisibility(
            visible = peeking != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = endGutter).wrapContentHeight(Alignment.Top, unbounded = true),
        ) { IslandBridge(NoticeOrbFill, orbCentreY = band / 2, cardTop = noticeCardTop, end = true, u = u) }
        SubcomposeLayout(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(band),
        ) { constraints ->
            val loose = Constraints(maxWidth = constraints.maxWidth)
            fun probe(key: String, content: @Composable () -> Unit) =
                subcompose(key) { Box(Modifier.clearAndSetSemantics {}) { content() } }.first().measure(loose)
            val full = probe("full") { centreSlot(false) }
            val tight = probe("tight") { centreSlot(true) }
            val dated = probe("dated") { statusSlot(!compact) }
            val bare = probe("bare") { statusSlot(false) }
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val fit = stripFit(
                width = width,
                left = islandWidth.roundToPx(),
                gap = u.dp(18).roundToPx(),
                centre = full.width,
                tightCentre = tight.width,
                dated = dated.width,
                bare = bare.width,
                dateRoom = !u.square,
            )
            val centrePlaceable = subcompose("centre") { centreSlot(!fit.labels) }.first().measure(loose)
            val statusPlaceable = subcompose("status") { statusSlot(fit.date && !compact) }.first().measure(loose)
            layout(width, height) {
                centrePlaceable.place((width - centrePlaceable.width) / 2, (height - centrePlaceable.height) / 2)
                statusPlaceable.place(width - statusPlaceable.width, (height - statusPlaceable.height) / 2)
            }
        }

        var shownPeek by remember { mutableStateOf<List<NoticeRow>?>(null) }
        peeking?.let { shownPeek = it }
        androidx.compose.animation.AnimatedVisibility(
            visible = peeking != null,
            enter = cardDrop(TransformOrigin(1f, 0f)),
            exit = cardLift(TransformOrigin(1f, 0f)),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = endGutter, top = noticeCardTop)
                .wrapContentHeight(Alignment.Top, unbounded = true),
        ) {
            shownPeek?.let { NoticeCard(it, noticeCursor, u, noticeCardTop, onNoticeOpen, onNoticeDismiss) }
        }
    }
}

// the right-hand island at rest (owner, 2026-10-05): with notifications, their count in the notification colour
// to the left of the ECHO mark; with none, the user's picture (the mark when there is none). The orb itself
// stays uncoloured
@Composable
private fun NoticeOrb(count: Int, avatar: String?, accent: Color, u: DesignUnits, onTapped: () -> Unit, connected: Boolean = false) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onTapped)
            .semantics { contentDescription = countLabel(count, "notification") },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(8)),
    ) {
        if (count > 0) Text(count.toString(), color = accent, fontSize = u.sp(15), fontWeight = FontWeight.Bold, maxLines = 1)
        Box(
            Modifier
                .size(u.dp(44))
                // the same ring and inset as the left island's orb, so the two read as a pair; joined to its card,
                // the ring gives way so the orb and the card are one shape
                .then(if (connected) Modifier else Modifier.border(u.dp(2), Color.White.copy(alpha = 0.18f), CircleShape))
                .padding(u.dp(5))
                .clip(CircleShape)
                .background(NoticeOrbFill),
            contentAlignment = Alignment.Center,
        ) {
            if (count == 0 && avatar != null) {
                AsyncImage(avatar, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    drawEchoMark(MarkPose(cx = size.width / 2 / density, cy = size.height / 2 / density, size = size.width * 0.8f / density))
                }
            }
        }
    }
}

private val NoticeOrbFill = Color(0xFF12151C)

private fun islandCardFill(tint: Color): Color = lerp(tint, Color.Black, 0.35f)

// the neck between an orb and the card below it (drawIslandNeck), on the card's inner side only, since the
// card lines up with the orb's outer edge
@Composable
private fun IslandBridge(fill: Color, orbCentreY: Dp, cardTop: Dp, end: Boolean, u: DesignUnits) {
    val orb = u.dp(44)
    // a long, soft flare (owner, 2026-10-05: curvier and smoother)
    val flare = u.dp(40)
    Canvas(Modifier.width(orb + flare).height(cardTop + 1.dp)) {
        val r = orb.toPx() / 2f
        val cx = if (end) size.width - r else r
        drawIslandNeck(fill, Offset(cx, orbCentreY.toPx()), r, size.height, flare.toPx(), if (end) NeckFlare.LEFT else NeckFlare.RIGHT)
    }
}

// owner, 2026-10-05: the notification card lists as many notifications as fit below it, newest first. A tap
// (or A on the pad's row) opens the app, a sideways swipe (or X) dismisses one that can be dismissed
@Composable
private fun NoticeCard(rows: List<NoticeRow>, cursor: Int?, u: DesignUnits, top: Dp, onOpen: (String) -> Unit, onDismiss: (String) -> Unit) {
    val screen = LocalConfiguration.current.screenHeightDp.dp
    val rowHeight = u.dp(58)
    // room down to the footer
    val fits = (((screen - top - u.dp(90) - u.dp(16)) / rowHeight).toInt()).coerceAtLeast(1)
    val shown = rows.take(fits)
    // its top right corner joins the orb above it
    val shape = RoundedCornerShape(topStart = u.dp(20), topEnd = 0.dp, bottomEnd = u.dp(20), bottomStart = u.dp(20))
    Column(
        Modifier
            .width(u.dp(320))
            .clip(shape)
            // opaque and uncoloured (owner, 2026-10-05)
            .background(NoticeOrbFill)
            .padding(u.dp(8)),
        verticalArrangement = Arrangement.spacedBy(u.dp(2)),
    ) {
        shown.forEachIndexed { index, row ->
            key(row.key) { NoticeCardRow(row, index == cursor, u, rowHeight, onOpen, onDismiss) }
        }
        if (rows.size > shown.size) {
            Text("+${rows.size - shown.size} more", color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(11),
                modifier = Modifier.padding(horizontal = u.dp(10), vertical = u.dp(4)))
        }
    }
}

@Composable
private fun NoticeCardRow(row: NoticeRow, focused: Boolean, u: DesignUnits, height: Dp, onOpen: (String) -> Unit, onDismiss: (String) -> Unit) {
    val icon = rememberAppIcon(row.packageName?.takeIf { it.isNotBlank() })
    var drag by remember { mutableStateOf(0f) }
    val threshold = with(LocalDensity.current) { u.dp(90).toPx() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .graphicsLayer { translationX = drag; alpha = 1f - (kotlin.math.abs(drag) / (threshold * 2)).coerceIn(0f, 0.7f) }
            .clip(RoundedCornerShape(u.dp(14)))
            .background(if (focused) Color.White.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (row.canDismiss) Modifier.pointerInput(row.key) {
                    detectHorizontalDragGestures(
                        onDragEnd = { if (kotlin.math.abs(drag) > threshold) onDismiss(row.key) else drag = 0f },
                        onDragCancel = { drag = 0f },
                    ) { change, dx -> change.consume(); drag += dx }
                } else Modifier,
            )
            .clickable { onOpen(row.key) }
            .padding(horizontal = u.dp(10), vertical = u.dp(8)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
    ) {
        Box(Modifier.size(u.dp(32)).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            if (icon != null) Image(icon.bitmap, null, Modifier.fillMaxSize())
            else Icon(Icons.Outlined.Notifications, null, Modifier.size(u.dp(16)), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(row.title, color = Color.White, fontSize = u.sp(13), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.detail?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(11), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (focused && row.canDismiss && LocalPadPrompts.current) ControllerPrompt(GamepadAction.CHANGE_SORT, "", glyphSize = u.dp(18), spacing = 0.dp)
    }
}

@Composable
private fun IslandCard(
    activity: StripLiveActivity,
    tint: Color,
    glow: Color,
    u: DesignUnits,
    compact: Boolean,
    onTapped: (() -> Unit)?,
    modifier: Modifier = Modifier,
    stageIcon: ImageBitmap? = null,

    // the orb's level 1: transport buttons; level 2 adds the next track
    transport: ((StageCommand) -> Unit)? = null,
    expanded: Boolean = false,
    holdMs: Long = 0L,
    holding: Boolean = false,
) {
    val stage = activity.stage
    val music = stage as? PanelStage.Music
    val positionMs = music?.livePositionMs() ?: 0L
    var pressing by remember { mutableStateOf(false) }
    // a launch hold borrows the edge the track position normally traces
    val progress = if (holdMs > 0L) holdProgress(holding || pressing, holdMs) else stage?.islandProgress(positionMs)
    val detail = listOfNotNull(activity.detail?.takeIf { it.isNotBlank() }, music?.timeLabel(positionMs)).joinToString("  ·  ")
    val playing = (stage as? PanelStage.Music)?.playing == true
    val radius = u.dp(20)
    // a whole card now, below the bar (owner, 2026-10-05); it no longer hangs from the top edge
    // its top left corner joins the orb above it, and its fill is solid so the bridge to the orb matches
    val shape = RoundedCornerShape(topStart = 0.dp, topEnd = radius, bottomEnd = radius, bottomStart = radius)
    Box(
        modifier
            .width(u.dp(264))
            .clip(shape)
            .background(islandCardFill(tint))
            .drawWithCache {
                val stroke = u.dp(2).toPx()
                val inset = stroke / 2f
                val r = radius.toPx() - inset
                val bottom = size.height - inset
                val right = size.width - inset
                // the whole edge, from the top left corner round, so a hold or the track traces all of it
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, inset, right, bottom, androidx.compose.ui.geometry.CornerRadius(r, r)))
                }
                val done = Path()
                progress?.let { p ->
                    val measure = PathMeasure()
                    measure.setPath(path, false)
                    measure.getSegment(0f, measure.length * p, done, true)
                }
                onDrawWithContent {
                    drawContent()
                    if (progress != null) {
                        drawPath(done, glow.copy(alpha = 0.3f), style = Stroke(stroke * 3f, cap = StrokeCap.Round))
                        drawPath(done, glow, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
            }
            .then(
                when {
                    onTapped == null -> Modifier
                    holdMs > 0L -> Modifier.pressAndHold(holdMs, activity.title, { pressing = it }, onTapped)
                    else -> Modifier.clickable(onClick = onTapped)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = u.dp(16), vertical = u.dp(12))) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            val tile = u.dp(34)
            Box(
                Modifier.size(tile).clip(RoundedCornerShape(u.dp(10))).background(tint),
                contentAlignment = Alignment.Center,
            ) {
                if (activity.art != null) {
                    AsyncImage(
                        model = activity.art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (stageIcon != null) {
                    androidx.compose.foundation.Image(stageIcon, null, Modifier.fillMaxSize())
                } else {
                    Icon(stageGlyph(stage ?: PanelStage.Empty), null, Modifier.size(tile * 0.6f), tint = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
                Text(
                    activity.title,
                    color = Color.White,
                    fontSize = u.sp(13),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                detail.takeIf { it.isNotBlank() && !compact }?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = u.sp(10),
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (transport != null) {
                Box(Modifier.width(1.dp).height(u.dp(30)).background(Color.White.copy(alpha = 0.2f)))
                TransportButton(Icons.Filled.SkipPrevious, "Previous track", u) { transport(StageCommand.PREV_TRACK) }
                TransportButton(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", u) {
                    transport(StageCommand.PLAY_PAUSE)
                }
                TransportButton(Icons.Filled.SkipNext, "Next track", u) { transport(StageCommand.NEXT_TRACK) }
            } else if (playing) {
                EqualizerGlyph(u)
            }
        }
        if (expanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = u.dp(10))
                    .drawBehind { drawLine(Color.White.copy(alpha = 0.18f), Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
                    .padding(top = u.dp(9)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(8)),
            ) {
                Text("NEXT", color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(10), letterSpacing = 1.4.sp, maxLines = 1)
                Text(
                    music?.nextTitle ?: "Skip to the next track",
                    color = Color.White,
                    fontSize = u.sp(10),
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (LocalPadPrompts.current) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(18), spacing = 0.dp)
            }
        }
        }
    }
}

@Composable
private fun TransportButton(icon: ImageVector, label: String, u: DesignUnits, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(u.dp(4)).size(u.dp(16)),
    )
}

// kit 05 at rest: the art in a circle, the track's progress in a ring round it
@Composable
private fun RestOrb(
    activity: StripLiveActivity,
    tint: Color,
    glow: Color,
    u: DesignUnits,
    stageIcon: ImageBitmap?,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val music = activity.stage as? PanelStage.Music
    val progress = music?.let { it.islandProgress(it.livePositionMs()) }
    var presses by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .size(u.dp(44))
            .echoPulse(presses, glow)
            .clip(CircleShape)
            .clickable(onClickLabel = activity.title) { presses++; onTapped() }
            .holdRing(progress ?: 0f, glow, if (music != null) glow.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.18f), u.dp(2))
            .padding(u.dp(5))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(tint, lerp(tint, Color.Black, 0.6f)))),
        contentAlignment = Alignment.Center,
    ) {
        when {
            activity.art != null -> AsyncImage(activity.art, activity.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            stageIcon != null -> androidx.compose.foundation.Image(stageIcon, activity.title, Modifier.fillMaxSize())
            else -> Box(Modifier.size(u.dp(10)).border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape))
        }
    }
}

@Composable
private fun EqualizerGlyph(u: DesignUnits) {
    Row(
        Modifier.height(u.dp(14)),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(u.dp(2)),
    ) {
        listOf(8, 14, 5, 11).forEach { h ->
            Box(Modifier.width(u.dp(3)).height(u.dp(h)).clip(RoundedCornerShape(u.dp(2))).background(Color.White))
        }
    }
}

@Composable
internal fun StripSections(
    labels: List<String>,
    selected: Int,
    onTapped: (Int) -> Unit,
    u: DesignUnits,
    shoulders: Boolean,
    modifier: Modifier = Modifier,
    icon: (@Composable (index: Int, tint: Color, modifier: Modifier) -> Unit)? = null,
) {
    val pad = shoulders && LocalPadPrompts.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(if (icon != null) 18 else 10))) {
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = u.dp(22), spacing = 0.dp)
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val tint = Color.White.copy(alpha = if (on) 1f else 0.5f)
            Column(
                Modifier
                    .clip(RoundedCornerShape(u.dp(8)))
                    .clickable { onTapped(i) }
                    .padding(horizontal = u.dp(6), vertical = u.dp(4))
                    .semantics { contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.dp(if (icon != null) 9 else 6)),
            ) {
                icon?.invoke(i, tint, Modifier.size(u.dp(22)))
                if (sectionLabelShown(icon != null)) {
                    Text(
                        label,
                        color = tint,
                        fontSize = u.sp(14),
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
                        maxLines = 1,
                    )
                }
                EchoDot(on, u)
            }
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(22), spacing = 0.dp)
    }
}

@Composable
private fun EchoDot(on: Boolean, u: DesignUnits) {
    Box(
        Modifier
            .size(u.dp(5))
            .then(
                if (on) Modifier.drawBehind {
                    val glow = size.minDimension * 1.8f
                    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), center, glow), glow)
                    drawCircle(Color.White)
                } else Modifier
            ),
    )
}

internal data class StripFit(val labels: Boolean, val date: Boolean)

internal fun stripFit(width: Int, left: Int, gap: Int, centre: Int, tightCentre: Int, dated: Int, bare: Int, dateRoom: Boolean): StripFit {
    fun fits(fit: StripFit): Boolean =
        (if (fit.labels) centre else tightCentre) / 2 + gap <= width / 2 - maxOf(left, if (fit.date) dated else bare)
    return listOf(StripFit(true, true), StripFit(true, false), StripFit(false, true), StripFit(false, false))
        .filter { dateRoom || !it.date }
        .firstOrNull(::fits) ?: StripFit(labels = false, date = false)
}

internal fun sectionLabelShown(hasIcon: Boolean): Boolean = !hasIcon

@Composable
internal fun BatteryLine(level: Int, charging: Boolean, glint: Boolean, modifier: Modifier = Modifier) {
    val fill = (level / 100f).coerceIn(0f, 1f)
    val travel by androidx.compose.runtime.produceState(0f, charging, glint) {
        if (!charging || !glint) return@produceState
        while (true) {
            androidx.compose.animation.core.withInfiniteAnimationFrameMillis { nowMs ->
                value = (com.echo.core.ui.wave.steppedFrameMs(nowMs) % GLINT_PERIOD_MS) / GLINT_PERIOD_MS.toFloat()
            }
        }
    }
    val low = level <= 20 && !charging
    Box(
        modifier
            .fillMaxHeight()
            .width(BatteryLineHeight)
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(fill)
                .width(BatteryLineHeight)
                .drawWithCache {
                    val base = if (low) LowBatteryTint else Color.White
                    val brush = if (!charging) {
                        SolidColor(base)
                    } else {
                        // the glint climbs from the bottom while charging
                        val glint = size.height * 0.22f
                        val head = size.height - (travel * (size.height + glint * 2f) - glint)
                        Brush.linearGradient(
                            colors = listOf(base.copy(alpha = 0.55f), Color.White, base.copy(alpha = 0.55f)),
                            start = Offset(0f, head),
                            end = Offset(0f, head - glint),
                        )
                    }
                    onDrawBehind { drawRect(brush) }
                },
        )
    }
}

@Composable
private fun StripHint(text: String, u: DesignUnits) {
    Text(text, color = StripMuted, fontSize = u.sp(10), fontWeight = FontWeight.Medium)
}

private enum class IslandMode { NONE, ORB, CARD }

private val BatteryLineHeight = 2.dp

private const val GLINT_PERIOD_MS = 2400L

internal val StripHeight: Dp
    @Composable @androidx.compose.runtime.ReadOnlyComposable get() = StatusStripHeight


private val LowBatteryTint = Color(0xFFFF6B6B)

private fun currentTimeString(context: Context): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date())

@Composable
internal fun rememberStripUnits(): DesignUnits {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    // the top bar's own size (LocalChromeScale) grows or shrinks everything in it
    val header = com.echo.core.ui.components.LocalChromeScale.current.header
    return remember(config.screenWidthDp, config.screenHeightDp, density, header) {
        val u = panelDesignUnits(config.screenWidthDp.toFloat(), config.screenHeightDp.toFloat(), density)
        DesignUnits(u.scale * header, density, u.square)
    }
}

@Composable
internal fun stripBandHeight(u: DesignUnits): Dp = maxOf(StripHeight, u.dp(76))
