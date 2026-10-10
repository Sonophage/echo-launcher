package com.echo.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
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

// owner, 2026-10-05: Recent and the game lists are short, and asked for the rail too
const val LETTER_JUMP_MIN_ITEMS = 10

const val LETTER_JUMP_MIN_LETTERS = 3

fun initialOf(title: String): Char {
    val c = title.trimStart().firstOrNull() ?: return '#'
    return if (c.isLetter()) c.uppercaseChar() else '#'
}

// one rung per initial, A to Z, each pointing at the first row with that initial in the list's own order.
// In a list sorted by title that is the letter's block; in one sorted by date it is that letter's most recent
// row (owner, 2026-10-05: Recent and the game lists sorted by play date get the rail too)
fun letterAnchors(titles: List<String>): List<LetterAnchor>? {
    if (titles.size < LETTER_JUMP_MIN_ITEMS) return null

    val first = LinkedHashMap<Char, Int>()
    titles.forEachIndexed { index, title -> first.getOrPut(initialOf(title)) { index } }
    val anchors = first.entries.sortedBy { it.key }.map { (letter, index) -> LetterAnchor(letter, index) }
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

    // the rung of the row the cursor is on
    val here = titles.getOrNull(currentIndex)?.let(::initialOf)
    val cursor = anchors.indexOfFirst { it.letter == here }.coerceAtLeast(0)
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

// the item a finger at [along] picks: the rung under it, and within a rung that stands for several items (an
// initial's artists) its place down the rung picks among them, so every item is reachable and the glyph under the
// finger is always the chosen item's
fun railItemAt(along: Float, extent: Float, spanPx: Float, pitchPx: Float, drawn: List<Int>, count: Int): Int {
    val rung = rungAt(along, extent, spanPx, pitchPx, drawn.size)
    val start = drawn[rung]
    val end = drawn.getOrNull(rung + 1) ?: count
    val lead = (extent - spanPx) / 2f
    val within = ((along - lead - rung * pitchPx) / pitchPx).coerceIn(0f, 0.999f)
    return (start + (within * (end - start)).toInt()).coerceIn(start, (end - 1).coerceAtLeast(start))
}

// the glyph each drawn rung shows. On a screen too short for every initial a rung stands for several, so the chosen
// rung shows the chosen item's own glyph: J for Jhené Aiko, not the I its rung starts at (seen on the Konker)
fun railGlyphs(letters: List<RailRung>, rungs: List<Int>, cursor: Int?): List<String> {
    val active = cursor?.let { c -> rungs.indexOfLast { it <= c }.coerceAtLeast(0) }
    return rungs.mapIndexed { rung, item -> letters[if (rung == active) cursor!! else item].glyph }
}

// the first item of each run of one glyph: one rung per initial, as the letter rail has
fun glyphRuns(rungs: List<RailRung>): List<Int> =
    rungs.indices.filter { it == 0 || rungs[it].glyph != rungs[it - 1].glyph }

// one rung: [glyph] is drawn on the rail, a letter or a group's initial; [name] is shown beside the chosen rung when
// it says more than the glyph (a system, a genre, an artist)
data class RailRung(val glyph: String, val name: String = glyph)

fun letterRungs(letters: List<Char>): List<RailRung> = letters.map { RailRung(it.toString()) }

fun letterMenuFor(titles: List<String>): List<Char> {
    if (titles.size < LETTER_JUMP_MIN_ITEMS) return emptyList()
    val letters = titles.map(::initialOf).distinct().sorted()
    return if (letters.size < LETTER_JUMP_MIN_LETTERS) emptyList() else letters
}

// owner, 2026-10-09: one side rail for letters and for groups. The crossbar's jumps to a letter; the App Drawer's
// filters by a letter, or by a system, genre, artist or album when its section has groups
@Composable
fun SideRail(
    letters: List<RailRung>,
    cursor: Int?,
    onTouch: (Int) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    top: Dp = StatusStripHeight,
    bottom: Dp = HintBarHeight,
) {
    if (letters.isEmpty()) return

    BoxWithConstraints(modifier.fillMaxSize()) {
        // owner, 2026-10-05: the rail sits on the screen's centre line, clear of the header and the footer: the
        // same margin above and below, with room for the chosen letter to grow at either end
        val edge = maxOf(top, bottom)
        val railHeight = maxHeight
        val runs = remember(letters) { glyphRuns(letters) }
        val firstPass = railMetrics(maxHeight - edge * 2, runs.size)
        val growth = firstPass.badge * (ACTIVE_SCALE - 1f) / 2f
        val metrics = railMetrics(maxHeight - (edge + growth) * 2, runs.size)
        val rungs = remember(runs, metrics.rungs) { bucketIndices(runs.size, metrics.rungs).map { runs[it] } }

        if (cursor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .menuBackdrop(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterVertically),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = edge + growth, bottom = edge + growth, end = RailEdgeGap)
                        .width(metrics.badge * ACTIVE_SCALE),
                ) {
                    Rungs(letters, rungs, cursor, metrics)
                }
                // the chosen group's full name, beside its rung
                cursor?.let { letters.getOrNull(it) }?.takeIf { it.name != it.glyph }?.let { rung ->
                    val active = rungs.indexOfLast { it <= cursor }.coerceAtLeast(0)
                    val span = metrics.badge * rungs.size + metrics.gap * (rungs.size - 1)
                    val lead = (railHeight - span) / 2
                    RailName(
                        rung.name,
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = RailEdgeGap + metrics.badge * ACTIVE_SCALE + RailEdgeGap)
                            .offset { androidx.compose.ui.unit.IntOffset(0, (lead + metrics.pitch * active + metrics.badge / 2).roundToPx()) }
                            .graphicsLayer { translationY = -size.height / 2 },
                    )
                }
            }
        }

        // owner, 2026-10-09: while the rail waits, a soft glow on the screen's edge at its middle, slowly pulsing,
        // shows it is there
        if (cursor == null) RailCue(Modifier.align(Alignment.CenterEnd).width(RailEdgeZone).height(railHeight * RAIL_CUE_SHARE))
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(RailEdgeZone)
                .padding(top = edge + growth, bottom = edge + growth)
                .railSlide(rungs, letters.size, metrics, onTouch = onTouch, onReleased = onReleased),
        )
    }
}

// the rail's cue: a half-glow hugging the right edge, breathing between faint and soft
@Composable
private fun RailCue(modifier: Modifier) {
    val breath by androidx.compose.animation.core.rememberInfiniteTransition(label = "railCue").animateFloat(
        initialValue = RAIL_CUE_MIN_ALPHA,
        targetValue = RAIL_CUE_MAX_ALPHA,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            tween(RAIL_CUE_BREATH_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "railCueBreath",
    )
    Box(modifier.drawBehind {
        val reach = size.height / 2
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(Color.White.copy(alpha = breath), Color.Transparent),
                center = Offset(size.width, size.height / 2), radius = reach,
            ),
            radius = reach, center = Offset(size.width, size.height / 2),
        )
    })
}

private const val RAIL_CUE_SHARE = 0.32f
private const val RAIL_CUE_MIN_ALPHA = 0.07f
private const val RAIL_CUE_MAX_ALPHA = 0.24f
private const val RAIL_CUE_BREATH_MS = 2200

@Composable
private fun RailName(name: String, modifier: Modifier) {
    Text(
        name,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun Rungs(
    letters: List<RailRung>,
    rungs: List<Int>,
    cursor: Int?,
    metrics: RailMetrics,
) {
    val activeRung = cursor?.let { c -> rungs.indexOfLast { it <= c }.coerceAtLeast(0) }
    val glyphs = railGlyphs(letters, rungs, cursor)
    rungs.forEachIndexed { rung, _ ->
        val wave = activeRung?.let { railWave(rung - it) } ?: 0f
        RailLetter(
            glyph = glyphs[rung],
            wave = wave,
            metrics = metrics,
            modifier = Modifier.zIndex(wave),
        )
    }
}

@Composable
private fun RailLetter(
    glyph: String,
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
            },
    ) {
        Text(
            text = glyph,
            color = Color.White,
            fontSize = metrics.glyph,
            fontWeight = if (wave >= 1f) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun Modifier.railSlide(
    rungs: List<Int>,
    count: Int,
    metrics: RailMetrics,
    onTouch: (Int) -> Unit,
    onReleased: () -> Unit,
): Modifier = pointerInput(rungs, count, metrics) {
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
                report(change.position, geometry, rungs, count, onTouch)
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
    count: Int,
    onTouch: (Int) -> Unit,
) {
    onTouch(railItemAt(at.y, size.height.toFloat(), geometry.spanPx, geometry.pitchPx, rungs, count))
}
