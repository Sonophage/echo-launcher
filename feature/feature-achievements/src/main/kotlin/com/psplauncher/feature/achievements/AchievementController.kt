package com.psplauncher.feature.achievements

import com.psplauncher.core.domain.achievement.Achievement
import com.psplauncher.core.domain.achievement.AchievementProvider
import com.psplauncher.core.domain.achievement.AchievementSet
import com.psplauncher.core.domain.achievement.AchievementTotals
import com.psplauncher.feature.achievements.api.ProviderSyncResult
import kotlinx.coroutines.flow.Flow

interface AchievementController {

    fun observeSetForGame(gameId: Long): Flow<AchievementSet?>

    fun observeSets(): Flow<List<AchievementSet>>

    fun observeAchievements(provider: AchievementProvider, providerGameId: String): Flow<List<Achievement>>

    fun observeTotals(): Flow<AchievementTotals>

    suspend fun syncAccountEntry(provider: AchievementProvider, providerGameId: String, title: String): ProviderSyncResult

    suspend fun syncGame(gameId: Long, provider: AchievementProvider, providerGameId: String): ProviderSyncResult

    suspend fun syncGameById(gameId: Long): ProviderSyncResult

    suspend fun syncAllLinked(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): BatchSyncResult

    suspend fun linkManually(gameId: Long, provider: AchievementProvider, providerGameId: String)

    suspend fun resolveSteamLink(gameId: Long, title: String): String?
}
