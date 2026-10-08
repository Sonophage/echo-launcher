package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.ControllerHintPolicy
import kotlinx.coroutines.flow.first

data class InterfaceChoices(
    val showDeviceNotifications: Boolean = true,
    val islandShowsRecent: Boolean = true,
    val lastPlayedSize: Int = InterfacePreferences.DEFAULT_LAST_PLAYED_SIZE,
    val rescanOnReturn: Boolean = true,
    val videoSeekStepSeconds: Int = InterfacePreferences.DEFAULT_VIDEO_SEEK_STEP_SECONDS,
    val videoControlsHideMs: Int = InterfacePreferences.DEFAULT_VIDEO_CONTROLS_HIDE_MS,
    // owner, 2026-10-07: the footer shows only the A card and the filter
    val minimalHints: Boolean = false,
)

// owner, 2026-10-07: one Button hints setting. Off hides the prompts; Minimal keeps only the A card and the filter
enum class ButtonHints(val label: String) {
    ALL("All"), MINIMAL("Minimal"), OFF("Off");

    val next: ButtonHints get() = entries[(ordinal + 1) % entries.size]
}

object InterfacePreferences {
    val KEY_SHOW_DEVICE_NOTIFICATIONS = booleanPreferencesKey("interface_show_device_notifications")
    val KEY_ISLAND_SHOWS_RECENT = booleanPreferencesKey("interface_island_shows_recent")
    val KEY_LAST_PLAYED_SIZE = intPreferencesKey("interface_last_played_size")
    val KEY_RESCAN_ON_RETURN = booleanPreferencesKey("library_rescan_on_return")
    val KEY_VIDEO_SEEK_STEP_SECONDS = intPreferencesKey("video_seek_step_seconds")
    val KEY_VIDEO_CONTROLS_HIDE_MS = intPreferencesKey("video_controls_hide_ms")
    val KEY_MINIMAL_HINTS = booleanPreferencesKey("interface_minimal_hints")
    val KEY_BUTTON_HINTS_ON = booleanPreferencesKey("interface_context_menu_hint")

    val LAST_PLAYED_SIZES = listOf(10, 15, 20, 30)
    const val DEFAULT_LAST_PLAYED_SIZE = 15

    val VIDEO_SEEK_STEPS_SECONDS = listOf(5, 10, 30)
    const val DEFAULT_VIDEO_SEEK_STEP_SECONDS = 10

    val VIDEO_CONTROLS_HIDE_MS = listOf(2_000, 3_500, 6_000)
    const val DEFAULT_VIDEO_CONTROLS_HIDE_MS = 3_500

    fun read(prefs: Preferences) = InterfaceChoices(
        showDeviceNotifications = prefs[KEY_SHOW_DEVICE_NOTIFICATIONS] ?: true,
        islandShowsRecent = prefs[KEY_ISLAND_SHOWS_RECENT] ?: true,
        lastPlayedSize = prefs[KEY_LAST_PLAYED_SIZE].oneOf(LAST_PLAYED_SIZES, DEFAULT_LAST_PLAYED_SIZE),
        rescanOnReturn = prefs[KEY_RESCAN_ON_RETURN] ?: true,
        videoSeekStepSeconds = prefs[KEY_VIDEO_SEEK_STEP_SECONDS]
            .oneOf(VIDEO_SEEK_STEPS_SECONDS, DEFAULT_VIDEO_SEEK_STEP_SECONDS),
        videoControlsHideMs = prefs[KEY_VIDEO_CONTROLS_HIDE_MS]
            .oneOf(VIDEO_CONTROLS_HIDE_MS, DEFAULT_VIDEO_CONTROLS_HIDE_MS),
        minimalHints = prefs[KEY_MINIMAL_HINTS] ?: false,
    )

    // Button hints is stored in the two keys it replaced, so a saved choice, a backup and settings.json carry over
    fun buttonHints(prefs: Preferences): ButtonHints =
        buttonHintsOf(prefs[KEY_BUTTON_HINTS_ON] ?: ControllerHintPolicy.DEFAULT_ENABLED, prefs[KEY_MINIMAL_HINTS] == true)

    fun buttonHintsOf(on: Boolean, minimal: Boolean): ButtonHints = when {
        !on -> ButtonHints.OFF
        minimal -> ButtonHints.MINIMAL
        else -> ButtonHints.ALL
    }

    fun MutablePreferences.setButtonHints(hints: ButtonHints) {
        this[KEY_BUTTON_HINTS_ON] = hints != ButtonHints.OFF
        this[KEY_MINIMAL_HINTS] = hints == ButtonHints.MINIMAL
    }

    suspend fun current(context: Context): InterfaceChoices = read(context.echoDataStore.data.first())

    private fun Int?.oneOf(allowed: List<Int>, default: Int) = this?.takeIf { it in allowed } ?: default
}
