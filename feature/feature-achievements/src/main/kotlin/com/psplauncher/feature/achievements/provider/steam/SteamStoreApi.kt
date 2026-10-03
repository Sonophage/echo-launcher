package com.psplauncher.feature.achievements.provider.steam

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface SteamStoreApi {
    @Headers("User-Agent: Mozilla/5.0")
    @GET("api/storesearch/")
    suspend fun search(
        @Query("term") term: String,
        @Query("cc") countryCode: String = "us",
        @Query("l") language: String = "en",
    ): Response<StoreSearchResponse>
}

@Serializable
data class StoreSearchResponse(val items: List<StoreItem> = emptyList())

@Serializable
data class StoreItem(val id: Long = 0, val name: String = "", val type: String = "")
