package com.echo.core.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.UiMediaSlot
import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.ThemeImage
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// owner, 2026-10-07: a theme carries sounds, boot and game-start media, the wave design, the game-start styles
// and the button set. A part the theme leaves out, or names a value ECHO does not know, keeps the person's own.
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EchoThemeStorePartsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val media = UiMediaStore(context)

    @Before
    fun clearState() {
        runBlocking { context.echoDataStore.edit { it.clear() } }
        File(context.filesDir, "pfpthemes").deleteRecursively()
        File(context.filesDir, UiMediaStore.UI_MEDIA_DIR).deleteRecursively()
        File(context.filesDir, EchoFontFiles.THEME_FONT_DIR).deleteRecursively()
        File(context.filesDir, EchoFontFiles.LOOK_FONT_DIR).deleteRecursively()
        mockkConstructor(MediaMetadataRetriever::class)
        every { anyConstructed<MediaMetadataRetriever>().setDataSource(any<String>()) } returns Unit
        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) } returns "100"
        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) } returns "audio/wav"
        every { anyConstructed<MediaMetadataRetriever>().release() } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `applying a theme sets the parts it carries and leaves the rest as they were`() = runTest {
        context.echoDataStore.edit {
            it[stringPreferencesKey("display_gameboot_style")] = "LENS"
            it[stringPreferencesKey("display_launch_disc_style")] = "LENS"
        }
        val store = EchoThemeStore(context, media)
        val saved = assertNotNull(store.importBundle(register(theme(
            EchoThemeManifest(
                name = "Arcs", accentColor = "#112233",
                waveDesign = "ECHO_ARCS", buttonSet = "PLAYSTATION", gameBootStyle = "SPARKLE",
            ),
            media = mapOf("sound_back" to ThemeImage(wav(), "wav")),
        ))))

        assertTrue(store.apply(saved.id))

        val prefs = context.echoDataStore.data.first()
        assertEquals("ECHO_ARCS", prefs[stringPreferencesKey("display_wave_design")])
        assertEquals("PLAYSTATION", prefs[stringPreferencesKey("controller_display_type")])
        assertEquals("LENS", prefs[stringPreferencesKey("display_gameboot_style")], "a style ECHO does not know keeps the person's own")
        assertEquals("LENS", prefs[stringPreferencesKey("display_launch_disc_style")], "a part the theme leaves out keeps the person's own")
        assertNotNull(media.pathFor(UiMediaSlot.SOUND_BACK), "the theme's Back sound is in use")
        assertEquals(null, media.pathFor(UiMediaSlot.SOUND_SCROLL))
    }

    @Test
    fun `saving the current look keeps its sounds and styles in the theme`() = runTest {
        val store = EchoThemeStore(context, media)
        val wavFile = File(context.cacheDir, "boot.wav").apply { writeBytes(wav()) }
        assertTrue(media.importFile(UiMediaSlot.BOOT_AUDIO, wavFile, "boot.wav").ok)
        context.echoDataStore.edit {
            it[stringPreferencesKey("display_wave_design")] = "ECHO_RINGS"
            it[stringPreferencesKey("display_gameboot_style")] = "DISC"
        }

        val saved = assertNotNull(store.saveCurrentLook("Mine"))

        val bundle = assertNotNull(EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${saved.id}.echo-theme")))
        assertEquals(setOf("boot_audio"), bundle.media.keys)
        assertEquals("ECHO_RINGS", bundle.manifest.waveDesign)
        assertEquals("DISC", bundle.manifest.gameBootStyle)
        assertEquals(null, bundle.manifest.buttonSet, "a setting the person never chose is not carried")
        assertEquals(null to null, bundle.manifest.focusStyle to bundle.manifest.motion, "nor a focus style or motion")
        assertEquals(null, bundle.manifest.settings, "nor a display setting")
    }

    // owner, 2026-10-07: applying a theme saves the look in use first, so a theme never costs you your own look
    @Test
    fun `applying a theme saves your own look first, once, however many themes you try`() = runTest {
        context.echoDataStore.edit { it[com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE] = 0x00AA33 }
        val store = EchoThemeStore(context, media)
        val a = assertNotNull(store.importBundle(register(theme(EchoThemeManifest(name = "Arcs", accentColor = "#112233"), media = emptyMap()))))
        val b = assertNotNull(store.importBundle(register(theme(EchoThemeManifest(name = "Rings", accentColor = "#445566"), media = emptyMap()))))

        assertTrue(store.apply(a.id))
        assertTrue(store.apply(b.id))

        val kept = store.themes.value.filter { it.name.startsWith("Before ") }
        assertEquals(listOf("Before Arcs"), kept.map { it.name }, "trying a second theme does not save the first one again")
        val bundle = assertNotNull(EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${kept.single().id}.echo-theme")))
        assertEquals("#00AA33", bundle.manifest.accentColor, "the saved look is the one in use before the theme")
    }

    @Test
    fun `a look changed after a theme is saved again, and the default look is not saved`() = runTest {
        val store = EchoThemeStore(context, media)
        val a = assertNotNull(store.importBundle(register(theme(EchoThemeManifest(name = "Arcs", accentColor = "#112233"), media = emptyMap()))))
        val b = assertNotNull(store.importBundle(register(theme(EchoThemeManifest(name = "Rings", accentColor = "#445566"), media = emptyMap()))))

        assertTrue(store.apply(a.id))
        assertEquals(emptyList(), store.themes.value.filter { it.name.startsWith("Before ") }.map { it.name },
            "the default look comes back with Reset, so it is not saved")

        context.echoDataStore.edit { it[com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE] = 0x00AA33 }
        assertTrue(store.apply(b.id))
        assertEquals(listOf("Before Rings"), store.themes.value.filter { it.name.startsWith("Before ") }.map { it.name })
    }

    // owner, 2026-10-07: a theme's store page takes each part on its own
    @Test
    fun `mixing takes one part from another theme and leaves the rest`() = runTest {
        val store = EchoThemeStore(context, media)
        val a = assertNotNull(store.importBundle(register(theme(
            EchoThemeManifest(name = "Arcs", accentColor = "#112233", waveDesign = "ECHO_ARCS"),
            media = mapOf("sound_back" to ThemeImage(wav(), "wav")),
        ))))
        val b = assertNotNull(store.importBundle(register(theme(
            EchoThemeManifest(name = "Rings", accentColor = "#445566", waveDesign = "ECHO_RINGS"),
            media = mapOf("sound_select" to ThemeImage(wav(), "wav")),
        ))))
        assertTrue(store.apply(a.id))
        assertTrue(store.apply(b.id, setOf(com.echo.themekit.ThemePart.SOUNDS)))

        val prefs = context.echoDataStore.data.first()
        assertEquals("ECHO_ARCS", prefs[stringPreferencesKey("display_wave_design")], "the wave stays the first theme's")
        assertEquals("Arcs", prefs[stringPreferencesKey("theme_applied_name")])
        assertNotNull(media.pathFor(UiMediaSlot.SOUND_SELECT), "the second theme's sounds are in use")
        assertEquals("Rings", store.partSources()[com.echo.themekit.ThemePart.SOUNDS])
        assertEquals("Arcs", store.partSources()[com.echo.themekit.ThemePart.WAVE])
        assertEquals("Arcs", store.partSources()[com.echo.themekit.ThemePart.COLOURS])
    }

    @Test
    fun `the store lists a theme's hero, metadata and parts, and its page has the screenshots`() = runTest {
        val store = EchoThemeStore(context, media)
        val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(16)
        val saved = assertNotNull(store.importBundle(register(EchoThemeCodec.write(EchoThemeBundle(
            EchoThemeManifest(name = "Aurora", accentColor = "#7C5CFF", buttonSet = "XBOX"), wallpaper = null, preview = null,
            hero = ThemeImage(jpg, "jpg"),
            screenshots = mapOf("01.jpg" to ThemeImage(jpg, "jpg")),
            readme = "---\nauthor: Seth\nversion: 2\ndescription: Night sky\n---\nBody",
        )))))

        val listed = store.themes.value.single { it.id == saved.id }
        assertNotNull(listed.heroPath)
        assertEquals("Seth", listed.author)
        assertEquals("Night sky", listed.description)
        assertEquals(setOf(com.echo.themekit.ThemePart.COLOURS, com.echo.themekit.ThemePart.BUTTONS), listed.parts)
        val details = assertNotNull(store.details(saved.id))
        assertEquals("Body", details.readme.body)
        assertEquals(1, details.screenshotPaths.size)
        assertTrue(File(details.screenshotPaths.single()).isFile)
    }

    // owner, 2026-10-09: a theme picks a focus style and a motion preset; one it leaves out or that ECHO does not
    // know keeps the person's own, and the look kept before the theme carries the person's
    @Test
    fun `a theme sets its focus style and motion, and the look before it keeps yours`() = runTest {
        context.echoDataStore.edit {
            it[EchoThemeStore.KEY_FOCUS_STYLE] = "BRACKET"
            it[EchoThemeStore.KEY_MOTION_PRESET] = "SNAPPY"
            it[com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE] = 0x00AA33
        }
        val store = EchoThemeStore(context, media)
        val halo = assertNotNull(store.importBundle(register(theme(
            EchoThemeManifest(name = "Halo", accentColor = "#112233", focusStyle = "HALO", motion = "WOBBLE"), media = emptyMap(),
        ))))
        assertTrue(com.echo.themekit.ThemePart.FOCUS in halo.parts)

        assertTrue(store.apply(halo.id))
        val prefs = context.echoDataStore.data.first()
        assertEquals("HALO", prefs[EchoThemeStore.KEY_FOCUS_STYLE])
        assertEquals("SNAPPY", prefs[EchoThemeStore.KEY_MOTION_PRESET], "a motion ECHO does not know keeps yours")

        val before = assertNotNull(store.themes.value.firstOrNull { it.name == "Before Halo" })
        val kept = assertNotNull(EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${before.id}.echo-theme"))).manifest
        assertEquals("BRACKET" to "SNAPPY", kept.focusStyle to kept.motion)
    }

    @Test
    fun `a look with nothing picked is kept as ECHO's defaults, so taking it back undoes everything a theme picked`() = runTest {
        context.echoDataStore.edit { it[com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE] = 0x00AA33 }
        val store = EchoThemeStore(context, media)
        val halo = assertNotNull(store.importBundle(register(theme(
            EchoThemeManifest(
                name = "Halo", accentColor = "#112233", focusStyle = "HALO", motion = "SOFT",
                waveDesign = "ECHO_ARCS", gameBootStyle = "DISC", launchDiscStyle = "DISC", buttonSet = "XBOX",
            ),
            media = emptyMap(),
        ))))
        assertTrue(store.apply(halo.id))
        val before = assertNotNull(store.themes.value.firstOrNull { it.name == "Before Halo" })
        assertTrue(store.apply(before.id))
        val prefs = context.echoDataStore.data.first()
        assertEquals("CLASSIC" to "CLASSIC", prefs[EchoThemeStore.KEY_FOCUS_STYLE] to prefs[EchoThemeStore.KEY_MOTION_PRESET])
        assertEquals(
            listOf("PSP", "LENS", "LENS", "GENERIC"),
            listOf("display_wave_design", "display_gameboot_style", "display_launch_disc_style", "controller_display_type")
                .map { prefs[stringPreferencesKey(it)] },
        )
    }

    // owner, 2026-10-09: a theme's font wins over the ECHO folder's Look/Fonts. A theme without one hands the font
    // back to the folder, Save as Theme carries the font in use, and the look kept before a theme leaves the
    // folder's font out, so taking it back hands the font back to the folder too
    @Test
    fun `a theme's font wins over the folder's, and leaving the theme hands the font back`() = runTest {
        val folderFont = File(context.filesDir, "${EchoFontFiles.LOOK_FONT_DIR}/font.ttf").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
        context.echoDataStore.edit { it[com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE] = 0x00AA33 }
        val store = EchoThemeStore(context, media)
        val typed = assertNotNull(store.importBundle(register(EchoThemeCodec.write(EchoThemeBundle(
            EchoThemeManifest(name = "Typed", accentColor = "#112233"), wallpaper = null, preview = null,
            font = ThemeImage(byteArrayOf(7, 7, 7), "otf"),
        )))))
        val plain = assertNotNull(store.importBundle(register(theme(EchoThemeManifest(name = "Plain", accentColor = "#445566"), media = emptyMap()))))
        assertTrue(com.echo.themekit.ThemePart.FONT in typed.parts)

        assertTrue(store.apply(typed.id))
        assertEquals(listOf(7, 7, 7), EchoFontFiles.theme(context)?.readBytes()?.map { it.toInt() })
        assertEquals("otf", EchoFontFiles.refresh(context)?.extension, "the theme's font is the one drawn")
        val mine = assertNotNull(store.saveCurrentLook("Mine"))
        assertEquals(ThemeImage(byteArrayOf(7, 7, 7), "otf"), EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${mine.id}.echo-theme"))?.font)
        val before = assertNotNull(store.themes.value.firstOrNull { it.name == "Before Typed" })
        assertEquals(null, EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${before.id}.echo-theme"))?.font, "the folder's font stays the folder's")

        assertTrue(store.apply(plain.id))
        assertEquals(null, EchoFontFiles.theme(context), "a whole theme without a font clears the last theme's")
        assertEquals(folderFont, EchoFontFiles.refresh(context), "and the folder's font is drawn again")
        val saved = assertNotNull(store.saveCurrentLook("Folder"))
        assertEquals(ThemeImage(byteArrayOf(1, 2, 3), "ttf"), EchoThemeCodec.read(File(context.filesDir, "pfpthemes/${saved.id}.echo-theme"))?.font,
            "Save as Theme carries the font in use")
    }

    private fun theme(manifest: EchoThemeManifest, media: Map<String, ThemeImage>) =
        EchoThemeCodec.write(EchoThemeBundle(manifest, wallpaper = null, preview = null, media = media))

    private fun register(bytes: ByteArray): Uri {
        val uri = Uri.parse("content://test/${System.nanoTime()}.echo-theme")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return uri
    }

    private fun wav(): ByteArray {
        val data = ByteArray(2048)
        val out = java.io.ByteArrayOutputStream()
        fun int(v: Int) = repeat(4) { out.write((v shr (8 * it)) and 0xFF) }
        fun short(v: Int) = repeat(2) { out.write((v shr (8 * it)) and 0xFF) }
        out.write("RIFF".toByteArray()); int(36 + data.size); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); int(16); short(1); short(1); int(44_100); int(88_200); short(2); short(16)
        out.write("data".toByteArray()); int(data.size); out.write(data)
        return out.toByteArray()
    }
}
