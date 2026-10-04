package com.echo.core.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// the settings the ECHO folder carries in settings.json (owner, 2026-10-04): how ECHO looks and
// behaves, and nothing else. No key, password, account, profile, folder path or internal flag is
// listed; EchoSettingsExportTest fails if one is added.
object EchoSettingsExport {
    const val FILE_NAME = "settings.json"
    const val FORMAT = "echo-settings"
    const val VERSION = 1

    val GROUPS: Map<String, List<String>> = linkedMapOf(
        "look" to listOf(
            "display_color_scheme", "theme_accent_override", "theme_accent_from_wallpaper", "theme_icon_color",
            "display_text_color", "display_wave_design", "display_wave_style", "display_wave_over_wallpaper",
            "display_icon_legibility", "display_xmb_layout_adjust", "theme_layout_spec",
        ),
        "interface" to listOf(
            "interface_last_played_size", "interface_island_shows_recent", "display_recents_include_apps",
            "interface_show_device_notifications", "interface_context_menu_hint",
            "interface_context_menu_hint_delay_seconds", "pref_xmb_item_backdrop", "pref_xmb_game_metadata",
            "pref_animated_icons", "display_launch_disc", "display_gameboot_enabled", "display_gameboot_mode",
            "display_show_boot", "display_boot_on_resume", "sound_menu_enabled", "sound_menu_music",
            "display_battery_saver", "display_thermal_aware",
        ),
        "controls" to listOf(
            "controller_confirm_back_layout", "controller_xy_layout", "controller_left_backs_out",
            "controller_scroll_speed", "controller_stick_sensitivity", "controller_trigger_sensitivity",
            "controller_shoulder_hold", "controller_display_type", "controller_mappings_v1",
            "interface_touch_sensitivity", "interface_touch_nav_button",
        ),
        "players" to listOf(
            "music_default_player_package", "video_default_player", "photo_default_viewer",
            "books_default_reader", "video_seek_step_seconds", "video_controls_hide_ms",
        ),
    )

    private val pretty = Json { prettyPrint = true }

    // [prefs] is the launcher's preferences by key name; a setting that is not set is left out
    fun json(prefs: Map<String, Any?>): String = pretty.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            GROUPS.forEach { (group, keys) ->
                val values = keys.mapNotNull { key -> prefs[key]?.let { key to element(it) } }.toMap()
                if (values.isNotEmpty()) put(group, JsonObject(values))
            }
        },
    ) + "\n"

    // a setting stored as JSON text (button mappings, layout) is written as JSON, so it reads as one
    private fun element(value: Any): JsonElement = when (value) {
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> value.takeIf { it.startsWith("{") || it.startsWith("[") }
            ?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } ?: JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }
}
