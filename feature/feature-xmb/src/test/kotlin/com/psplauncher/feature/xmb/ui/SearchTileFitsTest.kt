package com.psplauncher.feature.xmb.ui

import com.psplauncher.core.ui.components.PfpMediaCardDefaults
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTileFitsTest {
    private fun cardHeightDp(widthDp: Float): Float =
        widthDp / PfpMediaCardDefaults.ArtRatio + SEARCH_CARD_TEXT_HEIGHT.value

    @Test
    fun `with the keyboard up a card still has room for its name`() {
        val height = cardHeightDp(SEARCH_TILE_TARGET_WIDTH_IME.value)
        assertTrue(
            "a ${SEARCH_TILE_TARGET_WIDTH_IME.value}dp tile is ${height}dp tall, " +
                "which does not fit ${SEARCH_GRID_BAND_WITH_IME.value}dp",
            height <= SEARCH_GRID_BAND_WITH_IME.value,
        )
    }

    @Test
    fun `the full-size tile is the one that does not fit, which is why there are two`() {
        assertTrue(
            "if the normal tile fitted, the keyboard case would need no separate width",
            cardHeightDp(SEARCH_TILE_TARGET_WIDTH.value) > SEARCH_GRID_BAND_WITH_IME.value,
        )
        assertTrue(SEARCH_TILE_TARGET_WIDTH_IME < SEARCH_TILE_TARGET_WIDTH)
    }
}
