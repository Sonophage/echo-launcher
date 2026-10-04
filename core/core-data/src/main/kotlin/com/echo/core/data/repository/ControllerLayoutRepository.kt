package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.ConfirmBackLayout
import com.echo.core.domain.model.ControllerDisplayType
import com.echo.core.domain.model.ControllerLayoutPrefs
import com.echo.core.domain.model.gamepadMappingsFor
import com.echo.core.domain.model.ScrollSpeed
import com.echo.core.domain.model.XYLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_CONFIRM_BACK = stringPreferencesKey("controller_confirm_back_layout")
private val KEY_XY_LAYOUT    = stringPreferencesKey("controller_xy_layout")
private val KEY_DISPLAY_TYPE = stringPreferencesKey("controller_display_type")
private val KEY_SCROLL_SPEED = stringPreferencesKey("controller_scroll_speed")
private val KEY_STICK_SENSITIVITY = stringPreferencesKey("controller_stick_sensitivity")
private val KEY_TRIGGER_SENSITIVITY = stringPreferencesKey("controller_trigger_sensitivity")
private val KEY_SHOULDER_HOLD = stringPreferencesKey("controller_shoulder_hold")
private val KEY_LEFT_BACKS_OUT = booleanPreferencesKey("controller_left_backs_out")

@Singleton
class ControllerLayoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mappingRepository: ControllerMappingRepository,
) {
    val prefs: Flow<ControllerLayoutPrefs> = context.echoDataStore.data.map { store ->
        ControllerLayoutPrefs(
            confirmBackLayout = store[KEY_CONFIRM_BACK]
                ?.let { runCatching { ConfirmBackLayout.valueOf(it) }.getOrNull() }
                ?: ConfirmBackLayout.STANDARD,
            xyLayout = store[KEY_XY_LAYOUT]
                ?.let { runCatching { XYLayout.valueOf(it) }.getOrNull() }
                ?: XYLayout.STANDARD,
            displayType = ControllerDisplayType.fromName(store[KEY_DISPLAY_TYPE]),
            scrollSpeed = store[KEY_SCROLL_SPEED]
                ?.let { runCatching { ScrollSpeed.valueOf(it) }.getOrNull() }
                ?: ScrollSpeed.STANDARD,
            stickSensitivity = com.echo.core.domain.model.StickSensitivity
                .fromName(store[KEY_STICK_SENSITIVITY]),
            triggerSensitivity = com.echo.core.domain.model.TriggerSensitivity
                .fromName(store[KEY_TRIGGER_SENSITIVITY]),
            shoulderHoldTime = com.echo.core.domain.model.ShoulderHoldTime
                .fromName(store[KEY_SHOULDER_HOLD]),

            leftBacksOut = store[KEY_LEFT_BACKS_OUT] ?: true,
        )
    }

    suspend fun setConfirmBackLayout(layout: ConfirmBackLayout) {
        context.echoDataStore.edit { it[KEY_CONFIRM_BACK] = layout.name }
        applyLayout(confirmBack = layout, xy = currentXyLayout())
        Timber.i("ConfirmBackLayout set: $layout")
    }

    suspend fun setXYLayout(layout: XYLayout) {
        context.echoDataStore.edit { it[KEY_XY_LAYOUT] = layout.name }
        applyLayout(confirmBack = currentConfirmBackLayout(), xy = layout)
        Timber.i("XYLayout set: $layout")
    }

    private suspend fun applyLayout(confirmBack: ConfirmBackLayout, xy: XYLayout) {
        mappingRepository.saveMappings(gamepadMappingsFor(confirmBack, xy))
    }

    private suspend fun currentConfirmBackLayout(): ConfirmBackLayout =
        context.echoDataStore.data.first()[KEY_CONFIRM_BACK]
            ?.let { runCatching { ConfirmBackLayout.valueOf(it) }.getOrNull() }
            ?: ConfirmBackLayout.STANDARD

    private suspend fun currentXyLayout(): XYLayout =
        context.echoDataStore.data.first()[KEY_XY_LAYOUT]
            ?.let { runCatching { XYLayout.valueOf(it) }.getOrNull() }
            ?: XYLayout.STANDARD

    suspend fun setDisplayType(type: ControllerDisplayType) {
        context.echoDataStore.edit { it[KEY_DISPLAY_TYPE] = type.name }
        Timber.i("ControllerDisplayType set: $type")
    }

    suspend fun setStickSensitivity(value: com.echo.core.domain.model.StickSensitivity) {
        context.echoDataStore.edit { it[KEY_STICK_SENSITIVITY] = value.name }
        Timber.i("StickSensitivity set: $value")
    }

    suspend fun setTriggerSensitivity(value: com.echo.core.domain.model.TriggerSensitivity) {
        context.echoDataStore.edit { it[KEY_TRIGGER_SENSITIVITY] = value.name }
    }

    suspend fun setShoulderHoldTime(value: com.echo.core.domain.model.ShoulderHoldTime) {
        context.echoDataStore.edit { it[KEY_SHOULDER_HOLD] = value.name }
    }

    suspend fun setScrollSpeed(speed: ScrollSpeed) {
        context.echoDataStore.edit { it[KEY_SCROLL_SPEED] = speed.name }
        Timber.i("ScrollSpeed set: $speed")
    }

    suspend fun setLeftBacksOut(enabled: Boolean) {
        context.echoDataStore.edit { it[KEY_LEFT_BACKS_OUT] = enabled }
        Timber.i("LeftBacksOut set: $enabled")
    }

    suspend fun resetAllPrefs() {
        context.echoDataStore.edit { store ->
            store.remove(KEY_CONFIRM_BACK)
            store.remove(KEY_XY_LAYOUT)
            store.remove(KEY_DISPLAY_TYPE)
            store.remove(KEY_SCROLL_SPEED)
            store.remove(KEY_LEFT_BACKS_OUT)
            store.remove(KEY_STICK_SENSITIVITY)
            store.remove(KEY_TRIGGER_SENSITIVITY)
            store.remove(KEY_SHOULDER_HOLD)
        }
        mappingRepository.resetToDefaults()
        Timber.i("Controller layout prefs reset to defaults")
    }
}
