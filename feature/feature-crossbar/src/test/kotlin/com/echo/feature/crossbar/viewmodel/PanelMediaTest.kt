package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.MusicTrack
import com.echo.core.ui.notification.AndroidNotice
import com.echo.feature.crossbar.music.MusicPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08 (design 4a): what a player holds is pinned above the notifications and the panel opens on it
class PanelMediaTest {
    private val track = MusicTrack(id = "t1", folderId = "f", uri = "content://t1", displayName = "t.flac", title = "Pink Venom", artist = "BLACKPINK")
    private val notice = AndroidNotice(key = "k1", packageName = "com.discord", appLabel = "Discord", title = "#design", text = "3 new", postedAt = 5L)

    private fun state(playing: Boolean?, recent: CrossbarItem? = null) = CrossbarUiState(
        showBootSequence = false,
        androidNotices = listOf(notice),
        recentTop = recent,
        musicPlayback = playing?.let { MusicPlaybackState(track = track, isPlaying = it, durationMs = 180_000) } ?: MusicPlaybackState(),
    )

    @Test
    fun `the playing track is the first row and the panel opens on it`() {
        val s = state(playing = true)
        assertEquals(listOf(NoticeFocus.Media, NoticeFocus.Notice("k1")), s.noticeFocusables)
        assertEquals("Pink Venom", (s.panelStage() as PanelStage.Music).title)
    }

    @Test
    fun `a paused track stays pinned, the last track played with no player does not`() {
        assertTrue(NoticeFocus.Media in state(playing = false).noticeFocusables)
        val recentOnly = state(playing = null, recent = CrossbarItem(id = "mt_x", title = "Old song", type = CrossbarItemType.MUSIC_TRACK))
        assertNull(recentOnly.pinnedMedia())
        assertEquals(listOf(NoticeFocus.Notice("k1")), recentOnly.noticeFocusables)
    }

    // owner, 2026-10-08: the video or book just open is pinned too (4b, 4c), so stepping away or a notice arriving
    // while it is open leaves it one press away; a game stays on the orb
    @Test
    fun `the video or book last opened is pinned with its way back in, a game is not`() {
        val video = state(playing = null, recent = CrossbarItem(id = "vid_1", title = "Harbour Lights", type = CrossbarItemType.VIDEO_FILE, progressFraction = 0.4f))
        assertEquals(NoticeFocus.Media, video.noticeFocusables.first())
        assertEquals(listOf("Resume"), stageActions(video.panelStage(), clearable = 0).map { it.label })
        val book = state(playing = null, recent = CrossbarItem(id = "book_1", title = "Annihilation", type = CrossbarItemType.LIBRARY_BOOK))
        assertEquals(listOf("Continue reading"), stageActions(book.panelStage(), clearable = 0).map { it.label })
        val game = state(playing = null, recent = CrossbarItem(id = "7", title = "Crisis Core", gameId = 7))
        assertNull(game.pinnedMedia())
        assertTrue("music a player holds beats the video", state(playing = true, recent = video.recentTop).pinnedMedia() is PanelStage.Music)
    }

    @Test
    fun `the media stage's buttons are play, next track and open the player`() {
        val actions = stageActions(state(playing = true).panelStage(), clearable = 1)
        assertEquals(listOf(GamepadAction.SELECT to "Pause", GamepadAction.CHANGE_SORT to "Next track", GamepadAction.OPEN_CONTEXT_MENU to "Open Music"),
            actions.map { it.button to it.label })
    }
}
