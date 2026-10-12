package com.echo.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuTest {
    private enum class Act { PLAY, DETAILS, FAVOURITE, MARK, COLLECT, NOTE, EMULATOR, ICONS, LOCATION, MOVE, HIDE, DELETE, UNLINK }

    private fun row(
        action: Act,
        group: MenuGroup = MenuGroup.DO,
        destructive: Boolean = false,
        confirms: Boolean = destructive,
    ) = MenuRow(action, action.name, group, destructive, confirms)

    // the source order is shuffled on purpose: the fold, not the builder, decides the order
    private val fullMenu = listOf(
        row(Act.DETAILS, MenuGroup.ABOUT),
        row(Act.HIDE, MenuGroup.REMOVE),
        row(Act.FAVOURITE, MenuGroup.ORGANIZE),
        row(Act.MARK, MenuGroup.ORGANIZE),
        row(Act.COLLECT, MenuGroup.ORGANIZE),
        row(Act.MOVE, MenuGroup.EDIT_LAYOUT),
        row(Act.NOTE, MenuGroup.EDIT),
        row(Act.EMULATOR, MenuGroup.SETTINGS),
        row(Act.ICONS, MenuGroup.SETTINGS),
        // declared in Settings, but a destructive row always goes inside Remove
        row(Act.DELETE, MenuGroup.SETTINGS, destructive = true),
        row(Act.LOCATION, MenuGroup.SETTINGS),
        row(Act.PLAY),
    )

    private fun menu(rows: List<MenuRow<Act>> = fullMenu) = MenuState("Gran Turismo 4", rows)

    private fun shown(state: MenuState<Act>) = state.rowsShown().map { it.action?.name ?: it.label }

    @Test
    fun `sections come in order, Do and About at the top level, the rest folded unless they hold one row`() {
        assertEquals(
            listOf("PLAY", "DETAILS", "Organize", "NOTE", "Settings", "MOVE", "Remove"),
            shown(menu()),
        )
    }

    @Test
    fun `the five submenus carry their section names`() {
        assertEquals(
            listOf(null, null, "Organize", "Edit", "Settings", "Edit Layout", "Remove"),
            MenuGroup.entries.map { it.label },
        )
    }

    @Test
    fun `one divider is drawn after the top-level rows and nowhere else`() {
        assertEquals("the divider goes before the first folded section", 2, menu().dividerIndex())

        val flat = MenuState("Choose Disc", listOf(row(Act.PLAY), row(Act.COLLECT)))
        assertNull("a flat list has nothing to divide", flat.dividerIndex())

        val noTopLevel = MenuState("t", listOf(row(Act.NOTE, MenuGroup.EDIT), row(Act.HIDE, MenuGroup.REMOVE)))
        assertNull("a divider with nothing above it divides nothing", noTopLevel.dividerIndex())

        assertNull("a submenu draws no divider", menu().submenuFor(MenuGroup.SETTINGS)!!.dividerIndex())
    }

    @Test
    fun `the divider is not a row, so a drawn index still names the row drawn there`() {
        val state = menu()
        assertEquals(7, state.rowsShown().size)
        assertEquals(MenuSelect.Run(Act.NOTE), state.chose(shown(state).indexOf("NOTE")))
    }

    @Test
    fun `a destructive row sits inside Remove and still asks first`() {
        assertTrue("a destructive row was left at the top level", shown(menu()).none { it == "DELETE" })

        val remove = menu().chose(shown(menu()).indexOf("Remove"))
        assertEquals(
            "the destructive row is not in the Remove submenu",
            listOf("HIDE", "DELETE"),
            (remove as? MenuSelect.Replace)?.state?.let(::shown),
        )
        remove as MenuSelect.Replace

        val confirm = (remove.state.chose(1) as MenuSelect.Replace).state
        assertEquals(listOf(CONFIRM_CANCEL_LABEL, "DELETE"), confirm.rowsShown().map { it.label })
        assertEquals(0, confirm.selectedIndex)
        assertEquals("DELETE?", confirm.subtitle)
    }

    @Test
    fun `the drawn index does not address the source list once a destructive row moves`() {
        val rows = listOf(
            MenuRow("keep", "Keep"),
            MenuRow("delete", "Delete", isDestructive = true, confirms = false),
            MenuRow("info", "Info"),
        )
        val state = MenuState("t", rows)

        assertEquals(listOf("Keep", "Info", "Delete"), state.rowsShown().map { it.label })

        // Drawn row 1 is Info, but rows[1] is the delete. A call site that does
        // sourceList[index] instead of chose(index) runs the delete on an Info tap.
        assertEquals("info", (state.chose(1) as MenuSelect.Run).action)
        assertEquals("delete", rows[1].action)
    }

    @Test
    fun `a withheld action is drawn nowhere, so a duplicate cannot be reached twice`() {
        val state = menu().copy(withheld = setOf(Act.FAVOURITE, Act.MARK))

        assertTrue("a withheld row was drawn", shown(state).none { it == "FAVOURITE" || it == "MARK" })
        assertEquals(
            "with MARK withheld the Organize section is down to one row and should show it inline",
            "COLLECT",
            shown(state)[2],
        )
    }

    @Test
    fun `choosing a folded row opens its section, keeping the object's title`() {
        val opened = menu().chose(shown(menu()).indexOf("Settings"))

        assertTrue(opened is MenuSelect.Replace)
        val submenu = (opened as MenuSelect.Replace).state
        assertEquals(listOf("EMULATOR", "ICONS", "LOCATION"), shown(submenu))
        assertEquals("Gran Turismo 4", submenu.title)
        assertEquals("Settings", submenu.subtitle)
        assertEquals(0, submenu.selectedIndex)
    }

    @Test
    fun `a submenu does not fold again, which would hide its rows behind themselves`() {
        val submenu = menu().submenuFor(MenuGroup.SETTINGS)!!
        assertEquals(3, submenu.rowsShown().size)
        assertTrue("a submenu folded itself", submenu.rowsShown().none { it.opensSubmenu })
    }

    @Test
    fun `answering yes runs the action instead of asking again`() {
        val remove = menu().submenuFor(MenuGroup.REMOVE)!!
        val confirm = (remove.chose(shown(remove).indexOf("DELETE")) as MenuSelect.Replace).state
        assertEquals(MenuSelect.Run(Act.DELETE), confirm.chose(1))
    }

    @Test
    fun `answering no climbs back to the menu it came from`() {
        val remove = menu().submenuFor(MenuGroup.REMOVE)!!
        val confirm = (remove.chose(shown(remove).indexOf("DELETE")) as MenuSelect.Replace).state
        val cancelled = confirm.chose(0)

        assertTrue(cancelled is MenuSelect.Replace)
        assertEquals(shown(remove), shown((cancelled as MenuSelect.Replace).state))
    }

    @Test
    fun `a row that only unlinks runs straight away`() {
        val rows = listOf(row(Act.UNLINK, MenuGroup.REMOVE, destructive = true, confirms = false))
        assertEquals(MenuSelect.Run(Act.UNLINK), MenuState("Playlist", rows).chose(0))
    }

    @Test
    fun `Back climbs one level and then reports there is nowhere left to go`() {
        val submenu = menu().submenuFor(MenuGroup.ORGANIZE)!!
        assertEquals("Gran Turismo 4", submenu.back()?.title)
        assertNull("Back at the root should close rather than loop", menu().back())
    }

    @Test
    fun `the cursor enters from the end it was moved from and stops at the edges`() {
        val root = menu()
        assertEquals(0, root.moved(+1).selectedIndex)
        assertEquals(root.rowsShown().lastIndex, root.moved(-1).selectedIndex)

        val atTop = root.at(0)
        assertEquals("the cursor wrapped off the top", 0, atTop.moved(-1).selectedIndex)

        val atEnd = root.at(root.rowsShown().lastIndex)
        assertEquals("the cursor wrapped off the bottom", atEnd.selectedIndex, atEnd.moved(+1).selectedIndex)
    }
}
