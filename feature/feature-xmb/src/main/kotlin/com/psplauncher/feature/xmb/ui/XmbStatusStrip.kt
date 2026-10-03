package com.psplauncher.feature.xmb.ui

import androidx.compose.ui.graphics.ImageBitmap
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
import com.psplauncher.core.domain.model.Category
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPrompt
import com.psplauncher.core.ui.components.LocalPadPrompts
import com.psplauncher.core.ui.components.StatusStripHeight
import com.psplauncher.core.ui.components.chromeGutter
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.mediaAccent
import com.psplauncher.core.ui.icons.CategoryIconGlyph
import com.psplauncher.core.ui.icons.rememberAppIcon
import com.psplauncher.core.ui.theme.LocalPFPColors
import com.psplauncher.core.ui.theme.menuCursorEdge
import com.psplauncher.feature.xmb.R
import com.psplauncher.feature.xmb.viewmodel.PanelStage
import com.psplauncher.feature.xmb.viewmodel.countLabel
import com.psplauncher.feature.xmb.viewmodel.islandProgress
import com.psplauncher.feature.xmb.viewmodel.timeLabel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StripPrimary = Color(0xFFEEEEEE)
private val StripMuted   = Color(0xAAEEEEEE)

object XmbStatusIcons {
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


data class StripLiveActivity(val art: Any?, val title: String, val detail: String?, val stage: PanelStage? = null)

data class StripHints(val shoulder: Boolean = false, val leftRight: Boolean = false)

@Composable
fun XmbPspStatusStrip(
    sortLabel: String? = null,

    showSortButton: Boolean = false,
    onSortTapped: () -> Unit = {},

    live: StripLiveActivity? = null,

    hints: StripHints = StripHints(),

    onLiveAreaTapped: (() -> Unit)? = null,

    noticeCount: Int = 0,
    onNoticeCountTapped: (() -> Unit)? = null,

    sections: List<Category> = emptyList(),
    selectedSection: Int = 0,
    onSectionTapped: (Int) -> Unit = {},

    compact: Boolean = false,

    ambient: Boolean = true,

    modifier: Modifier = Modifier,

    centre: (@Composable BoxScope.(DesignUnits) -> Unit)? = null,
) {
    val context = LocalContext.current
    var batteryLevel   by remember { mutableIntStateOf(0) }
    var isCharging     by remember { mutableStateOf(false) }
    var dateString     by remember { mutableStateOf(currentDateString()) }
    var timeString     by remember { mutableStateOf(currentTimeString(context)) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
                isCharging   = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            dateString = currentDateString()
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
        stage?.let { stageTint(it, stageIcon?.color, fallback) } ?: fallback,
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
                .background(Brush.verticalGradient(0f to StripScrim, 1f to Color.Transparent)),
        )

        live?.let { activity ->
            IslandCard(
                activity = activity,
                tint = islandTint,
                glow = islandGlow,
                u = u,
                band = band,
                compact = compact,
                onTapped = onLiveAreaTapped,
                stageIcon = stageIcon?.bitmap,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 5.dp)
                    .wrapContentHeight(Alignment.Top, unbounded = true),
            )
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(band),
            contentAlignment = Alignment.Center,
        ) {
            when {
                centre != null -> centre.invoke(this, u)
                else -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(u.dp(18)),
                ) {
                    if (sections.isNotEmpty()) {
                        StripSections(
                            labels = sections.map { it.name },
                            selected = selectedSection,
                            onTapped = onSectionTapped,
                            u = u,
                            shoulders = false,
                        ) { i, _, m -> CategoryIconGlyph(sections[i].iconKey, sections[i].name, m) }
                    }
                    sortLabel?.let { label ->
                        Text(
                            "⇅ $label",
                            color = StripPrimary,
                            fontSize = u.sp(10),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(u.dp(9)))
                                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(u.dp(9)))
                                .then(if (showSortButton) Modifier.clickable(onClick = onSortTapped) else Modifier)
                                .padding(horizontal = u.dp(9), vertical = u.dp(4)),
                        )
                    }
                    if (hints.shoulder) StripHint("LB  RB", u)
                    if (hints.leftRight) StripHint("◀  ▶", u)
                }
            }
        }

        val sys = rememberSystemStatus()
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = chromeGutter(end = true))
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(band),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(22)),
        ) {
            NoticeBell(noticeCount, islandGlow, u, onNoticeCountTapped)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
                modifier = Modifier.alpha(0.85f),
            ) {
                if (sys.controllerConnected) {
                    Icon(
                        imageVector        = Icons.Filled.SportsEsports,
                        contentDescription = "Controller connected",
                        tint               = StripPrimary,
                        modifier           = Modifier.size(u.dp(17)),
                    )
                }
                if (sys.bluetoothOn) {
                    StatusIcon(
                        XmbStatusIcons.bluetooth, "Bluetooth",
                        Modifier.size(width = u.dp(11), height = u.dp(15)),
                        tint = StripPrimary,
                        slotKey = "status_bluetooth",
                    )
                }
                sys.wifiLevel?.let { level ->
                    WifiMeter(level, Modifier.size(width = u.dp(18), height = u.dp(15)))
                }
                sys.cellularLevel?.let { level ->
                    SignalBars(level, Modifier.size(width = u.dp(16), height = u.dp(15)))
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
                Text(
                    text       = timeString,
                    color      = StripPrimary,
                    fontSize   = u.sp(18),
                    lineHeight = u.sp(18),
                    fontWeight = FontWeight.Light,
                    maxLines   = 1,
                )
                if (!compact) {
                    Text(
                        text       = dateString,
                        color      = StripPrimary.copy(alpha = 0.6f),
                        fontSize   = u.sp(10),
                        fontWeight = FontWeight.Light,
                        maxLines   = 1,
                    )
                }
            }
        }

        BatteryLine(
            level = batteryLevel,
            charging = isCharging,
            glint = ambient,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
) {
    val stage = activity.stage
    val music = stage as? PanelStage.Music
    val positionMs = music?.livePositionMs() ?: 0L
    val progress = stage?.islandProgress(positionMs)
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
            .then(if (onTapped != null) Modifier.clickable(onClick = onTapped) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = gutter, end = u.dp(22), top = if (compact) 2.dp else u.dp(12)),
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
            if (playing) EqualizerGlyph(u)
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
        Icon(Icons.Outlined.Notifications, null, Modifier.size(u.dp(17)), tint = Color.White)
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
    val accent = mediaAccent(LocalPFPColors.current.accentColor.toArgb().toLong())
    val pad = shoulders && LocalPadPrompts.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = u.dp(22), spacing = 0.dp)
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val tint = if (on) accent else Color.White.copy(alpha = 0.5f)
            Row(
                Modifier
                    .clip(RoundedCornerShape(u.dp(8)))
                    .clickable { onTapped(i) }
                    .padding(horizontal = u.dp(6), vertical = u.dp(8)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(8)),
            ) {
                icon?.invoke(i, tint, Modifier.size(u.dp(if (on) 20 else 18)).alpha(if (on) 1f else 0.5f))
                if (sectionLabelShown(on, icon != null)) {
                    Text(
                        label,
                        color = tint,
                        fontSize = u.sp(14),
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
                        maxLines = 1,
                    )
                }
            }
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(22), spacing = 0.dp)
    }
}

internal fun sectionLabelShown(active: Boolean, hasIcon: Boolean): Boolean = active || !hasIcon

@Composable
private fun BatteryLine(level: Int, charging: Boolean, glint: Boolean, modifier: Modifier = Modifier) {
    val fill = (level / 100f).coerceIn(0f, 1f)
    val travel by androidx.compose.runtime.produceState(0f, charging, glint) {
        if (!charging || !glint) return@produceState
        while (true) {
            androidx.compose.animation.core.withInfiniteAnimationFrameMillis { nowMs ->
                value = (com.psplauncher.core.ui.wave.steppedFrameMs(nowMs) % GLINT_PERIOD_MS) / GLINT_PERIOD_MS.toFloat()
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

private val BatteryLineHeight = 2.dp

private const val GLINT_PERIOD_MS = 2400L

private val MeterActive   = StripPrimary
private val MeterInactive = Color(0x40EEEEEE)

@Composable
private fun SignalBars(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val bars = 4
        val gap = size.width * 0.14f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val barHeight = size.height * (0.35f + 0.65f * (i + 1) / bars)
            val x = i * (barWidth + gap)
            val top = size.height - barHeight
            drawRect(
                color = if (i < level) MeterActive else MeterInactive,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
private fun WifiMeter(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.92f
        val maxR = size.height * 0.9f
        val stroke = size.height * 0.11f

        fun color(threshold: Int) = if (level >= threshold) MeterActive else MeterInactive

        drawCircle(color = color(1), radius = stroke * 1.1f, center = Offset(cx, cy))

        for (i in 1..3) {
            val r = maxR * i / 3f
            drawArc(
                color = color(i + 1),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke),
            )
        }
    }
}

@Composable
private fun StatusIcon(
    @DrawableRes res: Int,
    description: String,
    modifier: Modifier = Modifier,
    tint: Color = StripMuted,

    slotKey: String? = null,
) {
    val override = slotKey?.let { key ->
        com.psplauncher.core.ui.icons.LocalCustomIcons.current[key]
            ?: com.psplauncher.core.ui.icons.LocalXmbIconOverrides.current[key]
    }
    if (override != null) {
        com.psplauncher.core.ui.icons.CustomIconSurface(
            icon = override,
            contentDescription = description,
            modifier = modifier,
        )
        return
    }
    Image(
        painter            = painterResource(res),
        contentDescription = description,
        colorFilter        = ColorFilter.tint(tint),
        modifier           = modifier,
    )
}

internal val StripHeight   = StatusStripHeight

private val StripScrim = Color(0xCC04060C)
private val BadgeInk = Color(0xFF1A0D05)

private val LowBatteryTint = Color(0xFFFF6B6B)

private fun currentTimeString(context: Context): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date())

private fun currentDateString(): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEMMMd"), locale).format(Date())
}

@Composable
internal fun rememberStripUnits(): DesignUnits {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    return remember(config.screenWidthDp, config.screenHeightDp, density) {
        DesignUnits(minOf(config.screenWidthDp / PANEL_DESIGN_WIDTH, config.screenHeightDp / PANEL_DESIGN_HEIGHT), density)
    }
}

internal fun stripBandHeight(u: DesignUnits): Dp = maxOf(StripHeight, u.dp(76))
