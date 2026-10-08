package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Move (owner, 2026-10-07) lifts one row or column and the d-pad carries it. A lifted system must not
// pass All Games or cross the pinned systems, and a column must skip the ones the owner cannot reach
// (Last Played on the Thor) instead of stopping at them.
class MoveStepTest {
    private val gamesColumn = listOf(null, "pinned", "systems", "systems", "systems", null)

    @Test
    fun `a lifted system moves among the systems and stops at the column's fixed rows`() {
        assertEquals(3, liftedRowStep(gamesColumn, at = 2, delta = +1))
        assertEquals(4, liftedRowStep(gamesColumn, at = 3, delta = +1))
        assertNull("not onto the Folders row", liftedRowStep(gamesColumn, at = 4, delta = +1))
    }

    @Test
    fun `an unpinned system does not pass a pinned one`() {
        assertNull(liftedRowStep(gamesColumn, at = 2, delta = -1))
    }

    @Test
    fun `a row with no group cannot be lifted`() {
        assertNull(liftedRowStep(gamesColumn, at = 0, delta = +1))
    }

    @Test
    fun `a lifted column skips a column the owner cannot reach`() {
        val reachable = listOf(true, false, true, true)
        assertEquals(2, liftedColumnStep(reachable, at = 0, delta = +1))
        assertEquals(0, liftedColumnStep(reachable, at = 2, delta = -1))
        assertNull(liftedColumnStep(reachable, at = 3, delta = +1))
    }

    @Test
    fun `swapping trades two places and leaves the rest`() {
        assertEquals(listOf("a", "c", "b", "d"), listOf("a", "b", "c", "d").swapped(1, 2))
    }
}

// Change Icon (owner, 2026-10-07) opens Custom Icons on the slot the row or column draws, not on the first
// slot of the first group, so the owner lands on the icon he asked to change.
class ChangeIconSlotTest {
    private val groups = listOf(
        com.echo.themekit.IconSlot.Group.CATEGORY_BAR,
        com.echo.themekit.IconSlot.Group.ITEMS,
        com.echo.themekit.IconSlot.Group.STATUS,
        com.echo.themekit.IconSlot.Group.CONSOLE,
    )

    @Test
    fun `a system opens on its console icon`() {
        val (group, slot) = customIconStart("sysicon_nds", groups)
        assertEquals(3, group)
        assertEquals("sysicon_nds", com.echo.themekit.CustomizableIcons.group(groups[group])[slot].key)
    }

    @Test
    fun `a column opens on its category bar icon`() {
        val (group, slot) = customIconStart("catbar_music", groups)
        assertEquals(0, group)
        assertEquals("catbar_music", com.echo.themekit.CustomizableIcons.group(groups[group])[slot].key)
    }

    @Test
    fun `an unknown key opens on the first slot`() {
        assertEquals(0 to 0, customIconStart("nope", groups))
        assertEquals(0 to 0, customIconStart(null, groups))
    }

    @Test
    fun `only a system row has a console icon slot`() {
        val card = CrossbarItem(id = "card_nds", title = "NDS", type = CrossbarItemType.MEMORY_CARD, platformId = "nds")
        assertEquals("sysicon_nds", systemIconSlot(card))
        assertNull(systemIconSlot(card.copy(platformId = "not_a_console")))
        assertNull(systemIconSlot(CrossbarItem(id = "g", title = "Game", type = CrossbarItemType.STANDARD, platformId = "nds")))
    }
}
