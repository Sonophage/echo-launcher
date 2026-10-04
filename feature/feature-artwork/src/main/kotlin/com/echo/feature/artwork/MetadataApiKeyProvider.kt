package com.echo.feature.artwork

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.common.security.KeystoreSecretCipher
import com.echo.core.common.security.SecretProtection
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val igdbClientIdFlow: Flow<String?> = context.echoDataStore.data.map { it[KEY_IGDB_CLIENT_ID] }

    suspend fun getIgdbClientId(): String? = igdbClientIdFlow.first()
    suspend fun getIgdbClientSecret(): String? =
        context.echoDataStore.data.first()[KEY_IGDB_CLIENT_SECRET]?.let { KeystoreSecretCipher.decryptOrLegacy(it) }

    suspend fun saveIgdbCredentials(clientId: String, clientSecret: String): SecretProtection {
        val sealed = KeystoreSecretCipher.seal(clientSecret.trim())
        context.echoDataStore.edit {
            it[KEY_IGDB_CLIENT_ID]     = clientId.trim()
            it[KEY_IGDB_CLIENT_SECRET] = sealed.stored
        }
        return SecretProtection.of(sealed)
    }

    suspend fun clearIgdbCredentials() {
        context.echoDataStore.edit {
            it.remove(KEY_IGDB_CLIENT_ID)
            it.remove(KEY_IGDB_CLIENT_SECRET)
        }
    }

    val hasIgdbCredentialsFlow: Flow<Boolean> = context.echoDataStore.data.map { prefs ->
        !prefs[KEY_IGDB_CLIENT_ID].isNullOrBlank() &&
            !prefs[KEY_IGDB_CLIENT_SECRET]?.let { KeystoreSecretCipher.decryptOrLegacy(it) }.isNullOrBlank()
    }

    suspend fun hasIgdbCredentials(): Boolean = hasIgdbCredentialsFlow.first()

    val ssUsernameFlow: Flow<String?> = context.echoDataStore.data.map { it[KEY_SS_USERNAME] }

    suspend fun getSsUsername(): String? = ssUsernameFlow.first()
    suspend fun getSsPassword(): String? =
        context.echoDataStore.data.first()[KEY_SS_PASSWORD]?.let { KeystoreSecretCipher.decryptOrLegacy(it) }

    suspend fun saveSsCredentials(username: String, password: String): SecretProtection {
        val sealed = KeystoreSecretCipher.seal(password.trim())
        context.echoDataStore.edit {
            it[KEY_SS_USERNAME] = username.trim()
            it[KEY_SS_PASSWORD] = sealed.stored
        }
        return SecretProtection.of(sealed)
    }

    suspend fun clearSsCredentials() {
        context.echoDataStore.edit {
            it.remove(KEY_SS_USERNAME)
            it.remove(KEY_SS_PASSWORD)
        }
    }

    val hasSsCredentialsFlow: Flow<Boolean> = context.echoDataStore.data.map { prefs ->
        !prefs[KEY_SS_USERNAME].isNullOrBlank() &&
            !prefs[KEY_SS_PASSWORD]?.let { KeystoreSecretCipher.decryptOrLegacy(it) }.isNullOrBlank()
    }

    suspend fun hasSsCredentials(): Boolean = hasSsCredentialsFlow.first()

    companion object {
        private val KEY_IGDB_CLIENT_ID     = stringPreferencesKey("igdb_client_id")
        private val KEY_IGDB_CLIENT_SECRET = stringPreferencesKey("igdb_client_secret")
        private val KEY_SS_USERNAME        = stringPreferencesKey("ss_username")
        private val KEY_SS_PASSWORD        = stringPreferencesKey("ss_password")
    }
}
