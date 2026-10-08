package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.GamepadAction
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.RecentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class BottomScreenState(
    // the game or app under the XMB's cursor
    val focused: GameInfoState? = null,
    // the game ECHO launched, while it runs in front of ECHO
    val playing: GameInfoState? = null,
    // when that game was launched, for the session clock (owner, 2026-10-08)
    val playingSince: Long? = null,
    // the Recent page: the Last Played screen, which leaves the top screen for this one (owner, 2026-10-06)
    val recent: List<CrossbarItem> = emptyList(),
    val recentFilters: List<RecentFilter> = listOf(RecentFilter.ALL),
    val recentFilter: RecentFilter = RecentFilter.ALL,
    val recentSelected: Int = 0,
    // Recent's list beside the item (true) or the item in full view, as the top screen's Last Played shows it
    val recentListOpen: Boolean = true,
    val page: BottomPage = BottomPage.INFO,
    // music is playing, so the companion offers its remote (owner, 2026-10-08)
    val music: Boolean = false,
)

class DrawerFocus(
    val app: com.echo.feature.appbar.InstalledApp,
    val onLaunch: () -> Unit,
    val onOptions: () -> Unit,
)

// the page in view: Info stands down for Recent while there is no info to show, so the button lit is
// the page drawn and the controller moves what is on screen
fun BottomScreenState.shownPage(): BottomPage = if (page in pages()) page else BottomPage.RECENT

// the pages the companion can show now, in pill order: Info when there is info, Recent always, Music while
// something plays
fun BottomScreenState.pages(): List<BottomPage> = listOfNotNull(
    BottomPage.INFO.takeIf { shownInfo() != null },
    BottomPage.RECENT,
    BottomPage.MUSIC.takeIf { music },
)

// the companion fits the crossbar's column (owner, 2026-10-08): Recent shows that column's kind, when Recent holds
// any; any other column, or a kind with nothing in it, shows All. A gaming column of your own counts as games
fun recentFilterFor(categoryId: String?, gaming: Boolean, shown: List<com.echo.feature.crossbar.viewmodel.RecentFilter>): com.echo.feature.crossbar.viewmodel.RecentFilter {
    val kind = when {
        categoryId == com.echo.core.domain.model.BuiltInCategory.MUSIC -> com.echo.feature.crossbar.viewmodel.RecentFilter.MUSIC
        categoryId == com.echo.core.domain.model.BuiltInCategory.VIDEO -> com.echo.feature.crossbar.viewmodel.RecentFilter.VIDEO
        categoryId == com.echo.core.domain.model.BuiltInCategory.LIBRARY -> com.echo.feature.crossbar.viewmodel.RecentFilter.BOOKS
        categoryId == com.echo.core.domain.model.BuiltInCategory.ANDROID -> com.echo.feature.crossbar.viewmodel.RecentFilter.APPS
        gaming -> com.echo.feature.crossbar.viewmodel.RecentFilter.GAMES
        else -> null
    }
    return kind?.takeIf { it in shown } ?: com.echo.feature.crossbar.viewmodel.RecentFilter.ALL
}

enum class MusicRemoteKey { PLAY_PAUSE, PREV, NEXT, LEAVE, OPTIONS }

// the music remote: A plays or pauses, up and down skip, B leaves the player for Recent with the music still
// playing, Y opens the player's options (Stop & Close among them) on this screen. Left and right stay the pages'
fun musicRemoteKey(action: GamepadAction): MusicRemoteKey? = when (action) {
    GamepadAction.SELECT -> MusicRemoteKey.PLAY_PAUSE
    GamepadAction.NAVIGATE_UP -> MusicRemoteKey.PREV
    GamepadAction.NAVIGATE_DOWN -> MusicRemoteKey.NEXT
    GamepadAction.BACK -> MusicRemoteKey.LEAVE
    GamepadAction.OPEN_CONTEXT_MENU -> MusicRemoteKey.OPTIONS
    else -> null
}

// left and right on the companion: on Recent they first close the list to show the item in full, and open it
// again, as on the top screen (recentRailStep); past that they walk the pages (owner, 2026-10-08: the game's
// info in full view, on either screen)
fun BottomScreenState.sideStep(action: GamepadAction): BottomScreenState {
    val left = action == GamepadAction.NAVIGATE_LEFT
    if (shownPage() == BottomPage.RECENT) {
        when (com.echo.feature.crossbar.viewmodel.recentRailStep(action, onLastPlayedHome = true, railVisible = recentListOpen)) {
            com.echo.feature.crossbar.viewmodel.RailStep.Open -> return copy(recentListOpen = true)
            com.echo.feature.crossbar.viewmodel.RailStep.Close -> return copy(recentListOpen = false)
            com.echo.feature.crossbar.viewmodel.RailStep.Pass -> Unit
        }
    }
    return copy(page = steppedPage(if (left) -1 else 1))
}

// left and right walk the pages there are, round the ends
fun BottomScreenState.steppedPage(delta: Int): BottomPage {
    val all = pages()
    return all[(all.indexOf(shownPage()) + delta).mod(all.size)]
}

// owner, 2026-10-06: on a device with a second screen, the bottom screen follows the top one. It is a
// second activity with no view model of its own: it draws the crossbar's state and acts through the
// crossbar, as the top screen does, so the App Drawer, Search and Settings have one owner on both.
@Singleton
class BottomScreenLink @Inject constructor() {
    private val _state = MutableStateFlow(BottomScreenState())
    val state: StateFlow<BottomScreenState> = _state.asStateFlow()

    // the crossbar of the running ECHO, while it lives
    private val _crossbar = MutableStateFlow<CrossbarViewModel?>(null)
    val crossbar: StateFlow<CrossbarViewModel?> = _crossbar.asStateFlow()

    // the bottom screen is on show, so the crossbar has something to keep current
    private val _attached = MutableStateFlow(false)
    val attached: StateFlow<Boolean> = _attached.asStateFlow()

    // ECHO's own screen is in front on the top display
    private val _hostShown = MutableStateFlow(true)
    val hostShown: StateFlow<Boolean> = _hostShown.asStateFlow()

    fun update(change: (BottomScreenState) -> BottomScreenState) = _state.update(change)

    // the App Drawer's focused app while the drawer is open on the companion, for the crossbar's screen to draw
    // large (owner, 2026-10-08), with the drawer's own Play and Options for a tap on it
    private val _drawerFocus = MutableStateFlow<DrawerFocus?>(null)
    val drawerFocus: StateFlow<DrawerFocus?> = _drawerFocus.asStateFlow()

    fun drawerFocused(focus: DrawerFocus?) { _drawerFocus.value = focus }

    // the theme the store has in focus on the companion, for the crossbar's screen (owner, 2026-10-08)
    private val _storePreview = MutableStateFlow<com.echo.feature.settings.ui.StorePreview?>(null)
    val storePreview: StateFlow<com.echo.feature.settings.ui.StorePreview?> = _storePreview.asStateFlow()

    fun storePreviewed(preview: com.echo.feature.settings.ui.StorePreview?) { _storePreview.value = preview }

    fun attach(on: Boolean) { _attached.value = on }

    fun hostShown(shown: Boolean) { _hostShown.value = shown }

    fun bind(crossbar: CrossbarViewModel) { _crossbar.value = crossbar }

    // a crossbar going away clears only itself, not one that has replaced it
    fun unbind(crossbar: CrossbarViewModel) { _crossbar.compareAndSet(crossbar, null) }
}

// the info the bottom screen shows: the running game while it is in front, else the cursor's item.
// None means the Recent shelf stands in.
fun BottomScreenState.shownInfo(): GameInfoState? = playing ?: focused

// the companion's session clock: "NOW PLAYING · 12 MIN", in hours past an hour; minutes round down, so a game
// just launched reads "JUST STARTED"
fun sessionLabel(since: Long?, now: Long): String {
    val minutes = since?.let { (now - it) / 60_000L } ?: return "NOW PLAYING"
    return when {
        minutes < 1 -> "NOW PLAYING · JUST STARTED"
        minutes < 60 -> "NOW PLAYING · $minutes MIN"
        else -> "NOW PLAYING · ${minutes / 60} H ${minutes % 60} MIN"
    }
}

// a game counts as playing while ECHO is behind the game it launched last
fun playingGameId(hostShown: Boolean, lastLaunchGameId: Long?): Long? = lastLaunchGameId.takeIf { !hostShown }
