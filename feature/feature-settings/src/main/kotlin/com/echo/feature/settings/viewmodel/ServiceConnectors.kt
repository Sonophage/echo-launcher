package com.echo.feature.settings.viewmodel

import com.echo.core.common.security.SecretProtection
import com.echo.core.data.achievement.AchievementCredentialsProvider
import com.echo.feature.artwork.api.IgdbApi
import com.echo.feature.achievements.provider.steam.SteamRemoteDataSource
import com.echo.feature.artwork.api.ScreenScraperApi

internal object ServiceConnectors {
    private val STEAM_ID64 = Regex("\\d{17}")

    suspend fun connectSteam(
        credentials: AchievementCredentialsProvider,
        steamApi: SteamRemoteDataSource,
        idOrVanity: String,
        apiKey: String,
    ): String {
        val input = idOrVanity.trim()
        val key = apiKey.trim()
        if (input.matches(STEAM_ID64)) {
            return unprotectedWarning("Steam API key", credentials.saveSteam(input, key)) ?: "Steam connected"
        }
        val previous = credentials.steamId64()?.takeIf { it.matches(STEAM_ID64) }.orEmpty()
        credentials.saveSteam(previous, key)
        val resolved = steamApi.resolveVanity(input)
        return if (resolved != null) {
            val protection = credentials.saveSteam(resolved, key)
            unprotectedWarning("Steam API key", protection) ?: "Steam connected, resolved \"$input\""
        } else {
            "Key saved, but \"$input\" couldn't be resolved. Enter your SteamID64."
        }
    }

    fun unprotectedWarning(what: String, protection: SecretProtection): String? =
        if (protection == SecretProtection.PROTECTED) null
        else "$what was saved, but this device's secure keystore was unavailable, so it is " +
            "stored unencrypted. Clearing and re-entering it later will try again."

    suspend fun testIgdb(igdbApi: IgdbApi, clientId: String, clientSecret: String): String =
        if (igdbApi.testCredentials(clientId.trim(), clientSecret.trim())) "Valid"
        else "Invalid — check Client ID and Secret"

    suspend fun testScreenScraper(
        screenScraperApi: ScreenScraperApi,
        username: String,
        password: String,
    ): String {
        val user = screenScraperApi.fetchUserInfo(username.trim(), password.trim())
        return if (user != null) {
            buildString {
                append("Valid")
                user.maxThreads?.let { t -> append(" — $t thread${if (t == "1") "" else "s"}") }
                user.maxRequestsPerDay?.let { q -> append(", $q requests/day") }
            }
        } else {
            "Invalid — check username and password"
        }
    }
}
