package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// owner, 2026-10-07: the Template theme shows people how to make their own. Every file name it gives must be
// one a theme keeps, or someone follows it and their file is silently dropped
class EchoThemeTemplateTest {
    private val template = EchoThemeTemplate.files()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8)

    @Test
    fun `the template is itself a theme with every folder`() {
        assertNotNull(EchoThemeFolder.toBundle(template))
        for (folder in listOf("Icons", "Icons/Consoles", "Wallpaper", "Fonts", "Sounds", "Boot", "GameStart", "Preview")) {
            assertTrue("$folder/README.txt" in template.keys, "$folder has a README")
        }
    }

    @Test
    fun `every file the template names is kept by a theme`() {
        val named = buildMap {
            for (folder in ThemeMedia.FOLDERS.values.toSet()) {
                lines("$folder/README.txt").forEach { key -> put("$folder/$key.mp3", ByteArray(4)) }
            }
            lines("Icons/README.txt").forEach { put("Icons/${it.substringBefore(' ')}", png) }
            lines("Icons/Consoles/README.txt").forEach { put("Icons/Consoles/$it", png) }
        }
        val theme = assertNotNull(EchoThemeFolder.toBundle(template + named))
        assertEquals(ThemeMedia.FOLDERS.keys, theme.media.keys)
        assertEquals(IconSlots.ALL.map { it.key }.toSet(), theme.icons.keys)
        assertEquals(SYSICON_PLATFORM_IDS.toSet(), theme.sysicons.keys)
    }

    // the README's list: the lines after its first blank line
    private fun lines(path: String): List<String> =
        template.getValue(path).decodeToString().substringAfter("\n\n").lines().filter { it.isNotBlank() }
}
