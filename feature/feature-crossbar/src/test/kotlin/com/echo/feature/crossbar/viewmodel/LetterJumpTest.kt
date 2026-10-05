package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.components.LETTER_JUMP_MIN_ITEMS
import com.echo.core.ui.components.at
import com.echo.core.ui.components.move
import com.echo.core.ui.components.initialOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterJumpTest {
    private fun item(title: String) = CrossbarItem(id = title, title = title)

    private fun alphabetical(n: Int = 40): List<CrossbarItem> =
        (0 until n).map { item("${('A' + it % 26)}$it title") }.sortedBy { it.title.lowercase() }

    @Test
    fun `an alphabetical list gets one rung per distinct initial`() {
        val anchors = letterAnchors(alphabetical(40))
        assertNotNull("40 alphabetical rows should raise a rail", anchors)

        assertEquals("one rung per distinct initial", 26, anchors!!.size)
        assertEquals("the rail starts at A", 'A', anchors.first().letter)
        assertEquals("A's rung points at the first row", 0, anchors.first().index)
    }

    // owner, 2026-10-05: Recent and the game lists sorted by play date get the rail too; a rung takes the
    // cursor to that letter's first row in the list's own order, its most recent one
    @Test
    fun `a list sorted by date still gets a rail, each letter at its first row`() {
        val byDate = listOf("Zelda 1", "Astro Bot", "Metroid", "Zelda 2", "Barnyard", "Astro Two", "Yakuza", "Metroid 2", "Barnyard 2", "Yakuza 2")
            .map(::item)
        assertTrue("fixture must clear the length minimum", byDate.size >= LETTER_JUMP_MIN_ITEMS)
        val anchors = letterAnchors(byDate)
        assertNotNull(anchors)
        assertEquals("the rungs still read A to Z", listOf('A', 'B', 'M', 'Y', 'Z'), anchors!!.map { it.letter })
        assertEquals("Z points at the first Zelda, not the second", 0, anchors.first { it.letter == 'Z' }.index)
        assertEquals("the rail opens on the rung of the row under the cursor", 'M', letterJumpFor(byDate, currentIndex = 7)!!.letter)
    }

    @Test
    fun `a short list gets no rail`() {
        val short = (0 until LETTER_JUMP_MIN_ITEMS - 1).map { item("${('A' + it)}") }
        assertNull("below the minimum the rail is not worth the screen", letterAnchors(short))
    }

    @Test
    fun `a long list with too few letters gets no rail`() {
        val twoLetters = (0 until 15).map { item("Alpha $it") } + (0 until 15).map { item("Beta $it") }
        assertNull("two rungs is a toggle, not a scrubber", letterAnchors(twoLetters))
    }

    @Test
    fun `digits and punctuation file under hash, and hash leads`() {
        val mixed = (0 until 10).map { item("$it Racer") } +
            (0 until 10).map { item("Alpha $it") } +
            (0 until 10).map { item("Beta $it") }
        val anchors = letterAnchors(mixed.sortedBy { it.title.lowercase() })
        assertNotNull(anchors)
        assertEquals("non-letter initials lead, as they do in the sort", '#', anchors!!.first().letter)
        assertEquals("three rungs: # A B", 3, anchors.size)
    }

    @Test
    fun `an accented initial keeps its own rung instead of breaking the rail`() {
        val withAccent = (0 until 12).map { item("Alpha $it") } +
            (0 until 12).map { item("Beta $it") } +
            (0 until 12).map { item("Étude $it") }
        val anchors = letterAnchors(withAccent.sortedBy { it.title.lowercase() })
        assertNotNull("one accented title must not withhold the whole rail", anchors)
        assertEquals("A B É", 3, anchors!!.size)
        assertEquals('É', anchors.last().letter)
    }

    @Test
    fun `the rail opens on the rung the cursor is already in`() {
        val items = alphabetical(40)
        val anchors = letterAnchors(items)!!
        val cRung = anchors.indexOfFirst { it.letter == 'C' }

        val partwayThroughC = anchors[cRung].index + 1
        val state = letterJumpFor(items, currentIndex = partwayThroughC)
        assertNotNull(state)
        assertEquals("opens where the eye already is, not at A", 'C', state!!.letter)
        assertEquals(
            "remembers where the cursor actually was, which is not where the rung points",
            partwayThroughC,
            state.returnIndex,
        )
        assertNotEquals(
            "fixture is only honest while those two differ",
            state.returnIndex,
            state.targetIndex,
        )
    }

    @Test
    fun `moving the rail stops at both ends instead of wrapping`() {
        val state = letterJumpFor(alphabetical(40), currentIndex = 0)!!
        assertEquals("already at the top, up is a no-op", state, state.move(-1))
        val end = state.move(1000)
        assertEquals("walks to the last rung and stops", state.anchors.lastIndex, end.cursor)
        assertEquals("already at the end, down is a no-op", end, end.move(1))
    }

    @Test
    fun `a rung the rail could not have drawn clamps instead of throwing`() {
        val state = letterJumpFor(alphabetical(40), currentIndex = 20)!!
        assertEquals("the first rung is reachable", 0, state.at(0).cursor)
        assertEquals("the last rung is reachable", state.anchors.lastIndex, state.at(999).cursor)
        assertEquals("a negative rung clamps rather than throwing", 0, state.at(-5).cursor)
    }

    @Test
    fun `the target index is the first row of the chosen letter`() {
        val items = alphabetical(40)
        val state = letterJumpFor(items, currentIndex = 0)!!.move(2)
        val landed = items[state.targetIndex]
        assertEquals(
            "the row under the cursor starts with the letter the rung shows",
            state.letter,
            initialOf(landed.title),
        )
    }
}
