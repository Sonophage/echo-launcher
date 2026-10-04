package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class GameBootPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val gameBootEnabledFlow: Flow<Boolean> = context.echoDataStore.data
        .map { resolve(it) }

    suspend fun setGameBootEnabled(enabled: Boolean) = context.echoDataStore.edit {
        it[KEY_GAMEBOOT_ENABLED] = enabled

        it.remove(KEY_GAMEBOOT_MODE)
    }

    companion object {
        private val KEY_GAMEBOOT_ENABLED = booleanPreferencesKey("display_gameboot_enabled")

        private val KEY_GAMEBOOT_MODE = stringPreferencesKey("display_gameboot_mode")

        private val KEY_INITIAL_SETUP_SEEN = InitialSetupFlag.KEY_SEEN

        private const val LEGACY_MODE_OFF = "OFF"

        fun resolve(prefs: Preferences): Boolean = when {
            prefs[KEY_GAMEBOOT_MODE] != null -> prefs[KEY_GAMEBOOT_MODE] != LEGACY_MODE_OFF
            prefs[KEY_GAMEBOOT_ENABLED] != null -> prefs[KEY_GAMEBOOT_ENABLED] == true
            prefs[KEY_INITIAL_SETUP_SEEN] == true -> false
            else -> true
        }
    }
}
