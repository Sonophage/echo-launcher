package com.echo.themekit

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ThemeSettingsTest {
    private val settings = buildJsonObject {
        put("pref_xmb_row_cover_art", true)
        put("display_icon_legibility", "CONTOUR_AUTO")
        put("display_fade_by_distance", JsonNull)
    }

    @Test
    fun `a theme keeps only look settings of the right type, so it can never set an account or a control`() {
        val raw = buildJsonObject {
            put("pref_xmb_row_cover_art", true)
            put("display_card_art_grid", "yes")
            put("display_icon_legibility", 3)
            put("sgdb_api_key", "secret")
            put("controller_xy_layout", "SWAPPED")
            put("display_fade_by_distance", JsonNull)
            put("display_color_scheme", "NEON")
            put("display_text_shadow", false)
        }
        assertEquals(
            JsonObject(mapOf(
                "pref_xmb_row_cover_art" to JsonPrimitive(true), "display_fade_by_distance" to JsonNull,
                "display_text_shadow" to JsonPrimitive(false),
            )),
            ThemeSettings.clean(raw),
        )
        assertNull(ThemeSettings.clean(buildJsonObject { put("sgdb_api_key", "secret") }), "nothing left is no settings")
        assertEquals("BLACK", ThemeSettings.clean(buildJsonObject { put("display_color_scheme", "BLACK") })?.get("display_color_scheme")?.let { (it as JsonPrimitive).content },
            "a colour scheme ECHO has is kept; NEON above is not, as ECHO would crash reading it")
    }

    @Test
    fun `settings travel in the file and the folder, nulls included`() {
        val bundle = EchoThemeBundle(EchoThemeManifest(name = "Rows", accentColor = "", settings = settings), null, null)
        assertEquals(settings, assertNotNull(EchoThemeCodec.read(EchoThemeCodec.write(bundle))).manifest.settings)
        assertEquals(settings, assertNotNull(EchoThemeFolder.toBundle(EchoThemeFolder.toFiles(bundle))).manifest.settings)
    }

    @Test
    fun `display settings is a part only when the theme carries a setting ECHO keeps`() {
        val plain = EchoThemeBundle(EchoThemeManifest(name = "Plain", accentColor = ""), null, null)
        assertEquals(emptySet(), plain.parts())
        assertEquals(setOf(ThemePart.SETTINGS), plain.copy(manifest = plain.manifest.copy(settings = settings)).parts())
        val unknown = plain.manifest.copy(settings = buildJsonObject { put("sgdb_api_key", "x") })
        assertEquals(emptySet(), plain.copy(manifest = unknown).parts())
    }
}
