package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GameGenre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: the game lists filter by genre, and a game's menu offers the filter and Edit Genre
class GenreFilterTest {
    private val rpg = CrossbarItem(id = "1", title = "Crisis Core", gameId = 1, genre = GameGenre.RPG)
    private val platformer = CrossbarItem(id = "2", title = "Wario Land", gameId = 2, genre = GameGenre.PLATFORMER)
    private val unknown = CrossbarItem(id = "3", title = "Homebrew", gameId = 3)
    private val addRow = CrossbarItem(id = "add_games", title = "Add Games", type = CrossbarItemType.ADD_ACTION)

    @Test
    fun `a genre shows only its games and keeps the column's other rows`() {
        val list = listOf(rpg, platformer, unknown, addRow)
        assertEquals(listOf(rpg, addRow), list.withGenre(GameGenre.RPG))
        assertEquals("no filter: everything", list, list.withGenre(null))
    }

    @Test
    fun `the menu offers this game's genre, or clearing the one in force`() {
        assertEquals("Show Only RPG", genreFilterRow(GameGenre.RPG, active = null)?.label)
        assertEquals("genre_all", genreFilterRow(GameGenre.RPG, active = GameGenre.PLATFORMER)?.action)
        assertNull("a game with no genre offers no filter", genreFilterRow(null, active = null))
    }

    // owner, 2026-10-08: Game Info from the App Drawer's Options did nothing when the game was not in the column
    @Test
    fun `a game not in the column is found in the library`() = kotlinx.coroutines.test.runTest {
        val fromLibrary = CrossbarItem(id = "9", title = "Castlevania", gameId = 9)
        assertEquals(fromLibrary, gameRowFor(9, listOf(rpg, platformer)) { fromLibrary })
        assertEquals("the column's own row first", rpg, gameRowFor(1, listOf(rpg)) { fromLibrary })
    }
}
