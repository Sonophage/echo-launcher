package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown
import com.echo.core.ui.components.foldedIntoGroups

class PillActionsTest {
    private fun state() = CrossbarUiState(
        categories = listOf(
            Category(
                id = BuiltInCategory.GAMES, name = "Game", iconKey = "ic_games",
                type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true,
            ),
        ),
        selectedCategoryIndex = 0,
    )

    private fun game(isFavorite: Boolean = false, androidApp: Boolean = false) = CrossbarItem(
        id = "g1", title = "Crisis Core", gameId = 1L, platformId = "psp",
        isFavorite = isFavorite, isAndroidApp = androidApp,
        packageName = if (androidApp) "com.example.game" else null,
    )

    private fun app() = CrossbarItem(
        id = "a1", title = "Spotify", packageName = "com.spotify.music",
    )

    private fun gameMenuIds(item: CrossbarItem) = gameContextMenuItems(
        item = item,
        state = state(),
        discCount = 1,
        onRecentShelf = false,
        hideLocation = null,
    ).mapNotNull { it.action }

    @Test
    fun `every game pill is a row the game menu offers`() {
        listOf(game(), game(isFavorite = true), game(androidApp = true)).forEach { item ->
            val menu = gameMenuIds(item)
            pillsFor(item).forEach { pill ->
                assertTrue(
                    "pill '${pill.label}' dispatches '${pill.id}', which the game menu does not offer: $menu",
                    pill.id in menu,
                )
            }
        }
    }

    @Test
    fun `every app pill is a row the app menu offers`() {
        val menu = appContextMenuItems(state(), categoryId = null, onRecentShelf = false).mapNotNull { it.action }
        pillsFor(app()).forEach { pill ->
            assertTrue(
                "pill '${pill.label}' dispatches '${pill.id}', which the app menu does not offer: $menu",
                pill.id in menu,
            )
        }
    }

    @Test
    fun `the menu cannot address a pill, so a menu index cannot either`() {
        val item    = game()
        val menu    = gameContextMenuItems(item, state(), discCount = 1, onRecentShelf = false, hideLocation = null)
        val pillIds = pillsFor(item).map { it.id }.toSet()
        val rows    = MenuState("Gran Turismo 4", menu, withheld = pillIds).rowsShown()

        assertTrue(
            "the menu drew a pill's own action: ${rows.mapNotNull { it.action }.filter { it in pillIds }}",
            rows.none { it.action in pillIds },
        )

        val firstPill = pillsFor(item).first()
        val atThatIndex = rows.getOrNull(menu.indexOfFirst { it.action == firstPill.id })?.action
        assertTrue(
            "activating by index would have run '$atThatIndex' for the '${firstPill.label}' pill",
            atThatIndex != firstPill.id,
        )
    }

    @Test
    fun `the pill row is never visible on the home shelf`() {
        val onShelf = CrossbarUiState(
            categories = listOf(
                Category(
                    id = BuiltInCategory.RECENTLY_PLAYED, name = "Last Played", iconKey = "ic_recent",
                    type = CategoryType.BUILT_IN, position = 0,
                ),
            ),
            selectedCategoryIndex = 0,
            currentItems = listOf(game()),
            selectedItemIndex = 0,
        )
        assertTrue("the fixture is not on the shelf", onShelf.onLastPlayedHome)
        assertTrue("this row does have pills", pillsFor(game()).isNotEmpty())
        assertFalse("a door was offered into a row that is not drawn", onShelf.pillRowVisible)
    }

    @Test
    fun `the game pill row is exactly Play then Shelves`() {
        assertEquals(listOf("play", "shelves"), pillsFor(game()).map { it.id })
        assertEquals(listOf("Play", "Shelves"), pillsFor(game()).map { it.label })
    }

    @Test
    fun `the pill row no longer changes with the row's favourite state`() {
        assertEquals(
            "Favourite is chosen inside the Shelves picker now, so the row must not restyle itself",
            pillsFor(game(isFavorite = false)),
            pillsFor(game(isFavorite = true)),
        )
    }

    @Test
    fun `a package-backed game is offered no emulator to change`() {
        assertTrue("change_emulator" !in gameMenuIds(game(androidApp = true)))
        assertTrue("change_emulator" in gameMenuIds(game(androidApp = false)))
    }

    @Test
    fun `rows with no pills get no row at all`() {
        val platformCard = CrossbarItem(id = "card_psp", title = "PSP", platformId = "psp")
        val settingsRow = CrossbarItem(id = "settings_open", title = "Settings")
        val track = CrossbarItem(id = "t1", title = "Blue Monday", type = CrossbarItemType.MUSIC_TRACK)
        listOf(platformCard, settingsRow, track).forEach {
            assertEquals("${'$'}{it.id} must draw no pill row", emptyList<CrossbarPill>(), pillsFor(it))
        }
    }
}
