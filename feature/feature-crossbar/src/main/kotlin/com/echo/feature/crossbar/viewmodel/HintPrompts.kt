package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction

data class CrossbarPrompt(
    val action: GamepadAction,
    val verb: String,
    val target: String? = null,
    val pairedWith: GamepadAction? = null,
    val detail: String? = null,
)

data class CrossbarPrompts(
    val primary: CrossbarPrompt?,
    val back: CrossbarPrompt,
    val right: List<CrossbarPrompt>,
)

internal fun primaryVerbFor(item: CrossbarItem?): String? = when {
    item == null || item.type == CrossbarItemType.EMPTY -> null
    item.gameId != null -> "Play"
    item.type == CrossbarItemType.MUSIC_TRACK -> "Play"
    item.type == CrossbarItemType.VIDEO_FILE -> "Play"
    item.type == CrossbarItemType.LIBRARY_BOOK -> "Read"
    item.type == CrossbarItemType.PHOTO_FILE -> "View"
    item.type == CrossbarItemType.ADD_ACTION -> "Add"

    item.mediaRootUri != null -> "Manage"
    item.type == CrossbarItemType.SEARCH -> "Search"
    item.packageName != null -> "Launch"
    else -> "Open"
}

fun promptsFor(state: CrossbarUiState): CrossbarPrompts {
    val focused = state.currentItems.getOrNull(state.selectedItemIndex)

    if (state.notificationsOpen) {
        return CrossbarPrompts(
            primary = null,
            back = CrossbarPrompt(GamepadAction.BACK, "Close"),
            right = listOf(CrossbarPrompt(GamepadAction.PREV_CATEGORY, "Switch tab", pairedWith = GamepadAction.NEXT_CATEGORY)),
        )
    }

    state.activeContextMenu?.let { menu ->
        val row = menu.selectedIndex?.let { state.menuRows().getOrNull(it) }
        val primaryVerb = primaryVerbFor(focused)
        return CrossbarPrompts(
            primary = when {
                row != null -> CrossbarPrompt(GamepadAction.SELECT, "Select", row.label)

                menu.primaryId != null && primaryVerb != null ->
                    CrossbarPrompt(GamepadAction.SELECT, primaryVerb, focused?.title, detail = focused?.subtitle)
                else -> null
            },
            back = CrossbarPrompt(GamepadAction.BACK, if (menu.parent == null) "Close" else "Back"),
            right = emptyList(),
        )
    }

    val right = buildList {
        when {
            state.canFilterRecents -> add(CrossbarPrompt(GamepadAction.PREV_CATEGORY, "Filter", pairedWith = GamepadAction.NEXT_CATEGORY))
            state.canSortCurrentList -> add(CrossbarPrompt(GamepadAction.CHANGE_SORT, "Sort"))
        }
        if (state.focusedItemHasContextMenu) add(CrossbarPrompt(GamepadAction.OPEN_CONTEXT_MENU, "Options"))
        if (!state.isInSubItem) add(CrossbarPrompt(GamepadAction.OPEN_SEARCH, "Search"))
    }

    return CrossbarPrompts(
        primary = primaryVerbFor(focused)?.takeIf { !state.onLastPlayedHome }?.let {
            CrossbarPrompt(GamepadAction.SELECT, it, focused?.title, detail = focused?.subtitle)
        },
        back = CrossbarPrompt(GamepadAction.BACK, if (state.isInSubItem || (state.onLastPlayedHome && state.recentRailVisible)) "Back" else "Apps"),
        right = right,
    )
}
