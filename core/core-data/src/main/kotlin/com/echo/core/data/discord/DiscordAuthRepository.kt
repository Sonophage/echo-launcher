package com.echo.core.data.discord

import com.echo.core.data.network.NetworkMonitor
import com.echo.core.domain.discord.DeviceLoginState
import com.echo.core.domain.discord.DiscordConfig
import com.echo.core.domain.discord.DiscordSessionActivator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordAuthRepository @Inject constructor(
    private val deviceAuth: DiscordDeviceAuthClient,
    private val tokenStore: DiscordTokenStore,
    private val sessionActivator: DiscordSessionActivator,
    private val networkMonitor: NetworkMonitor,
) {
    private val renewLock = Mutex()

    fun loginWithDeviceQr(scopes: String = DiscordConfig.DEFAULT_SCOPES): Flow<DeviceLoginState> = flow {
        emit(DeviceLoginState.Requesting)

        if (!networkMonitor.isOnline()) {
            emit(DeviceLoginState.Error("You're offline. Reconnect to the internet and try again."))
            return@flow
        }

        val challenge = try {
            deviceAuth.requestDeviceCode(scopes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(DeviceLoginState.Error(e.message ?: "Could not reach Discord"))
            return@flow
        }
        emit(DeviceLoginState.AwaitingApproval(challenge))

        var intervalSec = challenge.pollIntervalSeconds.coerceAtLeast(1)
        var remainingSec = challenge.expiresInSeconds

        while (remainingSec > 0) {
            delay(intervalSec * 1000L)
            remainingSec -= intervalSec

            val result = try {
                deviceAuth.pollForToken(challenge.deviceCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(DeviceLoginState.Error(e.message ?: "Network error while signing in"))
                return@flow
            }

            when (result) {
                TokenPollResult.Pending -> Unit
                TokenPollResult.SlowDown -> intervalSec += 5
                TokenPollResult.Expired -> { emit(DeviceLoginState.Expired); return@flow }
                TokenPollResult.Denied -> { emit(DeviceLoginState.Denied); return@flow }
                is TokenPollResult.Error -> { emit(DeviceLoginState.Error(result.message)); return@flow }
                is TokenPollResult.Approved -> {
                    tokenStore.save(result.tokens)
                    sessionActivator.activate(result.tokens.accessToken)
                    emit(DeviceLoginState.Success(result.tokens))
                    return@flow
                }
            }
        }
        emit(DeviceLoginState.Expired)
    }

    suspend fun hasSession(): Boolean = tokenStore.load() != null

    suspend fun restoreSession(): Boolean = renew(activateLive = true)

    suspend fun refreshIfExpiring() {
        renew(activateLive = false)
    }

    private suspend fun renew(activateLive: Boolean): Boolean = renewLock.withLock {
        val session = tokenStore.load() ?: return false
        if (session.expiresAtEpochMs - REFRESH_MARGIN_MS > System.currentTimeMillis()) {
            return if (activateLive) sessionActivator.activate(session.accessToken) else true
        }
        if (!networkMonitor.isOnline()) return false
        when (val refreshed = runCatching { deviceAuth.refreshTokens(session.refreshToken) }.getOrNull()) {
            is TokenPollResult.Approved -> {
                tokenStore.save(refreshed.tokens)
                sessionActivator.activate(refreshed.tokens.accessToken)
            }
            is TokenPollResult.Error -> {
                if (refreshed.message == REVOKED_GRANT) logout()
                false
            }
            else -> false
        }
    }

    suspend fun logout() {
        runCatching { sessionActivator.deactivate() }
        tokenStore.clear()
    }

    private companion object {
        const val REFRESH_MARGIN_MS = 5 * 60_000L
        const val REVOKED_GRANT = "invalid_grant"
    }
}
