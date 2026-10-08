package com.echo.feature.library.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SafDocumentIdPathTest {

    @Test
    fun `primary volume maps to emulated 0`() {
        assertEquals(
            "/storage/emulated/0/ROMs/PSP/game.iso",
            com.echo.core.data.repository.RomRootRepository.docIdToRawPath("primary:ROMs/PSP/game.iso"),
        )
    }

    @Test
    fun `removable volume maps under storage uuid`() {
        assertEquals(
            "/storage/1A2B-3C4D/Games/game.chd",
            com.echo.core.data.repository.RomRootRepository.docIdToRawPath("1A2B-3C4D:Games/game.chd"),
        )
    }

    @Test
    fun `primary is case-insensitive`() {
        assertEquals(
            "/storage/emulated/0/a.bin",
            com.echo.core.data.repository.RomRootRepository.docIdToRawPath("PRIMARY:a.bin"),
        )
    }

    @Test
    fun `document id without a relative part is rejected`() {
        assertNull(com.echo.core.data.repository.RomRootRepository.docIdToRawPath("primary:"))
        assertNull(com.echo.core.data.repository.RomRootRepository.docIdToRawPath("primary"))
        assertNull(com.echo.core.data.repository.RomRootRepository.docIdToRawPath(""))
    }
}
