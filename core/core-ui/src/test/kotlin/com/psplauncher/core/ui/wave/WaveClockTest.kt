package com.psplauncher.core.ui.wave

import org.junit.Assert.assertEquals
import org.junit.Test

class WaveClockTest {
    @Test
    fun `the clock only advances once per wave frame, so vsyncs in between redraw nothing`() {
        val atStart = waveClockSeconds(0L, 1f, WAVE_FRAME_MS)
        (1 until WAVE_FRAME_MS).forEach { ms ->
            assertEquals("a vsync ${ms}ms in must not move the wave", atStart, waveClockSeconds(ms, 1f, WAVE_FRAME_MS), 0f)
        }
        assertEquals(WAVE_FRAME_MS / 1000f, waveClockSeconds(WAVE_FRAME_MS, 1f, WAVE_FRAME_MS), 1e-6f)
    }

    @Test
    fun `the wave frame rate is 30 at full speed and 20 once idle has slowed it`() {
        assertEquals(1000L / 30, waveFrameMs(1f))
        assertEquals(1000L / 20, waveFrameMs(0.45f))
    }

    @Test
    fun `stepping does not change the wave's speed, only how often it is sampled`() {
        val oneMinute = 60_000L
        assertEquals(60f * 0.5f, waveClockSeconds(oneMinute - oneMinute % WAVE_FRAME_MS, 0.5f, WAVE_FRAME_MS), 0.05f)
    }

    @Test
    fun `clocks started at different moments still step on the same vsyncs`() {
        val boundaries = (0L..1000L).filter { steppedFrameMs(it) != steppedFrameMs(it - 1) }
        assertEquals("every step lands on a multiple of the ambient frame", boundaries.map { it % AMBIENT_FRAME_MS }.toSet(), setOf(0L))
        assertEquals(steppedFrameMs(1_000_007L), steppedFrameMs(1_000_020L))
    }
}
