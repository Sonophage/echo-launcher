package com.psplauncher.feature.xmb.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsShelfTest {
    private fun row(at: Long, title: String) = at to XMBItem(id = title, title = title)

    private val games = listOf(row(500, "Skyrim"), row(100, "Crash"))
    private val music = listOf(row(400, "Aja"))
    private val books = listOf(row(300, "Dune"))
    private val videos = listOf(row(200, "Akira"))

    private val noApps = emptyList<Pair<Long, XMBItem>>()

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

    @Test
    fun `the cycle visits every filter once and returns to All`() {
        val seen = generateSequence(RecentFilter.ALL) { it.next(includeApps = false) }
            .drop(1)
            .take(4)
            .toList()

        assertEquals(
            listOf(
                RecentFilter.GAMES, RecentFilter.MUSIC, RecentFilter.BOOKS,
                RecentFilter.VIDEO,
            ),
            seen,
        )
    }

    @Test
    fun `Apps is not in the cycle while it is switched off`() {
        assertEquals(RecentFilter.ALL, RecentFilter.VIDEO.next(includeApps = false))
        assertEquals(RecentFilter.APPS, RecentFilter.VIDEO.next(includeApps = true))
        assertEquals(RecentFilter.ALL, RecentFilter.APPS.next(includeApps = true))
    }

    @Test
    fun `a filter that has just been switched off falls back to All`() {
        assertEquals(RecentFilter.ALL, RecentFilter.APPS.next(includeApps = false))
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
}
