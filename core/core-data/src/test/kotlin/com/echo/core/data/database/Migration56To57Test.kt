package com.echo.core.data.database

import androidx.sqlite.execSQL
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// owner, 2026-10-07: the dead themes table goes. Dropping it must take nothing else with it
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration56To57Test {
    @get:Rule
    val helper = migrationTestHelper(DB)

    @Test
    fun `the themes table goes and the library stays`() {
        helper.createDatabase(56).use { db ->
            db.execSQL(
                "INSERT INTO platforms (id, name, short_name, accent_color, is_pinned_to_bar, " +
                    "bar_position, rom_extensions) " +
                    "VALUES ('snes', 'SNES', 'SNES', 0, 1, 0, 'sfc,smc')"
            )
            db.execSQL(
                "INSERT INTO games (id, title, platform_id, is_disc_primary, is_favorite, " +
                    "favorite_sort_order, total_play_time_millis, is_manual_entry, created_at, " +
                    "content_type, is_missing) " +
                    "VALUES (1, 'Chrono Trigger', 'snes', 1, 1, 7, 4200, 0, 100, 'GAME', 0)"
            )
        }

        helper.runMigrationsAndValidate(57, listOf(EchoDatabase.MIGRATION_56_57)).use { db ->
            val tables = db.rows("SELECT name FROM sqlite_master WHERE type = 'table'") { it.getText(0) }
            assertFalse("themes" in tables, "the themes table should be gone")
            assertEquals(1, db.count("SELECT COUNT(*) FROM games"))
            db.singleRow("SELECT title, total_play_time_millis FROM games WHERE id = 1") {
                assertEquals("Chrono Trigger", it.getText(0))
                assertEquals(4200L, it.getLong(1))
            }
            assertEquals(1, db.count("SELECT COUNT(*) FROM platforms"))
        }
    }

    private companion object {
        const val DB = "migration-56-57-test.db"
    }
}
