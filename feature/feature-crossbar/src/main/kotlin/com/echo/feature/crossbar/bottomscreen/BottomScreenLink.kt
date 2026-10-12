package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.GameInfoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

// the Main Screen's state the Companion Screen follows, and what it shows from it (companionContent)
data class BottomScreenState(
    // the item hovered on the Main Screen: the Crossbar's cursor, or Search's focused result while Search is open
    val hovered: GameInfoState? = null,
    // the app hovered in the App Drawer while it is open on the Main Screen (ADR 0001)
    val drawer: GameInfoState? = null,
    // Search is open on the Main Screen, over the drawer when it was opened from there
    val searching: Boolean = false,
    // a context menu is open on the Main Screen
    val menuOpen: Boolean = false,
    // Settings is open on the Main Screen
    val settingsOpen: Boolean = false,
    // the setting focused there; null while nothing is, or a touch has hidden the cursor
    val setting: com.echo.feature.settings.ui.SettingsFocusInfo? = null,
    // the theme focused in the Theme Store
    val theme: com.echo.feature.settings.ui.StorePreview? = null,
    // the colour scheme highlighted in the picker, while it is open
    val colorScheme: com.echo.feature.crossbar.viewmodel.ColorSchemeOption? = null,
    // the game ECHO launched, while it runs in front of ECHO
    val playing: GameInfoState? = null,
    // when that game was launched, for the session clock
    val playingSince: Long? = null,
    // what the Companion Screen shows, kept by update through the Companion rule
    val shown: CompanionContent? = null,
)

// the bridge between the Main Screen and the Companion Screen, and the one owner of the Companion's state
// (ADR 0001). The Companion Screen is a second activity with no view model of its own: it draws this state and
// acts through the Crossbar.
@Singleton
class BottomScreenLink @Inject constructor() {
    private val _state = MutableStateFlow(BottomScreenState())
    val state: StateFlow<BottomScreenState> = _state.asStateFlow()

    // the crossbar of the running ECHO, while it lives
    private val _crossbar = MutableStateFlow<CrossbarViewModel?>(null)
    val crossbar: StateFlow<CrossbarViewModel?> = _crossbar.asStateFlow()

    // the window on the second display is on show, so the crossbar has something to keep current
    private val _attached = MutableStateFlow(false)
    val attached: StateFlow<Boolean> = _attached.asStateFlow()

    // ECHO's own window is in front on the first display
    private val _hostShown = MutableStateFlow(true)
    val hostShown: StateFlow<Boolean> = _hostShown.asStateFlow()

    // every change goes through the Companion rule, which keeps what is shown
    fun update(change: (BottomScreenState) -> BottomScreenState) =
        _state.update { old -> change(old).let { it.copy(shown = companionContent(it, last = old.shown)) } }

    // the App Drawer's focused app while the drawer is open on the Main Screen; null once it closes
    private val _drawerFocus = MutableStateFlow<com.echo.feature.appbar.InstalledApp?>(null)
    val drawerFocus: StateFlow<com.echo.feature.appbar.InstalledApp?> = _drawerFocus.asStateFlow()

    fun drawerFocused(app: com.echo.feature.appbar.InstalledApp?) { _drawerFocus.value = app }

    // counted, not set: HOME starts one Companion window and ECHO can start a second over it, and the covered
    // one stops after the new one has started
    private var started = 0

    fun attach(on: Boolean) {
        started = (started + if (on) 1 else -1).coerceAtLeast(0)
        _attached.value = started > 0
    }

    // owner, 2026-10-08: the second screen is wanted and something else holds it (Android's own launcher after
    // HOME or a reinstall), so ECHO puts it back when its first screen resumes
    fun needsCompanion(enabled: Boolean): Boolean = enabled && !_attached.value

    fun hostShown(shown: Boolean) { _hostShown.value = shown }

    // counts the Companion's keyboard claims that have handed focus back: the Main Screen's window keeps its own
    // focus through the claim, so it hears of the end here, not in onWindowFocusChanged
    private val _keyboardClaims = MutableStateFlow(0)
    val keyboardClaims: StateFlow<Int> = _keyboardClaims.asStateFlow()

    fun keyboardClaimEnded() { _keyboardClaims.update { it + 1 } }

    fun bind(crossbar: CrossbarViewModel) { _crossbar.value = crossbar }

    // a crossbar going away clears only itself, not one that has replaced it
    fun unbind(crossbar: CrossbarViewModel) { _crossbar.compareAndSet(crossbar, null) }
}

// the Companion Screen's session clock: "NOW PLAYING · 12 MIN", in hours past an hour; minutes round down, so a game
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

// a drawer case as the Recent view draws it: an album, video or book as its own kind (its art, its label),
// an app as an app (owner, 2026-10-08: an album showed as "APP" with a letter tile)
internal fun drawerCaseItem(app: com.echo.feature.appbar.InstalledApp): CrossbarItem {
    val last = app.lastUsedAt.takeIf { it > 0L }
    val media = app.media ?: return CrossbarItem(
        id = app.packageName, title = app.label, packageName = app.packageName, isAndroidApp = true,
        lastOpenedAt = last, totalPlayTimeMillis = app.playTimeMillis,
    )
    val type = when (media.kind) {
        com.echo.feature.appbar.MediaKind.MUSIC -> com.echo.feature.crossbar.viewmodel.CrossbarItemType.MUSIC_GROUP
        com.echo.feature.appbar.MediaKind.VIDEO -> com.echo.feature.crossbar.viewmodel.CrossbarItemType.VIDEO_FILE
        com.echo.feature.appbar.MediaKind.BOOK -> com.echo.feature.crossbar.viewmodel.CrossbarItemType.LIBRARY_BOOK
    }
    val id = when (media.kind) {
        com.echo.feature.appbar.MediaKind.MUSIC -> "mg_album_${media.ref}"
        com.echo.feature.appbar.MediaKind.VIDEO -> "vid_${media.ref}"
        com.echo.feature.appbar.MediaKind.BOOK -> "book_${media.ref}"
    }
    return CrossbarItem(
        id = id, title = app.label, subtitle = listOfNotNull(media.maker, media.genre).joinToString("  ·  ").ifEmpty { null },
        coverUri = app.art, artworkUri = app.art, type = type, lastOpenedAt = last,
        musicGroupKey = media.ref.takeIf { media.kind == com.echo.feature.appbar.MediaKind.MUSIC },
    )
}
