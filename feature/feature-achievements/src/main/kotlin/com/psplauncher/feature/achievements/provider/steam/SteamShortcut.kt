package com.psplauncher.feature.achievements.provider.steam

import com.psplauncher.core.domain.model.Game

object SteamShortcut {
    private val STEAM_URI = Regex("""steam://(?:rungameid|run)/(\d+)""", RegexOption.IGNORE_CASE)
    private val APP_ID = Regex("""app_id=(\d+)""")

    fun appIdFrom(game: Game): String? {
        val intent = game.launchIntentUri ?: return null

        STEAM_URI.find(intent)?.let { return it.groupValues[1] }

        if (intent.contains("game_source=STEAM", ignoreCase = true)) {
            APP_ID.find(intent)?.let { return it.groupValues[1] }
        }
        return null
    }
}
