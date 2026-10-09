package com.echo.core.navigation

class NavigationEngine(contextId: String = "root") {
    private val contexts = ArrayDeque<NavigationContext>().apply {
        add(NavigationContext(contextId))
    }

    val focusedKey: String?
        get() = active.focusedKey

    private val active: NavigationContext
        get() = contexts.last()

    fun replaceNodes(nodes: List<NavigationNode>, geometry: Map<String, Float> = emptyMap()) {
        active.updateNodes(nodes, geometry)
    }

    fun focusFirst() {
        active.focusFirst()
    }

    fun setFocused(key: String?) {
        active.setFocused(key)
    }

    fun focusableKeys(): Set<String> = active.nodes.filter { it.focusable && it.enabled }.mapTo(mutableSetOf()) { it.key }

    fun currentGeometry(): Map<String, Float> = active.allGeometry()

    fun confirmDirect(): Boolean = active.confirm()

    fun moveVerticalActive(delta: Int): String? = active.moveVertical(delta)

    fun moveHorizontalActive(delta: Int): String? = active.moveHorizontal(delta)
}
