package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuGroup
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Spec #32, ADR 0002: the Companion's menu for an item is the Main Screen's menu for it, less Edit Layout (Move and
// Move Column are crossbar actions). Clear Recent is in Remove, so it stays. The menus are composed as
// openContextMenuFor does: the item's own rows, then CrossbarMove.menuRows for the slot the menu opens in.
class CompanionMenuTest {
    private val item = CrossbarItem(id = "g1", title = "Crisis Core", gameId = 1L, platformId = "psp")

    private fun added(column: String, slot: MenuSlot): List<CrossbarContextMenuItem> {
        val state = CrossbarUiState(
            categories = BUILT_IN_CATEGORIES,
            selectedCategoryIndex = BUILT_IN_CATEGORIES.indexOfFirst { it.id == column },
            currentItems = listOf(item),
        )
        return CrossbarMove(mockk(relaxed = true), MutableStateFlow(state), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            .menuRows(state, item, slot)
    }

    @Test
    fun `the Companion's menu is the Main Screen's for every item kind, less Edit Layout`() {
        // on Games the Main Screen adds Edit Layout only; on Recent it also adds Clear Recent, which the Companion keeps
        val companionAdds = mapOf(BuiltInCategory.GAMES to emptyList(), BuiltInCategory.RECENTLY_PLAYED to listOf("Clear Recent"))
        companionAdds.forEach { (column, kept) ->
            val mainAdds = added(column, MenuSlot.MAIN)
            assertTrue("$column: the Main Screen adds no Edit Layout row, so this test cannot fail", mainAdds.any { it.group == MenuGroup.EDIT_LAYOUT })
            ItemMenus.all.forEach { (kind, own) ->
                val main = own + mainAdds
                val companion = own + added(column, MenuSlot.COMPANION)
                assertEquals("$kind on $column: the Companion's rows", own.map { it.label } + kept, companion.map { it.label })
                assertTrue("$kind on $column: a Companion row is not on the Main Screen's menu", main.containsAll(companion))
            }
        }
    }
}
