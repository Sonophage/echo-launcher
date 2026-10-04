package com.echo.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsRailRowsTest {
    @Test
    fun `the rail is exactly its own section's screens`() {
        SETTINGS_CATALOG.forEach { entry ->
            assertEquals(
                settingsEntriesIn(entry.section).map { it.id },
                settingsRailRows(entry.id).map { it.id },
                "rail for ${entry.id}",
            )
        }
    }

    @Test
    fun `the screen you are on is always in its own rail, exactly once`() {
        SETTINGS_CATALOG.forEach { entry ->
            val ids = settingsRailRows(entry.id).map { it.id }
            assertEquals(1, ids.count { it == entry.id }, "${entry.id} in its own rail")
        }
    }

    @Test
    fun `no row is repeated`() {
        SETTINGS_CATALOG.forEach { entry ->
            val ids = settingsRailRows(entry.id).map { it.id }
            assertEquals(ids.distinct(), ids, "duplicate rail row for ${entry.id}")
        }
    }

    @Test
    fun `a route outside the catalog has no rail`() {
        assertEquals(emptyList(), settingsRailRows("settings_initial_setup_first"))
        assertEquals(emptyList(), settingsRailRows("settings_import_pc"))
        assertEquals(emptyList(), settingsRailRows(null))
    }

    @Test
    fun `every section has at least one screen to land on`() {
        SettingsSectionId.entries.forEach { section ->
            assertTrue(settingsEntriesIn(section).isNotEmpty(), "$section has no screens")
        }
    }

    @Test
    fun `the shoulders walk every tab of the section and wrap, never leaving it`() {
        SettingsSectionId.entries.forEach { section ->
            val tabs = settingsEntriesIn(section).map { it.id }
            listOf(1, -1).forEach { delta ->
                var id: String? = tabs.first()
                val seen = mutableListOf<String>()
                repeat(tabs.size) {
                    seen += id!!
                    id = settingsTabStepTarget(id, delta)
                }
                assertEquals(tabs.first(), id, "$section did not wrap stepping $delta")
                assertEquals(tabs.toSet(), seen.toSet(), "$section stepping $delta")
            }
        }
        assertEquals("settings_artwork", settingsTabStepTarget("settings_library", 1))
        assertEquals("settings_emulators_retroarch", settingsTabStepTarget("settings_library", -1))
        assertEquals(null, settingsTabStepTarget("settings_import_pc", 1))
    }
}
