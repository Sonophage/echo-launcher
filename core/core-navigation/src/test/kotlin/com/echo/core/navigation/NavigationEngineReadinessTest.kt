package com.echo.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEngineReadinessTest {
    private fun node(key: String) = NavigationNode(key = key, onSelect = { })

    @Test
    fun `input ignored and not buffered before ready`() {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a"), node("b")))
        assertEquals("a", engine.focusedKey)

        assertEquals(null, engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN)))
        assertEquals("input must not move focus before ready", "a", engine.focusedKey)

        assertEquals(null, engine.dispatch(NavigationCommand.Confirm))
    }

    @Test
    fun `initial focus appears after ready`() {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a"), node("b")))
        engine.markReady()
        assertEquals("a", engine.focusedKey)
        assertEquals("b", engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN)))
    }

}
