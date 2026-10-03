package com.psplauncher.feature.achievements.di

import com.psplauncher.feature.achievements.AchievementController
import com.psplauncher.feature.achievements.AchievementRepository
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
