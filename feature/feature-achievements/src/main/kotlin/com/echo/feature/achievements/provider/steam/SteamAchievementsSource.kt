package com.echo.feature.achievements.provider.steam

import com.echo.feature.achievements.api.ProviderSyncResult
import com.echo.feature.achievements.provider.RemoteAchievementSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAchievementsSource @Inject constructor(
    private val remote: SteamRemoteDataSource,
) : RemoteAchievementSource {
    override suspend fun fetch(providerGameId: String): ProviderSyncResult = remote.fetch(providerGameId)
}
