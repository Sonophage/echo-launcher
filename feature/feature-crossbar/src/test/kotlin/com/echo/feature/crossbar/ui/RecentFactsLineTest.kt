package com.echo.feature.crossbar.ui

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-08: the Recent view, and the split drawer that uses it, shows a game's details and achievements
class RecentFactsLineTest {
    @Test
    fun `details and achievements share one line, each only when known`() {
        val game = CrossbarItem(id = "7", title = "Crisis Core", metadataLine = "2008 · Action RPG · Square Enix")
        assertEquals("2008 · Action RPG · Square Enix  ·  Achievements 12/40", factsLine(game, "12/40"))
        assertEquals("Achievements 3/9", factsLine(game.copy(metadataLine = null), "3/9"))
        assertNull(factsLine(game.copy(metadataLine = " "), null))
    }
}
