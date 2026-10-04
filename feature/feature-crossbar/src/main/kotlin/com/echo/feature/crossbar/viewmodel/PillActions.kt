package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction

data class CrossbarPill(val id: String, val label: String)

internal fun pillsFor(item: CrossbarItem): List<CrossbarPill> = when {
    item.gameId != null -> listOf(
        CrossbarPill("play", "Play"),
        CrossbarPill("shelves", "Shelves"),
    )

    item.packageName != null &&
        item.platformId == null &&
        item.type == CrossbarItemType.STANDARD -> listOf(
        CrossbarPill("launch", "Launch"),
        CrossbarPill("edit_app", "Edit"),
        CrossbarPill("favorite", "Favorite"),
    )

    else -> emptyList()
}

// how long A or a finger must hold this pill: Launch and Play leave ECHO, so they take the item's
// launch hold like A on the row; the other pills act at once (owner, 2026-10-04)
internal fun pillHoldMs(item: CrossbarItem, pill: CrossbarPill): Long =
    if (pill.id == "launch" || pill.id == "play") holdMsFor(item) else 0L

internal sealed interface PillNav {
    data class Move(val index: Int) : PillNav

    data object ExitAndPass : PillNav

    data object Pass : PillNav
}

internal fun pillNav(action: GamepadAction, current: Int?, count: Int): PillNav {
    if (count <= 0) return PillNav.Pass
    return when (action) {
        GamepadAction.NAVIGATE_RIGHT -> when {
            current == null -> PillNav.Pass
            current < count - 1 -> PillNav.Move(current + 1)
            else -> PillNav.ExitAndPass
        }

        GamepadAction.NAVIGATE_LEFT -> when {
            current == null -> PillNav.Pass
            current > 0 -> PillNav.Move(current - 1)
            else -> PillNav.ExitAndPass
        }

        GamepadAction.NAVIGATE_DOWN -> if (current == null) PillNav.Move(0) else PillNav.Pass
        else -> PillNav.Pass
    }
}

internal enum class DownStep { LeaveRowAndStepItem, EnterRow, EnterColumn, StepItem }

internal fun downStep(inPillRow: Boolean, pillRowVisible: Boolean, inColumn: Boolean = true, hasPills: Boolean = false): DownStep = when {
    inPillRow -> DownStep.LeaveRowAndStepItem
    !inColumn && hasPills -> DownStep.EnterColumn
    pillRowVisible -> DownStep.EnterRow
    else -> DownStep.StepItem
}

data class PillCursor(val itemId: String, val index: Int)

val CrossbarUiState.pillRowVisible: Boolean
    get() = !onLastPlayedHome && inColumn && focusedPills().isNotEmpty()

val CrossbarItem.isInstalledApp: Boolean get() = gameId == null && packageName != null

fun CrossbarUiState.focusedPills(): List<CrossbarPill> =
    currentItems.getOrNull(selectedItemIndex)?.let(::pillsFor).orEmpty()

val CrossbarUiState.focusedPillIndex: Int? get() = activePillIndex()

internal fun CrossbarUiState.activePillIndex(): Int? {
    val item = currentItems.getOrNull(selectedItemIndex) ?: return null
    val cursor = pillCursor?.takeIf { it.itemId == item.id } ?: return null
    val pills = pillsFor(item)
    if (pills.isEmpty()) return null
    return cursor.index.coerceIn(0, pills.lastIndex)
}
