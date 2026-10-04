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
}
