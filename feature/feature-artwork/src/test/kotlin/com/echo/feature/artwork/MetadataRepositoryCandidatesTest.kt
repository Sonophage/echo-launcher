package com.echo.feature.artwork

import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.entity.GameEntity
import com.echo.feature.artwork.api.IgdbApi
import com.echo.feature.artwork.api.IgdbGameInfo
import com.echo.feature.artwork.api.ScrapeOptions
import com.echo.feature.artwork.api.SsGameInfo
import com.echo.feature.artwork.api.SsLookupResult
import com.echo.feature.artwork.api.SsLookupDiagnostics
import com.echo.feature.artwork.rom.RomHasher
import com.echo.feature.artwork.rom.RomIdentity
import com.echo.feature.artwork.api.ScreenScraperApi
import com.echo.core.data.steamgriddb.SgdbApiKeyProvider
import com.echo.core.data.steamgriddb.SteamGridDbApi
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.artwork.store.ArtworkStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataRepositoryCandidatesTest {
    private val gameDao = mockk<GameDao>(relaxed = true)
    private val screenScraper = mockk<ScreenScraperApi>(relaxed = true)
    private val steamGridDb = mockk<SteamGridDbApi>(relaxed = true)
    private val igdbApi = mockk<IgdbApi>(relaxed = true)
    private val sgdbKeyProvider = mockk<SgdbApiKeyProvider>(relaxed = true)
    private val artworkStore = mockk<ArtworkStore>(relaxed = true)

    private val romHasher = mockk<RomHasher> {
        coEvery { identify(any(), any()) } returns RomIdentity(crc32 = null, sizeBytes = null, fileName = null)
    }

    private val repo = MetadataRepository(
        context = mockk(relaxed = true),
        gameDao = gameDao,
        screenScraper = screenScraper,
        romHasher = romHasher,
        steamGridDb = steamGridDb,
        steamStoreApi = mockk(relaxed = true),
        igdbApi = igdbApi,
        sgdbKeyProvider = sgdbKeyProvider,
        imageLoader = mockk(relaxed = true),
        artworkStore = artworkStore,
        httpClient = mockk(relaxed = true),
        videoSnapTranscoder = mockk(relaxed = true),
        ssMediaCacheDao = mockk(relaxed = true),
    )

    private val ssHit = SsGameInfo(
        ssId = 7L,
        title = "Scraped Title",
        description = "A description",
        developer = null,
        publisher = null,
        releaseYear = 1994,
        genre = null,
        players = null,
        ageRating = null,
        franchise = null,
        communityRating = null,
        releaseDate = null,
        artworkUrl = null,
        boxArtUrl = null,
        box3dUrl = null,
        physicalMediaUrl = null,
        screenshotUrl = null,
        heroUrl = null,
        logoUrl = null,
        manualUrl = null,
        videoUrl = null,
        videoRawUrl = null,
    )

    private fun ssReturns(info: SsGameInfo?) {
        coEvery { screenScraper.fetchGameInfo(any(), any(), any()) } returns SsLookupResult(
            info = info,
            diagnostics = SsLookupDiagnostics(
                fileName = null,
                platformId = "snes",
                systemId = null,
                userCredentialsPresent = false,
                sentCrc = false,
            ),
        )
    }

    private fun givenGame(userTitleOverride: String? = null, iconUri: String? = null) {
        coEvery { gameDao.getById(1L) } returns GameEntity(
            id = 1L,
            title = "raw_rom_name",
            platformId = "snes",
            romPath = null,
            packageName = null,
            emulatorPackage = null,
            artworkUri = null,
            logoUri = null,
            description = null,
            developer = null,
            publisher = null,
            releaseYear = null,
            genre = null,
            steamGridDbId = null,
            userTitleOverride = userTitleOverride,
            iconUri = iconUri,
        )
        coEvery { screenScraper.isEnabled() } returns true
        coEvery { sgdbKeyProvider.getKey() } returns null
    }

    @Test
    fun `fetchCandidates writes no game column and saves no artwork`() = runTest {
        givenGame()
        ssReturns(ssHit)
        coEvery { igdbApi.hasCredentials() } returns true
        coEvery { igdbApi.fetchGameInfo(any(), any()) } returns
            IgdbGameInfo(artworkUrl = "https://igdb/cover.jpg", heroUrl = null, logoUrl = null)

        val candidates = repo.fetchCandidates(1L, "raw_rom_name", "snes", romPath = null)

        assertEquals(ssHit, candidates.ssInfo)
        assertEquals("https://igdb/cover.jpg", candidates.igdbInfo?.artworkUrl)
        assertFalse(candidates.isEmpty)

        coVerify(exactly = 1) { gameDao.getById(1L) }
        confirmVerified(gameDao)
        confirmVerified(artworkStore)
    }

    @Test
    fun `fetchCandidates searches by the user's title override, not the raw title`() = runTest {
        givenGame(userTitleOverride = "Chrono Trigger")

        coEvery { igdbApi.hasCredentials() } returns true
        ssReturns(null)

        val candidates = repo.fetchCandidates(1L, "raw_rom_name", "snes", romPath = null)

        assertEquals("Chrono Trigger", candidates.bestTitle)

        coVerify { igdbApi.fetchGameInfo("snes", "Chrono Trigger") }
    }

    @Test
    fun `metadata-only retrieval never asks the artwork-only providers`() = runTest {
        givenGame()
        coEvery { igdbApi.hasCredentials() } returns true
        coEvery { sgdbKeyProvider.getKey() } returns "sgdb-key"

        val candidates = repo.fetchCandidates(
            1L, "raw_rom_name", "snes", romPath = null,
            options = ScrapeOptions(metadataOnly = true),
        )

        assertNull(candidates.igdbInfo)
        assertNull(candidates.sgdbGameId)
        coVerify(exactly = 0) { igdbApi.fetchGameInfo(any(), any()) }
        coVerify(exactly = 0) { steamGridDb.searchGame(any()) }
    }

    @Test
    fun `nothing found is empty candidates, and fetchForGame still writes nothing`() = runTest {
        givenGame()
        ssReturns(null)

        assertTrue(repo.fetchCandidates(1L, "raw_rom_name", "snes", romPath = null).isEmpty)

        val result = repo.fetchForGame(1L, "raw_rom_name", "snes", romPath = null)

        assertFalse(result.success)
        assertEquals("none", result.source)
        coVerify(exactly = 2) { gameDao.getById(1L) }
        confirmVerified(gameDao)
        confirmVerified(artworkStore)
    }

    @Test
    fun `fetchForGame still persists the winners through the COALESCE write`() = runTest {
        givenGame()
        ssReturns(ssHit)

        val result = repo.fetchForGame(
            1L, "raw_rom_name", "snes", romPath = null,
            options = ScrapeOptions(metadataOnly = true),
        )

        assertTrue(result.success)
        assertEquals("screenscraper", result.source)
        assertEquals("Scraped Title", result.scrapedTitle)
        coVerify(exactly = 1) {
            gameDao.updateMetadata(
                id = 1L,
                description = "A description",
                developer = null,
                publisher = null,
                releaseYear = 1994,
                genre = null,
                artworkUri = null,
                logoUri = null,
                iconUri = null,
                scrapedTitle = null,
                players = null,
                ageRating = null,
                franchise = null,
                communityRating = null,
                releaseDate = null,

                ssId = 7L,
                igdbId = null,
                steamGridDbId = null,
                romCrc32 = null,
            )
        }
    }

    @Test
    fun `a scrape fills the title through the fill-only write`() = runTest {
        givenGame()
        ssReturns(ssHit)

        repo.fetchForGame(
            1L, "raw_rom_name", "snes", romPath = null,
            options = ScrapeOptions(metadataOnly = true),
        )

        coVerify(exactly = 1) { gameDao.fillScrapedTitleIfMissing(1L, "Scraped Title") }
    }

    @Test
    fun `a scrape does not touch the title of a game the user has named`() = runTest {
        givenGame(userTitleOverride = "The Name I Chose")
        ssReturns(ssHit)

        repo.fetchForGame(
            1L, "raw_rom_name", "snes", romPath = null,
            options = ScrapeOptions(metadataOnly = true),
        )

        coVerify(exactly = 0) { gameDao.fillScrapedTitleIfMissing(any(), any()) }
    }

    private val ssWithCover = ssHit.copy(boxArtUrl = "https://ss/box2d.png", heroUrl = "https://ss/wide-hero.jpg")

    @Test
    fun `the refetch puts the upright cover in the icon slot, because the tile shows the icon slot`() = runTest {
        givenGame()
        ssReturns(ssWithCover)
        coEvery { artworkStore.saveFromUrl(1L, ArtworkKind.ICON, "https://ss/box2d.png", any()) } returns "content://icon0/cover"

        repo.fetchForGame(1L, "raw_rom_name", "snes", romPath = null)

        coVerify(exactly = 1) { gameDao.updateIconUri(1L, "content://icon0/cover") }
        coVerify(exactly = 0) { artworkStore.saveFromUrl(1L, ArtworkKind.ICON, "https://ss/wide-hero.jpg", any()) }
    }

    @Test
    fun `the refetch leaves a working icon the user already has`() = runTest {
        givenGame(iconUri = "content://icon0/mine")
        ssReturns(ssWithCover)
        coEvery { artworkStore.isValidRef("content://icon0/mine") } returns true
        coEvery { artworkStore.saveFromUrl(1L, ArtworkKind.ICON, any(), any()) } returns "content://icon0/cover"

        repo.fetchForGame(1L, "raw_rom_name", "snes", romPath = null)

        coVerify(exactly = 0) { gameDao.updateIconUri(any(), any()) }
    }
}
