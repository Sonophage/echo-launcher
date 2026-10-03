package com.psplauncher.core.data.achievement

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.psplauncher.core.common.security.KeystoreSecretCipher
import com.psplauncher.core.common.security.SecretProtection
import com.psplauncher.core.data.datastore.pfpDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_RA_USERNAME = stringPreferencesKey("ra_username")
private val KEY_RA_API_KEY = stringPreferencesKey("ra_api_key")
private val KEY_STEAM_ID64 = stringPreferencesKey("steam_id64")
private val KEY_STEAM_API_KEY = stringPreferencesKey("steam_api_key")
private val KEY_ENABLED = booleanPreferencesKey("achievements_enabled")
private val KEY_SYNC_LAST = longPreferencesKey("achievements_sync_last")

@Singleton
class AchievementCredentialsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val raUsernameFlow: Flow<String?> =
        context.pfpDataStore.data.map { it[KEY_RA_USERNAME] }

    val steamId64Flow: Flow<String?> =
        context.pfpDataStore.data.map { it[KEY_STEAM_ID64] }

    val enabledFlow: Flow<Boolean> =
        context.pfpDataStore.data.map { it[KEY_ENABLED] ?: false }

    val lastSyncedAtFlow: Flow<Long?> =
        context.pfpDataStore.data.map { it[KEY_SYNC_LAST] }

    suspend fun raUsername(): String? = context.pfpDataStore.data.first()[KEY_RA_USERNAME]

    suspend fun raApiKey(): String? =
        context.pfpDataStore.data.first()[KEY_RA_API_KEY]?.let(::reveal)

    suspend fun steamId64(): String? = context.pfpDataStore.data.first()[KEY_STEAM_ID64]

    suspend fun steamApiKey(): String? =
        context.pfpDataStore.data.first()[KEY_STEAM_API_KEY]?.let(::reveal)

    private fun reveal(stored: String): String? =
        stored.takeIf(KeystoreSecretCipher::isUsableOnThisDevice)?.let(KeystoreSecretCipher::decryptOrLegacy)

    suspend fun lastSyncedAt(): Long? = context.pfpDataStore.data.first()[KEY_SYNC_LAST]

    suspend fun hasRetroAchievements(): Boolean =
        !raUsername().isNullOrBlank() && !raApiKey().isNullOrBlank()

    suspend fun hasSteam(): Boolean =
        !steamId64().isNullOrBlank() && !steamApiKey().isNullOrBlank()

    suspend fun saveRetroAchievements(username: String, apiKey: String): SecretProtection {
        val sealed = KeystoreSecretCipher.seal(apiKey.trim())
        context.pfpDataStore.edit {
            it[KEY_RA_USERNAME] = username.trim()
            it[KEY_RA_API_KEY] = sealed.stored
        }
        return SecretProtection.of(sealed)
    }

    suspend fun saveSteam(steamId64: String, apiKey: String): SecretProtection {
        val sealed = KeystoreSecretCipher.seal(apiKey.trim())
        context.pfpDataStore.edit {
            it[KEY_STEAM_ID64] = steamId64.trim()
            it[KEY_STEAM_API_KEY] = sealed.stored
        }
        return SecretProtection.of(sealed)
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.pfpDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setLastSyncedAt(epochMillis: Long) {
        context.pfpDataStore.edit { it[KEY_SYNC_LAST] = epochMillis }
    }

    suspend fun clearRetroAchievements() {
        context.pfpDataStore.edit {
            it.remove(KEY_RA_USERNAME)
            it.remove(KEY_RA_API_KEY)
        }
    }

    suspend fun clearSteam() {
        context.pfpDataStore.edit {
            it.remove(KEY_STEAM_ID64)
            it.remove(KEY_STEAM_API_KEY)
        }
    }
}
