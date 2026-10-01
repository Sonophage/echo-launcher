package com.psplauncher.feature.xmb.viewmodel

data class LibraryChip(val id: String, val name: String, val visible: Boolean)

val LIBRARY_CHIP_IDS = listOf(
    com.psplauncher.core.domain.model.BuiltInCategory.GAMES,
    com.psplauncher.core.domain.model.BuiltInCategory.MUSIC,
    com.psplauncher.core.domain.model.BuiltInCategory.VIDEO,
    com.psplauncher.core.domain.model.BuiltInCategory.LIBRARY,
    com.psplauncher.core.domain.model.BuiltInCategory.PHOTO,
    "network",
)

enum class QuickSetting { WAVE, BACKDROP, RECENT_APPS, LIBRARIES }

enum class PanelMove { UP, DOWN, LEFT, RIGHT }

data class PanelCursor(
    val quick: QuickSetting? = null,
    val chip: Int = 0,
    val notice: Int = 0,
)

fun movePanel(
    cursor: PanelCursor,
    move: PanelMove,
    hasMedia: Boolean,
    rightRows: Int,
    chips: Int,
): PanelCursor {
    val firstNotice = if (hasMedia) 1 else 0
    val noticeRows = rightRows - firstNotice
    val quick = cursor.quick
    if (quick != null) {
        val i = quick.ordinal
        return when (move) {
            PanelMove.UP -> when {
                i > 0 -> cursor.copy(quick = QuickSetting.entries[i - 1])
                hasMedia -> cursor.copy(quick = null, notice = 0)
                else -> cursor
            }
            PanelMove.DOWN -> cursor.copy(quick = QuickSetting.entries[(i + 1).coerceAtMost(QuickSetting.entries.lastIndex)])
            PanelMove.LEFT ->
                if (quick == QuickSetting.LIBRARIES && cursor.chip > 0) cursor.copy(chip = cursor.chip - 1) else cursor
            PanelMove.RIGHT -> when {
                quick == QuickSetting.LIBRARIES && cursor.chip < chips - 1 -> cursor.copy(chip = cursor.chip + 1)
                noticeRows > 0 -> cursor.copy(quick = null, notice = firstNotice + i.coerceAtMost(noticeRows - 1))
                else -> cursor
            }
        }
    }
    val onMedia = hasMedia && cursor.notice == 0
    return when (move) {
        PanelMove.UP -> if (cursor.notice > firstNotice) cursor.copy(notice = cursor.notice - 1)
            else if (!onMedia && hasMedia) cursor.copy(notice = 0) else cursor
        PanelMove.DOWN -> when {
            onMedia -> cursor.copy(quick = QuickSetting.WAVE)
            cursor.notice < rightRows - 1 -> cursor.copy(notice = cursor.notice + 1)
            else -> cursor
        }
        PanelMove.LEFT -> if (onMedia) cursor
            else cursor.copy(quick = QuickSetting.entries[(cursor.notice - firstNotice).coerceIn(0, QuickSetting.entries.lastIndex)])
        PanelMove.RIGHT -> cursor
    }
}
