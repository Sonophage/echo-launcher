package com.echo.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossbarDimSmoothTest {
    @Test
    fun `the cursor is never dimmed`() {
        assertEquals(1f, CrossbarDim.smoothed(distance = 0, span = 8), 0f)
    }

    @Test
    fun `both ends land exactly where the stepped ramp puts them`() {
        listOf(2, 3, 8, 20).forEach { span ->
            assertEquals("span $span, first step", CrossbarDim.ranked(1), CrossbarDim.smoothed(1, span), 0.0001f)
            assertEquals("span $span, last step", CrossbarDim.ranked(CrossbarDim.LastStep), CrossbarDim.smoothed(span, span), 0.0001f)
        }
    }

    @Test
    fun `a long list fades all the way down instead of cliffing`() {
        val span = 8
        val alphas = (1..span).map { CrossbarDim.smoothed(it, span) }
        alphas.zipWithNext().forEach { (near, far) ->
            assertTrue("each step must be dimmer than the one before it: $alphas", far < near)
        }
    }

    @Test
    fun `a three-row rail matches the stepped ramp it replaces`() {
        assertEquals(CrossbarDim.ranked(1), CrossbarDim.smoothed(1, 3), 0.0001f)
        assertEquals(CrossbarDim.ranked(3), CrossbarDim.smoothed(3, 3), 0.0001f)
    }

    @Test
    fun `distances past the span clamp rather than fading to nothing`() {
        assertEquals(CrossbarDim.smoothed(8, 8), CrossbarDim.smoothed(40, 8), 0f)
        assertTrue(CrossbarDim.smoothed(40, 8) > 0f)
    }

    @Test
    fun `a single-row list does not divide by zero`() {
        assertTrue(CrossbarDim.smoothed(1, span = 1) > 0f)
        assertTrue(CrossbarDim.smoothed(1, span = 0) > 0f)
    }
}
