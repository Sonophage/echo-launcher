package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossbarBackdropArtTest {
    @Test
    fun `a game reads its background slot`() {
        val game = CrossbarItem(
            id = "g1",
            title = "Crash",
            artworkUri = "content://bg",
            isRealGame = true,
        )
        assertEquals(listOf("content://bg"), game.backdropArt)
    }

    @Test
    fun `every library's own art field is read, not just a game's`() {
        listOf(
            CrossbarItem(id = "t1", title = "Track", coverUri = "content://album", type = CrossbarItemType.MUSIC_TRACK),
            CrossbarItem(id = "v1", title = "Clip", coverUri = "content://thumb", type = CrossbarItemType.VIDEO_FILE),
            CrossbarItem(id = "p1", title = "Photo", coverUri = "content://photo", type = CrossbarItemType.PHOTO_FILE),
        ).forEach { row ->
            assertTrue("${row.id} must be able to colour the shell", row.backdropArt.isNotEmpty())
        }
    }

    @Test
    fun `the shared folder icon never colours the shell`() {
        val folder = CrossbarItem(
            id = "all_music",
            title = "Music",
            coverUri = CrossbarViewModel.MEMORY_CARD_ASSET_URI,
            type = CrossbarItemType.MEMORY_CARD,
        )
        assertEquals(emptyList<String>(), folder.backdropArt)
    }

    @Test
    fun `a folder row with real art of its own still counts`() {
        val library = CrossbarItem(id = "lib1", title = "Movies", coverUri = "content://library-art")
        assertEquals(listOf("content://library-art"), library.backdropArt)
    }

    @Test
    fun `a logo with no artwork behind it is not a visible logo`() {
        val orphanLogo = CrossbarItem(id = "g1", title = "Crash", logoUri = "content://logo", isRealGame = true)
        assertTrue(orphanLogo.backdropArt.isEmpty())
        assertTrue("a row with no art must keep its title", !orphanLogo.hasVisibleLogo)
    }

    @Test
    fun `a logo with any readable art behind it is a visible logo`() {
        listOf(
            CrossbarItem(id = "a", title = "x", logoUri = "content://l", artworkUri = "content://bg"),
            CrossbarItem(id = "b", title = "x", logoUri = "content://l", coverUri = "content://cover"),
            CrossbarItem(id = "c", title = "x", logoUri = "content://l", iconUri = "content://icon"),
        ).forEach { assertTrue("${it.id} should draw its logo", it.hasVisibleLogo) }
    }

    @Test
    fun `no logo is never a visible logo, however much art there is`() {
        val noLogo = CrossbarItem(id = "g2", title = "Crash", artworkUri = "content://bg", isRealGame = true)
        assertTrue(!noLogo.hasVisibleLogo)

        assertTrue(!CrossbarItem(id = "g3", title = "x", logoUri = "  ", artworkUri = "content://bg").hasVisibleLogo)
    }

    @Test
    fun `a row with no art at all colours nothing`() {
        assertEquals(emptyList<String>(), CrossbarItem(id = "settings_library", title = "Settings").backdropArt)
        assertEquals(emptyList<String>(), CrossbarItem(id = "x", title = "X", artworkUri = "   ").backdropArt)
    }
}
