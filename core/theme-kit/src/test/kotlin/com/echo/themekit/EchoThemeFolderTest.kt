package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// owner, 2026-10-07: a theme lives in the ECHO folder as a folder a person can open and edit, and reads
// back as the same theme
class EchoThemeFolderTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8) { it.toByte() }
    private val mp4 = "ftypmp42".toByteArray() + ByteArray(12) { it.toByte() }

    @Test
    fun `a theme written as a folder reads back as the same theme`() {
        val bundle = EchoThemeBundle(
            manifest = EchoThemeManifest(name = "Dusk", accentColor = "#FF8800"),
            wallpaper = png,
            preview = null,
            icons = mapOf("catbar_games" to ThemeImage(png, "png")),
            sysicons = mapOf("psx" to ThemeImage(png, "png")),
            motion = ThemeMotion.ofBytes(mp4, "mp4"),
        )

        val files = EchoThemeFolder.toFiles(bundle)

        assertEquals(
            setOf("theme.json", "Wallpaper/wallpaper.png", "Wallpaper/motion.mp4", "Icons/catbar_games.png", "Icons/Consoles/psx.png"),
            files.keys,
        )
        val back = assertNotNull(EchoThemeFolder.toBundle(files))
        assertEquals(bundle.manifest, back.manifest)
        assertEquals(setOf("catbar_games"), back.icons.keys)
        assertEquals(setOf("psx"), back.sysicons.keys)
        assertTrue(back.wallpaper!!.contentEquals(png))
        assertEquals("mp4", back.motion?.extension)
    }

    @Test
    fun `a folder made by hand reads whatever the case of its folders, and ignores what has no place`() {
        val files = mapOf(
            "theme.json" to """{"manifest":"echo-theme","name":"Hand","accentColor":"#00FF00"}""".toByteArray(),
            "icons/catbar_music.png" to png,
            "wallpaper/wallpaper.png" to png,
            "notes.txt" to "my theme".toByteArray(),
            "Icons/Old/catbar_games.png" to png,
        )
        val theme = assertNotNull(EchoThemeFolder.toBundle(files))
        assertEquals("Hand", theme.manifest.name)
        assertEquals(setOf("catbar_music"), theme.icons.keys)
        assertNotNull(theme.wallpaper)
    }

    @Test
    fun `notes in several folders, or one folder written in two cases, do not break a theme`() {
        val files = mapOf(
            "theme.json" to """{"manifest":"echo-theme","name":"Notes","accentColor":"#00FF00"}""".toByteArray(),
            "Sounds/README.txt" to "a".toByteArray(),
            "Boot/README.txt" to "b".toByteArray(),
            "Icons/catbar_music.png" to png,
            "icons/catbar_music.png" to png,
            "Boot/sound_back.wav" to "RIFF".toByteArray(),
        )
        val theme = assertNotNull(EchoThemeFolder.toBundle(files))
        assertEquals(setOf("catbar_music"), theme.icons.keys)
        assertTrue(theme.media.isEmpty(), "a sound in another slot's folder has no place")
    }

    @Test
    fun `a folder without theme json is not a theme`() {
        assertNull(EchoThemeFolder.toBundle(mapOf("Icons/catbar_games.png" to png)))
    }

    @Test
    fun `a theme's folder name drops what file systems refuse`() {
        assertEquals("DuskBlue", EchoThemeFolder.folderName("Dusk/Blue?", "pfp_1"))
        assertEquals("Neon Night", EchoThemeFolder.folderName("Neon: Night", "pfp_1"))
        assertEquals("pfp_1", EchoThemeFolder.folderName("  ..", "pfp_1"))
    }
}
