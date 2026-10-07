package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-05: the boot and launch sounds were lost under the menu music; it waits while any of them plays
class CeremonySoundTest {
    @Test
    fun `the boot, the launch disc and GameBoot each hold the menu music back`() {
        assertFalse(CrossbarUiState(showBootSequence = false).ceremonyPlaying)
        assertTrue(CrossbarUiState(showBootSequence = true).ceremonyPlaying)
        assertTrue(CrossbarUiState(showBootSequence = false, discCeremony = DiscCeremonyState(art = null)).ceremonyPlaying)
        // owner, 2026-10-07: the boot sound runs on after the animation closes, and the music still waits for it
        assertTrue(CrossbarUiState(showBootSequence = false, bootSoundPlaying = true).ceremonyPlaying)
        assertTrue(CrossbarUiState(showBootSequence = false, activeGameBoot = com.echo.feature.launcher.GameBootRequest(gameTitle = "Skyrim")).ceremonyPlaying)
    }
}
