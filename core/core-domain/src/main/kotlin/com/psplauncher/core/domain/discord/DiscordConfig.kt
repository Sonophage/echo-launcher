package com.psplauncher.core.domain.discord

object DiscordConfig {
    const val APPLICATION_ID: String = "1522836772847878216"

    const val DEVICE_AUTHORIZATION_ENDPOINT: String = "https://discord.com/api/v10/oauth2/device/authorize"
    const val TOKEN_ENDPOINT: String = "https://discord.com/api/v10/oauth2/token"

    const val API_HOST: String = "discord.com"

    const val DEFAULT_SCOPES: String = "openid sdk.social_layer"

    const val DEVICE_CODE_GRANT_TYPE: String = "urn:ietf:params:oauth:grant-type:device_code"

    const val REFRESH_TOKEN_GRANT_TYPE: String = "refresh_token"

    const val STATUS_READY: Int = 3
}
