package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticePanelNavTest {
    private fun move(c: PanelCursor, m: PanelMove, rows: Int = 4, chips: Int = 6) =
        movePanel(c, m, rows = rows, quicks = PANEL_QUICK_SETTINGS.size, chips = chips)

    @Test fun `the shoulder buttons walk the tabs and wrap at both ends`() {
        assertEquals("Profile sits right after Notifications", PanelTab.PROFILE, move(PanelCursor(), PanelMove.NEXT_TAB).tab)
        assertEquals(PanelTab.QUICK, move(PanelCursor(tab = PanelTab.PROFILE), PanelMove.NEXT_TAB).tab)
        assertEquals(PanelTab.NOTIFICATIONS, move(PanelCursor(tab = PanelTab.SETTINGS), PanelMove.NEXT_TAB).tab)
        assertEquals(PanelTab.SETTINGS, move(PanelCursor(), PanelMove.PREV_TAB).tab)
    }

    @Test fun `switching tab keeps where you were in the list`() {
        val back = move(move(PanelCursor(notice = 3), PanelMove.NEXT_TAB), PanelMove.PREV_TAB)
        assertEquals(PanelCursor(notice = 3), back)
    }

    @Test fun `down walks the list and holds on the last row`() {
        assertEquals(1, move(PanelCursor(notice = 0), PanelMove.DOWN).notice)
        assertEquals(3, move(PanelCursor(notice = 3), PanelMove.DOWN).notice)
        assertEquals(0, move(PanelCursor(notice = 0), PanelMove.UP).notice)
    }

    @Test fun `a cursor left past a shrunk list moves from the last real row`() {
        assertEquals("dismissing rows must not strand the cursor below the list", 1, move(PanelCursor(notice = 9), PanelMove.UP, rows = 3).notice)
    }

    @Test fun `left and right walk the quick tiles and stop at the edges`() {
        val q = PanelCursor(tab = PanelTab.QUICK)
        assertEquals(1, move(q, PanelMove.RIGHT).quick)
        assertEquals(q, move(q, PanelMove.LEFT))
        val last = q.copy(quick = PANEL_QUICK_SETTINGS.lastIndex)
        assertEquals(last, move(last, PanelMove.RIGHT))
        assertEquals("up and down have nothing to reach on one row of tiles", q, move(q, PanelMove.DOWN))
    }

    @Test fun `the library grid moves by row and column and holds at its edges`() {
        val g = PanelCursor(tab = PanelTab.LIBRARIES, chip = 2)
        assertEquals("right off the end of a row must not wrap onto the next row", 2, move(g, PanelMove.RIGHT).chip)
        assertEquals(5, move(g, PanelMove.DOWN).chip)
        assertEquals(5, move(g.copy(chip = 5), PanelMove.DOWN).chip)
        assertEquals(1, move(g.copy(chip = 4), PanelMove.UP).chip)
        assertEquals(3, move(g.copy(chip = 3), PanelMove.LEFT).chip)
    }

    @Test fun `a short last row of libraries cannot be stepped past`() {
        val g = PanelCursor(tab = PanelTab.LIBRARIES, chip = 3)
        assertEquals(4, move(g, PanelMove.RIGHT, chips = 5).chip)
        assertEquals(4, move(g.copy(chip = 4), PanelMove.RIGHT, chips = 5).chip)
        assertEquals("down from a column with no tile below stays put", 1, move(g.copy(chip = 1), PanelMove.DOWN, chips = 4).chip)
    }

    // owner, 2026-10-05: a seventh section (Media) made the grid four wide, so it still fits in two rows
    @Test fun `the settings grid is four wide and reaches every section`() {
        val g = PanelCursor(tab = PanelTab.SETTINGS, setting = 3)
        assertEquals("right off the end of a row must not wrap", 3, move(g, PanelMove.RIGHT).setting)
        assertEquals(6, move(PanelCursor(tab = PanelTab.SETTINGS, setting = 2), PanelMove.DOWN).setting)
        var c = PanelCursor(tab = PanelTab.SETTINGS, setting = PANEL_SETTINGS.lastIndex % SETTINGS_GRID_COLUMNS)
        repeat(PANEL_SETTINGS.size) { c = move(c, PanelMove.DOWN) }
        assertEquals("the last section must be reachable", PANEL_SETTINGS.lastIndex, c.setting)
    }

    @Test fun `a settings tile is a section and opens that section's first screen`() {
        assertEquals("one tile per section, in section order", com.echo.core.domain.model.SettingsSectionId.entries, PANEL_SETTINGS)
        PANEL_SETTINGS.forEachIndexed { i, section ->
            val opens = panelSettingScreen(i)
            assertEquals(
                "the tab row starts on the first tab, so the tile must open it",
                com.echo.core.domain.model.settingsEntriesIn(section).first().id,
                opens,
            )
            assertEquals(true, opens in com.echo.feature.settings.ui.SETTINGS_SCREEN_ROUTES)
        }
        assertEquals(null, panelSettingScreen(PANEL_SETTINGS.size))
    }
}
