package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-05: apps and folder rows can be put in any order, per category
class ColumnOrderTest {
    private fun row(id: String, type: CrossbarItemType = CrossbarItemType.MEMORY_CARD) = CrossbarItem(id = id, title = id, type = type)
    private val column = listOf(row("app_spotify"), row("all_music"), row("albums"), row("add_menu", CrossbarItemType.ADD_ACTION))

    @Test
    fun `a saved order puts the rows it names first, and the rest keep their places after them`() {
        assertEquals(
            listOf("albums", "app_spotify", "all_music", "add_menu"),
            orderedColumn(column, listOf("albums", "app_spotify")).map { it.id },
        )
        assertEquals("no saved order leaves the column as built", column, orderedColumn(column, emptyList()))
    }


    @Test
    fun `add rows, search and placeholders do not move`() {
        assertFalse(row("add", CrossbarItemType.ADD_ACTION).movableInColumn)
        assertFalse(row("search", CrossbarItemType.SEARCH).movableInColumn)
        assertFalse(row("empty", CrossbarItemType.EMPTY).movableInColumn)
        assertFalse("a Resume row is a file, not a row of the category", row("vid_1", CrossbarItemType.VIDEO_FILE).movableInColumn)
    }

    @Test
    fun `only a media root or a category of apps can be ordered`() {
        fun state(id: String, vararg changes: (CrossbarUiState) -> CrossbarUiState): CrossbarUiState =
            changes.fold(CrossbarUiState(categories = listOf(Category(id = id, name = id, iconKey = "", type = CategoryType.MANUAL, position = 0)))) { s, f -> f(s) }

        assertEquals(BuiltInCategory.MUSIC, state(BuiltInCategory.MUSIC).columnOrderKey())
        assertNull("inside Folders the order is the music's own", state(BuiltInCategory.MUSIC, { it.copy(musicNav = MusicNav.Folders) }).columnOrderKey())
        assertNull(state(BuiltInCategory.GAMES).columnOrderKey())
        assertNull(state(BuiltInCategory.RECENTLY_PLAYED).columnOrderKey())
        assertEquals("my_apps", state("my_apps").columnOrderKey())
    }
}
