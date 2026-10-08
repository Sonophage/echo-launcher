package com.echo.feature.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: a video opened to play shows no details screen after it; it closes when playback stops
class AutoPlayCloseTest {
    @Test
    fun `it closes once playback has started and stopped, never before it starts`() {
        assertFalse("still loading", autoPlayEnded(started = false, playing = false, handedOff = false))
        assertFalse("playing", autoPlayEnded(started = true, playing = true, handedOff = false))
        assertFalse("in another app's player", autoPlayEnded(started = true, playing = false, handedOff = true))
        assertTrue("stopped", autoPlayEnded(started = true, playing = false, handedOff = false))
    }
}
