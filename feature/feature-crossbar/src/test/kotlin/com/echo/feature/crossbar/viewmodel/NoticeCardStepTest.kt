package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-05: on the notification card, up and down pick, A opens the app, X dismisses, B closes
class NoticeCardStepTest {
    @Test
    fun `up and down walk the rows and stop at the ends`() {
        assertEquals(NoticeCardStep.Move(1), noticeCardStep(GamepadAction.NAVIGATE_DOWN, cursor = 0, rows = 3))
        assertEquals(NoticeCardStep.Stay, noticeCardStep(GamepadAction.NAVIGATE_DOWN, cursor = 2, rows = 3))
        assertEquals(NoticeCardStep.Stay, noticeCardStep(GamepadAction.NAVIGATE_UP, cursor = 0, rows = 3))
    }

    @Test
    fun `A opens and X dismisses the row the pad is on`() {
        assertEquals(NoticeCardStep.Open(1), noticeCardStep(GamepadAction.SELECT, cursor = 1, rows = 3))
        assertEquals(NoticeCardStep.Dismiss(1), noticeCardStep(GamepadAction.CHANGE_SORT, cursor = 1, rows = 3))
        assertEquals("a row dismissed under the cursor leaves it in range", NoticeCardStep.Open(1), noticeCardStep(GamepadAction.SELECT, cursor = 2, rows = 2))
    }

    @Test
    fun `B, anything else, or an empty card closes it`() {
        assertEquals(NoticeCardStep.Close, noticeCardStep(GamepadAction.BACK, cursor = 0, rows = 3))
        assertEquals(NoticeCardStep.Close, noticeCardStep(GamepadAction.NAVIGATE_LEFT, cursor = 0, rows = 3))
        assertEquals(NoticeCardStep.Close, noticeCardStep(GamepadAction.SELECT, cursor = 0, rows = 0))
    }
}
