package com.psplauncher.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscFaceUriTest {
    private fun game(artwork: String? = null, icon: String? = null) =
        Game(id = 1L, title = "Crash", platformId = "psx", artworkUri = artwork, iconUri = icon)

    @Test
    fun `a game with only a background still gets a disc face`() {
        assertEquals(
            "content://bg",
            game(artwork = "content://bg").discFaceUri,
            "the boot screen spins this image. Most scraped games carry a background and no " +
                "icon, so dropping it from the chain leaves them with a blank disc.",
        )
    }

    @Test
    fun `a game with only an icon falls back to it`() {
        assertEquals("content://icon", game(icon = "content://icon").discFaceUri)
    }

    @Test
    fun `the background wins over the tile`() {
        assertEquals(
            "content://bg",
            game(artwork = "content://bg", icon = "content://icon").discFaceUri,
            "a tile is 144x80 and crops badly onto a disc; the background is the larger image",
        )
    }

    @Test
    fun `a game with no art at all has no disc face rather than an empty string`() {
        assertNull(game().discFaceUri)
        assertNull(game(artwork = "", icon = "   ").discFaceUri)
    }
}
