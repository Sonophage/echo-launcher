package com.psplauncher.feature.achievements.provider.retro

import com.haroldadmin.cnradapter.NetworkResponse
import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.api.RateLimiter
import org.retroachivements.api.data.pojo.user.GetUserCompletionProgress
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class RaProgressEntry(
    val gameId: String,
    val title: String,
    val iconUrl: String?,
    val earned: Long,
    val totalAchievements: Long,
)

sealed interface RaProgressResult {
    data class Success(val entries: List<RaProgressEntry>) : RaProgressResult
    data object MissingCredentials : RaProgressResult
    data class Failed(val reason: String) : RaProgressResult
}

@Singleton
class RaRemoteDataSource @Inject constructor(
    private val clientFactory: RaClientFactory,
) {
    private val rate = RateLimiter(1_100)

    suspend fun fetch(gameId: String): ProviderSyncResult {
        val session = clientFactory.session() ?: return ProviderSyncResult.MissingCredentials
        val id = gameId.toLongOrNull() ?: return ProviderSyncResult.Failed("invalid RetroAchievements game id")

        rate.await()

        val resp = runCatching { session.api.getGameInfoAndUserProgress(session.username, id) }
            .getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e

                Timber.i(e, "RA game sync threw for %s", gameId)
                return ProviderSyncResult.Failed("network error")
            }

        return when (resp) {
            is NetworkResponse.Success -> RaCoinMapper.map(resp.body, gameId)
            is NetworkResponse.ServerError -> when (resp.code) {
                401, 403 -> ProviderSyncResult.MissingCredentials
                else -> {
                    Timber.i("RA game sync failed for %s: HTTP %s", gameId, resp.code ?: "?")
                    ProviderSyncResult.Failed("RetroAchievements returned ${resp.code ?: "an error"}")
                }
            }
            is NetworkResponse.NetworkError -> {
                Timber.i("RA game sync failed for %s: %s", gameId, resp.error.toString())
                ProviderSyncResult.Failed("network error")
            }
            is NetworkResponse.UnknownError -> {
                Timber.i(resp.error, "RA game sync failed for %s", gameId)
                ProviderSyncResult.Failed("unexpected error")
            }
        }
    }

    suspend fun userCompletionProgress(): RaProgressResult {
        val session = clientFactory.session() ?: return RaProgressResult.MissingCredentials
        val entries = mutableListOf<RaProgressEntry>()
        var offset = 0
        while (true) {
            rate.await()
            val resp = runCatching {
                session.api.getUserCompletionProgress(session.username, PROGRESS_PAGE_SIZE, offset)
            }.getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                return RaProgressResult.Failed("network error")
            }

            when (resp) {
                is NetworkResponse.Success -> {
                    val page = resp.body.results
                    page.mapTo(entries) { it.toEntry() }
                    offset += page.size
                    if (page.isEmpty() || offset >= resp.body.total) return RaProgressResult.Success(entries)
                }
                is NetworkResponse.ServerError -> return when (resp.code) {
                    401, 403 -> RaProgressResult.MissingCredentials
                    else -> RaProgressResult.Failed("RetroAchievements returned ${resp.code ?: "an error"}")
                }
                is NetworkResponse.NetworkError -> return RaProgressResult.Failed("network error")
                is NetworkResponse.UnknownError -> return RaProgressResult.Failed("unexpected error")
            }
        }
    }

    suspend fun hashMap(consoleId: Int): Map<String, String>? {
        val session = clientFactory.session() ?: run {
            Timber.i("RA hash list: no credentials — console %d skipped", consoleId)
            return null
        }
        rate.await()
        val resp = runCatching {
            session.api.getGameList(
                consoleId = consoleId.toLong(),
                shouldOnlyRetrieveGamesWithAchievements = 1,
                shouldRetrieveGameHashes = 1,
            )
        }.getOrElse { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e

            Timber.i("RA hash list fetch threw for console %d: %s", consoleId, e.toString())
            return null
        }

        return when (resp) {
            is NetworkResponse.Success ->
                resp.body.flatMap { game -> game.hashes.map { it.lowercase() to game.id.toString() } }.toMap()
            is NetworkResponse.ServerError -> {
                Timber.i("RA hash list fetch failed for console %d: HTTP %s", consoleId, resp.code ?: "?")
                null
            }
            is NetworkResponse.NetworkError -> {
                Timber.i("RA hash list fetch failed for console %d: %s", consoleId, resp.error.javaClass.simpleName)
                null
            }
            is NetworkResponse.UnknownError -> {
                Timber.i(resp.error, "RA hash list fetch failed for console %d", consoleId)
                null
            }
        }
    }
}

private const val PROGRESS_PAGE_SIZE = 500

private const val MEDIA_BASE = "https://media.retroachievements.org"

private fun GetUserCompletionProgress.Progress.toEntry() = RaProgressEntry(
    gameId = gameId.toString(),
    title = title,
    iconUrl = imageIcon.takeIf { it.isNotBlank() }?.let { "$MEDIA_BASE$it" },
    earned = numAwarded,
    totalAchievements = maxPossible,
)
