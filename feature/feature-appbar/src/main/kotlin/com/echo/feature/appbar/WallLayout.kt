package com.echo.feature.appbar

import com.echo.core.domain.model.GamepadAction

const val WALL_COLUMNS = 3

data class WallCell(val row: Int, val col: Int, val span: Int = 1) {
    fun onRow(r: Int): Boolean = r in row until row + span
    fun gapTo(c: Int): Int = when {
        c < col -> col - c
        c >= col + span -> c - (col + span - 1)
        else -> 0
    }
}

fun wallLayout(sectionCount: Int, total: Int, columns: Int = WALL_COLUMNS): List<WallCell> {
    val section = sectionCount.coerceIn(0, total)
    val beside = columns - 2
    val cells = ArrayList<WallCell>(total)
    if (section > 0) cells += WallCell(0, 0, span = 2)
    for (slot in 0 until section - 1) {
        cells += if (slot < 2 * beside) WallCell(slot / beside, 2 + slot % beside)
        else (slot - 2 * beside).let { WallCell(2 + it / columns, it % columns) }
    }
    val restRow = if (section > 0) maxOf(2, cells.last().row + 1) else 0
    for (i in 0 until total - section) cells += WallCell(restRow + i / columns, i % columns)
    return cells
}

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
