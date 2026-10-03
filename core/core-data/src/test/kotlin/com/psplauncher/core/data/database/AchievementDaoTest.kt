package com.psplauncher.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.psplauncher.core.data.database.entity.AccountAchievementEntity
import com.psplauncher.core.data.database.entity.AccountAchievementSetEntity
import com.psplauncher.core.data.database.entity.GameEntity
import com.psplauncher.core.data.database.entity.ProviderGameLinkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AchievementDaoTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PFPDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val sets = db.accountAchievementSetDao()
    private val coins = db.accountAchievementDao()
    private val links = db.providerGameLinkDao()

    @After fun tearDown() = db.close()

    private suspend fun seedGame(): Long = db.gameDao().upsert(
        GameEntity(
            title = "Chrono Trigger",
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
        ),
    )

    private suspend fun seedLink(gameId: Long, provider: String, providerGameId: String) =
        links.upsert(ProviderGameLinkEntity(gameId, provider, providerGameId, "MANUAL", 0L))

    private fun set(
        provider: String,
        providerGameId: String,
        bronzeEarned: Int = 0, silverEarned: Int = 0, goldEarned: Int = 0,
        bronzeTotal: Int = 0, silverTotal: Int = 0, goldTotal: Int = 0,
        mastered: Boolean = false,
    ) = AccountAchievementSetEntity(
        provider = provider, providerGameId = providerGameId, title = "Chrono Trigger",
        bronzeTotal = bronzeTotal, silverTotal = silverTotal, goldTotal = goldTotal,
        bronzeEarned = bronzeEarned, silverEarned = silverEarned, goldEarned = goldEarned,
        mastered = mastered,
    )

    private fun coin(
        provider: String,
        providerGameId: String,
        id: String,
        earned: Boolean,
        points: Int? = null,
    ) = AccountAchievementEntity(
        provider = provider, providerGameId = providerGameId, providerAchievementId = id,
        title = id, description = "", tier = "BRONZE", globalRarity = 30.0, isEarned = earned,
        points = points,
    )

    @Test
    fun `a game's set counts its achievements and RA points through the provider link`() = runTest {
        val gameId = seedGame()
        seedLink(gameId, "RETRO_ACHIEVEMENTS", "319")
        sets.upsert(set("RETRO_ACHIEVEMENTS", "319").copy(lastSyncedAt = 1L))
        coins.upsertAll(
            listOf(
                coin("RETRO_ACHIEVEMENTS", "319", "1", earned = true, points = 10),
                coin("RETRO_ACHIEVEMENTS", "319", "2", earned = false, points = 25),
            ),
        )

        val row = sets.observeSetForGame(gameId).first()!!
        assertEquals(2, row.total)
        assertEquals(1, row.unlocked)
        assertEquals(35, row.points)
        assertEquals(10, row.earnedPoints)
        assertEquals(gameId, row.libraryGameId)
    }

    @Test
    fun `deleting a game severs the link but account rows survive`() = runTest {
        val gameId = seedGame()
        seedLink(gameId, "STEAM", "1337")
        sets.upsert(set("STEAM", "1337").copy(lastSyncedAt = 1L))
        coins.upsertAll(listOf(coin("STEAM", "1337", "ACH_WIN", earned = true)))

        db.openHelper.writableDatabase.execSQL("DELETE FROM games WHERE id = $gameId")

        assertNull(sets.observeSetForGame(gameId).first())
        assertEquals(1, coins.getForSet("STEAM", "1337").size)
        assertNull(sets.observeSets().first().single().libraryGameId)
    }

    @Test
    fun `deleteForSet clears one provider's coins and leaves the other's`() = runTest {
        coins.upsertAll(
            listOf(
                coin("STEAM", "1337", "ACH_WIN", earned = true),
                coin("RETRO_ACHIEVEMENTS", "1337", "77", earned = true),
            ),
        )

        coins.deleteForSet("STEAM", "1337")

        assertEquals(0, coins.getForSet("STEAM", "1337").size)
        assertEquals(1, coins.getForSet("RETRO_ACHIEVEMENTS", "1337").size)
    }

    @Test
    fun `insertIfAbsent never clobbers a synced set and backfill only fills missing icons`() = runTest {
        sets.upsert(
            set("RETRO_ACHIEVEMENTS", "319", bronzeEarned = 5, bronzeTotal = 5)
                .copy(lastSyncedAt = 111L),
        )

        sets.insertIfAbsent(set("RETRO_ACHIEVEMENTS", "319"))
        sets.insertIfAbsent(set("RETRO_ACHIEVEMENTS", "999"))
        sets.backfillIcon("RETRO_ACHIEVEMENTS", "319", "https://icon")

        val synced = sets.getSet("RETRO_ACHIEVEMENTS", "319")!!
        assertEquals(5, synced.bronzeEarned)
        assertEquals(111L, synced.lastSyncedAt)
        assertEquals("https://icon", synced.iconUrl)

        sets.backfillIcon("RETRO_ACHIEVEMENTS", "319", "https://other")
        assertEquals("https://icon", sets.getSet("RETRO_ACHIEVEMENTS", "319")!!.iconUrl)

        val pending = sets.getUnsyncedSets("RETRO_ACHIEVEMENTS")
        assertEquals(listOf("999"), pending.map { it.providerGameId })
    }

    @Test
    fun `the set list leaves out unsynced stubs and puts the last played library game first`() = runTest {
        val gameId = seedGame()
        db.openHelper.writableDatabase.execSQL("UPDATE games SET last_played_at = 500 WHERE id = $gameId")
        seedLink(gameId, "RETRO_ACHIEVEMENTS", "319")
        sets.upsert(set("STEAM", "220").copy(title = "Half-Life 2", lastSyncedAt = 1L))
        sets.upsert(set("RETRO_ACHIEVEMENTS", "319").copy(lastSyncedAt = 1L))
        sets.insertIfAbsent(set("RETRO_ACHIEVEMENTS", "999"))

        val rows = sets.observeSets().first()

        assertEquals(listOf("319", "220"), rows.map { it.providerGameId })
        assertEquals(500L, rows[0].lastPlayedAt)
        assertEquals(gameId, rows[0].libraryGameId)
        assertNull(rows[1].libraryGameId)
        assertEquals("Half-Life 2", rows[1].title)
    }

    @Test
    fun `totals count every unlock but only RetroAchievements points`() = runTest {
        coins.upsertAll(
            listOf(
                coin("RETRO_ACHIEVEMENTS", "319", "1", earned = true, points = 10),
                coin("RETRO_ACHIEVEMENTS", "319", "2", earned = false, points = 50),
                coin("STEAM", "220", "A", earned = true),
            ),
        )

        val totals = coins.observeTotals("RETRO_ACHIEVEMENTS").first()

        assertEquals(2, totals.unlocked)
        assertEquals(3, totals.total)
        assertEquals(10, totals.raPoints)
    }
}
