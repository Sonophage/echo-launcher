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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.res.painterResource
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
import com.echo.core.ui.design.LEGIBILITY_FLOOR_PX
import com.echo.core.ui.design.mediaAccent
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.feature.crossbar.R
import com.echo.feature.crossbar.viewmodel.PanelStage
import com.echo.feature.crossbar.viewmodel.countLabel
import com.echo.feature.crossbar.viewmodel.islandProgress
import com.echo.feature.crossbar.viewmodel.timeLabel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

data class StripHints(val shoulder: Boolean = false, val leftRight: Boolean = false)

@Composable
fun CrossbarStatusStrip(
    // the XMB's sort as a Recent-style row (owner, 2026-10-04): the mode labels and the active one
    sortRow: Pair<List<String>, Int>? = null,
    onSortPicked: (Int) -> Unit = {},

    live: StripLiveActivity? = null,

    hints: StripHints = StripHints(),

    onLiveAreaTapped: (() -> Unit)? = null,

    noticeCount: Int = 0,
    onNoticeCountTapped: (() -> Unit)? = null,

    // the top-left orb's level for the live activity (0 rest, 1 focused, 2 expanded); -1 keeps the plain island
    orbLevel: Int = -1,
    onOrbTapped: () -> Unit = {},
    onOrbTransport: (StageCommand) -> Unit = {},

    // over 0, the island's tap is a launch and must be held
    holdMs: Long = 0L,
    holding: Boolean = false,

    compact: Boolean = false,

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
            orbLevel == 0 && !compact -> IslandMode.ORB
            else -> IslandMode.CARD
        }
        AnimatedContent(
            targetState = islandMode,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 2 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it / 2 }) using SizeTransform(clip = false)
            },
            contentAlignment = Alignment.TopStart,
            label = "island",
            modifier = Modifier.align(Alignment.TopStart).wrapContentHeight(Alignment.Top, unbounded = true),
        ) { mode ->
          val activity = shownLive
          if (activity != null && mode != IslandMode.NONE) {
            if (mode == IslandMode.ORB) {
                RestOrb(
                    activity = activity,
                    tint = islandTint,
                    glow = islandGlow,
                    u = u,
                    stageIcon = stageIcon?.bitmap,
                    onTapped = onOrbTapped,
                    modifier = Modifier.padding(start = chromeGutter(), top = u.dp(10)),
                )
            } else {
                val music = orbLevel > 0 && activity.stage is PanelStage.Music
                IslandCard(
                    activity = activity,
                    tint = islandTint,
                    glow = islandGlow,
                    u = u,
                    band = band,
                    compact = compact,
                    onTapped = if (music) onOrbTapped else onLiveAreaTapped,
                    stageIcon = stageIcon?.bitmap,
                    transport = if (music) onOrbTransport else null,
                    expanded = music && orbLevel == 2,
                    holdMs = holdMs,
                    holding = holding,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .wrapContentHeight(Alignment.Top, unbounded = true),
                )
            }
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
                        if (hints.leftRight) StripHint("◀  ▶", u)
                    }
                }
            }
        }

        val endGutter = chromeGutter(end = true)
        val statusSlot: @Composable (Boolean) -> Unit = { date ->
            Row(
                modifier = Modifier.padding(end = endGutter),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(22)),
            ) {
                if (noticeCount > 0) NoticeBell(noticeCount, islandGlow, u, onNoticeCountTapped)

                // kit status corner: battery and time, nothing else
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
            }
        }

        val islandWidth = when {
            live == null -> 0.dp
            orbLevel == 0 && !compact -> u.dp(44) + chromeGutter()
            else -> u.dp(264) + chromeGutter() + 5.dp
        }
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
    }
}

@Composable
private fun IslandCard(
    activity: StripLiveActivity,
    tint: Color,
    glow: Color,
    u: DesignUnits,
    band: Dp,
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
    val shape = RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    val gutter = chromeGutter()
    Box(
        modifier
            .width(u.dp(264) + gutter)
            .heightIn(min = band)
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.62f), lerp(tint, Color.Black, 0.4f).copy(alpha = 0.5f))))
            .drawWithCache {
                val stroke = u.dp(2).toPx()
                val inset = stroke / 2f
                val r = radius.toPx() - inset
                val bottom = size.height - inset
                val right = size.width - inset
                val path = Path().apply {
                    moveTo(inset, 0f)
                    lineTo(inset, bottom - r)
                    arcTo(Rect(inset, bottom - 2 * r, inset + 2 * r, bottom), 180f, -90f, false)
                    lineTo(right - r, bottom)
                    arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
                    lineTo(right, 0f)
                }
                val done = Path()
                progress?.let { p ->
                    val measure = PathMeasure()
                    measure.setPath(path, false)
                    measure.getSegment(0f, measure.length * p, done, true)
                }
                onDrawWithContent {
                    drawContent()
                    drawPath(path, glow.copy(alpha = 0.22f), style = Stroke(stroke))
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
        Column(Modifier.fillMaxWidth().padding(start = gutter, end = u.dp(22), top = if (compact) 2.dp else u.dp(12), bottom = if (expanded) u.dp(12) else 0.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            val tile = if (compact) minOf(u.dp(34), band - 10.dp) else u.dp(34)
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
private fun NoticeBell(count: Int, accent: Color, u: DesignUnits, onTapped: (() -> Unit)?) {
    Row(
        Modifier
            .clip(RoundedCornerShape(u.dp(6)))
            .then(if (onTapped != null) Modifier.clickable(onClick = onTapped) else Modifier)
            .semantics { contentDescription = countLabel(count, "notification") }
            .padding(u.dp(6)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(4)),
    ) {
        Icon(Icons.Outlined.Notifications, null, Modifier.size(maxOf(u.dp(17), with(LocalDensity.current) { LEGIBILITY_FLOOR_PX.toDp() })), tint = Color.White)
        if (count > 0) {
            Box(
                Modifier
                    .heightIn(min = u.dp(16))
                    .widthIn(min = u.dp(16))
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .padding(horizontal = u.dp(5)),
                contentAlignment = Alignment.Center,
            ) {
                Text(count.toString(), color = BadgeInk, fontSize = u.sp(10), fontWeight = FontWeight.Bold, maxLines = 1)
            }
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
            .fillMaxWidth()
            .height(BatteryLineHeight)
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fill)
                .height(BatteryLineHeight)
                .drawWithCache {
                    val base = if (low) LowBatteryTint else Color.White
                    val brush = if (!charging) {
                        SolidColor(base)
                    } else {
                        val glint = size.width * 0.22f
                        val head = travel * (size.width + glint * 2f) - glint
                        Brush.linearGradient(
                            colors = listOf(base.copy(alpha = 0.55f), Color.White, base.copy(alpha = 0.55f)),
                            start = Offset(head, 0f),
                            end = Offset(head + glint, 0f),
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

private val MeterActive   = StripPrimary
private val MeterInactive = Color(0x40EEEEEE)

internal val StripHeight   = StatusStripHeight

private val BadgeInk = Color(0xFF1A0D05)

private val LowBatteryTint = Color(0xFFFF6B6B)

private fun currentTimeString(context: Context): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date())

@Composable
internal fun rememberStripUnits(): DesignUnits {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    return remember(config.screenWidthDp, config.screenHeightDp, density) {
        panelDesignUnits(config.screenWidthDp.toFloat(), config.screenHeightDp.toFloat(), density)
    }
}

internal fun stripBandHeight(u: DesignUnits): Dp = maxOf(StripHeight, u.dp(76))
