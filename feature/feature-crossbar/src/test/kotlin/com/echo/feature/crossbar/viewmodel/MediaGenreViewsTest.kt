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

    @Test
    fun `an album in the Albums view is not a Recent album, so Y gives its own menu`() {
        val album = MusicGroup(key = "born pink", name = "BORN PINK", subtitle = "", trackCount = 8, artUri = null)
        assertEquals(false, album.toGroupRow(ALBUMS_VIEW_PREFIX).isRecentAlbum)
        assertEquals("the shelf's own album rows still are", true, album.toGroupRow("alb").isRecentAlbum)
    }

    // owner, 2026-10-08: with the screens swapped, a name prompt must take the keys; namePromptOpen and
    // withNamePromptText must name the same prompts, or a prompt is typed into but never focused
    @Test
    fun `every prompt that takes typed text counts as an open name prompt`() {
        val base = CrossbarUiState(showBootSequence = false)
        val prompts = listOf(
            base.copy(collectionNameDialog = CollectionNameDialogState(title = "New Genre", editGenreTarget = GenreTarget.Book("b"))),
            base.copy(playlistNameDialog = PlaylistNameDialogState(title = "New Playlist")),
            base.copy(saveThemeNameDialog = PlaylistNameDialogState(title = "Save")),
            base.copy(renameAppTarget = "com.a"),
        )
        prompts.forEach { s ->
            assertTrue(s.namePromptOpen)
            assertTrue("typed text reaches it", s.withNamePromptText("x") != s)
        }
        assertEquals(false, base.namePromptOpen)
    }

    @Test
    fun `an album's menu plays it and sets its genre`() {
        assertEquals(listOf("play_album", "edit_genre"), albumContextMenuItems().map { it.action })
    }
}
