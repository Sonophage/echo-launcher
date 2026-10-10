package com.echo.feature.artwork.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.entity.GameEntity
import io.mockk.coEvery
import io.mockk.every
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

// seen on the Konker, 2026-10-10: Remove Missing deleted the newest game, and its art stayed, because the sweep
// spared every id above the highest one still in the library
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GoneGameArtworkSweepTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val root get() = File(context.filesDir, "artwork")

    @Before fun setUp() { root.deleteRecursively() }

    private fun art(gameId: Long) = File(File(root, "$gameId").apply { mkdirs() }, "boxart.jpg").apply { writeBytes(ByteArray(8)) }

    private fun game(id: Long) = mockk<GameEntity>(relaxed = true) { every { this@mockk.id } returns id }

    @Test
    fun `the newest game's art goes when it is deleted, and a game added during the sweep keeps its art`() = runTest {
        val gameDao = mockk<GameDao>(relaxed = true)
        coEvery { gameDao.lastIssuedId() } returns 168L
        coEvery { gameDao.getAll() } returns listOf(game(1L), game(167L))
        val references = mockk<ArtworkReferences> { coEvery { all() } returns emptySet() }
        val newest = art(168L)
        val live = art(167L)
        val addedDuring = art(169L)

        GoneGameArtworkSweep(gameDao, references, InternalArtworkStore(context, mockk(relaxed = true))).run()

        assertFalse(newest.exists(), "the deleted newest game's art was left behind")
        assertTrue(live.exists(), "a live game's art was deleted")
        assertTrue(addedDuring.exists(), "a game added during the sweep lost its art")
    }
}
