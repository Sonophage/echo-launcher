package com.echo.feature.achievements

import com.echo.core.domain.achievement.Achievement
import com.echo.core.domain.achievement.AchievementProvider
import com.echo.core.domain.achievement.AchievementSet
import com.echo.core.domain.achievement.AchievementTotals
import com.echo.feature.achievements.api.ProviderSyncResult
import kotlinx.coroutines.flow.Flow

interface AchievementController {

    fun observeSetForGame(gameId: Long): Flow<AchievementSet?>

    fun observeSets(): Flow<List<AchievementSet>>

    fun observeAchievements(provider: AchievementProvider, providerGameId: String): Flow<List<Achievement>>

    fun observeAllAchievements(): Flow<Map<Pair<AchievementProvider, String>, List<Achievement>>>

    fun observeTotals(): Flow<AchievementTotals>

    suspend fun syncAccountEntry(provider: AchievementProvider, providerGameId: String, title: String): ProviderSyncResult

    suspend fun syncGame(gameId: Long, provider: AchievementProvider, providerGameId: String): ProviderSyncResult

    suspend fun syncGameById(gameId: Long): ProviderSyncResult

    suspend fun syncAllLinked(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): BatchSyncResult

    suspend fun linkManually(gameId: Long, provider: AchievementProvider, providerGameId: String)

    suspend fun resolveSteamLink(gameId: Long, title: String): String?
}
