package com.echo.feature.artwork.store

import com.echo.core.data.database.entity.ArtworkRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class GameArtColumnTest {
    @Test
    fun `the three kinds that have a column on games reach it`() {
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
        assertNull(gameArtColumnFor(ArtworkKind.ICON, 1))
        assertNull(gameArtColumnFor(ArtworkKind.BACKGROUND, 2))
        assertNull(gameArtColumnFor(ArtworkKind.SCREENSHOT, 1))
    }

    @Test
    fun `a relinked cover fills the icon slot and a miximage the background, nothing else gains a column`() {
        assertEquals(GameArtColumn.ICON, relinkColumnFor(ArtworkKind.BOX_ART))
        assertEquals(GameArtColumn.ARTWORK, relinkColumnFor(ArtworkKind.HERO))
        ArtworkKind.entries
            .filter { gameArtColumnFor(it, 0) == null && it != ArtworkKind.BOX_ART && it != ArtworkKind.HERO }
            .forEach { assertNull("$it must not fill a games column on relink", relinkColumnFor(it)) }
    }

    @Test
    fun `a rescrape forgets only what a scraper fetched`() {
        val scraped = ArtworkRecordEntity(
            gameId = 1L, platformId = "gba", artworkType = "ICON", sortOrder = 0, portableName = "Game",
            relativePath = "Artwork/gba/pfp/icon0/Game.png", documentUri = "content://x", source = "scrape",
        )
        assertTrue(isRescrapable(scraped))
        assertFalse("a relinked file is his", isRescrapable(scraped.copy(source = "relink")))
        assertFalse("a pick is his", isRescrapable(scraped.copy(userAssigned = true)))
        assertFalse("a lock is his", isRescrapable(scraped.copy(locked = true)))
    }
}
