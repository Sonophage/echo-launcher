package com.echo.feature.achievements.provider.steam

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface SteamCommunityApi {
    @Headers("User-Agent: Mozilla/5.0")
    @GET("profiles/{steamId64}/stats/{appId}/achievements")
    suspend fun achievementsPage(
        @Path("steamId64") steamId64: String,
        @Path("appId") appId: String,

        @Query("l") language: String = "english",
    ): Response<ResponseBody>
}
