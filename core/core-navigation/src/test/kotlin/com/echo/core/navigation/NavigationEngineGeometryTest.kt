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
        assertEquals("a", engine.focusedKey)
        assertEquals("b", engine.moveVerticalActive(1))
        assertEquals("c", engine.moveVerticalActive(1))
    }

    @Test
    fun `removal falls back to order when no geometry existed`() {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a"), node("b"), node("c"), node("d")))
        engine.moveVerticalActive(1)
        engine.moveVerticalActive(1)
        engine.replaceNodes(listOf(node("a"), node("b"), node("d")))
        assertEquals("d", engine.focusedKey)
    }

}
