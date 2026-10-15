package com.echo.feature.settings.permissions

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionGrantsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val usage = AppPermissions.ALL.first { it.id == AppPermissions.USAGE_ACCESS }
    private val listener = AppPermissions.ALL.first { it.id == AppPermissions.NOTIFICATION_LISTENER }

    // Android 13 blocks notification access for an app installed outside a store; Android 15 adds usage access
    @Test
    fun `restricted settings cover notification access from Android 13 and usage access from Android 15`() {
        assertFalse(restrictedSettingsApply(Build.VERSION_CODES.S_V2, listener))
        assertTrue(restrictedSettingsApply(Build.VERSION_CODES.TIRAMISU, listener))
        assertFalse(restrictedSettingsApply(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, usage))
        assertTrue(restrictedSettingsApply(Build.VERSION_CODES.VANILLA_ICE_CREAM, usage))
    }

    @Test
    fun `the restricted hint shows only for a blocked permission that is not granted`() {
        val rows = listOf(usage, listener)
        val onAndroid14 = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        assertFalse("usage access is not restricted on 14", restrictedHintShows(onAndroid14, rows) { it == listener })
        assertTrue("notification access is", restrictedHintShows(onAndroid14, rows) { it == usage })
        assertTrue(restrictedHintShows(Build.VERSION_CODES.VANILLA_ICE_CREAM, rows) { it == listener })
        assertFalse("all granted", restrictedHintShows(Build.VERSION_CODES.VANILLA_ICE_CREAM, rows) { true })
    }

    // the first screen opens on ECHO's own switch; a ROM without it takes the list, and one without that ECHO's App info
    @Test
    fun `usage access tries ECHO's own switch, then the list, then App info`() {
        val intents = systemScreenIntents(app, usage)
        assertEquals(
            listOf(Settings.ACTION_USAGE_ACCESS_SETTINGS, Settings.ACTION_USAGE_ACCESS_SETTINGS, Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            intents.map(Intent::getAction),
        )
        assertEquals("package:${app.packageName}", intents[0].dataString)
        assertEquals(null, intents[1].data)
    }

    @Test
    fun `notification access falls back to App info`() {
        assertEquals(
            listOf(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
            systemScreenIntents(app, listener).map(Intent::getAction),
        )
    }
}
