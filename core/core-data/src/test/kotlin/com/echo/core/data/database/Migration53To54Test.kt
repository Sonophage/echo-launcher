package com.echo.core.data.database

import androidx.sqlite.execSQL
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration53To54Test {
    @get:Rule
    val helper = migrationTestHelper(DB)

    private fun seed(db: androidx.sqlite.SQLiteConnection) {
        db.execSQL(
            "INSERT INTO platforms (id, name, short_name, accent_color, is_pinned_to_bar, " +
                "bar_position, rom_extensions) VALUES ('ps2', 'PlayStation 2', 'PS2', 0, 1, 0, 'iso')"
        )
    }

    private fun insertGame(
        db: androidx.sqlite.SQLiteConnection,
        id: Long,
        artwork: String?,
        hero: String?,
        icon: String? = null,
        logo: String? = null,
        boxArt: String? = null,
        mode: String? = null,
    ) {
        fun q(v: String?) = if (v == null) "NULL" else "'$v'"
        db.execSQL(
            "INSERT INTO games (id, title, platform_id, is_disc_primary, artwork_uri, hero_uri, " +
                "logo_uri, icon_uri, box_art_uri, physical_media_uri, box3d_uri, icon_display_mode, " +
                "is_favorite, favorite_sort_order, total_play_time_millis, is_manual_entry, " +
                "created_at, content_type, is_missing) " +
                "VALUES ($id, 'Game $id', 'ps2', 1, ${q(artwork)}, ${q(hero)}, ${q(logo)}, " +
                "${q(icon)}, ${q(boxArt)}, NULL, NULL, ${q(mode)}, 0, 0, 0, 0, 100, 'GAME', 0)"
        )
    }

    @Test
    fun `the five art columns are gone and the surviving ones keep their values`() {
        helper.createDatabase(53).use { db ->
            seed(db)
            insertGame(db, 1, artwork = "content://bg", hero = "content://hero",
                icon = "content://icon", logo = "content://logo", boxArt = "content://box", mode = "BOX_ART")
        }

        helper.runMigrationsAndValidate(54, MIGRATIONS).use { db ->
            val columns = db.rows("PRAGMA table_info(games)") { it.getText(1) }.toSet()
            listOf("hero_uri", "box_art_uri", "box3d_uri", "physical_media_uri", "icon_display_mode")
                .forEach { assertFalse(it in columns, "$it survived the rebuild") }

            db.singleRow("SELECT artwork_uri, logo_uri, icon_uri FROM games WHERE id = 1") {
                assertEquals("content://bg", it.getText(0))
                assertEquals("content://logo", it.getText(1))
                assertEquals("content://icon", it.getText(2))
            }
        }
    }

    @Test
    fun `a game whose only background was its hero keeps a background`() {
        helper.createDatabase(53).use { db ->
            seed(db)
            insertGame(db, 1, artwork = null, hero = "content://hero")
            insertGame(db, 2, artwork = "", hero = "content://hero2")
            insertGame(db, 3, artwork = "content://own", hero = "content://hero3")
            insertGame(db, 4, artwork = null, hero = null, icon = "content://icon4")
        }

        helper.runMigrationsAndValidate(54, MIGRATIONS).use { db ->
            assertEquals(
                "content://hero",
                db.singleRow("SELECT artwork_uri FROM games WHERE id = 1") { it.getText(0) },
                "a hero WAS the background file for most scraped games; dropping the column " +
                    "without this leaves them with no backdrop at all",
            )
            assertEquals(
                "content://own",
                db.singleRow("SELECT artwork_uri FROM games WHERE id = 3") { it.getText(0) },
            )
            assertTrue(
                db.singleRow("SELECT artwork_uri IS NULL FROM games WHERE id = 4") { it.getLong(0) } == 1L,
                "a game with no art must not gain one",
            )
        }
    }

    @Test
    fun `the play history survives the table rebuild`() {
        helper.createDatabase(53).use { db ->
            seed(db)
            insertGame(db, 1, artwork = "content://bg", hero = null)
            insertGame(db, 2, artwork = null, hero = null)
            db.execSQL(
                "INSERT INTO play_sessions (id, game_id, platform_id, launched_at, duration_millis) " +
                    "VALUES (1, 1, 'ps2', 1000, 60000), (2, 1, 'ps2', 2000, 30000), " +
                    "(3, 2, 'ps2', 3000, 10000)"
            )
        }

        helper.runMigrationsAndValidate(54, MIGRATIONS).use { db ->
            assertEquals(
                3,
                db.count("SELECT COUNT(*) FROM play_sessions"),
                "play_sessions.game_id is ON DELETE CASCADE, so DROP TABLE games takes the whole " +
                    "play history with it unless the rebuild defers foreign keys",
            )
            assertEquals(2, db.count("SELECT COUNT(*) FROM games"))
        }
    }

    @Test
    fun `the rebuilt table keeps its ids, its indices and its autoincrement`() {
        helper.createDatabase(53).use { db ->
            seed(db)
            insertGame(db, 7, artwork = "content://bg", hero = null)
        }

        helper.runMigrationsAndValidate(54, MIGRATIONS).use { db ->
            assertEquals(7L, db.singleRow("SELECT id FROM games") { it.getLong(0) })

            val indices = db.rows("PRAGMA index_list(games)") { it.getText(1) }.toSet()
            listOf(
                "index_games_platform_id",
                "index_games_is_favorite",
                "index_games_last_played_at",
                "index_games_rom_path",
                "index_games_artwork_key",
                "index_games_disc_set_key",
                "index_games_storefront_storefront_game_id",
            ).forEach { assertTrue(it in indices, "$it was not recreated") }

            db.execSQL(
                "INSERT INTO games (title, platform_id, is_disc_primary, is_favorite, " +
                    "favorite_sort_order, total_play_time_millis, is_manual_entry, created_at, " +
                    "content_type, is_missing) " +
                    "VALUES ('New', 'ps2', 1, 0, 0, 0, 0, 100, 'GAME', 0)"
            )
            assertEquals(
                8L,
                db.singleRow("SELECT id FROM games WHERE title = 'New'") { it.getLong(0) },
                "a rebuild that loses AUTOINCREMENT starts handing out ids that were already used",
            )
        }
    }

    @Test
    fun `the play history survives the real Room upgrade path, not just the test helper`() {
        helper.createDatabase(53).use { db ->
            seed(db)
            insertGame(db, 1, artwork = "content://bg", hero = null)
            db.execSQL(
                "INSERT INTO play_sessions (id, game_id, platform_id, launched_at, duration_millis) " +
                    "VALUES (1, 1, 'ps2', 1000, 60000), (2, 1, 'ps2', 2000, 30000)"
            )
        }

        val context: android.content.Context =
            androidx.test.core.app.ApplicationProvider.getApplicationContext()
        val room = androidx.room.Room
            .databaseBuilder(context, EchoDatabase::class.java, DB)
            .addMigrations(*EchoDatabase.ALL_MIGRATIONS)
            .build()

        try {
            val sessions = room.openHelper.readableDatabase
                .query("SELECT COUNT(*) FROM play_sessions")
                .use { it.moveToFirst(); it.getInt(0) }
            assertEquals(
                2,
                sessions,
                "play_sessions.game_id is ON DELETE CASCADE. If foreign keys are enforced while " +
                    "the migration runs, DROP TABLE games takes the whole play history with it.",
            )
            room.openHelper.writableDatabase.execSQL("DELETE FROM games WHERE id = 1")
            val afterDelete = room.openHelper.readableDatabase
                .query("SELECT COUNT(*) FROM play_sessions")
                .use { it.moveToFirst(); it.getInt(0) }
            assertEquals(
                0,
                afterDelete,
                "if this is 2, foreign keys are not enforced in this environment at all and the " +
                    "assertion above passed for the wrong reason",
            )
        } finally {
            room.close()
        }
    }

    private companion object {
        const val DB = "migration-53-54-test.db"
        val MIGRATIONS = listOf(EchoDatabase.MIGRATION_53_54)
    }
}
