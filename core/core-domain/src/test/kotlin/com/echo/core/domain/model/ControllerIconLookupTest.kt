package com.echo.core.domain.model

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerIconLookupTest {
    private fun mappings(confirmBack: ConfirmBackLayout, xy: XYLayout) =
        gamepadMappingsFor(confirmBack, xy)

    private val standard = mappings(ConfirmBackLayout.STANDARD, XYLayout.STANDARD)

    @Test
    fun `face keycodes map to face positions`() {
        assertEquals(ControllerIcon.FACE_SOUTH, KeyEvent.KEYCODE_BUTTON_A.toControllerIcon())
        assertEquals(ControllerIcon.FACE_EAST, KeyEvent.KEYCODE_BUTTON_B.toControllerIcon())
        assertEquals(ControllerIcon.FACE_WEST, KeyEvent.KEYCODE_BUTTON_X.toControllerIcon())
        assertEquals(ControllerIcon.FACE_NORTH, KeyEvent.KEYCODE_BUTTON_Y.toControllerIcon())
    }

    @Test
    fun `shoulder and trigger keycodes map to their positions`() {
        assertEquals(ControllerIcon.BUMPER_LEFT, KeyEvent.KEYCODE_BUTTON_L1.toControllerIcon())
        assertEquals(ControllerIcon.BUMPER_RIGHT, KeyEvent.KEYCODE_BUTTON_R1.toControllerIcon())
        assertEquals(ControllerIcon.TRIGGER_LEFT, KeyEvent.KEYCODE_BUTTON_L2.toControllerIcon())
        assertEquals(ControllerIcon.TRIGGER_RIGHT, KeyEvent.KEYCODE_BUTTON_R2.toControllerIcon())
    }

    @Test
    fun `dpad, stick-click and utility keycodes map to their positions`() {
        assertEquals(ControllerIcon.DPAD_UP, KeyEvent.KEYCODE_DPAD_UP.toControllerIcon())
        assertEquals(ControllerIcon.DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN.toControllerIcon())
        assertEquals(ControllerIcon.DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT.toControllerIcon())
        assertEquals(ControllerIcon.DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT.toControllerIcon())
        assertEquals(ControllerIcon.STICK_LEFT_CLICK, KeyEvent.KEYCODE_BUTTON_THUMBL.toControllerIcon())
        assertEquals(ControllerIcon.STICK_RIGHT_CLICK, KeyEvent.KEYCODE_BUTTON_THUMBR.toControllerIcon())
        assertEquals(ControllerIcon.START, KeyEvent.KEYCODE_BUTTON_START.toControllerIcon())
        assertEquals(ControllerIcon.SELECT, KeyEvent.KEYCODE_BUTTON_SELECT.toControllerIcon())
    }

    @Test
    fun `keys that are not on a gamepad have no position`() {
        assertNull(KeyEvent.KEYCODE_ENTER.toControllerIcon())
        assertNull(KeyEvent.KEYCODE_BACK.toControllerIcon())
        assertNull(KeyEvent.KEYCODE_DPAD_CENTER.toControllerIcon())
        assertNull(KeyEvent.KEYCODE_SPACE.toControllerIcon())
    }

    @Test
    fun `confirm and back follow the Confirm-Back setting`() {
        assertEquals(ControllerIcon.FACE_SOUTH, standard.iconFor(GamepadAction.SELECT))
        assertEquals(ControllerIcon.FACE_EAST, standard.iconFor(GamepadAction.BACK))

        val reversed = mappings(ConfirmBackLayout.REVERSED, XYLayout.STANDARD)
        assertEquals(ControllerIcon.FACE_EAST, reversed.iconFor(GamepadAction.SELECT))
        assertEquals(ControllerIcon.FACE_SOUTH, reversed.iconFor(GamepadAction.BACK))
    }

    @Test
    fun `search follows the X-Y setting, as Options did before the kit's buttons`() {
        assertEquals(ControllerIcon.FACE_NORTH, standard.iconFor(GamepadAction.OPEN_SEARCH))

        val swapped = mappings(ConfirmBackLayout.STANDARD, XYLayout.SWAPPED)
        assertEquals(ControllerIcon.FACE_WEST, swapped.iconFor(GamepadAction.OPEN_SEARCH))
    }

    @Test
    fun `Options sits on the menu button under every layout, as the kit's hint row draws it`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                assertEquals(ControllerIcon.START, mappings(confirmBack, xy).iconFor(GamepadAction.OPEN_CONTEXT_MENU))
            }
        }
    }

    @Test
    fun `sort follows the X-Y setting`() {
        assertEquals(ControllerIcon.FACE_WEST, standard.iconFor(GamepadAction.CHANGE_SORT))

        val swapped = mappings(ConfirmBackLayout.STANDARD, XYLayout.SWAPPED)
        assertEquals(ControllerIcon.FACE_NORTH, swapped.iconFor(GamepadAction.CHANGE_SORT))
    }

    @Test
    fun `the two settings compose independently`() {
        val both = mappings(ConfirmBackLayout.REVERSED, XYLayout.SWAPPED)
        assertEquals(ControllerIcon.FACE_EAST, both.iconFor(GamepadAction.SELECT))
        assertEquals(ControllerIcon.FACE_SOUTH, both.iconFor(GamepadAction.BACK))
        assertEquals(ControllerIcon.FACE_WEST, both.iconFor(GamepadAction.OPEN_SEARCH))
        assertEquals(ControllerIcon.FACE_NORTH, both.iconFor(GamepadAction.CHANGE_SORT))
    }

    @Test
    fun `tabs and filters sit on the triggers, and the bumpers page, regardless of layout`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val m = mappings(confirmBack, xy)
                assertEquals(ControllerIcon.TRIGGER_LEFT, m.iconFor(GamepadAction.PREV_CATEGORY))
                assertEquals(ControllerIcon.TRIGGER_RIGHT, m.iconFor(GamepadAction.NEXT_CATEGORY))
                assertEquals(ControllerIcon.BUMPER_LEFT, m.iconFor(GamepadAction.PREV_PAGE))
                assertEquals(ControllerIcon.BUMPER_RIGHT, m.iconFor(GamepadAction.NEXT_PAGE))
            }
        }
    }

    @Test
    fun `SELECT resolves to a face button, never to one of its keyboard aliases`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val icon = mappings(confirmBack, xy).iconFor(GamepadAction.SELECT)
                assertTrue(
                    "SELECT resolved to $icon, which is not a face button",
                    icon == ControllerIcon.FACE_SOUTH || icon == ControllerIcon.FACE_EAST,
                )
            }
        }
    }

    @Test
    fun `BACK resolves to a face button, never to the hardware Back key`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val icon = mappings(confirmBack, xy).iconFor(GamepadAction.BACK)
                assertTrue(
                    "BACK resolved to $icon, which is not a face button",
                    icon == ControllerIcon.FACE_SOUTH || icon == ControllerIcon.FACE_EAST,
                )
            }
        }
    }

    @Test
    fun `a resolved icon is always reachable from the action it came from`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val m = mappings(confirmBack, xy)
                for (action in GamepadAction.entries) {
                    val icon = m.iconFor(action) ?: continue
                    val keys = m.bindings
                        .filter { it.keyCode.toControllerIcon() == icon }
                        .map { it.action }
                    assertTrue(
                        "$action renders as $icon but that button dispatches $keys",
                        action in keys,
                    )
                }
            }
        }
    }

    @Test
    fun `an unbound action has no prompt rather than a wrong one`() {
        val stripped = GamepadMappings(
            standard.bindings.filterNot { it.action == GamepadAction.CHANGE_SORT },
        )
        assertNull(stripped.iconFor(GamepadAction.CHANGE_SORT))

        assertNotNull(stripped.iconFor(GamepadAction.SELECT))
    }

    @Test
    fun `an action bound only to a keyboard alias has no prompt`() {
        val keyboardOnly = GamepadMappings(
            listOf(GamepadBinding(KeyEvent.KEYCODE_ENTER, GamepadAction.SELECT)),
        )
        assertNull(keyboardOnly.iconFor(GamepadAction.SELECT))
    }

    @Test
    fun `a multi-input prompt keeps the order it was asked for`() {
        assertEquals(
            listOf(ControllerIcon.DPAD_LEFT, ControllerIcon.DPAD_RIGHT),
            standard.iconsFor(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT)),
        )
        assertEquals(
            listOf(ControllerIcon.DPAD_RIGHT, ControllerIcon.DPAD_LEFT),
            standard.iconsFor(listOf(GamepadAction.NAVIGATE_RIGHT, GamepadAction.NAVIGATE_LEFT)),
        )
    }

    @Test
    fun `an unbound member drops out and the rest of the prompt survives`() {
        val stripped = GamepadMappings(
            standard.bindings.filterNot { it.action == GamepadAction.NEXT_CATEGORY },
        )
        assertEquals(
            listOf(ControllerIcon.TRIGGER_LEFT),
            stripped.iconsFor(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY)),
        )
    }

    @Test
    fun `a prompt whose every member is unbound resolves to nothing`() {
        val keyboardOnly = GamepadMappings(
            listOf(GamepadBinding(KeyEvent.KEYCODE_ENTER, GamepadAction.SELECT)),
        )
        assertTrue(keyboardOnly.iconsFor(listOf(GamepadAction.SELECT)).isEmpty())
    }

    @Test
    fun `two actions on one button draw that button once`() {
        val collapsed = GamepadMappings(
            listOf(
                GamepadBinding(KeyEvent.KEYCODE_BUTTON_A, GamepadAction.SELECT),
                GamepadBinding(KeyEvent.KEYCODE_BUTTON_A, GamepadAction.BACK),
            ),
        )
        assertEquals(
            listOf(ControllerIcon.FACE_SOUTH),
            collapsed.iconsFor(listOf(GamepadAction.SELECT, GamepadAction.BACK)),
        )
    }

    @Test
    fun `each member agrees with the single-action lookup`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val m = mappings(confirmBack, xy)
                val actions = GamepadAction.entries.toList()
                assertEquals(
                    actions.mapNotNull { m.iconFor(it) }.distinct(),
                    m.iconsFor(actions),
                )
            }
        }
    }

    @Test
    fun `the video transport bar resolves under every layout combination`() {
        val transport = listOf(
            listOf(GamepadAction.SELECT),
            listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT),
            listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY),
            listOf(GamepadAction.OPEN_CONTEXT_MENU),
        )
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val m = mappings(confirmBack, xy)
                for (prompt in transport) {
                    assertEquals(
                        "$prompt lost a glyph under $confirmBack/$xy",
                        prompt.size,
                        m.iconsFor(prompt).size,
                    )
                }
            }
        }
    }

    @Test
    fun `Home, and the pickers' Add, draw the guide button, which the Echo mark stands in for`() {
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                assertEquals(
                    ControllerIcon.SYSTEM,
                    mappings(confirmBack, xy).iconFor(GamepadAction.HOME),
                )
            }
        }
    }

    @Test
    fun `the drawer command bar resolves under every layout combination`() {
        val commandBar = listOf(
            GamepadAction.PREV_CATEGORY,
            GamepadAction.NEXT_CATEGORY,
            GamepadAction.BACK,
            GamepadAction.SELECT,
            GamepadAction.OPEN_CONTEXT_MENU,
            GamepadAction.CHANGE_SORT,
        )
        for (confirmBack in ConfirmBackLayout.entries) {
            for (xy in XYLayout.entries) {
                val m = mappings(confirmBack, xy)
                val icons = commandBar.map { action ->
                    requireNotNull(m.iconFor(action)) { "$action has no prompt under $confirmBack/$xy" }
                }
                assertEquals(
                    "$confirmBack/$xy draws the same glyph twice in the command bar",
                    commandBar.size,
                    icons.toSet().size,
                )
            }
        }
    }
}
