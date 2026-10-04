package com.echo.core.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// settings.json sits in a folder a user may share: it must never carry a secret or a personal detail
class EchoSettingsExportTest {
    // checked against all of the launcher's preference names on 2026-10-04: it flags every key, account,
    // path, identifier, reading position and usage record, and none of the listed settings
    private val forbidden = Regex(
        "key|secret|password|token|user|session|uri|path|steam|igdb|ss_|ra_|discord|profile|seeded|stamp|_v\\d+$|" +
            "custom_wallpaper|motion_wallpaper|uuid|bookmarks|position|dismissals|cached|resolved",
    )

    @Test
    fun `no listed setting looks like a secret, an account, a path or an internal flag`() {
        EchoSettingsExport.GROUPS.values.flatMap { it.keys }
            .filterNot { it == "controller_mappings_v1" }
            .forEach { assertFalse(forbidden.containsMatchIn(it), "$it must not be in settings.json") }
    }

    @Test
    fun `secrets in the preferences never reach the file`() {
        val text = EchoSettingsExport.json(mapOf(
            "sgdb_api_key" to "abc123", "ss_password" to "hunter2", "profile_name" to "Seth",
            "artwork_folder_tree_uri" to "content://x", "display_color_scheme" to "OCEAN",
        ))
        listOf("abc123", "hunter2", "Seth", "content://").forEach { assertFalse(it in text, "$it leaked") }
        assertTrue("OCEAN" in text)
    }

    @Test
    fun `values keep their types, JSON settings are nested, and unset groups are left out`() {
        val root = Json.parseToJsonElement(EchoSettingsExport.json(mapOf(
            "display_wave_over_wallpaper" to true, "interface_last_played_size" to 2,
            "controller_mappings_v1" to """{"bindings":[]}""",
        ))).jsonObject
        assertEquals("echo-settings", root["format"]!!.jsonPrimitive.content)
        assertEquals("true", root["look"]!!.jsonObject["display_wave_over_wallpaper"].toString())
        assertEquals("2", root["interface"]!!.jsonObject["interface_last_played_size"].toString())
        assertTrue(root["controls"]!!.jsonObject["controller_mappings_v1"]!!.jsonObject.containsKey("bindings"))
        assertNull(root["players"])
    }

    @Test
    fun `boot media goes under Boot and every other sound under Sounds`() {
        assertEquals("Boot", EchoFolder.lookFolderFor("boot_audio.mp3"))
        assertEquals("Boot", EchoFolder.lookFolderFor("gameboot_audio.mp3"))
        assertEquals("Boot", EchoFolder.lookFolderFor("launch_disc_audio.mp3"))
        assertEquals("Sounds", EchoFolder.lookFolderFor("menu_music.mp3"))
        assertEquals("Sounds", EchoFolder.lookFolderFor("sound_back.mp3"))
    }

    @Test
    fun `a newer file in the folder is the user's edit and is kept`() {
        assertTrue(EchoFolder.shouldWrite(100, 10, null, null), "missing")
        assertFalse(EchoFolder.shouldWrite(100, 10, 200, 99), "edited in the folder after ECHO's copy")
        assertFalse(EchoFolder.shouldWrite(100, 10, 150, 10), "unchanged copy")
        assertTrue(EchoFolder.shouldWrite(300, 12, 150, 10), "ECHO's file changed since")
    }

    @Test
    fun `a settings file read back keeps each value's type and skips what is not listed or is mistyped`() {
        val parsed = EchoSettingsExport.parse("""
            {"format":"echo-settings","version":1,
             "look":{"display_wave_over_wallpaper":false,"theme_accent_override":4292430381,"display_color_scheme":"OCEAN",
                     "display_xmb_layout_adjust":{"compact":{"scale":1.2}}},
             "interface":{"interface_last_played_size":"big","interface_context_menu_hint_delay_seconds":2.5},
             "players":{"sgdb_api_key":"abc"},
             "secrets":{"ss_password":"x"}}
        """.trimIndent())!!
        assertEquals(false, parsed.values["display_wave_over_wallpaper"])
        assertEquals(4292430381L, parsed.values["theme_accent_override"])
        assertEquals("OCEAN", parsed.values["display_color_scheme"])
        assertEquals(2.5f, parsed.values["interface_context_menu_hint_delay_seconds"])
        assertTrue((parsed.values["display_xmb_layout_adjust"] as String).contains("\"scale\":1.2"))
        assertNull(parsed.values["interface_last_played_size"], "a word where a number belongs is not stored")
        assertNull(parsed.values["sgdb_api_key"], "an unlisted key is never stored, whatever group it sits in")
        assertTrue("secrets" in parsed.skipped && "players.sgdb_api_key" in parsed.skipped)
    }

    @Test
    fun `a file that is not ECHO's settings is not read at all`() {
        assertNull(EchoSettingsExport.parse("""{"format":"something-else","look":{}}"""))
        assertNull(EchoSettingsExport.parse("not json"))
    }

    @Test
    fun `what ECHO writes it reads back unchanged`() {
        val prefs = mapOf<String, Any>("display_wave_over_wallpaper" to true, "theme_accent_override" to 4292430381L,
            "interface_context_menu_hint_delay_seconds" to 1.5f, "interface_last_played_size" to 2, "display_color_scheme" to "BLACK")
        assertEquals(prefs, EchoSettingsExport.parse(EchoSettingsExport.json(prefs))!!.values)
    }

    // the declared kind and the key's declaration in the source must agree, or a value read back is
    // stored as a type ECHO never reads
    @Test
    fun `each listed setting's kind matches how its key is declared in the code`() {
        val repo = generateSequence(java.io.File("").absoluteFile) { it.parentFile }.first { java.io.File(it, "settings.gradle.kts").isFile }
        val declared = HashMap<String, String>()
        val pattern = Regex("""(boolean|int|long|float|string)PreferencesKey\("([a-z0-9_]+)"\)""")
        repo.walkTopDown().onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.extension == "kt" && "/src/main/" in it.path }
            .forEach { f -> pattern.findAll(f.readText()).forEach { declared[it.groupValues[2]] = it.groupValues[1] } }
        val expected = mapOf(EchoSettingsExport.Kind.BOOL to "boolean", EchoSettingsExport.Kind.INT to "int",
            EchoSettingsExport.Kind.LONG to "long", EchoSettingsExport.Kind.FLOAT to "float",
            EchoSettingsExport.Kind.TEXT to "string", EchoSettingsExport.Kind.JSON to "string")
        EchoSettingsExport.GROUPS.values.flatMap { it.entries }.forEach { (key, kind) ->
            val actual = declared[key]
            if (actual != null) assertEquals(expected[kind], actual, "$key is declared as $actual")
        }
        assertTrue(declared.size > 100, "the scan found ${declared.size} keys; it is not reading the sources")
    }

    @Test
    fun `a folder file replaces ECHO's only when it is newer and different, so ECHO's own copy never loops back`() {
        assertTrue(EchoFolder.shouldRead(100, null, sameContent = false), "ECHO has none")
        assertFalse(EchoFolder.shouldRead(500, 100, sameContent = true), "the mirror's own copy: newer but the same")
        assertTrue(EchoFolder.shouldRead(500, 100, sameContent = false), "edited in the folder")
        assertFalse(EchoFolder.shouldRead(50, 100, sameContent = false), "ECHO's is newer")
    }
}
