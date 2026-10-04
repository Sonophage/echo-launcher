package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalCategoryBarTest {
    private fun cat(
        id: String,
        name: String,
        position: Int,
        iconKey: String = "ic_$id",
        gaming: Boolean = false,
    ) = Category(
        id = id,
        name = name,
        iconKey = iconKey,
        type = CategoryType.BUILT_IN,
        position = position,
        isGamingCategory = gaming,
    )

    private val fallbacks = listOf(
        cat(BuiltInCategory.SETTINGS, "Settings", 0),
        cat("photos", "Photo", 1),
        cat("music", "Music", 2),
        cat(BuiltInCategory.GAMES, "Game", 3, gaming = true),
    )

    @Test
    fun `reordering a built-in moves it on the bar`() {
        val stored = listOf(
            cat(BuiltInCategory.GAMES, "Game", 0, gaming = true),
            cat("photos", "Photo", 1),
            cat("music", "Music", 2),
            cat(BuiltInCategory.SETTINGS, "Settings", 3),
        )
        val bar = canonicalCrossbarCategories(stored, fallbacks).map { it.id }
        assertEquals(listOf(BuiltInCategory.GAMES, "photos", "music", BuiltInCategory.SETTINGS), bar)
    }

    @Test
    fun `renaming a built-in shows the new name on the bar`() {
        val stored = listOf(cat(BuiltInCategory.GAMES, "Emulation", 0, gaming = true))
        val games = canonicalCrossbarCategories(stored, fallbacks).first { it.id == BuiltInCategory.GAMES }
        assertEquals("Emulation", games.name)
    }

    @Test
    fun `a blank stored name falls back rather than leaving the bar unlabelled`() {
        val stored = listOf(cat(BuiltInCategory.GAMES, "   ", 0, gaming = true))
        val games = canonicalCrossbarCategories(stored, fallbacks).first { it.id == BuiltInCategory.GAMES }
        assertEquals("Game", games.name)
    }

    @Test
    fun `the icon stays canonical even when the stored row disagrees`() {
        val stored = listOf(cat(BuiltInCategory.GAMES, "Game", 0, iconKey = "ic_something_else", gaming = true))
        val games = canonicalCrossbarCategories(stored, fallbacks).first { it.id == BuiltInCategory.GAMES }
        assertEquals("ic_games", games.iconKey)
    }

    @Test
    fun `a hidden built-in is dropped from the bar`() {
        val stored = listOf(cat(BuiltInCategory.GAMES, "Game", 0, gaming = true))
        val bar = canonicalCrossbarCategories(stored, fallbacks).map { it.id }
        assertEquals(listOf(BuiltInCategory.GAMES), bar)
        assertTrue("a hidden built-in must not reach the bar", "photos" !in bar && "music" !in bar)
    }

    @Test
    fun `a hidden Settings leaves the bar, because the panel's Settings tab is the way back`() {
        val bar = canonicalCrossbarCategories(listOf(cat(BuiltInCategory.GAMES, "Game", 0, gaming = true)), fallbacks)
        assertTrue(BuiltInCategory.SETTINGS !in bar.map { it.id })
        assertTrue(
            "with Settings hidden, the panel must still reach the category manager or nothing hidden can come back",
            PANEL_SETTINGS.any { section -> com.echo.core.domain.model.settingsEntriesIn(section).any { it.id == "settings_categories" } },
        )
    }

    @Test
    fun `a custom category interleaves with built-ins by position`() {
        val stored = listOf(
            cat(BuiltInCategory.GAMES, "Game", 0, gaming = true),
            Category(
                id = "custom_android_10", name = "Android", iconKey = "ic_android",
                type = CategoryType.MANUAL, position = 1,
            ),
            cat("photos", "Photo", 2),
            cat(BuiltInCategory.SETTINGS, "Settings", 3),
        )
        val bar = canonicalCrossbarCategories(stored, fallbacks).map { it.id }
        assertEquals(
            listOf(BuiltInCategory.GAMES, "custom_android_10", "photos", BuiltInCategory.SETTINGS),
            bar,
        )
    }

    @Test
    fun `the gaming flag still comes from the database`() {
        val stored = listOf(cat("photos", "Photo", 0, gaming = true))
        val photos = canonicalCrossbarCategories(stored, fallbacks).first { it.id == "photos" }
        assertTrue(photos.isGamingCategory)
    }
}
