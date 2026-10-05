package com.echo.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.echo.core.ui.design.menuBackdrop
import kotlin.math.abs

data class LetterAnchor(val letter: Char, val index: Int)

const val LETTER_JUMP_MIN_ITEMS = 25

const val LETTER_JUMP_MIN_LETTERS = 3

fun initialOf(title: String): Char {
    val c = title.trimStart().firstOrNull() ?: return '#'
    return if (c.isLetter()) c.uppercaseChar() else '#'
}

fun letterAnchors(titles: List<String>): List<LetterAnchor>? {
    if (titles.size < LETTER_JUMP_MIN_ITEMS) return null

    val anchors = ArrayList<LetterAnchor>()
    var previous: Char? = null
    for ((index, title) in titles.withIndex()) {
        val letter = initialOf(title)
        if (previous != null && letter < previous) return null
        if (letter != previous) anchors.add(LetterAnchor(letter, index))
        previous = letter
    }
    return if (anchors.size < LETTER_JUMP_MIN_LETTERS) null else anchors
}

data class LetterJumpState(
    val anchors: List<LetterAnchor>,
    val cursor: Int = 0,

    val returnIndex: Int = 0,
) {
    val letter: Char get() = anchors[cursor].letter
    val targetIndex: Int get() = anchors[cursor].index
}

fun letterJumpFor(titles: List<String>, currentIndex: Int): LetterJumpState? {
    val anchors = letterAnchors(titles) ?: return null

    val cursor = anchors.indexOfLast { it.index <= currentIndex }.coerceAtLeast(0)
    return LetterJumpState(anchors = anchors, cursor = cursor, returnIndex = currentIndex)
}

fun LetterJumpState.move(delta: Int): LetterJumpState {
    val next = (cursor + delta).coerceIn(0, anchors.lastIndex)
    return if (next == cursor) this else copy(cursor = next)
}

fun LetterJumpState.at(rung: Int): LetterJumpState {
    val next = rung.coerceIn(0, anchors.lastIndex)
    return if (next == cursor) this else copy(cursor = next)
}

data class RailMetrics(
    val rungs: Int,
    val badge: Dp,
    val gap: Dp,
    val glyph: TextUnit,
) {
    val pitch: Dp get() = badge + gap
}

internal val RailMinBadge = 16.dp
internal val RailMaxBadge = RailIcon
private val RungGap = 3.dp
private val BadgeSideGap = 4.dp
private const val GlyphRatio = 0.6f

// owner, 2026-10-05: the rail reads like an XMB column: bare letters, the chosen one biggest, its neighbours
// swelling in a short wave, the rest dimmed
private const val ACTIVE_SCALE = 1.7f
private const val INACTIVE_ALPHA = 0.4f
private const val WAVE_REACH = 3

// 1 at the chosen rung, easing to 0 at WAVE_REACH rungs away
internal fun railWave(distance: Int): Float {
    val d = abs(distance).toFloat() / WAVE_REACH
    return if (d >= 1f) 0f else (0.5f + 0.5f * kotlin.math.cos(Math.PI * d)).toFloat()
}

internal val RailEdgeZone = RailIcon + RailEdgeGap * 2

fun railMetrics(available: Dp, anchorCount: Int): RailMetrics {
    val usable = available.coerceAtLeast(RailMinBadge)
    val minPitch = RailMinBadge + RungGap
    val maxRungs = (usable / minPitch).toInt().coerceAtLeast(1)
    val rungs = anchorCount.coerceIn(1, maxRungs)
    val share = usable / rungs
    val badge = (share - RungGap).coerceIn(RailMinBadge, RailMaxBadge)
    val gap = (share - badge).coerceIn(RungGap, RailRowGap)
    return RailMetrics(
        rungs = rungs,
        badge = badge,
        gap = gap,
        glyph = (badge.value * GlyphRatio).sp,
    )
}

fun bucketIndices(anchorCount: Int, rungs: Int): List<Int> =
    if (anchorCount <= rungs) List(anchorCount) { it }
    else List(rungs) { (it * anchorCount.toFloat() / rungs).toInt() }

fun rungAt(along: Float, extent: Float, spanPx: Float, pitchPx: Float, rungCount: Int): Int {
    val lead = (extent - spanPx) / 2f
    return ((along - lead) / pitchPx).toInt().coerceIn(0, rungCount - 1)
}

fun letterMenuFor(titles: List<String>): List<Char> {
    if (titles.size < LETTER_JUMP_MIN_ITEMS) return emptyList()
    val letters = titles.map(::initialOf).distinct().sorted()
    return if (letters.size < LETTER_JUMP_MIN_LETTERS) emptyList() else letters
}

@Composable
fun CrossbarLetterRail(
    letters: List<Char>,
    cursor: Int?,
    onTouch: (Int) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    top: Dp = StatusStripHeight,
    bottom: Dp = HintBarHeight,
) {
    if (letters.isEmpty()) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        val metrics = railMetrics(maxHeight - top - bottom, letters.size)
        val rungs = remember(letters.size, metrics.rungs) { bucketIndices(letters.size, metrics.rungs) }

        if (cursor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .menuBackdrop(),
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterVertically),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = top, bottom = bottom, end = RailEdgeGap),
                ) {
                    Rungs(letters, rungs, cursor, metrics)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(RailEdgeZone)
                .padding(top = top, bottom = bottom)
                .railSlide(rungs, metrics, onTouch = onTouch, onReleased = onReleased),
        )
    }
}

@Composable
private fun Rungs(
    letters: List<Char>,
    rungs: List<Int>,
    cursor: Int?,
    metrics: RailMetrics,
) {
    val activeRung = cursor?.let { c -> rungs.indexOfLast { it <= c }.coerceAtLeast(0) }
    rungs.forEachIndexed { rung, letter ->
        val wave = activeRung?.let { railWave(rung - it) } ?: 0f
        RailLetter(
            letter = letters[letter],
            wave = wave,
            metrics = metrics,
            modifier = Modifier.zIndex(wave),
        )
    }
}

@Composable
private fun RailLetter(
    letter: Char,
    wave: Float,
    metrics: RailMetrics,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(1f + (ACTIVE_SCALE - 1f) * wave, tween(140), label = "railLetterScale")
    val alpha by animateFloatAsState(INACTIVE_ALPHA + (1f - INACTIVE_ALPHA) * wave, tween(140), label = "railLetterAlpha")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(metrics.badge)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                // the letters grow towards the screen, away from the edge
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
            },
    ) {
        Text(
            text = letter.toString(),
            color = Color.White,
            fontSize = metrics.glyph,
            fontWeight = if (wave >= 1f) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun Modifier.railSlide(
    rungs: List<Int>,
    metrics: RailMetrics,
    onTouch: (Int) -> Unit,
    onReleased: () -> Unit,
): Modifier = pointerInput(rungs, metrics) {
    val geometry = geometryOf(metrics)
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var sliding = false
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (!sliding) {
                val dy = change.position.y - down.position.y
                val dx = change.position.x - down.position.x
                sliding = abs(dy) > slop && abs(dy) > abs(dx)
            }
            if (sliding) {
                report(change.position, geometry, rungs, onTouch)
                change.consume()
            }
        }
        if (sliding) onReleased()
    }
}

private data class RailGeometry(val spanPx: Float, val pitchPx: Float)

private fun Density.geometryOf(metrics: RailMetrics) = RailGeometry(
    spanPx = with(metrics) { badge.toPx() * rungs + gap.toPx() * (rungs - 1) },
    pitchPx = metrics.pitch.toPx(),
)

private fun PointerInputScope.report(
    at: Offset,
    geometry: RailGeometry,
    rungs: List<Int>,
    onTouch: (Int) -> Unit,
) {
    onTouch(rungs[rungAt(at.y, size.height.toFloat(), geometry.spanPx, geometry.pitchPx, rungs.size)])
}
