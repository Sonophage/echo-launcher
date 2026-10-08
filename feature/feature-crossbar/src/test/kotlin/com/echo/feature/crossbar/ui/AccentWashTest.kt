package com.echo.feature.crossbar.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: with the wave off the selected item's colour still reaches the screen
class AccentWashTest {
    @Test
    fun `the wash is the item's colour, and nothing when it has none`() {
        val red = accentWashColor(0xFFC0392BL)
        assertEquals(Color(0xFFC0392B).red, red.red, 0.01f)
        assertEquals(ACCENT_WASH_ALPHA, red.alpha, 0.01f)
        assertEquals(Color.Transparent, accentWashColor(null))
    }
}
