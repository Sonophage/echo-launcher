package com.psplauncher.core.data.discord

import com.psplauncher.core.data.network.NetworkMonitor
import com.psplauncher.core.domain.discord.DeviceLoginState
import com.psplauncher.core.domain.discord.DiscordConfig
import com.psplauncher.core.domain.discord.DiscordSessionActivator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordAuthRepository @Inject constructor(
    private val deviceAuth: DiscordDeviceAuthClient,
    private val tokenStore: DiscordTokenStore,
    private val sessionActivator: DiscordSessionActivator,
    private val networkMonitor: NetworkMonitor,
) {
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

    suspend fun restoreSession(): Boolean {
        val session = tokenStore.load() ?: return false
        if (session.expiresAtEpochMs > System.currentTimeMillis()) {
            return sessionActivator.activate(session.accessToken)
        }
        if (!networkMonitor.isOnline()) return false
        val refreshed = runCatching { deviceAuth.refreshTokens(session.refreshToken) }.getOrNull()
        val tokens = (refreshed as? TokenPollResult.Approved)?.tokens ?: return false
        tokenStore.save(tokens)
        return sessionActivator.activate(tokens.accessToken)
    }

    suspend fun logout() {
        runCatching { sessionActivator.deactivate() }
        tokenStore.clear()
    }
}
