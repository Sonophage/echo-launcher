package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.GamepadAction

data class XmbPrompt(
    val action: GamepadAction,
    val verb: String,
    val target: String? = null,
    val pairedWith: GamepadAction? = null,
)

data class XmbPrompts(
    val primary: XmbPrompt?,
    val back: XmbPrompt,
    val right: List<XmbPrompt>,
)

internal fun primaryVerbFor(item: XMBItem?): String? = when {
    item == null || item.type == XMBItemType.EMPTY -> null
    item.gameId != null -> "Play"
    item.type == XMBItemType.MUSIC_TRACK -> "Play"
    item.type == XMBItemType.VIDEO_FILE -> "Play"
    item.type == XMBItemType.LIBRARY_BOOK -> "Read"
    item.type == XMBItemType.PHOTO_FILE -> "View"
    item.type == XMBItemType.ADD_ACTION -> "Add"

    item.mediaRootUri != null -> "Manage"
    item.type == XMBItemType.SEARCH -> "Search"
    item.packageName != null -> "Launch"
    else -> "Open"
}

fun promptsFor(state: XMBUiState): XmbPrompts {
    val focused = state.currentItems.getOrNull(state.selectedItemIndex)

    if (state.notificationsOpen) {
        return XmbPrompts(
            primary = null,
            back = XmbPrompt(GamepadAction.BACK, "Close"),
            right = listOf(XmbPrompt(GamepadAction.PREV_CATEGORY, "Switch tab", pairedWith = GamepadAction.NEXT_CATEGORY)),
        )
    }

    state.activeContextMenu?.let { menu ->
        val row = menu.selectedIndex?.let { state.menuRows().getOrNull(it) }
        val primaryVerb = primaryVerbFor(focused)
        return XmbPrompts(
            primary = when {
                row != null -> XmbPrompt(GamepadAction.SELECT, "Select", row.label)

                menu.primaryId != null && primaryVerb != null ->
                    XmbPrompt(GamepadAction.SELECT, primaryVerb, focused?.title)
                else -> null
            },
            back = XmbPrompt(GamepadAction.BACK, if (menu.parent == null) "Close" else "Back"),
            right = emptyList(),
        )
    }

    val right = buildList {
        when {
            state.canFilterRecents -> add(XmbPrompt(GamepadAction.CHANGE_SORT, "Filter"))
            state.canSortCurrentList -> add(XmbPrompt(GamepadAction.CHANGE_SORT, "Sort"))
        }
        if (state.focusedItemHasContextMenu) add(XmbPrompt(GamepadAction.OPEN_CONTEXT_MENU, "Options"))
        if (!state.isInSubItem) add(XmbPrompt(GamepadAction.OPEN_SEARCH, "Search"))
    }

    return XmbPrompts(
        primary = primaryVerbFor(focused)?.let {
            XmbPrompt(GamepadAction.SELECT, it, focused?.title)
        },

        back = XmbPrompt(GamepadAction.BACK, if (state.isInSubItem) "Back" else "Apps"),
        right = right,
    )
}
