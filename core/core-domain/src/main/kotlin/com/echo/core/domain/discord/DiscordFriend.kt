package com.echo.core.domain.discord

data class DiscordFriend(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val presence: DiscordPresence,
    val activity: String? = null,
) {
    val label: String get() = displayName.ifBlank { username }

    companion object {
        fun composeActivity(name: String, details: String, state: String): String? {
            val raw = when {
                details.isNotBlank() && state.isNotBlank() -> "$details · $state"
                details.isNotBlank() -> details
                else -> name
            }
            return DiscordSanitize.text(raw, DiscordSanitize.ACTIVITY_MAX).ifBlank { null }
        }
    }
}

enum class DiscordPresence {
    ONLINE, IDLE, DND, STREAMING, OFFLINE, UNKNOWN;

    val isOnline: Boolean get() = this == ONLINE || this == IDLE || this == DND || this == STREAMING

    companion object {
        fun fromStatusOrdinal(ordinal: Int): DiscordPresence = when (ordinal) {
            0 -> ONLINE
            3 -> IDLE
            4 -> DND
            6 -> STREAMING
            1, 5 -> OFFLINE
            else -> UNKNOWN
        }
    }
}
