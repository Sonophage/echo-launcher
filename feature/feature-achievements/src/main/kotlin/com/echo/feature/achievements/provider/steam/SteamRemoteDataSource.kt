package com.echo.feature.achievements.provider.steam

import com.echo.core.data.achievement.AchievementCredentialsProvider
import com.echo.feature.achievements.api.ProviderSyncResult
import com.echo.feature.achievements.api.RateLimiter
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class SteamOwnedEntry(
    val appId: String,
    val name: String,
    val playtimeForeverMinutes: Long,
)

sealed interface SteamOwnedGamesResult {
    data class Success(val entries: List<SteamOwnedEntry>) : SteamOwnedGamesResult
    data object MissingCredentials : SteamOwnedGamesResult

    data object ProfileNotPublic : SteamOwnedGamesResult
    data class Failed(val reason: String) : SteamOwnedGamesResult
}

@Singleton
class SteamRemoteDataSource @Inject constructor(
    private val webApi: SteamWebApi,
    private val communityApi: SteamCommunityApi,
    private val credentials: AchievementCredentialsProvider,
) {
    private val rate = RateLimiter(1_100)

    suspend fun resolveVanity(vanity: String): String? {
        val key = credentials.steamApiKey()?.takeIf { it.isNotBlank() } ?: return null
        rate.await()
        return runCatching { webApi.resolveVanityUrl(key, vanity.trim()) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                return null
            }
            .body()?.response?.takeIf { it.success == 1 }?.steamid
    }

    suspend fun ownedGames(): SteamOwnedGamesResult {
        val key = credentials.steamApiKey()?.takeIf { it.isNotBlank() }
            ?: return SteamOwnedGamesResult.MissingCredentials
        val steamId = credentials.steamId64()?.takeIf { it.isNotBlank() }
            ?: return SteamOwnedGamesResult.MissingCredentials

        rate.await()
        val response = runCatching { webApi.getOwnedGames(key, steamId) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                return SteamOwnedGamesResult.Failed("network error")
            }
        if (response.code() == 401 || response.code() == 403) {
            return SteamOwnedGamesResult.Failed("Steam rejected the API key")
        }
        val inner = response.body()?.response
            ?: return SteamOwnedGamesResult.Failed("Steam returned ${response.code()}")
        if (inner.gameCount == 0 && inner.games.isEmpty()) return SteamOwnedGamesResult.ProfileNotPublic

        return SteamOwnedGamesResult.Success(
            inner.games.map {
                SteamOwnedEntry(
                    appId = it.appid.toString(),
                    name = it.name.orEmpty(),
                    playtimeForeverMinutes = it.playtimeForever,
                )
            },
        )
    }

    suspend fun fetch(appId: String): ProviderSyncResult {
        val key = credentials.steamApiKey()?.takeIf { it.isNotBlank() }
            ?: return ProviderSyncResult.MissingCredentials
        val steamId = credentials.steamId64()?.takeIf { it.isNotBlank() }
            ?: return ProviderSyncResult.MissingCredentials

        rate.await()
        val schema = runCatching { webApi.getSchemaForGame(key, appId) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                return ProviderSyncResult.Failed("schema request failed")
            }
        if (schema.code() == 403) return ProviderSyncResult.MissingCredentials
        if (!schema.isSuccessful) return ProviderSyncResult.Failed("Steam returned ${schema.code()}")
        val schemaCoins = schema.body()?.game?.availableGameStats?.achievements.orEmpty()
        if (schemaCoins.isEmpty()) return ProviderSyncResult.NotFound

        rate.await()
        val percentByName = runCatching { webApi.getGlobalAchievementPercentages(appId) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                null
            }
            ?.body()?.achievementpercentages?.achievements
            ?.associate { it.name to it.percent }
            .orEmpty()

        rate.await()
        val player = runCatching { webApi.getPlayerAchievements(key, steamId, appId) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                return ProviderSyncResult.Failed("player request failed")
            }
        val stats = player.body()?.playerstats
        if (player.code() == 403 ||
            (stats?.success == false && stats.error?.contains("not public", ignoreCase = true) == true)
        ) {
            return ProviderSyncResult.ProfileNotPublic
        }
        if (!player.isSuccessful || stats?.success != true) {
            return ProviderSyncResult.Failed("Steam returned ${player.code()}")
        }

        val earnedByName = stats?.achievements?.associateBy { it.apiname }.orEmpty()
        val coins = SteamCoinMapper.map(schemaCoins, percentByName, earnedByName)
        return ProviderSyncResult.Success(appId, enrichHiddenDescriptions(appId, steamId, coins))
    }

    private suspend fun enrichHiddenDescriptions(
        appId: String,
        steamId: String,
        coins: List<com.echo.feature.achievements.api.SyncedCoin>,
    ): List<com.echo.feature.achievements.api.SyncedCoin> {
        if (coins.none { it.isHidden && it.isEarned && it.description.isBlank() }) return coins
        return runCatching {
            rate.await()
            val response = communityApi.achievementsPage(steamId, appId)

            val body = response.body()?.takeIf { it.contentLength() <= 4_000_000 }?.string()
                ?: return coins
            val descriptionByTitle = SteamCommunityAchievementsParser.parse(body)
            coins.map { coin ->
                if (!coin.isHidden || !coin.isEarned || coin.description.isNotBlank()) return@map coin
                val found = descriptionByTitle[SteamCommunityAchievementsParser.normalizeTitle(coin.title)]
                if (found != null) coin.copy(description = found) else coin
            }
        }.getOrElse { e ->
            if (e is CancellationException) throw e
            coins
        }
    }
}
