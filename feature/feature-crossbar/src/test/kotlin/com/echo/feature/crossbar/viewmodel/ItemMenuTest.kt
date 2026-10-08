package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.ui.components.rowsShown
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-06: a row has no pill row under it any more; what its pills did is in its menu
class ItemMenuTest {
    private fun state() = CrossbarUiState(
        categories = listOf(
            Category(
                id = BuiltInCategory.GAMES, name = "Game", iconKey = "ic_games",
                type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true,
            ),
        ),
        selectedCategoryIndex = 0,
    )

    private fun game(androidApp: Boolean = false) = CrossbarItem(
        id = "g1", title = "Crisis Core", gameId = 1L, platformId = "psp",
        isAndroidApp = androidApp, packageName = if (androidApp) "com.example.game" else null,
    )

    private fun gameMenu(item: CrossbarItem) = gameContextMenuItems(
        item = item, state = state(), discCount = 1, onRecentShelf = false, hideLocation = null,
    )

    @Test
    fun `a game's open menu shows Shelves, which the pill row used to hold back`() {
        val open = state().copy(
            currentItems = listOf(game()),
            activeContextMenu = CrossbarContextMenu(state = com.echo.core.ui.components.MenuState(title = "Crisis Core", rows = gameMenu(game()))),
        )
        val shown = open.menuRows().mapNotNull { it.action }
        assertTrue("Shelves is not on the open menu: $shown", "shelves" in shown)
    }

    @Test
    fun `an app's menu offers what its pills did, Launch, Edit and Favorite`() {
        val menu = appContextMenuItems(state(), categoryId = null, onRecentShelf = false)
        val shown = com.echo.core.ui.components.MenuState(title = "Spotify", rows = menu).rowsShown().mapNotNull { it.action }
        listOf("launch", "favorite").forEach { assertTrue("'$it' is not on the app menu's first level: $shown", it in shown) }
        // owner, 2026-10-08: Artwork through Artwork Studio replaces the old Edit App Details
        assertTrue("Artwork is on the app menu", menu.any { it.action == "app_artwork" })
        assertTrue("the old editor is gone", menu.none { it.action == "edit_app" })
        val withPackage = appContextMenuItems(state(), categoryId = null, onRecentShelf = true, packageName = "com.spotify.music")
        assertTrue("App Info and Uninstall are offered", withPackage.any { it.action == "app_info" } && withPackage.any { it.action == "uninstall" })
        assertTrue("Uninstall asks first", withPackage.first { it.action == "uninstall" }.isDestructive)
    }

    @Test
    fun `a package-backed game is offered no emulator to change`() {
        assertTrue("change_emulator" !in gameMenu(game(androidApp = true)).mapNotNull { it.action })
        assertTrue("change_emulator" in gameMenu(game(androidApp = false)).mapNotNull { it.action })
    }
}
