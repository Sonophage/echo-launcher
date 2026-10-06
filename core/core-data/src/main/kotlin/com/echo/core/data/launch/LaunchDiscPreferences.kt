package com.echo.core.data.launch

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.repository.GameBootStyle
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LaunchDiscPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val launchDiscEnabledFlow: Flow<Boolean> = context.echoDataStore.data.map { resolve(it) }

    suspend fun setLaunchDiscEnabled(enabled: Boolean) = context.echoDataStore.edit {
        it[KEY_LAUNCH_DISC_ENABLED] = enabled
    }

    // owner, 2026-10-05: the launch disc offers the same choice of animation as GameBoot
    val styleFlow: Flow<GameBootStyle> = context.echoDataStore.data.map { styleOf(it) }

    suspend fun setStyle(style: GameBootStyle) = context.echoDataStore.edit { it[KEY_LAUNCH_DISC_STYLE] = style.name }

    companion object {
        private val KEY_LAUNCH_DISC_ENABLED = booleanPreferencesKey("display_launch_disc")
        private val KEY_LAUNCH_DISC_STYLE = stringPreferencesKey("display_launch_disc_style")

        // owner, 2026-10-06: ECHO's own Lens is the default; an earlier install keeps Disc (keepOldDefaults)
        fun styleOf(prefs: Preferences): GameBootStyle =
            GameBootStyle.entries.firstOrNull { it.name == prefs[KEY_LAUNCH_DISC_STYLE] } ?: GameBootStyle.LENS

        fun resolve(prefs: Preferences): Boolean = prefs[KEY_LAUNCH_DISC_ENABLED] ?: true
    }
}
