package com.echo.core.domain.achievement

enum class AchievementProvider {
    RETRO_ACHIEVEMENTS,
    STEAM;

    companion object {
        fun fromName(name: String?): AchievementProvider? =
            entries.firstOrNull { it.name == name }
    }
}
