package com.echo.core.ui.components

import com.echo.core.domain.model.ControllerIcon
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EchoHintBarTest {
    private val back = ControllerPromptItem(GamepadAction.BACK, "Back")
    private val select = ControllerPromptItem(GamepadAction.SELECT, "Play")
    private val options = ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options")
    private val search = ControllerPromptItem(GamepadAction.OPEN_SEARCH, "Search")
    private val dpad = ControllerPromptItem.fixed(ControllerIcon.DPAD_ALL, "Scroll")
    private val play = HintAction(GamepadAction.SELECT, "Play", "Skyrim")

    // owner, 2026-10-07: Minimal hints drops the fixed button hints; touch keeps Back, its only way back
    @Test
    fun `minimal hints keep no button hint on a controller, and only Back on touch`() {
        val all = listOf(back, select, options, search, dpad)
        assertEquals(all, minimalHintItems(all, minimal = false, pad = true))
        assertEquals(emptyList<ControllerPromptItem>(), minimalHintItems(all, minimal = true, pad = true))
        assertEquals(listOf(back), minimalHintItems(all, minimal = true, pad = false))
    }

    @Test
    fun `the action tab's prompt is not drawn a second time in the row, and back closes the row`() {
        assertEquals(listOf(options, search, back), hintBarRow(listOf(back, select, options, search), play, pad = true))
    }

    @Test
    fun `without an action tab the confirm stays in the row, so nothing is lost`() {
        assertEquals(listOf(select, options, back), hintBarRow(listOf(back, select, options), null, pad = true))
    }

    @Test
    fun `a range prompt that includes confirm is not taken for the tab's prompt`() {
        val range = ControllerPromptItem(listOf(GamepadAction.SELECT, GamepadAction.BACK), "Seek")
        assertEquals(listOf(range, back), hintBarRow(listOf(range, back, select), play, pad = true))
    }

    @Test
    fun `after a tap only prompts a finger can press are shown, because a label with no glyph and no tap means nothing`() {
        val range = ControllerPromptItem(listOf(GamepadAction.SELECT, GamepadAction.BACK), "Seek")
        assertEquals(listOf(options, back), hintBarRow(listOf(dpad, range, back, options), null, pad = false))
        assertEquals(listOf(dpad, range, options, back), hintBarRow(listOf(dpad, range, back, options), null, pad = true))
    }

    @Test
    fun `the tab takes the confirm prompt's own label and the caller's detail`() {
        assertEquals(HintAction(GamepadAction.SELECT, "Enter", "Display"), primaryHint(listOf(back, select.copy(label = "Enter")), "Display"))
        assertEquals(HintAction(GamepadAction.SELECT, "Play"), primaryHint(listOf(select), " "))
    }

    @Test
    fun `a bar with no confirm has no tab, and a range prompt does not make one`() {
        val range = ControllerPromptItem(listOf(GamepadAction.SELECT, GamepadAction.BACK), "Seek")
        assertNull(primaryHint(listOf(dpad, range, back)))
    }

    @Test
    fun `Home and Back sit on the left on every screen, what the screen adds goes right of the card`() {
        val home = ControllerPromptItem(listOf(GamepadAction.HOME), "Home")
        val (left, right) = hintBarSides(listOf(home, options, search, back))
        assertEquals(listOf(home, back), left)
        assertEquals(listOf(options, search), right)
    }
}
