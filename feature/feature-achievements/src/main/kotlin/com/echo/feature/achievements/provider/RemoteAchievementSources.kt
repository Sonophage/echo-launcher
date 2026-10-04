package com.echo.feature.achievements.provider

import com.echo.core.domain.achievement.AchievementProvider
import com.echo.feature.achievements.provider.retro.RetroAchievementsSource
import com.echo.feature.achievements.provider.steam.SteamAchievementsSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAchievementSources @Inject constructor(
    private val retroAchievements: RetroAchievementsSource,
    private val steam: SteamAchievementsSource,
) {
    fun forProvider(provider: AchievementProvider): RemoteAchievementSource = when (provider) {
        AchievementProvider.RETRO_ACHIEVEMENTS -> retroAchievements
        AchievementProvider.STEAM -> steam
    }
}
