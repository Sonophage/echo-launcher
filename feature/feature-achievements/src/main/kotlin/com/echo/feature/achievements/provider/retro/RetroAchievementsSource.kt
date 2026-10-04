package com.echo.feature.achievements.provider.retro

import com.echo.feature.achievements.api.ProviderSyncResult
import com.echo.feature.achievements.provider.RemoteAchievementSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetroAchievementsSource @Inject constructor(
    private val remote: RaRemoteDataSource,
) : RemoteAchievementSource {
    override suspend fun fetch(providerGameId: String): ProviderSyncResult = remote.fetch(providerGameId)
}
