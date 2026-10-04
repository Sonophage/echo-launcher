package com.echo.core.ui.wave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveClockTest {
    private fun run(vsyncs: List<Long>, speedAt: (Long) -> Float, frameMs: Long = WAVE_FRAME_MS): List<Float> {
        var t = 0f
        var last = -1L
        return vsyncs.map { now ->
            val stepped = steppedFrameMs(now, frameMs)
            t = advanceWaveClock(t, last, stepped, speedAt(now))
            last = stepped
            t
        }
    }

    @Test
    fun `the clock only advances once per wave frame, so vsyncs in between redraw nothing`() {
        val times = run((0L..WAVE_FRAME_MS).toList(), { 1f })
        (1 until WAVE_FRAME_MS.toInt()).forEach { ms ->
            assertEquals("a vsync ${ms}ms in must not move the wave", times[0], times[ms], 0f)
        }
        assertEquals(WAVE_FRAME_MS / 1000f, times.last(), 1e-6f)
    }

    @Test
    fun `the wave frame rate is 30 at full speed and 20 once idle has slowed it`() {
        assertEquals(1000L / 30, waveFrameMs(1f))
        assertEquals(1000L / 20, waveFrameMs(0.45f))
    }

    @Test
    fun `stepping does not change the wave's speed, only how often it is sampled`() {
        val oneMinute = (0L..60_000L step 16).toList()
        assertEquals(60f * 0.5f, run(oneMinute, { 0.5f }).last(), 0.05f)
    }

    @Test
    fun `a speed tween bends the clock but never sends it back to the start`() {
        val vsyncs = (0L..3_000L step 16).toList()
        val times = run(vsyncs, { now -> if (now < 1_000L) 1f else 1f + (now - 1_000L) / 900f })
        times.zipWithNext().forEach { (a, b) -> assertTrue("the wave time went backwards: $a to $b", b >= a) }
        assertTrue("a faster wave must cover more time than a steady one", times.last() > 3f)
    }

    @Test
    fun `clocks started at different moments still step on the same vsyncs`() {
        val boundaries = (0L..1000L).filter { steppedFrameMs(it) != steppedFrameMs(it - 1) }
        assertEquals("every step lands on a multiple of the ambient frame", boundaries.map { it % AMBIENT_FRAME_MS }.toSet(), setOf(0L))
        assertEquals(steppedFrameMs(1_000_007L), steppedFrameMs(1_000_020L))
    }
}
