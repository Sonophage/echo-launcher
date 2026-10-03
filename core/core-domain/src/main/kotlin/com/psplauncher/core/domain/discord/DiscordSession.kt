package com.psplauncher.core.domain.discord

data class DiscordSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val scopes: String,
)
