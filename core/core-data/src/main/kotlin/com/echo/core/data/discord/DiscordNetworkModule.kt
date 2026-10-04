package com.echo.core.data.discord

import com.echo.core.domain.model.GameLaunchListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DiscordHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DiscordNetworkModule {

    @Provides
    @Singleton
    @DiscordHttpClient
    fun provideDiscordHttpClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }

    @Provides
    @IntoSet
    fun providePresenceLaunchListener(presence: DiscordPresenceController): GameLaunchListener = presence
}
