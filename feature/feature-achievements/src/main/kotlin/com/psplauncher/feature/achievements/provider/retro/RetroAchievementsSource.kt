package com.psplauncher.feature.achievements.provider.retro

import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.provider.RemoteAchievementSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetroAchievementsSource @Inject constructor(
    private val remote: RaRemoteDataSource,
) : RemoteAchievementSource {
    override suspend fun fetch(providerGameId: String): ProviderSyncResult = remote.fetch(providerGameId)
}
