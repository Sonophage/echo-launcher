package com.psplauncher.core.domain.discord

data class DeviceAuthChallenge(
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String,
    val deviceCode: String,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int,
)

data class DeviceTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
    val scopes: String,
)

sealed interface DeviceLoginState {
    data object Requesting : DeviceLoginState

    data class AwaitingApproval(val challenge: DeviceAuthChallenge) : DeviceLoginState

    data class Success(val tokens: DeviceTokens) : DeviceLoginState

    data object Expired : DeviceLoginState

    data object Denied : DeviceLoginState

    data class Error(val message: String) : DeviceLoginState
}
