package com.psplauncher.feature.achievements.provider.retro

import com.psplauncher.core.data.achievement.AchievementCredentialsProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.retroachivements.api.RetroClient
import org.retroachivements.api.RetroInterface
import org.retroachivements.api.data.RetroCredentials
import javax.inject.Inject
import javax.inject.Singleton

data class RaSession(val api: RetroInterface, val username: String)

@Singleton
class RaClientFactory @Inject constructor(
    private val credentials: AchievementCredentialsProvider,
) {
    private val mutex = Mutex()
    private var cachedKey: Pair<String, String>? = null
    private var cachedApi: RetroInterface? = null

    suspend fun session(): RaSession? {
        val user = credentials.raUsername()?.takeIf { it.isNotBlank() } ?: return null
        val key = credentials.raApiKey()?.takeIf { it.isNotBlank() } ?: return null
        val credKey = user to key
        val api = mutex.withLock {
            if (cachedKey != credKey || cachedApi == null) {
                cachedApi = buildApi(user, key)
                cachedKey = credKey
            }
            cachedApi!!
        }
        return RaSession(api, user)
    }

    private fun buildApi(user: String, key: String): RetroInterface {
        val client = RetroClient(RetroCredentials(user, key), debugging = false)
        val patched = client.httpClient.newBuilder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        return client.retroClient.newBuilder().client(patched).build()
            .create(RetroInterface::class.java)
    }
}
