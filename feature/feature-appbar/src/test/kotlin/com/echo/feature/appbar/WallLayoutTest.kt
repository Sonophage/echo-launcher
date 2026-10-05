package com.echo.feature.appbar

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-05: the apps stand as cases in columns beside the info column
class WallLayoutTest {
    @Test
    fun `the d-pad walks the grid the way it is drawn, a row of columns at a time`() {
        val cells = wallLayout(total = 14, columns = 6)
        fun move(action: GamepadAction, from: Int) = wallMove(action, from, cells)

        assertEquals("right steps along the row", 1, move(GamepadAction.NAVIGATE_RIGHT, 0))
        assertEquals("left steps back", 0, move(GamepadAction.NAVIGATE_LEFT, 1))
        assertEquals("down drops one row in the same column", 8, move(GamepadAction.NAVIGATE_DOWN, 2))
        assertEquals("up climbs one row in the same column", 2, move(GamepadAction.NAVIGATE_UP, 8))
        assertEquals("down into a short last row takes its nearest tile", 13, move(GamepadAction.NAVIGATE_DOWN, 10))
        assertEquals("no wrap at the right edge", 5, move(GamepadAction.NAVIGATE_RIGHT, 5))
        assertEquals("no wrap at the left edge", 6, move(GamepadAction.NAVIGATE_LEFT, 6))
        assertEquals("up from the top row stays", 3, move(GamepadAction.NAVIGATE_UP, 3))
    }

    @Test
    fun `every app has its own cell, four to a row`() {
        val cells = wallLayout(total = 20)
        assertEquals(20, cells.size)
        assertEquals(cells.size, cells.toSet().size)
        assertEquals(WallCell(row = 1, col = 0), cells[WALL_COLUMNS])
    }
}
