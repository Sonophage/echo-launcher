package com.echo.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationEngineGeometryTest {
    private fun node(key: String, onSelect: (() -> Unit)? = null) = NavigationNode(key = key, onSelect = onSelect)

    @Test
    fun `currentGeometry exposes the geometry passed via replaceNodes`() {
        val engine = NavigationEngine()
        engine.replaceNodes(
            listOf(node("a"), node("b")),
            geometry = mapOf("a" to 100f, "b" to 200f),
        )
        assertEquals(mapOf("a" to 100f, "b" to 200f), engine.currentGeometry())
    }

    @Test
    fun `order fallback when geometry is unavailable`() {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a"), node("b"), node("c")))
        engine.markReady()
        assertEquals("a", engine.focusedKey)
        assertEquals("b", engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN)))
        assertEquals("c", engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN)))
    }

    @Test
    fun `removal falls back to order when no geometry existed`() {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a"), node("b"), node("c"), node("d")))
        engine.markReady()
        engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN))
        engine.dispatch(NavigationCommand.Direction(NavigationDirection.DOWN))
        engine.replaceNodes(listOf(node("a"), node("b"), node("d")))
        assertEquals("d", engine.focusedKey)
    }

}
