package com.psplauncher.feature.achievements.provider

import com.psplauncher.feature.achievements.api.ProviderSyncResult

interface RemoteAchievementSource {
    suspend fun fetch(providerGameId: String): ProviderSyncResult
}
