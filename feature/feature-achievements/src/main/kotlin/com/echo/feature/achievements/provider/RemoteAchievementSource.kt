package com.echo.feature.achievements.provider

import com.echo.feature.achievements.api.ProviderSyncResult

interface RemoteAchievementSource {
    suspend fun fetch(providerGameId: String): ProviderSyncResult
}
