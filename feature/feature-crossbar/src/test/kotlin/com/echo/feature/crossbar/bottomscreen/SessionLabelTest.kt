package com.echo.feature.crossbar.bottomscreen

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: the companion shows how long the game has been running
class SessionLabelTest {
    private val start = 1_000_000_000L
    private fun at(minutes: Long) = start + minutes * 60_000L

    @Test
    fun `the clock reads minutes, then hours, and never a negative time`() {
        assertEquals("NOW PLAYING · JUST STARTED", sessionLabel(start, at(0)))
        assertEquals("NOW PLAYING · 12 MIN", sessionLabel(start, at(12)))
        assertEquals("NOW PLAYING · 1 H 5 MIN", sessionLabel(start, at(65)))
        assertEquals("a clock behind the launch time", "NOW PLAYING · JUST STARTED", sessionLabel(start, start - 120_000))
        assertEquals("no launch time: no clock", "NOW PLAYING", sessionLabel(null, at(3)))
    }
}
