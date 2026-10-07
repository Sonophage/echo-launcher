package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-06: LB is Apps and RB Search on every screen, Select the notifications, Home back to the
// crossbar; the players and editors keep the bumpers for paging and seeking
class GlobalStepTest {
    private val lb = GamepadAction.PREV_PAGE
    private val rb = GamepadAction.NEXT_PAGE

    // past the boot sequence, which a fresh state is still showing
    private fun state() = CrossbarUiState(showBootSequence = false)

    @Test
    fun `LB and RB open Apps and Search from the crossbar, Settings and Game Info alike`() {
        val screens = listOf(
            state(),
            state().copy(activeSettingsScreen = "settings_display"),
            state().copy(gameInfo = GameInfoState(CrossbarItem(id = "1", title = "Skyrim", gameId = 1L))),
        )
        screens.forEach { s ->
            assertEquals(GlobalStep.APPS, globalStep(lb, s))
            assertEquals(GlobalStep.SEARCH, globalStep(rb, s))
        }
    }

    @Test
    fun `a bumper pressed again closes what it opened`() {
        val drawer = state().copy(activeAppDrawerFilter = "APPS")
        assertNull("LB in the drawer is the drawer's own, which closes it", globalStep(lb, drawer))
        assertNull("RB in the drawer is the drawer's own search", globalStep(rb, drawer))
        val search = state().copy(search = SearchState(scope = SearchScope.ALL))
        assertEquals(GlobalStep.CLOSE_SEARCH, globalStep(rb, search))
        assertEquals("LB from Search goes to Apps", GlobalStep.APPS, globalStep(lb, search))
    }

    @Test
    fun `the video player keeps its bumpers for seeking, and the boot sequence its buttons`() {
        assertNull(globalStep(lb, CrossbarUiState(showBootSequence = true)))
        val video = state().copy(activeVideoId = "v")
        assertNull(globalStep(lb, video))
        assertNull(globalStep(rb, video))
        assertNull(globalStep(GamepadAction.HOME, video))
    }

    @Test
    fun `Select opens the notifications, and Home never leaves the first-run wizard`() {
        assertEquals(GlobalStep.NOTIFICATIONS, globalStep(GamepadAction.OPEN_NOTIFICATIONS, state()))
        assertEquals(GlobalStep.HOME, globalStep(GamepadAction.HOME, state().copy(activeSettingsScreen = "settings_display")))
        val wizard = CrossbarViewModel.WIZARD_SCREEN_IDS.first()
        assertNull(globalStep(GamepadAction.HOME, state().copy(activeSettingsScreen = wizard)))
    }

    @Test
    fun `the face buttons and the d-pad are left to the screen`() {
        listOf(GamepadAction.SELECT, GamepadAction.BACK, GamepadAction.OPEN_CONTEXT_MENU, GamepadAction.NAVIGATE_UP)
            .forEach { assertNull(globalStep(it, state())) }
    }
}
