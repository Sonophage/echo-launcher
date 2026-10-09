package com.echo.core.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.themekit.ThemeSettings
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeSettingsPrefsTest {
    private val rowCovers = booleanPreferencesKey("pref_xmb_row_cover_art")
    private val legibility = stringPreferencesKey("display_icon_legibility")
    private val fade = booleanPreferencesKey("display_fade_by_distance")

    @Test
    fun `every theme setting is one the ECHO folder carries, with the same type`() {
        val folder = EchoSettingsExport.GROUPS.values.fold(emptyMap<String, EchoSettingsExport.Kind>()) { all, g -> all + g }
        val same = mapOf(ThemeSettings.Kind.BOOL to EchoSettingsExport.Kind.BOOL, ThemeSettings.Kind.TEXT to EchoSettingsExport.Kind.TEXT)
        ThemeSettings.KEYS.forEach { (key, kind) ->
            assertEquals(same[kind], folder[key], "$key: a theme and settings.json must store it the same way")
        }
    }

    @Test
    fun `a theme sets its values and puts a null back to ECHO's default`() {
        val prefs = mutablePreferencesOf(fade to true, legibility to "NONE")
        prefs.applyThemeSettings(buildJsonObject {
            put("pref_xmb_row_cover_art", true)
            put("display_icon_legibility", "CONTOUR_AUTO")
            put("display_fade_by_distance", JsonNull)
            put("sgdb_api_key", "x")
        })
        assertEquals(true, prefs[rowCovers])
        assertEquals("CONTOUR_AUTO", prefs[legibility])
        assertNull(prefs[fade], "null clears the setting")
        assertNull(prefs[stringPreferencesKey("sgdb_api_key")], "a key not on the list is never written")
    }

    @Test
    fun `the look kept before a theme puts back settings the theme changed, even ones never set`() {
        val before = mutablePreferencesOf(rowCovers to false)
        val kept = before.themeSettings()
        assertEquals(ThemeSettings.KEYS.keys, kept.keys, "every setting is recorded")
        assertEquals(JsonNull, kept["display_fade_by_distance"], "an unset setting is recorded as null")

        val after = before.toMutablePreferences()
        after.applyThemeSettings(buildJsonObject { put("pref_xmb_row_cover_art", true); put("display_fade_by_distance", false) })
        after.applyThemeSettings(kept)
        assertEquals(before.asMap(), after.asMap())
    }
}
