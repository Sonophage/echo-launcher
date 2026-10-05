package com.echo.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.entity.BookEntity
import com.echo.core.data.database.entity.BookLibraryEntity
import com.echo.core.data.database.entity.PhotoEntity
import com.echo.core.data.database.entity.PhotoLibraryEntity
import com.echo.core.data.database.entity.VideoEntity
import com.echo.core.data.database.entity.VideoLibraryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-05: removing one video, photo or book left "Videos 12" on the crossbar, because the
// library's stored count only changed on a rescan
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LibraryRecountTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EchoDatabase::class.java)
        .allowMainThreadQueries().build()

    @After fun tearDown() = db.close()

    @Test
    fun `a recount after one removal matches what is left, for videos, photos and books`() = runTest {
        db.videoLibraryDao().upsert(VideoLibraryEntity(id = "v", displayName = "V", treeUri = "t/v", createdAt = 1, updatedAt = 1))
        db.videoDao().insertAll(listOf("a", "b").map { VideoEntity(id = it, libraryId = "v", uri = "t/v/$it", displayName = it) })
        // another library's video must not be counted
        db.videoLibraryDao().upsert(VideoLibraryEntity(id = "w", displayName = "W", treeUri = "t/w", createdAt = 1, updatedAt = 1))
        db.videoDao().insertAll(listOf(VideoEntity(id = "z", libraryId = "w", uri = "t/w/z", displayName = "z")))
        db.videoLibraryDao().updateScanResult("v", 2, 1)
        db.videoDao().deleteById("a")
        db.videoLibraryDao().recount("v")
        assertEquals(1, db.videoLibraryDao().getById("v")!!.videoCount)

        db.photoLibraryDao().upsert(PhotoLibraryEntity(id = "p", displayName = "P", treeUri = "t/p", createdAt = 1, updatedAt = 1))
        db.photoDao().insertAll(listOf("a", "b").map { PhotoEntity(id = it, libraryId = "p", uri = "t/p/$it", displayName = it) })
        db.photoLibraryDao().updateScanResult("p", 2, 1)
        db.photoDao().deleteById("a")
        db.photoLibraryDao().recount("p")
        assertEquals(1, db.photoLibraryDao().getById("p")!!.photoCount)

        db.bookLibraryDao().upsert(BookLibraryEntity(id = "b", displayName = "B", treeUri = "t/b", createdAt = 1, updatedAt = 1))
        db.bookDao().insertAll(listOf("a", "b").map { BookEntity(id = it, libraryId = "b", uri = "t/b/$it", displayName = it) })
        db.bookLibraryDao().updateScanResult("b", 2, 1)
        db.bookDao().deleteById("a")
        db.bookLibraryDao().recount("b")
        assertEquals(1, db.bookLibraryDao().getById("b")!!.bookCount)
    }
}
