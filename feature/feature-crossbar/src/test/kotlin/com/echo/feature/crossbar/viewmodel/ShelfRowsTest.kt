package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: inside a shelf, the strip at the left showed a blank card; it is the shelves, with covers
class ShelfRowsTest {
    @Test
    fun `the shelves' rows carry each shelf's covers`() {
        val s = CrossbarUiState(showBootSequence = false, favoritesCount = 2,
            shelfFanCovers = mapOf(SHELF_FAVORITES_ID to listOf("a.png", "b.png")))
        val rows = s.shelfRows()
        assertEquals(listOf(SHELF_FAVORITES_ID), rows.map { it.id })
        assertEquals(listOf("a.png", "b.png"), rows.single().insideCovers)
    }
}
