package com.psplauncher.core.domain.discord

interface DiscordSessionActivator {
    suspend fun activate(accessToken: String): Boolean

    suspend fun deactivate()

    suspend fun currentUser(): DiscordUser?

    suspend fun friends(): List<DiscordFriend>

    suspend fun setActivity(name: String, details: String?)

    suspend fun clearActivity()
}
