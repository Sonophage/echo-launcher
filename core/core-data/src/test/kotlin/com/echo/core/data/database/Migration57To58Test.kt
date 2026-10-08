package com.echo.core.data.database

import androidx.sqlite.execSQL
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// owner, 2026-10-08: games gain genre_override, empty, and keep their scraped genre and everything else
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration57To58Test {
    @get:Rule
    val helper = migrationTestHelper(DB)

    @Test
    fun `games gain an empty genre override and keep their scraped genre`() {
        helper.createDatabase(57).use { db ->
            db.execSQL(
                "INSERT INTO platforms (id, name, short_name, accent_color, is_pinned_to_bar, " +
                    "bar_position, rom_extensions) " +
                    "VALUES ('psp', 'PSP', 'PSP', 0, 1, 0, 'iso')"
            )
            db.execSQL(
                "INSERT INTO games (id, title, platform_id, genre, is_disc_primary, is_favorite, " +
                    "favorite_sort_order, total_play_time_millis, is_manual_entry, created_at, " +
                    "content_type, is_missing) " +
                    "VALUES (1, 'Crisis Core', 'psp', 'Action RPG', 1, 0, 0, 4200, 0, 100, 'GAME', 0)"
            )
        }

        helper.runMigrationsAndValidate(58, listOf(EchoDatabase.MIGRATION_57_58)).use { db ->
            db.singleRow("SELECT title, genre, genre_override, total_play_time_millis FROM games WHERE id = 1") {
                assertEquals("Crisis Core", it.getText(0))
                assertEquals("Action RPG", it.getText(1))
                assertNull(if (it.isNull(2)) null else it.getText(2))
                assertEquals(4200L, it.getLong(3))
            }
        }
    }

    private companion object {
        const val DB = "migration-57-58-test.db"
    }
}
