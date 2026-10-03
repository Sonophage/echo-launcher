package com.psplauncher.core.domain.discord

data class DiscordUser(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
) {
    val label: String get() = displayName.ifBlank { username }
}
