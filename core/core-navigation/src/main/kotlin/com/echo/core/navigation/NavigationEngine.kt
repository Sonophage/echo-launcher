package com.echo.core.navigation

class NavigationEngine(
    contextId: String = "root",
    private val logger: NavigationLogger = NavigationLogger.NONE,
) {
    private val contexts = ArrayDeque<NavigationContext>().apply {
        add(NavigationContext(contextId))
    }

    private var ready = false

    val focusedKey: String?
        get() = active.focusedKey

    val isModalActive: Boolean
        get() = contexts.size > 1

    val isEditing: Boolean
        get() = active.editHandler != null

    val acceptsInput: Boolean
        get() = ready

    var cursorVisible: Boolean = true
        private set

    fun markTouchInput() {
        cursorVisible = false
    }

    fun markControllerInput() {
        cursorVisible = true
    }

    fun dispatchTouch(key: String, action: NavigationTouchAction): Boolean {
        if (!ready) return false
        val node = active.findNode(key) ?: return false
        markTouchInput()
        active.setFocused(key)
        return when (action) {
            NavigationTouchAction.TAP -> active.confirm()
            NavigationTouchAction.LONG_PRESS -> node.onLongPress?.let { it(); true } ?: false
        }
    }

    private val active: NavigationContext
        get() = contexts.last()

    fun markReady() {
        ready = true
    }

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

    fun dispatch(command: NavigationCommand): String? {
        markControllerInput()

        if (!ready) return null


        val context = active
        val handler = context.editHandler

        if (handler != null) {
            when (command) {
                is NavigationCommand.Direction -> {
                    if (handler.onDirection(command.direction)) return null
                    return dispatchDirection(context, command.direction)
                }
                NavigationCommand.Confirm -> {
                    return if (handler.onConfirm()) null else context.focusedKey
                }
                is NavigationCommand.Back -> {
                    context.clearEditHandler()
                    return null
                }
            }
        }

        return when (command) {
            is NavigationCommand.Direction -> dispatchDirection(context, command.direction)
            NavigationCommand.Confirm -> {
                val key = context.focusedKey
                if (key != null) {
                    val editHandler = context.editHandlerFor(key)
                    if (editHandler != null) return null
                }
                if (context.confirm()) key else null
            }
            is NavigationCommand.Back -> {
                context.clearEditHandler()
                backHandler?.invoke()
                null
            }
        }
    }

    private fun dispatchDirection(context: NavigationContext, direction: NavigationDirection): String? {
        return when (direction) {
            NavigationDirection.UP -> context.moveVertical(-1)
            NavigationDirection.DOWN -> context.moveVertical(1)
            NavigationDirection.LEFT -> context.moveHorizontal(-1)
            NavigationDirection.RIGHT -> context.moveHorizontal(1)
        }
    }

    var backHandler: (() -> Unit)? = null

    fun confirmDirect(): Boolean = active.confirm()

    fun moveVerticalActive(delta: Int): String? = active.moveVertical(delta)

    fun moveHorizontalActive(delta: Int): String? = active.moveHorizontal(delta)
}
