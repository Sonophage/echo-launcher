package com.echo.feature.artwork.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.common.security.KeystoreSecretCipher
import com.echo.core.common.security.SecretProtection
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")

@Singleton
class TmdbApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val keyFlow = context.echoDataStore.data
        .map { prefs -> prefs[KEY_TMDB_API_KEY]?.let { KeystoreSecretCipher.decryptOrLegacy(it) } }

    suspend fun getKey(): String? =
        context.echoDataStore.data.first()[KEY_TMDB_API_KEY]?.let { KeystoreSecretCipher.decryptOrLegacy(it) }

    suspend fun saveKey(key: String): SecretProtection {
        val sealed = KeystoreSecretCipher.seal(key.trim())
        context.echoDataStore.edit { it[KEY_TMDB_API_KEY] = sealed.stored }
        return SecretProtection.of(sealed)
    }

    suspend fun clearKey() {
        context.echoDataStore.edit { it.remove(KEY_TMDB_API_KEY) }
    }

    suspend fun hasKey(): Boolean = !getKey().isNullOrBlank()
}
