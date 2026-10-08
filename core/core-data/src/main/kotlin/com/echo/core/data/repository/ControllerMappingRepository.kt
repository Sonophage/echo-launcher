package com.echo.core.data.repository

import com.echo.core.domain.model.withKitButtons
import com.echo.core.domain.model.withStartSelectSwapped
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.GamepadMappings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_MAPPINGS = stringPreferencesKey("controller_mappings_v1")

// set once a mapping has been saved after Start and Select traded roles (2026-10-07); until then a saved mapping
// is read with the trade applied
private val KEY_START_SELECT_SWAPPED = androidx.datastore.preferences.core.booleanPreferencesKey("controller_start_select_swapped")

@Singleton
class ControllerMappingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val mappings: Flow<GamepadMappings> = context.echoDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_MAPPINGS]
            if (raw != null) {
                runCatching {
                    json.decodeFromString<GamepadMappings>(raw).withKitButtons()
                        .let { if (prefs[KEY_START_SELECT_SWAPPED] == true) it else it.withStartSelectSwapped() }
                }
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
            prefs[KEY_START_SELECT_SWAPPED] = true
        }
        Timber.i("Controller mappings saved")
    }

    suspend fun resetToDefaults() {
        context.echoDataStore.edit { prefs ->
            prefs.remove(KEY_MAPPINGS)
            prefs[KEY_START_SELECT_SWAPPED] = true
        }
        Timber.i("Controller mappings reset to defaults")
    }
}
