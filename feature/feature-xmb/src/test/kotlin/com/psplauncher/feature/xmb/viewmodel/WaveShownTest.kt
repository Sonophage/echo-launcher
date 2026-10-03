package com.psplauncher.feature.xmb.viewmodel

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveShownTest {
    @Test
    fun `the wave belongs to the crossbar and is gone behind every screen that covers it`() {
        val crossbar = XMBUiState(showBootSequence = false)
        assertTrue(crossbar.waveShown)
        assertFalse(crossbar.copy(notificationsOpen = true).waveShown)
        assertFalse(crossbar.copy(activeSettingsScreen = "settings_overview").waveShown)
        assertFalse(crossbar.copy(activeAppDrawerFilter = "RECENT").waveShown)
        assertFalse(crossbar.copy(musicPlayerVisible = true).waveShown)
    }
}
