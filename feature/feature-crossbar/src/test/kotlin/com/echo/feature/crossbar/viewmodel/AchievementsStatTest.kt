package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.achievement.AchievementProvider
import com.echo.core.domain.achievement.AchievementSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-08: Recent shows a game's achievements under its details, worded as Game Info words them
class AchievementsStatTest {
    private fun set(unlocked: Int, total: Int) = AchievementSet(
        provider = AchievementProvider.entries.first(), providerGameId = "1", gameId = 1L, title = "Skyrim",
        iconUrl = null, total = total, unlocked = unlocked, points = 0, earnedPoints = 0, mastered = false,
        lastSyncedAt = null, lastPlayedAt = null,
    )

    @Test
    fun `a game with a set reads unlocked over total, and one without reads nothing`() {
        assertEquals("7/75", achievementsStatOf(set(7, 75)))
        assertNull(achievementsStatOf(set(0, 0)))
        assertNull(achievementsStatOf(null))
    }
}
