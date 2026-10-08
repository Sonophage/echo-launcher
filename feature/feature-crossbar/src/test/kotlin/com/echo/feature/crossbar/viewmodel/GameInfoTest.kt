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

    @Test
    fun `LT and RT walk screenshots, info, video and the manual, skipping what the game lacks, and wrap`() {
        val full = GameInfoState(CrossbarItem(id = "g", title = "Ico", gameId = 1L),
            content = DetailPanelContent(title = "Ico", platformName = "PS2"), videoUri = "/v.mp4", manualPath = "/m.pdf")
        assertEquals(GameInfoAction.INFO, stepGameInfoSection(full, +1))
        assertEquals(GameInfoAction.VIDEO, stepGameInfoSection(full.copy(open = GameInfoAction.INFO), +1))
        assertEquals("the manual is a view of its own now", GameInfoAction.MANUAL, stepGameInfoSection(full.copy(open = GameInfoAction.VIDEO), +1))
        assertEquals("RT past the last wraps to the screenshots", null, stepGameInfoSection(full.copy(open = GameInfoAction.MANUAL), +1))
        assertEquals("LT from the screenshots wraps to the last", GameInfoAction.MANUAL, stepGameInfoSection(full, -1))

        val noVideo = full.copy(videoUri = null, manualPath = null)
        assertEquals(null, stepGameInfoSection(noVideo.copy(open = GameInfoAction.INFO), +1))
    }

    private fun set(total: Int) = com.echo.core.domain.achievement.AchievementSet(
        provider = com.echo.core.domain.achievement.AchievementProvider.STEAM, providerGameId = "489830", gameId = 1L,
        title = "Skyrim", iconUrl = null, total = total, unlocked = 7, points = 0, earnedPoints = 0, mastered = false,
        lastSyncedAt = null, lastPlayedAt = null,
    )

    // owner, 2026-10-06: achievements first, then screenshots, meta, video, and the manual when there is one
    @Test
    fun `a game with achievements opens on them, in the owner's order`() {
        val full = GameInfoState(CrossbarItem(id = "g", title = "Skyrim", gameId = 1L),
            content = DetailPanelContent(title = "Skyrim", platformName = "PC"), videoUri = "/v.mp4", manualPath = "/m.pdf",
            achievementSet = set(75))
        assertEquals(
            listOf(GameInfoAction.ACHIEVEMENTS, null, GameInfoAction.INFO, GameInfoAction.VIDEO, GameInfoAction.MANUAL),
            gameInfoSections(full),
        )
        assertEquals(GameInfoAction.ACHIEVEMENTS, full.firstSection())
        val described = full.copy(content = full.content?.copy(description = "A dragon returns."))
        assertEquals("no set, no Achievements view: it opens on the screenshots and description",
            null, described.copy(achievementSet = null).firstSection())
        assertEquals("an empty set is no achievements", null, described.copy(achievementSet = set(0)).firstSection())
        // owner, 2026-10-08: with no screenshots and no description that view is empty, so Info comes first
        assertEquals(GameInfoAction.INFO, full.copy(achievementSet = null).firstSection())
    }

    private fun badge(id: String, unlockedAt: Long?) = com.echo.core.domain.achievement.Achievement(
        id = id, name = id, description = "", iconUrl = null, isHidden = false,
        isUnlocked = unlockedAt != null, unlockedAt = unlockedAt, globalPercent = null, points = null,
    )

    @Test
    fun `the view shows the latest unlocked first, then what comes next, through the wall's filter`() {
        val all = listOf(badge("a", 10L), badge("b", null), badge("c", 30L), badge("d", null), badge("e", 20L))
        assertEquals(listOf("c", "e", "a", "b", "d"), gameInfoBadges(all, BadgeFilter.ALL).map { it.id })
        assertEquals(listOf("c", "e", "a"), gameInfoBadges(all, BadgeFilter.UNLOCKED).map { it.id })
        assertEquals(listOf("b", "d"), gameInfoBadges(all, BadgeFilter.LOCKED).map { it.id })
    }
}
