package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-10: Clear Recent empties the shelf from any of its rows, after a confirm; what is used again
// comes back
class ClearRecentTest {
    private fun on(categoryId: String): Pair<CrossbarUiState, CrossbarItem> {
        val item = CrossbarItem(id = "g1", title = "Crisis Core", gameId = 1L)
        val categories = BUILT_IN_CATEGORIES
        return CrossbarUiState(
            categories = categories,
            selectedCategoryIndex = categories.indexOfFirst { it.id == categoryId },
            currentItems = listOf(item),
        ) to item
    }

    private fun rowsFor(categoryId: String) = on(categoryId).let { (state, item) ->
        CrossbarMove(mockk(relaxed = true), MutableStateFlow(state), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            .menuRows(state, item)
    }

    @Test
    fun `a row on Recent offers Clear Recent, and it asks first`() {
        val row = rowsFor(BuiltInCategory.RECENTLY_PLAYED).firstOrNull { it.action == CLEAR_RECENT }
        assertTrue("Clear Recent is not on a Recent row's menu", row != null)
        assertTrue("Clear Recent runs without a confirm", row!!.confirms)
    }

    @Test
    fun `a row elsewhere does not`() {
        assertFalse(rowsFor(BuiltInCategory.GAMES).any { it.action == CLEAR_RECENT })
    }

    @Test
    fun `only what was used after the clear is kept`() {
        val used = listOf(100L, 200L, null, 300L)
        assertEquals(listOf(300L), used.usedSince(200L) { it })
        assertEquals("never cleared keeps all", used, used.usedSince(0L) { it })
    }
}
