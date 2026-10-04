package com.echo.core.domain.achievement

data class AchievementSet(
    val provider: AchievementProvider,
    val providerGameId: String,
    val gameId: Long?,
    val title: String,
    val iconUrl: String?,
    val total: Int,
    val unlocked: Int,
    val points: Int,
    val earnedPoints: Int,
    val mastered: Boolean,
    val lastSyncedAt: Long?,
    val lastPlayedAt: Long?,
    val platformId: String? = null,
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val isHidden: Boolean,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val globalPercent: Double?,
    val points: Int?,
)

data class AchievementTotals(
    val unlocked: Int,
    val total: Int,
    val raPoints: Int,
)
