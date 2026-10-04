package com.echo.feature.crossbar.ui.detail

import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The studio's three menus are drawn by EchoContextMenuOverlay, which sorts rows
 * through foldedIntoGroups() -- destructive rows move to the end. Activation
 * indexes the source list. So the source list must already be in drawn order,
 * or a tap runs the row next to the one the finger landed on.
 *
 * Found on hardware: tapping "Change Provider" ran "Change Match", and tapping
 * "View File Information" would have run "Clear Artwork".
 */
class StudioMenuOrderTest {
    private fun <T> drawn(rows: List<MenuRow<T>>): List<T?> =
        MenuState(title = "t", rows = rows).rowsShown().map { it.action }

    private fun rowsFor(actions: List<StudioAction>) = actions.map {
        MenuRow(it, it.label, isDestructive = it == StudioAction.CLEAR, confirms = false)
    }

    @Test
    fun `every action, in declaration order, draws in that order`() {
        val actions = StudioAction.entries.toList()
        assertEquals(actions, drawn(rowsFor(actions)), "activation indexes this list by the drawn row")
    }

    @Test
    fun `the list the studio actually builds draws in the order it was built`() {
        val state = ArtworkStudioUiState(
            currentUri = "content://tile",
            sgdbSourceActive = true,
            matchProvider = com.echo.feature.artwork.match.MatchProvider.STEAMGRIDDB,
        )
        val built = state.availableActions
        assertEquals(
            built,
            drawn(rowsFor(built)),
            "a tap on row i runs availableActions[i], so the two orders must agree",
        )
        assertEquals(StudioAction.CLEAR, built.last(), "the destructive row is drawn last")
    }

    @Test
    fun `the leave prompt is built in the order it is drawn`() {
        val choices = StudioLeaveChoice.entries
        val rows = choices.map {
            MenuRow(it, it.label, isDestructive = it == StudioLeaveChoice.DISCARD, confirms = false)
        }
        assertEquals(choices.toList(), drawn(rows), "Stay must not run Discard")
        assertEquals(StudioLeaveChoice.DISCARD, choices.last())
    }

    @Test
    fun `the replace prompt is built in the order it is drawn`() {
        val choices = StudioReplaceChoice.entries
        val rows = choices.map {
            MenuRow(it, it.label, isDestructive = it == StudioReplaceChoice.REPLACE, confirms = false)
        }
        assertEquals(choices.toList(), drawn(rows))
    }

    @Test
    fun `the crop options menu is built in the order it is drawn`() {
        val options = CropOption.entries
        val rows = options.map { MenuRow(it, it.name) }
        assertEquals(options.toList(), drawn(rows))
    }
}
