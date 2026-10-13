package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BUILT_IN_CATEGORIES
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.chose
import com.echo.core.ui.components.rowsShown
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The layout rows appended to every Crossbar menu are one Edit Layout submenu, and Clear Recent is in Remove and
// still asks first (owner, 2026-10-12). The menus are composed as openContextMenuFor does: the item's rows, then
// CrossbarMove.menuRows.
class EditLayoutTest {
    private val layout = setOf(MOVE_ROW, MOVE_COLUMN, RENAME_COLUMN, CHANGE_COLUMN_ICON, CHANGE_SYSTEM_ICON, CHANGE_SYSTEM_ART)

    private fun layoutRows(categoryId: String, item: CrossbarItem): List<CrossbarContextMenuItem> {
        val state = CrossbarUiState(
            categories = BUILT_IN_CATEGORIES,
            selectedCategoryIndex = BUILT_IN_CATEGORIES.indexOfFirst { it.id == categoryId },
            currentItems = listOf(item),
        )
        return CrossbarMove(mockk(relaxed = true), MutableStateFlow(state), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            .menuRows(state, item, MenuSlot.MAIN)
    }

    private val game = CrossbarItem(id = "g1", title = "Crisis Core", gameId = 1L, platformId = "psp")
    private val system = CrossbarItem(id = "ps2", title = "PlayStation 2", platformId = "ps2", type = CrossbarItemType.MEMORY_CARD)
    private val state = CrossbarUiState(categories = BUILT_IN_CATEGORIES)

    // every kind of item's own rows, with the focused item the layout rows are built for
    private val everyMenu: Map<String, Pair<CrossbarItem, List<CrossbarContextMenuItem>>> = mapOf(
        "game" to (game to gameContextMenuItems(game, state, discCount = 1, onRecentShelf = false, hideLocation = null)),
        "app" to (game to appContextMenuItems(state, categoryId = null, onRecentShelf = false)),
        "video file" to (game to videoFileContextMenuItems(false, 0L, hasWatchStamp = true, inPlaylist = true)),
        "photo" to (game to photoFileContextMenuItems()),
        "book" to (game to bookContextMenuItems(hasOpenStamp = true)),
        "music track" to (game to musicTrackContextMenuItems(playlistId = 7L, hasPlayStamp = true)),
        "album" to (game to albumContextMenuItems()),
        "recent album" to (game to recentAlbumContextMenuItems()),
        "system" to (system to platformContextMenuItems("ps2", pinned = false)),
        "All Games" to (game to allGamesContextMenuItems()),
        "media folder" to (game to mediaRootContextMenuItems(linked = true, kind = MediaRootKind.MUSIC)),
        "media folders card" to (game to mediaFoldersContextMenuItems(MediaRootKind.MUSIC)),
        "a row with no menu of its own" to (game to emptyList()),
    )

    private fun opened(select: MenuSelect<String>): MenuState<String> =
        (select as? MenuSelect.Replace)?.state ?: throw AssertionError("the row did not open anything: $select")

    private fun MenuState<String>.choseLabel(label: String): MenuSelect<String> {
        val index = rowsShown().indexOfFirst { it.label == label }
        assertTrue("'$label' is not on ${rowsShown().map { it.label }}", index >= 0)
        return chose(index)
    }

    @Test
    fun `no layout row is at the top level of any Crossbar menu, they are all in Edit Layout`() {
        everyMenu.forEach { (name, menu) ->
            val (item, rows) = menu
            val added = layoutRows(BuiltInCategory.GAMES, item)
            assertTrue("$name: fewer than two layout rows, so nothing folds: $added", added.size >= 2)
            val top = MenuState(title = name, rows = rows + added)
            val atTop = top.rowsShown().mapNotNull { it.action }.filter { it in layout }
            assertTrue("$name: layout rows at the top level: $atTop", atTop.isEmpty())
            assertEquals(
                "$name: Edit Layout does not hold the layout rows",
                added.mapNotNull { it.action }.toSet(),
                opened(top.choseLabel("Edit Layout")).rowsShown().mapNotNull { it.action }.toSet(),
            )
        }
    }

    @Test
    fun `Clear Recent is inside Remove and still asks first`() {
        val rows = gameContextMenuItems(game, state, discCount = 1, onRecentShelf = true, hideLocation = null) +
            layoutRows(BuiltInCategory.RECENTLY_PLAYED, game)
        val top = MenuState(title = "Crisis Core", rows = rows)
        assertTrue("Clear Recent is at the top level", top.rowsShown().none { it.action == CLEAR_RECENT })
        val confirm = opened(opened(top.choseLabel("Remove")).choseLabel("Clear Recent"))
        assertEquals("Clear Recent ran without a confirm", "Clear Recent?", confirm.subtitle)
    }
}
