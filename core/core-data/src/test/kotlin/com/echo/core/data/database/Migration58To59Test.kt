package com.echo.core.data.database

import androidx.sqlite.execSQL
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// owner, 2026-10-08: tracks and books gain genre and genre_override, empty, and lose nothing
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration58To59Test {
    @get:Rule
    val helper = migrationTestHelper(DB)

    @Test
    fun `tracks and books gain empty genres and keep their rows`() {
        helper.createDatabase(58).use { db ->
            db.execSQL("INSERT INTO music_folders (id, display_name, tree_uri, enabled, track_count, created_at, updated_at) VALUES ('f1', 'Music', 'file:///m', 1, 1, 0, 0)")
            db.execSQL("INSERT INTO music_tracks (id, folder_id, uri, display_name, title) VALUES ('t1', 'f1', 'file:///m/t1.flac', 't1.flac', 'Tally')")
            db.execSQL("INSERT INTO book_libraries (id, display_name, tree_uri, enabled, scan_recursively, book_count, created_at, updated_at) VALUES ('l1', 'Books', 'file:///b', 1, 1, 1, 0, 0)")
            db.execSQL("INSERT INTO books (id, library_id, uri, display_name, title) VALUES ('b1', 'l1', 'file:///b/b1.epub', 'b1.epub', 'Annihilation')")
        }
        helper.runMigrationsAndValidate(59, listOf(EchoDatabase.MIGRATION_58_59)).use { db ->
            db.singleRow("SELECT title, genre, genre_override FROM music_tracks WHERE id = 't1'") {
                assertEquals("Tally", it.getText(0))
                assertTrue(it.isNull(1) && it.isNull(2), "genres start empty, so the next scan reads them")
            }
            db.singleRow("SELECT title, genre, genre_override FROM books WHERE id = 'b1'") {
                assertEquals("Annihilation", it.getText(0))
                assertTrue(it.isNull(1) && it.isNull(2))
            }
        }
    }

    private companion object {
        const val DB = "migration-58-59-test.db"
    }
}
