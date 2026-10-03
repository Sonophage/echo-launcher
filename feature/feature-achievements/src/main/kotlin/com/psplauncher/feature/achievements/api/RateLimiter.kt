package com.psplauncher.feature.achievements.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(private val minIntervalMs: Long) {
    private val mutex = Mutex()
    private var lastAt = 0L

    suspend fun await() = mutex.withLock {
        val wait = minIntervalMs - (System.currentTimeMillis() - lastAt)
        if (wait > 0) delay(wait)
        lastAt = System.currentTimeMillis()
    }
}
