package com.echo.feature.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: a settings picker is the kit's rail; it must still tick the choice in force and put the
// cursor where the pad is, which may be another row
class SettingsPickerMenuTest {
    @Test
    fun `the rail is titled by the row, ticks the current choice, and follows the cursor`() {
        val request = SettingsPickerRequest(
            title = "Type",
            options = listOf("Generic", "Xbox", "PlayStation").map { SettingsPickerOption(it) },
            selectedIndex = 1,
            onPick = {},
        )
        val menu = request.menu(cursor = 2)
        assertEquals("Type", menu.title)
        assertEquals(listOf("Generic", "Xbox", "PlayStation"), menu.rows.map { it.label })
        assertEquals(listOf(false, true, false), menu.rows.map { it.checked })
        assertEquals(2, menu.selectedIndex)
    }
}
