package com.echo.feature.crossbar.viewmodel

import org.junit.Test
import kotlin.test.assertTrue

class BlockingOverlayBranchTest {
    private fun idle() = CrossbarUiState(showBootSequence = false)

    @Test
    fun `an idle crossbar blocks nothing`() {
        assertTrue(idle().hasBlockingOverlay.not())
    }

    @Test
    fun `the save-as-theme dialog blocks the crossbar`() {
        val open = idle().copy(
            saveThemeNameDialog = PlaylistNameDialogState(title = "Save Current Look as Theme"),
        )
        assertTrue(open.hasBlockingOverlay, "the crossbar would navigate behind an open dialog")
    }

    @Test
    fun `the boot sequence blocks the crossbar, which is why the default state does`() {
        assertTrue(CrossbarUiState().hasBlockingOverlay)
        assertTrue(idle().copy(showBootSequence = true).hasBlockingOverlay)
    }
}
