package com.echo.themekit

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// owner, 2026-10-07: the online theme store reads index.json and downloads each theme as a zip of its folder
class ThemeCatalogTest {
    private val sha = "a".repeat(64)

    @Test
    fun `the catalog's paths resolve against the index, and an entry it cannot install is left out`() {
        val text = """
            {"format": 1, "themes": [
              {"id": "PSP", "name": "PSP", "archive": "themes/PSP.zip", "sha256": "$sha", "size": 10,
               "hero": "themes/PSP/Preview/hero.jpg", "readme": "themes/PSP/README.md",
               "screenshots": ["themes/PSP/Preview/Screenshots/01.jpg", "file:///etc/passwd"]},
              {"id": "NoSum", "archive": "themes/x.zip", "sha256": "nope", "size": 10},
              {"id": "Local", "archive": "file:///sdcard/x.zip", "sha256": "$sha", "size": 10}
            ]}
        """.trimIndent()
        val themes = assertNotNull(ThemeCatalog.parse(text, "https://sonophage.github.io/echo-themes/index.json"))
        assertEquals(listOf("PSP"), themes.map { it.id })
        val psp = themes.single()
        assertEquals("https://sonophage.github.io/echo-themes/themes/PSP.zip", psp.archiveUrl)
        assertEquals("https://sonophage.github.io/echo-themes/themes/PSP/Preview/hero.jpg", psp.heroUrl)
        assertEquals(listOf("https://sonophage.github.io/echo-themes/themes/PSP/Preview/Screenshots/01.jpg"), psp.screenshotUrls)
    }

    @Test
    fun `a catalog of another format, or not a catalog, reads as nothing`() {
        assertNull(ThemeCatalog.parse("""{"format": 2, "themes": []}""", "https://x/index.json"))
        assertNull(ThemeCatalog.parse("<html>", "https://x/index.json"))
    }

    @Test
    fun `a zipped theme folder reads as its theme, inside a top folder or not`() {
        val manifest = """{"manifest":"echo-theme","name":"Wild","accentColor":"#E8C66A","buttonSet":"NINTENDO"}""".toByteArray()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8)
        val nested = zip("Wild/" to null, "Wild/theme.json" to manifest, "Wild/Icons/catbar_games.png" to png, "Wild/README.md" to "---\nauthor: ECHO\n---\n".toByteArray())
        val theme = assertNotNull(EchoThemeFolder.fromArchive(nested.inputStream()))
        assertEquals("NINTENDO", theme.manifest.buttonSet)
        assertEquals(setOf("catbar_games"), theme.icons.keys)
        assertEquals("ECHO", ThemeReadme.parse(theme.readme).author)

        assertNotNull(EchoThemeFolder.fromArchive(zip("theme.json" to manifest).inputStream()))
        assertNull(EchoThemeFolder.fromArchive(zip("Wild/Icons/catbar_games.png" to png).inputStream()), "no theme.json, no theme")
    }

    private fun zip(vararg entries: Pair<String, ByteArray?>): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                for ((name, data) in entries) {
                    z.putNextEntry(ZipEntry(name)); data?.let(z::write); z.closeEntry()
                }
            }
        }.toByteArray()
}
