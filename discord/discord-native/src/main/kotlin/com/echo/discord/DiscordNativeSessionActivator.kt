package com.echo.discord

import com.echo.core.domain.discord.DiscordFriend
import com.echo.core.domain.discord.DiscordPresence
import com.echo.core.domain.discord.DiscordSanitize
import com.echo.core.domain.discord.DiscordSessionActivator
import com.echo.core.domain.discord.DiscordUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordNativeSessionActivator @Inject constructor() : DiscordSessionActivator {

    override suspend fun activate(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { DiscordNativeBridge.updateToken(accessToken) }
            .onFailure { Timber.w(it, "Discord SDK could not start") }
            .getOrDefault(false)
    }

    override suspend fun deactivate() {
        withContext(Dispatchers.IO) {
            DiscordNativeBridge.clearActivity()
            DiscordNativeBridge.disconnect()
        }
    }

    override suspend fun currentUser(): DiscordUser? = withContext(Dispatchers.IO) {
        val json = DiscordNativeBridge.currentUserJson()
        if (json.isBlank()) return@withContext null
        runCatching {
            val obj = JSONObject(json)
            DiscordUser(
                id = obj.optString("id"),
                username = DiscordSanitize.text(obj.optString("username"), DiscordSanitize.NAME_MAX),
                displayName = DiscordSanitize.text(obj.optString("displayName"), DiscordSanitize.NAME_MAX),
                avatarUrl = DiscordSanitize.avatarUrl(obj.optString("avatarUrl")).orEmpty(),
            )
        }.getOrNull()
    }

    override suspend fun friends(): List<DiscordFriend> = withContext(Dispatchers.IO) {
        val json = DiscordNativeBridge.friendsJson()
        runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                DiscordFriend(
                    id = obj.optString("id"),
                    username = DiscordSanitize.text(obj.optString("username"), DiscordSanitize.NAME_MAX),
                    displayName = DiscordSanitize.text(obj.optString("displayName"), DiscordSanitize.NAME_MAX),
                    avatarUrl = DiscordSanitize.avatarUrl(obj.optString("avatarUrl")).orEmpty(),
                    presence = DiscordPresence.fromStatusOrdinal(obj.optInt("status", 7)),
                    activity = DiscordFriend.composeActivity(
                        name = obj.optString("activityName"),
                        details = obj.optString("activityDetails"),
                        state = obj.optString("activityState"),
                    ),
                )
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun setActivity(name: String, details: String?) = withContext(Dispatchers.IO) {
        DiscordNativeBridge.setActivity(name, details.orEmpty())
    }

    override suspend fun clearActivity() = withContext(Dispatchers.IO) {
        DiscordNativeBridge.clearActivity()
    }
}
