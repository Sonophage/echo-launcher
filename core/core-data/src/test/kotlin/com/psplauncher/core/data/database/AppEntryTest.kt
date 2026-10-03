package com.psplauncher.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.psplauncher.core.data.database.entity.GameEntity
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
class AppEntryTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        PFPDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val gameDao = db.gameDao()

    @After fun tearDown() = db.close()

    private fun row(title: String, platform: String, intent: String?) = GameEntity(
        title = title,
        platformId = platform,
        romPath = null,
        packageName = "app.gamenative",
        emulatorPackage = null,
        artworkUri = null,
        logoUri = null,
        description = null,
        developer = null,
        publisher = null,
        releaseYear = null,
        genre = null,
        steamGridDbId = null,
        launchIntentUri = intent,
    )

    @Test
    fun `a game launched through an app's intent is not that app's own entry`() = runTest {
        gameDao.upsert(row("Coral Island", "windows", "intent:#Intent;action=app.gamenative.LAUNCH_GAME;end"))

        assertNull(gameDao.getAppEntry("app.gamenative"))

        val appId = gameDao.upsert(row("GameNative", "app_shortcut", null))
        assertEquals(appId, gameDao.getAppEntry("app.gamenative")?.id)
    }
}
