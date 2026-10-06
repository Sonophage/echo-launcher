package com.echo.feature.appbar

import com.echo.core.domain.model.GamepadAction
import kotlin.math.abs

// owner, 2026-10-05: three columns of cases beside a wider info column (was four, the design's 6a), so covers are
// big enough to read
const val WALL_COLUMNS = 3
const val WALL_ROWS = 2

data class WallCell(val row: Int, val col: Int)

fun wallLayout(total: Int, columns: Int = WALL_COLUMNS): List<WallCell> =
    List(total) { WallCell(it / columns, it % columns) }

// the d-pad walks the grid as it is drawn; up or down into a shorter row takes its nearest case, and nothing wraps
fun wallMove(action: GamepadAction, index: Int, cells: List<WallCell>): Int {
    if (cells.isEmpty()) return 0
    val at = index.coerceIn(0, cells.lastIndex)
    val cur = cells[at]
    fun cell(r: Int, c: Int) = cells.indexOfFirst { it.row == r && it.col == c }.takeIf { it >= 0 } ?: at
    fun nearestIn(r: Int) = cells.indices.filter { cells[it].row == r }.minByOrNull { abs(cells[it].col - cur.col) } ?: at
    return when (action) {
        GamepadAction.NAVIGATE_LEFT -> cell(cur.row, cur.col - 1)
        GamepadAction.NAVIGATE_RIGHT -> cell(cur.row, cur.col + 1)
        GamepadAction.NAVIGATE_UP -> nearestIn(cur.row - 1)
        GamepadAction.NAVIGATE_DOWN -> nearestIn(cur.row + 1)
        else -> at
    }
}
