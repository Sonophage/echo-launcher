package com.echo.feature.crossbar.ui.detail

import com.echo.core.domain.model.Game
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private val item = CrossbarItem(
        id = "1",
        title = "Crash Bandicoot",
        platformId = "psx",
        logoUri = "file:///logo.png",
        artworkUri = "file:///bg.png",
        romPath = "/storage/roms/psx/Crash Bandicoot (USA).bin",
        description = "A marsupial runs right.",
        metadataLine = "1996   ·   Platform   ·   Naughty Dog   ·   1 player",
    )

    @Test
    fun `both hosts describe the same game the same way`() {
        val fromGame = detailPanelContentFor(game, "PlayStation", media = emptyList())
        val fromItem = detailPanelContentFor(item, "PlayStation")

        assertEquals(fromGame.title, fromItem.title)
        assertEquals(fromGame.logoUri, fromItem.logoUri)
        assertEquals(fromGame.posterFallbackUri, fromItem.posterFallbackUri)
        assertEquals(fromGame.description, fromItem.description)
        assertEquals(fromGame.fileName, fromItem.fileName)
        assertEquals(fromGame.metaLine, fromItem.metaLine)
    }

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
    fun `the crossbar never offers the media page`() {
        val content = detailPanelContentFor(item, "PlayStation")

        assertTrue(content.media.isEmpty())
        assertTrue(DetailPanelPage.GALLERY !in content.pages)
    }

    @Test
    fun `the video page follows the clip, on both hosts`() {
        assertTrue(DetailPanelPage.VIDEO !in detailPanelContentFor(item, "PlayStation").pages)
        assertTrue(
            DetailPanelPage.VIDEO in
                detailPanelContentFor(item, "PlayStation", videoUri = "file:///snap.mp4").pages,
        )
        assertTrue(
            DetailPanelPage.VIDEO in
                detailPanelContentFor(game, "PlayStation", emptyList(), videoUri = "file:///snap.mp4").pages,
        )
    }

    @Test
    fun `the drill-down offers the media page once there is media`() {
        val content = detailPanelContentFor(
            game,
            "PlayStation",
            media = listOf(DetailMedia(uri = "file:///shot.png", isVideo = false)),
        )

        assertTrue(DetailPanelPage.GALLERY in content.pages)
    }

    @Test
    fun `the info tab goes when the game filled nothing in`() {
        val bare = item.copy(description = null, metadataLine = null, romPath = null)
        val content = detailPanelContentFor(bare, "PlayStation")

        assertTrue(DetailPanelPage.INFO !in content.pages)

        assertTrue(
            DetailPanelPage.INFO in detailPanelContentFor(bare.copy(romPath = "/r/x.bin"), "PS").pages,
        )
    }

    @Test
    fun `a logo that will never be drawn is not offered as a logo`() {
        val orphanLogo = item.copy(artworkUri = null)

        assertNull(detailPanelContentFor(orphanLogo, "PlayStation").logoUri)
    }

    @Test
    fun `a game never launched from here shows no play time at all`() {
        assertNull(detailPanelContentFor(item.copy(totalPlayTimeMillis = 0L), "PS").playTime)
        assertNull(detailPanelContentFor(game.copy(totalPlayTimeMillis = 0L), "PS", emptyList()).playTime)
        assertEquals("2 hr", detailPanelContentFor(item.copy(totalPlayTimeMillis = (2 * 60 + 5) * 60_000L), "PS").playTime)
    }

    @Test
    fun `a row with no video and no gallery walks logo to info`() {
        val noArt = detailPanelContentFor(item, "PlayStation")

        assertEquals(listOf(DetailPanelPage.LOGO, DetailPanelPage.INFO), noArt.pages)
        assertEquals(DetailPanelPage.INFO, stepPanelPage(DetailPanelPage.LOGO, noArt.pages, +1))
    }
}
