package com.echo.feature.appbar

import com.echo.core.domain.model.Book
import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: the App Drawer holds albums, videos and books in their own sections
class DrawerMediaTest {
    private fun t(id: String, album: String?, artist: String, genre: String? = null) =
        MusicTrack(id = id, folderId = "f", uri = "content://$id", displayName = "$id.flac", album = album, artist = artist, genreOverride = genre)

    private val albums = albumCases(listOf(
        t("1", "BORN PINK", "BLACKPINK", "K-Pop"), t("2", "born pink", "BLACKPINK", "K-Pop"), t("3", "Longitudes", "Vela Quartet"), t("4", null, "Loose"),
    ))

    @Test
    fun `an album is one case, a track with no album is not one`() {
        assertEquals(listOf("BORN PINK", "Longitudes"), albums.map { it.label })
        assertEquals("K-Pop", albums.first().media?.genre)
        assertEquals("born pink", albums.first().media?.ref)
    }

    @Test
    fun `media sit in their own sections, never among the apps or Recently Used`() {
        val video = videoCases(listOf(Video(id = "v", libraryId = "l", uri = "content://v", displayName = "Harbour Lights.mp4", lastWatchedAt = 5L))).single()
        val book = bookCases(listOf(Book(id = "b", libraryId = "l", uri = "content://b", displayName = "Annihilation.epub"))).single()
        assertTrue(AppFilter.MUSIC.matches(albums.first()) && AppFilter.VIDEOS.matches(video) && AppFilter.BOOKS.matches(book))
        listOf(AppFilter.APPS, AppFilter.RECENT, AppFilter.GAMES).forEach { f ->
            assertTrue("$f holds media", listOf(albums.first(), video, book).none(f::matches))
        }
    }

    @Test
    fun `Music's buttons are artists, or genres with X, and each keeps its own`() {
        assertEquals(listOf("All", "BLACKPINK", "Vela Quartet"), mediaChips(albums, byGenre = false).map { it.label })
        assertEquals(listOf("All", "K-Pop"), mediaChips(albums, byGenre = true).map { it.label })
        assertEquals(listOf("Longitudes"), albums.ofMediaChip("Vela Quartet", byGenre = false).map { it.label })
        assertEquals("Group by Genre", mediaGroupingHint(AppFilter.MUSIC, byGenre = false))
        assertEquals("Group by Author", mediaGroupingHint(AppFilter.BOOKS, byGenre = true))
        assertNull("videos have no grouping", mediaGroupingHint(AppFilter.VIDEOS, byGenre = false))
    }

    // owner, 2026-10-08: grouped by genre the row shows with one genre, so X visibly does something
    @Test
    fun `genre buttons show with one genre, artists and systems need two`() {
        val one = listOf(SystemChip(null, "All", 3), SystemChip("K-Pop", "K-Pop", 1))
        assertTrue(AppDrawerUiState(activeFilter = AppFilter.MUSIC, systemChips = one, mediaChipsByGenre = true).showSystemChips)
        assertEquals(false, AppDrawerUiState(activeFilter = AppFilter.MUSIC, systemChips = one, mediaChipsByGenre = false).showSystemChips)
        assertTrue(AppDrawerUiState(activeFilter = AppFilter.GAMES, systemChips = one, chipsByGenre = true).showSystemChips)
        assertEquals(false, AppDrawerUiState(activeFilter = AppFilter.GAMES, systemChips = one).showSystemChips)
    }

    // owner, 2026-10-08: the info column shows an album's artist and genre, and a game's achievements
    @Test
    fun `the info column names the artist and genre, and counts achievements only when there are some`() {
        assertEquals("BLACKPINK  ·  K-Pop", mediaByline(albums.first().media!!))
        assertEquals("Vela Quartet", mediaByline(albums[1].media!!))
        assertEquals("12/40", achievementsLabel(12, 40))
        assertNull(achievementsLabel(0, 0))
        assertNull(achievementsLabel(null, null))
    }

    @Test
    fun `an album or video plays, a book reads, an app opens`() {
        assertEquals("Play", com.echo.feature.appbar.appdrawer.actionLabel(albums.first()))
        assertEquals("Read", com.echo.feature.appbar.appdrawer.actionLabel(bookCases(listOf(Book(id = "b", libraryId = "l", uri = "u", displayName = "b.epub"))).single()))
        assertEquals("Open", com.echo.feature.appbar.appdrawer.actionLabel(InstalledApp("com.a", "A", null, isGame = false, isEmulator = false)))
    }
}
