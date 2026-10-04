package com.echo.feature.crossbar.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelPullTest {
    @Test fun `a short pull down springs back, a third of the screen opens it`() {
        assertFalse(pullSettlesOpen(wasOpen = false, progress = 0.2f, velocityY = 0f))
        assertTrue(pullSettlesOpen(wasOpen = false, progress = 0.4f, velocityY = 0f))
    }

    @Test fun `a short push up on the open panel keeps it, a third of the screen dismisses it`() {
        assertTrue(pullSettlesOpen(wasOpen = true, progress = 0.8f, velocityY = 0f))
        assertFalse(pullSettlesOpen(wasOpen = true, progress = 0.6f, velocityY = 0f))
    }

    @Test fun `a flick decides on its own, however short the travel`() {
        assertTrue("a quick flick down from the strip opens the panel", pullSettlesOpen(false, 0.05f, 2_000f))
        assertFalse("a quick flick up dismisses the whole panel", pullSettlesOpen(true, 0.95f, -2_000f))
    }
}
