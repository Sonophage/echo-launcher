package com.psplauncher.feature.achievements.provider.steam

import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamAppListResolver @Inject constructor(
    private val storeApi: SteamStoreApi,
) {
    suspend fun resolveAppId(title: String): String? {
        val key = normalize(title)
        if (key.isEmpty()) return null
        return storeSearch(title)
            .firstOrNull { it.type == "app" && normalize(it.name) == key }
            ?.id?.toString()
    }

    private suspend fun storeSearch(term: String): List<StoreItem> =
        runCatching { storeApi.search(term) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                return emptyList()
            }
            .body()?.items.orEmpty()

    private fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }
}
