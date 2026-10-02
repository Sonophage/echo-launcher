package com.psplauncher.feature.backup

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertFailsWith

class ReaderSnapshotTest {

    @Test
    fun `per-book keys the reader builds at runtime survive the round trip`() {
        val device = mutablePreferencesOf(
            stringPreferencesKey("position_7f3e") to """{"href":"ch3.xhtml","locations":{"progression":0.4}}""",
            stringPreferencesKey("bookmarks_7f3e") to """[{"locatorJson":"{}","label":"p. 12","createdAt":1}]""",
            stringPreferencesKey("display_page") to "SEPIA",
            floatPreferencesKey("display_text_scale") to 1.3f,
        )

        val restored = mutablePreferencesOf().apply { replaceWith(device.toReaderSnapshot()) }

        assertEquals(device.asMap(), restored.asMap())
    }

    @Test
    fun `a restore replaces the reader store rather than merging into it`() {
        val stale = mutablePreferencesOf(stringPreferencesKey("position_gone") to "{}")

        stale.replaceWith(ReaderSnapshot(strings = mapOf("display_page" to "PAPER")))

        assertNull(
            "a book position the backup does not hold must not outlive the restore",
            stale[stringPreferencesKey("position_gone")],
        )
        assertEquals("PAPER", stale[stringPreferencesKey("display_page")])
    }

    @Test
    fun `a value type the snapshot cannot carry fails the backup instead of vanishing`() {
        val prefs = mutablePreferencesOf(intPreferencesKey("display_columns") to 2)

        assertFailsWith<IllegalStateException> { prefs.toReaderSnapshot() }
    }
}
