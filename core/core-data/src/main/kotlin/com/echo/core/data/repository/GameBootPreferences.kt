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

// owner, 2026-10-05: the built-in game launch animation; Lens is the second option beside the disc
enum class GameBootStyle(val label: String) { DISC("Disc"), LENS("Lens") }

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

    val styleFlow: Flow<GameBootStyle> = context.echoDataStore.data.map { styleOf(it) }

    suspend fun setStyle(style: GameBootStyle) = context.echoDataStore.edit { it[KEY_GAMEBOOT_STYLE] = style.name }

    companion object {
        private val KEY_GAMEBOOT_STYLE = stringPreferencesKey("display_gameboot_style")

        // owner, 2026-10-06: ECHO's own Lens is the default; an earlier install keeps Disc (keepOldDefaults)
        fun styleOf(prefs: Preferences): GameBootStyle =
            GameBootStyle.entries.firstOrNull { it.name == prefs[KEY_GAMEBOOT_STYLE] } ?: GameBootStyle.LENS

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
