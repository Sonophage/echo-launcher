package com.echo.feature.artwork.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeleteUnderRootTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = InternalArtworkStore(context, mockk(relaxed = true))
    private val root get() = File(context.filesDir, "artwork")

    @Before
    fun setUp() {
        root.deleteRecursively()
    }

    private fun artworkFile(gameId: Long, name: String) =
        File(root, gameId.toString()).apply { mkdirs() }
            .let { File(it, name).apply { writeBytes(ByteArray(8)) } }

    @Test
    fun `an artwork file is removed so dropping its record does not strand it`() = runTest {
        val f = artworkFile(1L, "screenshot_01.jpg")

        assertTrue(store.deleteUnderRoot(f.absolutePath))
        assertFalse(f.exists())
    }

    @Test
    fun `a path outside the artwork root is refused`() = runTest {
        val outsider = File(context.filesDir, "wallpaper").apply { mkdirs() }
            .let { File(it, "user.jpg").apply { writeBytes(ByteArray(8)) } }

        assertFalse(
            store.deleteUnderRoot(outsider.absolutePath),
            "a malformed record must never be able to delete outside the artwork store",
        )
        assertTrue(outsider.exists())
    }

    @Test
    fun `a traversal out of the root is refused`() = runTest {
        val outsider = File(context.filesDir, "wallpaper").apply { mkdirs() }
            .let { File(it, "user.jpg").apply { writeBytes(ByteArray(8)) } }

        assertFalse(store.deleteUnderRoot("${root.absolutePath}/../wallpaper/user.jpg"))
        assertTrue(outsider.exists())
    }

    @Test
    fun `a directory is not a file and is refused`() = runTest {
        val dir = File(root, "5").apply { mkdirs() }

        assertFalse(store.deleteUnderRoot(dir.absolutePath))
        assertTrue(dir.exists())
    }

    @Test
    fun `a content uri is not a path and is refused`() = runTest {
        assertFalse(store.deleteUnderRoot("content://com.android.externalstorage.documents/tree/x"))
    }
}
