package com.echo.feature.achievements.di

import com.echo.feature.achievements.AchievementController
import com.echo.feature.achievements.AchievementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AchievementModule {
    @Binds
    fun bindAchievementController(impl: AchievementRepository): AchievementController
}
