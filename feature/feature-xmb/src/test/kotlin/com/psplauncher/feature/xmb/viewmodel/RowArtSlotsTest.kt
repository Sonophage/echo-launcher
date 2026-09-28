package com.psplauncher.feature.xmb.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RowArtSlotsTest {
    private val bySlot = mapOf(
        "coverUri" to XMBItem(id = "2", title = "t", coverUri = "file:///cover.png"),
        "artworkUri" to XMBItem(id = "3", title = "t", artworkUri = "file:///art.png"),
        "iconUri" to XMBItem(id = "5", title = "t", iconUri = "file:///icon.png"),
    )

    @Test
    fun `every art slot is seen by both the backdrop list and the shelf card`() {
        val missedByCard = bySlot.filterValues { it.shelfCoverArt == null }.keys
        val missedByBackdrop = bySlot.filterValues { it.backdropArt.isEmpty() }.keys

        assertEquals(
            "these slots fill a row's art but the home shelf card cannot see them, so the card " +
                "falls back to drawing the title as text: $missedByCard",
            emptySet<String>(),
            missedByCard,
        )
        assertEquals(emptySet<String>(), missedByBackdrop)
    }

    @Test
    fun `the card prefers a portrait cover and the backdrop prefers a landscape`() {
        val all = XMBItem(
            id = "6", title = "t",
            coverUri = "file:///cover.png", artworkUri = "file:///art.png",
        )

        assertEquals("file:///cover.png", all.shelfCoverArt)
        assertEquals("file:///art.png", all.backdropArt.first())
    }

    /**
     * toXmbItems fills artworkUri, iconUri and logoUri and never coverUri, so this is the
     * shape a real recently played game arrives in. The test above states that the card
     * prefers a portrait cover -- but it says so using a coverUri no game ever carries, so
     * it passed while the recents card drew ArtworkKind.BACKGROUND, a 16:9 image, inside a
     * 2:3 card. The tile (ArtworkKind.ICON -> iconUri) is the curated crossbar image.
     */
    @Test
    fun `a recents row shaped like a real game shows the tile, not the background`() {
        val asBuiltByToXmbItems = XMBItem(
            id = "10", title = "t",
            artworkUri = "file:///background.png",
            iconUri = "file:///tile.png",
            logoUri = "file:///logo.png",
        )

        assertEquals("file:///tile.png", asBuiltByToXmbItems.tileArt)

        assertEquals("file:///background.png", asBuiltByToXmbItems.backdropArt.first())
    }

    @Test
    fun `the tile falls back so a row that has only a background still draws something`() {
        assertEquals(
            "file:///background.png",
            XMBItem(id = "11", title = "t", artworkUri = "file:///background.png").tileArt,
        )
        assertNull(XMBItem(id = "12", title = "t").tileArt)
        assertNull(XMBItem(id = "13", title = "t", iconUri = "  ").tileArt)
    }

    @Test
    fun `a row with no art at all has none, rather than an empty string`() {
        val bare = XMBItem(id = "7", title = "t")

        assertNull(bare.shelfCoverArt)
        assertEquals(emptyList<String>(), bare.backdropArt)
        assertNull(XMBItem(id = "8", title = "t", coverUri = "  ").shelfCoverArt)
        assertNotNull(XMBItem(id = "9", title = "t", coverUri = "file:///c.png").shelfCoverArt)
    }
}
