package com.echo.feature.achievements

import com.echo.core.data.database.dao.AccountAchievementDao
import com.echo.core.data.database.dao.AccountAchievementSetDao
import com.echo.core.data.database.dao.ProviderGameLinkDao
import com.echo.core.data.database.dao.SteamOwnedGamesDao
import com.echo.core.data.database.entity.SteamNoAchievementsEntity
import com.echo.core.data.database.entity.SteamOwnedGameEntity
import com.echo.core.domain.achievement.AchievementProvider
import com.echo.feature.achievements.api.ProviderSyncResult
import com.echo.feature.achievements.provider.steam.SteamOwnedGamesResult
import com.echo.feature.achievements.provider.steam.SteamRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

data class SteamImportResult(
    val total: Int,
    val imported: Int,
    val noCoins: Int,
    val noProgress: Int,
    val failed: Int,
    val missingCredentials: Boolean,
    val profileNotPublic: Boolean,
)

@Singleton
class SteamAccountImporter @Inject constructor(
    private val steamSource: SteamRemoteDataSource,
    private val ownedDao: SteamOwnedGamesDao,
    private val setDao: AccountAchievementSetDao,
    private val coinDao: AccountAchievementDao,
    private val linkDao: ProviderGameLinkDao,
    private val repository: AchievementRepository,
) {
    suspend fun import(onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }): SteamImportResult {
        when (val owned = steamSource.ownedGames()) {
            is SteamOwnedGamesResult.Success -> {
                val now = System.currentTimeMillis()
                ownedDao.replaceOwned(owned.entries.map {
                    SteamOwnedGameEntity(
                        appid = it.appId,
                        name = it.name,
                        playtimeForeverMinutes = it.playtimeForeverMinutes,
                        fetchedAt = now,
                    )
                })
            }
            SteamOwnedGamesResult.MissingCredentials -> return emptyResult(missingCredentials = true)
            SteamOwnedGamesResult.ProfileNotPublic -> return emptyResult(profileNotPublic = true)
            is SteamOwnedGamesResult.Failed -> return emptyResult(failed = 1)
        }

        val memo = ownedDao.noAchievementAppids().toHashSet()
        val candidates = ownedDao.getAll()
            .filter { it.appid !in memo && it.syncedPlaytimeMinutes != it.playtimeForeverMinutes }

        var imported = 0
        var noCoins = 0
        var noProgress = 0
        var failed = 0
        var missingCredentials = false
        var profileNotPublic = false
        for ((index, game) in candidates.withIndex()) {
            onProgress(index, candidates.size)
            when (val result = repository.syncAccountEntry(STEAM, game.appid, game.name)) {
                is ProviderSyncResult.Success -> {
                    if (hasProgress(result)) imported++
                    else { discardUnlessLibraryLinked(game.appid); noProgress++ }
                    ownedDao.markSynced(game.appid)
                }
                ProviderSyncResult.NotFound -> {
                    ownedDao.rememberNoAchievements(
                        SteamNoAchievementsEntity(game.appid, System.currentTimeMillis()),
                    )
                    ownedDao.markSynced(game.appid)
                    noCoins++
                }
                ProviderSyncResult.MissingCredentials -> { missingCredentials = true; break }
                ProviderSyncResult.ProfileNotPublic -> { profileNotPublic = true; break }
                else -> failed++
            }
        }
        onProgress(candidates.size, candidates.size)
        return SteamImportResult(
            total = candidates.size,
            imported = imported,
            noCoins = noCoins,
            noProgress = noProgress,
            failed = failed,
            missingCredentials = missingCredentials,
            profileNotPublic = profileNotPublic,
        )
    }

    private fun hasProgress(result: ProviderSyncResult.Success): Boolean =
        result.coins.any { it.isEarned }

    private suspend fun discardUnlessLibraryLinked(appId: String) {
        if (linkDao.linkExistsFor(STEAM.name, appId)) return
        coinDao.deleteForSet(STEAM.name, appId)
        setDao.deleteSet(STEAM.name, appId)
    }

    private fun emptyResult(
        failed: Int = 0,
        missingCredentials: Boolean = false,
        profileNotPublic: Boolean = false,
    ) = SteamImportResult(
        total = 0, imported = 0, noCoins = 0, noProgress = 0, failed = failed,
        missingCredentials = missingCredentials, profileNotPublic = profileNotPublic,
    )

    private companion object {
        val STEAM = AchievementProvider.STEAM
    }
}
