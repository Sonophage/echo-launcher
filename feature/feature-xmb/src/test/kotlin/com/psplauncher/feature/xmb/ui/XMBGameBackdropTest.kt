package com.psplauncher.feature.xmb.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XMBGameBackdropTest {
    private val stops = xmbStillOverVideoStops()

    @Test
    fun `the left edge is fully solid, so the crossbar sits on artwork`() {
        assertEquals(0f, stops.first().first, 0f)
        assertEquals(
            "the category bar, the item list and the game's name all live here",
            1f,
            stops.first().second.alpha,
            0f,
        )
    }

    @Test
    fun `the right edge is fully clear, so the clip is actually visible`() {
        assertEquals(1f, stops.last().first, 0f)
        assertEquals("a mask that never clears hides the snap entirely", 0f, stops.last().second.alpha, 0f)
    }

    @Test
    fun `the transition happens around the middle of the screen, not at an edge`() {
        assertTrue("solid band ends at $XMB_STILL_SOLID_END", XMB_STILL_SOLID_END in 0.25f..0.55f)
        assertTrue("fade ends at $XMB_STILL_FADE_END", XMB_STILL_FADE_END in 0.55f..0.85f)
        assertTrue("the fade must have width", XMB_STILL_FADE_END > XMB_STILL_SOLID_END)
    }

    @Test
    fun `the mask only ever gets clearer, left to right`() {
        val alphas = stops.map { it.second.alpha }
        assertEquals(
            "an alpha that rises again would put a second band of artwork over the clip",
            alphas.sortedDescending(),
            alphas,
        )
        val positions = stops.map { it.first }
        assertEquals("gradient positions must ascend", positions.sorted(), positions)
    }

    @Test
    fun `the mask is greyscale, so it tints nothing`() {
        stops.forEach { (at, color) ->
            assertTrue("stop at $at is not black or transparent", color == Color.Black || color == Color.Transparent)
        }
    }

    @Test
    fun `the drift never pulls an edge of the art into view`() {
        for (seed in 0..7) {
            for (ms in 0L..KEN_BURNS_MS step 500L) {
                val f = kenBurnsFrame(ms, seed)
                val overflow = (f.scale - 1f) / 2f
                assertTrue("seed $seed at $ms ms shows the left or right edge", kotlin.math.abs(f.dxFraction) <= overflow + 1e-6f)
                assertTrue("seed $seed at $ms ms shows the top or bottom edge", kotlin.math.abs(f.dyFraction) <= overflow + 1e-6f)
                assertTrue("seed $seed at $ms ms shrinks the art below the screen", f.scale >= 1f)
            }
        }
    }

    @Test
    fun `the drift stops after one pass, so a resting backdrop costs no frames`() {
        for (seed in 0..7) {
            assertEquals("seed $seed still moving after the pass", kenBurnsFrame(KEN_BURNS_MS, seed), kenBurnsFrame(KEN_BURNS_MS * 3, seed))
        }
    }

    @Test
    fun `the drift starts from where the crossfade left the art`() {
        for (seed in 0..7) {
            val f = kenBurnsFrame(0L, seed)
            assertEquals("seed $seed jumps sideways on arrival", 0f, f.dxFraction, 0f)
            assertEquals("seed $seed jumps vertically on arrival", 0f, f.dyFraction, 0f)
        }
    }

    @Test
    fun `the drift actually moves, both in and out`() {
        val scales = (0..7).map { kenBurnsFrame(KEN_BURNS_MS, it).scale - kenBurnsFrame(0L, it).scale }
        assertTrue("no seed zooms in: $scales", scales.any { it > 0.05f })
        assertTrue("no seed zooms out: $scales", scales.any { it < -0.05f })
    }

    @Test
    fun `the crossfade is slow enough to read as a fade, not a blink`() {
        assertTrue("$XMB_BACKDROP_FADE_MS ms", XMB_BACKDROP_FADE_MS in 400..1200)
    }
}
