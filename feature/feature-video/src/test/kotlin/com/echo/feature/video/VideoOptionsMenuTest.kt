package com.echo.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: the player's Options is the kit's rail; each row must still say what is set
class VideoOptionsMenuTest {
    @Test
    fun `each row names its setting and current value, and the cursor is on the chosen row`() {
        val menu = videoOptionsMenu(selectedRow = 2, speed = 1.5f, subtitleLabel = "Off", audioLabel = "English", screenMode = "Fit")
        assertEquals(
            listOf("Playback Speed · 1.5×", "Subtitles · Off", "Audio Track · English", "Screen Mode · Fit"),
            menu.rows.map { it.label },
        )
        assertEquals(listOf(0, 1, 2, 3), menu.rows.map { it.action })
        assertEquals(2, menu.selectedIndex)
    }
}
