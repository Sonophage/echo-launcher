package com.echo.core.data.database.seeder

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.launch.LaunchDiscPreferences
import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.GameBootStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-06: new installs get the Lens and Black; an update must not change what a user already saw
class OldDefaultsTest {
    private val launchStyle = stringPreferencesKey("display_launch_disc_style")
    private val gameBootStyle = stringPreferencesKey("display_gameboot_style")
    private val colorScheme = stringPreferencesKey("display_color_scheme")

    @Test
    fun `a new install is left on the new defaults`() {
        val prefs = mutablePreferencesOf()

        keepOldDefaults(prefs)

        assertNull(prefs[launchStyle])
        assertNull(prefs[colorScheme])
        assertEquals(GameBootStyle.LENS, LaunchDiscPreferences.styleOf(prefs))
        assertEquals(GameBootStyle.LENS, GameBootPreferences.styleOf(prefs))
    }

    @Test
    fun `an earlier install keeps Disc and Classic Blue, and a choice it made is not touched`() {
        val prefs = mutablePreferencesOf(intPreferencesKey("data_prep_version") to 270, gameBootStyle to "LENS")

        keepOldDefaults(prefs)

        assertEquals(GameBootStyle.DISC, LaunchDiscPreferences.styleOf(prefs))
        assertEquals("its own pick stays", GameBootStyle.LENS, GameBootPreferences.styleOf(prefs))
        assertEquals("CLASSIC_BLUE", prefs[colorScheme])
    }

    @Test
    fun `it runs once, so a later reset to the default is not undone`() {
        val prefs = mutablePreferencesOf(intPreferencesKey("data_prep_version") to 270)
        keepOldDefaults(prefs)
        prefs.remove(colorScheme)

        keepOldDefaults(prefs)

        assertNull(prefs[colorScheme])
    }
}
