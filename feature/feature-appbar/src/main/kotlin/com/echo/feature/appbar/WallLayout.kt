package com.echo.feature.appbar

import com.echo.core.domain.model.GamepadAction

// owner, 2026-10-04: the focused app is the hero banner, so the wall below it is a plain grid in columns
const val WALL_COLUMNS = 6

data class WallCell(val row: Int, val col: Int, val span: Int = 1) {
    fun onRow(r: Int): Boolean = r in row until row + span
    fun gapTo(c: Int): Int = when {
        c < col -> col - c
        c >= col + span -> c - (col + span - 1)
        else -> 0
    }
}

fun wallLayout(total: Int, columns: Int = WALL_COLUMNS): List<WallCell> =
    List(total) { WallCell(it / columns, it % columns) }

fun wallMove(action: GamepadAction, index: Int, cells: List<WallCell>): Int {
    if (cells.isEmpty()) return 0
    val at = index.coerceIn(0, cells.lastIndex)
    val cur = cells[at]
    fun hit(r: Int, c: Int) = cells.indexOfFirst { it.onRow(r) && it.gapTo(c) == 0 }.takeIf { it >= 0 } ?: at
    return when (action) {
        GamepadAction.NAVIGATE_LEFT -> hit(cur.row, cur.col - 1)
        GamepadAction.NAVIGATE_RIGHT -> hit(cur.row, cur.col + cur.span)
        GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN -> {
            val up = action == GamepadAction.NAVIGATE_UP
            fun distance(c: WallCell) = if (up) cur.row - (c.row + c.span - 1) else c.row - (cur.row + cur.span - 1)
            cells.indices
                .filter { distance(cells[it]) > 0 }
                .minWithOrNull(compareBy<Int>({ cells[it].gapTo(cur.col) }, { distance(cells[it]) }))
                ?: at
        }
        else -> at
    }
}
