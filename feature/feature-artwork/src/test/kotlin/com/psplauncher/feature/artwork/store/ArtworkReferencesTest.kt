package com.psplauncher.feature.artwork.store

import com.psplauncher.core.data.database.dao.ArtworkRecordDao
import com.psplauncher.core.data.database.dao.CategoryDao
import com.psplauncher.core.data.database.dao.GameDao
import com.psplauncher.core.data.database.entity.ArtworkRecordEntity
import com.psplauncher.core.data.database.entity.CategoryEntity
import com.psplauncher.core.data.database.entity.GameEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ArtworkReferencesTest {
    private val gameDao: GameDao = mockk()
    private val artworkRecordDao: ArtworkRecordDao = mockk()
    private val categoryDao: CategoryDao = mockk()
    private val references = ArtworkReferences(gameDao, artworkRecordDao, categoryDao)

    private fun game(artwork: String? = null, icon: String? = null, logo: String? = null) = GameEntity(
        id = 1L, title = "Game", platformId = "ps2", romPath = null,
        packageName = null, emulatorPackage = null,
        artworkUri = artwork, logoUri = logo, iconUri = icon,
        description = null, developer = null, publisher = null,
        releaseYear = null, genre = null, steamGridDbId = null, createdAt = 0L,
    )

    private fun record(uri: String, prev: String? = null) = ArtworkRecordEntity(
        gameId = 1L, platformId = "ps2", artworkType = ArtworkKind.SCREENSHOT.name, sortOrder = 0,
        portableName = "Game", relativePath = uri, documentUri = uri, source = "scrape",
        sizeBytes = 1, userAssigned = false, locked = false, prevDocumentUri = prev,
        prevSizeBytes = 0, hasOriginal = false, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `an artwork record counts as a reference, because some kinds have no column`() = runTest {
        val screenshot = "/data/user/0/app/files/artwork/1/screenshot.jpg"
        coEvery { gameDao.getAll() } returns listOf(game())
        coEvery { artworkRecordDao.getAll() } returns listOf(record(screenshot))
        coEvery { categoryDao.getAll() } returns emptyList()

        assertTrue(
            screenshot in references.all(),
            "screenshots, videos, manuals and tile videos are named only by artwork_records. If " +
                "this set does not read them, a reaper run treats every one as garbage.",
        )
    }

    @Test
    fun `all three game columns are read`() = runTest {
        coEvery { gameDao.getAll() } returns listOf(game(artwork = "/a", icon = "/i", logo = "/l"))
        coEvery { artworkRecordDao.getAll() } returns emptyList()
        coEvery { categoryDao.getAll() } returns emptyList()

        val all = references.all()
        listOf("/a", "/i", "/l").forEach { assertTrue(it in all, "$it is a live art reference") }
    }

    @Test
    fun `a superseded file is still referenced while the record remembers it`() = runTest {
        coEvery { gameDao.getAll() } returns listOf(game())
        coEvery { artworkRecordDao.getAll() } returns listOf(record("/new", prev = "/old"))
        coEvery { categoryDao.getAll() } returns emptyList()

        assertTrue("/old" in references.all(), "prev_document_uri is how a restore finds its way back")
    }

    @Test
    fun `a custom column icon is a reference too`() = runTest {
        coEvery { gameDao.getAll() } returns emptyList()
        coEvery { artworkRecordDao.getAll() } returns emptyList()
        coEvery { categoryDao.getAll() } returns listOf(
            CategoryEntity(id = "c1", name = "Fav", iconKey = "star", type = "BUILTIN", position = 0,
                customIconUri = "/data/custom.png"),
        )

        assertTrue("/data/custom.png" in references.all())
    }
}
