package com.echo.feature.achievements.provider.steam

import com.echo.feature.achievements.api.RateLimiter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAppListResolver @Inject constructor(
    private val storeApi: SteamStoreApi,
) {
    private val rate = RateLimiter(1_500)

    suspend fun resolveAppId(title: String): String? {
        val key = normalize(title)
        if (key.isEmpty()) return null
        return storeSearch(title)
            .firstOrNull { it.type == "app" && normalize(it.name) == key }
            ?.id?.toString()
    }

    private suspend fun storeSearch(term: String): List<StoreItem> {
        rate.await()
        val response = storeApi.search(term)
        if (!response.isSuccessful) throw IOException("Steam store search returned ${response.code()}")
        return response.body()?.items.orEmpty()
    }

    private fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }
}
