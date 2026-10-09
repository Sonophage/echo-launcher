package com.echo.core.navigation

enum class NavigationDirection { UP, DOWN, LEFT, RIGHT }

data class NavigationNode(
    val key: String,
    val focusable: Boolean = true,
    val selectable: Boolean = true,
    val enabled: Boolean = true,

    val onSelect: (() -> Unit)? = null,

    val children: List<NavigationNode> = emptyList(),
)
