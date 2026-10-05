package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-05: the boot is "B4 Horizon Ripple" from the ECHO Animations v2 spec; these pin its key frames
class BootRippleTest {
    private val w = 821f
    private val h = 462f

    @Test
    fun `it opens on a line of light, before any of the mark`() {
        val f = bootRippleFrame(500f, w, h)
        assertEquals(.7f * w, f.lineWidth, .5f)
        assertEquals(0f, f.mark.dotAlpha, .001f)
        assertEquals(0f, f.mark.ringAlpha, .001f)
        assertEquals(0f, f.mark.crescentAlpha, .001f)
    }

    @Test
    fun `by the hold the whole mark is in place and the word is up`() {
        val f = bootRippleFrame(2600f, w, h)
        assertEquals(0f, f.lineWidth, .001f)
        assertEquals(1f, f.mark.ringAlpha, .001f)
        assertEquals(1f, f.mark.crescentAlpha, .001f)
        assertEquals(0f, f.mark.lx, .001f)
        assertEquals(0f, f.mark.dotY, .001f)
        assertEquals(.6f, f.wordAlpha, .001f)
        assertEquals(4f, f.wordSpacing, .001f)
        assertEquals(1f, f.layerAlpha, .001f)
    }

    @Test
    fun `it ends faded out at 3500 ms`() {
        assertEquals(0f, bootRippleFrame(3500f, w, h).layerAlpha, .001f)
        assertEquals(3500f, BootRipple.endMs(null), .001f)
    }

    @Test
    fun `a skip freezes the parts at the pressed frame and fades out over 300 ms`() {
        val pressed = bootRippleFrame(1200f, w, h)
        val during = bootRippleFrame(1350f, w, h, skipAt = 1200f)
        assertEquals("parts do not keep moving after the press", pressed.mark.ringAlpha, during.mark.ringAlpha, .001f)
        assertTrue(during.layerAlpha in .01f..0.99f)
        assertEquals(0f, bootRippleFrame(1500f, w, h, skipAt = 1200f).layerAlpha, .001f)
        assertEquals(1500f, BootRipple.endMs(1200f), .001f)
    }

    // owner, 2026-10-05: the boot's ripple is the wave the user picked, rising when the rings would leave
    @Test
    fun `the chosen wave rises with the ripple and is fully up by the hold`() {
        assertEquals(0f, bootRippleFrame(800f, w, h).waveAlpha, .001f)
        assertTrue(bootRippleFrame(1200f, w, h).waveAlpha in .01f..0.99f)
        assertEquals(1f, bootRippleFrame(2600f, w, h).waveAlpha, .001f)
    }
}
