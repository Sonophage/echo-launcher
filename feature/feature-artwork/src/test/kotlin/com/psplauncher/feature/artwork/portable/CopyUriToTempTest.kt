package com.psplauncher.feature.artwork.portable

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CopyUriToTempTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val library = PortableArtworkLibrary(context)
    private val payload = ByteArray(64) { 7 }

    private fun sourceFile(): File =
        File.createTempFile("source_", ".jpg", context.cacheDir).apply { writeBytes(payload) }

    @Test
    fun `a record that names a bare filesystem path is readable`() {
        val src = sourceFile()

        val stream = library.openSource(Uri.parse(src.absolutePath))

        assertNotNull(
            stream,
            "artwork_records name /data paths once artwork lives in app storage, and " +
                "ContentResolver.openInputStream rejects a uri with no scheme. Without this the " +
                "version and original backups silently stop happening.",
        )
        assertContentEquals(payload, stream.use { it.readBytes() })
    }

    @Test
    fun `a file uri is readable`() {
        val src = sourceFile()

        val stream = library.openSource(Uri.fromFile(src))

        assertNotNull(stream)
        assertContentEquals(payload, stream.use { it.readBytes() })
    }

    @Test
    fun `a local path that is not there reads as absent rather than throwing`() {
        assertNull(library.openSource(Uri.parse("/data/does/not/exist.jpg")))
        assertNull(library.openSource(Uri.fromFile(File(context.cacheDir, "missing.jpg"))))
    }

    @Test
    fun `an empty file is not offered as a source`() {
        val empty = File.createTempFile("empty_", ".jpg", context.cacheDir)

        assertNull(library.openSource(Uri.parse(empty.absolutePath)))
    }
}
