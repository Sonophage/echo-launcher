package com.echo.feature.artwork.api

import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.database.entity.ArtworkRecordEntity
import com.echo.feature.artwork.store.ArtworkKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PortableArtworkCountTest {
    private val artworkRecordDao: ArtworkRecordDao = mockk()
    private val gameDao: com.echo.core.data.database.dao.GameDao = mockk(relaxed = true)

    private fun record(type: ArtworkKind) = ArtworkRecordEntity(
        gameId = 1L,
        platformId = "ps2",
        artworkType = type.name,
        sortOrder = 0,
        portableName = "Game",
        relativePath = "Artwork/ps2/x/Game.png",
        documentUri = "content://tree/Game-${type.name}.png",
        source = "scrape",
        sizeBytes = 1,
        userAssigned = false,
        locked = false,
        prevSizeBytes = 0,
        hasOriginal = false,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun manager() = ArtworkImportManager(
        context = mockk(relaxed = true),
        folderRepository = mockk(relaxed = true),
        library = mockk(relaxed = true),
        planner = mockk(relaxed = true),
        reportDao = mockk(relaxed = true),
        gameDao = gameDao,
        artworkRecordDao = artworkRecordDao,
        artworkStore = mockk(relaxed = true),
        internalStore = mockk(relaxed = true),
        identityRecorder = mockk(relaxed = true),
        linkRepoint = mockk(relaxed = true),
    )

    @Test
    fun `the count offered before a copy is what the copy will actually bring across`() = runTest {
        coEvery { artworkRecordDao.getAll() } returns listOf(
            record(ArtworkKind.ICON),
            record(ArtworkKind.BACKGROUND),
            record(ArtworkKind.LOGO),
            record(ArtworkKind.HERO),
            record(ArtworkKind.BOX_ART),
            record(ArtworkKind.BOX_3D),
            record(ArtworkKind.PHYSICAL_MEDIA),
        )

        assertEquals(
            3,
            manager().portableArtworkCount(),
            "this number is what the user reads before deciding to delete the folder the files " +
                "came from; counting rows the importer will skip overstates it by the kinds that " +
                "no longer have a slot",
        )
    }

    @Test
    fun `a dropped kind still counts when a game column names its file`() = runTest {
        val heroFile = "content://tree/miximages/Crash.png"
        coEvery { gameDao.getAll() } returns listOf(
            com.echo.core.data.database.entity.GameEntity(
                id = 1L, title = "Crash", platformId = "ps2", romPath = null,
                packageName = null, emulatorPackage = null,
                artworkUri = heroFile, logoUri = null, iconUri = null,
                description = null, developer = null, publisher = null,
                releaseYear = null, genre = null, steamGridDbId = null, createdAt = 0L,
            )
        )
        coEvery { artworkRecordDao.getAll() } returns listOf(
            record(ArtworkKind.HERO).copy(documentUri = heroFile),
            record(ArtworkKind.BOX_ART),
        )

        assertEquals(
            1,
            manager().portableArtworkCount(),
            "the offered number must match the copy set exactly, including the hero files that " +
                "are doing a background's job",
        )
    }

    @Test
    fun `a record whose type is not an ArtworkKind at all is not counted`() = runTest {
        coEvery { artworkRecordDao.getAll() } returns listOf(
            record(ArtworkKind.ICON).copy(artworkType = "TITLESCREEN"),
            record(ArtworkKind.ICON).copy(artworkType = "NOT_A_KIND"),
            record(ArtworkKind.ICON),
        )

        assertEquals(1, manager().portableArtworkCount())
    }
}
