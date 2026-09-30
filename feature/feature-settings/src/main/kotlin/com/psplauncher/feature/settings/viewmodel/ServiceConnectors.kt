package com.psplauncher.feature.settings.viewmodel

import com.psplauncher.core.common.security.SecretProtection
import com.psplauncher.feature.artwork.api.IgdbApi
import com.psplauncher.feature.artwork.api.ScreenScraperApi

internal object ServiceConnectors {
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
