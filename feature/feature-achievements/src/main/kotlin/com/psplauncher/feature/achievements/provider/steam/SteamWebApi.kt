package com.psplauncher.feature.achievements.provider.steam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamWebApi {
    @GET("ISteamUserStats/GetSchemaForGame/v2/")
    suspend fun getSchemaForGame(
        @Query("key") key: String,
        @Query("appid") appId: String,
    ): Response<SteamSchemaResponse>

    @GET("ISteamUserStats/GetGlobalAchievementPercentagesForApp/v2/")
    suspend fun getGlobalAchievementPercentages(
        @Query("gameid") gameId: String,
    ): Response<SteamGlobalResponse>

    @GET("ISteamUserStats/GetPlayerAchievements/v1/")
    suspend fun getPlayerAchievements(
        @Query("key") key: String,
        @Query("steamid") steamId: String,
        @Query("appid") appId: String,
    ): Response<SteamPlayerResponse>

    @GET("ISteamUser/ResolveVanityURL/v1/")
    suspend fun resolveVanityUrl(
        @Query("key") key: String,
        @Query("vanityurl") vanity: String,
    ): Response<SteamVanityResponse>

    @GET("IPlayerService/GetOwnedGames/v1/")
    suspend fun getOwnedGames(
        @Query("key") key: String,
        @Query("steamid") steamId: String,
        @Query("include_appinfo") includeAppInfo: Int = 1,
        @Query("include_played_free_games") includePlayedFreeGames: Int = 1,
    ): Response<SteamOwnedGamesResponse>
}

@Serializable
data class SteamSchemaResponse(val game: SteamGame? = null)

@Serializable
data class SteamGame(
    @SerialName("availableGameStats") val availableGameStats: SteamGameStats? = null,
)

@Serializable
data class SteamGameStats(
    val achievements: List<SteamSchemaAchievement> = emptyList(),
)

@Serializable
data class SteamSchemaAchievement(
    val name: String,
    @SerialName("displayName") val displayName: String? = null,
    val description: String? = null,
    val hidden: Int = 0,
    val icon: String? = null,
    val icongray: String? = null,
)

@Serializable
data class SteamGlobalResponse(val achievementpercentages: SteamGlobalWrap? = null)

@Serializable
data class SteamGlobalWrap(val achievements: List<SteamGlobalPct> = emptyList())

@Serializable
data class SteamGlobalPct(val name: String, val percent: Double = 0.0)

@Serializable
data class SteamPlayerResponse(val playerstats: SteamPlayerStats? = null)

@Serializable
data class SteamPlayerStats(
    val success: Boolean = false,
    val error: String? = null,
    val achievements: List<SteamPlayerAchievement> = emptyList(),
)

@Serializable
data class SteamPlayerAchievement(
    val apiname: String,
    val achieved: Int = 0,
    val unlocktime: Long = 0,
)

@Serializable
data class SteamVanityResponse(val response: SteamVanityInner? = null)

@Serializable
data class SteamVanityInner(val steamid: String? = null, val success: Int = 0)

@Serializable
data class SteamOwnedGamesResponse(val response: SteamOwnedGamesInner? = null)

@Serializable
data class SteamOwnedGamesInner(
    @SerialName("game_count") val gameCount: Int = 0,
    val games: List<SteamOwnedGame> = emptyList(),
)

@Serializable
data class SteamOwnedGame(
    val appid: Long,
    val name: String? = null,
    @SerialName("playtime_forever") val playtimeForever: Long = 0,
)
