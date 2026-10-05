package com.echo.feature.crossbar.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.echo.core.domain.model.GamepadAction

class LibrarySearchTest {
    @Test
    fun `punctuation on either side is ignored`() {
        assertEquals("the legend of zelda ocarina of time", normalizeForSearch("The Legend of Zelda: Ocarina of Time"))
        assertEquals("spider man", normalizeForSearch("Spider-Man"))
        assertEquals("the legend mp4", normalizeForSearch("THE_LEGEND.mp4"))
        assertEquals("", normalizeForSearch("   "))
        assertEquals("", normalizeForSearch("---"))
    }

    @Test
    fun `every word must match, in any order`() {
        val title = "The Legend of Zelda: Ocarina of Time"
        assertTrue(matchesSearch("zelda", title))
        assertTrue("word order must not matter", matchesSearch("ocarina zelda", title))
        assertTrue("punctuation in the query is ignored too", matchesSearch("zelda: ocarina", title))
        assertFalse("a word that is not there fails the whole query", matchesSearch("zelda majora", title))
    }

    @Test
    fun `a match can span several fields`() {
        assertTrue(matchesSearch("tolkien hobbit", "The Hobbit", "J.R.R. Tolkien"))
        assertFalse(matchesSearch("tolkien dune", "The Hobbit", "J.R.R. Tolkien"))

        assertTrue(matchesSearch("hobbit", "The Hobbit", null))
        assertFalse(matchesSearch("tolkien", "The Hobbit", null))
    }

    @Test
    fun `a blank query matches nothing at all`() {
        assertFalse(matchesSearch("", "The Hobbit"))
        assertFalse(matchesSearch("   ", "The Hobbit"))
        assertFalse(matchesSearch("!!!", "The Hobbit"))
    }

    @Test
    fun `an entry with nothing to match on is never a hit`() {
        assertFalse(matchesSearch("anything", ""))
        assertFalse(matchesSearch("anything", null, null))
    }

    @Test
    fun `a partial word still matches`() {
        assertTrue(matchesSearch("ocar", "The Legend of Zelda: Ocarina of Time"))
        assertTrue(matchesSearch("fina fan", "Final Fantasy VII"))
    }

    @Test
    fun `an empty list says which kind of empty it is`() {
        assertEquals(SearchEmptyState.LOADING, searchEmptyState(loaded = false, query = ""))
        assertEquals(SearchEmptyState.LOADING, searchEmptyState(loaded = false, query = "zelda"))

        assertEquals(SearchEmptyState.PROMPT, searchEmptyState(loaded = true, query = ""))

        assertEquals(SearchEmptyState.PROMPT, searchEmptyState(loaded = true, query = "  !!  "))

        assertEquals(SearchEmptyState.NO_MATCHES, searchEmptyState(loaded = true, query = "zelda"))
    }

    @Test
    fun `an empty library says so instead of claiming nothing matched`() {
        assertEquals(
            SearchEmptyState.EMPTY_LIBRARY,
            searchEmptyState(loaded = true, query = "zel", anyContent = false),
        )
    }

    @Test
    fun `an empty library says so before it invites a search`() {
        assertEquals(
            SearchEmptyState.EMPTY_LIBRARY,
            searchEmptyState(loaded = true, query = "", anyContent = false),
        )
    }

    @Test
    fun `still loading beats an empty library`() {
        assertEquals(
            SearchEmptyState.LOADING,
            searchEmptyState(loaded = false, query = "zel", anyContent = false),
        )
    }

    @Test
    fun `a library with content still distinguishes prompt from no matches`() {
        assertEquals(SearchEmptyState.PROMPT, searchEmptyState(loaded = true, query = "", anyContent = true))
        assertEquals(SearchEmptyState.NO_MATCHES, searchEmptyState(loaded = true, query = "zel", anyContent = true))
    }

    @Test
    fun `every scope can say what to do about being empty`() {
        SearchScope.entries.forEach { scope ->
            assertTrue("blank emptyTitle for $scope", scope.emptyTitle.isNotBlank())
            assertTrue("blank emptyHint for $scope", scope.emptyHint.isNotBlank())
            assertTrue("$scope does not say where to go", scope.emptyHint.length > 10)
        }
    }

    // owner, 2026-10-05: two columns, so up and down move a whole row and left and right stay in it
    @Test
    fun `the results are two columns, so up and down move a row and left and right stay in it`() {
        assertEquals(2, searchStep(GamepadAction.NAVIGATE_DOWN, index = 0))
        assertEquals(-2, searchStep(GamepadAction.NAVIGATE_UP, index = 3))
        assertEquals(1, searchStep(GamepadAction.NAVIGATE_RIGHT, index = 2))
        assertEquals(0, searchStep(GamepadAction.NAVIGATE_RIGHT, index = 3))
        assertEquals(-1, searchStep(GamepadAction.NAVIGATE_LEFT, index = 3))
        assertEquals(0, searchStep(GamepadAction.NAVIGATE_LEFT, index = 2))
    }

    // owner, 2026-10-05: the hints sent people to "Settings ▸ Media ▸ Video", which did not exist. Every
    // "Settings ▸ Section ▸ Page" a hint names must be a real section holding that page
    @Test
    fun `every Settings path a hint names exists`() {
        val path = Regex("Settings ▸ ([^▸,]+?)(?: ▸ ([^,]+?))?(?=,| or |$)")
        val named = SearchScope.entries.flatMap { scope -> path.findAll(scope.emptyHint).toList() }
        assertTrue("no hint names a Settings path, so this check sees nothing", named.isNotEmpty())
        named.forEach { m ->
            val section = com.echo.core.domain.model.SettingsSectionId.entries.firstOrNull { it.title == m.groupValues[1].trim() }
            assertTrue("no section called '${m.groupValues[1]}'", section != null)
            m.groupValues[2].takeIf { it.isNotBlank() }?.let { page ->
                assertTrue("no '$page' in ${section!!.title}", com.echo.core.domain.model.settingsEntriesIn(section).any { it.title == page.trim() })
            }
        }
    }

    // owner, 2026-10-05: with Media off the hint said "music, video, photos and books" over nothing
    @Test
    fun `the search hint names apps and only the libraries on the crossbar`() {
        val everything = listOf("games", "music", "videos", "photos", "library")
        assertEquals("Games, apps, music, video, photos and books", searchAllHint(searchKindsShown(everything)))
        assertEquals("Games and apps", searchAllHint(searchKindsShown(listOf("games", "network"))))
        assertEquals("a launcher only still searches apps", "Apps", searchAllHint(searchKindsShown(emptyList())))
        assertFalse(SearchKind.MUSIC in searchKindsShown(listOf("games")))
    }
}
