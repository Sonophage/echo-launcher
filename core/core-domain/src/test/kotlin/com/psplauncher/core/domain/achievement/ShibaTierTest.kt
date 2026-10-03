package com.psplauncher.core.domain.achievement

import org.junit.Assert.assertEquals
import org.junit.Test

class ShibaTierTest {
    @Test
    fun `rarity below 10 percent is Gold`() {
        assertEquals(ShibaTier.GOLD, ShibaTier.forRarity(0.5))
        assertEquals(ShibaTier.GOLD, ShibaTier.forRarity(9.99))
    }

    @Test
    fun `rarity between 10 and 25 percent is Silver`() {
        assertEquals(ShibaTier.SILVER, ShibaTier.forRarity(10.0))
        assertEquals(ShibaTier.SILVER, ShibaTier.forRarity(24.99))
    }

    @Test
    fun `rarity at or above 25 percent is Bronze`() {
        assertEquals(ShibaTier.BRONZE, ShibaTier.forRarity(25.0))
        assertEquals(ShibaTier.BRONZE, ShibaTier.forRarity(93.7))
    }

    @Test
    fun `zero percent with valid data is ultra-rare Gold, missing data is Bronze`() {
        assertEquals(ShibaTier.GOLD, ShibaTier.forRarity(0.0))
        assertEquals(ShibaTier.BRONZE, ShibaTier.forRarity(null))
    }

    @Test
    fun `forRarity never returns Platinum`() {
        val tiers = (0..100).map { ShibaTier.forRarity(it.toDouble()) }
        assertEquals(false, tiers.contains(ShibaTier.PLATINUM))
    }

    @Test
    fun `RA points map to tiers by difficulty`() {
        assertEquals(ShibaTier.BRONZE, ShibaTier.forRaPoints(0))
        assertEquals(ShibaTier.BRONZE, ShibaTier.forRaPoints(5))

        assertEquals(ShibaTier.SILVER, ShibaTier.forRaPoints(10))
        assertEquals(ShibaTier.SILVER, ShibaTier.forRaPoints(25))
        assertEquals(ShibaTier.SILVER, ShibaTier.forRaPoints(49))

        assertEquals(ShibaTier.GOLD, ShibaTier.forRaPoints(50))
        assertEquals(ShibaTier.GOLD, ShibaTier.forRaPoints(100))
    }

    @Test
    fun `forRaPoints never returns Platinum`() {
        val tiers = (0..200).map { ShibaTier.forRaPoints(it) }
        assertEquals(false, tiers.contains(ShibaTier.PLATINUM))
    }
}
