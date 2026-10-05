package com.echo.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

// owner, 2026-10-05: the second launch style, "L2 Lens" from the ECHO Animations v2 spec; these pin its key frames
class LensCeremonyTest {
    private val w = 821f
    private val h = 462f

    @Test
    fun `the disc turns exactly two times, so the art comes to rest upright`() {
        assertEquals(0f, LensCeremony.spinAngle(700f), .001f)
        assertEquals(720f, LensCeremony.spinAngle(2700f), .01f)
        assertEquals(720f, LensCeremony.spinAngle(4000f), .01f)
        assertTrue("it keeps turning one way", LensCeremony.spinAngle(2000f) in 0f..720f)
    }

    @Test
    fun `the ring has drawn on and the art spins inside it before the sound`() {
        val f = lensFrame(2000f, w, h)
        assertEquals(0f, f.mark.ringOffset, .001f)
        assertEquals(1f, f.artAlpha, .001f)
        assertEquals(1f, f.discAlpha, .001f)
        assertEquals(0f, f.cardAlpha, .001f)
    }

    @Test
    fun `at the hand-off the lens has opened over the whole screen and the disc is gone`() {
        val f = lensFrame(LensCeremony.HAND_OFF_MS.toFloat(), w, h)
        assertTrue(f.artRadius >= hypot(w / 2, h / 2))
        assertEquals(0f, f.discAlpha, .001f)
        assertEquals(1f, f.artScale, .001f)
    }

    @Test
    fun `while Android loads, the title is up and only the dot pulses`() {
        val a = lensFrame(4400f, w, h)
        val b = lensFrame(5000f, w, h)
        assertEquals(1f, a.titleAlpha, .001f)
        assertEquals(.12f * h, a.mark.size, .01f)
        assertTrue("the dot pulses", a.mark.dotAlpha != b.mark.dotAlpha)
        assertTrue(LensCeremony.SOUND_MS < LensCeremony.HAND_OFF_MS)
    }
}
