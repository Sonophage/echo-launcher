package com.echo.feature.crossbar.ui.detail

import com.echo.feature.artwork.api.SgdbArtType
import com.echo.feature.artwork.store.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SgdbGridDimensionsTest {
    @Test
    fun `the tile slot asks for vertical grids only, or SteamGridDB returns landscape too`() {
        assertEquals(
            SGDB_PORTRAIT_GRIDS,
            sgdbGridDimensions(ArtworkKind.ICON, SgdbArtType.GRID),
        )
    }

    @Test
    fun `every size it asks for is taller than it is wide`() {
        SGDB_PORTRAIT_GRIDS.forEach { size ->
            val (w, h) = size.split("x").map { it.toInt() }
            assertTrue("$size is not a vertical banner", h > w)
        }
    }

    @Test
    fun `the filter is a grid thing, so the other endpoints are left unfiltered`() {
        listOf(SgdbArtType.HERO, SgdbArtType.LOGO, SgdbArtType.ICON).forEach { type ->
            assertEquals(
                "$type must not carry grid dimensions",
                emptyList<String>(),
                sgdbGridDimensions(ArtworkKind.ICON, type),
            )
        }
    }

    @Test
    fun `slots that want the whole catalogue still get it`() {
        assertEquals(
            emptyList<String>(),
            sgdbGridDimensions(ArtworkKind.SCREENSHOT, SgdbArtType.GRID),
        )
        assertEquals(
            emptyList<String>(),
            sgdbGridDimensions(ArtworkKind.BACKGROUND, SgdbArtType.GRID),
        )
    }
}
