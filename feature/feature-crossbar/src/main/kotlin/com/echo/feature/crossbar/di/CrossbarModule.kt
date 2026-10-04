package com.echo.feature.crossbar.di

import com.echo.core.data.repository.ControllerRegistry
import com.echo.core.data.repository.RemapCoordinator
import com.echo.feature.crossbar.gamepad.GamepadInputHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CrossbarModule {

    @Provides
    @Singleton
    fun provideGamepadInputHandler(
        remapCoordinator: RemapCoordinator,
        registry: ControllerRegistry,
    ): GamepadInputHandler = GamepadInputHandler(remapCoordinator, registry)
}
