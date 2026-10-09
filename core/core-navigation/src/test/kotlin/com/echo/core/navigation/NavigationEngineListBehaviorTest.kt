package com.echo.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEngineListBehaviorTest {
    private fun node(
        key: String,
        focusable: Boolean = true,
        selectable: Boolean = true,
        enabled: Boolean = true,
        onSelect: (() -> Unit)? = null,
        children: List<NavigationNode> = emptyList(),
    ) = NavigationNode(
        key = key,
        focusable = focusable,
        selectable = selectable,
        enabled = enabled,
        onSelect = onSelect,
        children = children,
    )

    private fun engineWith(vararg nodes: NavigationNode): NavigationEngine {
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(*nodes))
        return engine
    }

    @Test
    fun `empty list has no focus, no movement and no selection`() {
        val engine = NavigationEngine()
        assertNull(engine.focusedKey)
        assertNull(engine.moveVerticalActive(1))
        assertNull(engine.moveVerticalActive(-1))
        assertFalse(engine.confirmDirect())
    }

    @Test
    fun `updateNodes focuses the first navigable node`() {
        val engine = engineWith(node("a"), node("b"))
        assertEquals("a", engine.focusedKey)
    }

    @Test
    fun `up and down movement traverse the list in order`() {
        val engine = engineWith(node("a"), node("b"), node("c"))
        assertEquals("b", engine.moveVerticalActive(1))
        assertEquals("c", engine.moveVerticalActive(1))
        assertEquals("b", engine.moveVerticalActive(-1))
        assertEquals("a", engine.moveVerticalActive(-1))
    }

    @Test
    fun `movement clamps at both boundaries - no wrapping`() {
        val engine = engineWith(node("a"), node("b"))
        assertEquals("a", engine.moveVerticalActive(-1))
        assertEquals("b", engine.moveVerticalActive(1))
        assertEquals("b", engine.moveVerticalActive(1))
    }

    @Test
    fun `navigation skips non focusable landmarks`() {
        val engine = engineWith(node("hdr", focusable = false), node("a"), node("b"))
        assertEquals("a", engine.focusedKey)
        assertEquals("b", engine.moveVerticalActive(1))
        assertEquals("a", engine.moveVerticalActive(-1))
        assertEquals("a", engine.moveVerticalActive(-1))
    }

    @Test
    fun `confirm dispatches to the focused node action`() {
        val engine = engineWith(
            node("a", onSelect = { }),
            node("b", onSelect = { }),
        )
        assertTrue(engine.confirmDirect())
        engine.moveVerticalActive(1)
        assertTrue(engine.confirmDirect())
    }

    @Test
    fun `confirm ignores non selectable read-only rows`() {
        val engine = engineWith(node("ro", selectable = false, onSelect = { }))
        assertFalse(engine.confirmDirect())
    }

    @Test
    fun `movement skips disabled nodes`() {
        val engine = engineWith(node("a"), node("disabled", enabled = false), node("b"))
        assertEquals("b", engine.moveVerticalActive(1))
        assertEquals("a", engine.moveVerticalActive(-1))
    }

    @Test
    fun `confirm is a no-op on a disabled node`() {
        val engine = engineWith(node("a"), node("disabled", enabled = false, onSelect = { }))
        engine.setFocused("disabled")
        assertFalse(engine.confirmDirect())
    }

    @Test
    fun `stable key preserves focus across list updates`() {
        val engine = engineWith(node("a"), node("b"), node("c"))
        engine.moveVerticalActive(1)
        engine.replaceNodes(listOf(node("a"), node("b"), node("c"), node("d")))
        assertEquals("b", engine.focusedKey)
        engine.replaceNodes(listOf(node("x"), node("b"), node("c")))
        assertEquals("b", engine.focusedKey)
    }

    @Test
    fun `removing the focused node recovers to the nearest survivor by order`() {
        val engine = engineWith(node("a"), node("b"), node("c"), node("d"))
        engine.moveVerticalActive(1)
        engine.moveVerticalActive(1)
        engine.replaceNodes(listOf(node("a"), node("b"), node("d")))
        assertEquals("d", engine.focusedKey)
    }

    @Test
    fun `removing the focused first node falls back to the first survivor`() {
        val engine = engineWith(node("a"), node("b"), node("c"))
        engine.replaceNodes(listOf(node("b"), node("c")))
        assertEquals("b", engine.focusedKey)
    }

    @Test
    fun `setFocused aligns the focused key and ignores unknown keys`() {
        val engine = engineWith(node("a"), node("b"))
        engine.setFocused("b")
        assertEquals("b", engine.focusedKey)
        engine.setFocused("nope")
        assertEquals("b", engine.focusedKey)
        engine.setFocused(null)
        assertNull(engine.focusedKey)
    }

    @Test
    fun `confirm invokes the updated callback after a node changes`() {
        var selected = ""
        val engine = NavigationEngine()
        engine.replaceNodes(listOf(node("a", onSelect = { selected = "first" })))
        engine.replaceNodes(listOf(node("a", onSelect = { selected = "second" })))
        assertTrue(engine.confirmDirect())
        assertEquals("second", selected)
    }

    @Test
    fun `horizontal movement enters inline children and returns to the row`() {
        val engine = engineWith(
            node("row", onSelect = {}, children = listOf(
                node("row:a", onSelect = { }),
                node("row:b"),
            )),
        )
        assertEquals("row", engine.focusedKey)
        assertEquals("row:a", engine.moveHorizontalActive(1))
        assertEquals("row:b", engine.moveHorizontalActive(1))
        assertNull(engine.moveHorizontalActive(1))
        assertEquals("row:b", engine.focusedKey)
        assertEquals("row:a", engine.moveHorizontalActive(-1))
        assertEquals("row", engine.moveHorizontalActive(-1))
        assertNull(
            "LEFT from the row moves nothing and must report null, so a caller can fall through to what is left of it",
            engine.moveHorizontalActive(-1),
        )
        assertEquals("row", engine.focusedKey)
    }

    @Test
    fun `horizontal movement is a no-op on rows without children`() {
        val engine = engineWith(node("a"), node("b"))
        assertNull(engine.moveHorizontalActive(1))
        assertNull(engine.moveHorizontalActive(-1))
    }

    @Test
    fun `vertical movement exits a child to its row first`() {
        val engine = engineWith(
            node("top"),
            node("row", children = listOf(node("row:a"))),
            node("bottom"),
        )
        engine.setFocused("row:a")
        assertEquals("bottom", engine.moveVerticalActive(1))
        engine.setFocused("row:a")
        assertEquals("top", engine.moveVerticalActive(-1))
    }

    @Test
    fun `child focus survives a list update that keeps its row`() {
        val engine = engineWith(node("row", children = listOf(node("row:a"))))
        engine.setFocused("row:a")
        engine.replaceNodes(listOf(node("row", children = listOf(node("row:a")))))
        assertEquals("row:a", engine.focusedKey)
    }

    @Test
    fun `child focus recovers to its row when the child disappears`() {
        val engine = engineWith(node("row", children = listOf(node("row:a"))))
        engine.setFocused("row:a")
        engine.replaceNodes(listOf(node("row")))
        assertEquals("row", engine.focusedKey)
    }
}
