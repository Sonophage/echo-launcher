package com.psplauncher.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRootRowsTest {
    private val music = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
    private val sdCard = "content://com.android.externalstorage.documents/tree/1A2B-3C4D%3AMusic"

    private fun rows(
        roots: List<String>,
        persisted: Set<String> = roots.toSet(),
        scanned: List<MediaScannedEntry> = emptyList(),
    ) = mediaRootRows(roots, persisted, scanned) { "Music" }

    @Test
    fun `a root with a completed scan carries its count and stamp`() {
        val row = rows(
            roots = listOf(music),
            scanned = listOf(MediaScannedEntry(music, "Music", itemCount = 412, lastScannedAt = 1_700L)),
        ).single()

        assertEquals("the count belongs to the root it was scanned from", 412, row.itemCount)
        assertEquals(1_700L, row.lastScannedAt)
        assertTrue("a stamped row is a scanned row", row.scanned)
    }

    @Test
    fun `a granted root that has never been scanned reports no count, not zero`() {
        val row = rows(roots = listOf(music)).single()

        assertNull(
            "zero tracks and never-looked are different states, and the row must not claim the folder is empty",
            row.itemCount,
        )
        assertEquals("Music", row.name)
    }

    @Test
    fun `a folder row that exists but never completed a scan still reports no count`() {
        val row = rows(
            roots = listOf(music),
            scanned = listOf(MediaScannedEntry(music, "Music", itemCount = 0, lastScannedAt = null)),
        ).single()

        assertNull(
            "addFolder creates the row before the scanner ever runs, so its zero is not a result",
            row.itemCount,
        )
    }

    @Test
    fun `a root whose grant did not survive is listed and marked unlinked`() {
        val row = rows(roots = listOf(music), persisted = emptySet()).single()

        assertTrue(
            "a lost grant must still be listed, or Relink is unreachable and the folder is stranded",
            row.treeUri == music,
        )
        assertEquals(false, row.linked)
    }

    @Test
    fun `a scanned folder with no matching root is not listed`() {
        val listed = rows(
            roots = listOf(music),
            scanned = listOf(
                MediaScannedEntry(music, "Music", 5, 1L),
                MediaScannedEntry(sdCard, "SDCARD", 99, 1L),
            ),
        )

        assertEquals(
            "roots drive the list; an orphan left over from a removed root must not reappear as a folder you can act on",
            listOf(music),
            listed.map { it.treeUri },
        )
    }

    @Test
    fun `roots are listed in the order they were granted`() {
        val listed = rows(roots = listOf(sdCard, music))

        assertEquals(listOf(sdCard, music), listed.map { it.treeUri })
    }
}
