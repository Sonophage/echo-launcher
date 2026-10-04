package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.MusicTrack
import com.echo.core.ui.notification.ExternalPlayback
import com.echo.feature.crossbar.music.MusicPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandStageTest {
    private val track = MusicTrack(
        id = "t1", folderId = "f", uri = "content://t1", displayName = "track.mp3",
        title = "Blue Monday", artist = "New Order",
    )
    private val game = CrossbarItem(id = "7", title = "Crisis Core", gameId = 7, totalPlayTimeMillis = 7_200_000)

    @Test
    fun `the recent island takes the recent game's colour even while a paused track is loaded`() {
        val s = CrossbarUiState(
            showBootSequence = false,
            recentTop = game,
            musicPlayback = MusicPlaybackState(track = track, isPlaying = false, durationMs = 4_000),
        )
        assertTrue("the panel's media row still prefers the loaded track", s.mediaStage() is PanelStage.Music)
        assertTrue("the island shows the recent game, so its stage and tint must be the game's", s.recentStage() is PanelStage.Game)
    }

    @Test
    fun `the island draws a progress line only when there is a real position to show`() {
        assertEquals(0.25f, PanelStage.Music("a", null, null, null, true, true, 4_000).islandProgress(1_000)!!, 0.001f)
        assertNull("a recent track has no duration, so no line", PanelStage.Music("a", null, null, null, false, false, 0).islandProgress(0))
        assertEquals(0.6f, PanelStage.Video("v", null, null, 0.6f, null).islandProgress(0)!!, 0.001f)
        assertNull(PanelStage.Game("g", null, null, 0).islandProgress(0))
    }

    @Test
    fun `the island shows a time only for a loaded track, and an app session only once it reports a length`() {
        assertEquals("0:01 / 0:04", PanelStage.Music("a", null, null, null, true, true, 4_000).timeLabel(1_000))
        assertNull("a recent track has no position", PanelStage.Music("a", null, null, null, false, false, 0).timeLabel(0))
        assertNull("an app that reports no length gets no time", PanelStage.Music("a", null, null, null, true, true, 0, "Stremio", "com.stremio.one").timeLabel(5_000))
    }

    @Test
    fun `the moving position lives outside the ui state, so a playback tick cannot recompose the shell`() {
        listOf(MusicPlaybackState::class.java, ExternalPlayback::class.java, PanelStage.Music::class.java).forEach { type ->
            assertTrue(
                "${type.simpleName} carries a position; it belongs in the position flows",
                type.declaredFields.none { it.name.contains("position", ignoreCase = true) },
            )
        }
    }

    private fun stremio(playing: Boolean) =
        ExternalPlayback("com.stremio.one", "Stremio", "Hotel Del Luna", null, null, playing, 3_600_000)

    private fun nowPlaying(ownPlaying: Boolean?, external: ExternalPlayback?) = CrossbarUiState(
        showBootSequence = false,
        recentTop = game,
        externalPlayback = external,
        musicPlayback = ownPlaying?.let { MusicPlaybackState(track = track, isPlaying = it) } ?: MusicPlaybackState(),
    ).mediaStage() as? PanelStage.Music

    @Test
    fun `the launcher's own music wins while it plays, otherwise a playing app session takes the slot`() {
        assertEquals("Blue Monday", nowPlaying(ownPlaying = true, external = stremio(playing = true))?.title)
        assertEquals("Hotel Del Luna", nowPlaying(ownPlaying = false, external = stremio(playing = true))?.title)
        assertEquals("a paused loaded track still beats a paused app", "Blue Monday", nowPlaying(ownPlaying = false, external = stremio(playing = false))?.title)
        assertEquals("an app session beats the recent item", "Hotel Del Luna", nowPlaying(ownPlaying = null, external = stremio(playing = false))?.title)
    }

    @Test
    fun `an app session's stage drives that app, and Y opens the app rather than the launcher's player`() {
        val stage = nowPlaying(ownPlaying = null, external = stremio(playing = true))!!
        val actions = stageActions(stage, clearable = 0)
        assertEquals(listOf(StageCommand.PLAY_PAUSE, StageCommand.NEXT_TRACK, StageCommand.OPEN_APP), actions.map { it.command })
        assertEquals("Open Stremio", actions.first { it.button == GamepadAction.OPEN_CONTEXT_MENU }.label)
    }
}
