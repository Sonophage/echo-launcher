package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.Book
import com.echo.core.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: Music and the Library browse by genre, and a track, album or book's genre can be set
class MediaGenreViewsTest {
    private fun t(id: String, genre: String?, override: String? = null) =
        MusicTrack(id = id, folderId = "f", uri = "content://$id", displayName = "$id.flac", genre = genre, genreOverride = override)

    @Test
    fun `music groups by genre, the owner's first, untagged under No Genre last`() {
        val groups = listOf(t("1", "Pop"), t("2", "pop"), t("3", "Rock", override = "K-Pop"), t("4", "")).genreGroups()
        assertEquals(listOf("K-Pop", "Pop", "No Genre"), groups.map { it.name })
        assertEquals("Pop and pop are one genre", 2, groups[1].trackCount)
    }

    @Test
    fun `books group by genre and a book with none is left out`() {
        fun b(id: String, genre: String?, override: String? = null) =
            Book(id = id, libraryId = "l", uri = "content://$id", displayName = "$id.epub", genre = genre, genreOverride = override)
        val groups = listOf(b("1", "Horror"), b("2", "Horror"), b("3", "", override = "Weird"), b("4", "")).genreGroups()
        assertEquals(listOf("Horror" to 2, "Weird" to 1), groups.map { it.name to it.bookCount })
    }

    @Test
    fun `the picker offers the tag, the library's genres once each and a new one`() {
        val rows = genrePickerRows(tagged = "Pop", current = "K-Pop", inLibrary = listOf("Rock", "K-Pop", "rock"))
        assertEquals(listOf("As tagged · Pop", "K-Pop", "Rock", "New Genre…"), rows.map { it.label })
        assertTrue("the owner's genre is ticked", rows.first { it.label == "K-Pop" }.checked)
        assertEquals("K-Pop", genreOfPick(rows[1].action!!))
        assertNull("As tagged clears the owner's genre", genreOfPick(GENRE_PICK_TAG))
    }

    @Test
    fun `a track's and a book's menus offer Edit Genre`() {
        assertTrue("edit_genre" in musicTrackContextMenuItems(null, hasPlayStamp = false).map { it.action })
        assertTrue("edit_genre" in bookContextMenuItems(hasOpenStamp = false).map { it.action })
    }
}
