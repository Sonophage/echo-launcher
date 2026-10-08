package com.echo.feature.crossbar.viewmodel

import com.echo.core.data.repository.MediaRootKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: each column's folder row is named after the column's settings, never "Folders"
class ColumnSettingsRowTest {
    @Test
    fun `the row is named for the column, renamed columns included`() {
        assertEquals("Music Settings", columnSettingsTitle("Music"))
        assertEquals("Emulation Settings", columnSettingsTitle("Emulation"))
        assertEquals("Library Settings", columnSettingsTitle(null))
    }

    @Test
    fun `only a column's own folder row is renamed, not a folder inside it`() {
        val own = CrossbarItem(id = CrossbarViewModel.mediaFoldersItemId(MediaRootKind.MUSIC), title = "Folders",
            type = CrossbarItemType.MEDIA_ROOT, mediaRootKind = MediaRootKind.MUSIC)
        val inside = own.copy(id = "mroot_1", title = "Music", mediaRootUri = "content://tree/Music")
        assertTrue(own.isColumnFoldersRow)
        assertFalse(inside.isColumnFoldersRow)
    }
}
