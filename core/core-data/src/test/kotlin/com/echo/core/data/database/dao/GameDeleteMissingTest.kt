package com.echo.core.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.EchoDatabase
import com.echo.core.data.database.entity.GameEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-10: Remove Missing deletes the games a scan hid, on that console only
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GameDeleteMissingTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        EchoDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val games = db.gameDao()

    @After fun tearDown() = db.close()

    private suspend fun seed(platformId: String, romPath: String) = games.upsert(
        GameEntity(
            title = romPath, platformId = platformId, romPath = romPath, packageName = null, emulatorPackage = null,
            artworkUri = null, logoUri = null, description = null, developer = null, publisher = null,
            releaseYear = null, genre = null, steamGridDbId = null,
        ),
    )

    @Test
    fun `only the console's hidden games are deleted`() = runTest {
        seed("snes", "/roms/snes/here.sfc")
        seed("snes", "/roms/snes/gone.sfc")
        seed("gba", "/roms/gba/gone.gba")
        games.markMissing(listOf("/roms/snes/gone.sfc", "/roms/gba/gone.gba"))

        assertEquals(1, games.deleteMissing("snes"))

        assertEquals(listOf("/roms/gba/gone.gba", "/roms/snes/here.sfc"), games.getAll().map { it.romPath }.sortedBy { it })
    }
}
