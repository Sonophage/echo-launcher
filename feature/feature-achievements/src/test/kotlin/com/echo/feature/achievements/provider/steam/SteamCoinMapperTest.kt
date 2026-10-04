package com.echo.feature.achievements.provider.steam

import com.echo.feature.achievements.api.SyncedCoin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamCoinMapperTest {
    private fun schema(
        name: String,
        displayName: String? = name,
        description: String? = "",
        hidden: Int = 0,
    ) = SteamSchemaAchievement(
        name = name, displayName = displayName, description = description, hidden = hidden,
        icon = "$name.jpg", icongray = "$name-gray.jpg",
    )

    private fun earned(name: String, unlockAt: Long = 1_700_000_000) =
        SteamPlayerAchievement(apiname = name, achieved = 1, unlocktime = unlockAt)

    private fun map(
        vararg achievements: SteamSchemaAchievement,
        percents: Map<String, Double> = emptyMap(),
        earned: Map<String, SteamPlayerAchievement> = emptyMap(),
    ) = SteamCoinMapper.map(achievements.toList(), percents, earned)

    @Test
    fun `global percent maps through and a missing one is the unavailable sentinel, not zero`() {
        val coins = map(
            schema("rare"), schema("nodata"), schema("zero"),
            percents = mapOf("rare" to 9.9, "zero" to 0.0),
        ).associateBy { it.providerAchievementId }

        assertEquals(9.9, coins.getValue("rare").globalRarity, 1e-9)
        assertEquals(SyncedCoin.RARITY_UNAVAILABLE, coins.getValue("nodata").globalRarity, 1e-9)
        assertEquals(0.0, coins.getValue("zero").globalRarity, 1e-9)
    }

    @Test
    fun `hidden flag maps through`() {
        val coins = map(schema("secret", hidden = 1), schema("open"))
            .associateBy { it.providerAchievementId }
        assertTrue(coins.getValue("secret").isHidden)
        assertFalse(coins.getValue("open").isHidden)
    }

    @Test
    fun `earned state, unlock time and the colour or grey icon map through`() {
        val coins = map(
            schema("a"), schema("b"),
            earned = mapOf("a" to earned("a", unlockAt = 1_700_000_000)),
        ).associateBy { it.providerAchievementId }

        val a = coins.getValue("a")
        assertTrue(a.isEarned)
        assertEquals(1_700_000_000_000L, a.earnedAt)
        assertEquals("a.jpg", a.iconUrl)

        val b = coins.getValue("b")
        assertFalse(b.isEarned)
        assertNull(b.earnedAt)
        assertEquals("b-gray.jpg", b.iconUrl)
    }
}
