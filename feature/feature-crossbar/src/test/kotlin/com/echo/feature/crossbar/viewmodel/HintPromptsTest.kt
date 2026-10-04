package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.CategoryType
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown
import com.echo.core.ui.components.foldedIntoGroups
import com.echo.core.ui.components.submenuFor

class HintPromptsTest {
    private fun state(
        items: List<CrossbarItem> = listOf(CrossbarItem(id = "g", title = "All Games", type = CrossbarItemType.ALL_GAMES)),
        selected: Int = 0,
        menu: CrossbarContextMenu? = null,

        drilled: String? = null,
    ) = CrossbarUiState(
        categories = listOf(
            Category(
                id = BuiltInCategory.GAMES, name = "Game", iconKey = "ic_games",
                type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true,
            ),
        ),
        currentItems = items,
        selectedItemIndex = selected,
        activeContextMenu = menu,
        selectedPlatformId = drilled,
    )

    @Test
    fun `the primary names what it acts on`() {
        val p = promptsFor(state()).primary
        assertEquals("Open", p?.verb)
        assertEquals("All Games", p?.target)
        assertEquals(GamepadAction.SELECT, p?.action)
    }

    @Test
    fun `the tab's detail is the focused item's own subtitle`() {
        val game = listOf(CrossbarItem(id = "1", title = "Crisis Core", gameId = 1L, subtitle = "PSP"))
        assertEquals("PSP", promptsFor(state(items = game)).primary?.detail)
    }

    @Test
    fun `a game's verb follows direct launch, because that setting IS the question`() {
        val game = listOf(CrossbarItem(id = "1", title = "Crisis Core", gameId = 1L))
        assertEquals("Play", promptsFor(state(items = game)).primary?.verb)
    }

    @Test
    fun `each kind of row gets its own verb, not a generic Open`() {
        fun verb(item: CrossbarItem) = promptsFor(state(items = listOf(item))).primary?.verb
        assertEquals("Play", verb(CrossbarItem(id = "t", title = "T", type = CrossbarItemType.MUSIC_TRACK)))
        assertEquals("Play", verb(CrossbarItem(id = "v", title = "V", type = CrossbarItemType.VIDEO_FILE)))
        assertEquals("Read", verb(CrossbarItem(id = "b", title = "B", type = CrossbarItemType.LIBRARY_BOOK)))
        assertEquals("View", verb(CrossbarItem(id = "p", title = "P", type = CrossbarItemType.PHOTO_FILE)))
        assertEquals("Launch", verb(CrossbarItem(id = "a", title = "A", packageName = "com.x")))
        assertEquals("Open", verb(CrossbarItem(id = "c", title = "C", type = CrossbarItemType.COLLECTION)))
    }

    @Test
    fun `an empty column promises nothing`() {
        val empty = listOf(CrossbarItem(id = "e", title = "Nothing here yet", type = CrossbarItemType.EMPTY))
        assertNull(promptsFor(state(items = empty)).primary)
    }

    @Test
    fun `back is named after what it DOES, which at the root is opening the drawer`() {
        assertEquals("Apps", promptsFor(state()).back.verb)
        assertEquals("Back", promptsFor(state(drilled = "psp")).back.verb)
    }

    @Test
    fun `the rail owns the bar while it is open`() {
        val menu = CrossbarContextMenu(state = MenuState(title = "Crisis Core", rows = listOf(
                CrossbarContextMenuItem("icon_display", "Icon Display"),
                CrossbarContextMenuItem("file_location", "View File Location"),
            ), selectedIndex = 1))
        val prompts = promptsFor(state(menu = menu))
        assertEquals("Select", prompts.primary?.verb)
        assertEquals("View File Location", prompts.primary?.target)
        assertEquals("Close", prompts.back.verb)
        assertTrue("the rail's own actions must not be repeated on the right", prompts.right.isEmpty())
    }

    @Test
    fun `Search is offered at the root and withdrawn inside a drill`() {
        assertTrue(promptsFor(state()).right.any { it.verb == "Search" })
        assertTrue(promptsFor(state(drilled = "psp")).right.none { it.verb == "Search" })
    }

    @Test
    fun `Sort and Filter are one button and never both`() {
        val right = promptsFor(state()).right.map { it.verb }
        assertTrue("Sort and Filter both offered: $right", right.count { it == "Sort" || it == "Filter" } <= 1)
    }

    @Test
    fun `inside a submenu Back climbs a level, and the hint says so`() {
        val root = CrossbarContextMenu(state = MenuState(title = "Gran Turismo 4", rows = listOf(
                CrossbarContextMenuItem("icon_display", "Icon Display", group = MenuGroup.SETTINGS),
                CrossbarContextMenuItem("file_location", "View File Location", group = MenuGroup.SETTINGS),
            )))
        assertEquals("Close", promptsFor(state(menu = root)).back.verb)

        val submenu = root.copy(state = root.state.submenuFor(MenuGroup.SETTINGS)!!)
        assertEquals(
            "Back would close the whole menu while claiming to close it, losing the root",
            "Back",
            promptsFor(state(menu = submenu)).back.verb,
        )
    }
}
