package com.echo.core.data.repository

import com.echo.core.domain.model.withKitButtons
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.GamepadBinding
import com.echo.core.domain.model.GamepadMappings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_MAPPINGS = stringPreferencesKey("controller_mappings_v1")

@Singleton
class ControllerMappingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val mappings: Flow<GamepadMappings> = context.echoDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_MAPPINGS]
            if (raw != null) {
                runCatching { json.decodeFromString<GamepadMappings>(raw).withKitButtons() }
                    .getOrElse {
                        Timber.w("Failed to parse controller mappings, using defaults")
                        GamepadMappings()
                    }
            } else {
                GamepadMappings()
            }
        }

    suspend fun saveMappings(mappings: GamepadMappings) {
        context.echoDataStore.edit { prefs ->
            prefs[KEY_MAPPINGS] = json.encodeToString(mappings)
        }
        Timber.i("Controller mappings saved")
    }

    suspend fun resetToDefaults() {
        context.echoDataStore.edit { prefs ->
            prefs.remove(KEY_MAPPINGS)
        }
        Timber.i("Controller mappings reset to defaults")
    }

    suspend fun remap(action: GamepadAction, newKeyCode: Int) {
        val prefs = context.echoDataStore.data.first()
        val current = prefs[KEY_MAPPINGS]?.let {
            runCatching { json.decodeFromString<GamepadMappings>(it) }.getOrNull()
        }?.withKitButtons() ?: GamepadMappings()

        val updated = current.bindings
            .filter { it.keyCode != newKeyCode && it.action != action }
            .plus(GamepadBinding(newKeyCode, action))

        saveMappings(GamepadMappings(updated))
        Timber.i("Remapped $action → keycode $newKeyCode")
    }
}
