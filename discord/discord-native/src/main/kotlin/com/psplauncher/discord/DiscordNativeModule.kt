package com.psplauncher.discord

import com.psplauncher.core.domain.discord.DiscordSessionActivator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscordNativeModule {
    @Binds
    abstract fun bindSessionActivator(impl: DiscordNativeSessionActivator): DiscordSessionActivator
}
