package com.psplauncher.core.data.database.seeder

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RetiredKeysTest {
    @Test
    fun `every achievement key a device still holds is wiped, whatever its type, and nothing else`() {
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
        }

        wipeRetiredKeys(prefs)

        assertEquals(
            "only the live key should be left",
            setOf("sgdb_api_key"),
            prefs.asMap().keys.map { it.name }.toSet(),
        )
    }
}
