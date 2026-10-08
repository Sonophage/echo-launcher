package com.echo.core.data.database.seeder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// "Memory Card" left the UI (owner, 2026-10-07). Installs from before keep the old defaults in the
// database until this rule renames them, and it must never touch a name the owner typed.
class LegacyNameTest {
    @Test
    fun `a default card name loses Memory Card`() {
        assertEquals("PlayStation 2", renamedFromLegacy("ps2", "PlayStation 2 Memory Card", "PlayStation 2"))
    }

    @Test
    fun `both old PC defaults become PC`() {
        assertEquals("PC", renamedFromLegacy("windows", "Windows Memory Card", "PC"))
        assertEquals("PC", renamedFromLegacy("windows", "Windows Games", null))
    }

    @Test
    fun `a name the owner chose is kept`() {
        assertNull(renamedFromLegacy("windows", "Steam", "PC"))
        assertNull(renamedFromLegacy("ps2", "My PS2 Memory Card", "PlayStation 2"))
    }

    @Test
    fun `an already-renamed card is left alone`() {
        assertNull(renamedFromLegacy("ps2", "PlayStation 2", "PlayStation 2"))
    }
}
