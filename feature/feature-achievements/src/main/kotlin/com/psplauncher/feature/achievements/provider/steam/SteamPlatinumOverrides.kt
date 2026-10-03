package com.psplauncher.feature.achievements.provider.steam

internal object SteamPlatinumOverrides {
    private val byAppId: Map<String, String> = emptyMap()

    fun completionApiName(appId: String): String? = byAppId[appId]
}
