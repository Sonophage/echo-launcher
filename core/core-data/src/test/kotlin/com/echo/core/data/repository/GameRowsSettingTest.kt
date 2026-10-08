package com.echo.core.data.repository

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.echo.core.data.repository.IconDisplayPreferences.Companion.setGameRows
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-07: Game rows is one setting, in the panel and in Settings; Icons means icons in every
// column, so a console card's grid of covers follows it
class GameRowsSettingTest {
    @Test
    fun `choosing icons turns the console cards' cover grid off too, and cover art turns it back on`() {
        val prefs = mutablePreferencesOf()

        prefs.setGameRows(false)
        assertEquals(false, IconDisplayPreferences.gameRowsShowCovers(prefs))
        assertEquals("a console card shows its icon, not four covers", false, prefs[IconDisplayPreferences.KEY_CARD_ART_GRID])

        prefs.setGameRows(true)
        assertEquals(true, IconDisplayPreferences.gameRowsShowCovers(prefs))
        assertEquals(true, prefs[IconDisplayPreferences.KEY_CARD_ART_GRID])
    }

    // owner, 2026-10-08: a new install showed icon rows beside console cards of covers; both start as icons
    @Test
    fun `nothing stored starts with icons in the rows and on the console cards`() {
        val prefs = androidx.datastore.preferences.core.preferencesOf()
        assertEquals(false, IconDisplayPreferences.gameRowsShowCovers(prefs))
        assertEquals(false, IconDisplayPreferences.cardArtGrid(prefs))
    }

    @Test
    fun `a card grid the owner chose before is kept`() {
        val prefs = mutablePreferencesOf().apply { this[IconDisplayPreferences.KEY_CARD_ART_GRID] = true }
        assertEquals(true, IconDisplayPreferences.cardArtGrid(prefs))
    }
}
