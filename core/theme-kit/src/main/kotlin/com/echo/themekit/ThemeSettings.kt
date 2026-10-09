package com.echo.themekit

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// the look settings a theme may carry in theme.json's "settings" (owner, 2026-10-09): how the rows and cards
// show games, never an account, a control or a player. Each key is ECHO's own preference name, stored with
// the type EchoSettingsExport gives it (a test in core-data pins the two lists together). A null value puts
// the setting back to ECHO's default, which is how the look kept before a theme records one never set
object ThemeSettings {
    enum class Kind { BOOL, TEXT }

    val KEYS: Map<String, Kind> = linkedMapOf(
        "pref_xmb_row_cover_art" to Kind.BOOL,
        "display_card_art_grid" to Kind.BOOL,
        "pref_animated_icons" to Kind.BOOL,
        "pref_xmb_item_backdrop" to Kind.BOOL,
        "pref_xmb_game_metadata" to Kind.BOOL,
        "display_icon_legibility" to Kind.TEXT,
        "display_fade_by_distance" to Kind.BOOL,
        "display_wave_over_wallpaper" to Kind.BOOL,
    )

    // the listed keys whose value has the listed type, or is null; null when nothing is left
    fun clean(settings: JsonObject?): JsonObject? {
        val kept = settings.orEmpty().filter { (key, value) ->
            val kind = KEYS[key] ?: return@filter false
            value is JsonNull || (value is JsonPrimitive && when (kind) {
                Kind.BOOL -> !value.isString && value.booleanOrNull != null
                Kind.TEXT -> value.isString
            })
        }
        return kept.takeIf { it.isNotEmpty() }?.let(::JsonObject)
    }
}
