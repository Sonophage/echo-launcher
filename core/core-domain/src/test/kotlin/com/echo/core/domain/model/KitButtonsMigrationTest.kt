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
        assertEquals(GamepadAction.OPEN_ISLAND, m.actionFor(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadAction.OPEN_NOTIFICATIONS, m.actionFor(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, m.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals("the hint draws Home as the guide button", ControllerIcon.SYSTEM, m.iconFor(GamepadAction.HOME))
    }

    @Test
    fun `the owner's own choices survive, a swapped X and Y stays swapped, a remapped key stays put`() {
        val m = oldSaved(
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            listOf(GamepadBinding(KeyEvent.KEYCODE_BUTTON_THUMBL, GamepadAction.CHANGE_SORT)),
        ).withKitButtons()
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, m.actionFor(KeyEvent.KEYCODE_BUTTON_X))
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

    // what controller_mappings_v1 held under the 2026-10-04 map: Y Search, Start Options, Select Home
    private fun savedBeforeSplit(searchKey: Int, sortKey: Int) = GamepadMappings(
        listOf(
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_A, GamepadAction.SELECT),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_B, GamepadAction.BACK),
            GamepadBinding(sortKey, GamepadAction.CHANGE_SORT),
            GamepadBinding(searchKey, GamepadAction.OPEN_SEARCH),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_L2, GamepadAction.PREV_CATEGORY),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_R2, GamepadAction.NEXT_CATEGORY),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_L1, GamepadAction.PREV_PAGE),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_R1, GamepadAction.NEXT_PAGE),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_MODE, GamepadAction.HOME),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_START, GamepadAction.OPEN_CONTEXT_MENU),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_SELECT, GamepadAction.HOME),
        ),
    )

    // owner, 2026-10-06: Y the context menu, Start the island, Select the notifications. A saved mapping
    // that kept the old roles would leave the pad disagreeing with every hint ECHO draws.
    @Test
    fun `a mapping saved under the old map moves Search, Options and Home to the new buttons`() {
        val m = savedBeforeSplit(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X).withKitButtons()
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, m.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(GamepadAction.CHANGE_SORT, m.actionFor(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GamepadAction.OPEN_ISLAND, m.actionFor(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadAction.OPEN_NOTIFICATIONS, m.actionFor(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals("the guide button stays Home", GamepadAction.HOME, m.actionFor(KeyEvent.KEYCODE_BUTTON_MODE))
        assertEquals("migrating twice changes nothing", m.bindings, m.withKitButtons().bindings)
        // the repository then trades Start and Select once (2026-10-07); after both, it is a fresh install
        val now = m.withStartSelectSwapped()
        now.bindings.forEach { assertEquals("key ${it.keyCode} matches a fresh install", GamepadMappings().actionFor(it.keyCode), it.action) }
    }

    @Test
    fun `a swapped X and Y stays swapped through the move`() {
        val m = savedBeforeSplit(KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y).withKitButtons()
        assertEquals(GamepadAction.OPEN_CONTEXT_MENU, m.actionFor(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GamepadAction.CHANGE_SORT, m.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(gamepadMappingsFor(ConfirmBackLayout.STANDARD, XYLayout.SWAPPED).actionFor(KeyEvent.KEYCODE_BUTTON_X), m.actionFor(KeyEvent.KEYCODE_BUTTON_X))
    }

    // owner, 2026-10-07: a mapping saved with the 2026-10-06 layout trades Start and Select; one the owner set
    // differently, or one already traded, is left alone
    @Test
    fun `the old Start and Select layout trades, any other stays`() {
        val old = GamepadMappings(listOf(
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_START, GamepadAction.OPEN_ISLAND),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_SELECT, GamepadAction.OPEN_NOTIFICATIONS),
        )).withStartSelectSwapped()
        assertEquals(GamepadAction.OPEN_NOTIFICATIONS, old.actionFor(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadAction.OPEN_ISLAND, old.actionFor(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals("already traded stays", old, old.withStartSelectSwapped())

        val own = GamepadMappings(listOf(
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_START, GamepadAction.OPEN_SEARCH),
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_SELECT, GamepadAction.OPEN_NOTIFICATIONS),
        ))
        assertEquals(own, own.withStartSelectSwapped())
    }

    // owner, 2026-10-08: L3 and R3 swap the screens; a saved map gains them once, and a stick bound on purpose stays
    @Test
    fun `a saved map gains Swap Screens on both stick clicks, and keeps a stick bound to something else`() {
        val m = savedBeforeSplit(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X).withKitButtons()
        assertEquals(GamepadAction.SWAP_SCREENS, m.actionFor(KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertEquals(GamepadAction.SWAP_SCREENS, m.actionFor(KeyEvent.KEYCODE_BUTTON_THUMBR))

        val own = GamepadMappings(DEFAULT_BINDINGS.filterNot { it.action == GamepadAction.SWAP_SCREENS } +
            GamepadBinding(KeyEvent.KEYCODE_BUTTON_THUMBL, GamepadAction.OPEN_SEARCH)).withKitButtons()
        assertEquals(GamepadAction.OPEN_SEARCH, own.actionFor(KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertEquals(GamepadAction.SWAP_SCREENS, own.actionFor(KeyEvent.KEYCODE_BUTTON_THUMBR))
    }
}
