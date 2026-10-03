package com.psplauncher.feature.achievements.provider.steam

import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.provider.RemoteAchievementSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAchievementsSource @Inject constructor(
    private val remote: SteamRemoteDataSource,
) : RemoteAchievementSource {
    override suspend fun fetch(providerGameId: String): ProviderSyncResult = remote.fetch(providerGameId)
}
