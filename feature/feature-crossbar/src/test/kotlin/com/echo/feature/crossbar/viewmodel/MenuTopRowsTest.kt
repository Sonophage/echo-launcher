package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown
import com.echo.core.ui.components.submenuFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// App Info is in About, at the top of the menu; Open Folder is in Settings (owner, 2026-10-12). This replaces the
// 2026-10-10 rule that put both at the top
class MenuTopRowsTest {
    private val state = CrossbarUiState(
        categories = BUILT_IN_CATEGORIES,
        selectedCategoryIndex = BUILT_IN_CATEGORIES.indexOfFirst { it.id == BuiltInCategory.GAMES },
    )

    private fun top(rows: List<CrossbarContextMenuItem>) = MenuState(title = "x", rows = rows).rowsShown().mapNotNull { it.action }

    private fun inSettings(rows: List<CrossbarContextMenuItem>) =
        MenuState(title = "x", rows = rows).submenuFor(MenuGroup.SETTINGS)?.rowsShown()?.mapNotNull { it.action }.orEmpty()

    @Test
    fun `a ROM game offers Open Folder in Settings, not at the top`() {
        val game = CrossbarItem(id = "g1", title = "Tetris", gameId = 1L, platformId = "gb", romPath = "/storage/emulated/0/ROMs/gb/Tetris.gb")
        val rows = gameContextMenuItems(game, state, discCount = 1, onRecentShelf = false, hideLocation = null)
        assertTrue("Open Folder is at the top: ${top(rows)}", "open_folder" !in top(rows))
        assertTrue("Open Folder is not in Settings: ${inSettings(rows)}", "open_folder" in inSettings(rows))
    }

    // the row opens the game's folder (owner, 2026-10-12); a game with no file has no folder, so no row that could
    // only show a package name
    @Test
    fun `a game with no file, such as a PC or Android game, has no Open Folder row`() {
        listOf(
            CrossbarItem(id = "g3", title = "Hades", gameId = 3L, platformId = com.echo.core.domain.model.PlatformIds.WINDOWS),
            CrossbarItem(id = "g2", title = "Genshin", gameId = 2L, platformId = "android", packageName = "com.game", isAndroidApp = true),
        ).forEach { game ->
            val rows = gameContextMenuItems(game, state, discCount = 1, onRecentShelf = false, hideLocation = null)
            assertTrue("${game.title} offers Open Folder with no folder to open", rows.none { it.action == "open_folder" })
        }
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
