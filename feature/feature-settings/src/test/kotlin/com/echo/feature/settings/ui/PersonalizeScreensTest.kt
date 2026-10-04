package com.echo.feature.settings.ui

import com.echo.core.domain.model.settingsEntryFor
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizeScreensTest {
    @Test
    fun `every Accounts row in setup opens the one real settings screen for that key`() {
        ACCOUNT_SCREENS.forEach { id ->
            assertTrue("$id is not in the catalog, so the row would have no title", settingsEntryFor(id) != null)
            assertTrue("$id has no route", id in SETTINGS_SCREEN_ROUTES)
        }
    }
}
