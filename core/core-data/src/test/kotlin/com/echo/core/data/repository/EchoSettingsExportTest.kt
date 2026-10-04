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
        EchoSettingsExport.GROUPS.values.flatten()
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
}
