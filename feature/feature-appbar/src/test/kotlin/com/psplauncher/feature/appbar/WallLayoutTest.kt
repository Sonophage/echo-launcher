package com.psplauncher.feature.appbar

import com.psplauncher.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

class WallLayoutTest {
    @Test
    fun `the d-pad walks the wall the way it is drawn, around the big tile and onto the rest`() {
        val cells = wallLayout(sectionCount = 3, total = 10)
        fun move(action: GamepadAction, from: Int) = wallMove(action, from, cells)

        assertEquals("right off the big tile lands on the tile beside it", 1, move(GamepadAction.NAVIGATE_RIGHT, 0))
        assertEquals("left from beside the big tile goes back to it", 0, move(GamepadAction.NAVIGATE_LEFT, 1))
        assertEquals("the rest starts on its own row under the big tile", 3, move(GamepadAction.NAVIGATE_DOWN, 0))
        assertEquals("down from beside the big tile lands in the same column, not on the big tile", 6, move(GamepadAction.NAVIGATE_DOWN, 2))
        assertEquals("up from under the big tile returns to it", 0, move(GamepadAction.NAVIGATE_UP, 4))
        assertEquals("up into a short row takes its nearest tile", 2, move(GamepadAction.NAVIGATE_UP, 7))
        assertEquals("the wall has no wrap at the right edge", 8, move(GamepadAction.NAVIGATE_RIGHT, 8))
    }
}
