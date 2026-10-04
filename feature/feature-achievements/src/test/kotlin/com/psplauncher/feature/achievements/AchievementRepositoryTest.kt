package com.psplauncher.feature.achievements

import com.psplauncher.core.data.achievement.AchievementCredentialsProvider
import com.psplauncher.core.data.database.dao.AccountAchievementDao
import com.psplauncher.core.data.database.dao.AccountAchievementSetDao
import com.psplauncher.core.data.database.dao.AchievementSetRow
import com.psplauncher.core.data.database.dao.ProviderGameLinkDao
import com.psplauncher.core.data.database.entity.AccountAchievementEntity
import com.psplauncher.core.data.database.entity.AccountAchievementSetEntity
import com.psplauncher.core.data.database.entity.ProviderGameLinkEntity
import com.psplauncher.core.domain.achievement.AchievementProvider
import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.provider.steam.SteamAppListResolver
import com.psplauncher.feature.achievements.api.SyncedCoin
import com.psplauncher.feature.achievements.provider.RemoteAchievementSources
import com.psplauncher.feature.achievements.provider.retro.RetroAchievementsSource
import com.psplauncher.feature.achievements.provider.steam.SteamAchievementsSource
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AchievementRepositoryTest {
    private val retroSource = mockk<RetroAchievementsSource>()
    private val steamSource = mockk<SteamAchievementsSource>()
    private val remoteSources = RemoteAchievementSources(retroSource, steamSource)
    private val credentials = mockk<AchievementCredentialsProvider>(relaxed = true)
    private val setDao = mockk<AccountAchievementSetDao>(relaxed = true)
    private val coinDao = mockk<AccountAchievementDao>(relaxed = true)
    private val linkDao = mockk<ProviderGameLinkDao>(relaxed = true)
    private val steamResolver = mockk<SteamAppListResolver>(relaxed = true)
    private val gameRepository = mockk<com.psplauncher.core.domain.repository.GameRepository>(relaxed = true)
    private val matchNoteDao = mockk<com.psplauncher.core.data.database.dao.AchievementMatchNoteDao>(relaxed = true)

    private val repo = AchievementRepository(remoteSources, credentials, setDao, coinDao, linkDao, matchNoteDao, steamResolver, gameRepository)

    init {
        coEvery { setDao.getSet(any(), any()) } returns null
        coEvery { coinDao.getForSet(any(), any()) } returns emptyList()
        coEvery { gameRepository.getById(any()) } returns null
        coEvery { setDao.getAllSets() } returns emptyList()
    }

    private fun coin(id: String, earned: Boolean) =
        SyncedCoin(
            id, id, "", 0.0, null,
            isHidden = false, isEarned = earned,
            earnedAt = if (earned) 1L else null,
        )

    private fun setEntity(
        provider: String,
        providerGameId: String,
        lastSyncedAt: Long? = null,
    ) = AccountAchievementSetEntity(
        provider = provider, providerGameId = providerGameId, title = "Some Game",
        lastSyncedAt = lastSyncedAt,
    )

    @Test
    fun `a game's set reaches the UI with its counts, and an unknown provider is dropped`() = runTest {
        val row = AchievementSetRow(
            provider = "RETRO_ACHIEVEMENTS", providerGameId = "319", libraryGameId = 1L,
            title = "Chrono Trigger", iconUrl = null, total = 47, unlocked = 12,
            points = 400, earnedPoints = 95, mastered = false, lastSyncedAt = 5L, lastPlayedAt = 9L,
        )
        every { setDao.observeSetForGame(1L) } returns flowOf(row)
        every { setDao.observeSets() } returns flowOf(listOf(row, row.copy(provider = "LOCAL_STEAM")))

        val set = repo.observeSetForGame(1L).first()!!
        assertEquals(AchievementProvider.RETRO_ACHIEVEMENTS, set.provider)
        assertEquals(12, set.unlocked)
        assertEquals(47, set.total)
        assertEquals(95, set.earnedPoints)
        assertEquals(9L, set.lastPlayedAt)
        assertEquals(listOf("319"), repo.observeSets().first().map { it.providerGameId })
    }

    @Test
    fun `one query feeds every set its own achievements, even sets sharing a game id across providers`() = runTest {
        fun row(provider: String, gameId: String, id: String) = AccountAchievementEntity(
            provider = provider, providerGameId = gameId, providerAchievementId = id,
            title = id, description = "", tier = "", globalRarity = 0.0,
        )
        every { coinDao.observeAll() } returns flowOf(
            listOf(row("STEAM", "440", "s1"), row("RETRO_ACHIEVEMENTS", "440", "r1"), row("STEAM", "440", "s2"), row("STEAM", "620", "p1")),
        )

        val all = repo.observeAllAchievements().first()

        assertEquals(listOf("s1", "s2"), all[AchievementProvider.STEAM to "440"]?.map { it.id })
        assertEquals(listOf("r1"), all[AchievementProvider.RETRO_ACHIEVEMENTS to "440"]?.map { it.id })
        assertEquals(listOf("p1"), all[AchievementProvider.STEAM to "620"]?.map { it.id })
    }

    @Test
    fun `an achievement with no reported rarity has no global percent rather than a negative one`() = runTest {
        every { coinDao.observeForSet("STEAM", "440") } returns flowOf(
            listOf(
                AccountAchievementEntity(
                    provider = "STEAM", providerGameId = "440", providerAchievementId = "a",
                    title = "A", description = "", tier = "",
                    globalRarity = SyncedCoin.RARITY_UNAVAILABLE,
                ),
                AccountAchievementEntity(
                    provider = "STEAM", providerGameId = "440", providerAchievementId = "b",
                    title = "B", description = "", tier = "", globalRarity = 0.0,
                    isEarned = true, earnedAt = 7L,
                ),
            ),
        )

        val achievements = repo.observeAchievements(AchievementProvider.STEAM, "440").first()

        assertNull(achievements[0].globalPercent)
        assertEquals(0.0, achievements[1].globalPercent)
        assertTrue(achievements[1].isUnlocked)
        assertEquals(7L, achievements[1].unlockedAt)
    }

    @Test
    fun `a sync stores each achievement's RetroAchievements points`() = runTest {
        coEvery { retroSource.fetch("319") } returns ProviderSyncResult.Success(
            "319",
            listOf(coin("1", earned = true).copy(points = 25)),
        )
        val coinsSlot = slot<List<AccountAchievementEntity>>()
        coEvery { coinDao.replaceSet(any(), capture(coinsSlot)) } just Runs

        repo.syncAccountEntry(AchievementProvider.RETRO_ACHIEVEMENTS, "319", "Chrono Trigger")

        assertEquals(25, coinsSlot.captured.single().points)
    }

    @Test
    fun `syncGame stores every achievement's unlock state and a summary that is not mastered while one is locked`() = runTest {
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success(
            "440",
            listOf(
                coin("g1", earned = true).copy(globalRarity = 4.5, iconUrl = "g1.png", isHidden = true),
                coin("b1", earned = false),
                coin("b2", earned = true),
            ),
        )
        val setSlot = slot<AccountAchievementSetEntity>()
        val coinsSlot = slot<List<AccountAchievementEntity>>()
        coEvery { coinDao.replaceSet(capture(setSlot), capture(coinsSlot)) } just Runs

        val result = repo.syncGame(1L, AchievementProvider.STEAM, "440")

        assertTrue(result is ProviderSyncResult.Success)
        coVerify { coinDao.replaceSet(any(), match { it.size == 3 }) }
        coVerify { credentials.setLastSyncedAt(any()) }

        val summary = setSlot.captured
        assertEquals("440", summary.providerGameId)
        assertFalse(summary.mastered)

        val byId = coinsSlot.captured.associateBy { it.providerAchievementId }
        assertEquals(listOf(true, false, true), listOf("g1", "b1", "b2").map { byId.getValue(it).isEarned })
        assertEquals(1L, byId.getValue("g1").earnedAt)
        assertNull(byId.getValue("b1").earnedAt)
        assertEquals(4.5, byId.getValue("g1").globalRarity, 0.0)
        assertEquals("g1.png", byId.getValue("g1").iconUrl)
        assertTrue(byId.getValue("g1").isHidden)
    }

    @Test
    fun `syncGame names the account row after its library game`() = runTest {
        coEvery { gameRepository.getById(1L) } returns
            com.psplauncher.core.domain.model.Game(id = 1, title = "Team Fortress 2", platformId = "windows")
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success(
            "440",
            listOf(coin("b1", earned = true)),
        )
        val setSlot = slot<AccountAchievementSetEntity>()
        coEvery { coinDao.replaceSet(capture(setSlot), any()) } just Runs

        repo.syncGame(1L, AchievementProvider.STEAM, "440")

        assertEquals("Team Fortress 2", setSlot.captured.title)
    }

    @Test
    fun `syncAccountEntry stores the provider title and keeps a stub's icon`() = runTest {
        coEvery { setDao.getSet("RETRO_ACHIEVEMENTS", "319") } returns AccountAchievementSetEntity(
            provider = "RETRO_ACHIEVEMENTS", providerGameId = "319",
            title = "Chrono Trigger", iconUrl = "https://media.retroachievements.org/Images/3.png",
        )
        coEvery { retroSource.fetch("319") } returns ProviderSyncResult.Success(
            "319",
            listOf(coin("b1", earned = true)),
        )
        val setSlot = slot<AccountAchievementSetEntity>()
        coEvery { coinDao.replaceSet(capture(setSlot), any()) } just Runs

        repo.syncAccountEntry(AchievementProvider.RETRO_ACHIEVEMENTS, "319", "Chrono Trigger")

        assertEquals("Chrono Trigger", setSlot.captured.title)
        assertEquals("https://media.retroachievements.org/Images/3.png", setSlot.captured.iconUrl)
    }

    @Test
    fun `a blank sync title falls back to the stored set's title`() = runTest {
        coEvery { setDao.getSet("STEAM", "440") } returns setEntity(provider = "STEAM", providerGameId = "440")
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success(
            "440",
            listOf(coin("b1", earned = true)),
        )
        val setSlot = slot<AccountAchievementSetEntity>()
        coEvery { coinDao.replaceSet(capture(setSlot), any()) } just Runs

        repo.syncGame(1L, AchievementProvider.STEAM, "440")

        assertEquals("Some Game", setSlot.captured.title)
    }

    @Test
    fun `syncGame marks mastered when every achievement is earned`() = runTest {
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success(
            "440",
            listOf(coin("g1", earned = true), coin("b1", earned = true)),
        )
        val setSlot = slot<AccountAchievementSetEntity>()
        coEvery { coinDao.replaceSet(capture(setSlot), any()) } just Runs

        repo.syncGame(1L, AchievementProvider.STEAM, "440")

        assertTrue(setSlot.captured.mastered)
    }

    @Test
    fun `a non-success result leaves the database untouched`() = runTest {
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.ProfileNotPublic

        val result = repo.syncGame(1L, AchievementProvider.STEAM, "440")

        assertEquals(ProviderSyncResult.ProfileNotPublic, result)
        coVerify(exactly = 0) { coinDao.replaceSet(any(), any()) }
    }

    @Test
    fun `syncGameById syncs from the stored link`() = runTest {
        coEvery { linkDao.getForGame(1L) } returns ProviderGameLinkEntity(1L, "STEAM", "440", "MANUAL", 0L)
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success(
            "440",
            listOf(coin("b1", earned = true)),
        )

        val result = repo.syncGameById(1L)

        assertTrue(result is ProviderSyncResult.Success)
        coVerify { steamSource.fetch("440") }
    }

    @Test
    fun `syncGameById returns NotLinked when the game has no link`() = runTest {
        coEvery { linkDao.getForGame(1L) } returns null
        assertEquals(ProviderSyncResult.NotLinked, repo.syncGameById(1L))
    }

    @Test
    fun `syncAllLinked syncs every link and tallies the outcomes`() = runTest {
        coEvery { linkDao.getAll() } returns listOf(
            ProviderGameLinkEntity(1L, "STEAM", "440", "MANUAL", 0L),
            ProviderGameLinkEntity(2L, "RETRO_ACHIEVEMENTS", "14402", "MANUAL", 0L),
            ProviderGameLinkEntity(3L, "STEAM", "999", "MANUAL", 0L),
        )
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success("440", listOf(coin("b1", earned = true)))
        coEvery { retroSource.fetch("14402") } returns ProviderSyncResult.NotFound
        coEvery { steamSource.fetch("999") } returns ProviderSyncResult.Failed("network error")

        val progress = mutableListOf<Pair<Int, Int>>()
        val result = repo.syncAllLinked { done, total -> progress += done to total }

        assertEquals(3, result.total)
        assertEquals(1, result.synced)
        assertEquals(1, result.noCoins)
        assertEquals(1, result.failed)
        assertFalse(result.missingCredentials)
        assertEquals(3 to 3, progress.last())
        coVerify { steamSource.fetch("440") }
        coVerify { retroSource.fetch("14402") }
    }

    @Test
    fun `syncAllLinked also refreshes account entries that no library game links to`() = runTest {
        coEvery { linkDao.getAll() } returns listOf(ProviderGameLinkEntity(1L, "STEAM", "440", "MANUAL", 0L))
        coEvery { setDao.getAllSets() } returns listOf(
            setEntity(provider = "STEAM", providerGameId = "440"),
            setEntity(provider = "RETRO_ACHIEVEMENTS", providerGameId = "319"),
        )
        coEvery { steamSource.fetch("440") } returns ProviderSyncResult.Success("440", emptyList())
        coEvery { retroSource.fetch("319") } returns ProviderSyncResult.Success("319", emptyList())

        val result = repo.syncAllLinked()

        assertEquals(2, result.total)
        assertEquals(2, result.synced)
        coVerify(exactly = 1) { steamSource.fetch("440") }
        coVerify(exactly = 1) { retroSource.fetch("319") }
    }

    @Test
    fun `syncAllLinked flags missing credentials`() = runTest {
        coEvery { linkDao.getAll() } returns listOf(
            ProviderGameLinkEntity(1L, "RETRO_ACHIEVEMENTS", "14402", "MANUAL", 0L),
        )
        coEvery { retroSource.fetch("14402") } returns ProviderSyncResult.MissingCredentials

        val result = repo.syncAllLinked()

        assertTrue(result.missingCredentials)
        assertEquals(0, result.synced)
    }

    @Test
    fun `resolveSteamLink stores a link when the title matches`() = runTest {
        coEvery { steamResolver.resolveAppId("Half-Life 2") } returns "220"
        val slot = slot<ProviderGameLinkEntity>()
        coEvery { linkDao.upsert(capture(slot)) } just Runs

        val appId = repo.resolveSteamLink(1L, "Half-Life 2")

        assertEquals("220", appId)
        assertEquals("220", slot.captured.providerGameId)
        assertEquals(AchievementProvider.STEAM.name, slot.captured.provider)
    }
}
