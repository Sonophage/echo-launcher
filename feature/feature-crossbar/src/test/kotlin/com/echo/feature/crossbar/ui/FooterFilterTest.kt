package com.echo.feature.crossbar.ui

import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.PanelTab
import com.echo.feature.crossbar.viewmodel.SearchKind
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-06: a screen's filter is the footer's, one LT/RT mark and the current one's word; a tap steps on
class FooterFilterTest {
    // owner, 2026-10-07: the open panel's tabs are a tab row along its top, as in Settings, not a footer filter
    @Test
    fun `the open panel has no footer filter, its tabs are its own row`() {
        assertNull(footerFilter(CrossbarUiState(showBootSequence = false, notificationsOpen = true, panelTab = PanelTab.SETTINGS)))
    }

    @Test
    fun `a crossbar with no filter and no sort shows none`() {
        assertNull(footerFilter(CrossbarUiState(showBootSequence = false)))
    }

    @Test
    fun `Search names the kind with its count, and a tap moves through All and each kind`() {
        val counts = listOf(SearchKind.GAMES to 4, SearchKind.APPS to 2)
        val all = SearchState(scope = SearchScope.ALL, total = 6, kindCounts = counts)
        assertEquals("All 6" to SearchKind.GAMES, searchKindFilter(all))
        assertEquals("Games 4" to SearchKind.APPS, searchKindFilter(all.copy(kind = SearchKind.GAMES)))
        assertEquals("the last kind wraps to All", "Apps 2" to null, searchKindFilter(all.copy(kind = SearchKind.APPS)))
        assertNull("one kind is nothing to filter", searchKindFilter(all.copy(kindCounts = counts.take(1))))
    }

    // owner, 2026-10-08: a genre filter in force reads beside the sort, so a short game list explains itself
    @Test
    fun `a game list names the genre it is filtered to beside its sort`() {
        val games = com.echo.core.domain.model.Category(id = com.echo.core.domain.model.BuiltInCategory.GAMES, name = "Game",
            iconKey = "ic_games", type = com.echo.core.domain.model.CategoryType.BUILT_IN, position = 2, isGamingCategory = true)
        val state = CrossbarUiState(showBootSequence = false, categories = listOf(games), selectedCategoryIndex = 0, selectedPlatformId = "psp")
        assertEquals("Title", footerFilter(state)?.first)
        assertEquals("Title · RPG", footerFilter(state.copy(genreFilter = com.echo.core.domain.model.GameGenre.RPG))?.first)
    }
}
