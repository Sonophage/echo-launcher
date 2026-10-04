package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.feature.crossbar.ui.detail.DetailPanelContent
import org.junit.Assert.assertEquals
import org.junit.Test

class GameInfoTest {
    private val now = 10L * 24 * 60 * 60_000L

    @Test
    fun `the band shows only stats the item really has, never a placeholder`() {
        val bare = GameInfoState(CrossbarItem(id = "g", title = "Ico", gameId = 1L))
        assertEquals(emptyList<GameInfoStat>(), gameInfoStats(bare, newNotices = 0, now = now))

        val played = GameInfoState(
            CrossbarItem(id = "g", title = "Ico", gameId = 1L, totalPlayTimeMillis = 3 * 60 * 60_000L, lastOpenedAt = now - 2 * 60 * 60_000L),
            content = DetailPanelContent(title = "Ico", platformName = "PlayStation 2"),
        )
        assertEquals(
            listOf(GameInfoStat("Played", "3 hr"), GameInfoStat("Last played", "2 hr ago"), GameInfoStat("Platform", "PlayStation 2")),
            gameInfoStats(played, newNotices = 5, now = now),
        )
        assertEquals("Achievements", gameInfoStats(played.copy(achievementsStat = "41/75"), 0, now).first().label)

        val app = GameInfoState(CrossbarItem(id = "a", title = "Discord", packageName = "com.discord"), appVersion = "214.1")
        assertEquals(listOf(GameInfoStat("New", "3"), GameInfoStat("Version", "214.1")), gameInfoStats(app, newNotices = 3, now = now))
        assertEquals("Storage", gameInfoStats(app.copy(appStorageBytes = 1L shl 30), 0, now).first().label)
    }

    @Test
    fun `down enters the card row, the cursor stays inside it, up returns to the buttons`() {
        assertEquals(null, stepGameInfoCursor(null, 0, GamepadAction.NAVIGATE_DOWN))
        assertEquals(0, stepGameInfoCursor(null, 3, GamepadAction.NAVIGATE_DOWN))
        assertEquals(null, stepGameInfoCursor(null, 3, GamepadAction.NAVIGATE_RIGHT))
        assertEquals(2, stepGameInfoCursor(2, 3, GamepadAction.NAVIGATE_RIGHT))
        assertEquals(0, stepGameInfoCursor(0, 3, GamepadAction.NAVIGATE_LEFT))
        assertEquals(1, stepGameInfoCursor(4, 2, GamepadAction.SELECT))
        assertEquals(null, stepGameInfoCursor(1, 3, GamepadAction.NAVIGATE_UP))
    }

    @Test
    fun `Game Info offers Video and Manual only when the game has them, so Options never lists a dead row`() {
        val bare = GameInfoState(CrossbarItem(id = "g", title = "Ico", gameId = 1L))
        assertEquals(listOf(GameInfoAction.PLAY, GameInfoAction.OPTIONS), gameInfoActions(bare))

        val loaded = bare.copy(content = DetailPanelContent(title = "Ico", platformName = "PlayStation 2"))
        assertEquals(listOf(GameInfoAction.PLAY, GameInfoAction.INFO, GameInfoAction.OPTIONS), gameInfoActions(loaded))

        val full = loaded.copy(videoUri = "/pfp/video/ico.mp4", manualPath = "/pfp/manual/ico.pdf")
        val actions = gameInfoActions(full)
        assertEquals(
            listOf(GameInfoAction.PLAY, GameInfoAction.INFO, GameInfoAction.VIDEO, GameInfoAction.MANUAL, GameInfoAction.OPTIONS),
            actions,
        )

        val app = GameInfoState(CrossbarItem(id = "a", title = "Discord", packageName = "com.discord"), videoUri = "/x.mp4", manualPath = "/x.pdf")
        assertEquals(listOf(GameInfoAction.PLAY, GameInfoAction.OPTIONS), gameInfoActions(app))
    }

    @Test
    fun `the Info sheet stops at its last page, so one Up always moves it back`() {
        val sheet = GameInfoState(CrossbarItem(id = "g", title = "Ico", gameId = 1L), open = GameInfoAction.INFO, infoScrollMax = 2)
        val pastEnd = (1..5).fold(sheet) { s, _ -> s.scrolledBy(+1) }
        assertEquals(2, pastEnd.infoScroll)
        assertEquals(1, pastEnd.scrolledBy(-1).infoScroll)
        assertEquals(0, sheet.scrolledBy(-1).infoScroll)
    }
}
