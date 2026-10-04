package com.echo.core.domain.discord

object DiscordSanitize {
    const val NAME_MAX = 80
    const val ACTIVITY_MAX = 128

    fun text(raw: String, maxLen: Int): String {
        val stripped = buildString(raw.length) {
            for (c in raw) {
                if (c.isISOControl() || c.code in BIDI_OVERRIDES) continue
                append(c)
            }
        }
        return stripped.replace(WHITESPACE, " ").trim().take(maxLen)
    }

    fun avatarUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        if (!url.lowercase().startsWith("https://")) return null
        val host = url.substring("https://".length).substringBefore('/').substringBefore(':').lowercase()
        return if (host in ALLOWED_AVATAR_HOSTS) url else null
    }

    private val WHITESPACE = Regex("\\s+")

    private val BIDI_OVERRIDES = setOf(
        0x200E, 0x200F, 0x061C,
        0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
        0x2066, 0x2067, 0x2068, 0x2069,
    )

    private val ALLOWED_AVATAR_HOSTS = setOf("cdn.discordapp.com", "media.discordapp.net")
}
