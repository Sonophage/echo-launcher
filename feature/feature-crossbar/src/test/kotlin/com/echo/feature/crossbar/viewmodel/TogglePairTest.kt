package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.ui.components.MenuGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Anything turned on from a menu can be turned off from the same menu: every on/off row shows the
// half of its pair that fits the item's current state (spec #2, ticket #15)
class TogglePairTest {
    private val games = Category(
        id = BuiltInCategory.GAMES, name = "Game", iconKey = "ic_games",
        type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true,
    )
    private val retro = Category(
        id = "retro", name = "Retro", iconKey = "retro", type = CategoryType.MANUAL, position = 1, isGamingCategory = true,
    )
    private val state = CrossbarUiState(categories = listOf(games, retro), selectedCategoryIndex = 1)

    private fun game(isFavorite: Boolean = false, pinned: Boolean = false) = CrossbarItem(
        id = "g1", title = "Crisis Core", gameId = 1L, platformId = "psp",
        isFavorite = isFavorite, pinnedInCategory = pinned,
    )

    private class Toggle(val name: String, val actions: Set<String>, val build: (on: Boolean) -> List<CrossbarContextMenuItem>)

    private val toggles = listOf(
        Toggle("game Favorites", setOf("favorite", "unfavorite")) { gameContextMenuItems(game(isFavorite = it), state, 1, false, null) },
        Toggle("app Favorites", setOf("favorite", "unfavorite")) { appContextMenuItems(state, null, false, "com.app", isFavorite = it) },
        Toggle("video Favorites", setOf("video_favorite", "video_unfavorite")) { videoFileContextMenuItems(isFavorite = it, 0, false, false) },
        Toggle("photo Favorites", setOf("photo_favorite", "photo_unfavorite")) { photoFileContextMenuItems(isFavorite = it) },
        Toggle("game Pin to Recent", setOf("pin_recent", "unpin_recent")) {
            gameContextMenuItems(game(), state.copy(recentPins = if (it) listOf("g:1") else emptyList()), 1, false, null)
        },
        Toggle("app Pin to Recent", setOf("pin_recent", "unpin_recent")) {
            appContextMenuItems(state.copy(recentPins = if (it) listOf("a:com.app") else emptyList()), null, false, "com.app")
        },
        Toggle("game pin in its column", setOf("pin_category", "unpin_category")) { gameContextMenuItems(game(pinned = it), state, 1, false, null) },
        Toggle("app Mark as Game", setOf("mark_game", "unmark_game")) { appContextMenuItems(state, null, false, "com.app", isGame = it) },
        Toggle("app Pin to Top", setOf("pin", "unpin")) { appContextMenuItems(state, "retro", false, "com.app", pinnedInCategory = it) },
    )

    // the off label names the action; the on label is the same words with the verb undone
    private fun undo(label: String): String = when {
        label.startsWith("Add to ") -> "Remove from " + label.removePrefix("Add to ")
        label.startsWith("Pin to ") -> "Unpin from " + label.removePrefix("Pin to ")
        label.startsWith("Mark as ") -> "Unmark as " + label.removePrefix("Mark as ")
        label.startsWith("Allow ") -> "Block " + label.removePrefix("Allow ")
        else -> error("'$label' is not the off half of a known pair")
    }

    // a rail outside the crossbar follows the same pairs (ticket #16). Not "Hide": CONTEXT.md keeps Hide for items
    @Test
    fun `Artwork Studio's Mature Content row shows its pair from the current state`() {
        val off = com.echo.feature.artwork.studio.ArtworkStudioUiState(includeNsfw = false)
            .labelOf(com.echo.feature.artwork.studio.StudioAction.TOGGLE_MATURE)
        val on = com.echo.feature.artwork.studio.ArtworkStudioUiState(includeNsfw = true)
            .labelOf(com.echo.feature.artwork.studio.StudioAction.TOGGLE_MATURE)
        assertEquals("the on state does not offer to undo '$off'", undo(off), on)
    }

    private fun Toggle.row(on: Boolean): CrossbarContextMenuItem {
        val rows = build(on).filter { it.action in actions }
        assertEquals("$name ${if (on) "on" else "off"}: expected one row of $actions, got ${rows.map { it.label }}", 1, rows.size)
        return rows.single()
    }

    @Test
    fun `every toggle row shows its pair from the current state`() {
        toggles.forEach { t ->
            val off = t.row(on = false)
            val on = t.row(on = true)
            assertEquals("${t.name}: the on state does not offer to undo '${off.label}'", undo(off.label), on.label)
            assertEquals("${t.name}: the two halves sit in different sections", off.group, on.group)
            // the action names the half, so a row chosen from a menu built before the state changed still does what it says
            assertTrue("${t.name}: both halves run '${off.action}'", off.action != on.action)
        }
    }

    @Test
    fun `Favorites is one pair of words under Organize on games, apps, videos and photos`() {
        toggles.filter { it.name.endsWith("Favorites") }.forEach { t ->
            assertEquals(t.name, "Add to Favorites", t.row(on = false).label)
            assertEquals(t.name, "Remove from Favorites", t.row(on = true).label)
            assertEquals(t.name, MenuGroup.ORGANIZE, t.row(on = true).group)
        }
    }

    // a disc set is one game on the crossbar, a favourite when any disc is, so its Favorites row changes every disc;
    // else Remove from Favorites can clear the shown disc and leave the set a favourite through another
    @Test
    fun `Remove from Favorites on a disc set takes the whole set off Favorites`() {
        val disc1 = com.echo.core.domain.model.Game(id = 1, title = "FF7", platformId = "psx", discSetKey = "ff7", discNumber = 1, isDiscPrimary = true)
        val disc2 = disc1.copy(id = 2, discNumber = 2, isDiscPrimary = false, isFavorite = true)
        val set = listOf(disc1, disc2)
        assertTrue("the set shows as a favourite", set.projectGamesForDisplay().single().isFavorite)

        val changed = favoriteIds(disc1.id, set)
        val after = set.map { if (it.id in changed) it.copy(isFavorite = false) else it }
        assertFalse("the set is still a favourite after Remove from Favorites", after.projectGamesForDisplay().single().isFavorite)
        assertEquals("a single game changes only itself", listOf(5L), favoriteIds(5L, emptyList()))
    }

    @Test
    fun `the Shelves picker holds play states only`() {
        val rows = shelvesPickerItems(current = null)
        assertFalse("Favorites is still a shelf in the picker: ${rows.map { it.label }}", rows.any { "Favorite" in it.label })
        assertTrue("every row is a play state or Unmarked", rows.all { it.action?.startsWith("pstate_") == true })
    }
}
