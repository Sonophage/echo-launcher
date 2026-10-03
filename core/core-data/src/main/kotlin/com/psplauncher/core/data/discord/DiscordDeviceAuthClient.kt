package com.psplauncher.core.data.discord

import com.psplauncher.core.domain.discord.DeviceAuthChallenge
import com.psplauncher.core.domain.discord.DeviceTokens
import com.psplauncher.core.domain.discord.DiscordConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordDeviceAuthClient @Inject constructor(
    @DiscordHttpClient private val http: HttpClient,
) {
    suspend fun requestDeviceCode(scopes: String = DiscordConfig.DEFAULT_SCOPES): DeviceAuthChallenge {
        val response = http.submitForm(
            url = DiscordConfig.DEVICE_AUTHORIZATION_ENDPOINT,
            formParameters = parameters {
                append("client_id", DiscordConfig.APPLICATION_ID)
                append("scope", scopes)
            },
        )
        require(response.status.isSuccess()) {
            "Device-code request failed (${response.status.value})"
        }
        val dto = response.body<DeviceCodeResponse>()
        val complete = dto.verificationUriComplete
            ?: "${dto.verificationUri}?user_code=${dto.userCode}"
        return DeviceAuthChallenge(
            userCode = dto.userCode,
            verificationUri = dto.verificationUri,
            verificationUriComplete = complete,
            deviceCode = dto.deviceCode,
            expiresInSeconds = dto.expiresIn,
            pollIntervalSeconds = dto.interval,
        )
    }

    suspend fun pollForToken(deviceCode: String): TokenPollResult {
        val response = http.submitForm(
            url = DiscordConfig.TOKEN_ENDPOINT,
            formParameters = parameters {
                append("client_id", DiscordConfig.APPLICATION_ID)
                append("device_code", deviceCode)
                append("grant_type", DiscordConfig.DEVICE_CODE_GRANT_TYPE)
            },
        )
        val dto = runCatching { response.body<TokenResponse>() }.getOrNull()
            ?: return TokenPollResult.Error("Malformed token response (${response.status.value})")

        return when {
            response.status.isSuccess() && dto.accessToken != null && dto.refreshToken != null ->
                TokenPollResult.Approved(
                    DeviceTokens(
                        accessToken = dto.accessToken,
                        refreshToken = dto.refreshToken,
                        expiresInSeconds = dto.expiresIn ?: 0,
                        scopes = dto.scope.orEmpty(),
                    ),
                )
            dto.error == "authorization_pending" -> TokenPollResult.Pending
            dto.error == "slow_down"             -> TokenPollResult.SlowDown
            dto.error == "expired_token"         -> TokenPollResult.Expired
            dto.error == "access_denied"         -> TokenPollResult.Denied
            else -> TokenPollResult.Error(dto.error ?: "unknown_error (${response.status.value})")
        }
    }

    suspend fun refreshTokens(refreshToken: String): TokenPollResult {
        val response = http.submitForm(
            url = DiscordConfig.TOKEN_ENDPOINT,
            formParameters = parameters {
                append("client_id", DiscordConfig.APPLICATION_ID)
                append("grant_type", DiscordConfig.REFRESH_TOKEN_GRANT_TYPE)
                append("refresh_token", refreshToken)
            },
        )
        val dto = runCatching { response.body<TokenResponse>() }.getOrNull()
            ?: return TokenPollResult.Error("Malformed refresh response (${response.status.value})")

        return if (response.status.isSuccess() && dto.accessToken != null) {
            TokenPollResult.Approved(
                DeviceTokens(
                    accessToken = dto.accessToken,
                    refreshToken = dto.refreshToken ?: refreshToken,
                    expiresInSeconds = dto.expiresIn ?: 0,
                    scopes = dto.scope.orEmpty(),
                ),
            )
        } else {
            TokenPollResult.Error(dto.error ?: "refresh_failed (${response.status.value})")
        }
    }
}

sealed interface TokenPollResult {
    data object Pending : TokenPollResult

    data object SlowDown : TokenPollResult

    data class Approved(val tokens: DeviceTokens) : TokenPollResult

    data object Expired : TokenPollResult

    data object Denied : TokenPollResult

    data class Error(val message: String) : TokenPollResult
}

@Serializable
private data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int = 5,
)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("error") val error: String? = null,
)
