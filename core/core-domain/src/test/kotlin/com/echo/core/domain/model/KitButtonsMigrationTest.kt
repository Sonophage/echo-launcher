package com.echo.core.domain.model

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KitButtonsMigrationTest {
    // what controller_mappings_v1 held before the kit's buttons
    private fun oldSaved(optionsKey: Int, sortKey: Int, extra: List<GamepadBinding> = emptyList()) = GamepadMappings(
        listOf(
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_A, GamepadAction.SELECT),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_B, GamepadAction.BACK),
            GamepadBinding(sortKey, GamepadAction.CHANGE_SORT),
            GamepadBinding(optionsKey, GamepadAction.OPEN_CONTEXT_MENU),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_START, GamepadAction.HOME),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_SELECT, GamepadAction.OPEN_SEARCH),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_L1, GamepadAction.PREV_CATEGORY),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_R1, GamepadAction.NEXT_CATEGORY),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_L2, GamepadAction.PREV_PAGE),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_R2, GamepadAction.NEXT_PAGE),
        ) + extra,
    )

    @Test
    fun `a mapping saved before the kit gets the kit's buttons, or the hints would lie about the pad`() {
        val m = oldSaved(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X).withKitButtons()
        assertEquals(GamepadAction.HOME, m.actionFor(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, m.actionFor(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadAction.HOME, m.actionFor(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(GamepadAction.OPEN_SEARCH, m.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals("the hint draws Home as the guide button", ControllerIcon.SYSTEM, m.iconFor(GamepadAction.HOME))
    }

    @Test
    fun `the owner's own choices survive, a swapped X and Y stays swapped, a remapped key stays put`() {
        val m = oldSaved(
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            listOf(GamepadBinding(KeyEvent.KEYCODE_BUTTON_THUMBL, GamepadAction.CHANGE_SORT)),
        ).withKitButtons()
        assertEquals(GamepadAction.OPEN_SEARCH, m.actionFor(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GamepadAction.CHANGE_SORT, m.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(GamepadAction.CHANGE_SORT, m.actionFor(KeyEvent.KEYCODE_BUTTON_THUMBL))
    }

    @Test
    fun `a current mapping is left exactly as it is`() {
        val current = GamepadMappings()
        assertSame(current, current.withKitButtons())
        assertEquals(gamepadMappingsFor(ConfirmBackLayout.STANDARD, XYLayout.STANDARD).bindings, GamepadMappings().bindings)
    }

    @Test
    fun `a saved mapping moves its tabs to the triggers and its paging to the bumpers`() {
        val m = oldSaved(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X).withKitButtons()
        assertEquals(GamepadAction.PREV_CATEGORY, m.actionFor(KeyEvent.KEYCODE_BUTTON_L2))
        assertEquals(GamepadAction.NEXT_CATEGORY, m.actionFor(KeyEvent.KEYCODE_BUTTON_R2))
        assertEquals(GamepadAction.PREV_PAGE, m.actionFor(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals(GamepadAction.NEXT_PAGE, m.actionFor(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals("migrating twice changes nothing", m.bindings, m.withKitButtons().bindings)
    }
}
