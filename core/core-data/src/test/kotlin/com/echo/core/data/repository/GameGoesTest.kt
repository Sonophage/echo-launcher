package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.EchoDatabase
import com.echo.core.data.database.entity.GameEntity
import com.echo.core.data.datastore.echoDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// owner, 2026-10-10: a game that goes comes off Pinned and loses its Playing or Backlog mark; its play history stays
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GameGoesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, EchoDatabase::class.java).allowMainThreadQueries().build()
    private val repo = GameRepositoryImpl(context, db.gameDao(), db.playSessionDao(), db.platformDao())

    @Before fun clearPins() = runTest { context.echoDataStore.edit { it.remove(RecentPins.KEY) } }
    @After fun tearDown() = db.close()

    private suspend fun seed(romPath: String) = db.gameDao().upsert(
        GameEntity(
            title = romPath, platformId = "gb", romPath = romPath, packageName = null, emulatorPackage = null,
            artworkUri = null, logoUri = null, description = null, developer = null, publisher = null,
            releaseYear = null, genre = null, steamGridDbId = null, playState = "BACKLOG", lastPlayedAt = 5_000L,
        ),
    )

    private suspend fun pins() = RecentPins.parse(context.echoDataStore.data.first()[RecentPins.KEY])

    private suspend fun pin(vararg keys: String) = context.echoDataStore.edit { it[RecentPins.KEY] = keys.joinToString("\n") }

    @Test
    fun `a game whose file goes is unpinned and loses its mark, and keeps its history`() = runTest {
        val gone = seed("/roms/gb/gone.gb")
        val kept = seed("/roms/gb/kept.gb")
        pin(RecentPins.gameKey(gone), RecentPins.gameKey(kept), RecentPins.appKey("com.app"))

        repo.markMissing(listOf("/roms/gb/gone.gb"))

        assertEquals(listOf(RecentPins.gameKey(kept), RecentPins.appKey("com.app")), pins())
        val row = db.gameDao().getById(gone)!!
        assertNull(row.playState, "the Backlog mark stayed")
        assertEquals(5_000L, row.lastPlayedAt, "the play history was lost")
        assertEquals("BACKLOG", db.gameDao().getById(kept)!!.playState)
    }

    @Test
    fun `a deleted game is unpinned`() = runTest {
        val id = seed("/roms/gb/a.gb")
        pin(RecentPins.gameKey(id))
        repo.delete(id)
        assertEquals(emptyList(), pins())
    }

    @Test
    fun `Remove Missing unpins what it deletes`() = runTest {
        val id = seed("/roms/gb/a.gb")
        pin(RecentPins.gameKey(id))
        db.gameDao().markMissing(listOf("/roms/gb/a.gb"))
        pin(RecentPins.gameKey(id))
        repo.deleteMissing("gb")
        assertEquals(emptyList(), pins())
    }
}
