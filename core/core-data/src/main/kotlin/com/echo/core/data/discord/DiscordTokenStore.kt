package com.echo.core.data.discord

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.echo.core.common.security.KeystoreAesGcm
import com.echo.core.domain.discord.DeviceTokens
import com.echo.core.domain.discord.DiscordSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.discordSecureStore: DataStore<Preferences> by
    preferencesDataStore(name = "discord_secure")

@Singleton
class DiscordTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val blobKey = stringPreferencesKey("session_blob")
    private val aesGcm = KeystoreAesGcm("pfp_discord_token_key")

    val hasSession: Flow<Boolean>
        get() = context.discordSecureStore.data.map { it[blobKey] != null }.distinctUntilChanged()

    suspend fun save(tokens: DeviceTokens, nowMs: Long = System.currentTimeMillis()) {
        val session = DiscordSession(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresAtEpochMs = nowMs + tokens.expiresInSeconds * 1000L,
            scopes = tokens.scopes,
        )
        val sealed = aesGcm.seal(json.encodeToString(SessionBlob.serializer(), session.toBlob()))
        context.discordSecureStore.edit { it[blobKey] = sealed }
    }

    suspend fun load(): DiscordSession? {
        val sealed = context.discordSecureStore.data.first()[blobKey] ?: return null
        return runCatching {
            json.decodeFromString(SessionBlob.serializer(), aesGcm.open(sealed)).toSession()
        }.getOrNull()
    }

    suspend fun clear() {
        context.discordSecureStore.edit { it.remove(blobKey) }
        aesGcm.deleteKey()
    }
}

@Serializable
private data class SessionBlob(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val scopes: String,
)

private fun DiscordSession.toBlob() = SessionBlob(accessToken, refreshToken, expiresAtEpochMs, scopes)
private fun SessionBlob.toSession() = DiscordSession(accessToken, refreshToken, expiresAtEpochMs, scopes)
