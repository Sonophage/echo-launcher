package com.echo.core.data.database

import androidx.sqlite.execSQL
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration54To55Test {
    @get:Rule
    val helper = migrationTestHelper(DB)

    @Test
    fun `every photo survives and starts out not favourited`() {
        helper.createDatabase(54).use { db ->
            db.execSQL(
                "INSERT INTO photo_libraries (id, display_name, tree_uri, enabled, scan_recursively, photo_count, " +
                    "created_at, updated_at) VALUES ('lib', 'Pictures', 'content://tree', 1, 1, 2, 1, 1)"
            )
            db.execSQL("INSERT INTO photos (id, library_id, uri, display_name) VALUES ('a', 'lib', 'content://a', 'a.jpg')")
            db.execSQL("INSERT INTO photos (id, library_id, uri, display_name) VALUES ('b', 'lib', 'content://b', 'b.jpg')")
        }

        helper.runMigrationsAndValidate(55, MIGRATIONS).use { db ->
            assertEquals(2L, db.singleRow("SELECT COUNT(*) FROM photos") { it.getLong(0) })
            assertEquals(0L, db.singleRow("SELECT COUNT(*) FROM photos WHERE is_favorite <> 0") { it.getLong(0) })
        }
    }

    private companion object {
        const val DB = "migration-54-55-test.db"
        val MIGRATIONS = listOf(EchoDatabase.MIGRATION_54_55)
    }
}
