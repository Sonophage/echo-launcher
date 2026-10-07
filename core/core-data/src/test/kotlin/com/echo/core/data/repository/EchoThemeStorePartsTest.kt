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
