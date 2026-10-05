package com.echo.core.data.network

import com.echo.core.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val SECRET_QUERY_PARAMS = Regex("(sspassword|devpassword|apikey)=[^&\\s]*", RegexOption.IGNORE_CASE)

internal fun redactSecretQueryParams(message: String): String =
    SECRET_QUERY_PARAMS.replace(message) { "${it.groupValues[1]}=REDACTED" }

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false

        install(HttpTimeout)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) = Timber.tag("Ktor").d(redactSecretQueryParams(message))
            }
            sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)

                readTimeout(15, TimeUnit.SECONDS)
            }
        }
    }
}
