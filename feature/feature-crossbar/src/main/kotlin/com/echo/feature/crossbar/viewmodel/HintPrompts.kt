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

    // the action orb's level 2 (kit 06): Y Resume beside A Play
    val resume: CrossbarPrompt? = null,
)

// the focused game is the one ECHO just sent away, so Y can take you back into it
internal fun CrossbarUiState.resumableFocus(): CrossbarItem? =
    currentItems.getOrNull(selectedItemIndex)?.takeIf { it.gameId != null && it.isRealGame && it.gameId == resumeGameId }

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

// Last Played's verb, which the action orb carries (kit: "Continue Skyrim", "Resume 14:02")
internal fun recentVerbFor(item: CrossbarItem): String = when (recentKind(item)) {
    RecentKind.GAME, RecentKind.BOOK -> "Continue"
    RecentKind.VIDEO -> if (item.progressFraction != null) "Resume" else "Play"
    RecentKind.MUSIC -> "Play"
    RecentKind.APP -> "Open"
}

// X on Last Played: the stage opens info, the rail removes the row
private fun recentInfoPrompt(state: CrossbarUiState, item: CrossbarItem): CrossbarPrompt? = when {
    state.recentRailVisible -> CrossbarPrompt(GamepadAction.CHANGE_SORT, "Remove").takeIf { item.removableFromRecent }
    recentKind(item) == RecentKind.GAME -> CrossbarPrompt(GamepadAction.CHANGE_SORT, "Game info")
    recentKind(item) == RecentKind.APP -> CrossbarPrompt(GamepadAction.CHANGE_SORT, "App info")
    else -> null
}

fun promptsFor(state: CrossbarUiState): CrossbarPrompts {
    val focused = state.currentItems.getOrNull(state.selectedItemIndex)

    if (state.notificationsOpen) {
        // kit 11: the focused notice's actions are the footer's: A on the card, X Dismiss, Y Clear all.
        // The tab row already shows its shoulder buttons, so the footer does not repeat them
        val stage = state.panelStage()
        val actions = if (state.panelTab == PanelTab.NOTIFICATIONS) stageActions(stage, state.clearableNoticeCount) else emptyList()
        val target = (stage as? PanelStage.Android)?.notice?.appLabel ?: (stage as? PanelStage.Launcher)?.let { "Launcher" }
        return CrossbarPrompts(
            primary = actions.firstOrNull { it.button == GamepadAction.SELECT }?.let { CrossbarPrompt(it.button, it.label, target) },
            back = CrossbarPrompt(GamepadAction.BACK, "Close"),
            right = actions.filter { it.button != GamepadAction.SELECT }.map { CrossbarPrompt(it.button, it.label) },
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

    val resumable = state.resumableFocus()
    // owner, 2026-10-04: the top bar shows the filter row with its triggers and the sort row with X,
    // so the footer does not repeat Filter or Sort
    val right = buildList {
        if (state.onLastPlayedHome) focused?.let { recentInfoPrompt(state, it) }?.let(::add)
        if (state.focusedItemHasContextMenu) add(CrossbarPrompt(GamepadAction.OPEN_CONTEXT_MENU, "Options"))
        if (!state.isInSubItem) add(CrossbarPrompt(GamepadAction.PREV_PAGE, "Search"))
    }

    return CrossbarPrompts(
        primary = when {
            resumable != null -> CrossbarPrompt(GamepadAction.SELECT, "Play", "New session")
            else -> (if (state.onLastPlayedHome) focused?.takeIf { it.type != CrossbarItemType.EMPTY }?.let(::recentVerbFor) else primaryVerbFor(focused))?.let {
                CrossbarPrompt(GamepadAction.SELECT, it, focused?.title, detail = focused?.subtitle)
            }
        },
        resume = resumable?.let { CrossbarPrompt(GamepadAction.OPEN_SEARCH, "Resume", it.title) },
        back = CrossbarPrompt(GamepadAction.BACK, if (state.isInSubItem || (state.onLastPlayedHome && state.recentRailVisible)) "Back" else "Apps"),
        right = right,
    )
}
