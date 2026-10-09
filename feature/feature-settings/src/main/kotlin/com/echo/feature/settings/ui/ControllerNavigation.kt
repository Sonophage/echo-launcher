package com.echo.feature.settings.ui

import com.echo.core.navigation.NavigationEngine
import com.echo.core.navigation.NavigationNode
import kotlin.math.abs

data class ControllerNavItem(
    val key: String,
    val focusable: Boolean = true,
    val selectable: Boolean = true,
    val enabled: Boolean = true,
    val onSelect: (() -> Unit)? = null,

    val trailingActions: List<ControllerNavItem> = emptyList(),
)

internal fun ControllerNavItem.toNavigationNode(): NavigationNode = NavigationNode(
    key = key,
    focusable = focusable,
    selectable = selectable,
    enabled = enabled,
    onSelect = onSelect,

    children = trailingActions.map { it.toNavigationNode() },
)

class ControllerNavigationState(
    private val engine: NavigationEngine = NavigationEngine("settings"),
) {
    var focusedKey: String? = null
        private set

    fun focusNearestTo(y: Float): String? {
        val target = engine.currentGeometry()
            .filterKeys { key -> key in engine.focusableKeys() }
            .minByOrNull { (_, nodeY) -> abs(nodeY - y) }
            ?.key
        if (target != null) engine.setFocused(target)
        focusedKey = engine.focusedKey
        return focusedKey
    }

    fun updateItems(
        newItems: List<ControllerNavItem>,
        geometry: Map<String, Float> = emptyMap(),
    ) {
        engine.replaceNodes(newItems.map { it.toNavigationNode() }, geometry)
        focusedKey = engine.focusedKey
    }

    fun move(delta: Int): String? {
        focusedKey = engine.moveVerticalActive(delta)
        return focusedKey
    }

    fun moveHorizontal(delta: Int): String? {
        val moved = engine.moveHorizontalActive(delta)
        focusedKey = engine.focusedKey
        return moved
    }

    fun focusFirst(): String? {
        engine.focusFirst()
        focusedKey = engine.focusedKey
        return focusedKey
    }

    fun setFocused(key: String?) {
        engine.setFocused(key)
        focusedKey = engine.focusedKey
    }

    fun select(): Boolean = engine.confirmDirect()
}
