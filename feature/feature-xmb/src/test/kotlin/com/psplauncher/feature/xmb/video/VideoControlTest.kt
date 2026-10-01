package com.psplauncher.feature.xmb.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoControlTest {
    @Test fun `the bar stops at its ends instead of wrapping`() {
        assertEquals(VideoControl.RESTART, VideoControl.RESTART.step(-1))
        assertEquals(VideoControl.SCREEN_MODE, VideoControl.SCREEN_MODE.step(1))
    }

    @Test fun `left and right walk the bar in the order the mock draws it`() {
        assertEquals(VideoControl.BACK, VideoControl.PLAY_PAUSE.step(-1))
        assertEquals(VideoControl.FORWARD, VideoControl.PLAY_PAUSE.step(1))
        assertEquals(VideoControl.SUBTITLES, VideoControl.NEXT.step(1))
    }

    @Test fun `back hides the controls while playing, and leaves otherwise`() {
        assertTrue("B over a playing video hides the bar", videoBackHides(controlsVisible = true, isPlaying = true))
        assertFalse("B with the bar already hidden must leave, or B could never exit", videoBackHides(controlsVisible = false, isPlaying = true))
        assertFalse("paused, the bar stays up, so B has to leave", videoBackHides(controlsVisible = true, isPlaying = false))
    }
}
