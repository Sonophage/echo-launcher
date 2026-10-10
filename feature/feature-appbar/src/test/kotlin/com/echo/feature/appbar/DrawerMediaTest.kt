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

    // owner, 2026-10-08: X steps Music through artist, album and genre buttons
    @Test
    fun `Music's buttons step artist, album letter, genre, and each keeps its own`() {
        assertEquals(listOf("All", "BLACKPINK", "Vela Quartet"), mediaChips(albums, MediaGrouping.MAKER).map { it.label })
        assertEquals(listOf("All", "B", "L"), mediaChips(albums, MediaGrouping.TITLE).map { it.label })
        assertEquals(listOf("All", "K-Pop"), mediaChips(albums, MediaGrouping.GENRE).map { it.label })
        assertEquals(listOf("Longitudes"), albums.ofMediaChip("L", MediaGrouping.TITLE).map { it.label })
        assertEquals(listOf("Longitudes"), albums.ofMediaChip("Vela Quartet", MediaGrouping.MAKER).map { it.label })
        assertEquals("By Artist", mediaGroupingHint(AppFilter.MUSIC, MediaGrouping.MAKER))
        assertEquals("Albums A–Z", mediaGroupingHint(AppFilter.MUSIC, MediaGrouping.TITLE))
        assertEquals("By Genre", mediaGroupingHint(AppFilter.MUSIC, MediaGrouping.GENRE))
        assertEquals("By Author", mediaGroupingHint(AppFilter.BOOKS, MediaGrouping.MAKER))
        assertNull("videos have no grouping", mediaGroupingHint(AppFilter.VIDEOS, MediaGrouping.MAKER))
    }

    @Test
    fun `genre buttons show with one genre, artists and systems need two`() {
        val one = listOf(SystemChip(null, "All", 3), SystemChip("K-Pop", "K-Pop", 1))
        assertTrue(AppDrawerUiState(activeFilter = AppFilter.MUSIC, systemChips = one, mediaGrouping = MediaGrouping.GENRE).showSystemChips)
        assertEquals(false, AppDrawerUiState(activeFilter = AppFilter.MUSIC, systemChips = one, mediaGrouping = MediaGrouping.MAKER).showSystemChips)
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

    // owner, 2026-10-08: the last badges earned, in one row
    @Test
    fun `the row holds the last badges earned, newest first, and only earned ones with an icon`() {
        val coins = listOf(
            EarnedBadge("old", true, 10), EarnedBadge("new", true, 30), EarnedBadge("locked", false, null),
            EarnedBadge(null, true, 40), EarnedBadge("mid", true, 20),
        )
        assertEquals(listOf("new", "mid", "old"), recentBadges(coins))
        assertEquals(listOf("new", "mid"), recentBadges(coins, max = 2))
    }

    // owner, 2026-10-09 (seen on the Konker): a library with no genre tags has no genre groups, so the rail fell back
    // to letters while X's hint said By Genre. X now skips a grouping with nothing to show
    @Test
    fun `X skips a grouping with nothing to show, so the hint matches the rail`() {
        val untagged = albumCases(listOf(t("1", "Halfaxa", "Grimes"), t("2", "Longitudes", "Vela Quartet"), t("3", "Visions", "Grimes")))
        val onAlbums = AppDrawerUiState(activeFilter = AppFilter.MUSIC, mediaGrouping = MediaGrouping.TITLE)
        assertEquals(MediaGrouping.MAKER, nextMediaGrouping(onAlbums, untagged))
        assertEquals("with genre tags, genres come next", MediaGrouping.GENRE, nextMediaGrouping(onAlbums, albums))
    }
}
