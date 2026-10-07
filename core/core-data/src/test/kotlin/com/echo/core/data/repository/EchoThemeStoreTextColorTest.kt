package com.echo.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.datastore.echoDataStore
import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EchoThemeStoreTextColorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        runBlocking { context.echoDataStore.edit { it.clear() } }
        context.filesDir.resolve("themes").deleteRecursively()
    }

    @Test
    fun `a bundle carrying a text colour applies it`() = runTest {
        val store = EchoThemeStore(context, UiMediaStore(context))
        val saved = requireNotNull(store.importBundle(register(bundle(textColor = "#FF8800"))))

        assertTrue(store.apply(saved.id))
        assertEquals(0xFFFF8800L, context.echoDataStore.data.first()[KEY_TEXT_COLOR])
    }

    @Test
    fun `applying a bundle without a text colour removes the previous theme's`() = runTest {
        val store = EchoThemeStore(context, UiMediaStore(context))

        context.echoDataStore.edit { it[KEY_TEXT_COLOR] = 0xFFFF8800L }

        val saved = requireNotNull(store.importBundle(register(bundle())))
        assertTrue(store.apply(saved.id))

        assertNull(
            context.echoDataStore.data.first()[KEY_TEXT_COLOR],
            "a theme silent about text must not leave the last theme's colour behind",
        )
    }

    @Test
    fun `a malformed text colour is treated as auto rather than applied`() = runTest {
        val store = EchoThemeStore(context, UiMediaStore(context))
        context.echoDataStore.edit { it[KEY_TEXT_COLOR] = 0xFFFF8800L }

        val saved = requireNotNull(store.importBundle(register(bundle(textColor = "not-a-colour"))))
        assertTrue(store.apply(saved.id))

        assertNull(context.echoDataStore.data.first()[KEY_TEXT_COLOR])
    }

    @Test
    fun `resetting the applied theme clears the text colour`() = runTest {
        val store = EchoThemeStore(context, UiMediaStore(context))
        context.echoDataStore.edit { it[KEY_TEXT_COLOR] = 0xFFFF8800L }

        store.resetApplied()

        assertNull(context.echoDataStore.data.first()[KEY_TEXT_COLOR])
    }

    private fun bundle(textColor: String = EchoThemeManifest.ICON_COLOR_AUTO): ByteArray =
        EchoThemeCodec.write(
            EchoThemeBundle(
                manifest = EchoThemeManifest(
                    name = "Text Theme",
                    accentColor = "#0055AA",
                    textColor = textColor,
                ),
                wallpaper = null,
                preview = null,
            ),
        )

    private fun register(bytes: ByteArray): Uri {
        val uri = Uri.parse("content://test/${System.nanoTime()}.pfptheme")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return uri
    }

    private companion object {
        val KEY_TEXT_COLOR = longPreferencesKey("display_text_color")
    }
}
