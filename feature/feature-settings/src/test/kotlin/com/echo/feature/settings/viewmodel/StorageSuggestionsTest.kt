package com.echo.feature.settings.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageSuggestionsTest {
    private val card = mapOf(
        "" to listOf(" Library", "Videos", "Movies", "Music", "Pictures", "Emulation"),
        "Emulation" to listOf("BIOS", "PSPL", "ROMs"),
    )
    private val children: (String) -> List<String>? = { card[it] }

    @Test
    fun `each slot lands on the folder the owner actually uses, so one tap grants it`() {
        assertEquals("Emulation/ROMs", suggestFolder(StorageSlot.GAMES, children))
        assertEquals("Emulation/PSPL", suggestFolder(StorageSlot.ARTWORK, children))
        assertEquals("Movies", suggestFolder(StorageSlot.VIDEO, children))
        assertEquals("Pictures", suggestFolder(StorageSlot.PHOTOS, children))
    }

    @Test
    fun `a folder name with a stray leading space still matches, keeping the real name`() {
        assertEquals(" Library", suggestFolder(StorageSlot.BOOKS, children))
    }

    @Test
    fun `nothing matching means no suggestion rather than a wrong folder`() {
        assertNull(suggestFolder(StorageSlot.GAMES, mapOf("" to listOf("Music"))::get))
        assertNull(suggestFolder(StorageSlot.MUSIC) { null })
    }
}
