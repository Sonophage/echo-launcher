package com.echo.core.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
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

    // how ECHO stores each setting, so a value read back from the folder keeps the type ECHO reads
    enum class Kind { BOOL, INT, LONG, FLOAT, TEXT, JSON }

    val GROUPS: Map<String, Map<String, Kind>> = linkedMapOf(
        "look" to linkedMapOf(
            "display_color_scheme" to Kind.TEXT,
            "theme_accent_override" to Kind.LONG,
            "theme_accent_from_wallpaper" to Kind.BOOL,
            "theme_icon_color" to Kind.LONG,
            "display_text_color" to Kind.LONG,
            "display_wave_design" to Kind.TEXT,
            "display_wave_style" to Kind.TEXT,
            "display_motion_style" to Kind.TEXT,
            "display_wave_over_wallpaper" to Kind.BOOL,
            "display_icon_legibility" to Kind.TEXT,
            com.echo.core.data.datastore.CROSSBAR_LAYOUT_ADJUST_KEY.name to Kind.JSON,
            "theme_layout_spec" to Kind.JSON,
        ),
        "interface" to linkedMapOf(
            "interface_last_played_size" to Kind.INT,
            "interface_island_shows_recent" to Kind.BOOL,
            "interface_minimal_hints" to Kind.BOOL,
            "display_recents_include_apps" to Kind.BOOL,
            "interface_show_device_notifications" to Kind.BOOL,
            "interface_context_menu_hint" to Kind.BOOL,
            "interface_context_menu_hint_delay_seconds" to Kind.FLOAT,
            "pref_xmb_item_backdrop" to Kind.BOOL,
            "pref_xmb_row_cover_art" to Kind.BOOL,
            "pref_xmb_game_metadata" to Kind.BOOL,
            "pref_animated_icons" to Kind.BOOL,
            "display_launch_disc" to Kind.BOOL,
            "display_launch_disc_style" to Kind.TEXT,
            "display_gameboot_enabled" to Kind.BOOL,
            "display_gameboot_style" to Kind.TEXT,
            "display_gameboot_mode" to Kind.TEXT,
            "display_show_boot" to Kind.BOOL,
            "display_boot_on_resume" to Kind.BOOL,
            "sound_menu_enabled" to Kind.BOOL,
            "sound_menu_music" to Kind.BOOL,
            "display_battery_saver" to Kind.BOOL,
            "display_thermal_aware" to Kind.BOOL,
        ),
        "controls" to linkedMapOf(
            "controller_confirm_back_layout" to Kind.TEXT,
            "controller_xy_layout" to Kind.TEXT,
            "controller_left_backs_out" to Kind.BOOL,
            "controller_scroll_speed" to Kind.TEXT,
            "controller_stick_sensitivity" to Kind.TEXT,
            "controller_trigger_sensitivity" to Kind.TEXT,
            "controller_shoulder_hold" to Kind.TEXT,
            "controller_display_type" to Kind.TEXT,
            "controller_mappings_v1" to Kind.JSON,
            "interface_touch_sensitivity" to Kind.TEXT,
            "interface_touch_nav_button" to Kind.TEXT,
        ),
        "players" to linkedMapOf(
            "music_default_player_package" to Kind.TEXT,
            "video_default_player" to Kind.TEXT,
            "photo_default_viewer" to Kind.TEXT,
            "books_default_reader" to Kind.TEXT,
            "video_seek_step_seconds" to Kind.INT,
            "video_controls_hide_ms" to Kind.INT,
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
                val values = keys.keys.mapNotNull { key -> prefs[key]?.let { key to element(it) } }.toMap()
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

    // what a settings.json from the folder holds: the listed settings with the type ECHO stores them
    // as (Boolean, Int, Long, Float or String); anything else is skipped and named in [skipped]
    data class Parsed(val values: Map<String, Any>, val skipped: List<String>)

    fun parse(text: String): Parsed? {
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        if ((root["format"] as? JsonPrimitive)?.contentOrNull != FORMAT) return null
        val values = LinkedHashMap<String, Any>()
        val skipped = ArrayList<String>()
        root.forEach { (group, element) ->
            if (group == "format" || group == "version") return@forEach
            val listed = GROUPS[group]
            val entries = element as? JsonObject
            if (listed == null || entries == null) { skipped += group; return@forEach }
            entries.forEach { (key, value) ->
                val kind = listed[key]
                val typed = kind?.let { typed(it, value) }
                if (typed == null) skipped += "$group.$key" else values[key] = typed
            }
        }
        return Parsed(values, skipped)
    }

    private fun typed(kind: Kind, value: JsonElement): Any? {
        val p = value as? JsonPrimitive
        return when (kind) {
            Kind.BOOL -> p?.takeUnless { it.isString }?.booleanOrNull
            Kind.INT -> p?.takeUnless { it.isString }?.intOrNull
            Kind.LONG -> p?.takeUnless { it.isString }?.longOrNull
            Kind.FLOAT -> p?.takeUnless { it.isString }?.floatOrNull
            Kind.TEXT -> p?.takeIf { it.isString }?.content
            Kind.JSON -> (value as? JsonObject ?: value as? JsonArray)?.toString()
        }
    }
}
