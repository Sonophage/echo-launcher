package com.psplauncher.core.data.achievement

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.psplauncher.core.data.datastore.pfpDataStore
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AchievementCredentialsProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val provider = AchievementCredentialsProvider(context)

    @Test
    fun `saves, trims and reads credentials, then clears everything`() = runTest {
        clearAll()

        provider.saveRetroAchievements("  Chrono  ", "  ra-key-123  ")
        provider.saveSteam("  76561197960287930  ", "  steam-key-456  ")
        provider.setEnabled(true)

        assertEquals("Chrono", provider.raUsername())
        assertEquals("ra-key-123", provider.raApiKey())
        assertEquals("76561197960287930", provider.steamId64())
        assertEquals("steam-key-456", provider.steamApiKey())
        assertTrue(provider.hasRetroAchievements())
        assertTrue(provider.hasSteam())

        clearAll()

        assertNull(provider.raUsername())
        assertNull(provider.raApiKey())
        assertNull(provider.steamId64())
        assertNull(provider.steamApiKey())
        assertFalse(provider.hasRetroAchievements())
        assertFalse(provider.hasSteam())
    }

    @Test
    fun `a sealed key this device cannot open reads as missing, never as the ciphertext sent to Steam`() = runTest {
        clearAll()
        provider.saveSteam("76561197960287930", "unused")
        val foreignCiphertext = Base64.encodeToString(ByteArray(60) { it.toByte() }, Base64.NO_WRAP)
        context.pfpDataStore.edit { it[stringPreferencesKey("steam_api_key")] = foreignCiphertext }

        assertNull(provider.steamApiKey())
        assertFalse(provider.hasSteam())
    }

    @Test
    fun `a 32 character hex key stored unprotected still reads back`() = runTest {
        clearAll()
        val hexKey = "0123456789ABCDEF0123456789ABCDEF"
        context.pfpDataStore.edit { it[stringPreferencesKey("steam_api_key")] = hexKey }

        assertEquals(hexKey, provider.steamApiKey())
    }

    private suspend fun clearAll() {
        provider.clearRetroAchievements()
        provider.clearSteam()
    }
}
