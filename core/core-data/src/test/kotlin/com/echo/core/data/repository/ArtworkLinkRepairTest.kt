package com.echo.core.data.repository

import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.entity.GameEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ArtworkLinkRepairTest {
    private val gameDao: GameDao = mockk(relaxed = true)
    private val artworkAccent: ArtworkAccent = mockk {
        coEvery { isReadable(any()) } returns false
    }
    private val folderRepository: ArtworkFolderRepository = mockk {
        coEvery { getTreeUri() } returns TREE
        coEvery { hasLiveGrant() } returns false
    }
    private val repair = ArtworkLinkRepair(gameDao, artworkAccent, folderRepository)

    @Test
    fun `a lost artwork folder grant must not clear any game's background`() = runTest {
        coEvery { gameDao.getAll() } returns listOf(
            game(1, artwork = "$TREE/document/fanart1.png", icon = "$TREE/document/icon1.png"),
            game(2, artwork = "$TREE/document/fanart2.png", icon = null),
        )

        val report = repair.run()

        coVerify(exactly = 0) { gameDao.updateArtwork(any(), any()) }
        assertTrue(report.folderAccessLost)
    }

    @Test
    fun `a live grant still clears a background that is really gone`() = runTest {
        coEvery { folderRepository.hasLiveGrant() } returns true
        coEvery { gameDao.getAll() } returns listOf(game(1, artwork = "$TREE/document/gone.png", icon = null))

        repair.run()

        coVerify { gameDao.updateArtwork(1, null) }
    }

    private fun game(id: Long, artwork: String, icon: String?) = GameEntity(
        id = id,
        title = "Game $id",
        platformId = "psp",
        romPath = null,
        packageName = null,
        emulatorPackage = null,
        artworkUri = artwork,
        iconUri = icon,
        logoUri = null,
        description = null,
        developer = null,
        publisher = null,
        releaseYear = null,
        genre = null,
        steamGridDbId = null,
    )

    private companion object {
        const val TREE = "content://com.android.externalstorage.documents/tree/primary%3AArtwork"
    }
}
