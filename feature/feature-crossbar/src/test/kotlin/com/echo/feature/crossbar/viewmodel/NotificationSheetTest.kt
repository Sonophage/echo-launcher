package com.echo.feature.crossbar.viewmodel

import com.echo.core.common.format.relativeTime
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.MusicTrack
import com.echo.core.ui.notification.AndroidNotice
import com.echo.core.ui.notification.SystemToast
import com.echo.core.ui.notification.ToastKind
import com.echo.feature.crossbar.music.MusicPlaybackState
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
        recent: CrossbarItem? = null,
        cursor: Int = 0,
        tab: PanelTab = PanelTab.NOTIFICATIONS,
    ) = CrossbarUiState(
        showBootSequence = false,
        notificationsOpen = true,
        panelTab = tab,
        androidNotices = notices,
        launcherNotices = toasts,
        noticeCursor = cursor,
        recentTop = recent,
        musicPlayback = if (playing) {
            MusicPlaybackState(track = track(), isPlaying = true, durationMs = 4_000)
        } else {
            MusicPlaybackState()
        },
    )

    private val game = CrossbarItem(id = "7", title = "Crisis Core", gameId = 7, totalPlayTimeMillis = 7_200_000)

    private fun labels(s: CrossbarUiState) = stageActions(s.panelStage(), s.clearableNoticeCount).map { it.label }

    @Test
    // owner, 2026-10-08: what a player holds is now pinned first (PanelMediaTest); the last thing played, with no
    // player holding it, still stays on the orb
    fun `what was last played stays on the orb, not in the notifications list`() {
        assertEquals(listOf(NoticeFocus.Media), state(playing = true).noticeFocusables)
        assertTrue(state(recent = game).noticeFocusables.isEmpty())
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
        val stage = state(recent = game).copy(recentTopAt = 99L).recentStage() as PanelStage.Game
        assertEquals(99L, stage.lastPlayedAt)
        assertEquals(7_200_000L, stage.playTimeMs)
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

    @Test
    fun `the chips sort messages from the device's own notices, and All keeps everything`() {
        val chat = notice("chat").copy(category = "msg")
        val update = notice("sys").copy(packageName = "com.android.vending", category = null)
        val reminder = notice("cal").copy(packageName = "com.google.android.calendar", category = "reminder")
        val s = state(notices = listOf(chat, update, reminder), toasts = listOf(toast(1, at = 5)))
        assertEquals(4, s.noticeFocusables.size)
        assertEquals(listOf(NoticeFocus.Notice("chat")), s.copy(noticeChip = NoticeChip.MESSAGES).noticeFocusables)
        assertEquals(
            "Android's own notices and ECHO's toasts are System; a reminder is in neither",
            setOf(NoticeFocus.Notice("sys"), NoticeFocus.Launcher(1)),
            s.copy(noticeChip = NoticeChip.SYSTEM).noticeFocusables.toSet(),
        )
    }

    @Test
    fun `the footer carries the focused notice's actions, Y clearing all`() {
        val s = state(notices = listOf(notice("a").copy(canOpen = true, canDismiss = true))).copy(notificationsOpen = true)
        val prompts = promptsFor(s)
        assertEquals(GamepadAction.SELECT, prompts.primary?.action)
        assertTrue(prompts.right.any { it.action == GamepadAction.CHANGE_SORT && it.verb == "Dismiss" })
        assertTrue(prompts.right.any { it.action == GamepadAction.OPEN_CONTEXT_MENU && it.verb.startsWith("Clear all") })
    }
}
