package com.echo.core.ui.wave

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-05: the Quick Settings wave tile steps through every style, Off included, and comes back
class WaveStyleCycleTest {
    @Test
    fun `the tile visits each style once and returns to the first`() {
        val seen = generateSequence(WaveStyle.ANIMATED) { it.next }.take(WaveStyle.entries.size + 1).toList()
        assertEquals(WaveStyle.entries + WaveStyle.ANIMATED, seen)
    }
}
