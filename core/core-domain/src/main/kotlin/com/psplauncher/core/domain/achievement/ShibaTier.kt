package com.psplauncher.core.domain.achievement

enum class ShibaTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM;

    companion object {
        const val GOLD_MAX_RARITY = 10.0

        const val SILVER_MAX_RARITY = 25.0

        const val GOLD_MIN_POINTS = 50

        const val SILVER_MIN_POINTS = 10

        fun forRaPoints(points: Int): ShibaTier = when {
            points >= GOLD_MIN_POINTS   -> GOLD
            points >= SILVER_MIN_POINTS -> SILVER
            else                        -> BRONZE
        }

        fun forRarity(globalUnlockPercent: Double?): ShibaTier {
            val rarity = globalUnlockPercent ?: return BRONZE
            return when {
                rarity < GOLD_MAX_RARITY   -> GOLD
                rarity < SILVER_MAX_RARITY -> SILVER
                else                       -> BRONZE
            }
        }
    }
}
