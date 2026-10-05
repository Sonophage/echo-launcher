package com.echo.feature.settings.viewmodel

import com.echo.core.domain.model.Book
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType
import com.echo.core.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewCountsTest {
    private fun game(id: Long, platform: String, pkg: String? = null, played: Long? = null, type: GameContentType = GameContentType.GAME) =
        Game(id = id, title = "g$id", platformId = platform, packageName = pkg, lastPlayedAt = played, iconUri = "art/$id", contentType = type)

    private fun track(artist: String?, albumArtist: String? = null) =
        MusicTrack(id = artist ?: "x", folderId = "f", uri = "u", displayName = "t", artist = artist, albumArtist = albumArtist)

    @Test fun `games split into consoles, Android and PC, and apps are not games`() {
        val c = overviewCounts(
            listOf(
                game(1, "gba"), game(2, "gba"), game(3, "nds"),
                game(4, "android", pkg = "com.a"),
                game(5, "windows"),
                game(7, "windows", pkg = "app.gamenative"),
                game(6, "android", pkg = "com.app", type = GameContentType.ANDROID_APP),
            ),
            emptyList(), emptyList(), 0, 0,
        )
        assertEquals(6, c.games)
        assertEquals("two consoles, not three games", 2, c.consoles)
        assertEquals(1, c.android)
        assertEquals("a PC game launched through an Android app is still PC", 2, c.pc)
    }

    @Test fun `artists are counted once each, by album artist first`() {
        val c = overviewCounts(emptyList(), listOf(track("Halsey"), track("halsey "), track("Feat X", albumArtist = "Halsey"), track("Grimes")), emptyList(), 0, 0)
        assertEquals(2, c.artists)
    }

    @Test fun `the fan shows the three most recently played, newest first`() {
        val c = overviewCounts(
            listOf(game(1, "gba", played = 10), game(2, "gba", played = 40), game(3, "gba"), game(4, "gba", played = 30), game(5, "gba", played = 20)),
            emptyList(), emptyList(), 0, 0,
        )
        assertEquals(listOf("g2", "g4", "g5"), c.lastPlayed.map { it.title })
    }

    @Test fun `opened books are the ones with a last-opened time`() {
        val books = listOf(
            Book(id = "a", libraryId = "l", uri = "u", displayName = "a", lastOpenedAt = 5),
            Book(id = "b", libraryId = "l", uri = "u", displayName = "b"),
        )
        assertEquals(1, overviewCounts(emptyList(), emptyList(), books, 0, 0).booksOpened)
    }

    // owner, 2026-10-05: the Overview's media column shows only the kinds whose category is on the crossbar
    @Test
    fun `the media column lists only the media categories that are turned on`() {
        val counts = OverviewCounts(tracks = 3966, artists = 248, videos = 33, photos = 12, books = 80, booksOpened = 1)
        val shown = setOf(com.echo.core.domain.model.BuiltInCategory.MUSIC, com.echo.core.domain.model.BuiltInCategory.LIBRARY, "network")

        val rows = overviewMediaRows(counts, shown)

        org.junit.Assert.assertEquals(listOf(OverviewMedia.MUSIC, OverviewMedia.BOOKS), rows.map { it.kind })
        org.junit.Assert.assertEquals("3966 tracks", rows.first().main)
    }
}
