package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

class PillNavTest {
    private val right = GamepadAction.NAVIGATE_RIGHT
    private val left = GamepadAction.NAVIGATE_LEFT
    private val down = GamepadAction.NAVIGATE_DOWN

    @Test
    fun `a row with no pills passes every press through`() {
        assertEquals(PillNav.Pass, pillNav(right, current = null, count = 0))
        assertEquals(PillNav.Pass, pillNav(left, current = null, count = 0))
    }

    @Test
    fun `down enters the row and neither left nor right does, so the crossbar owns the horizontal`() {
        assertEquals(PillNav.Move(0), pillNav(down, current = null, count = 4))
        assertEquals(PillNav.Pass, pillNav(right, current = null, count = 4))
        assertEquals(PillNav.Pass, pillNav(left, current = null, count = 4))
    }

    @Test
    fun `inside the row the presses walk it`() {
        assertEquals(PillNav.Move(2), pillNav(right, current = 1, count = 4))
        assertEquals(PillNav.Move(0), pillNav(left, current = 1, count = 4))
    }

    @Test
    fun `falling off either end leaves the row and carries the press out with it`() {
        assertEquals(PillNav.ExitAndPass, pillNav(right, current = 3, count = 4))
        assertEquals(PillNav.ExitAndPass, pillNav(left, current = 0, count = 4))
    }

    @Test
    fun `a single pill is entered and left, never walked`() {
        assertEquals(PillNav.Pass, pillNav(right, current = null, count = 1))
        assertEquals(PillNav.Move(0), pillNav(down, current = null, count = 1))
        assertEquals(PillNav.ExitAndPass, pillNav(right, current = 0, count = 1))
        assertEquals(PillNav.ExitAndPass, pillNav(left, current = 0, count = 1))
    }

    @Test
    fun `down does nothing from inside the row, and up is not this function's business`() {
        assertEquals(PillNav.Pass, pillNav(down, current = 1, count = 4))
        listOf(GamepadAction.NAVIGATE_UP, GamepadAction.SELECT).forEach {
            assertEquals("$it must pass", PillNav.Pass, pillNav(it, current = 1, count = 4))
        }
    }

    @Test
    fun `a row with no pills still takes none of them`() {
        listOf(right, left, down).forEach {
            assertEquals("$it was taken by a row with no pills", PillNav.Pass, pillNav(it, null, count = 0))
        }
    }

    @Test
    fun `stepping a category costs one press however many pills the row has`() {
        listOf(0, 1, 2, 3, 4, 9).forEach { count ->
            assertEquals(
                "a $count-pill row must not swallow a press meant for the next category",
                PillNav.Pass,
                pillNav(right, current = null, count = count),
            )
        }
    }

    @Test
    fun `a press is still only consumed once you are deliberately inside the row`() {
        assertEquals(PillNav.Move(1), pillNav(right, current = 0, count = 4))
        assertEquals(PillNav.ExitAndPass, pillNav(right, current = 3, count = 4))
    }

    @Test
    fun `down reaches the pill row before it steps the item, or the row is only reachable at the foot of a column`() {
        assertEquals(DownStep.EnterRow, downStep(inPillRow = false, pillRowVisible = true))
    }

    @Test
    fun `down out of the pill row lands on the next item, so the row is a stop and not a trap`() {
        assertEquals(DownStep.LeaveRowAndStepItem, downStep(inPillRow = true, pillRowVisible = true))
    }

    @Test
    fun `a row with no pills is stepped straight past`() {
        assertEquals(DownStep.StepItem, downStep(inPillRow = false, pillRowVisible = false))
    }

    @Test
    fun `arriving on a category shows no pills, and the first DOWN on an item with pills enters the column instead of moving`() {
        assertEquals(DownStep.EnterColumn, downStep(inPillRow = false, pillRowVisible = false, inColumn = false, hasPills = true))
        assertEquals(DownStep.StepItem, downStep(inPillRow = false, pillRowVisible = false, inColumn = false, hasPills = false))
        assertEquals(DownStep.EnterRow, downStep(inPillRow = false, pillRowVisible = true, inColumn = true, hasPills = true))
    }
}
