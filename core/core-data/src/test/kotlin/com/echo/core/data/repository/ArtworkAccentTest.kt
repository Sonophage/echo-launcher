package com.echo.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtworkAccentTest {
    @get:Rule val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val accent = ArtworkAccent(context)

    @Test
    fun `a file that could not be read is read again once it is there`() = runTest {
        val file = File(tmp.root, "fanart.png")
        assertNull(accent.resolve(file.path), "precondition: a missing file resolves to nothing")

        file.writePng(RED)

        assertEquals(file.path, accent.resolve(file.path)?.uri)
    }

    @Test
    fun `a replaced file keeps its old accent until it is forgotten`() = runTest {
        val file = File(tmp.root, "fanart.png").apply { writePng(RED) }
        val red = assertNotNull(accent.resolve(file.path)?.accent)

        file.writePng(BLUE)
        assertEquals(red, accent.resolve(file.path)?.accent, "precondition: the accent is cached")

        accent.forget(listOf(file.path))

        assertNotEquals(red, accent.resolve(file.path)?.accent)
    }

    @Test
    fun `forgetAll re-reads every file`() = runTest {
        val file = File(tmp.root, "fanart.png").apply { writePng(RED) }
        val red = accent.resolve(file.path)?.accent
        file.writePng(BLUE)

        accent.forgetAll()

        assertNotEquals(red, accent.resolve(file.path)?.accent)
    }

    private fun File.writePng(color: Int) {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val RED = 0xFFD02020.toInt()
        const val BLUE = 0xFF2040D0.toInt()
    }
}
