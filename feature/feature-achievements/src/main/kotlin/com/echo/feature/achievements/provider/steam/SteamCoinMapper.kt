package com.echo.feature.achievements.provider.steam

import com.echo.feature.achievements.api.SyncedCoin

internal object SteamCoinMapper {
    fun map(
        schema: List<SteamSchemaAchievement>,
        percentByName: Map<String, Double>,
        earnedByName: Map<String, SteamPlayerAchievement>,
    ): List<SyncedCoin> = schema.map { a ->
        val earned = earnedByName[a.name]
        val isEarned = earned?.achieved == 1
        SyncedCoin(
            providerAchievementId = a.name,
            title = a.displayName ?: a.name,
            description = a.description.orEmpty(),
            globalRarity = percentByName[a.name] ?: SyncedCoin.RARITY_UNAVAILABLE,
            iconUrl = if (isEarned) a.icon else (a.icongray ?: a.icon),
            isHidden = a.hidden == 1,
            isEarned = isEarned,
            earnedAt = earned?.unlocktime?.takeIf { it > 0 }?.times(1_000),
        )
    }
}
