package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.achievement.Achievement
import com.echo.core.domain.achievement.AchievementProvider
import com.echo.core.domain.achievement.AchievementSet
import com.echo.core.domain.discord.DiscordFriend
import com.echo.core.domain.discord.DiscordPresence
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {
    private fun set(id: String, title: String, gameId: Long? = null) =
        AchievementSet(AchievementProvider.STEAM, id, gameId, title, null, 10, 5, 0, 0, false, null, null)

    private fun badge(id: String, unlocked: Boolean, percent: Double?) =
        Achievement(id, id, "", null, false, unlocked, null, percent, null)

    private fun friend(name: String, presence: DiscordPresence, activity: String? = null) =
        DiscordFriend(name, name, name, "", presence, activity)

    @Test
    fun `tiers follow the global unlock percent at 1, 5 and 20, and a missing percent is never called common`() {
        assertEquals(RarityTier.LEGENDARY, rarityTier(0.99))
        assertEquals(RarityTier.EPIC, rarityTier(1.0))
        assertEquals(RarityTier.EPIC, rarityTier(4.9))
        assertEquals(RarityTier.RARE, rarityTier(5.0))
        assertEquals(RarityTier.RARE, rarityTier(19.9))
        assertEquals(RarityTier.COMMON, rarityTier(20.0))
        assertEquals(RarityTier.UNKNOWN, rarityTier(null))
    }

    @Test
    fun `the showcase is the rarest unlocks across every set, never a locked badge or one without a percent`() {
        val sets = listOf(set("a", "Ico"), set("b", "Rez"))
        val badges = mapOf(
            "STEAM:a" to listOf(badge("common", true, 60.0), badge("locked", false, 0.1), badge("unknown", true, null)),
            "STEAM:b" to listOf(badge("rarest", true, 0.5), badge("rare", true, 12.0)),
        )
        val shown = showcase(sets, badges, count = 2)
        assertEquals(listOf("rarest", "rare"), shown.map { it.achievement.id })
        assertEquals("Rez", shown.first().game)
    }

    @Test
    fun `friends group as playing, then online, then offline, and an idle friend counts as online`() {
        val groups = groupFriends(
            listOf(
                friend("zed", DiscordPresence.OFFLINE),
                friend("amy", DiscordPresence.IDLE),
                friend("bo", DiscordPresence.ONLINE, activity = "Ico"),
                friend("cy", DiscordPresence.UNKNOWN),
            ),
        )
        assertEquals(listOf(FriendGroup.PLAYING, FriendGroup.ONLINE, FriendGroup.OFFLINE), groups.map { it.first })
        assertEquals(listOf("cy", "zed"), groups.last().second.map { it.label })
        assertEquals(listOf("amy"), groups[1].second.map { it.label })
    }

    @Test
    fun `left and right pick a set on the strip, down enters the grid, up from the top row returns to the strip`() {
        val data = ProfileData(
            sets = listOf(set("a", "Ico"), set("b", "Rez")),
            badges = mapOf("STEAM:a" to (1..8).map { badge("$it", it <= 4, 50.0) }),
        )
        val strip = ProfileState(tab = ProfileTab.ACHIEVEMENTS, data = data)
        assertEquals(1, stepProfile(strip, GamepadAction.NAVIGATE_RIGHT).set)
        assertEquals(strip, stepProfile(strip, GamepadAction.NAVIGATE_LEFT))

        val grid = stepProfile(strip, GamepadAction.NAVIGATE_DOWN)
        assertEquals(true, grid.inGrid)
        val secondRow = stepProfile(grid, GamepadAction.NAVIGATE_DOWN)
        assertEquals(BADGE_COLUMNS, secondRow.badge)
        assertEquals(secondRow, stepProfile(secondRow, GamepadAction.NAVIGATE_DOWN))
        assertEquals(false, stepProfile(grid, GamepadAction.NAVIGATE_UP).inGrid)

        // owner, 2026-10-06: X filters, as everywhere; Y is the context menu and leaves the filter alone
        assertEquals(BadgeFilter.UNLOCKED, stepProfile(grid, GamepadAction.CHANGE_SORT).filter)
        assertEquals(BadgeFilter.ALL, stepProfile(grid, GamepadAction.OPEN_CONTEXT_MENU).filter)
    }

    @Test
    fun `opening from a game's info lands on that game's set once the sets arrive`() {
        val waiting = ProfileState(tab = ProfileTab.ACHIEVEMENTS, openOnGameId = 7L)
        assertEquals(0, waiting.withData(ProfileData()).set)
        val landed = waiting.withData(ProfileData(sets = listOf(set("a", "Ico", 3L), set("b", "Rez", 7L))))
        assertEquals(1, landed.set)
        assertEquals(null, landed.openOnGameId)
    }

    @Test
    fun `recently played is the latest played games, one per disc set, and the banner is the latest game's own art`() {
        val games = listOf(
            Game(id = 1, title = "Never", platformId = "psx", artworkUri = "never.png"),
            Game(id = 2, title = "Old", platformId = "psx", lastPlayedAt = 10, artworkUri = "old.png"),
            Game(id = 3, title = "Disc 1", platformId = "psx", discSetKey = "ff7", lastPlayedAt = 30, artworkUri = "ff7.png"),
            Game(id = 4, title = "Disc 2", platformId = "psx", discSetKey = "ff7", lastPlayedAt = 20),
            Game(id = 5, title = "Mid", platformId = "psx", lastPlayedAt = 25, iconUri = "mid.png"),
        )
        val recent = recentlyPlayed(games)
        assertEquals(listOf(3L, 5L, 2L), recent.map { it.id })
        assertEquals("ff7.png", profileBanner(recent))
        assertEquals("a game with only an icon still gives the banner its art", "mid.png", profileBanner(recent.drop(1)))
        assertEquals(null, profileBanner(emptyList()))
    }

    @Test
    // owner, 2026-10-06: the row of big numbers is a stop between Edit and the cards; it opens Overview
    fun `the profile tab walks edit, the stats, the recent cards, showcase and friends, and comes back to the card it left`() {
        fun go(f: ProfileFocus, vararg moves: PanelMove, recents: Int = 3) = moves.fold(f) { acc, m -> moveProfileFocus(acc, m, recents) }
        val edit = ProfileFocus()
        val stats = go(edit, PanelMove.DOWN)
        assertEquals(ProfileSpot.STATS, stats.spot)
        assertEquals(ProfileFocus(ProfileSpot.RECENT, 0), go(stats, PanelMove.DOWN))
        assertEquals("down holds on the last card", ProfileFocus(ProfileSpot.RECENT, 2), go(stats, PanelMove.DOWN, PanelMove.DOWN, PanelMove.DOWN, PanelMove.DOWN))
        val showcase = go(stats, PanelMove.DOWN, PanelMove.DOWN, PanelMove.RIGHT)
        assertEquals(ProfileSpot.SHOWCASE, showcase.spot)
        assertEquals("left from the showcase returns to the card you came from", ProfileFocus(ProfileSpot.RECENT, 1), go(showcase, PanelMove.LEFT))
        assertEquals(ProfileSpot.FRIENDS, go(showcase, PanelMove.RIGHT).spot)
        assertEquals("up from the columns is the stats", ProfileSpot.STATS, go(showcase, PanelMove.RIGHT, PanelMove.UP).spot)
        assertEquals(ProfileSpot.STATS, go(stats, PanelMove.DOWN, PanelMove.UP).spot)
        assertEquals("and up again is Edit", ProfileSpot.EDIT, go(stats, PanelMove.UP).spot)
        assertEquals("with nothing played, down from the stats skips to the showcase", ProfileSpot.SHOWCASE, go(stats, PanelMove.DOWN, recents = 0).spot)
        assertEquals(ProfileSpot.SHOWCASE, go(stats, PanelMove.DOWN, PanelMove.LEFT, recents = 0).spot)
        val choice = ProfileFocus(ProfileSpot.EDIT_NAME)
        assertEquals(ProfileSpot.EDIT_PICTURE, go(choice, PanelMove.RIGHT).spot)
        assertEquals(ProfileSpot.EDIT_NAME, go(choice, PanelMove.RIGHT, PanelMove.LEFT).spot)
    }

    // owner, 2026-10-07: the strip starts at the game played last; never-played sets follow in their own order
    @Test
    fun `achievement sets run from the last played, never played last`() {
        fun played(id: String, at: Long?) = set(id, id).copy(lastPlayedAt = at)
        val sorted = setsByLastPlayed(listOf(played("old", 10), played("never1", null), played("new", 30), played("never2", null), played("mid", 20)))
        assertEquals(listOf("new", "mid", "old", "never1", "never2"), sorted.map { it.title })
    }
}
