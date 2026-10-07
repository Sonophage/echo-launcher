package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// owner, 2026-10-07: the theme store shows a theme's hero, screenshots and README metadata, and the Mix
// screen offers each part a theme has. These travel inside the theme, so a shared theme looks the same
class ThemeStoreFormatTest {
    private val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(16)

    @Test
    fun `hero, screenshots and readme travel in the file and the folder`() {
        val bundle = EchoThemeBundle(
            EchoThemeManifest(name = "Aurora", accentColor = "#7C5CFF"), wallpaper = null, preview = null,
            hero = ThemeImage(jpg, "jpg"),
            screenshots = mapOf("01-crossbar.jpg" to ThemeImage(jpg, "jpg"), "02-music.png" to ThemeImage(jpg, "png")),
            readme = "---\nauthor: ECHO\n---\nHello",
        )
        val back = assertNotNull(EchoThemeCodec.read(EchoThemeCodec.write(bundle)))
        assertEquals(bundle.hero, back.hero)
        assertEquals(bundle.screenshots, back.screenshots)
        assertEquals(bundle.readme, back.readme)

        val files = EchoThemeFolder.toFiles(bundle)
        assertEquals(setOf("theme.json", "README.md", "Preview/hero.jpg", "Preview/Screenshots/01-crossbar.jpg", "Preview/Screenshots/02-music.png"), files.keys)
        assertEquals(bundle.screenshots, assertNotNull(EchoThemeFolder.toBundle(files)).screenshots)
    }

    @Test
    fun `screenshots past the limit and files that are not pictures are dropped`() {
        val shots = (1..12).associate { "%02d.jpg".format(it) to ThemeImage(jpg, "jpg") } + ("notes.txt" to ThemeImage(jpg, "txt"))
        val back = assertNotNull(EchoThemeCodec.read(EchoThemeCodec.write(
            EchoThemeBundle(EchoThemeManifest(name = "Many", accentColor = ""), null, null, screenshots = shots),
        )))
        assertEquals(EchoThemeCodec.MAX_SCREENSHOTS, back.screenshots.size)
        assertNull(back.screenshots["notes.txt"])
    }

    @Test
    fun `the readme's front matter is the theme's metadata`() {
        val r = ThemeReadme.parse("---\nauthor: Seth\nversion: 1.2\ndescription: Night sky: violet\ntags: dark, calm\n---\n\n# Aurora\nText")
        assertEquals("Seth", r.author)
        assertEquals("1.2", r.version)
        assertEquals("Night sky: violet", r.description, "only the first colon splits a line")
        assertEquals(listOf("dark", "calm"), r.tags)
        assertEquals("# Aurora\nText", r.body)
        assertEquals(ThemeReadme(emptyMap(), "Just text"), ThemeReadme.parse("Just text"))
        assertEquals(emptyMap(), ThemeReadme.parse("---\nauthor: x").fields, "an unclosed block is text, not metadata")
    }

    @Test
    fun `a theme offers exactly the parts it has`() {
        val sounds = EchoThemeBundle(
            EchoThemeManifest(name = "Pack", accentColor = ""), null, null,
            media = mapOf("sound_back" to ThemeImage(jpg, "wav")),
        )
        assertEquals(setOf(ThemePart.SOUNDS), sounds.parts())
        val full = sounds.copy(
            manifest = sounds.manifest.copy(accentColor = "#112233", waveDesign = "PSP", buttonSet = "XBOX", gameBootStyle = "LENS"),
            icons = mapOf("catbar_games" to ThemeImage(jpg, "png")),
            wallpaper = jpg,
            media = sounds.media + ("boot_audio" to ThemeImage(jpg, "wav")),
        )
        assertEquals(ThemePart.entries.toSet(), full.parts())
    }
}
