package com.psplauncher.feature.settings.ui

import com.psplauncher.core.domain.model.settingsEntryFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizeScreensTest {
    @Test
    fun `every Personalize row opens a real settings screen`() {
        PERSONALIZE_SCREENS.forEach { id ->
            assertTrue("$id is not in the catalog, so the row would have no title", settingsEntryFor(id) != null)
            assertTrue("$id has no route", id in SETTINGS_SCREEN_ROUTES)
        }
    }

    @Test
    fun `wallpaper is offered by the screen that holds it`() {
        assertTrue(
            "the wallpaper and wave rows live on settings_appearance; the wizard once sent them to settings_layout",
            "settings_appearance" in PERSONALIZE_SCREENS,
        )
        assertEquals("Wallpaper & Text", settingsEntryFor("settings_appearance")?.title)
    }
}
