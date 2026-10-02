package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.domain.model.MusicTrack
import com.psplauncher.core.ui.notification.AndroidNotice
import com.psplauncher.core.ui.notification.SystemToast
import com.psplauncher.core.ui.notification.ToastKind
import com.psplauncher.feature.xmb.music.MusicPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSheetTest {
    private fun notice(key: String, canOpen: Boolean = true, canDismiss: Boolean = true, at: Long = 1L) = AndroidNotice(
        key = key, appLabel = "Signal", title = "A message", text = "hello", postedAt = at,
        canOpen = canOpen, canDismiss = canDismiss, packageName = "org.signal",
    )

    private fun toast(id: Long, at: Long) = SystemToast(id, "Backup complete", null, ToastKind.SUCCESS, postedAt = at)

    private fun track() = MusicTrack(
        id = "t1", folderId = "f", uri = "content://t1", displayName = "track.mp3",
        title = "Blue Monday", artist = "New Order",
    )

    private fun state(
        notices: List<AndroidNotice> = emptyList(),
        toasts: List<SystemToast> = emptyList(),
        playing: Boolean = false,
        recent: XMBItem? = null,
        cursor: Int = 0,
        tab: PanelTab = PanelTab.NOTIFICATIONS,
    ) = XMBUiState(
        showBootSequence = false,
        notificationsOpen = true,
        panelTab = tab,
        androidNotices = notices,
        launcherNotices = toasts,
        noticeCursor = cursor,
        recentTop = recent,
        musicPlayback = if (playing) {
            MusicPlaybackState(track = track(), isPlaying = true, positionMs = 1_000, durationMs = 4_000)
        } else {
            MusicPlaybackState()
        },
    )

    private val game = XMBItem(id = "7", title = "Crisis Core", gameId = 7, totalPlayTimeMillis = 7_200_000)

    private fun labels(s: XMBUiState) = stageActions(s.panelStage(), s.clearableNoticeCount).map { it.label }

    @Test
    fun `the recent row leads the list when something is playing or was played`() {
        assertEquals(listOf(NoticeFocus.Media), state(playing = true).noticeFocusables)
        assertEquals(listOf(NoticeFocus.Media), state(recent = game).noticeFocusables)
        assertTrue(state().noticeFocusables.isEmpty())
        assertEquals(PanelStage.Empty, state().panelStage())
    }

    @Test
    fun `android and launcher notices share one list, newest first`() {
        val s = state(notices = listOf(notice("a", at = 30)), toasts = listOf(toast(1, at = 50), toast(2, at = 10)))
        assertEquals(
            listOf(NoticeFocus.Launcher(1), NoticeFocus.Notice("a"), NoticeFocus.Launcher(2)),
            s.noticeFocusables,
        )
    }

    @Test
    fun `every notice is reachable, not just the first few`() {
        val many = (1..12).map { notice("k$it") }
        assertEquals(12, state(notices = many).noticeFocusables.size)
    }

    @Test
    fun `a cursor past the end lands on the last row rather than nowhere`() {
        assertEquals(NoticeFocus.Notice("a"), state(notices = listOf(notice("a")), cursor = 4).focusedNotice)
    }

    @Test
    fun `on the other tabs nothing in the list is focused, so A, X and Y cannot act on it`() {
        val s = state(notices = listOf(notice("a")), tab = PanelTab.QUICK)
        assertNull(s.focusedNotice)
        assertEquals(PanelStage.Empty, s.panelStage())
    }

    @Test
    fun `the stage offers Open and Dismiss only where they work`() {
        assertEquals(listOf("Open", "Dismiss", "Clear all 1"), labels(state(notices = listOf(notice("a")))))
        assertEquals(
            "an ongoing notification with no content intent offers nothing but Clear all of the others",
            listOf("Clear all 1"),
            labels(state(notices = listOf(notice("a", canOpen = false, canDismiss = false), notice("b", at = 0)))),
        )
    }

    @Test
    fun `Clear all counts what it will remove, launcher notices included`() {
        val s = state(notices = listOf(notice("a"), notice("b", canDismiss = false)), toasts = listOf(toast(1, at = 0)))
        assertEquals(2, s.clearableNoticeCount)
        assertEquals("a launcher notice can be dismissed on its own", listOf("Dismiss", "Clear all 2"),
            labels(s.copy(noticeCursor = 2)))
    }

    @Test
    fun `the playing track gets transport, a track from Recent gets Play`() {
        assertEquals(listOf("Pause", "Next track", "Open Music"), labels(state(playing = true)))
        val fromRecent = XMBItem(id = "m1", title = "Blue Monday", type = XMBItemType.MUSIC_TRACK)
        assertEquals(listOf("Play"), labels(state(recent = fromRecent)))
        assertEquals(RecentKind.MUSIC, recentKind(XMBItem(id = "mg", title = "Album", type = XMBItemType.MUSIC_GROUP)))
    }

    @Test
    fun `each Recent type gets its own verb`() {
        assertEquals(listOf("Continue"), labels(state(recent = game)))
        assertEquals(listOf("Return to app"), labels(state(recent = XMBItem(id = "app_x", title = "Spotify", packageName = "com.spotify", isAndroidApp = true))))
        assertEquals(listOf("Continue reading"), labels(state(recent = XMBItem(id = "book_1", title = "Dune", type = XMBItemType.LIBRARY_BOOK))))
        val video = XMBItem(id = "vid_1", title = "Northline", type = XMBItemType.VIDEO_FILE)
        assertEquals(listOf("Play"), labels(state(recent = video)))
        assertEquals("a video with a resume point is resumed", listOf("Resume"), labels(state(recent = video.copy(progressFraction = 0.6f))))
    }

    @Test
    fun `no stage puts two commands on one button`() {
        val stages = listOf(
            state(playing = true), state(recent = game), state(notices = listOf(notice("a"))),
            state(toasts = listOf(toast(1, at = 0))),
        ).map { it.panelStage() }
        stages.forEach { stage ->
            val buttons = stageActions(stage, clearable = 3).map { it.button }
            assertEquals("duplicate button on $stage", buttons.distinct(), buttons)
        }
    }

    @Test
    fun `the game stage carries when it was played and for how long`() {
        val stage = state(recent = game).copy(recentTopAt = 99L).panelStage() as PanelStage.Game
        assertEquals(99L, stage.lastPlayedAt)
        assertEquals(7_200_000L, stage.playTimeMs)
    }

    @Test
    fun `the bar keeps only Close and Switch tab, since the stage carries the rest`() {
        val prompts = promptsFor(state(notices = listOf(notice("a"))))
        assertNull(prompts.primary)
        assertEquals(GamepadAction.BACK, prompts.back.action)
        assertEquals("Close", prompts.back.verb)
        assertEquals(listOf(GamepadAction.PREV_CATEGORY to GamepadAction.NEXT_CATEGORY), prompts.right.map { it.action to it.pairedWith })
    }

    @Test
    fun `relative times read the way the list shows them`() {
        val now = 10_000_000L
        assertEquals("Now", relativeTime(now, now - 30_000))
        assertEquals("8m ago", relativeTime(now, now - 8 * 60_000))
        assertEquals("2 hr ago", relativeTime(now, now - 125 * 60_000))
        assertEquals("a clock that runs behind the post time must not say minus", "Now", relativeTime(now, now + 60_000))
    }

    @Test
    fun `the sheet blocks the XMB but keeps the strip and the bar`() {
        val open = state(playing = true)
        assertTrue(open.hasBlockingOverlay)
        assertTrue(open.overlayKeepsChrome)
    }
}
