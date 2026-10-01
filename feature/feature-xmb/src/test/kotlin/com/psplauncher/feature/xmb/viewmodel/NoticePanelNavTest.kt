package com.psplauncher.feature.xmb.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticePanelNavTest {
    private fun move(c: PanelCursor, m: PanelMove, media: Boolean = true, rows: Int = 4, chips: Int = 6) =
        movePanel(c, m, hasMedia = media, rightRows = rows, chips = chips)

    @Test fun `down from now playing lands on the first quick setting`() {
        assertEquals(PanelCursor(quick = QuickSetting.WAVE), move(PanelCursor(notice = 0), PanelMove.DOWN))
    }

    @Test fun `up from the first quick setting goes back to now playing`() {
        assertEquals(PanelCursor(quick = null, notice = 0), move(PanelCursor(quick = QuickSetting.WAVE), PanelMove.UP))
    }

    @Test fun `right crosses from quick settings to the notification on the same row`() {
        val c = move(PanelCursor(quick = QuickSetting.BACKDROP), PanelMove.RIGHT)
        assertEquals(null, c.quick)
        assertEquals("row 2 of the left lands on the second notification", 2, c.notice)
    }

    @Test fun `left and right walk the library chips before leaving the row`() {
        val start = PanelCursor(quick = QuickSetting.LIBRARIES, chip = 0)
        assertEquals(1, move(start, PanelMove.RIGHT).chip)
        assertEquals("left on the first chip stays put", start, move(start, PanelMove.LEFT))
        val last = start.copy(chip = 5)
        assertEquals("the library bar spans the panel; right past the last chip stays put", last, move(last, PanelMove.RIGHT))
    }

    @Test fun `down from the last notification reaches the library bar`() {
        assertEquals(PanelCursor(quick = QuickSetting.LIBRARIES, chip = 0, notice = 3), move(PanelCursor(notice = 3), PanelMove.DOWN))
    }

    @Test fun `left from a notification returns to quick settings`() {
        assertEquals(QuickSetting.WAVE, move(PanelCursor(notice = 1), PanelMove.LEFT).quick)
    }

    @Test fun `with no notifications, right from quick settings stays put`() {
        val c = PanelCursor(quick = QuickSetting.WAVE)
        assertEquals(c, move(c, PanelMove.RIGHT, media = true, rows = 1))
    }

    @Test fun `without now playing, up from the first quick setting stays put`() {
        val c = PanelCursor(quick = QuickSetting.WAVE)
        assertEquals(c, move(c, PanelMove.UP, media = false, rows = 2))
    }
}
