package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentRailStepTest {
    @Test
    fun `RIGHT from the open rail closes it and stays on Last Played, only the next RIGHT leaves for the crossbar`() {
        assertEquals(RailStep.Close, recentRailStep(GamepadAction.NAVIGATE_RIGHT, onLastPlayedHome = true, railVisible = true))
        assertEquals(RailStep.Pass, recentRailStep(GamepadAction.NAVIGATE_RIGHT, onLastPlayedHome = true, railVisible = false))
    }

    @Test
    fun `BACK closes the open rail, LEFT opens a closed one, and the rail never claims a press off Last Played`() {
        assertEquals(RailStep.Close, recentRailStep(GamepadAction.BACK, onLastPlayedHome = true, railVisible = true))
        assertEquals(RailStep.Open, recentRailStep(GamepadAction.NAVIGATE_LEFT, onLastPlayedHome = true, railVisible = false))
        assertEquals(RailStep.Pass, recentRailStep(GamepadAction.NAVIGATE_LEFT, onLastPlayedHome = true, railVisible = true))
        assertEquals(RailStep.Pass, recentRailStep(GamepadAction.NAVIGATE_RIGHT, onLastPlayedHome = false, railVisible = true))
    }
}
