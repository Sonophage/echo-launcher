package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsShelfTest {
    private fun row(at: Long, title: String) = at to CrossbarItem(id = title, title = title)

    private val games = listOf(row(500, "Skyrim"), row(100, "Crash"))
    private val music = listOf(row(400, "Aja"))
    private val books = listOf(row(300, "Dune"))
    private val videos = listOf(row(200, "Akira"))

    private val noApps = emptyList<Pair<Long, CrossbarItem>>()

    @Test
    fun `All interleaves the four media by recency, not by medium`() {
        val merged = mergeRecents(games, music, books, videos, noApps, RecentFilter.ALL, limit = 10)

        assertEquals(listOf("Skyrim", "Aja", "Dune", "Akira", "Crash"), merged.map { it.title })
    }

    @Test
    fun `a filter shows that medium's newest, not the survivors of a cut made across all four`() {
        val merged = mergeRecents(games, music, books, videos, noApps, RecentFilter.GAMES, limit = 2)

        assertEquals(listOf("Skyrim", "Crash"), merged.map { it.title })
    }

    @Test
    fun `a medium with nothing in it yields an empty shelf rather than everything`() {
        assertEquals(
            emptyList<String>(),
            mergeRecents(games, music, emptyList(), videos, noApps, RecentFilter.BOOKS, 10).map { it.title },
        )
    }

    private fun musicTrack(id: String, title: String, album: String? = null) =
        com.echo.core.domain.model.MusicTrack(
            id = id, folderId = "f1", uri = "content://$id", displayName = "$id.mp3",
            title = title, album = album, lastPlayedAt = 900L,
        )

    @Test
    fun `the island can open every shape the recents shelf holds`() {
        val shapes = listOf<Pair<String, CrossbarItem>>(
            "a game" to CrossbarItem(id = "1", title = "Skyrim", gameId = 1L, isRealGame = true),
            "an Android game" to CrossbarItem(
                id = "2", title = "Vampire Survivors", gameId = 2L, isRealGame = true,
                packageName = "com.poncle.vampiresurvivors", isAndroidApp = true,
            ),
            "a recent app" to CrossbarItem(
                id = "${CrossbarViewModel.RECENT_APP_ID_PREFIX}com.discord", title = "Discord",
                subtitle = "App", packageName = "com.discord", isAndroidApp = true,
            ),
            "a harvested shortcut" to CrossbarItem(
                id = "3", title = "Playlist", packageName = "com.spotify.music", shortcutId = "pl1",
            ),
            "a stored intent" to CrossbarItem(id = "4", title = "Maps", launchIntentUri = "intent://maps"),
            "a video" to CrossbarItem(id = "vid_9", title = "Akira", type = CrossbarItemType.VIDEO_FILE),
            "a book" to CrossbarItem(id = "book_9", title = "Dune", type = CrossbarItemType.LIBRARY_BOOK),
        )

        shapes.forEach { (what, item) ->
            assertTrue(
                "$what reaches the shelf but the island cannot open it, so tapping the island " +
                    "would do nothing",
                recentLaunchFor(item) != null,
            )
        }
    }

    @Test
    fun `a run of tracks from one album reaches the shelf as an album, and the island opens it`() {
        val singles = listOf(musicTrack("t1", "Deacon Blues", album = "Aja")).recentMusicRows()
        assertEquals(RecentLaunch.TRACK, recentLaunchFor(singles.single().second))

        val run = listOf(
            musicTrack("t1", "Black Cow", album = "Aja"),
            musicTrack("t2", "Aja", album = "Aja"),
        ).recentMusicRows()

        assertEquals(
            "two tracks from one album collapse to an album row, which is what the island gets",
            CrossbarItemType.MUSIC_GROUP,
            run.single().second.type,
        )
        assertEquals(RecentLaunch.ALBUM, recentLaunchFor(run.single().second))
    }

    @Test
    fun `a row with nothing to launch is not offered to the island`() {
        assertEquals(null, recentLaunchFor(CrossbarItem(id = "x", title = "Folders")))
    }

    // owner, 2026-10-05: an album on the shelf could not be removed; it had no menu and X skipped it
    @Test
    fun `an album on the shelf can be removed, and removing it clears every recent track of it`() {
        val tracks = listOf(
            musicTrack("t1", "Black Cow", album = "Aja"),
            musicTrack("t2", "Aja", album = "Aja"),
            musicTrack("t3", "Kid A", album = "Kid A"),
            musicTrack("t4", "Peg", album = " aja "),
            musicTrack("t5", "Josie", album = "Aja").copy(lastPlayedAt = null),
        )
        val album = tracks.take(2).recentMusicRows().single().second

        assertTrue(album.isRecentAlbum)
        assertTrue(album.removableFromRecent)
        assertEquals(listOf("t1", "t2", "t4"), recentAlbumTrackIds(tracks, album.musicGroupKey!!))
    }

    private val everyKind = RecentFilter.shown(RecentFilter.entries.toSet())

    @Test
    fun `the cycle visits every filter once and returns to All`() {
        val seen = generateSequence(RecentFilter.ALL) { it.step(+1, everyKind) }
            .drop(1)
            .take(6)
            .toList()

        assertEquals(
            listOf(
                RecentFilter.GAMES, RecentFilter.MUSIC, RecentFilter.BOOKS,
                RecentFilter.VIDEO, RecentFilter.APPS, RecentFilter.ALL,
            ),
            seen,
        )
    }

    // owner, 2026-10-05: a filter with nothing in it is neither drawn nor stepped onto
    @Test
    fun `only the kinds with something played are offered, after All`() {
        val stocked = stockedRecentFilters(
            games = listOf(1), music = emptyList(), books = emptyList(), videos = listOf(1), apps = emptyList(),
        )
        assertEquals(listOf(RecentFilter.ALL, RecentFilter.GAMES, RecentFilter.VIDEO), RecentFilter.shown(stocked))
    }

    @Test
    fun `stepping skips an empty filter`() {
        val shown = listOf(RecentFilter.ALL, RecentFilter.GAMES, RecentFilter.VIDEO)
        assertEquals(RecentFilter.VIDEO, RecentFilter.GAMES.step(+1, shown))
        assertEquals(RecentFilter.ALL, RecentFilter.VIDEO.step(+1, shown))
    }

    @Test
    fun `a filter that has just emptied falls back to All`() {
        assertEquals(RecentFilter.ALL, RecentFilter.APPS.step(+1, listOf(RecentFilter.ALL, RecentFilter.GAMES)))
    }

    @Test
    fun `apps are merged by recency like any other medium, and only when present`() {
        val apps = listOf(row(250, "Termux"))

        assertEquals(
            listOf("Skyrim", "Aja", "Dune", "Termux", "Akira", "Crash"),
            mergeRecents(games, music, books, videos, apps, RecentFilter.ALL, limit = 10).map { it.title },
        )

        assertEquals(
            emptyList<String>(),
            mergeRecents(games, music, books, videos, noApps, RecentFilter.APPS, 10).map { it.title },
        )
    }

    @Test
    fun `Today and Yesterday are calendar days, and each row keeps its index in the shelf`() {
        val zone = java.time.ZoneId.of("UTC")
        fun at(text: String) = java.time.LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
        val now = at("2026-10-03T00:30")
        val items = listOf(
            CrossbarItem(id = "a", title = "Skyrim", lastOpenedAt = at("2026-10-03T00:10")),
            CrossbarItem(id = "b", title = "Aja", lastOpenedAt = at("2026-10-02T23:50")),
            CrossbarItem(id = "c", title = "Dune", lastOpenedAt = at("2026-10-01T23:59")),
            CrossbarItem(id = "d", title = "Discord"),
        )

        val grouped = groupRecentsByDay(items, now, zone)
            .map { (day, rows) -> day to rows.map { it.index to it.value.title } }

        assertEquals(
            "40 minutes ago but before midnight is Yesterday, not Today; an unstamped row is Earlier",
            listOf(
                RecentDay.TODAY to listOf(0 to "Skyrim"),
                RecentDay.YESTERDAY to listOf(1 to "Aja"),
                RecentDay.EARLIER to listOf(2 to "Dune", 3 to "Discord"),
            ),
            grouped,
        )
    }

    @Test
    fun `the merge stamps each row with the time it was opened`() {
        val merged = mergeRecents(games, music, books, videos, noApps, RecentFilter.ALL, limit = 2)

        assertEquals(listOf(500L, 400L), merged.map { it.lastOpenedAt })
    }

    @Test
    fun `the limit is honoured across the merge`() {
        val merged = mergeRecents(games, music, books, videos, noApps, RecentFilter.ALL, limit = 3)

        assertEquals(listOf("Skyrim", "Aja", "Dune"), merged.map { it.title })
    }
}

class ShelfCardTitleTest {
    /**
     * SHELF_CARD_IDS and shelfCardFor are a pair. A shelf column's id is not in
     * enabledCards, so every name lookup falls through to shelfCardFor -- and when
     * that returned null the raw id reached the screen as
     * "Hide from __shelf_marked_PLAYING".
     */
    @org.junit.Test
    fun `every shelf card id resolves to a title a person would recognise`() {
        SHELF_CARD_IDS.forEach { id ->
            val card = shelfCardFor(id)
            org.junit.Assert.assertNotNull("$id resolves to no card", card)
            org.junit.Assert.assertFalse(
                "$id leaked into its own title",
                card!!.title.contains("__") || card.title == id,
            )
        }
    }
}

/**
 * Remove from Recent used to write a HiddenPlacement for apps, which is permanent
 * until undone in Settings -- while the same menu row on a game or a track simply
 * cleared the play stamp and let it come back. These pin the app case to the game
 * case: dropped now, back the moment it is used again.
 */
class RecentAppDismissalTest {
    private val dismissedAt = 1_000L

    @Test
    fun `an app used again after being dismissed comes back`() {
        assertFalse(
            "using the app again is exactly what should undo a dismissal",
            dismissedFromRecents(lastUsedAt = dismissedAt + 1, dismissedAt = dismissedAt),
        )
    }

    @Test
    fun `an app not used since being dismissed stays off the shelf`() {
        assertTrue(dismissedFromRecents(lastUsedAt = dismissedAt - 1, dismissedAt = dismissedAt))

        assertTrue(
            "a dismissal must cover the launch that caused it, or the app never leaves",
            dismissedFromRecents(lastUsedAt = dismissedAt, dismissedAt = dismissedAt),
        )
    }

    @Test
    fun `an app that was never dismissed is untouched`() {
        assertFalse(dismissedFromRecents(lastUsedAt = 5L, dismissedAt = null))
    }

    @Test
    fun `dismissing the same app twice replaces the stamp instead of stacking`() {
        val once = withRecentDismissal(emptySet(), "com.example.app", 10L)
        val twice = withRecentDismissal(once, "com.example.app", 20L)

        assertEquals("one entry per package, or the set grows without bound", 1, twice.size)
        assertEquals(20L, parseRecentDismissals(twice)["com.example.app"])
    }

    @Test
    fun `dismissing one app leaves the others alone`() {
        val set = withRecentDismissal(withRecentDismissal(emptySet(), "a.b.c", 10L), "d.e.f", 20L)
        val parsed = parseRecentDismissals(set)

        assertEquals(10L, parsed["a.b.c"])
        assertEquals(20L, parsed["d.e.f"])
    }

    @Test
    fun `a malformed entry is ignored rather than crashing the shelf`() {
        val parsed = parseRecentDismissals(setOf("no-separator", "|123", "a.b.c|notanumber", "a.b.c|7"))

        assertEquals("only the well formed entry survives", mapOf("a.b.c" to 7L), parsed)
    }

    @Test
    fun `a recent app's id carries the prefix, or its menu would never offer Remove from Recent`() {
        val id = recentAppId("com.discord")
        assertEquals("recentapp_com.discord", id)
        assertTrue(id.startsWith(CrossbarViewModel.RECENT_APP_ID_PREFIX))
    }

    // owner, 2026-10-06: a pinned list under Recent, games and apps, in the order they were pinned
    @Test
    fun `pins toggle on and off, keep their order, and survive a round trip through storage`() {
        val pinned = togglePin(togglePin(emptyList(), "g:1"), "a:com.discord")
        assertEquals(listOf("g:1", "a:com.discord"), pinned)
        assertEquals(listOf("a:com.discord"), togglePin(pinned, "g:1"))
        assertEquals(pinned, parsePins(pinned.joinToString("\n")))
        assertEquals("a stray blank line or a repeat is no pin", listOf("g:1"), parsePins("g:1\n\ng:1"))
        assertEquals("g:7", pinKey(CrossbarItem(id = "x", title = "Ico", gameId = 7L)))
        assertEquals("a:com.discord", pinKey(CrossbarItem(id = "y", title = "Discord", packageName = "com.discord")))
    }

    @Test
    // owner, 2026-10-07: pinned first
    fun `the pinned rows lead the rail, in their own group above the dated ones`() {
        val now = 1_000_000_000L
        val items = listOf(
            CrossbarItem(id = pinnedRowId("g:1"), title = "Skyrim", lastOpenedAt = now, pinnedToRecent = true),
            CrossbarItem(id = "a", title = "Skyrim", lastOpenedAt = now),
        )
        val grouped = groupRecentsByDay(items, now).map { (day, rows) -> day to rows.map { it.index } }
        assertEquals(listOf(RecentDay.PINNED to listOf(0), RecentDay.TODAY to listOf(1)), grouped)
    }

    @Test
    fun `a filter shows the pinned rows of its own kind`() {
        val game = CrossbarItem(id = pinnedRowId("g:1"), title = "Ico", gameId = 1L, pinnedToRecent = true)
        val app = CrossbarItem(id = pinnedRowId("a:com.discord"), title = "Discord", packageName = "com.discord", isAndroidApp = true, pinnedToRecent = true)
        assertEquals(listOf(game, app), pinnedForFilter(listOf(game, app), RecentFilter.ALL))
        assertEquals(listOf(game), pinnedForFilter(listOf(game, app), RecentFilter.GAMES))
        assertEquals(listOf(app), pinnedForFilter(listOf(game, app), RecentFilter.APPS))
        assertEquals(emptyList<CrossbarItem>(), pinnedForFilter(listOf(game, app), RecentFilter.MUSIC))
    }
}
