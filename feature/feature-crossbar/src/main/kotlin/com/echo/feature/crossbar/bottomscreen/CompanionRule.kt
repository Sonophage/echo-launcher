package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.typing
import com.echo.feature.crossbar.viewmodel.ColorSchemeOption
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.settings.ui.SettingsFocusInfo
import com.echo.feature.settings.ui.StorePreview

// what the Companion Screen shows (ADR 0001)
sealed interface CompanionContent {
    // an item's Details; while it is the game ECHO launched and that game runs, with the session clock and Resume
    data class Details(val info: GameInfoState, val playing: Boolean = false, val playingSince: Long? = null) : CompanionContent

    // the setting focused in Settings on the Main Screen, its description in large type, and a preview of the
    // focused choice where there is one
    data class Setting(val info: SettingsFocusInfo, val preview: SettingPreview? = null) : CompanionContent
}

// a preview of the choice focused in Settings
sealed interface SettingPreview {
    // the theme focused in the Theme Store
    data class Theme(val store: StorePreview) : SettingPreview

    // the colour scheme highlighted in the picker
    data class Colors(val option: ColorSchemeOption) : SettingPreview
}

// the Companion rule: the running game, else the colour scheme highlighted in the picker, else in Settings the
// theme focused in the Theme Store or the focused setting, else the item an open menu belongs to (the one shown
// when it opened), else the hovered item (the App Drawer's over the Crossbar's, Search's result over both), else
// what it showed [last], so it never goes blank. A game kept after it ends loses its clock and Resume.
fun companionContent(main: BottomScreenState, last: CompanionContent?): CompanionContent? {
    main.playing?.let { return CompanionContent.Details(it, playing = true, playingSince = main.playingSince) }
    val kept = (last as? CompanionContent.Details)?.copy(playing = false, playingSince = null) ?: last
    main.open.colorScheme?.let { return CompanionContent.Setting(SettingsFocusInfo(it.label, null, it.sublabel), SettingPreview.Colors(it)) }
    if (main.open.settings) {
        main.theme?.let { return CompanionContent.Setting(SettingsFocusInfo(it.name, null, it.line), SettingPreview.Theme(it)) }
        return main.setting?.let { CompanionContent.Setting(it) } ?: kept
    }
    // ponytail: the owner is what was shown when the menu opened; a menu opened within the 150 ms the cursor
    // takes to settle keeps the item before it, until the menu closes. Track the menu's own item if that shows.
    if (main.open.menu && kept != null) return kept
    val hovered = if (main.open.search) main.hovered else main.drawer ?: main.hovered
    return hovered?.let { CompanionContent.Details(it) } ?: kept
}

// what the Main Screen has hovered, besides the App Drawer (which reports its own): Search's focused result while
// Search is open, else the Crossbar's cursor
fun mainScreenHover(s: CrossbarUiState): CrossbarItem? =
    s.search?.let { it.rows.getOrNull(it.selectedIndex) } ?: s.focusedItem.takeIf { s.search == null }

// the context menu the Main Screen draws: every menu, with or without a Companion Screen (ADR 0001). CrossbarShell
// draws by this, so MainScreenRollbackGuardTest sees it
fun mainScreenMenu(s: CrossbarUiState): com.echo.feature.crossbar.viewmodel.CrossbarContextMenu? = s.activeContextMenu

// what is open on the Main Screen, which the Companion rule reads
fun mainScreenOpen(s: CrossbarUiState): MainScreenOpen = MainScreenOpen(
    search = s.search != null,
    menu = s.activeContextMenu != null,
    settings = s.activeSettingsScreen != null,
    colorScheme = s.colorSchemePicker?.let { it.options.getOrNull(it.selectedIndex) },
)

// the items with Details on the Companion Screen: games, installed apps, songs, videos, books, photos and a
// system's card. A settings row or a folder has none, so hovering one keeps the last thing shown
internal fun companionShows(item: CrossbarItem): Boolean = when {
    item.gameId != null -> true
    item.isAndroidApp && item.packageName != null -> true
    item.type == CrossbarItemType.MEMORY_CARD -> item.platformId != null
    else -> item.type in MEDIA_TYPES
}

private val MEDIA_TYPES = setOf(
    CrossbarItemType.MUSIC_TRACK, CrossbarItemType.MUSIC_GROUP, CrossbarItemType.VIDEO_FILE,
    CrossbarItemType.LIBRARY_BOOK, CrossbarItemType.PHOTO_FILE,
)

// a game or app is drawn as Game Info; a song, video, book, photo or system as Recent draws an item in full view
internal fun showsGameInfo(item: CrossbarItem): Boolean = item.gameId != null || item.isAndroidApp

// Swap (L3, R3 or the Swap button). With the Companion Screen on a second display, the two screens change displays,
// remembered across restarts; with one display (or one screen chosen in Quick settings), the Companion Screen is
// shown in place of the Main Screen until the next Swap, held in memory only, so ECHO always starts on the Main
// Screen (ADR 0001)
sealed interface Swap {
    data class Displays(val swapped: Boolean) : Swap
    data class InPlace(val shown: Boolean) : Swap
}

fun swap(s: CrossbarUiState): Swap =
    if (s.secondDisplayPresent && s.secondScreenEnabled) Swap.Displays(!s.screensSwapped) else Swap.InPlace(!s.companionInPlace)

// what the Companion Screen's window does with key focus. The Thor draws the keyboard on the Companion Screen's
// display (ime_show_on_second), and only for the window last focused there, so the Companion's window must take
// focus for it to show. CLAIM: typing on the Main Screen, it takes focus once and gives it back, so the controller
// and the field stay on the Main Screen (ADR 0001). HOLD: in Swap the field is in this window, which keeps the keys
// while typing. NONE: touch only.
enum class CompanionKeys { NONE, CLAIM, HOLD }

fun companionKeys(s: CrossbarUiState): CompanionKeys = when {
    !s.typing -> CompanionKeys.NONE
    s.screensSwapped -> CompanionKeys.HOLD
    else -> CompanionKeys.CLAIM
}

// the Companion's CLAIM has ended while typing with a Companion attached and the Main Screen's window has focus: the
// claim moved the focused display away and back, which hid the keyboard, so the Main Screen shows it again for its
// field. The window keeps its own focus through the claim (seen on the Thor), so the end comes through the link
fun reshowsKeyboard(windowFocused: Boolean, s: CrossbarUiState): Boolean =
    windowFocused && s.secondScreen && companionKeys(s) == CompanionKeys.CLAIM

// the display ECHO started on shows the Companion Screen: swapped with the Companion Screen attached on the other
// display, or shown in place on one display
fun firstDisplayShowsCompanion(s: CrossbarUiState): Boolean = mainScreenOnSecondDisplay(s) || s.companionInPlace

// Swap has put the Main Screen on the second display, with the Companion Screen attached on the first
fun mainScreenOnSecondDisplay(s: CrossbarUiState): Boolean = s.secondScreen && s.screensSwapped

// a Companion Screen is on show: attached on a second display, or shown in place by Swap on one
fun companionOnShow(attached: Boolean, companionInPlace: Boolean): Boolean = attached || companionInPlace
