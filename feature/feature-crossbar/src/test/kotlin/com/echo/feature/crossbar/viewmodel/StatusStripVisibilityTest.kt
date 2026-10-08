package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.echo.core.ui.components.MenuState

class StatusStripVisibilityTest {
    private fun crossbar() = CrossbarUiState(showBootSequence = false)

    @Test
    fun `on the crossbar it is drawn`() {
        assertTrue(crossbar().statusStripVisible)
        assertTrue("the crossbar's own context is the strip's", crossbar().stripShowsCrossbarContext)
    }

    @Test
    fun `the launcher's own screens keep it`() {
        val chrome = mapOf(
            "App Drawer" to crossbar().copy(activeAppDrawerFilter = "DEFAULT"),
            "Settings" to crossbar().copy(activeSettingsScreen = "settings_appearance"),
            "Search" to crossbar().copy(search = SearchState(scope = SearchScope.ALL)),

            "App picker" to crossbar().copy(
                appPicker = AppPickerState(
                    title = "Add Apps",
                    target = AppPickerTarget.CategoryShortcuts("cat"),
                    apps = emptyList(),
                ),
            ),
            "Game picker" to crossbar().copy(gamePickerCategoryId = "cat"),
        )
        chrome.forEach { (name, state) ->
            assertTrue("$name must still cover the crossbar", state.hasBlockingOverlay)
            assertTrue("$name lost the clock", state.statusStripVisible)

            assertFalse(
                "$name is not the crossbar, so the sort label must not claim to describe it",
                state.stripShowsCrossbarContext,
            )
        }
    }

    @Test
    fun `a full-screen overlay covers it`() {
        val fullscreen = mapOf(
            "boot" to crossbar().copy(showBootSequence = true),
            "video player" to crossbar().copy(activeVideoId = "v1"),
            "music player" to crossbar().copy(musicPlayerVisible = true),
        )
        fullscreen.forEach { (name, state) ->
            assertTrue("$name must cover the crossbar", state.hasBlockingOverlay)
            assertFalse("$name must cover the clock too", state.statusStripVisible)
        }
    }

    @Test
    fun `the rail and the sheet keep both the strip and its crossbar context`() {
        val rail = crossbar().copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Options", rows = emptyList())),
        )
        assertTrue(rail.statusStripVisible)
        assertTrue("the rail is not a cover; the crossbar is still underneath it", rail.stripShowsCrossbarContext)

        val sheet = crossbar().copy(notificationsOpen = true)
        assertTrue(sheet.statusStripVisible)
        assertTrue(sheet.stripShowsCrossbarContext)
    }

    @Test
    fun `a rail over a chrome screen does not restore the crossbar's context`() {
        val state = crossbar().copy(
            activeAppDrawerFilter = "DEFAULT",
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Options", rows = emptyList())),
        )
        assertTrue("the drawer still keeps the clock", state.statusStripVisible)
        assertFalse("the crossbar is two screens away", state.stripShowsCrossbarContext)
    }
}
