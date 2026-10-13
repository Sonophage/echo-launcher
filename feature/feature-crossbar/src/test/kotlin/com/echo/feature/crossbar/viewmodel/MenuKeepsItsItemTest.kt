package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.Video
import com.echo.core.domain.repository.VideoRepository
import com.echo.feature.crossbar.music.MusicPlayerController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

// Spec #32, ADR 0002, review P3: a menu's rows act on the item and the place the menu opened for, taken when it
// opens, never on what the Main Screen shows when the row runs. The Main Screen here has moved on: no playlist open,
// nothing listed, another list of songs browsed.
class MenuKeepsItsItemTest {
    private val vm = mockk<CrossbarViewModel>(relaxed = true)
    private val ui = MutableStateFlow(CrossbarUiState())
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    init {
        every { vm.appAction(any()) } answers { runBlocking { firstArg<suspend () -> Unit>()() } }
    }

    @Test
    fun `Remove from Playlist removes the video from the playlist its menu opened in`() {
        val videos = mockk<VideoRepository>(relaxed = true)
        coEvery { videos.getVideo("v1") } returns mockk<Video>(relaxed = true)
        val video = CrossbarVideo(vm, ui, scope, videos, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        video.openVideoFileContextMenu("v1", "Heat", MenuSlot.COMPANION, at = CrossbarUiState(videoNav = VideoNav.Playlist(5L, "Films")))
        val menu = ui.value.companionMenu!!
        video.handleVideoFileAction("v1", "video_remove_playlist", MenuSlot.COMPANION, menu.playlistId)

        coVerify { videos.removeVideoFromPlaylist(5L, "v1") }
    }

    @Test
    fun `Play queues the songs listed where the song's menu opened`() {
        val player = mockk<MusicPlayerController>(relaxed = true)
        val music = CrossbarMusic(vm, ui, scope, mockk(relaxed = true), player, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        fun t(id: String) = MusicTrack(id = id, folderId = "f", uri = "content://$id", displayName = "$id.flac")
        val listed = listOf(t("a"), t("b"))
        music.currentMusicTracks = listed
        music.openMusicContextMenu(CrossbarItem(id = "mt_b", title = "b", type = CrossbarItemType.MUSIC_TRACK), MenuSlot.COMPANION, at = CrossbarUiState())
        val menu = ui.value.companionMenu!!
        music.currentMusicTracks = listOf(t("x"), t("y"))

        music.handleMusicTrackAction("b", "play", menu.playlistId, MenuSlot.COMPANION, menu.musicQueue)

        verify { player.setQueue(listed, 1) }
    }

    @Test
    fun `Rename Playlist starts from the name of the playlist its menu opened for, on the screen of that menu`() {
        val music = CrossbarMusic(vm, ui, scope, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        val video = CrossbarVideo(vm, ui, scope, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        music.handlePlaylistRowAction(7L, "Night Drive", "rename_playlist", MenuSlot.COMPANION)
        assertEquals("Night Drive", ui.value.companionPrompts.playlistNameDialog?.text)
        assertNull("not on the Main Screen", ui.value.mainPrompts.playlistNameDialog)

        ui.value = CrossbarUiState()
        video.handleVideoPlaylistRowAction(5L, "Films", "rename_video_playlist", MenuSlot.COMPANION)
        assertEquals("Films", ui.value.companionPrompts.playlistNameDialog?.text)
        assertNull(ui.value.mainPrompts.playlistNameDialog)
    }

    // review P2: Open Folder's path and Export Game's report, when a Companion row ends with one, show on the Companion
    // and leave the Main Screen live
    @Test
    fun `a message a Companion row ends with is drawn on the Companion`() {
        val shown = CrossbarUiState(showBootSequence = false).withInfoDialog(MenuSlot.COMPANION, InfoDialogState(title = "Crisis Core", message = "/storage/emulated/0/ROMs/psp"))
        assertEquals("/storage/emulated/0/ROMs/psp", shown.companionInfoDialog?.message)
        assertNull(shown.infoDialog)
        assertFalse("the Main Screen stays live", shown.hasBlockingOverlay)
        assertNull("dismissed on its own screen", shown.withInfoDialog(MenuSlot.COMPANION, null).companionInfoDialog)
    }
}
