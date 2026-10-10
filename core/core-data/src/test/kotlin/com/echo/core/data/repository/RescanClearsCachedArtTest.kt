package com.echo.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.EchoDatabase
import com.echo.core.domain.model.MusicTrack
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// owner, 2026-10-10: a rescan clears out the cached art of what is no longer in the folder
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RescanClearsCachedArtTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, EchoDatabase::class.java).allowMainThreadQueries().build()
    private val repo = MusicRepositoryImpl(context, db.musicFolderDao(), db.musicTrackDao(), db.playlistDao())

    @After fun tearDown() = db.close()

    private fun cached(name: String) = File(File(context.filesDir, "music_art").apply { mkdirs() }, name).apply { writeBytes(ByteArray(8)) }

    private fun track(id: String, folderId: String, art: File) =
        MusicTrack(id = id, folderId = folderId, uri = "content://$id", displayName = "$id.mp3", artUri = Uri.fromFile(art).toString())

    @Test
    fun `a rescan deletes the art of an album that is gone and keeps the art still in use`() = runTest {
        val folder = repo.addFolder("Music", "content://tree/m")
        val kept = cached("kept.img")
        val gone = cached("gone.img")
        repo.replaceTracksForFolder(folder.id, listOf(track("a", folder.id, kept), track("b", folder.id, gone)), 1L)

        repo.replaceTracksForFolder(folder.id, listOf(track("a", folder.id, kept)), 2L)

        assertTrue(kept.exists(), "art a track still uses was deleted")
        assertFalse(gone.exists(), "the art of a track no longer there was left behind")
    }

    // the owner's own files are never the app's to delete, whatever a row points at
    @Test
    fun `a file outside the app's own folders is never deleted`() = runTest {
        val outside = File(Files.createTempDirectory("owner-media").toFile(), "cover.jpg").apply { writeBytes(ByteArray(8)) }

        deleteOrphanedThumbnails(context, listOf(Uri.fromFile(outside).toString())) { false }

        assertTrue(outside.exists(), "a file outside the app's folders was deleted")
    }
}
