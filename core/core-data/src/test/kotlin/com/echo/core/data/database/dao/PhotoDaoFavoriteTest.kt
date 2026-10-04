package com.echo.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.EchoDatabase
import com.echo.core.data.database.entity.PhotoEntity
import com.echo.core.data.database.entity.PhotoLibraryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PhotoDaoFavoriteTest {
    private lateinit var db: EchoDatabase
    private lateinit var dao: PhotoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EchoDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.photoDao()
    }

    @After
    fun tearDown() = db.close()

    private fun photo(id: String) = PhotoEntity(id = id, libraryId = "lib", uri = "content://$id", displayName = "$id.jpg")

    @Test
    fun `a rescan keeps the photos you favourited`() = runTest {
        db.photoLibraryDao().upsert(PhotoLibraryEntity(id = "lib", displayName = "Pictures", treeUri = "content://tree", createdAt = 1, updatedAt = 1))
        dao.insertAll(listOf(photo("a"), photo("b")))
        dao.setFavorite("a", true)

        dao.replaceForLibrary("lib", listOf(photo("a"), photo("b"), photo("c")))

        assertEquals("the scanner rebuilds rows without the flag; the replace must carry it", listOf("a"),
            dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun `a photo that left the folder takes its favourite with it`() = runTest {
        db.photoLibraryDao().upsert(PhotoLibraryEntity(id = "lib", displayName = "Pictures", treeUri = "content://tree", createdAt = 1, updatedAt = 1))
        dao.insertAll(listOf(photo("a")))
        dao.setFavorite("a", true)

        dao.replaceForLibrary("lib", listOf(photo("b")))

        assertEquals(emptyList<String>(), dao.observeFavorites().first().map { it.id })
    }
}
