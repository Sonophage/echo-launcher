package com.echo.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RomRootDerivationTest {
    @Test
    fun `docIdToRawPath maps primary and removable volumes`() {
        assertEquals("/storage/emulated/0/Roms", RomRootRepository.docIdToRawPath("primary:Roms"))
        assertEquals("/storage/1A2B-3C4D/Games", RomRootRepository.docIdToRawPath("1A2B-3C4D:Games"))
    }

    @Test
    fun `docIdToRawPath returns null for opaque ids`() {
        assertNull(RomRootRepository.docIdToRawPath("primary:"))
        assertNull(RomRootRepository.docIdToRawPath("noColon"))
    }

}
