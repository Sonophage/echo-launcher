package com.psplauncher.feature.achievements.provider.retro

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RaHashLookup {
    data class Found(val gameId: String) : RaHashLookup

    data object NotRegistered : RaHashLookup

    data object Unavailable : RaHashLookup
}

@Singleton
class RaHashResolver @Inject constructor(
    private val remote: RaRemoteDataSource,
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<Int, Map<String, String>>()

    suspend fun lookup(consoleId: Int, hash: String): RaHashLookup {
        val map = mutex.withLock {
            cache[consoleId] ?: remote.hashMap(consoleId)?.also { cache[consoleId] = it }
        } ?: return RaHashLookup.Unavailable
        return map[hash.lowercase()]?.let { RaHashLookup.Found(it) } ?: RaHashLookup.NotRegistered
    }
}
