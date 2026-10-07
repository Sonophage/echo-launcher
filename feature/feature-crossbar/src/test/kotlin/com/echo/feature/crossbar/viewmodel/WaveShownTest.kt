package com.echo.feature.crossbar.viewmodel

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveShownTest {
    @Test
    fun `the wave belongs to the crossbar and is gone behind every screen that covers it`() {
        val crossbar = CrossbarUiState(showBootSequence = false)
        assertTrue(crossbar.waveShown)
        assertFalse(crossbar.copy(notificationsOpen = true).waveShown)
        assertFalse(crossbar.copy(activeSettingsScreen = "settings_about").waveShown)
        assertFalse(crossbar.copy(activeAppDrawerFilter = "RECENT").waveShown)
        assertFalse(crossbar.copy(musicPlayerVisible = true).waveShown)
    }
}
