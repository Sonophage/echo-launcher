package com.psplauncher.feature.artwork.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameArtColumnTest {
    @Test
    fun `the three kinds that have a column on games reach it`() {
        // The crossbar reads these columns, not the records. The studio wrote the file
        // and the record but left the column naming the old file, so a pick whose
        // extension differed left the column pointing at something deleted.
        assertEquals(GameArtColumn.ICON, gameArtColumnFor(ArtworkKind.ICON, 0))
        assertEquals(GameArtColumn.ARTWORK, gameArtColumnFor(ArtworkKind.BACKGROUND, 0))
        assertEquals(GameArtColumn.LOGO, gameArtColumnFor(ArtworkKind.LOGO, 0))
    }

    @Test
    fun `every other kind has no column, so nothing is clobbered`() {
        ArtworkKind.entries
            .filter { it !in setOf(ArtworkKind.ICON, ArtworkKind.BACKGROUND, ArtworkKind.LOGO) }
            .forEach { assertNull("$it must not write a games column", gameArtColumnFor(it, 0)) }
    }

    @Test
    fun `only the primary asset repoints, because a column names one file`() {
        // A second screenshot or preview video is not what a column points at.
        assertNull(gameArtColumnFor(ArtworkKind.ICON, 1))
        assertNull(gameArtColumnFor(ArtworkKind.BACKGROUND, 2))
        assertNull(gameArtColumnFor(ArtworkKind.SCREENSHOT, 1))
    }
}
