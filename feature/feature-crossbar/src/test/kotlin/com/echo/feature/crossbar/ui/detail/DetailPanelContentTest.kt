package com.echo.feature.crossbar.ui.detail

import com.echo.core.domain.model.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailPanelContentTest {
    private val game = Game(
        id = 1L,
        title = "Crash Bandicoot",
        platformId = "psx",
        logoUri = "file:///logo.png",
        artworkUri = "file:///bg.png",
        romPath = "/storage/roms/psx/Crash Bandicoot (USA).bin",
        description = "A marsupial runs right.",
        releaseYear = 1996,
        genre = "Platform",
        developer = "Naughty Dog",
        players = "1",
    )

    @Test
    fun `the filename is the file, not the path`() {
        assertEquals(
            "Crash Bandicoot (USA).bin",
            detailPanelContentFor(game, "PlayStation", emptyList()).fileName,
        )
    }

    @Test
    fun `a package-backed entry has no filename rather than an empty one`() {
        val app = game.copy(romPath = null)

        assertNull(detailPanelContentFor(app, "Android", emptyList()).fileName)
        assertNull(panelFileName(""))
        assertNull(panelFileName("/trailing/slash/"))
    }

    @Test
    fun `a game never launched from here shows no play time at all`() {
        assertNull(detailPanelContentFor(game.copy(totalPlayTimeMillis = 0L), "PS", emptyList()).playTime)
        assertEquals("2 hr", detailPanelContentFor(game.copy(totalPlayTimeMillis = (2 * 60 + 5) * 60_000L), "PS", emptyList()).playTime)
    }
}
