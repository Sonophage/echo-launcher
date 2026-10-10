package com.echo.feature.artwork.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.echo.feature.artwork.migrate.PortableArtworkImportWorker
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class InternalArtworkReaperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = InternalArtworkStore(context, mockk(relaxed = true))
    private val root get() = File(context.filesDir, "artwork")

    @Before
    fun setUp() {
        root.deleteRecursively()
    }

    private val kept = PortableArtworkImportWorker.KEPT_KINDS

    private suspend fun reap(referenced: Set<String> = emptySet(), live: Set<Long> = setOf(1L, 2L, 3L, 4L, 5L)) =
        store.reapUnreferenced(referenced = referenced, liveGameIds = live, keptKinds = kept)

    private fun write(gameId: Long, name: String, bytes: Int = 8): File =
        File(root, gameId.toString()).apply { mkdirs() }
            .let { File(it, name).apply { writeBytes(ByteArray(bytes)) } }

    @Test
    fun `a file something still points at is never deleted`() = runTest {
        val live = write(1L, "hero.jpg")
        val dead = write(1L, "boxart.jpg")

        val report = reap(referenced = setOf(live.absolutePath))

        assertTrue(live.exists(), "the reaper deleted a file the library still references")
        assertFalse(dead.exists())
        assertEquals(1, report.deleted)
    }

    @Test
    fun `a kind that still has a slot is never deleted, referenced or not`() = runTest {
        val screenshot = write(1L, "screenshot_01.jpg")
        val video = write(1L, "video.mp4")
        val manual = write(1L, "manual.pdf")
        val icon1 = write(1L, "icon1.mp4")

        val report = reap(referenced = emptySet())

        listOf(screenshot, video, manual, icon1).forEach {
            assertTrue(
                it.exists(),
                "screenshots, videos, manuals and tile videos are named by artwork_records and by " +
                    "no column on games. Reaping them for want of a column reference destroys " +
                    "every one of them the moment the record index is rebuilt.",
            )
        }
        assertEquals(0, report.deleted)
    }

    @Test
    fun `a kind with no slot left goes once nothing points at it`() = runTest {
        val dropped = listOf(
            write(1L, "hero.jpg"), write(1L, "boxart.jpg"),
            write(1L, "box3d.png"), write(1L, "physicalmedia.png"),
        )

        val report = reap(referenced = emptySet())

        dropped.forEach { assertFalse(it.exists()) }
        assertEquals(4, report.deleted)
    }

    @Test
    fun `a deleted game takes its whole folder, whatever the kinds`() = runTest {
        val orphanIcon = write(9L, "icon.jpg")
        val orphanShot = write(9L, "screenshot.jpg")
        val livingIcon = write(1L, "icon.jpg")

        val report = reap(referenced = emptySet(), live = setOf(1L))

        assertFalse(orphanIcon.exists())
        assertFalse(orphanShot.exists())
        assertTrue(livingIcon.exists())
        assertEquals(2, report.deleted)
    }

    @Test
    fun `a file that is not artwork is left alone even though nothing points at it`() = runTest {
        val stranger = write(3L, "notes.txt")
        val dead = write(3L, "boxart.jpg")

        val report = reap(referenced = emptySet())

        assertTrue(
            stranger.exists(),
            "the reaper only knows game artwork; anything else in the folder is not its to delete",
        )
        assertFalse(dead.exists())
        assertEquals(1, report.deleted)
    }

    @Test
    fun `a directory that is not a game id is skipped whole`() = runTest {
        val outside = File(root, "wallpaper").apply { mkdirs() }
            .let { File(it, "hero.jpg").apply { writeBytes(ByteArray(8)) } }

        reap(referenced = emptySet())

        assertTrue(outside.exists(), "only numbered game folders belong to the artwork store")
    }

    @Test
    fun `an emptied game folder goes with its last file`() = runTest {
        write(4L, "hero.jpg")

        reap(referenced = emptySet())

        assertFalse(File(root, "4").exists())
    }

    @Test
    fun `the report counts the bytes it reclaimed`() = runTest {
        write(5L, "hero.jpg", bytes = 100)
        write(5L, "boxart.jpg", bytes = 40)

        val report = reap(referenced = emptySet())

        assertEquals(2, report.deleted)
        assertEquals(140L, report.bytes)
    }

    // owner, 2026-10-10: a game taken out of the library takes its art with it; a game added after the library was
    // read may already have art saved, and keeps it
    @Test
    fun `with every kind kept, a removed game's art goes and a live or newer game's stays`() = runTest {
        val gone = write(2L, "boxart.jpg")
        val live = write(3L, "boxart.jpg")
        val newer = write(9L, "boxart.jpg")

        store.reapUnreferenced(
            referenced = emptySet(), liveGameIds = setOf(1L, 3L), keptKinds = ArtworkKind.entries.toSet(), spareAbove = 3L,
        )

        assertFalse(gone.exists(), "a removed game's art was left behind")
        assertTrue(live.exists(), "a live game's art was deleted")
        assertTrue(newer.exists(), "a game newer than the read lost its art")
    }
}
