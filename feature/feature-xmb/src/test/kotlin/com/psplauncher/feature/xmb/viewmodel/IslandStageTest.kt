package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.MusicTrack
import com.psplauncher.feature.xmb.music.MusicPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandStageTest {
    private val track = MusicTrack(
        id = "t1", folderId = "f", uri = "content://t1", displayName = "track.mp3",
        title = "Blue Monday", artist = "New Order",
    )
    private val game = XMBItem(id = "7", title = "Crisis Core", gameId = 7, totalPlayTimeMillis = 7_200_000)

    @Test
    fun `the recent island takes the recent game's colour even while a paused track is loaded`() {
        val s = XMBUiState(
            showBootSequence = false,
            recentTop = game,
            musicPlayback = MusicPlaybackState(track = track, isPlaying = false, positionMs = 1_000, durationMs = 4_000),
        )
        assertTrue("the panel's media row still prefers the loaded track", s.mediaStage() is PanelStage.Music)
        assertTrue("the island shows the recent game, so its stage and tint must be the game's", s.recentStage() is PanelStage.Game)
    }

    @Test
    fun `the island draws a progress line only when there is a real position to show`() {
        assertEquals(0.25f, PanelStage.Music("a", null, null, null, true, true, 1_000, 4_000).islandProgress!!, 0.001f)
        assertNull("a recent track has no duration, so no line", PanelStage.Music("a", null, null, null, false, false, 0, 0).islandProgress)
        assertEquals(0.6f, PanelStage.Video("v", null, null, 0.6f, null).islandProgress!!, 0.001f)
        assertNull(PanelStage.Game("g", null, null, 0).islandProgress)
    }
}
