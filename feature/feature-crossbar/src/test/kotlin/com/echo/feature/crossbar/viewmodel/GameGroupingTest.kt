package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GameGenre
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: the Game column's folders can be genres instead of systems
class GameGroupingTest {
    @Test
    fun `a folder per genre with games, in the genre list's order, each opening its genre`() {
        val rows = genreFolderRows(mapOf(GameGenre.PLATFORMER to 14, GameGenre.RPG to 33, GameGenre.PUZZLE to 0), emptyMap())
        assertEquals(listOf("RPG", "Platformer"), rows.map { it.title })
        assertEquals("33 games", rows.first().subtitle)
        assertEquals(listOf(GameGenre.RPG, GameGenre.PLATFORMER), rows.map { genreOfItemId(it.id) })
    }

    @Test
    fun `All Games offers the other grouping, and an unknown saved value means systems`() {
        assertEquals("group_by_genre", allGamesContextMenuItems(GameGrouping.SYSTEM).first().action)
        assertEquals("group_by_system", allGamesContextMenuItems(GameGrouping.GENRE).first().action)
        assertEquals(GameGrouping.SYSTEM, GameGrouping.fromName(null))
        assertEquals(GameGrouping.GENRE, GameGrouping.fromName("GENRE"))
    }

    @Test
    fun `a game's Options from the App Drawer offer the other grouping, from the crossbar they do not`() {
        val game = CrossbarItem(id = "1", title = "Crisis Core", gameId = 1)
        val base = CrossbarUiState(showBootSequence = false)
        val inDrawer = gameContextMenuItems(game, base.copy(activeAppDrawerFilter = "GAMES"), 1, false, null).map { it.action }
        assertEquals(true, "group_by_genre" in inDrawer)
        val grouped = gameContextMenuItems(game, base.copy(activeAppDrawerFilter = "GAMES", gameGrouping = GameGrouping.GENRE), 1, false, null).map { it.action }
        assertEquals(true, "group_by_system" in grouped)
        assertEquals(false, "group_by_genre" in gameContextMenuItems(game, base, 1, false, null).map { it.action })
    }
}
