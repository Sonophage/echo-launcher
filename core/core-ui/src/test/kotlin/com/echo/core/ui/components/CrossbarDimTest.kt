package com.echo.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossbarDimTest {
    @Test
    fun `the ramp is the running prototype's, scaled for the panel`() {
        assertEquals(1.00f, CrossbarDim.ranked(0), 1e-6f)
        assertEquals(0.85f * CrossbarDim.PanelScale, CrossbarDim.ranked(1), 1e-6f)
        assertEquals(0.55f * CrossbarDim.PanelScale, CrossbarDim.ranked(2), 1e-6f)
        assertEquals(0.30f * CrossbarDim.PanelScale, CrossbarDim.ranked(3), 1e-6f)
    }

    @Test
    fun `the scale actually dims, and never to nothing`() {
        assertTrue("the panel scale must dim something", CrossbarDim.PanelScale < 1f)
        assertTrue("the panel scale must not erase the far stops", CrossbarDim.ranked(CrossbarDim.LastStep) >= 0.1f)
    }

    @Test
    fun `the cursor's stop was not scaled with the rest`() {
        assertEquals("the selection must not be dimmed at all", 1f, CrossbarDim.ranked(0), 1e-6f)
    }

    @Test
    fun `the stops kept their proportions`() {
        val nearToMid = CrossbarDim.ranked(1) / CrossbarDim.ranked(2)
        val midToFar = CrossbarDim.ranked(2) / CrossbarDim.ranked(3)
        assertEquals("the published ramp's near:mid ratio", 0.85f / 0.55f, nearToMid, 1e-4f)
        assertEquals("the published ramp's mid:far ratio", 0.55f / 0.30f, midToFar, 1e-4f)
    }

    @Test
    fun `further is never brighter`() {
        var previous = Float.MAX_VALUE
        for (d in 0..12) {
            val alpha = CrossbarDim.ranked(d)
            assertTrue("distance $d is brighter than ${d - 1}: $alpha after $previous", alpha <= previous)
            previous = alpha
        }
    }

    @Test
    fun `distances past the end of the ramp stay visible`() {
        for (d in CrossbarDim.LastStep..64) {
            assertEquals(
                "distance $d must rest on the last stop rather than fading on",
                CrossbarDim.ranked(CrossbarDim.LastStep),
                CrossbarDim.ranked(d),
                1e-6f,
            )
        }
        assertTrue("the last stop must still be visible", CrossbarDim.ranked(CrossbarDim.LastStep) > 0f)
    }

    @Test
    fun `a negative distance is treated as the cursor`() {
        assertEquals(CrossbarDim.ranked(0), CrossbarDim.ranked(-5), 1e-6f)
    }
}
