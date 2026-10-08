package com.echo.core.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.database.entity.toEntity
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType
import com.echo.core.domain.model.PlatformIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-08: giving an app art makes it a hidden shortcut row, which then sat in Recently Added under the
// raw label "app_shortcut". The shelf is games only, as every other game query is
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RecentlyAddedShelfTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        EchoDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val dao = db.gameDao()

    @After fun tearDown() = db.close()

    @Test
    fun `an app's shortcut row is not a recently added game`() = runTest {
        dao.upsert(Game(title = "Ico", platformId = "ps2", romPath = "/r/ico.iso", dateAdded = 10L).toEntity())
        dao.upsert(Game(title = "YouTube", platformId = PlatformIds.APP_SHORTCUT, packageName = "com.google.android.youtube",
            dateAdded = 20L, contentType = GameContentType.ANDROID_APP).toEntity())

        assertEquals(listOf("Ico"), dao.observeRecentlyAdded().first().map { it.title })
        assertEquals(1, dao.observeRecentlyAddedCount().first(), "the shelf's count agrees with what it lists")
    }
}
