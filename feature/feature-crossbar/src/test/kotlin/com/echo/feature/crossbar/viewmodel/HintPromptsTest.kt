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
    // owner, 2026-10-06: LB is Apps and B is only ever Back
    fun `the root offers Apps on LB, not on BACK, so a touch back arrow only ever goes back`() {
        assertEquals("Apps", promptsFor(state()).back.verb)
        assertEquals(GamepadAction.PREV_PAGE, promptsFor(state()).back.action)
        assertEquals("Back", promptsFor(state(drilled = "psp")).back.verb)
        assertEquals(GamepadAction.BACK, promptsFor(state(drilled = "psp")).back.action)
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
        assertTrue("RB is Search", promptsFor(state()).right.any { it.verb == "Search" && it.action == GamepadAction.NEXT_PAGE })
        assertTrue(promptsFor(state(drilled = "psp")).right.none { it.verb == "Search" })
    }

    @Test
    fun `the footer never repeats Sort or Filter, which the top bar already shows with their buttons`() {
        val right = promptsFor(state()).right.map { it.verb }
        assertTrue("Sort or Filter repeated in the footer: $right", right.none { it == "Sort" || it == "Filter" })
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

class LastPlayedPromptsTest {
    private fun home(item: CrossbarItem, rail: Boolean = false) = CrossbarUiState(
        categories = listOf(
            Category(id = BuiltInCategory.RECENTLY_PLAYED, name = "Recent", iconKey = "ic_recent", type = CategoryType.BUILT_IN, position = 0),
        ),
        currentItems = listOf(item),
        recentRailVisible = rail,
    )

    private val skyrim = CrossbarItem(id = "g", title = "Skyrim", gameId = 1L, isRealGame = true)

    @Test
    fun `the action orb carries the stage's verb, since the page has no buttons of its own`() {
        val prompts = promptsFor(home(skyrim))
        assertEquals("Continue", prompts.primary?.verb)
        assertEquals("Skyrim", prompts.primary?.target)
    }

    @Test
    fun `X opens info on the stage and removes the row in the rail`() {
        assertTrue(promptsFor(home(skyrim)).right.any { it.action == GamepadAction.CHANGE_SORT && it.verb == "Game info" })
        val app = CrossbarItem(id = "a", title = "Discord", packageName = "com.discord")
        assertTrue(promptsFor(home(app)).right.any { it.verb == "App info" })
    }

    @Test
    // owner, 2026-10-06: Y is the context menu, so Resume is Y held and a tap of Y still opens the menu
    fun `the game ECHO just sent away splits the orb into Y Resume and A Play, a new session`() {
        val back = promptsFor(home(skyrim).copy(resumeGameId = 1L))
        assertEquals("Resume", back.resume?.verb)
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, back.resume?.action)
        assertEquals("Play", back.primary?.verb)
        assertEquals("New session", back.primary?.target)

        val other = promptsFor(home(skyrim).copy(resumeGameId = 2L))
        assertNull("another game's session is not this one's to resume", other.resume)
    }

    // owner, 2026-10-05: while the notification card has the pad, the footer is the card's
    @Test
    fun `with the card pinned the footer offers Open, Dismiss and Close for the row the pad is on`() {
        val notices = listOf(
            com.echo.core.ui.notification.AndroidNotice(key = "old", appLabel = "Mail", title = "a", text = null, postedAt = 1L, canDismiss = true),
            com.echo.core.ui.notification.AndroidNotice(key = "new", appLabel = "Chat", title = "b", text = null, postedAt = 2L, canDismiss = false),
        )
        val pinned = CrossbarUiState(androidNotices = notices, noticeCardOut = true, noticeCardPinned = true, noticeCardCursor = 0)
        val p = promptsFor(pinned)
        assertEquals("the newest row is first", "Chat", p.primary?.target)
        assertEquals("Close", p.back.verb)
        assertTrue("a row that cannot be dismissed offers no Dismiss", p.right.none { it.action == GamepadAction.CHANGE_SORT })
        val second = promptsFor(pinned.copy(noticeCardCursor = 1))
        assertEquals("Mail", second.primary?.target)
        assertTrue(second.right.any { it.action == GamepadAction.CHANGE_SORT && it.verb == "Dismiss" })
    }
}
