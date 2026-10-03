package com.psplauncher.feature.achievements

import com.psplauncher.core.data.database.dao.AccountAchievementSetDao
import com.psplauncher.core.data.database.entity.AccountAchievementSetEntity
import com.psplauncher.core.domain.achievement.AchievementProvider
import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.provider.retro.RaProgressEntry
import com.psplauncher.feature.achievements.provider.retro.RaProgressResult
import com.psplauncher.feature.achievements.provider.retro.RaRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

data class RaImportResult(
    val total: Int,
    val imported: Int,
    val noCoins: Int,
    val failed: Int,
    val missingCredentials: Boolean,
)

@Singleton
class RaAccountImporter @Inject constructor(
    private val raSource: RaRemoteDataSource,
    private val setDao: AccountAchievementSetDao,
    private val repository: AchievementRepository,
) {
    suspend fun import(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): RaImportResult {
        val discovered = when (val walk = raSource.userCompletionProgress()) {
            is RaProgressResult.Success -> walk.entries
            RaProgressResult.MissingCredentials -> return RaImportResult(0, 0, 0, 0, missingCredentials = true)
            is RaProgressResult.Failed -> return RaImportResult(0, 0, 0, 1, missingCredentials = false)
        }

        discovered.filter { it.earned > 0 }.forEach { entry ->
            setDao.insertIfAbsent(entry.toStub())
            entry.iconUrl?.let { setDao.backfillIcon(RA.name, entry.gameId, it) }
        }

        val pending = setDao.getUnsyncedSets(RA.name)
        var imported = 0
        var noCoins = 0
        var failed = 0
        var missingCredentials = false
        for ((index, stub) in pending.withIndex()) {
            onProgress(index, pending.size)
            when (repository.syncAccountEntry(RA, stub.providerGameId, stub.title)) {
                is ProviderSyncResult.Success -> imported++
                ProviderSyncResult.NotFound -> noCoins++
                ProviderSyncResult.MissingCredentials -> { missingCredentials = true; break }
                else -> failed++
            }
        }
        onProgress(pending.size, pending.size)
        return RaImportResult(
            total = pending.size,
            imported = imported,
            noCoins = noCoins,
            failed = failed,
            missingCredentials = missingCredentials,
        )
    }

    private companion object {
        val RA = AchievementProvider.RETRO_ACHIEVEMENTS
    }
}

private fun RaProgressEntry.toStub() = AccountAchievementSetEntity(
    provider = AchievementProvider.RETRO_ACHIEVEMENTS.name,
    providerGameId = gameId,
    title = title,
    iconUrl = iconUrl,
)
