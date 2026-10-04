package com.echo.feature.achievements.api

data class SyncedCoin(
    val providerAchievementId: String,
    val title: String,
    val description: String,

    val globalRarity: Double,
    val iconUrl: String?,
    val isHidden: Boolean,
    val isEarned: Boolean,
    val earnedAt: Long?,
    val points: Int? = null,
) {
    companion object {
        const val RARITY_UNAVAILABLE = -1.0
    }
}

sealed interface ProviderSyncResult {
    data class Success(val providerGameId: String, val coins: List<SyncedCoin>) : ProviderSyncResult

    data object MissingCredentials : ProviderSyncResult

    data object NotLinked : ProviderSyncResult

    data object ProfileNotPublic : ProviderSyncResult

    data object NotFound : ProviderSyncResult

    data class Failed(val reason: String) : ProviderSyncResult
}
