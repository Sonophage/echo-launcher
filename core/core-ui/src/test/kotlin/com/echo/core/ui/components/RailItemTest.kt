package com.echo.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-09: Music's artists run to hundreds, more than the rail can draw. The rail draws one rung per
// initial; a finger reaches every artist, and the glyph under it is always the chosen artist's
class RailItemTest {
    // 30 artists: 10 under A, 15 under B, 5 under C
    private val artists = List(10) { RailRung("A", "A$it") } + List(15) { RailRung("B", "B$it") } + List(5) { RailRung("C", "C$it") }
    private val runs = glyphRuns(artists)

    // three 20 px rungs filling a 60 px rail
    private fun at(along: Float) = railItemAt(along, extent = 60f, spanPx = 60f, pitchPx = 20f, drawn = runs, count = artists.size)

    @Test
    fun `one rung per initial`() = assertEquals(listOf(0, 10, 25), runs)

    @Test
    fun `a finger reaches every item, and the glyph under it is the item's`() {
        val picks = (0 until 600).map { at(it / 10f) }
        assertEquals((0 until 30).toSet(), picks.toSet())
        (0 until 600).forEach { tenth ->
            val rung = (tenth / 10f / 20f).toInt().coerceIn(0, 2)
            assertEquals(artists[runs[rung]].glyph, artists[at(tenth / 10f)].glyph, "at ${tenth / 10f} px")
        }
    }

    @Test
    fun `letters, one item to a rung, pick the rung under the finger`() {
        val letters = letterRungs(listOf('A', 'B', 'C'))
        assertEquals(1, railItemAt(25f, 60f, 60f, 20f, glyphRuns(letters), letters.size))
    }

    // seen on the Konker: the rail had room for fewer rungs than initials, so J's artists sat under the I rung and
    // "Jhené Aiko" was labelled beside an I
    @Test
    fun `the chosen rung shows the chosen item's glyph when its own initial is not drawn`() {
        val rungs = listOf(RailRung("H", "Hozier"), RailRung("I", "Imagine Dragons"), RailRung("J", "Jhené Aiko"), RailRung("K", "Kehlani"))
        val drawn = listOf(0, 1, 3)
        assertEquals(listOf("H", "J", "K"), railGlyphs(rungs, drawn, cursor = 2))
        assertEquals(listOf("H", "I", "K"), railGlyphs(rungs, drawn, cursor = null))
    }
}
