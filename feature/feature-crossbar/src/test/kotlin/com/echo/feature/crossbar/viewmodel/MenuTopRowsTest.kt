package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-10: a ROM game's menu opens its folder, and an app's shows Android's App Info, both at the top of
// the menu rather than inside Settings
class MenuTopRowsTest {
    private val state = CrossbarUiState(
        categories = BUILT_IN_CATEGORIES,
        selectedCategoryIndex = BUILT_IN_CATEGORIES.indexOfFirst { it.id == BuiltInCategory.GAMES },
    )

    private fun top(rows: List<CrossbarContextMenuItem>) = MenuState(title = "x", rows = rows).rowsShown().mapNotNull { it.action }

    @Test
    fun `a ROM game offers Open Folder at the top`() {
        val game = CrossbarItem(id = "g1", title = "Tetris", gameId = 1L, platformId = "gb")
        val shown = top(gameContextMenuItems(game, state, discCount = 1, onRecentShelf = false, hideLocation = null))
        assertTrue("Open Folder is not at the top: $shown", "open_folder" in shown)
    }

    @Test
    fun `an app offers App Info at the top`() {
        val shown = top(appContextMenuItems(state, categoryId = null, onRecentShelf = false, packageName = "com.app"))
        assertTrue("App Info is not at the top: $shown", "app_info" in shown)
    }

    @Test
    fun `an Android game offers App Info at the top`() {
        val game = CrossbarItem(id = "g2", title = "Genshin", gameId = 2L, platformId = "android", packageName = "com.game", isAndroidApp = true)
        val shown = top(gameContextMenuItems(game, state, discCount = 1, onRecentShelf = false, hideLocation = null))
        assertTrue("App Info is not at the top: $shown", "app_info" in shown)
    }
}
