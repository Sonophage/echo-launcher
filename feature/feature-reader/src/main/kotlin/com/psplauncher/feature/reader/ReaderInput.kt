package com.psplauncher.feature.reader

import com.psplauncher.core.domain.model.GamepadAction

enum class OptionsTab(val label: String) { CONTENTS("Contents"), BOOKMARKS("Bookmarks"), DISPLAY("Display") }

enum class DisplayRow { TEXT_SIZE, TYPEFACE, PAGE, LAYOUT }

sealed interface ReaderCommand {
    data object PageForward : ReaderCommand
    data object PageBackward : ReaderCommand
    data object NextChapter : ReaderCommand
    data object PreviousChapter : ReaderCommand
    data object OpenOptions : ReaderCommand
    data object CloseOptions : ReaderCommand
    data object Close : ReaderCommand
    data object ToggleBookmark : ReaderCommand
    data class MoveCursor(val delta: Int) : ReaderCommand
    data class SwitchTab(val delta: Int) : ReaderCommand
    data class Adjust(val delta: Int) : ReaderCommand
    data object Activate : ReaderCommand
}

fun readingCommand(action: GamepadAction): ReaderCommand? = when (action) {
    GamepadAction.NAVIGATE_RIGHT, GamepadAction.NAVIGATE_DOWN, GamepadAction.NEXT_PAGE -> ReaderCommand.PageForward
    GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_UP, GamepadAction.PREV_PAGE -> ReaderCommand.PageBackward
    GamepadAction.NEXT_CATEGORY -> ReaderCommand.NextChapter
    GamepadAction.PREV_CATEGORY -> ReaderCommand.PreviousChapter
    GamepadAction.OPEN_CONTEXT_MENU, GamepadAction.SELECT -> ReaderCommand.OpenOptions
    GamepadAction.CHANGE_SORT -> ReaderCommand.ToggleBookmark
    GamepadAction.BACK -> ReaderCommand.Close
    else -> null
}

fun optionsCommand(action: GamepadAction, tab: OptionsTab): ReaderCommand? = when (action) {
    GamepadAction.NAVIGATE_UP -> ReaderCommand.MoveCursor(-1)
    GamepadAction.NAVIGATE_DOWN -> ReaderCommand.MoveCursor(+1)
    GamepadAction.NAVIGATE_LEFT -> if (tab == OptionsTab.DISPLAY) ReaderCommand.Adjust(-1) else null
    GamepadAction.NAVIGATE_RIGHT -> if (tab == OptionsTab.DISPLAY) ReaderCommand.Adjust(+1) else null
    GamepadAction.PREV_CATEGORY -> ReaderCommand.SwitchTab(-1)
    GamepadAction.NEXT_CATEGORY -> ReaderCommand.SwitchTab(+1)
    GamepadAction.SELECT -> ReaderCommand.Activate
    GamepadAction.CHANGE_SORT -> ReaderCommand.ToggleBookmark
    GamepadAction.BACK, GamepadAction.OPEN_CONTEXT_MENU -> ReaderCommand.CloseOptions
    else -> null
}

fun adjustDisplay(display: ReaderDisplay, row: DisplayRow, delta: Int): ReaderDisplay = when (row) {
    DisplayRow.TEXT_SIZE -> display.withTextScale(delta * ReaderDisplay.TEXT_SCALE_STEP)
    DisplayRow.TYPEFACE -> display.copy(typeface = display.typeface.cycle(delta))
    DisplayRow.PAGE -> display.copy(page = display.page.cycle(delta))
    DisplayRow.LAYOUT -> display.copy(layout = display.layout.cycle(delta))
}

private inline fun <reified E : Enum<E>> E.cycle(delta: Int): E {
    val all = enumValues<E>()
    return all[(ordinal + delta).mod(all.size)]
}
