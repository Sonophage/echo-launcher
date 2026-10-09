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
        // owner, 2026-10-09: the colour scheme and the text shadow are look too
        "display_color_scheme" to Kind.TEXT,
        "display_text_shadow" to Kind.BOOL,
    )

    // the names a text setting may take, where ECHO reads it with valueOf and a stray name would crash. It
    // mirrors CrossbarColorScheme (core-domain), which theme-kit cannot see; ThemeColorSchemeTest in core-data
    // fails when the two drift
    val CHOICES: Map<String, Set<String>> = mapOf(
        "display_color_scheme" to setOf(
            "ORIGINAL", "CLASSIC_BLUE", "SUNSET_ORANGE", "FRESH_GREEN", "ROYAL_PURPLE", "CRIMSON_RED", "SILVER_MONO",
            "SAKURA_PINK", "GOLDEN_AMBER", "AQUA_TEAL", "MIDNIGHT_NAVY", "CHARCOAL", "BLACK",
        ),
    )

    // the listed keys whose value has the listed type, or is null; null when nothing is left
    fun clean(settings: JsonObject?): JsonObject? {
        val kept = settings.orEmpty().filter { (key, value) ->
            val kind = KEYS[key] ?: return@filter false
            value is JsonNull || (value is JsonPrimitive && when (kind) {
                Kind.BOOL -> !value.isString && value.booleanOrNull != null
                Kind.TEXT -> value.isString && CHOICES[key]?.contains(value.content) != false
            })
        }
        return kept.takeIf { it.isNotEmpty() }?.let(::JsonObject)
    }
}
