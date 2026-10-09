package com.echo.core.data.database.seeder

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RetiredKeysTest {
    @Test
    fun `only the Local Steam, Goldberg and old luminance keys are wiped, the accounts and the wallpaper accent stay`() {
        val prefs = mutablePreferencesOf().apply {
            this[stringPreferencesKey("ra_username")] = "someone"
            this[stringPreferencesKey("ra_api_key")] = "secret"
            this[stringPreferencesKey("steam_id64")] = "7656"
            this[stringPreferencesKey("steam_api_key")] = "secret"
            this[booleanPreferencesKey("achievements_enabled")] = true
            this[longPreferencesKey("achievements_sync_last")] = 1L
            this[booleanPreferencesKey("goldberg_installer_enabled")] = true
            this[booleanPreferencesKey("local_steam_tracking_enabled")] = true
            this[stringPreferencesKey("sgdb_api_key")] = "kept"
            this[stringPreferencesKey("display_wallpaper_luma")] = "{}"
            this[stringPreferencesKey("wallpaper_accent_source")] = "/w.jpg"
            this[longPreferencesKey("wallpaper_accent")] = 0xFF336699L
        }

        wipeRetiredKeys(prefs)

        assertEquals(
            setOf(
                "ra_username",
                "ra_api_key",
                "steam_id64",
                "steam_api_key",
                "achievements_enabled",
                "achievements_sync_last",
                "sgdb_api_key",
                "wallpaper_accent_source",
                "wallpaper_accent",
            ),
            prefs.asMap().keys.map { it.name }.toSet(),
        )
    }
}
