package com.psplauncher.feature.settings.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OverviewArtTest {
    private val art = (1..20).map { "content://art/$it" }

    @Test
    fun `the two cards never show the same art when there is more than one to choose from`() {
        repeat(200) { seed ->
            val (a, b) = pickOverviewArt(art, Random(seed))
            assertNotEquals("seed $seed picked the same art twice", a, b)
        }
    }

    @Test
    fun `different opens pick different art`() {
        val picks = (0 until 50).map { pickOverviewArt(art, Random(it)).first }.toSet()
        assertTrue("50 opens picked only ${picks.size} distinct pieces of art", picks.size > 5)
    }

    @Test
    fun `one piece of art goes on the first card and none leaves both empty`() {
        assertEquals("content://art/1" to null, pickOverviewArt(listOf("content://art/1"), Random(0)))
        assertEquals(null to null, pickOverviewArt(emptyList(), Random(0)))
    }
}
