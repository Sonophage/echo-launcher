package com.echo.feature.crossbar.viewmodel

import com.echo.feature.crossbar.bottomscreen.hasInfo
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.chose
import com.echo.core.domain.model.PlatformIds.ANDROID as ANDROID_PLATFORM_ID

import com.echo.core.domain.model.PlatformIds.WINDOWS as WINDOWS_PLATFORM_ID

import android.net.Uri
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.MediaStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.database.dao.PlatformDao
import com.echo.core.data.database.entity.HiddenPlacementEntity
import com.echo.core.data.database.entity.PlatformEntity
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CategoryRepositoryImpl
import com.echo.core.data.repository.ControllerMappingRepository
import com.echo.core.data.repository.CustomIconStore
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.music.MusicIntentResolver
import com.echo.feature.launcher.PlatformEmulatorChoices
import com.echo.feature.launcher.platformEmulatorChoices
import com.echo.core.data.repository.MemoryCardRepository
import com.echo.core.data.repository.EchoThemeStore
import com.echo.core.ui.icons.CustomIcon
import com.echo.themekit.CustomizableIcons
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.ControllerHintPolicy
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType
import com.echo.core.domain.model.GamepadAction
import com.echo.feature.crossbar.ui.detail.DetailPanelContent
import com.echo.feature.crossbar.ui.detail.detailPanelContentFor
import com.echo.core.domain.model.HiddenPlacement
import com.echo.core.domain.model.HideLocationType
import com.echo.core.domain.model.PlayState
import com.echo.core.domain.model.VideoSnapPlacement
import com.echo.core.domain.model.MemoryCard
import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.CrossbarColorScheme
import com.echo.core.domain.model.CrossbarPalette
import com.echo.core.domain.model.resolve
import com.echo.core.domain.repository.GameRepository
import com.echo.core.ui.icons.GameIconStyle
import com.echo.core.ui.notification.AndroidNotice
import com.echo.core.ui.notification.AndroidNotifications
import com.echo.core.ui.notification.BackgroundTaskNotifier
import com.echo.feature.crossbar.ui.detail.ManualViewerUi
import com.echo.feature.crossbar.ui.detail.MetadataPreviewUi
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import com.echo.core.ui.sound.MenuSound
import com.echo.core.ui.theme.DefaultEchoColors
import com.echo.core.ui.theme.EchoColors
import com.echo.core.ui.wave.WaveStyle
import com.echo.feature.appbar.AppCategoryRepository
import com.echo.feature.appbar.CategorizedApp
import com.echo.feature.appbar.LauncherShortcutRepository
import com.echo.feature.launcher.LaunchRecoveryAction
import com.echo.feature.artwork.api.ArtworkRepository
import com.echo.feature.library.scanner.LibraryScanner
import com.echo.feature.crossbar.R
import com.echo.feature.crossbar.gamepad.GamepadInputHandler
import com.echo.core.ui.components.LetterJumpState
import com.echo.core.ui.components.at
import com.echo.core.ui.components.move
import com.echo.feature.crossbar.gamepad.ShoulderHold
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal fun CrossbarPalette.toEchoColors() = EchoColors(
    waveColor         = androidx.compose.ui.graphics.Color(waveColor),
    accentColor       = androidx.compose.ui.graphics.Color(accentColor),
    textPrimary       = androidx.compose.ui.graphics.Color(textColor),
    textSecondary     = androidx.compose.ui.graphics.Color(textColor).copy(alpha = 0.7f),
    backgroundOverlay = androidx.compose.ui.graphics.Color(0x88000000),
    selectedItem      = androidx.compose.ui.graphics.Color(accentColor),
    categoryBar       = androidx.compose.ui.graphics.Color(0x00000000),
    backgroundTop     = androidx.compose.ui.graphics.Color(backgroundTop),
    backgroundBottom  = androidx.compose.ui.graphics.Color(backgroundBottom),
)

data class CrossbarContextMenu(
    val state: MenuState<String>,

    // the Genre picker's track, album or book (owner, 2026-10-08)
    val genreTarget: GenreTarget? = null,

    val primaryId: String? = null,

    val platformId: String? = null,

    val isAllGames: Boolean = false,
    val gameId: Long? = null,
    val packageName: String? = null,

    val categoryContext: String? = null,

    val pendingAppAction: String? = null,

    val isAddMenu: Boolean = false,

    val shortcutId: String? = null,

    val launchIntentUri: String? = null,

    val musicFolderId: String? = null,
    val musicTrackId: String? = null,

    val recentAlbum: CrossbarItem? = null,

    // an album's own menu, by its key (the Albums view and the App Drawer's Music)
    val albumKey: String? = null,

    val playlistId: Long? = null,

    val playlistPickerTrackId: String? = null,

    val videoPlaylistId: Long? = null,

    val videoFileId: String? = null,

    val videoLibraryId: String? = null,

    val videoPlaylistPickerVideoId: String? = null,

    val bookFileId: String? = null,

    val photoFileId: String? = null,

    val photoLibraryId: String? = null,

    val mediaRootUri: String? = null,
    val mediaRootKind: MediaRootKind? = null,
) {
    val title: String get() = state.title
    val items: List<CrossbarContextMenuItem> get() = state.rows
    val subtitle: String? get() = state.subtitle
    val selectedIndex: Int? get() = state.selectedIndex
    val parent: MenuState<String>? get() = state.parent

    fun withSelected(index: Int): CrossbarContextMenu = copy(state = state.copy(selectedIndex = index))
}

typealias CrossbarContextMenuItem = MenuRow<String>

data class CollectionNameDialogState(
    val title: String,

    val subtitle: String? = null,

    val resetLabel: String? = null,

    val initialText: String = "",

    val text: String = initialText,

    val editTitleGameId: Long? = null,

    val editNoteGameId: Long? = null,

    val editGenreTarget: GenreTarget? = null,

    val renameCardPlatformId: String? = null,

    val renameCategoryId: String? = null,

    val renameProfile: Boolean = false,

    val placeholder: String = "e.g. RPGs, Currently Playing",
    val confirmLabel: String = "Save",
)

data class InfoDialogState(
    val title: String,
    val message: String,
)

data class ColorSchemePickerState(
    val options: List<ColorSchemeOption>,
    val selectedIndex: Int = 0,
)

data class ColorSchemeOption(
    val scheme: CrossbarColorScheme?,
    val label: String,

    val sublabel: String?,
    val swatch: Long,
    val isCustom: Boolean = false,
)

data class CustomColorPickerState(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val selectedChannel: Int = 0,
)

data class CustomIconSession(
    val groups: List<com.echo.themekit.IconSlot.Group>,
    val groupIndex: Int = 0,
    val slotIndex: Int = 0,
    val message: String? = null,

    val revision: Int = 0,
) {
    val group: com.echo.themekit.IconSlot.Group get() = groups[groupIndex]

    val focusedSlot: com.echo.themekit.IconSlot?
        get() = CustomizableIcons.group(group).getOrNull(slotIndex)
}

sealed interface AppPickerTarget {
    data class AndroidGames(val platformId: String) : AppPickerTarget

    data class CategoryShortcuts(val categoryId: String) : AppPickerTarget
}

data class AppPickerEntry(
    val packageName: String,
    val label: String,
)

const val PICKER_GRID_COLUMNS = 7

data class AppPickerState(
    val title: String,
    val target: AppPickerTarget,
    val apps: List<AppPickerEntry>,
    val selected: Set<String> = emptySet(),

    val initialSelected: Set<String> = emptySet(),

    val focusedIndex: Int = 0,
    val query: String = "",
    val searchActive: Boolean = false,
    val confirmingRemovals: Boolean = false,

    val confirmFocusedOption: Int = CONFIRM_CANCEL,

    val columns: Int = PICKER_GRID_COLUMNS,

    val usingTouch: Boolean = false,
) {
    companion object {
        const val CONFIRM_CANCEL = 0
        const val CONFIRM_REMOVE = 1
    }
}

sealed interface VideoNav {
    data object Root : VideoNav
    data object Folders : VideoNav
    data object AllVideos : VideoNav

    data object Collections : VideoNav
    data object RecentlyWatched : VideoNav
    data object Favorites : VideoNav
    data object Playlists : VideoNav
    data class Playlist(val id: Long, val name: String) : VideoNav
    data object Libraries : VideoNav
    data class Library(val id: String, val name: String) : VideoNav
}

private val VideoNav.isVideoCollectionChild: Boolean
    get() = this == VideoNav.RecentlyWatched || this == VideoNav.Favorites || this == VideoNav.Playlists

sealed interface BooksNav {
    data object Root : BooksNav
    data object Folders : BooksNav
    data object AllBooks : BooksNav
    data object Shelves : BooksNav
    data class Shelf(val id: String, val name: String) : BooksNav

    data object SeriesList : BooksNav

    data class Series(val name: String) : BooksNav

    // owner, 2026-10-08: the books by genre, as Series drills
    data object Genres : BooksNav
    data class Genre(val name: String) : BooksNav
}

data class BookSeries(val name: String, val bookCount: Int, val coverUri: String?)

sealed interface PhotoNav {
    data object Root : PhotoNav
    data object Folders : PhotoNav
    data object AllPhotos : PhotoNav
    data object Albums : PhotoNav
    data object Favorites : PhotoNav
    data class Library(val id: String, val name: String) : PhotoNav
}

data class PhotoViewerRequest(
    val photoId: String,
    val libraryId: String?,
    val openWallpaperPreview: Boolean = false,
    val favoritesOnly: Boolean = false,
)

sealed interface RootTarget {
    data class Media(val kind: MediaRootKind) : RootTarget
    data object Rom : RootTarget
}

data class RootPick(val target: RootTarget, val relinkFrom: String? = null)

sealed interface MusicNav {
    data object Root : MusicNav
    data object Folders : MusicNav
    data object AllMusic : MusicNav
    data object Playlists : MusicNav
    data class Playlist(val id: Long, val name: String) : MusicNav

    // owner, 2026-10-06: Artists and Albums drill on the crossbar like All Games, not in a browser
    data object Artists : MusicNav
    data object Albums : MusicNav
    data class Artist(val name: String, val key: String) : MusicNav
    data class Album(val name: String, val key: String) : MusicNav

    // owner, 2026-10-08: the tracks by genre, as Artists and Albums drill
    data object Genres : MusicNav
    data class Genre(val name: String, val key: String) : MusicNav
}

// a column of tracks, which the music sorts order
internal val MusicNav.listsTracks: Boolean
    get() = this == MusicNav.AllMusic || this is MusicNav.Playlist || this is MusicNav.Artist || this is MusicNav.Album ||
        this is MusicNav.Genre

data class SearchState(
    val scope: SearchScope,
    val query: String = "",
    val rows: List<CrossbarItem> = emptyList(),
    val selectedIndex: Int = 0,

    val scrollToTopToken: Int = 0,

    val loaded: Boolean = false,
    // the shelf's filter (null is All), and each kind the results hold with how many
    val kind: SearchKind? = null,
    val kindCounts: List<Pair<SearchKind, Int>> = emptyList(),
    val total: Int = 0,
)

data class PlaylistNameDialogState(
    val title: String,
    val initialText: String = "",

    val text: String = initialText,
    val forTrackId: String? = null,

    val renamePlaylistId: Long? = null,

    val videoContext: Boolean = false,

    val forVideoId: String? = null,
)

data class MusicTrackPickerState(
    val playlistId: Long,
    val playlistName: String,
    val tracks: List<MusicTrack>,
    val selected: Set<String> = emptySet(),
    val selectedIndex: Int = 0,
)

enum class DrillOutStep {
    MUSIC,

    VIDEO_LIBRARY,
    VIDEO_PLAYLIST,
    VIDEO_COLLECTION_CHILD,
    VIDEO,

    PHOTO_LIBRARY,
    PHOTO,

    LIBRARY_SHELF,

    LIBRARY_SERIES,
    LIBRARY_GENRE,
    LIBRARY,
    ROM_FOLDERS,

    PLATFORM_FOLDER,
}

internal enum class InitialSetupDecision { ALREADY_SEEN, SEED_AS_SEEN, OPEN_WIZARD }

@androidx.compose.runtime.Immutable

data class MediaCovers(
    val music: List<String> = emptyList(),
    val video: List<String> = emptyList(),
    val photo: List<String> = emptyList(),
    val books: List<String> = emptyList(),
)

data class CrossbarUiState(

    val categories: List<Category> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val platformGameCounts: Map<String, Int> = emptyMap(),

    val allGamesCount: Int = 0,

    val cardFanCovers: Map<String, List<String>> = emptyMap(),

    val shelfFanCovers: Map<String, List<String>> = emptyMap(),

    val favoritesCount: Int = 0,

    val missingCount: Int = 0,

    val playStateCounts: Map<PlayState, Int> = emptyMap(),
    val recentlyAddedCount: Int = 0,
    val selectedPlatformId: String? = null,

    val musicNav: MusicNav = MusicNav.Root,
    val rootPick: RootPick? = null,
    val romFoldersOpen: Boolean = false,
    val musicFolders: List<com.echo.core.domain.model.MusicFolder> = emptyList(),

    val mediaCovers: MediaCovers = MediaCovers(),

    val musicPlaylists: List<com.echo.core.domain.model.Playlist> = emptyList(),
    val videoPlaylists: List<com.echo.core.domain.model.VideoPlaylist> = emptyList(),

    val gameSortMode: CrossbarSortMode = CrossbarSortMode.TITLE,
    val musicSortMode: CrossbarSortMode = CrossbarSortMode.TITLE,
    val videoSortMode: CrossbarSortMode = CrossbarSortMode.TITLE,
    val bookSortMode: CrossbarSortMode = CrossbarSortMode.TITLE,
    val sortLabel: String? = null,
    // the one genre the game lists show, or null for all (owner, 2026-10-08)
    val genreFilter: com.echo.core.domain.model.GameGenre? = null,
    // the Game column's folders: one per system, or one per genre (owner, 2026-10-08)
    val gameGrouping: GameGrouping = GameGrouping.SYSTEM,
    val genreCounts: Map<com.echo.core.domain.model.GameGenre, Int> = emptyMap(),

    val musicPlayerVisible: Boolean = false,
    val musicPlayback: com.echo.feature.crossbar.music.MusicPlaybackState =
        com.echo.feature.crossbar.music.MusicPlaybackState(),
    val musicAccentArgb: Long? = null,

    val currentItems: List<CrossbarItem> = emptyList(),
    val selectedItemIndex: Int = 0,

    val letterJump: LetterJumpState? = null,

    val drillTitle: String? = null,

    val drillSiblings: List<CrossbarItem> = emptyList(),
    val drillSiblingIndex: Int = 0,

    val scrollToTopToken: Int = 0,

    val lastInputWasTouch: Boolean = false,

    val showContextMenuHint: Boolean = false,

    val idle: Boolean = false,

    val showSettingsHint: Boolean = false,

    val contextMenuHintEnabled: Boolean = ControllerHintPolicy.DEFAULT_ENABLED,

    val contextMenuHintDelaySeconds: Float = ControllerHintPolicy.DEFAULT_DELAY_SECONDS,
    val touchNavButtonMode: com.echo.core.domain.model.TouchNavButtonMode =
        com.echo.core.domain.model.TouchNavButtonMode.AUTO,

    val touchSensitivity: com.echo.core.domain.model.TouchSensitivity =
        com.echo.core.domain.model.TouchSensitivity.NORMAL,

    val waveStyle: WaveStyle = WaveStyle.ANIMATED,

    val waveDesign: com.echo.core.ui.wave.WaveDesign = com.echo.core.ui.wave.WaveDesign.PSP,

    // the item whose launch ring is filling while A is held
    val launchHold: String? = null,

    // the top-left orb: 0 at rest, 1 focused, 2 expanded
    val orbLevel: Int = 0,

    // the notifications tab's chip (kit 11)
    val noticeChip: NoticeChip = NoticeChip.ALL,

    // the game ECHO last sent away, which Y can resume while ECHO is still running
    val resumeGameId: Long? = null,

    // bumps when A comes up while the drawer is open, ending its hold-to-launch
    val drawerSelectReleases: Int = 0,

    val respectBatterySaver: Boolean = true,

    val waveOverWallpaper: Boolean = false,

    val wallpaperAccent: Long? = null,
    val thermalThrottleAware: Boolean = true,
    val customWallpaperPath: String? = null,

    val motionWallpaperPath: String? = null,
    // how the video wallpaper moves, its own setting apart from the wave
    val motionStyle: com.echo.core.ui.wave.WaveStyle = com.echo.core.ui.wave.WaveStyle.ANIMATED,

    val showBootSequence: Boolean = true,

    // A or B during the boot animation: it runs its short exit, then completes
    val bootSkipRequested: Boolean = false,

    val bootVideoPath: String? = null,
    val bootAudioPath: String? = null,

    // the boot sound is playing; it can run on after the animation closes
    val bootSoundPlaying: Boolean = false,

    val activeGameBoot: com.echo.feature.launcher.GameBootRequest? = null,

    val gameBootIsPreview: Boolean = false,

    val discCeremony: DiscCeremonyState? = null,

    val startupPermissionsSettled: Boolean = false,

    val initialSetupDecided: Boolean = false,

    val activeSettingsScreen: String? = null,

    val settingsReturnTo: String? = null,

    val leftBacksOut: Boolean = true,
    val pendingSettingsAction: GamepadAction? = null,
    val activeAppDrawerFilter: String? = null,
    val pendingDrawerAction: GamepadAction? = null,

    val drawerLetterRailHeld: Boolean = false,

    val pendingDrawerTypedChar: String? = null,
    val artworkFetchTitle: String? = null,

    val artworkStudioGameId: Long? = null,

    val pendingArtworkStudioAction: GamepadAction? = null,

    val manualViewer: com.echo.feature.crossbar.ui.detail.ManualViewerUi? = null,

    val metadataPreview: com.echo.feature.crossbar.ui.detail.MetadataPreviewUi? = null,

    val metadataPreviewGameId: Long? = null,

    val gameInfo: GameInfoState? = null,

    val profile: ProfileState? = null,
    val profileName: String = DEFAULT_PROFILE_NAME,
    val profileAvatar: String? = null,
    val profileAvatarPick: Boolean = false,
    val profileData: ProfileData = ProfileData(),






    val videoNav: VideoNav = VideoNav.Root,
    val videoLibraries: List<com.echo.core.domain.model.VideoLibrary> = emptyList(),
    val activeVideoId: String? = null,

    val resumeVideo: com.echo.core.domain.model.Video? = null,

    val activeVideoAutoPlay: Boolean = false,
    val pendingVideoDetailAction: GamepadAction? = null,

    val photoNav: PhotoNav = PhotoNav.Root,
    val booksNav: BooksNav = BooksNav.Root,

    val continueBook: com.echo.core.domain.model.Book? = null,
    val bookLibraries: List<com.echo.core.domain.model.BookLibrary> = emptyList(),

    val bookSeries: List<BookSeries> = emptyList(),
    // the books by genre, each as a name, a count and a cover (owner, 2026-10-08)
    val bookGenres: List<BookSeries> = emptyList(),

    val defaultReader: String? = null,
    val defaultReaderLabel: String? = null,
    val photoLibraries: List<com.echo.core.domain.model.PhotoLibrary> = emptyList(),
    val photoFavoriteCount: Int = 0,
    val activePhotoViewer: PhotoViewerRequest? = null,
    val pendingPhotoViewerAction: GamepadAction? = null,

    val activeContextMenu: CrossbarContextMenu? = null,

    val moving: MoveSession? = null,

    val colorSchemePicker: ColorSchemePickerState? = null,
    val customColorPicker: CustomColorPickerState? = null,

    val renameAppTarget: String? = null,
    val renameAppCurrent: String? = null,
    val renameAppText: String = "",

    val collectionNameDialog: CollectionNameDialogState? = null,

    val playlistNameDialog: PlaylistNameDialogState? = null,

    val musicTrackPicker: MusicTrackPickerState? = null,

    val search: SearchState? = null,

    // a second screen is showing ECHO (the AYN Thor's bottom screen)
    val secondScreen: Boolean = false,
    // owner, 2026-10-06: the XMB on the second screen and the companion (Info, Recent, the drawer,
    // Search, Settings) on the main one; remembered
    val screensSwapped: Boolean = false,
    // the companion takes the controller, after a tap on its screen, until B or a tap on the XMB
    val companionActive: Boolean = false,
    // the open menu, and any it leads to, was asked for on the second screen and is drawn there (owner,
    // 2026-10-08: a menu opens where it was asked for); cleared whenever no menu is open
    val menuOnCompanion: Boolean = false,
    // the device has a second display, and whether ECHO uses it (Quick settings, owner 2026-10-06)
    val secondDisplayPresent: Boolean = false,
    val secondScreenEnabled: Boolean = true,

    val infoDialog: InfoDialogState? = null,

    val showWindowsSetupPrompt: Boolean = false,

    val launchRecovery: com.echo.feature.launcher.LaunchRecoveryRequest? = null,

    val launchRecoveryCursor: Int = 0,

    val appPicker: AppPickerState? = null,

    val gamePickerCategoryId: String? = null,
    val pendingGamePickerAction: GamepadAction? = null,

    val iconStyle: GameIconStyle = GameIconStyle.PSP_RECTANGLE,

    val iconLegibility: com.echo.core.domain.model.IconLegibilityStyle =
        com.echo.core.domain.model.IconLegibilityStyle.DEFAULT,

    val fadeByDistance: Boolean = true,

    val cardArtGrid: Boolean = false,

    val recentsIncludeApps: Boolean = false,
    val interfaceChoices: com.echo.core.data.repository.InterfaceChoices =
        com.echo.core.data.repository.InterfaceChoices(),

    val textShadow: Boolean = true,

    val focusedGameVideo: com.echo.feature.crossbar.ui.FocusedGameVideo? = null,

    val snapPlacement: com.echo.core.domain.model.VideoSnapPlacement =
        com.echo.core.domain.model.VideoSnapPlacement.DEFAULT,

    val gameMetadataVisible: Boolean = true,

    val itemBackdropEnabled: Boolean = true,

    val focusedItemAccentArgb: Long? = null,

    val focusedItemBackdrop: String? = null,

    val recentFilter: RecentFilter = RecentFilter.ALL,

    val recentFilters: List<RecentFilter> = listOf(RecentFilter.ALL),
    // the games and apps pinned under Recent, as pinKey keys, oldest pin first
    val recentPins: List<String> = emptyList(),

    val recentRailVisible: Boolean = false,

    val notificationsOpen: Boolean = false,
    // the notification island's card is out (owner, 2026-10-05). Pinned when the user brought it out, so it
    // stays and takes the pad until it is closed; a new notification's own peek is not pinned and times out
    val noticeCardOut: Boolean = false,
    val noticeCardPinned: Boolean = false,
    val noticeCardCursor: Int = 0,

    val panelTab: PanelTab = PanelTab.NOTIFICATIONS,
    val noticeCursor: Int = 0,
    val panelQuick: QuickSetting = QuickSetting.WAVE,
    val panelProfile: ProfileFocus = ProfileFocus(),
    val panelChip: Int = 0,
    val panelSetting: Int = 0,
    val settingsFromPanel: Boolean = false,
    val libraryChips: List<LibraryChip> = emptyList(),
    val launcherNotices: List<com.echo.core.ui.notification.SystemToast> = emptyList(),

    val androidNotices: List<AndroidNotice> = emptyList(),
    val externalPlayback: com.echo.core.ui.notification.ExternalPlayback? = null,

    val recentTop: CrossbarItem? = null,

    val recentTopAccentArgb: Long? = null,
    val recentTopAt: Long? = null,
    val librarySetupComplete: Boolean = false,
    val themeColors: EchoColors = DefaultEchoColors,

    val iconOverrides: Map<String, CustomIcon> = emptyMap(),

    val customIcons: Map<String, CustomIcon> = emptyMap(),

    val customIconSession: CustomIconSession? = null,

    val pendingThemeShareFile: java.io.File? = null,

    val pendingCustomIconsAction: GamepadAction? = null,

    val saveThemeNameDialog: PlaylistNameDialogState? = null,

    val layoutSpec: com.echo.themekit.CrossbarLayoutSpec = com.echo.themekit.CrossbarLayoutSpec.DEFAULT,
    val focusStyle: com.echo.themekit.FocusStyle = com.echo.themekit.FocusStyle.CLASSIC,
    val motion: com.echo.themekit.MotionPreset = com.echo.themekit.MotionPreset.CLASSIC,


    val crossbarLayoutAdjustMap: Map<String, com.echo.themekit.CrossbarLayoutAdjust> = emptyMap(),

) {
    val isInSubItem: Boolean
        get() = drillOutStep != null

    val drillOutStep: DrillOutStep?
        get() = when {
            musicNav != MusicNav.Root -> DrillOutStep.MUSIC
            videoNav is VideoNav.Library -> DrillOutStep.VIDEO_LIBRARY
            videoNav is VideoNav.Playlist -> DrillOutStep.VIDEO_PLAYLIST
            videoNav.isVideoCollectionChild -> DrillOutStep.VIDEO_COLLECTION_CHILD
            videoNav != VideoNav.Root -> DrillOutStep.VIDEO
            photoNav is PhotoNav.Library -> DrillOutStep.PHOTO_LIBRARY
            photoNav != PhotoNav.Root -> DrillOutStep.PHOTO
            booksNav is BooksNav.Series -> DrillOutStep.LIBRARY_SERIES
            booksNav is BooksNav.Genre -> DrillOutStep.LIBRARY_GENRE
            booksNav is BooksNav.Shelf -> DrillOutStep.LIBRARY_SHELF
            booksNav != BooksNav.Root -> DrillOutStep.LIBRARY
            romFoldersOpen -> DrillOutStep.ROM_FOLDERS
            selectedPlatformId != null -> DrillOutStep.PLATFORM_FOLDER
            else -> null
        }

    val focusedItem: CrossbarItem?
        get() = currentItems.getOrNull(selectedItemIndex)

    val onLastPlayedHome: Boolean
        get() = categories.getOrNull(selectedCategoryIndex)?.id == BuiltInCategory.RECENTLY_PLAYED &&
            !isInSubItem &&
            !(recentFilter == RecentFilter.ALL && currentItems.isEmpty())

    val canFilterRecents: Boolean
        get() = onLastPlayedHome

    val focusedItemHasContextMenu: Boolean
        get() = focusedItem?.hasContextMenu(this) == true

    val canSortCurrentList: Boolean
        get() = activeSortModes() != null

    val resolvedShowTouchButton: Boolean
        get() = when (touchNavButtonMode) {
            com.echo.core.domain.model.TouchNavButtonMode.AUTO -> lastInputWasTouch
            com.echo.core.domain.model.TouchNavButtonMode.ALWAYS_SHOW -> true
            com.echo.core.domain.model.TouchNavButtonMode.ALWAYS_HIDE -> false
        }

    val overlayKeepsChrome: Boolean
        get() = (activeContextMenu != null || notificationsOpen || moving != null) && !otherBlockingOverlay

    // the notifications under the current chip
    val noticeEntries: List<PanelEntry>
        get() = panelEntries(androidNotices, launcherNotices).filter { noticeChip in it.chips() }

    // the media a player holds comes first, under every chip, so the panel opens on it
    val noticeFocusables: List<NoticeFocus>
        get() = listOfNotNull(NoticeFocus.Media.takeIf { pinnedMedia() != null }) + noticeEntries.map { it.focus }

    val focusedNotice: NoticeFocus?
        get() = if (panelTab != PanelTab.NOTIFICATIONS) null else noticeFocusables.let { rows ->
            if (rows.isEmpty()) null else rows[noticeCursor.coerceIn(0, rows.lastIndex)]
        }

    val shelfCards: List<ShelfCard>
        get() = buildList {
            if (favoritesCount > 0) add(ShelfCard.Favorites(favoritesCount))
            PlayState.entries.forEach { state ->
                playStateCounts[state]?.takeIf { it > 0 }?.let { add(ShelfCard.Marked(state, it)) }
            }
            if (recentlyAddedCount > 0) add(ShelfCard.RecentlyAdded(recentlyAddedCount))
        }

    fun categoryReachable(category: Category): Boolean =
        (category.id != BuiltInCategory.SHELVES || shelfCards.isNotEmpty()) &&
            // owner, 2026-10-06: with a second screen, Last Played is the bottom screen's Recent page
            !(secondScreen && category.id == BuiltInCategory.RECENTLY_PLAYED)

    val enterOpensAppDrawer: Boolean
        get() = search == null &&
            !hasBlockingOverlay &&
            !isInSubItem &&
            !onLastPlayedHome

    val hasBlockingOverlay: Boolean
        get() = otherBlockingOverlay || activeContextMenu != null || notificationsOpen || moving != null

    private val otherBlockingOverlay: Boolean
        get() = chromeOverlay || fullscreenOverlay

    val waveShown: Boolean
        get() = !chromeOverlay && !notificationsOpen && !showBootSequence && !onLastPlayedHome &&
            artworkStudioGameId == null && manualViewer == null && metadataPreview == null &&
            activeVideoId == null && activePhotoViewer == null &&
            musicTrackPicker == null && !musicPlayerVisible

    // owner, 2026-10-06: with a second screen, the App Drawer, Search and Settings open there whatever
    // opened them (LB, RB, a tap on either screen); the controller still drives them. The first-run
    // wizard stays on this screen. These are what this screen draws.
    val topSearch: SearchState?
        get() = search?.takeUnless { secondScreen }

    val topDrawerFilter: String?
        get() = activeAppDrawerFilter?.takeUnless { secondScreen }

    val topSettingsScreen: String?
        get() = activeSettingsScreen?.takeUnless { secondScreen && it !in CrossbarViewModel.WIZARD_SCREEN_IDS }

    private val chromeOverlay: Boolean
        get() = topSettingsScreen != null ||
            appPicker != null ||
            gamePickerCategoryId != null ||
            topDrawerFilter != null ||
            gameInfo != null ||
            profile != null ||
            topSearch != null

    private val fullscreenOverlay: Boolean
        get() = showBootSequence ||
            artworkStudioGameId != null ||
            manualViewer != null ||
            metadataPreview != null ||
            activeGameBoot != null ||
            discCeremony != null ||
            activeVideoId != null ||
            activePhotoViewer != null ||
            colorSchemePicker != null ||
            customColorPicker != null ||
            customIconSession != null ||
            saveThemeNameDialog != null ||
            renameAppTarget != null ||
            collectionNameDialog != null ||
            playlistNameDialog != null ||
            musicTrackPicker != null ||
            musicPlayerVisible ||
            infoDialog != null ||
            launchRecovery != null ||
            showWindowsSetupPrompt

    val stripShowsCrossbarContext: Boolean
        get() = !hasBlockingOverlay || overlayKeepsChrome

    val statusStripVisible: Boolean
        get() = !fullscreenOverlay
}

// the boot sequence, the launch disc or GameBoot is on screen, with its own sound; the menu music waits for it
val CrossbarUiState.ceremonyPlaying: Boolean
    get() = showBootSequence || bootSoundPlaying || discCeremony != null || activeGameBoot != null

data class DiscCeremonyState(val art: Any?, val style: com.echo.core.data.repository.GameBootStyle = com.echo.core.data.repository.GameBootStyle.DISC)

enum class CrossbarItemType {
    STANDARD,
    ALL_GAMES,
    FAVORITES,

    SHELF,

    MISSING,
    MEMORY_CARD,
    COLLECTION,

    MUSIC_GROUP,

    MUSIC_ARTISTS,
    MUSIC_ALBUMS,
    MUSIC_GENRES,
    LIBRARY_GENRE,
    MUSIC_TRACK,
    PLAYLIST,
    VIDEO_LIBRARY,
    VIDEO_FOLDER,
    VIDEO_FILE,
    VIDEO_RECENT,
    VIDEO_FAVORITES,
    VIDEO_COLLECTIONS,
    PHOTO_ALBUMS,
    PHOTO_FAVORITES,
    PHOTO_FOLDER,

    LIBRARY_SHELVES,
    LIBRARY_READER,
    LIBRARY_FOLDER,
    LIBRARY_BOOK,

    LIBRARY_SERIES,
    PHOTO_FILE,
    CAMERA,

    SEARCH,

    MEDIA_ROOT,

    ADD_ACTION,
    EMPTY,
}

enum class CrossbarSortMode(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    RECENT_PLAYED("Recently Played"),
    DATE_ADDED("Date Added"),
    SERIES("Series"),
}

private val MUSIC_SORTS = listOf(CrossbarSortMode.TITLE, CrossbarSortMode.ARTIST, CrossbarSortMode.ALBUM, CrossbarSortMode.DATE_ADDED)
private val GAME_SORTS  = listOf(CrossbarSortMode.TITLE, CrossbarSortMode.RECENT_PLAYED, CrossbarSortMode.DATE_ADDED)
private val VIDEO_SORTS = listOf(CrossbarSortMode.TITLE, CrossbarSortMode.DATE_ADDED, CrossbarSortMode.RECENT_PLAYED)
private val BOOK_SORTS  = listOf(CrossbarSortMode.TITLE, CrossbarSortMode.SERIES, CrossbarSortMode.DATE_ADDED)

private val BY_SERIES_POSITION = compareBy<com.echo.core.domain.model.Book>(
    { it.seriesIndex ?: Double.MAX_VALUE },
    { it.displayTitle.lowercase() },
)

internal fun List<com.echo.core.domain.model.Book>.inSeriesOrder(): List<com.echo.core.domain.model.Book> =
    sortedWith(BY_SERIES_POSITION)

// owner, 2026-10-08: the books by genre, the owner's own first; books with none are left out
internal fun List<com.echo.core.domain.model.Book>.genreGroups(): List<BookSeries> =
    filter { it.genreName != null }
        .groupBy { it.genreName!! }
        .map { (name, books) -> BookSeries(name, books.size, books.firstNotNullOfOrNull { it.coverUri }) }
        .sortedBy { it.name.lowercase() }

internal fun List<com.echo.core.domain.model.Book>.seriesGroups(): List<BookSeries> =
    filter { it.seriesName != null }
        .groupBy { it.seriesName!! }
        .map { (name, books) ->
            BookSeries(
                name = name,
                bookCount = books.size,
                coverUri = books.inSeriesOrder().firstNotNullOfOrNull { it.coverUri },
            )
        }
        .sortedBy { it.name.lowercase() }

internal fun List<com.echo.core.domain.model.Book>.bookSorted(mode: CrossbarSortMode): List<com.echo.core.domain.model.Book> = when (mode) {
    CrossbarSortMode.SERIES -> sortedWith(
        compareBy<com.echo.core.domain.model.Book> { it.seriesName == null }
            .thenBy { it.seriesName?.lowercase() ?: "" }
            .then(BY_SERIES_POSITION)
    )
    CrossbarSortMode.DATE_ADDED -> sortedByDescending { it.dateAdded ?: 0L }
    else -> sortedBy { it.displayTitle.lowercase() }
}

internal fun List<com.echo.core.domain.model.Video>.videoSorted(mode: CrossbarSortMode): List<com.echo.core.domain.model.Video> = when (mode) {
    CrossbarSortMode.RECENT_PLAYED -> sortedByDescending { it.lastWatchedAt ?: 0L }
    CrossbarSortMode.DATE_ADDED    -> sortedByDescending { it.dateAdded ?: 0L }
    else                      -> sortedBy { it.displayTitle.lowercase() }
}

internal fun List<Game>.gameSorted(mode: CrossbarSortMode): List<Game> = when (mode) {
    CrossbarSortMode.RECENT_PLAYED -> sortedByDescending { it.lastPlayedAt ?: 0L }
    CrossbarSortMode.DATE_ADDED    -> sortedByDescending { it.id }
    else                      -> sortedBy { it.displayTitle.lowercase() }
}

// the Shelves column's rows: one per shelf card, with its covers
internal fun CrossbarUiState.shelfRows(): List<CrossbarItem> = shelfCards.map { card ->
    CrossbarItem(
        id       = card.cardId,
        title    = card.title,
        subtitle = countLabel(card.count, "game", "games"),
        insideCovers = shelfFanCovers[card.cardId].orEmpty(),
        type     = CrossbarItemType.SHELF,
    )
}

internal fun cursorAfterRefresh(previous: List<CrossbarItem>, previousIndex: Int, next: List<CrossbarItem>): Int {
    val selectedId = previous.getOrNull(previousIndex)?.id
    val kept = selectedId?.let { id -> next.indexOfFirst { it.id == id } } ?: -1
    return if (kept >= 0) kept else previousIndex.coerceIn(0, (next.size - 1).coerceAtLeast(0))
}

internal fun List<Game>.projectGamesForDisplay(): List<Game> {
    val singles = filter { it.discSetKey == null && !it.isMissing }
    val sets = groupBy { it.discSetKey }
        .filterKeys { it != null }
        .values
        .mapNotNull { members ->
            val present = members.filterNot { it.isMissing }
            if (present.isEmpty()) return@mapNotNull null
            val display = members.firstOrNull { it.isDiscPrimary } ?: present.first()

            display.copy(isFavorite = members.any { it.isFavorite })
        }
    return singles + sets
}

internal fun List<MusicTrack>.trackSorted(mode: CrossbarSortMode): List<MusicTrack> = when (mode) {
    CrossbarSortMode.ARTIST     -> sortedWith(
        compareBy(nullsLast<String>()) { t: MusicTrack -> t.artist?.lowercase() }
            .thenBy(nullsLast<String>()) { t -> t.album?.lowercase() }
            .thenBy { t -> t.displayTitle.lowercase() }
    )
    CrossbarSortMode.ALBUM      -> sortedWith(
        compareBy(nullsLast<String>()) { t: MusicTrack -> t.album?.lowercase() }
            .thenBy { t -> t.trackNumber ?: Int.MAX_VALUE }
            .thenBy { t -> t.displayTitle.lowercase() }
    )
    CrossbarSortMode.DATE_ADDED -> sortedByDescending { it.lastModified ?: 0L }
    else                   -> sortedBy { it.displayTitle.lowercase() }
}

internal fun CrossbarItem.owningCategory(): String? = when (type) {
    CrossbarItemType.VIDEO_FILE -> BuiltInCategory.VIDEO
    CrossbarItemType.PHOTO_FILE -> BuiltInCategory.PHOTO
    CrossbarItemType.LIBRARY_BOOK -> BuiltInCategory.LIBRARY
    CrossbarItemType.MUSIC_TRACK -> BuiltInCategory.MUSIC
    else -> if (gameId != null) BuiltInCategory.GAMES else null
}

internal fun CrossbarItem.menuHostCategory(currentCategoryId: String?): String? =
    owningCategory() ?: currentCategoryId

fun CrossbarItem.hasContextMenu(state: CrossbarUiState): Boolean {
    val categoryId = menuHostCategory(state.categories.getOrNull(state.selectedCategoryIndex)?.id)
    return when {
        categoryId == BuiltInCategory.MUSIC && (
            id == CrossbarViewModel.NOW_PLAYING_ITEM_ID ||
                type == CrossbarItemType.MUSIC_TRACK ||
                (type == CrossbarItemType.PLAYLIST && playlistId != null)
        ) -> true

        categoryId == BuiltInCategory.VIDEO && (
            (type == CrossbarItemType.VIDEO_FILE && id.startsWith("vid_")) ||
                (type == CrossbarItemType.VIDEO_FOLDER && id.startsWith("vlib_")) ||
                (type == CrossbarItemType.PLAYLIST && playlistId != null)
        ) -> true

        categoryId == BuiltInCategory.LIBRARY &&
            type == CrossbarItemType.LIBRARY_BOOK && id.startsWith("book_") -> true

        categoryId == BuiltInCategory.PHOTO && (
            (type == CrossbarItemType.PHOTO_FILE && id.startsWith("pho_")) ||
                (type == CrossbarItemType.PHOTO_FOLDER && id.startsWith("plib_"))
        ) -> true
        isRecentAlbum -> true
        movableInColumn && state.columnOrderKey() != null -> true
        // every row of a column that can move offers Move Column (owner, 2026-10-07)
        state.currentItems.getOrNull(state.selectedItemIndex)?.id == id && state.columnMovable() -> true
        mediaRootKind != null && type == CrossbarItemType.MEDIA_ROOT -> true
        type == CrossbarItemType.MEDIA_ROOT -> true
        gameId != null -> true
        type == CrossbarItemType.ALL_GAMES -> true
        platformId != null -> true
        packageName != null -> true
        else -> false
    }
}

fun CrossbarUiState.activeSortModes(): List<CrossbarSortMode>? {
    val cat = categories.getOrNull(selectedCategoryIndex) ?: return null
    return when {
        cat.id == BuiltInCategory.MUSIC &&
            musicNav.listsTracks -> MUSIC_SORTS

        cat.id == BuiltInCategory.VIDEO &&
            (videoNav == VideoNav.AllVideos || videoNav == VideoNav.Favorites ||
                videoNav is VideoNav.Library) -> VIDEO_SORTS

        cat.id == BuiltInCategory.LIBRARY &&
            (booksNav == BooksNav.AllBooks || booksNav is BooksNav.Shelf) -> BOOK_SORTS
        cat.id == BuiltInCategory.GAMES &&
            selectedPlatformId != null -> GAME_SORTS
        cat.isGamingCategory -> GAME_SORTS
        else -> null
    }
}

internal fun canonicalCrossbarCategories(
    categories: List<Category>,
    fallbacks: List<Category>,
): List<Category> {
    val byId = categories.associateBy { it.id }
    val builtInIds = fallbacks.map { it.id }.toSet()

    val builtIns = fallbacks.mapNotNull { fallback ->
        val stored = byId[fallback.id]

        if (stored == null) return@mapNotNull null
        fallback.copy(
            name             = stored.name.takeIf { it.isNotBlank() } ?: fallback.name,
            position         = stored.position,
            accentColor      = stored.accentColor,
            customIconUri    = stored.customIconUri,
            filterRules      = stored.filterRules,

            isGamingCategory = stored.isGamingCategory,
        )
    }

    val customCategories = categories.filter { it.id !in builtInIds }

    return (builtIns + customCategories).sortedBy { it.position }
}

// the XMB top bar's sort row: the modes this list can take and the one it is in
fun CrossbarUiState.sortRow(): Pair<List<CrossbarSortMode>, CrossbarSortMode>? = activeSortModes()?.let { it to sortModeFor(it) }

internal fun CrossbarUiState.sortModeFor(cycle: List<CrossbarSortMode>): CrossbarSortMode = when {
    cycle === MUSIC_SORTS -> musicSortMode
    cycle === VIDEO_SORTS -> videoSortMode
    cycle === BOOK_SORTS  -> bookSortMode
    else                  -> gameSortMode
}

internal fun CrossbarUiState.withSortMode(cycle: List<CrossbarSortMode>, mode: CrossbarSortMode): CrossbarUiState = when {
    cycle === MUSIC_SORTS -> copy(musicSortMode = mode)
    cycle === VIDEO_SORTS -> copy(videoSortMode = mode)
    cycle === BOOK_SORTS  -> copy(bookSortMode = mode)
    else                  -> copy(gameSortMode = mode)
}

val CrossbarUiState.hintsAutoHide: Boolean
    get() = contextMenuHintDelaySeconds > 0f

internal fun CrossbarUiState.withHintsShownNow(): CrossbarUiState = copy(
    showContextMenuHint = shouldShowContextMenuHint(this, 0L),
    showSettingsHint = shouldShowSettingsHint(this, 0L),
)

fun shouldShowContextMenuHint(state: CrossbarUiState, idleMs: Long): Boolean =
    state.contextMenuHintEnabled &&

        state.stripShowsCrossbarContext &&

        (state.focusedItemHasContextMenu || state.canSortCurrentList || state.canFilterRecents) &&
        idleMs >= (state.contextMenuHintDelaySeconds * 1_000f).toLong()

fun shouldShowSettingsHint(state: CrossbarUiState, idleMs: Long): Boolean =
    state.contextMenuHintEnabled &&
        state.activeSettingsScreen != null &&
        idleMs >= (state.contextMenuHintDelaySeconds * 1_000f).toLong()

internal fun gameMetadataLine(
    releaseYear: Int?,
    genre: String?,
    developer: String?,
    players: String?,
): String? {
    fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    val parts = listOfNotNull(

        releaseYear?.takeIf { it > 0 }?.toString(),
        genre.clean(),
        developer.clean(),

        players.clean()?.let { if (it == "1") "1 player" else "$it players" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("   ·   ")
}

data class CrossbarItem(
    val id: String,
    val title: String,
    // a row of Recent's pinned list (owner, 2026-10-06)
    val pinnedToRecent: Boolean = false,
    // pinned to the top of its category; the menu offers Unpin from this, not from the "Pinned" subtitle
    val pinnedInCategory: Boolean = false,
    val artworkUri: String? = null,
    val iconUri: String? = null,
    val logoUri: String? = null,

    val subtitle: String? = null,

    val metadataLine: String? = null,

    val description: String? = null,
    val romPath: String? = null,

    val totalPlayTimeMillis: Long = 0L,
    // a game's genre: its edit, else what the scraped text reads as (owner, 2026-10-08)
    val genre: com.echo.core.domain.model.GameGenre? = null,

    val insideCovers: List<String> = emptyList(),
    val gameId: Long? = null,
    val platformId: String? = null,
    val iconKey: String? = null,
    val accentColor: Long? = null,
    val isFavorite: Boolean = false,

    val playState: String? = null,
    val isAndroidApp: Boolean = false,

    val isRealGame: Boolean = false,
    val packageName: String? = null,

    val shortcutId: String? = null,

    val launchIntentUri: String? = null,

    val musicFolderId: String? = null,

    val mediaRootUri: String? = null,
    val mediaRootKind: MediaRootKind? = null,

    val musicGroupKey: String? = null,
    val mediaUri: String? = null,
    val mimeType: String? = null,

    val coverUri: String? = null,

    val progressFraction: Float? = null,

    val progressLabel: String? = null,

    val playlistId: Long? = null,

    val textOnly: Boolean = false,

    val type: CrossbarItemType = CrossbarItemType.STANDARD,

    val lastOpenedAt: Long? = null,
) {
    val backdropArt: List<String>
        get() = listOfNotNull(artworkUri, coverUri, iconUri)
            .filter { it.isNotBlank() && it != CrossbarViewModel.MEMORY_CARD_ASSET_URI }

    val shelfCoverArt: String?
        get() = listOfNotNull(coverUri, artworkUri, iconUri)
            .firstOrNull { it.isNotBlank() && it != CrossbarViewModel.MEMORY_CARD_ASSET_URI }

    val tileArt: String?
        get() = listOfNotNull(iconUri, coverUri, artworkUri)
            .firstOrNull { it.isNotBlank() && it != CrossbarViewModel.MEMORY_CARD_ASSET_URI }

    val hasVisibleLogo: Boolean
        get() = !logoUri.isNullOrBlank() && backdropArt.isNotEmpty()
}

data class BackgroundTaskInfo(
    val id: String,
    val label: String,
    val progress: Float?,
)

internal fun CrossbarUiState.withSettingsClosed(): CrossbarUiState = copy(
    activeSettingsScreen = null,
    settingsReturnTo = null,
    pendingSettingsAction = null,
    settingsFromPanel = false,
)

// owner, 2026-10-05: a screen opened while the App Drawer or Search is up (from the top panel, say) opened
// under it, so nothing seemed to happen until the drawer was closed. Opening a screen closes both
internal fun CrossbarUiState.withDrawerAndSearchClosed(): CrossbarUiState = copy(
    activeAppDrawerFilter = null,
    pendingDrawerAction = null,
    pendingDrawerTypedChar = null,
    drawerLetterRailHeld = false,
    search = null,
)

enum class GlobalStep { APPS, SEARCH, CLOSE_SEARCH, NOTIFICATIONS, HOME }

// owner, 2026-10-06: the same buttons on every screen but the players and editors, which keep the bumpers
// for paging and seeking: LB Apps, RB Search, Start the notifications, Home (the guide button, or Start
// held) back to the crossbar. Null leaves the press to the screen.
internal fun globalStep(action: GamepadAction, s: CrossbarUiState): GlobalStep? {
    val keepsItsButtons = s.activePhotoViewer != null || s.activeVideoId != null || s.metadataPreview != null ||
        s.manualViewer != null || s.artworkStudioGameId != null || s.showBootSequence || s.activeGameBoot != null ||
        s.customIconSession != null || s.saveThemeNameDialog != null
    if (keepsItsButtons) return null
    return when (action) {
        // LB in the drawer closes it, as RB in Search closes Search; from the drawer's own search, back to it
        GamepadAction.PREV_PAGE -> GlobalStep.APPS.takeUnless { s.activeAppDrawerFilter != null && s.search == null }
        GamepadAction.NEXT_PAGE -> when {
            s.search != null -> GlobalStep.CLOSE_SEARCH
            // in the drawer, Search is the drawer's own
            s.activeAppDrawerFilter != null -> null
            else -> GlobalStep.SEARCH
        }
        GamepadAction.OPEN_NOTIFICATIONS -> GlobalStep.NOTIFICATIONS.takeIf { s.statusStripVisible }
        // the first-run wizard is not left this way
        GamepadAction.HOME -> GlobalStep.HOME.takeUnless { s.activeSettingsScreen in CrossbarViewModel.WIZARD_SCREEN_IDS }
        else -> null
    }
}

// Game Info, Profile and an app's page draw above the drawer and Search, so they close for them
internal fun CrossbarUiState.withScreensOverCrossbarClosed(): CrossbarUiState =
    copy(gameInfo = null, profile = null)

internal fun CrossbarUiState.withSettingsOpen(screenId: String): CrossbarUiState = withDrawerAndSearchClosed().copy(
    activeSettingsScreen = screenId,
    gameInfo = null,
    profile = null,
)

internal fun CrossbarUiState.withGameInfoOpen(info: GameInfoState): CrossbarUiState =
    withDrawerAndSearchClosed().copy(gameInfo = info, profile = null)

// a prompt with a text field is open; the same four withNamePromptText fills
val CrossbarUiState.namePromptOpen: Boolean
    get() = renameAppTarget != null || collectionNameDialog != null || playlistNameDialog != null || saveThemeNameDialog != null

fun CrossbarUiState.withNamePromptText(text: String): CrossbarUiState = when {
    renameAppTarget != null      -> copy(renameAppText = text)
    collectionNameDialog != null -> copy(collectionNameDialog = collectionNameDialog.copy(text = text))
    playlistNameDialog != null   -> copy(playlistNameDialog = playlistNameDialog.copy(text = text))
    saveThemeNameDialog != null  -> copy(saveThemeNameDialog = saveThemeNameDialog.copy(text = text))
    else -> this
}

@HiltViewModel
class CrossbarViewModel @Inject constructor(
    internal val gameRepository: GameRepository,
    internal val platformDao: PlatformDao,
    internal val memoryCardRepository: MemoryCardRepository,
    private val categoryRepository: CategoryRepositoryImpl,
    internal val appCategoryRepository: AppCategoryRepository,
    internal val gameCategoryRepository: com.echo.core.data.repository.GameCategoryRepository,
    private val launcherShortcutRepository: LauncherShortcutRepository,
    private val libraryScanner: LibraryScanner,
    private val artworkRepository: ArtworkRepository,
    @ApplicationContext internal val context: Context,
    internal val gamepadInputHandler: GamepadInputHandler,
    private val remapCoordinator: com.echo.core.data.repository.RemapCoordinator,
    private val mappingRepository: ControllerMappingRepository,
    private val controllerLayoutRepository: com.echo.core.data.repository.ControllerLayoutRepository,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
    internal val musicRepository: com.echo.core.domain.repository.MusicRepository,
    private val musicScanner: com.echo.feature.library.scanner.MusicScanner,
    internal val musicPlayer: com.echo.feature.crossbar.music.MusicPlayerController,
    private val emulatorProfileRepository: com.echo.feature.launcher.EmulatorProfileRepository,
    private val intentResolver: com.echo.feature.launcher.EmulatorIntentResolver,
    internal val videoRepository: com.echo.core.domain.repository.VideoRepository,
    internal val photoRepository: com.echo.core.domain.repository.PhotoRepository,
    private val photoScanner: com.echo.feature.library.scanner.PhotoScanner,
    internal val bookRepository: com.echo.core.domain.repository.BookRepository,
    private val bookIntentResolver: com.echo.core.data.book.BookIntentResolver,
    private val hiddenPlacementDao: com.echo.core.data.database.dao.HiddenPlacementDao,
    private val iconDisplayPreferences: com.echo.core.data.repository.IconDisplayPreferences,
    internal val artworkStore: com.echo.feature.artwork.store.ArtworkStore,
    internal val artworkAccent: com.echo.core.data.repository.ArtworkAccent,
    private val windowsLibrarySetup: com.echo.core.data.repository.WindowsLibrarySetup,
    private val pcShortcutImporter: com.echo.feature.launcher.PcShortcutImporter,
    private val pcGameScanner: com.echo.feature.settings.pc.PcGameScanner,
    private val pcGameExporter: com.echo.feature.settings.pc.PcGameExporter,
    private val launchDispatcher: com.echo.feature.launcher.LaunchDispatcher,
    private val launchResolver: com.echo.feature.launcher.GameLaunchResolver,
    private val setupStateProvider: com.echo.feature.launcher.SetupStateProvider,
    internal val customIconStore: CustomIconStore,
    internal val echoThemeStore: EchoThemeStore,
    internal val motionWallpaper: com.echo.core.data.wallpaper.MotionWallpaper,
    internal val uiMediaStore: com.echo.core.data.repository.UiMediaStore,
    private val gameBootGate: com.echo.feature.launcher.GameBootGate,
    private val mediaLaunchGate: com.echo.core.data.launch.MediaLaunchGate,

    internal val uiMediaAudioPlayer: com.echo.core.ui.media.UiMediaAudioPlayer,
    internal val mediaRootRepository: com.echo.core.data.repository.MediaRootRepository,
    private val videoScanner: com.echo.feature.library.scanner.VideoScanner,
    private val bookScanner: com.echo.feature.library.scanner.BookScanner,
    private val musicIntentResolver: com.echo.core.data.music.MusicIntentResolver,
    private val videoIntentResolver: com.echo.core.data.video.VideoIntentResolver,
    private val photoIntentResolver: com.echo.core.data.photo.PhotoIntentResolver,
    private val autoCoreMemory: com.echo.feature.launcher.AutoCoreMemory,
    private val romRootRepository: com.echo.core.data.repository.RomRootRepository,
    internal val achievementController: com.echo.feature.achievements.AchievementController,
    internal val achievementCredentials: com.echo.core.data.achievement.AchievementCredentialsProvider,
    internal val discordSocial: com.echo.core.data.discord.DiscordSocialRepository,
    bottomScreenLink: com.echo.feature.crossbar.bottomscreen.BottomScreenLink,
) : ViewModel() {
    @Volatile
    private var lastInteractionMs: Long = 0L

    private val _uiState = MutableStateFlow(CrossbarUiState())
    val uiState: StateFlow<CrossbarUiState> = _uiState.asStateFlow()

    internal val gameActions = CrossbarGames(this, _uiState, viewModelScope, memoryCardRepository, menuSound)
    internal val move = CrossbarMove(this, _uiState, viewModelScope, context, categoryRepository, memoryCardRepository, menuSound)

    internal val artworkTools = CrossbarArtwork(this, _uiState, viewModelScope, artworkRepository, menuSound)

    internal val launching = CrossbarLauncher(this, _uiState, viewModelScope, launchDispatcher, launchResolver, intentResolver, gameBootGate, mediaLaunchGate, launcherShortcutRepository, menuSound)

    internal val appPickerSection = CrossbarAppPicker(this, _uiState, viewModelScope, menuSound)

    internal val folders = CrossbarFolders(this, _uiState, viewModelScope, mediaRootRepository, romRootRepository, libraryScanner, pcGameScanner, menuSound)

    internal val look = CrossbarLook(this, _uiState, viewModelScope, menuSound)

    internal val recents = CrossbarRecents(this, _uiState, viewModelScope, menuSound)

    internal val gameDetail = CrossbarGameInfo(this, _uiState, viewModelScope, menuSound)
    internal val bottomScreen = com.echo.feature.crossbar.bottomscreen.CrossbarBottomScreen(
        this, _uiState, viewModelScope, bottomScreenLink, launchDispatcher.lastLaunch,
    )

    private val launchHold = LaunchHold(viewModelScope) { id -> _uiState.update { it.copy(launchHold = id) } }

    // true only while a physical A press is being dispatched; a tap on screen launches at once
    // the pad button being dispatched right now, A or Y; a tap on screen acts at once
    private var heldFromPad: GamepadAction? = null
    private var holdButton: GamepadAction? = null

    internal fun startLaunchHold(item: CrossbarItem, launch: () -> Unit) = launchHold.start(item.id, launch)

    internal fun releaseLaunchHold() { launchHold.release() }

    // starts the launch ring when a pad A press would leave ECHO; false means act now
    internal fun holdToLaunch(item: CrossbarItem, launch: () -> Unit): Boolean {
        if (heldFromPad != GamepadAction.SELECT || !item.launchesOut()) return false
        holdButton = GamepadAction.SELECT
        launchHold.start(item.id, launch)
        return true
    }

    // Resume is a launch too, so it takes the same hold, on Y; Y let go early opens the menu instead
    private fun holdToResume(item: CrossbarItem, resume: () -> Unit): Boolean {
        if (heldFromPad != GamepadAction.OPEN_CONTEXT_MENU) return false
        holdButton = GamepadAction.OPEN_CONTEXT_MENU
        launchHold.start(resumeHoldId(item.id), resume)
        return true
    }

    internal val panel = CrossbarPanel(this, _uiState, viewModelScope, menuSound)
    internal val genres = CrossbarGenres(this, _uiState, viewModelScope)

    internal val librarySearch = CrossbarSearch(this, _uiState, viewModelScope, menuSound)

    fun typeToSearchAllowed(): Boolean = librarySearch.typeToSearchAllowed()

    internal val bookshelf = CrossbarBookshelf(this, _uiState, viewModelScope, bookRepository, bookIntentResolver, bookScanner, menuSound)

    internal val gallery = CrossbarGallery(this, _uiState, viewModelScope, photoRepository, photoScanner, photoIntentResolver, menuSound)

    internal val video = CrossbarVideo(this, _uiState, viewModelScope, videoRepository, videoScanner, videoIntentResolver, menuSound)

    internal val music = CrossbarMusic(this, _uiState, viewModelScope, musicRepository, musicPlayer, musicScanner, menuSound, artworkAccent)

    val musicPositionMs: StateFlow<Int> get() = musicPlayer.positionMs

    val externalPositionMs: StateFlow<Long> get() = AndroidNotifications.playbackPositionMs

    private var currentItemsJob: Job? = null

    internal var platformCache: Map<String, PlatformEntity> = emptyMap()
    internal var enabledCards: List<MemoryCard> = emptyList()
    internal val taskNotifier = BackgroundTaskNotifier(context)

    init {
        viewModelScope.launch {
            _uiState.map { it.activeContextMenu == null }.distinctUntilChanged().collect { closed ->
                if (closed) _uiState.update { if (it.menuOnCompanion) it.copy(menuOnCompanion = false) else it }
            }
        }
    }

    init {
        gamepadInputHandler.scope = viewModelScope

        musicPlayer.onTrackStarted = { track ->
            viewModelScope.launch {
                runCatching { musicRepository.markTrackPlayed(track.id, System.currentTimeMillis()) }
                    .onFailure { Timber.w(it, "Could not stamp ${track.displayTitle} as played") }
            }
        }
        observeContextMenuHintIdle()
        observeIconPreferences()
        observeFocusedGameVideo()
        observeFocusedItemAccent()
        bottomScreen.observe()
        recents.observeRecentTopAccent()
        observeBackgroundSettings()
        observeTouchNavButtonMode()
        look.observeWallpaper()
        observeLibrarySetupState()
        checkInitialSetup()
        logStartupSequence()
        look.observeColorScheme()
        look.observeFocusAndMotion()
        observeCategoryBar()
        observeLibraryChips()
        panel.observeProfilePrefs()
        panel.observeProfileData()
        observeCategories()
        observeMissingGames()
        observeAppChanges()
        observeGamepadMappings()
        launching.observeBootPreferences()
        launching.observeGameBoot()
        launching.observeMediaLaunch()
        music.observeMusic()
        video.observeVideo()
        gallery.observePhoto()
        bookshelf.observeBooks()
        artworkTools.observeMediaCovers()
        bookshelf.observeContinueBook()
        observeHiddenPlacements()
        observeColumnOrders()
        panel.observeAndroidNotices()
        recents.observeRecentTop()
        recents.observeShelfCounts()
        collectGamepadActions()
        consumeWindowsSetupPrompt()
        launching.observeLaunchRecoveryRequests()
        observeSetupState()

        pcShortcutImporter.watchPinChanges(viewModelScope)
    }

    private fun consumeWindowsSetupPrompt() {
        viewModelScope.launch {
            if (runCatching { windowsLibrarySetup.consumeSetupPrompt() }.getOrDefault(false)) {
                _uiState.update { it.copy(showWindowsSetupPrompt = true) }
            }
        }
    }

    fun confirmWindowsSetupPrompt() = _uiState.update {
        it.withSettingsOpen("settings_library").copy(showWindowsSetupPrompt = false)
    }

    fun dismissWindowsSetupPrompt() = _uiState.update { it.copy(showWindowsSetupPrompt = false) }

    private fun gameMetaLabel(g: Game): String = gameMetaLine(
        platform = platformCache[g.platformId]?.name ?: g.platformId,
        lastPlayedAt = g.lastPlayedAt,
        publisher = g.publisher,
    )

    private fun observeCategoryBar() {
        viewModelScope.launch {
            categoryRepository.observeVisible().collect { categories ->
                val allCategories = canonicalCrossbarCategories(categories.ifEmpty { FALLBACK_CATEGORIES })
                val prevId   = _uiState.value.categories.getOrNull(_uiState.value.selectedCategoryIndex)?.id
                val isInitialSelection = _uiState.value.categories.isEmpty()

                val newIndex = if (isInitialSelection) {
                    defaultCrossbarCategoryIndex(allCategories)
                } else {
                    allCategories.indexOfFirst { it.id == prevId }
                        .takeIf { it >= 0 }
                        ?: defaultCrossbarCategoryIndex(allCategories)
                }
                val newId    = allCategories.getOrNull(newIndex)?.id

                _uiState.update { it.copy(categories = allCategories, selectedCategoryIndex = newIndex) }

                if (newId != prevId || _uiState.value.currentItems.isEmpty()) {
                    tintWaveForCategory(allCategories.getOrNull(newIndex))
                    loadItemsForCategory(allCategories.getOrNull(newIndex))
                }
            }
        }
    }

    private fun observeMissingGames() {
        viewModelScope.launch {
            gameRepository.observeMissing().collect { missing ->
                val was = _uiState.value.missingCount
                _uiState.update { it.copy(missingCount = missing.size) }

                if ((was == 0) != (missing.size == 0) &&
                    currentCategory()?.id == BuiltInCategory.GAMES
                ) {
                    loadItemsForCategory(currentCategory())
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            combine(
                memoryCardRepository.observeEnabled(),
                gameRepository.observeAll(),
                platformDao.observeAll(),
                gameRepository.observeFavorites(),
            ) { cards, games, platforms, favorites ->
                CardsGamesPlatforms(cards, games, platforms, favorites)
            }
                .collect { (cards, games, platforms, favorites) ->
                    platformCache = platforms.associateBy { it.id }
                    enabledCards  = cards

                    val displayGames = games.projectGamesForDisplay()
                    val counts = displayGames.filter { it.contentType == GameContentType.GAME }
                        .groupBy { it.platformId }.mapValues { it.value.size }
                    val gamesOnlyTotal = displayGames.count { it.contentType == GameContentType.GAME }

                    val favoritesTotal = favorites.size

                    fun fanOf(games: List<Game>): List<String> = fanCoversOf(games)
                    val realGames = displayGames.filter { it.contentType == GameContentType.GAME }
                    val fanCovers = buildMap<String, List<String>> {
                        put(ALL_GAMES_ITEM_ID, fanOf(realGames))

                        realGames.groupBy { it.platformId }
                            .forEach { (pid, list) -> put(cardItemId(pid), fanOf(list)) }
                        realGames.groupBy { com.echo.core.domain.model.effectiveGenre(it.genre, it.genreOverride) }
                            .forEach { (genre, list) -> genre?.let { put(genreItemId(it), fanOf(list)) } }
                    }
                    val genreCounts = realGames.mapNotNull { com.echo.core.domain.model.effectiveGenre(it.genre, it.genreOverride) }
                        .groupingBy { it }.eachCount()

                    val validPlatformId = _uiState.value.selectedPlatformId
                        ?.takeIf { id ->
                            id == ALL_GAMES_PLATFORM_ID ||
                                id == FAVORITES_PLATFORM_ID ||
                                id == MISSING_PLATFORM_ID ||
                                cards.any { c -> c.platformId == id }
                        }

                    _uiState.update { it.copy(
                        platformGameCounts = counts,
                        genreCounts = genreCounts,
                        allGamesCount = gamesOnlyTotal,
                        cardFanCovers = fanCovers,
                        favoritesCount = favoritesTotal,
                        selectedPlatformId = validPlatformId,
                    )}

                    if (categoryShowsGameRows(currentCategory())) {
                        loadItemsForCategory(currentCategory(), keepCursorOnRow = true)
                    }
                }
        }
    }

    private data class CardsGamesPlatforms(
        val cards: List<MemoryCard>,
        val games: List<Game>,
        val platforms: List<PlatformEntity>,
        val favorites: List<Game>,
    )

    private fun observeAppChanges() {
        viewModelScope.launch {
            appCategoryRepository.changes().collect {
                val category = currentCategory() ?: return@collect
                if (isAppCategory(category.id)) loadItemsForCategory(category)
            }
        }
    }

    internal fun currentCategory(): Category? = _uiState.value.currentCategoryOrNull()

    private fun isAppCategory(categoryId: String): Boolean =
        categoryId != BuiltInCategory.SETTINGS && categoryId != BuiltInCategory.GAMES

    private val nonGameRowCategoryIds = setOf(
        BuiltInCategory.FAVORITES, BuiltInCategory.RECENTLY_PLAYED, BuiltInCategory.MUSIC,
        BuiltInCategory.VIDEO, BuiltInCategory.PHOTO, BuiltInCategory.ANDROID,
        BuiltInCategory.APP_DRAWER, BuiltInCategory.SETTINGS,
    )

    private fun categoryShowsGameRows(category: Category?): Boolean {
        if (category == null) return false
        return category.isGamingCategory || category.id !in nonGameRowCategoryIds
    }


    internal fun loadItemsForCategory(category: Category?, keepCursorOnRow: Boolean = false) {
        currentItemsJob?.cancel()
        if (category == null) { _uiState.update { it.copy(currentItems = emptyList(), sortLabel = null, drillTitle = null, drillSiblings = emptyList(), drillSiblingIndex = 0) }; return }
        val drill = computeDrillTitle()
        val (sibs, sibIdx) = if (drill != null) computeDrillSiblings(category) else (emptyList<CrossbarItem>() to 0)
        _uiState.update { it.copy(sortLabel = currentSortLabel(), drillTitle = drill, drillSiblings = sibs, drillSiblingIndex = sibIdx) }

        currentItemsJob = viewModelScope.launch {
            when (category.id) {
                BuiltInCategory.FAVORITES -> {
                    var keepCursor = keepCursorOnRow
                    gameRepository.observeFavorites().collect { games ->
                        publishGameItems(games.notHiddenAt(HideLocationType.FAVORITES).gameSorted(_uiState.value.gameSortMode).toCrossbarItems(), keepCursor)
                        keepCursor = true
                    }
                }
                BuiltInCategory.SHELVES -> {
                    var keepCursor = keepCursorOnRow
                    when (val shelf = shelfCardFor(_uiState.value.selectedPlatformId)) {
                        null -> _uiState.update { s ->
                            val items = s.shelfRows()
                            s.copy(
                                currentItems = items,
                                selectedItemIndex = s.selectedItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                            )
                        }

                        is ShelfCard.Favorites -> gameRepository.observeFavorites().collect { games ->
                            publishGameItems(
                                games.notHiddenAt(HideLocationType.FAVORITES)
                                    .gameSorted(_uiState.value.gameSortMode).toCrossbarItems(),
                                keepCursor,
                            )
                            keepCursor = true
                        }
                        is ShelfCard.Marked -> gameRepository.observeByPlayState(shelf.state).collect { games ->
                            publishGameItems(
                                games.notHiddenAt(HideLocationType.ALL_GAMES)
                                    .gameSorted(_uiState.value.gameSortMode).toCrossbarItems(),
                                keepCursor,
                            )
                            keepCursor = true
                        }

                        is ShelfCard.RecentlyAdded -> gameRepository.observeRecentlyAdded().collect { games ->
                            publishGameItems(
                                games.notHiddenAt(HideLocationType.ALL_GAMES).toCrossbarItems(),
                                keepCursor,
                            )
                            keepCursor = true
                        }
                    }
                }
                BuiltInCategory.RECENTLY_PLAYED -> recents.loadColumn(keepCursorOnRow)
                BuiltInCategory.ANDROID -> {
                    _uiState.update { it.copy(currentItems = ANDROID_ITEMS) }
                }
                BuiltInCategory.SETTINGS -> {
                    _uiState.update { state ->
                        val items = SETTINGS_ROOT_ITEMS
                        state.copy(
                            currentItems = items,
                            selectedItemIndex = state.selectedItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                        )
                    }
                }
                BuiltInCategory.GAMES -> {
                    val platformId = _uiState.value.selectedPlatformId
                    if (_uiState.value.romFoldersOpen) {
                        _uiState.update { it.copy(currentItems = folders.romFolderItems()) }
                    } else if (platformId == ALL_GAMES_PLATFORM_ID) {
                        var keepCursor = keepCursorOnRow
                        gameRepository.observeAllGames().collect { games ->
                            val visible = games.notHiddenAt(HideLocationType.ALL_GAMES)
                            val items = if (visible.isEmpty()) listOf(emptyAllGamesItem())
                                        else visible.gameSorted(_uiState.value.gameSortMode).toCrossbarItems()
                            publishGameItems(items, keepCursor)
                            keepCursor = true
                        }
                    } else if (platformId == FAVORITES_PLATFORM_ID) {
                        var keepCursor = keepCursorOnRow
                        gameRepository.observeFavorites().collect { games ->
                            val visible = games.notHiddenAt(HideLocationType.FAVORITES)
                            val items = if (visible.isEmpty()) listOf(gameActions.emptyFavoritesItem())
                                        else visible.gameSorted(_uiState.value.gameSortMode).toCrossbarItems()
                            publishGameItems(items, keepCursor)
                            keepCursor = true
                        }
                    } else if (platformId == MISSING_PLATFORM_ID) {
                        var keepCursor = keepCursorOnRow
                        gameRepository.observeMissing().collect { games ->
                            val items = if (games.isEmpty()) listOf(emptyMissingItem())
                                        else games.gameSorted(_uiState.value.gameSortMode)
                                            .toCrossbarItems()

                                            .map { it.copy(subtitle = MISSING_REASON) }
                            publishGameItems(items, keepCursor)
                            keepCursor = true
                        }
                    } else if (platformId != null) {
                        var keepCursor = keepCursorOnRow
                        gameRepository.observePlatformGames(platformId).collect { all ->

                            val games = all.filter { it.contentType == GameContentType.GAME }

                            val visible = if (platformId == ANDROID_PLATFORM_ID)
                                games.notHiddenAt(HideLocationType.ANDROID_PLATFORM)
                            else
                                games.notHiddenAt(HideLocationType.PLATFORM, platformId)
                            val items = if (visible.isEmpty()) listOf(folders.emptyFolderItem(platformId))
                                        else visible.gameSorted(_uiState.value.gameSortMode).toCrossbarItems()
                            publishGameItems(items, keepCursor)
                            keepCursor = true
                        }
                    } else {
                        combine(
                            memoryCardRepository.observeEnabled(),
                            gameRepository.observeAll(),
                        ) { _, _ -> }.collect {
                            _uiState.update { it.copy(currentItems = gameActions.memoryCardItems()) }
                        }
                    }
                }
                BuiltInCategory.MUSIC -> music.loadColumn()
                BuiltInCategory.VIDEO -> video.loadColumn()
                BuiltInCategory.PHOTO -> gallery.loadColumn()
                BuiltInCategory.LIBRARY -> bookshelf.loadColumn()
                else -> {
                    if (category.isGamingCategory) {
                        val gameRows = gameCategoryRepository.itemsForCategory(category.id)
                            .filterIsInstance<com.echo.core.data.repository.GameCategoryItem.GameItem>()
                            .filterNot { isHiddenAt(HiddenPlacement.gameKey(it.game.id), HideLocationType.CATEGORY, category.id) }
                        val pinnedGameIds = gameRows.filter { it.pinned }.map { it.game.id }.toSet()
                        val gameItems = gameRows.map { it.game }.gameSorted(_uiState.value.gameSortMode).toCrossbarItems().map { crossbar ->
                            if (crossbar.gameId in pinnedGameIds) crossbar.copy(subtitle = "Pinned", pinnedInCategory = true) else crossbar
                        }

                        val combined = gameItems
                        val items = if (combined.isEmpty()) listOf(emptyCategoryItem(category)) else combined

                        publishGameItems(items + addGamesItem(), keepCursorOnRow)
                    } else {
                        val apps = appCategoryRepository.appsForCategory(category.id)
                            .notHiddenAt(HideLocationType.CATEGORY, category.id)
                        val appItems = apps.map { it.toCrossbarItem(gameRepository.getAppEntry(it.packageName)) }

                        val combined = appItems
                        val items = if (combined.isEmpty()) listOf(emptyCategoryItem(category)) else combined

                        val lead = if (category.id == NETWORK_CATEGORY_ID) listOf(librarySearch.quickSearchItem()) else emptyList()

                        _uiState.update { it.copy(currentItems = lead + orderedColumn(items, columnOrderFor(category.id)) + addAppsItem()) }
                    }
                }
            }
        }
    }

    private fun CategorizedApp.toCrossbarItem(
        artwork: com.echo.core.domain.model.Game? = null,
    ): CrossbarItem = CrossbarItem(
        id           = "app_$packageName",
        title        = label,
        subtitle     = if (pinned) "Pinned" else null,
        pinnedInCategory = pinned,
        packageName  = packageName,
        isAndroidApp = true,
        iconUri      = artwork?.let { it.iconUri ?: it.artworkUri },

        artworkUri   = artwork?.artworkUri,
        accentColor  = artwork?.let { platformCache[it.platformId]?.accentColor },
    )

    internal fun libraryColumn(body: List<CrossbarItem>, scope: SearchScope): List<CrossbarItem> =
        body + librarySearch.librarySearchItem(scope)

    internal fun mediaRootColumn(
        hasFolders: Boolean,
        sections: List<CrossbarItem>,
        apps: List<CrossbarItem>,
        addRows: List<CrossbarItem>,
        scope: SearchScope,
    ): List<CrossbarItem> =
        orderedColumn(if (hasFolders) mediaColumn(sections, apps, addRows) else folderlessColumn(apps, addRows), columnOrderFor(currentCategory()?.id))
            .let { if (hasFolders) libraryColumn(it, scope) else it }
            .map { row -> if (row.isColumnFoldersRow) row.copy(title = columnSettingsTitle(currentCategory()?.name)) else row }

    private fun addAppsItem(): CrossbarItem = CrossbarItem(
        id       = ADD_APPS_ITEM_ID,
        title    = "Add Apps",
        subtitle = "Pick installed apps to add to this section",
        type     = CrossbarItemType.ADD_ACTION,
    )

    private fun addGamesItem(): CrossbarItem = CrossbarItem(
        id       = ADD_GAMES_ITEM_ID,
        title    = "Add Games",
        subtitle = "Pick games to add to this category",
        type     = CrossbarItemType.ADD_ACTION,
    )

    private fun currentAddActions(): List<CrossbarItem> = when (currentCategory()?.id) {
        BuiltInCategory.MUSIC   -> music.musicAddActions()
        BuiltInCategory.VIDEO   -> video.videoAddActions()
        BuiltInCategory.PHOTO   -> gallery.photoAddActions()
        BuiltInCategory.LIBRARY -> bookshelf.booksAddActions()
        else -> emptyList()
    }

    internal fun openAddMenu() {
        val actions = currentAddActions()
        if (actions.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Add", rows = actions.map { CrossbarContextMenuItem(it.id, it.title) }), isAddMenu = true)
            )
        }
    }

    internal suspend fun musicAppItems(): List<CrossbarItem> {
        val apps = appCategoryRepository.appsForCategory(MUSIC_APPS_CATEGORY_ID)
            .notHiddenAt(HideLocationType.CATEGORY, MUSIC_APPS_CATEGORY_ID)
        return apps.map { it.toCrossbarItem(gameRepository.getAppEntry(it.packageName)) }
    }

    @Volatile private var hiddenKeys: Set<String> = emptySet()

    @Volatile private var columnOrders: Map<String, List<String>> = emptyMap()

    internal fun columnOrderFor(categoryId: String?): List<String> = categoryId?.let { columnOrders[it] }.orEmpty()

    private fun observeColumnOrders() {
        viewModelScope.launch {
            context.echoDataStore.data
                .map { prefs ->
                    prefs.asMap().mapNotNull { (key, value) ->
                        key.name.removePrefix(COLUMN_ORDER_PREFIX).takeIf { key.name.startsWith(COLUMN_ORDER_PREFIX) }
                            ?.let { it to (value as? String).orEmpty().split('\n').filter(String::isNotBlank) }
                    }.toMap()
                }
                .distinctUntilChanged()
                .collect { orders ->
                    columnOrders = orders
                    loadItemsForCategory(currentCategory())
                }
        }
    }

    private fun observeHiddenPlacements() {
        viewModelScope.launch {
            hiddenPlacementDao.observeAll().collect { rows ->
                hiddenKeys = rows.map { "${it.itemKey}|${it.locationType}|${it.locationId}" }.toSet()

                loadItemsForCategory(currentCategory())
            }
        }
    }

    internal fun isHiddenAt(itemKey: String, type: HideLocationType, locationId: String = ""): Boolean =
        hiddenKeys.contains("$itemKey|${type.name}|$locationId")

    @JvmName("gamesNotHiddenAt")
    internal fun List<Game>.notHiddenAt(type: HideLocationType, locationId: String = ""): List<Game> =
        filterNot { isHiddenAt(HiddenPlacement.gameKey(it.id), type, locationId) }

    @JvmName("appsNotHiddenAt")
    private fun List<CategorizedApp>.notHiddenAt(type: HideLocationType, locationId: String = ""): List<CategorizedApp> =
        filterNot { isHiddenAt(HiddenPlacement.appKey(it.packageName), type, locationId) }

    internal fun persistHide(itemKey: String, itemLabel: String, type: HideLocationType, locationId: String, locationLabel: String) {
        viewModelScope.launch {
            hiddenPlacementDao.upsert(
                HiddenPlacementEntity(itemKey, itemLabel, type.name, locationId, locationLabel, System.currentTimeMillis())
            )
        }
    }

    /**
     * Drops an app off the Last Played shelf until it is used again. Deliberately not
     * persistHide: hiding is permanent until undone in Settings, and these are two
     * different things that used to be one.
     */
    private fun categoryDisplayName(id: String): String = _uiState.value.categoryDisplayNameOf(id)

    private fun knownPlatformName(platformId: String): String? =
        enabledCards.firstOrNull { it.platformId == platformId }?.displayName
            ?: shelfCardFor(platformId)?.title
            ?: platformCache[platformId]?.name

    internal fun currentHideLocation(): Triple<HideLocationType, String, String>? {
        val s = _uiState.value
        val cat = currentCategory()
        return when {
            s.selectedPlatformId == FAVORITES_PLATFORM_ID || cat?.id == BuiltInCategory.FAVORITES ->
                Triple(HideLocationType.FAVORITES, "", "Favorites")

            s.selectedPlatformId == MISSING_PLATFORM_ID -> null
            s.selectedPlatformId == ANDROID_PLATFORM_ID -> Triple(HideLocationType.ANDROID_PLATFORM, "", "Android")

            s.selectedPlatformId == ALL_GAMES_PLATFORM_ID ->
                Triple(HideLocationType.ALL_GAMES, "", "All Games")

            s.selectedPlatformId != null -> {
                val name = knownPlatformName(s.selectedPlatformId) ?: s.selectedPlatformId
                Triple(HideLocationType.PLATFORM, s.selectedPlatformId, name)
            }

            cat != null && cat.isGamingCategory && cat.id != BuiltInCategory.GAMES ->
                Triple(HideLocationType.CATEGORY, cat.id, cat.name)

            // owner, 2026-10-05: hidden from the Recent panel only, until it is unhidden in Hidden Items
            cat?.id == BuiltInCategory.RECENTLY_PLAYED -> Triple(HideLocationType.RECENTS, "", "Recent")
            else -> null
        }
    }

    internal suspend fun videoAppItems(): List<CrossbarItem> {
        val apps = appCategoryRepository.appsForCategory(VIDEO_APPS_CATEGORY_ID)
            .notHiddenAt(HideLocationType.CATEGORY, VIDEO_APPS_CATEGORY_ID)
        return apps.map { it.toCrossbarItem(gameRepository.getAppEntry(it.packageName)) }
    }

    private val viewCursor = mutableMapOf<String, Int>()

    private fun viewCursorKey(s: CrossbarUiState): String {
        val catId = s.categories.getOrNull(s.selectedCategoryIndex)?.id ?: "none"
        val sub = when {
            catId == BuiltInCategory.MUSIC -> "music_${music.musicNavKey(s.musicNav)}"
            catId == BuiltInCategory.VIDEO -> "video_${video.videoNavKey(s.videoNav)}"
            catId == BuiltInCategory.PHOTO -> "photo_${gallery.photoNavKey(s.photoNav)}"
            catId == BuiltInCategory.LIBRARY -> "books_${bookshelf.booksNavKey(s.booksNav)}"
            catId == BuiltInCategory.SETTINGS -> "settings_root"
            s.selectedPlatformId != null   -> "plat_${s.selectedPlatformId}"
            else                           -> "root"
        }
        return "$catId/$sub"
    }

    internal fun navigateRememberingCursor(mutate: (CrossbarUiState) -> CrossbarUiState) {
        val cur = _uiState.value
        viewCursor[viewCursorKey(cur)] = cur.selectedItemIndex
        _uiState.update { state ->
            val next = mutate(state)
            val remembered = viewCursor[viewCursorKey(next)] ?: 0
            next.copy(selectedItemIndex = remembered)
        }
        loadItemsForCategory(currentCategory())
    }

    internal suspend fun bookAppItems(): List<CrossbarItem> {
        val apps = appCategoryRepository.appsForCategory(LIBRARY_APPS_CATEGORY_ID)
            .notHiddenAt(HideLocationType.CATEGORY, LIBRARY_APPS_CATEGORY_ID)
        return apps.map { it.toCrossbarItem(gameRepository.getAppEntry(it.packageName)) }
    }

    internal fun emptySeriesItem(): CrossbarItem = CrossbarItem(
        id       = "series_empty",
        title    = "No series yet",
        subtitle = "No scanned book declares one. Embed series metadata, then Deep Rescan.",
        type     = CrossbarItemType.EMPTY,
    )

    @Suppress("QueryPermissionsNeeded")
    internal val cameraAvailable: Boolean by lazy {
        runCatching {
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                .resolveActivity(context.packageManager) != null
        }.getOrDefault(false)
    }

    internal suspend fun photoAppItems(): List<CrossbarItem> {
        val apps = appCategoryRepository.appsForCategory(PHOTO_APPS_CATEGORY_ID)
            .notHiddenAt(HideLocationType.CATEGORY, PHOTO_APPS_CATEGORY_ID)
        return apps.map { it.toCrossbarItem(gameRepository.getAppEntry(it.packageName)) }
    }

    fun enterOpensAppDrawer(): Boolean = _uiState.value.enterOpensAppDrawer

    fun onTypedCharacter(ch: String): Boolean {
        if (_uiState.value.activeAppDrawerFilter != null) {
            _uiState.update { it.copy(pendingDrawerTypedChar = ch) }
            return true
        }
        if (!librarySearch.typeToSearchAllowed()) return false
        librarySearch.openSearchTyping(ch)
        return true
    }

    fun onDrawerTypedCharConsumed() {
        _uiState.update { it.copy(pendingDrawerTypedChar = null) }
    }

    internal fun selectCategoryById(categoryId: String) {
        val index = _uiState.value.categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) onCategorySelected(index)
    }

    internal fun openNowPlayingContextMenu() {
        val playback = _uiState.value.musicPlayback
        if (playback.track == null) return
        _uiState.update {
            it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = playback.track.displayTitle, rows = nowPlayingContextMenuItems(playback.isPlaying)), musicTrackId = MUSIC_PLAYER_MENU_MARKER)
            )
        }
    }

    internal fun mediaKindNouns(kind: MediaRootKind): Pair<String, String> = when (kind) {
        MediaRootKind.MUSIC -> "track" to "tracks"
        MediaRootKind.VIDEO -> "video" to "videos"
        MediaRootKind.PHOTO -> "photo" to "photos"
        MediaRootKind.BOOK  -> "book" to "books"
    }

    internal suspend fun pruneOrphanEntries(kind: MediaRootKind, roots: List<String>) {
        when (kind) {
            MediaRootKind.MUSIC -> musicRepository.getFolders()
                .filter { it.treeUri !in roots }.forEach { musicRepository.removeFolder(it.id) }
            MediaRootKind.VIDEO -> videoRepository.getLibraries()
                .filter { it.treeUri !in roots }.forEach { videoRepository.removeLibrary(it.id) }
            MediaRootKind.PHOTO -> photoRepository.getLibraries()
                .filter { it.treeUri !in roots }.forEach { photoRepository.removeLibrary(it.id) }
            MediaRootKind.BOOK -> bookRepository.getLibraries()
                .filter { it.treeUri !in roots }.forEach { bookRepository.removeLibrary(it.id) }
        }
    }

    internal fun openDefaultMediaAppMenu(kind: MediaRootKind) {
        viewModelScope.launch {
            val current: String? = when (kind) {
                MediaRootKind.MUSIC -> musicRepository.observeDefaultPlayerPackage().first()
                MediaRootKind.VIDEO -> videoRepository.observeDefaultVideoPlayer().first()
                MediaRootKind.BOOK  -> bookRepository.observeDefaultReader().first()
                MediaRootKind.PHOTO -> photoRepository.observeDefaultViewer().first()
            }

            val choices: List<Pair<String?, String>> = when (kind) {
                MediaRootKind.MUSIC -> listOf(
                    MusicIntentResolver.BUILTIN to "ECHO",
                    null to "System Default",
                ) + musicIntentResolver.availablePlayers().map { it.packageName to it.label }

                MediaRootKind.VIDEO -> listOf(
                    VIDEO_PLAYER_BUILTIN to "ECHO",
                    VIDEO_PLAYER_ASK to "System Default",
                ) + videoIntentResolver.availablePlayers().map { it.packageName to it.label }

                MediaRootKind.BOOK -> listOf(
                    null to "ECHO",
                    com.echo.core.data.book.BuiltInReader.ASK_EVERY_TIME to "Ask Every Time",
                ) + bookIntentResolver.availableReaders().map { it.packageName to it.label }

                MediaRootKind.PHOTO -> listOf(
                    null to "ECHO",
                    PHOTO_VIEWER_ASK to "Ask Every Time",
                ) + photoIntentResolver.availableViewers().map { it.packageName to it.label }
            }

            val rows = choices.map { (pkg, label) ->
                CrossbarContextMenuItem(
                    "$MEDIA_APP_PREFIX${pkg ?: MEDIA_APP_NONE}",
                    label,
                    checked = current == pkg,
                )
            }
            _uiState.update {
                it.copy(
                    activeContextMenu = CrossbarContextMenu(
                        state = MenuState(title = "Default App", rows = rows),
                        mediaRootKind = kind,
                    ),
                )
            }
        }
    }

    internal fun setDefaultMediaApp(kind: MediaRootKind, packageName: String?) {
        appAction {
            when (kind) {
                MediaRootKind.MUSIC -> musicRepository.setDefaultPlayerPackage(packageName)
                MediaRootKind.VIDEO -> videoRepository.setDefaultVideoPlayer(packageName)
                MediaRootKind.BOOK  -> bookRepository.setDefaultReader(packageName)
                MediaRootKind.PHOTO -> photoRepository.setDefaultViewer(packageName)
            }
        }
    }

    internal fun clearMediaCache(kind: MediaRootKind) {
        appAction {
            val removed = when (kind) {
                MediaRootKind.PHOTO -> photoScanner.clearThumbnailCache()
                MediaRootKind.BOOK  -> bookScanner.clearCoverCache()
                else -> return@appAction
            }
            SystemToasts.post("Cleared $removed cached file(s)", "Rescan to regenerate them.", ToastKind.SUCCESS)
        }
    }

    private fun activeSortContext(): List<CrossbarSortMode>? = _uiState.value.activeSortModes()

    fun onSortLabelTapped() {
        markTouchInput()
        cycleSort()
    }

    internal fun cycleSort() {
        stepSort(+1)
    }

    // LT/RT walk the sorts either way, wrapping
    internal fun stepSort(delta: Int) {
        val cycle = activeSortContext() ?: return
        val current = _uiState.value.sortModeFor(cycle)
        applySort(cycle, cycle[(cycle.indexOf(current).coerceAtLeast(0) + delta).mod(cycle.size)])
    }

    // a tap on the XMB's sort row picks that mode outright
    fun onSortPicked(mode: CrossbarSortMode) {
        markTouchInput()
        val cycle = activeSortContext() ?: return
        if (_uiState.value.sortModeFor(cycle) == mode) return
        applySort(cycle, mode)
    }

    private fun applySort(cycle: List<CrossbarSortMode>, next: CrossbarSortMode) {
        val isMusic = cycle === MUSIC_SORTS
        menuSound.play(MenuSound.SYSTEM_BROWSE)

        _uiState.update {
            it.withSortMode(cycle, next)
                .copy(selectedItemIndex = 0, scrollToTopToken = it.scrollToTopToken + 1)
        }

        if (isMusic) {
            val trailing = if (_uiState.value.musicNav is MusicNav.Playlist) listOf(music.addTracksItem()) else emptyList()
            val emptyItem = if (_uiState.value.musicNav is MusicNav.Playlist) music.emptyPlaylistItem() else music.emptyAllMusicItem()
            music.setMusicTrackItems(music.currentMusicTracksRaw, emptyItem, trailing)
            _uiState.update { it.copy(sortLabel = currentSortLabel()) }
            return
        }
        loadItemsForCategory(currentCategory())
    }

    private fun computeDrillTitle(): String? {
        val s = _uiState.value

        val musicTitle = when (val nav = s.musicNav) {
            MusicNav.AllMusic    -> "Songs"
            MusicNav.Playlists   -> "Playlist"
            is MusicNav.Playlist -> nav.name
            MusicNav.Artists     -> "Artists"
            MusicNav.Genres      -> "Genres"
            is MusicNav.Genre    -> nav.name
            MusicNav.Albums      -> "Albums"
            is MusicNav.Artist   -> nav.name
            is MusicNav.Album    -> nav.name
            MusicNav.Folders     -> columnSettingsTitle(currentCategory()?.name)
            MusicNav.Root        -> null
        }
        if (musicTitle != null) return musicTitle

        val videoTitle = when (val nav = s.videoNav) {
            VideoNav.AllVideos       -> "All Videos"
            VideoNav.Collections     -> "Collections"
            VideoNav.RecentlyWatched -> "Recently Watched"
            VideoNav.Favorites       -> "Favorites"
            VideoNav.Playlists       -> "Playlists"
            is VideoNav.Playlist     -> nav.name
            VideoNav.Libraries       -> "Video Libraries"
            is VideoNav.Library      -> nav.name
            VideoNav.Folders         -> columnSettingsTitle(currentCategory()?.name)
            VideoNav.Root            -> null
        }
        if (videoTitle != null) return videoTitle

        val photoTitle = when (val nav = s.photoNav) {
            PhotoNav.AllPhotos  -> "All Photos"
            PhotoNav.Albums     -> "Albums"
            PhotoNav.Favorites  -> "Favourites"
            is PhotoNav.Library -> nav.name
            PhotoNav.Folders    -> columnSettingsTitle(currentCategory()?.name)
            PhotoNav.Root       -> null
        }
        if (photoTitle != null) return photoTitle
        val booksTitle = when (val nav = s.booksNav) {
            BooksNav.AllBooks -> "Books"
            BooksNav.Shelves  -> "Shelves"
            is BooksNav.Shelf -> nav.name
            BooksNav.SeriesList -> "Series"
            is BooksNav.Series  -> nav.name
            BooksNav.Genres     -> "Genres"
            is BooksNav.Genre   -> nav.name
            BooksNav.Folders  -> columnSettingsTitle(currentCategory()?.name)
            BooksNav.Root     -> null
        }
        if (booksTitle != null) return booksTitle
        return when {
            s.selectedPlatformId == ALL_GAMES_PLATFORM_ID ->
                s.genreFilter?.takeIf { s.gameGrouping == GameGrouping.GENRE }?.label ?: "All Games"
            s.selectedPlatformId == FAVORITES_PLATFORM_ID -> "Favorites"
            s.selectedPlatformId == MISSING_PLATFORM_ID   -> "Missing"
            s.selectedPlatformId != null ->
                knownPlatformName(s.selectedPlatformId) ?: s.selectedPlatformId
            else -> null
        }
    }

    private fun computeDrillSiblings(category: Category?): Pair<List<CrossbarItem>, Int> {
        val s = _uiState.value

        if (s.musicNav != MusicNav.Root) {
            (s.musicNav as? MusicNav.Playlist)?.let { nav ->
                val pls = music.musicPlaylistSiblings()
                if (pls.isNotEmpty()) return pls to pls.indexOfFirst { it.playlistId == nav.id }.coerceAtLeast(0)
            }
            val sibs = _uiState.value.musicRootSections().filter { it.id in MUSIC_DRILL_ROOTS }
            val root = when (s.musicNav) {
                MusicNav.AllMusic -> ALL_MUSIC_ITEM_ID
                MusicNav.Artists, is MusicNav.Artist -> MUSIC_ARTISTS_ITEM_ID
                MusicNav.Albums, is MusicNav.Album -> MUSIC_ALBUMS_ITEM_ID
                MusicNav.Genres, is MusicNav.Genre -> MUSIC_GENRES_ITEM_ID
                else -> PLAYLISTS_ITEM_ID
            }
            val idx = sibs.indexOfFirst { it.id == root }.coerceAtLeast(0)
            return sibs to idx
        }

        if (s.videoNav != VideoNav.Root) {
            (s.videoNav as? VideoNav.Library)?.let { nav ->
                val libs = video.videoLibrarySiblings()
                if (libs.isNotEmpty()) return libs to libs.indexOfFirst { it.id == "vlib_${nav.id}" }.coerceAtLeast(0)
            }
            (s.videoNav as? VideoNav.Playlist)?.let { nav ->
                val pls = video.videoPlaylistSiblings()
                if (pls.isNotEmpty()) return pls to pls.indexOfFirst { it.playlistId == nav.id }.coerceAtLeast(0)
            }

            if (s.videoNav.isVideoCollectionChild || s.videoNav is VideoNav.Playlist) {
                val sibs = video.videoCollectionsItems()
                val idx = sibs.indexOfFirst { sib ->
                    when (s.videoNav) {
                        VideoNav.RecentlyWatched -> sib.type == CrossbarItemType.VIDEO_RECENT
                        VideoNav.Favorites       -> sib.type == CrossbarItemType.VIDEO_FAVORITES
                        else                     -> sib.type == CrossbarItemType.PLAYLIST
                    }
                }.coerceAtLeast(0)
                return sibs to idx
            }

            val sibs = _uiState.value.videoRootSections().filter {
                it.type == CrossbarItemType.MEMORY_CARD || it.type == CrossbarItemType.VIDEO_COLLECTIONS ||
                    it.type == CrossbarItemType.VIDEO_LIBRARY
            }
            val idx = sibs.indexOfFirst { sib ->
                when (s.videoNav) {
                    VideoNav.AllVideos   -> sib.type == CrossbarItemType.MEMORY_CARD
                    VideoNav.Collections -> sib.type == CrossbarItemType.VIDEO_COLLECTIONS
                    else                 -> sib.type == CrossbarItemType.VIDEO_LIBRARY
                }
            }.coerceAtLeast(0)
            return sibs to idx
        }

        if (s.photoNav != PhotoNav.Root) {
            (s.photoNav as? PhotoNav.Library)?.let { nav ->
                val albums = gallery.photoAlbumSiblings()
                if (albums.isNotEmpty()) return albums to albums.indexOfFirst { it.id == "plib_${nav.id}" }.coerceAtLeast(0)
            }
            val sibs = _uiState.value.photoRootSections(cameraAvailable).filter {
                it.type == CrossbarItemType.MEMORY_CARD || it.type == CrossbarItemType.PHOTO_ALBUMS || it.type == CrossbarItemType.PHOTO_FAVORITES
            }
            val idx = sibs.indexOfFirst { sib ->
                when (s.photoNav) {
                    PhotoNav.AllPhotos -> sib.type == CrossbarItemType.MEMORY_CARD
                    PhotoNav.Favorites -> sib.type == CrossbarItemType.PHOTO_FAVORITES
                    else               -> sib.type == CrossbarItemType.PHOTO_ALBUMS
                }
            }.coerceAtLeast(0)
            return sibs to idx
        }
        if (category?.id == BuiltInCategory.GAMES) {
            val sibs = gameActions.memoryCardItems().filter {
                it.type == CrossbarItemType.ALL_GAMES || it.type == CrossbarItemType.FAVORITES ||
                    it.type == CrossbarItemType.MISSING ||
                    it.type == CrossbarItemType.MEMORY_CARD
            }
            val idx = sibs.indexOfFirst { sib ->
                when {
                    // inside a genre folder its own row is lit, not All Games
                    s.selectedPlatformId == ALL_GAMES_PLATFORM_ID && s.gameGrouping == GameGrouping.GENRE && s.genreFilter != null ->
                        sib.id == genreItemId(s.genreFilter)
                    s.selectedPlatformId == ALL_GAMES_PLATFORM_ID -> sib.id == ALL_GAMES_ITEM_ID
                    s.selectedPlatformId == FAVORITES_PLATFORM_ID -> sib.type == CrossbarItemType.FAVORITES
                    s.selectedPlatformId == MISSING_PLATFORM_ID   -> sib.type == CrossbarItemType.MISSING
                    s.selectedPlatformId != null                 -> sib.platformId == s.selectedPlatformId
                    else -> false
                }
            }.coerceAtLeast(0)
            return sibs to idx
        }

        // inside a shelf, the shelves are the siblings, with their covers (owner, 2026-10-08: a blank card stood
        // in for the shelf)
        if (category?.id == BuiltInCategory.SHELVES) {
            val sibs = s.shelfRows()
            sibs.indexOfFirst { it.id == s.selectedPlatformId }.takeIf { it >= 0 }?.let { return sibs to it }
        }

        val parent = CrossbarItem(id = "drill_parent", title = computeDrillTitle().orEmpty(), type = CrossbarItemType.COLLECTION)
        return listOf(parent) to 0
    }

    private fun currentSortLabel(): String? {
        val cycle = activeSortContext() ?: return null
        return _uiState.value.sortModeFor(cycle).label
    }

    private fun emptyCategoryItem(category: Category): CrossbarItem {
        val (message, subtitle) = if (category.isGamingCategory) {
            "No games assigned." to "Add games to this category."
        } else {
            val msg = when (category.id) {
                "network"   -> "No browser apps found."
                else        -> "No apps assigned."
            }
            msg to "Install some apps to get started."
        }
        return CrossbarItem(
            id       = EMPTY_CATEGORY_ITEM_ID,
            title    = message,
            subtitle = subtitle,
            type     = CrossbarItemType.EMPTY,
        )
    }

    internal fun cardItemId(platformId: String): String = "card_" + platformId

    internal fun setupGapItem(): CrossbarItem? {
        val gap = setupState.firstGap
        if (gap == com.echo.feature.launcher.SetupGap.NONE) return null
        return CrossbarItem(
            id       = SETUP_GAP_ITEM_ID,
            title    = gap.message,
            subtitle = "Press confirm to open Settings and fix it.",
            type     = CrossbarItemType.EMPTY,
        )
    }

    private fun emptyAllGamesItem(): CrossbarItem {
        setupGapItem()?.let { return it }
        return CrossbarItem(
            id       = NO_GAMES_ITEM_ID,
            title    = "No games imported yet",
            subtitle = "Open a system to scan your library.",
            type     = CrossbarItemType.EMPTY,
        )
    }

    private fun emptyMissingItem(): CrossbarItem = CrossbarItem(
        id       = EMPTY_MISSING_ITEM_ID,
        title    = "Nothing missing",
        subtitle = "Every game's file was found on the last scan.",
        type     = CrossbarItemType.EMPTY,
    )

    // the game list as built, before the genre filter, so changing the filter needs no reload
    private var builtGameItems: List<CrossbarItem> = emptyList()

    internal fun publishGameItems(items: List<CrossbarItem>, keepCursorOnRow: Boolean) {
        builtGameItems = items
        _uiState.update {
            val shown = items.withGenre(it.genreFilter)
            if (!keepCursorOnRow) it.copy(currentItems = shown)
            else it.copy(
                currentItems = shown,
                selectedItemIndex = cursorAfterRefresh(it.currentItems, it.selectedItemIndex, shown),
            )
        }
    }

    // owner, 2026-10-08: the game lists show one genre, or all; it holds from system to system until cleared
    internal fun setGenreFilter(genre: com.echo.core.domain.model.GameGenre?) {
        menuSound.play(MenuSound.SELECT)
        _uiState.update { it.copy(genreFilter = genre) }
        publishGameItems(builtGameItems, keepCursorOnRow = false)
    }

    // owner, 2026-10-08: Game Info from the App Drawer's Options did nothing, since the drawer's game is seldom in
    // the crossbar's column; the column's row when it is there, else the game from the library
    // owner, 2026-10-08: Y on an album, video or book in the App Drawer gives the menu its own column gives it;
    // asked from the companion's drawer, it opens there, where the controller is
    fun openDrawerMediaMenu(media: com.echo.feature.appbar.DrawerMedia, title: String) {
        when (media.kind) {
            com.echo.feature.appbar.MediaKind.MUSIC -> music.openAlbumMenu(media.ref, title)
            com.echo.feature.appbar.MediaKind.VIDEO -> video.openVideoFileContextMenu(media.ref, title)
            com.echo.feature.appbar.MediaKind.BOOK -> bookshelf.openBookMenu(media.ref, title)
        }
    }

    internal fun openDrawerMediaMenuOnCompanion(media: com.echo.feature.appbar.DrawerMedia, title: String) {
        _uiState.update { it.copy(menuOnCompanion = true) }
        openDrawerMediaMenu(media, title)
    }

    // owner, 2026-10-08: an album, video or book picked in the App Drawer opens as the crossbar's own column opens
    // it: an album plays from its first track, a video plays, a book opens in the reader
    fun openDrawerMedia(media: com.echo.feature.appbar.DrawerMedia) {
        when (media.kind) {
            com.echo.feature.appbar.MediaKind.MUSIC -> music.playAlbum(media.ref)
            com.echo.feature.appbar.MediaKind.VIDEO -> _uiState.update { it.copy(activeVideoId = media.ref, activeVideoAutoPlay = true) }
            com.echo.feature.appbar.MediaKind.BOOK -> bookshelf.openBook(media.ref)
        }
    }

    internal fun openGameInfoFor(gameId: Long) {
        viewModelScope.launch {
            val item = gameRowFor(gameId, _uiState.value.currentItems) {
                gameRepository.getById(gameId)?.let { listOf(it).toCrossbarItems().first() }
            } ?: return@launch
            gameDetail.onOpenGameInfo(item)
        }
    }

    // the game's genre from the library, so the drawer's Options, whose game is not in the column, filter too
    internal fun filterByGenreOf(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            com.echo.core.domain.model.effectiveGenre(game.genre, game.genreOverride)?.let(::setGenreFilter)
        }
    }

    internal fun openGenrePickerMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val current = com.echo.core.domain.model.GameGenre.fromName(game.genreOverride)
            val items = buildList {
                add(CrossbarContextMenuItem("genre_pick_scraped", game.genre?.takeIf { it.isNotBlank() }?.let { "As scraped · $it" } ?: "As scraped · none", checked = current == null))
                com.echo.core.domain.model.GameGenre.entries.forEach { g ->
                    add(CrossbarContextMenuItem("genre_pick_${g.name}", g.label, checked = current == g))
                }
            }
            _uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Genre", rows = items), gameId = gameId)) }
        }
    }

    internal fun List<com.echo.core.domain.model.Game>.toCrossbarItems() = map { g ->
        CrossbarItem(
            id           = g.id.toString(),
            title        = g.displayTitle,
            artworkUri   = g.artworkUri,
            iconUri      = g.iconUri,
            logoUri      = g.logoUri,
            subtitle     = gameMetaLabel(g),
            metadataLine = gameMetadataLine(g.releaseYear, com.echo.core.domain.model.GameGenre.fromName(g.genreOverride)?.label ?: g.genre, g.developer, g.players),
            genre        = com.echo.core.domain.model.effectiveGenre(g.genre, g.genreOverride),
            description  = g.description,
            romPath      = g.romPath,
            totalPlayTimeMillis = g.totalPlayTimeMillis,
            gameId       = g.id,
            platformId   = g.platformId,
            accentColor  = platformCache[g.platformId]?.accentColor,
            isFavorite   = g.isFavorite,
            playState    = g.playState,
            isAndroidApp = g.packageName != null,
            isRealGame   = g.contentType == GameContentType.GAME,
            packageName  = g.packageName,
            shortcutId   = g.shortcutId,
            launchIntentUri = g.launchIntentUri,
        )
    }

    private fun tintWaveForCategory(category: Category?) {
        _uiState.update { it.copy(themeColors = look.baseThemeColors) }
    }

    private fun observeGamepadMappings() {
        viewModelScope.launch {
            mappingRepository.mappings.collect { mappings ->
                gamepadInputHandler.currentMappings = mappings
            }
        }
        viewModelScope.launch {
            controllerLayoutRepository.prefs.collect { prefs ->

                gamepadInputHandler.scrollSpeed = prefs.scrollSpeed
                gamepadInputHandler.stickSensitivity = prefs.stickSensitivity
                gamepadInputHandler.triggerSensitivity = prefs.triggerSensitivity
                gamepadInputHandler.shoulderHoldMs = prefs.shoulderHoldTime.millis
                _uiState.update { it.copy(leftBacksOut = prefs.leftBacksOut) }
            }
        }
    }

    // a finger on the footer's Resume card
    fun resumeFocusedGame() {
        markTouchInput()
        _uiState.value.resumableFocus()?.gameId?.let(launching::resumeGame)
    }

    fun onPromptTapped(action: GamepadAction) {
        markTouchInput()
        dispatchGamepadAction(action)
    }

    fun onClaimedKey(action: GamepadAction) {
        markControllerInput()
        onUserInteraction()
        dispatchGamepadAction(action)
    }

    private fun collectGamepadActions() {
        viewModelScope.launch {
            gamepadInputHandler.actions.collect { action ->
                markControllerInput()
                onUserInteraction()
                if (action != holdButton) launchHold.release()
                heldFromPad = action.takeIf { it == GamepadAction.SELECT || it == GamepadAction.OPEN_CONTEXT_MENU }
                try {
                    dispatchGamepadAction(action)
                } finally {
                    heldFromPad = null
                }
            }
        }
        viewModelScope.launch {
            gamepadInputHandler.holdReleases.collect { released ->
                if (released == holdButton && launchHold.release() && released == GamepadAction.OPEN_CONTEXT_MENU) openContextMenuForFocusedItem()
                if (released == GamepadAction.SELECT && _uiState.value.activeAppDrawerFilter != null) {
                    _uiState.update { it.copy(drawerSelectReleases = it.drawerSelectReleases + 1) }
                }
            }
        }
        viewModelScope.launch {
            gamepadInputHandler.shoulderHolds.collect { hold ->
                markControllerInput()
                onUserInteraction()
                if (_uiState.value.activeAppDrawerFilter != null) {
                    _uiState.update { it.copy(drawerLetterRailHeld = hold is ShoulderHold.Start) }
                    return@collect
                }
                when (hold) {
                    is ShoulderHold.Start -> openLetterJump()

                    is ShoulderHold.End ->
                        if (_uiState.value.letterJump != null) closeLetterJump()
                        else dispatchGamepadAction(hold.action)
                }
            }
        }
    }

    private fun openLetterJump() {
        val s = _uiState.value
        if (s.hasBlockingOverlay || s.letterJump != null) return
        val rail = letterJumpFor(s.currentItems, s.selectedItemIndex) ?: return
        _uiState.update { it.copy(letterJump = rail, selectedItemIndex = rail.targetIndex) }
    }

    private fun closeLetterJump() = _uiState.update { it.copy(letterJump = null) }

    private fun moveLetterJump(delta: Int) {
        val rail = _uiState.value.letterJump ?: return
        val next = rail.move(delta)
        if (next === rail) return
        menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(letterJump = next, selectedItemIndex = next.targetIndex) }
    }

    fun onLetterRailTouch(rung: Int) {
        onUserInteraction()
        val s = _uiState.value

        if (s.hasBlockingOverlay) return
        val rail = s.letterJump
            ?: letterJumpFor(s.currentItems, s.selectedItemIndex)?.also { raised ->
                _uiState.update { it.copy(letterJump = raised) }
            }
            ?: return
        val next = rail.at(rung)
        if (next === rail) return
        menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(letterJump = next, selectedItemIndex = next.targetIndex) }
    }

    fun onLetterRailReleased() = closeLetterJump()

    private fun observeContextMenuHintIdle() {
        viewModelScope.launch {
            while (isActive) {
                delay(IDLE_HINT_POLL_MS)
                val s = _uiState.value
                val idleMs = SystemClock.elapsedRealtime() - lastInteractionMs

                val waveIdle = idleMs >= WAVE_IDLE_MS
                if (waveIdle != s.idle) _uiState.update { it.copy(idle = waveIdle) }
                val shouldShow = com.echo.feature.crossbar.viewmodel.shouldShowContextMenuHint(
                    state = s,
                    idleMs = idleMs,
                )

                val shouldShowSettings = com.echo.feature.crossbar.viewmodel.shouldShowSettingsHint(
                    state = s,
                    idleMs = idleMs,
                )
                if (shouldShow != s.showContextMenuHint ||
                    shouldShowSettings != s.showSettingsHint
                ) {
                    _uiState.update {
                        it.copy(
                            showContextMenuHint = shouldShow,
                            showSettingsHint = shouldShowSettings,
                        )
                    }
                }
            }
        }
    }

    private fun dispatchGamepadAction(action: GamepadAction) {
        val state = _uiState.value

        // L3 or R3 (owner, 2026-10-08): the same as the Swap button, from either screen, over anything open
        if (action == GamepadAction.SWAP_SCREENS) {
            if (state.secondDisplayPresent && state.secondScreenEnabled) bottomScreen.toggleSwap()
            return
        }

        if (state.letterJump != null) {
            when (action) {
                GamepadAction.NAVIGATE_UP -> moveLetterJump(-1)
                GamepadAction.NAVIGATE_DOWN -> moveLetterJump(+1)
                GamepadAction.BACK -> _uiState.update {
                    it.copy(letterJump = null, selectedItemIndex = state.letterJump.returnIndex)
                }
                else -> Unit
            }
            return
        }

        if (state.appPicker != null) {
            appPickerSection.onButton(action, state)
            return
        }

        if (state.musicTrackPicker != null) {
            music.onTrackPickerButton(action, state)
            return
        }

        if (state.gamePickerCategoryId != null) {
            when (action) {
                GamepadAction.NAVIGATE_UP,
                GamepadAction.NAVIGATE_DOWN,
                GamepadAction.SELECT,
                GamepadAction.HOME,
                GamepadAction.OPEN_ISLAND,
                GamepadAction.BACK,
                GamepadAction.OPEN_CONTEXT_MENU -> _uiState.update { it.copy(pendingGamePickerAction = action) }
                else -> Unit
            }
            return
        }

        if (state.notificationsOpen) {
            panel.onButton(action, state)
            return
        }

        if (state.activeContextMenu != null) {
            when (action) {
                GamepadAction.NAVIGATE_UP   -> shiftContextMenu(-1)
                GamepadAction.NAVIGATE_DOWN -> shiftContextMenu(+1)

                GamepadAction.SELECT        -> {
                    val menu = state.activeContextMenu
                    val picked = menu.selectedIndex?.let { state.menuRows().getOrNull(it) }
                    when {
                        picked != null -> onContextMenuItemActivatedAt(menu.selectedIndex!!)
                        menu.primaryId != null -> activateContextMenuItem(menu.primaryId)
                        else -> Unit
                    }
                }
                GamepadAction.BACK                   -> popContextMenu()
                GamepadAction.OPEN_CONTEXT_MENU      -> closeContextMenu()
                else -> Unit
            }
            return
        }

        if (state.moving != null) {
            move.onButton(action, state.moving)
            return
        }

        if (state.musicPlayerVisible) {
            music.onPlayerButton(action, state)
            return
        }

        if (state.customColorPicker != null) {
            look.onCustomColorButton(action, state)
            return
        }
        if (state.colorSchemePicker != null) {
            look.onSchemePickerButton(action, state)
            return
        }

        if (state.renameAppTarget != null) {
            when (action) {
                GamepadAction.SELECT -> onConfirmAppRename(state.renameAppText)
                GamepadAction.BACK   -> onCancelAppRename()
                else                 -> Unit
            }
            return
        }
        if (state.collectionNameDialog != null) {
            when (action) {
                GamepadAction.SELECT -> gameActions.onConfirmCollectionName(state.collectionNameDialog.text)
                GamepadAction.BACK   -> gameActions.onCancelCollectionName()
                else                 -> Unit
            }
            return
        }
        if (state.playlistNameDialog != null) {
            music.onPlaylistNameButton(action, state)
            return
        }

        if (state.infoDialog != null) {
            if (action == GamepadAction.BACK || action == GamepadAction.SELECT) dismissInfoDialog()
            return
        }

        state.launchRecovery?.let { recovery ->
            val actions = com.echo.feature.launcher.launchRecoveryActions(recovery)
            when (action) {
                GamepadAction.NAVIGATE_UP -> _uiState.update {
                    it.copy(launchRecoveryCursor = (it.launchRecoveryCursor - 1 + actions.size) % actions.size)
                }
                GamepadAction.NAVIGATE_DOWN -> _uiState.update {
                    it.copy(launchRecoveryCursor = (it.launchRecoveryCursor + 1) % actions.size)
                }
                GamepadAction.SELECT -> actions
                    .getOrNull(state.launchRecoveryCursor.coerceIn(0, actions.lastIndex))
                    ?.let { (a, _) -> launching.onLaunchRecoveryAction(a) }
                GamepadAction.BACK -> launching.onLaunchRecoveryAction(LaunchRecoveryAction.DISMISS)
                else -> Unit
            }
            return
        }

        if (state.showWindowsSetupPrompt) {
            when (action) {
                GamepadAction.SELECT -> confirmWindowsSetupPrompt()
                GamepadAction.BACK   -> dismissWindowsSetupPrompt()
                else                 -> Unit
            }
            return
        }

        if (globalButton(action, state)) return

        if (state.noticeCardPinned && action != GamepadAction.OPEN_NOTIFICATIONS && panel.onNoticeCardButton(action, state)) return

        if (state.search != null) {
            librarySearch.onButton(action, state)
            return
        }

        if (state.showBootSequence) {
            launching.onBootButton(action, state)
            return
        }

        if (state.activeGameBoot != null) {
            launching.onGameBootButton(action, state)
            return
        }

        when {
            state.activePhotoViewer != null -> {
                _uiState.update { it.copy(pendingPhotoViewerAction = action) }
                return
            }
            state.activeVideoId != null -> {
                _uiState.update { it.copy(pendingVideoDetailAction = action) }
                return
            }
            state.metadataPreview != null -> {
                artworkTools.handleMetadataPreviewInput(action)
                return
            }
            state.manualViewer != null -> {
                gameDetail.handleManualViewerInput(action)
                return
            }
            state.artworkStudioGameId != null -> {
                _uiState.update { it.copy(pendingArtworkStudioAction = action) }
                return
            }
            state.activeAppDrawerFilter != null -> {
                _uiState.update { it.copy(pendingDrawerAction = action) }
                return
            }
            state.activeSettingsScreen != null -> {
                Timber.d("Gamepad → settings(${state.activeSettingsScreen}): $action")

                when (action) {
                    GamepadAction.BACK,
                    GamepadAction.NAVIGATE_UP,
                    GamepadAction.NAVIGATE_DOWN,

                    GamepadAction.NAVIGATE_LEFT,
                    GamepadAction.NAVIGATE_RIGHT,
                    GamepadAction.OPEN_CONTEXT_MENU,
                    GamepadAction.CHANGE_SORT,

                    GamepadAction.PREV_CATEGORY,
                    GamepadAction.NEXT_CATEGORY,
                    GamepadAction.SELECT -> _uiState.update { it.copy(pendingSettingsAction = action) }
                    else -> Unit
                }
                return
            }
            state.saveThemeNameDialog != null -> {
                when (action) {
                    GamepadAction.SELECT -> look.confirmSaveCurrentLookAsTheme(state.saveThemeNameDialog.text)
                    GamepadAction.BACK   -> look.dismissSaveThemeNameDialog()
                    else                 -> Unit
                }
                return
            }
            state.customIconSession != null -> {
                when (action) {
                    GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_UP -> onCustomIconSlotMove(-1)
                    GamepadAction.NAVIGATE_RIGHT, GamepadAction.NAVIGATE_DOWN -> onCustomIconSlotMove(+1)
                    GamepadAction.PREV_CATEGORY -> onCustomIconGroupMove(-1)
                    GamepadAction.NEXT_CATEGORY -> onCustomIconGroupMove(+1)
                    GamepadAction.SELECT,
                    GamepadAction.OPEN_CONTEXT_MENU,
                    GamepadAction.CHANGE_SORT,
                    GamepadAction.BACK -> _uiState.update { it.copy(pendingCustomIconsAction = action) }
                    else -> Unit
                }
                return
            }
            state.profile != null -> {
                panel.handleProfileInput(state.profile, action)
                return
            }
            state.gameInfo != null -> {
                gameDetail.handleGameInfoInput(state.gameInfo, state.androidNotices, action)
                return
            }
        }

        if (state.hasBlockingOverlay) {
            Timber.w("Gamepad action $action dropped: a blocking overlay has no branch in this dispatcher")
            return
        }

        // owner, 2026-10-06: with a second screen the controller drives the screen last touched; Home
        // and anything the companion does not use still reach the XMB
        if (state.secondScreen && state.companionActive && action != GamepadAction.HOME && bottomScreen.onButton(action)) return

        if (orbPressHandled(action, state)) return

        when (action) {
            GamepadAction.NAVIGATE_UP   -> {
                if (!moveItemCursor(-1)) {
                    gamepadInputHandler.cancelRepeat()
                    // up from the top of a list focuses the orb (kit 05)
                    if (state.orbKind() != null) {
                        menuSound.play(MenuSound.SCROLL)
                        _uiState.update { it.copy(orbLevel = 1) }
                    }
                }
            }
            // owner, 2026-10-06: no pill row under a row any more; its actions are in the row's menu
            GamepadAction.NAVIGATE_DOWN ->
                if (!moveItemCursor(+1)) gamepadInputHandler.cancelRepeat()
            GamepadAction.NAVIGATE_LEFT -> {
                if (state.isInSubItem) {
                    gamepadInputHandler.cancelRepeat()
                    if (!state.leftBacksOut) return
                    menuSound.play(MenuSound.BACK)
                    backOutOfDrill(state)
                    return
                }

                if (recentRailStep(action, state.onLastPlayedHome, state.recentRailVisible) == RailStep.Open) {
                    gamepadInputHandler.cancelRepeat()
                    menuSound.play(MenuSound.SYSTEM_BROWSE)
                    _uiState.update { it.copy(recentRailVisible = true) }
                    return
                }
                val next = state.stepToReachableCategory(-1)
                if (next != state.selectedCategoryIndex) onCategorySelected(next)
                else gamepadInputHandler.cancelRepeat()
            }
            GamepadAction.NAVIGATE_RIGHT -> {
                if (recentRailStep(action, state.onLastPlayedHome, state.recentRailVisible) == RailStep.Close) {
                    gamepadInputHandler.cancelRepeat()
                    menuSound.play(MenuSound.SYSTEM_BROWSE)
                    _uiState.update { it.copy(recentRailVisible = false) }
                    return
                }

                if (state.isInSubItem) { gamepadInputHandler.cancelRepeat(); return }
                val next = state.stepToReachableCategory(+1)
                if (next != state.selectedCategoryIndex) onCategorySelected(next)
                else gamepadInputHandler.cancelRepeat()
            }
            GamepadAction.SELECT     -> {
                val index = state.selectedItemIndex
                val item = state.currentItems.getOrNull(index)
                val held = item != null && !state.hasBlockingOverlay && holdToLaunch(item) { onItemSelected(index) }
                if (!held) onItemSelected(index)
            }
            GamepadAction.BACK       -> {
                menuSound.play(MenuSound.BACK)

                if (recentRailStep(action, state.onLastPlayedHome, state.recentRailVisible) == RailStep.Close) {
                    _uiState.update { it.copy(recentRailVisible = false) }
                    return
                }

                // owner, 2026-10-06: B is only ever Back; at the root there is nothing to go back to
                backOutOfDrill(state)
            }

            // on the game that is running, Y held resumes it; a tap of Y opens its menu when let go
            GamepadAction.OPEN_CONTEXT_MENU -> {
                val running = state.resumableFocus()
                if (running == null || !holdToResume(running) { running.gameId?.let(launching::resumeGame) }) openContextMenuForFocusedItem()
            }

            // LB, RB, Select and Home are taken by globalButton before the crossbar sees them
            GamepadAction.PREV_PAGE, GamepadAction.NEXT_PAGE, GamepadAction.HOME, GamepadAction.OPEN_NOTIFICATIONS -> Unit
            GamepadAction.OPEN_ISLAND   -> focusOrb(state)

            // owner, 2026-10-07: X opens the focused game's or app's details (Game Info); LT/RT sort
            GamepadAction.CHANGE_SORT -> when {
                state.onLastPlayedHome && state.recentRailVisible -> state.focusedItem?.let(recents::removeFromRecent)
                state.onLastPlayedHome -> state.focusedItem?.takeIf { recentKind(it) == RecentKind.GAME || recentKind(it) == RecentKind.APP }
                    ?.let(gameDetail::onOpenGameInfo)
                else -> state.focusedItem?.takeIf(::hasInfo)?.let(gameDetail::onOpenGameInfo)
            }

            GamepadAction.OPEN_SEARCH -> librarySearch.openSearch(SearchScope.ALL)

            GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY -> {
                val step = if (action == GamepadAction.NEXT_CATEGORY) +1 else -1
                when {
                    state.onLastPlayedHome -> recents.stepRecentFilter(step)
                    activeSortContext() != null -> stepSort(step)
                }
            }
        }
    }

    internal fun loadAppInfo(info: GameInfoState): GameInfoState {
        val pkg = info.item.packageName ?: return info
        val pm = context.packageManager
        val version = runCatching { pm.getPackageInfo(pkg, 0).versionName }.getOrNull()
        val storage = if (com.echo.core.data.permission.UsageAccess.isGranted(context)) runCatching {
            val stats = context.getSystemService(android.app.usage.StorageStatsManager::class.java)
                .queryStatsForPackage(pm.getApplicationInfo(pkg, 0).storageUuid, pkg, android.os.Process.myUserHandle())
            stats.appBytes + stats.dataBytes
        }.getOrNull() else null
        return info.copy(appVersion = version, appStorageBytes = storage)
    }

    private fun openPlatformContextMenu(platformId: String) {
        val card = enabledCards.firstOrNull { it.platformId == platformId } ?: return
        viewModelScope.launch {
            val choices = platformChoices(platformId)
            val overrideCount = if (platformId in NON_EMULATOR_PLATFORM_IDS) 0
                else gameRepository.getByPlatform(platformId).count { !it.emulatorPackage.isNullOrBlank() }

            val items = platformContextMenuItems(
                platformId = platformId,
                pinned = card.pinned,
                emulatorLabel = choices?.let { it.resolvedName ?: "None" },
                overrideCount = overrideCount,
                romDirectory = card.romDirectory,
            )

            _uiState.update { it.copy(
                activeContextMenu = CrossbarContextMenu(state = MenuState(title = card.displayName, rows = items), platformId = platformId)
            )}
        }
    }

    private suspend fun platformChoices(platformId: String): PlatformEmulatorChoices? {
        if (platformId in NON_EMULATOR_PLATFORM_IDS) return null
        val card = enabledCards.firstOrNull { it.platformId == platformId }
        return platformEmulatorChoices(
            platformId = platformId,
            installedProfiles = emulatorProfileRepository.getInstalledProfiles(),
            rememberedCoreId = autoCoreMemory.rememberedIds()[platformId],
            memoryCardEmulatorId = card?.emulatorId,
            platformDefaultPackage = platformDao.getById(platformId)?.preferredEmulatorPackage,
        )
    }

    internal fun openDefaultEmulatorMenu(platformId: String) {
        viewModelScope.launch {
            val choices = platformChoices(platformId) ?: return@launch
            val rows = buildList {
                val recommended = choices.choices.firstOrNull { it.isRecommended }?.name
                add(
                    CrossbarContextMenuItem(
                        "emu_automatic",
                        if (recommended != null) "Automatic  ·  $recommended" else "Automatic",
                        checked = choices.isAutomatic,
                    ),
                )
                choices.choices.forEach { choice ->
                    add(
                        CrossbarContextMenuItem(
                            "$PLATFORM_EMU_PREFIX${choice.profileId}",
                            choice.name,
                            checked = choice.isCurrent,
                        ),
                    )
                }
            }
            _uiState.update { it.copy(
                activeContextMenu = CrossbarContextMenu(
                    state = MenuState(title = "Default Emulator", rows = rows),
                    platformId = platformId,
                ),
            )}
        }
    }

    private fun openAllGamesContextMenu() {
        _uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = "All Games", rows = allGamesContextMenuItems(_uiState.value.gameGrouping)), isAllGames = true)
        )}
    }

    internal fun openAppContextMenu(item: CrossbarItem, categoryIdOverride: String? = null) {
        val pkg = item.packageName ?: return
        val categoryId = categoryIdOverride ?: currentCategory()?.id

        val items = appContextMenuItems(_uiState.value, categoryId, onRecentShelf = item.id.startsWith(RECENT_APP_ID_PREFIX), packageName = pkg)
        _uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = items), gameId = item.gameId, packageName = pkg, categoryContext = categoryId)
        )}
    }

    private fun openCategoryPicker(pkg: String, fromCategory: String?, action: String) {
        val items = _uiState.value.categories.map { cat ->
            CrossbarContextMenuItem("pick_${cat.id}", cat.name)
        }
        _uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = if (action == "move") "Move To…" else "Add To…", rows = items), packageName = pkg, categoryContext = fromCategory, pendingAppAction = action)
        )}
    }

    private fun shiftContextMenu(delta: Int) {
        val state = _uiState.value
        val menu = state.activeContextMenu ?: return

        val rows = state.menuRows()
        if (rows.isEmpty()) return

        val current = menu.selectedIndex ?: return run {
            val entry = if (delta > 0) 0 else rows.lastIndex
            _uiState.update { it.copy(activeContextMenu = menu.withSelected(entry)) }
        }
        val next = (current + delta).coerceIn(0, rows.size - 1)
        _uiState.update { it.copy(activeContextMenu = menu.withSelected(next)) }
    }

    private fun activateContextMenuItem(itemId: String) {
        val state  = _uiState.value
        val menu   = state.activeContextMenu ?: return

        if (itemId.startsWith("cat_") && menu.gameId != null && menu.categoryContext != null && menu.pendingAppAction != null) {
            val gameId = menu.gameId
            val fromCategory = menu.categoryContext
            val toCategory = itemId.removePrefix("cat_")
            val action = menu.pendingAppAction
            closeContextMenu()

            appAction {
                when (action) {
                    "move" -> gameCategoryRepository.moveGameToCategory(gameId, fromCategory, toCategory)
                    "add"  -> gameCategoryRepository.addGameToCategory(gameId, toCategory)
                }
                if (currentCategory()?.id == fromCategory) {
                    loadItemsForCategory(currentCategory())
                }
            }
            return
        }

        menu.genreTarget?.let { return genres.onPick(it, itemId) }

        if (itemId == MOVE_ROW || itemId == MOVE_COLUMN) {
            closeContextMenu()
            move.lift(column = itemId == MOVE_COLUMN)
            return
        }
        if (itemId == RENAME_COLUMN || itemId == CHANGE_COLUMN_ICON || itemId == CHANGE_SYSTEM_ICON || itemId == CHANGE_SYSTEM_ART) {
            val focused = state.currentItems.getOrNull(state.selectedItemIndex)
            closeContextMenu()
            when (itemId) {
                RENAME_COLUMN -> move.promptRenameColumn()
                CHANGE_COLUMN_ICON -> openCustomIcons(state.columnIconSlot())
                CHANGE_SYSTEM_ART -> openCustomIcons(systemArtSlotFor(focused))
                else -> openCustomIcons(focused?.let(::systemIconSlot))
            }
            return
        }

        if (menu.isAddMenu) {
            val row = currentAddActions().firstOrNull { it.id == itemId }
            closeContextMenu()
            if (row != null) dispatchCategorySelection(row)
            return
        }

        if (menu.videoPlaylistPickerVideoId != null) {
            video.onPlaylistPickerItem(itemId, menu)
            return
        }

        if (menu.playlistPickerTrackId != null) {
            music.onPlaylistPickerItem(itemId, menu)
            return
        }

        closeContextMenu()

        menu.albumKey?.let { key ->
            when (itemId) {
                "play_album" -> music.playAlbum(key)
                "edit_genre" -> music.openAlbumGenrePicker(key)
            }
            return
        }
        if (menu.recentAlbum != null) {
            when (itemId) {
                "open_album" -> _uiState.value.currentItems.indexOfFirst { it.id == menu.recentAlbum.id }.takeIf { it >= 0 }?.let(::onItemSelected)
                "remove_from_recent" -> recents.removeFromRecent(menu.recentAlbum)
            }
            return
        }
        if (menu.videoFileId != null) {
            video.handleVideoFileAction(menu.videoFileId, itemId)
            return
        }
        if (menu.bookFileId != null) {
            bookshelf.handleBookAction(menu.bookFileId, itemId)
            return
        }
        if (menu.videoLibraryId != null) {
            video.handleVideoLibraryAction(menu.videoLibraryId, itemId)
            return
        }
        if (menu.photoFileId != null) {
            gallery.handlePhotoFileAction(menu.photoFileId, itemId)
            return
        }
        if (menu.photoLibraryId != null) {
            gallery.handlePhotoLibraryAction(menu.photoLibraryId, itemId)
            return
        }
        if (menu.videoPlaylistId != null) {
            video.handleVideoPlaylistRowAction(menu.videoPlaylistId, itemId)
            return
        }

        if (menu.playlistId != null && menu.musicTrackId == null) {
            music.handlePlaylistRowAction(menu.playlistId, itemId)
            return
        }

        when {
            menu.musicTrackId == MUSIC_PLAYER_MENU_MARKER -> when (itemId) {
                "music_background" -> music.musicPlayInBackground()
                "music_playpause"  -> music.musicPlayPause()
                "music_close"      -> music.stopAndCloseMusicPlayer()
                "music_shuffle"    -> music.musicToggleShuffle()
                "music_repeat"     -> music.musicCycleRepeat()
            }
            menu.musicTrackId != null -> music.handleMusicTrackAction(menu.musicTrackId, itemId, menu.playlistId)
            menu.mediaRootKind != null && menu.mediaRootUri != null ->
                folders.handleMediaRootAction(menu.mediaRootKind, menu.mediaRootUri, itemId)
            menu.mediaRootKind == null && menu.mediaRootUri != null ->
                folders.handleRomRootAction(menu.mediaRootUri, itemId)
            menu.mediaRootKind != null -> folders.handleMediaFoldersAction(menu.mediaRootKind, itemId)
            menu.musicFolderId != null -> music.handleMusicFolderAction(menu.musicFolderId, itemId)
            menu.isAllGames -> when (itemId) {
                "library_manager" -> _uiState.update { it.withSettingsOpen("settings_library") }
                "import_pc_games" -> _uiState.update { it.withSettingsOpen("settings_import_pc") }
                "group_by_genre"  -> setGameGrouping(GameGrouping.GENRE)
                "group_by_system" -> setGameGrouping(GameGrouping.SYSTEM)
            }
            menu.platformId != null -> if (itemId.startsWith(PLATFORM_EMU_PREFIX)) {
                launching.setPlatformEmulator(menu.platformId, itemId.removePrefix(PLATFORM_EMU_PREFIX))
            } else when (itemId) {
                "default_emulator" -> openDefaultEmulatorMenu(menu.platformId)
                "emu_automatic"    -> launching.setPlatformEmulator(menu.platformId, null)
                "clear_emulator_overrides" -> launching.clearPlatformEmulatorOverrides(menu.platformId)
                "rename_card"      -> gameActions.promptRenameCard(menu.platformId)
                "card_rom_directory" -> folders.openRomFolders()
                "find_games"       -> appPickerSection.openAppPicker(AppPickerTarget.AndroidGames(menu.platformId), "Find Games")
                "import_pc_games"  -> _uiState.update { it.withSettingsOpen("settings_import_pc") }
                "scan_roms"        -> folders.scanCard(menu.platformId)
                "scrape_missing_artwork" -> artworkTools.scrapeMissingArtworkForPlatform(menu.platformId)
                "update_metadata"        -> artworkTools.updatePlatformMetadata(menu.platformId)
                "pin"              -> gameActions.setCardPinned(menu.platformId, true)
                "unpin"            -> gameActions.setCardPinned(menu.platformId, false)
                "library_manager"  -> _uiState.update { it.withSettingsOpen("settings_library") }
                "hide"             -> gameActions.hideCard(menu.platformId)
                "remove"           -> gameActions.removeCard(menu.platformId)
            }
            menu.gameId != null -> gameActions.onGameMenuItem(itemId, menu)
            menu.packageName != null -> {
                val pkg = menu.packageName
                if (itemId.startsWith("pick_")) {
                    val targetCategory = itemId.removePrefix("pick_")
                    when (menu.pendingAppAction) {
                        "move" -> appAction { appCategoryRepository.moveToCategory(pkg, targetCategory) }
                        "add"  -> appAction { appCategoryRepository.addToCategory(pkg, targetCategory) }
                    }
                } else when (itemId) {
                    "launch"    -> launching.launchAppWithDisc(pkg, selectedItemArt())
                    "pin_recent", "unpin_recent" -> { closeContextMenu(); recents.togglePinned("a:$pkg") }
                    "app_artwork" -> viewModelScope.launch { artworkTools.openArtworkStudio(ensureAppShortcut(pkg)) }
                    "app_info"  -> { closeContextMenu(); com.echo.core.data.apps.AppSystemActions.openAppInfo(context, pkg) }
                    "uninstall" -> { closeContextMenu(); com.echo.core.data.apps.AppSystemActions.uninstall(context, pkg) }

                    "mark_game" -> appAction {
                        val existing = gameRepository.getAppEntry(pkg)
                        if (existing == null) {
                            gameRepository.upsert(Game(
                                title         = menu.title,
                                platformId    = ANDROID_PLATFORM_ID,
                                packageName   = pkg,
                                isManualEntry = true,
                                contentType   = GameContentType.GAME,
                            ))
                        } else {
                            gameRepository.upsert(existing.copy(
                                platformId  = ANDROID_PLATFORM_ID,
                                contentType = GameContentType.GAME,
                            ))
                        }
                        memoryCardRepository.recountGames(ANDROID_PLATFORM_ID)
                    }
                    "favorite"          -> gameActions.addAppToFavorites(pkg, menu.title)
                    "move"      -> openCategoryPicker(pkg, menu.categoryContext, "move")
                    "add"       -> openCategoryPicker(pkg, menu.categoryContext, "add")
                    "remove"    -> menu.categoryContext?.let { cat -> appAction { appCategoryRepository.removeFromCategory(pkg, cat) } }
                    "pin"       -> menu.categoryContext?.let { cat -> appAction { appCategoryRepository.pinToCategory(pkg, cat) } }
                    "hide_from_category" -> menu.categoryContext?.let { cat ->
                        persistHide(HiddenPlacement.appKey(pkg), menu.title, HideLocationType.CATEGORY, cat, categoryDisplayName(cat))
                    }

                    "remove_from_recent" -> recents.dismissAppFromRecents(pkg)
                    "hide_from_recent" -> persistHide(HiddenPlacement.appKey(pkg), menu.title, HideLocationType.RECENTS, "", "Recent")
                    "hide_everywhere" -> appAction { appCategoryRepository.setHidden(pkg, true) }
                    "rename"    -> _uiState.update {
                        it.copy(renameAppTarget = pkg, renameAppCurrent = menu.title, renameAppText = menu.title)
                    }
                }
            }
        }
    }

    internal fun appAction(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    internal fun openShelvesPickerMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val current = PlayState.fromName(game.playState)
            val items = buildList {
                add(CrossbarContextMenuItem("shelf_favorite", "Favorites", checked = game.isFavorite))
                PlayState.entries.forEach { state ->
                    add(CrossbarContextMenuItem("pstate_${state.name}", state.label, checked = current == state))
                }
                add(CrossbarContextMenuItem("pstate_none", "Unmarked", checked = current == null))
            }
            _uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Shelves", rows = items), gameId = gameId))}
        }
    }

    internal fun openEmulatorPickerMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val profiles = emulatorProfileRepository.getProfilesForPlatform(game.platformId)
            val items = buildList {
                add(CrossbarContextMenuItem("emu_pick_default", "Use Platform Default"))
                profiles.forEach { add(CrossbarContextMenuItem("emu_pick_${it.id}", it.name)) }
            }
            _uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Choose Emulator", rows = items), gameId = gameId))}
        }
    }

    internal fun openDiscPickerMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val key = game.discSetKey ?: return@launch
            val members = gameRepository.getDiscSetMembers(key)
            if (members.size <= 1) return@launch
            val preferredDiscId = members.firstOrNull { it.isDiscPrimary }?.id
            val items = members
                .sortedWith(compareBy<Game> { it.discNumber == null }.thenBy { it.discNumber ?: Int.MAX_VALUE }.thenBy { it.id })
                .map { member ->
                    CrossbarContextMenuItem(
                        action = "disc_pick_${member.id}",
                        label   = member.discNumber?.let { "Disc $it" } ?: "Playlist",
                        checked = member.id == preferredDiscId,
                    )
                }
            _uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Choose Disc", rows = items), gameId = gameId))}
        }
    }

    internal suspend fun removeGameFromLibrary(gameId: Long) {
        val game = gameRepository.getById(gameId) ?: return
        gameRepository.delete(gameId)
        memoryCardRepository.recountGames(game.platformId)
        loadItemsForCategory(currentCategory())
    }

    fun onConfirmAppRename(newLabel: String) {
        val pkg = _uiState.value.renameAppTarget ?: return
        viewModelScope.launch {
            appCategoryRepository.rename(pkg, newLabel.ifBlank { null })
            _uiState.update { it.copy(renameAppTarget = null, renameAppCurrent = null) }
        }
    }

    fun onNamePromptTextChanged(text: String) {
        _uiState.update { it.withNamePromptText(text) }
    }

    fun onCancelAppRename() {
        _uiState.update { it.copy(renameAppTarget = null, renameAppCurrent = null) }
    }

    internal fun runQuickSearch(text: String) {
        val intent = when (val action = quickSearchActionFor(text)) {
            is QuickSearchAction.None -> return
            is QuickSearchAction.Open ->
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(action.url))
            is QuickSearchAction.Search ->
                android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH)
                    .putExtra(android.app.SearchManager.QUERY, action.query)
        }.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        menuSound.play(MenuSound.LAUNCH)
        try {
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Timber.w(e, "No app can handle Quick Search")
            _uiState.update {
                it.copy(
                    infoDialog = InfoDialogState(
                        title = "Nothing to search with",
                        message = "No app on this device can open a web search. Install a browser, " +
                            "then try again.",
                    )
                )
            }
        }
    }

    internal fun showGameFileLocation(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val location = game.romPath
                ?: game.packageName?.let { "Package: $it" }
                ?: "No file location on record"
            _uiState.update {
                it.copy(infoDialog = InfoDialogState(title = game.displayTitle, message = location))
            }
        }
    }

    fun dismissInfoDialog() = _uiState.update { it.copy(infoDialog = null) }

    internal fun exportGameFromMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            val report = runCatching { pcGameExporter.exportGame(gameId) }
                .onFailure { Timber.e(it, "Export Game failed for gameId=$gameId") }
                .getOrNull()
            _uiState.update {
                it.copy(infoDialog = InfoDialogState(title = game.displayTitle, message = report?.message ?: "Export failed — see the log."))
            }
        }
    }

    private fun openContextMenuForFocusedItem() {
        val state = _uiState.value
        openContextMenuFor(state.currentItems.getOrNull(state.selectedItemIndex))
    }

    // a library game's Options from the App Drawer: the same menu as on the crossbar, drawn over the drawer
    // the drawer on the companion asked: the menu opens there, where the controller is (owner, 2026-10-08)
    internal fun openGameMenuOnCompanion(gameId: Long) {
        _uiState.update { it.copy(menuOnCompanion = true) }
        openGameMenu(gameId)
    }

    fun openGameMenu(gameId: Long) {
        viewModelScope.launch {
            val game = gameRepository.getById(gameId) ?: return@launch
            openContextMenuFor(listOf(game).toCrossbarItems().first())
        }
    }

    // the one place an item's Options menu is chosen: the crossbar, a long press, Search's banner and the drawer
    internal fun openContextMenuFor(item: CrossbarItem?) {
        val before = _uiState.value.activeContextMenu
        // the focused row of the crossbar also offers Move (when it can move) and Move Column
        val moveRows = item?.let { move.menuRows(_uiState.value, it) }.orEmpty()
        openItemMenu(item)
        if (item == null || moveRows.isEmpty()) return
        fun CrossbarContextMenu.withMove() = copy(state = state.copy(rows = state.rows + moveRows))
        val now = _uiState.value.activeContextMenu
        if (now != null && now !== before) {
            _uiState.update { it.copy(activeContextMenu = now.withMove()) }
            return
        }
        // some menus are built in a coroutine (a system's reads its emulators first): add the rows when it
        // opens. ponytail: a row with no menu of its own waits MENU_WAIT_MS before its Move-only menu shows
        viewModelScope.launch {
            val opened = withTimeoutOrNull(MENU_WAIT_MS) {
                _uiState.first { it.activeContextMenu != null && it.activeContextMenu !== before }.activeContextMenu
            }
            _uiState.update { s ->
                when {
                    opened != null && s.activeContextMenu === opened -> s.copy(activeContextMenu = opened.withMove())
                    opened == null && s.activeContextMenu == null && s.moving == null ->
                        s.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = moveRows)))
                    else -> s
                }
            }
        }
    }

    // a Recent item's Options from the second screen (owner, 2026-10-08): its own menu, with no Move, which
    // belongs to the crossbar's focused row
    internal fun openRecentItemMenu(item: CrossbarItem) {
        _uiState.update { it.copy(menuOnCompanion = true) }
        openItemMenu(item)
    }

    // Y on the companion's music remote: the player's options open on that screen
    internal fun openMusicOptionsOnCompanion() {
        _uiState.update { it.copy(menuOnCompanion = true) }
        music.openMusicPlayerOptions()
    }

    // the one way the music player opens: with a second screen it is the companion's Music page, and the
    // crossbar's screen stays free to browse (owner, 2026-10-08); on one screen it covers the crossbar
    internal fun showMusicPlayer() {
        if (_uiState.value.secondScreen) bottomScreen.showPage(com.echo.feature.crossbar.bottomscreen.BottomPage.MUSIC)
        else _uiState.update { it.copy(musicPlayerVisible = true) }
    }

    private fun openItemMenu(item: CrossbarItem?) {
        when {
            item?.mediaRootUri != null && item.mediaRootKind == null -> folders.openRomRootContextMenu(item)
            item?.mediaRootUri != null -> folders.openMediaRootContextMenu(item)
            item?.mediaRootKind != null && item.type == CrossbarItemType.MEDIA_ROOT ->
                folders.openMediaFoldersContextMenu(item)
            item?.isRecentAlbum == true -> music.openRecentAlbumContextMenu(item)
            item != null && music.openMusicContextMenu(item) -> Unit
            item != null && video.openVideoContextMenu(item) -> Unit
            item != null && bookshelf.openBookContextMenu(item) -> Unit
            item != null && gallery.openPhotoContextMenu(item) -> Unit
            item?.gameId != null -> gameActions.openGameContextMenu(item)
            item?.type == CrossbarItemType.ALL_GAMES -> openAllGamesContextMenu()
            item?.platformId != null -> openPlatformContextMenu(item.platformId)
            item?.packageName != null -> openAppContextMenu(item)
        }
    }

    fun toggleQuickSetting(setting: QuickSetting, chip: Int = _uiState.value.panelChip) {
        menuSound.play(MenuSound.SELECT)
        val s = _uiState.value
        viewModelScope.launch {
            when (setting) {
                QuickSetting.WAVE -> context.echoDataStore.edit { prefs ->
                    prefs[KEY_WAVE_STYLE] = s.waveStyle.next.name
                }
                QuickSetting.BACKDROP -> iconDisplayPreferences.setItemBackdrop(!s.itemBackdropEnabled)
                // owner, 2026-10-07: Icons means icons in every column, so a folder's grid of covers goes too
                QuickSetting.ROW_ART -> {
                    iconDisplayPreferences.setGameRows(s.iconStyle != GameIconStyle.COVER_ART)
                }
                QuickSetting.RECENT_APPS -> context.echoDataStore.edit { it[KEY_RECENTS_INCLUDE_APPS] = !s.recentsIncludeApps }
                QuickSetting.MINIMAL_HINTS -> context.echoDataStore.edit {
                    with(com.echo.core.data.repository.InterfacePreferences) {
                        it.setButtonHints(buttonHintsOf(s.contextMenuHintEnabled, s.interfaceChoices.minimalHints).next)
                    }
                }
                QuickSetting.SECOND_SCREEN -> bottomScreen.setSecondScreenEnabled(!s.secondScreenEnabled)
                QuickSetting.LIBRARIES -> s.libraryChips.getOrNull(chip)?.let { categoryRepository.setVisible(it.id, !it.visible) }
                QuickSetting.ANDROID_SETTINGS -> {
                    panel.closeNotifications()
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure { Timber.w(it, "Could not open device settings") }
                }
            }
        }
    }

    fun onQuickSettingTapped(setting: QuickSetting, chip: Int = 0) {
        _uiState.update {
            it.copy(
                panelTab = if (setting == QuickSetting.LIBRARIES) PanelTab.LIBRARIES else PanelTab.QUICK,
                panelQuick = if (setting == QuickSetting.LIBRARIES) it.panelQuick else setting,
                panelChip = chip,
            )
        }
        toggleQuickSetting(setting, chip)
    }


    private fun observeLibraryChips() {
        viewModelScope.launch {
            com.echo.core.ui.notification.SystemToasts.recent.collect { recent ->
                _uiState.update { it.copy(launcherNotices = recent) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeAll().collect { all ->
                val chips = LIBRARY_CHIP_IDS.mapNotNull { id ->
                    all.firstOrNull { it.id == id }?.let { LibraryChip(it.id, it.name, it.isVisible) }
                }
                _uiState.update { it.copy(libraryChips = chips, panelChip = it.panelChip.coerceIn(0, (chips.size - 1).coerceAtLeast(0))) }
            }
        }
    }

    internal fun runStageButton(button: GamepadAction) {
        val s = _uiState.value
        stageActions(s.panelStage(), s.clearableNoticeCount)
            .firstOrNull { it.button == button }
            ?.let { runStageCommand(it.command) }
    }

    // the one place a transport press reaches a player: ECHO's own, or the external app's media session
    private fun transport(command: StageCommand, external: String?) {
        when (command) {
            StageCommand.PLAY_PAUSE -> if (external != null) AndroidNotifications.playPause() else musicPlayer.playPause()
            StageCommand.NEXT_TRACK -> if (external != null) AndroidNotifications.skipNext() else musicPlayer.next()
            StageCommand.PREV_TRACK -> if (external != null) AndroidNotifications.skipPrevious() else musicPlayer.prev()
            else -> Unit
        }
    }

    // when the island was last pressed, by tap or Select, so a second press in time opens the player
    private var orbPressAt = 0L

    private fun orbDoublePressed(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        val double = isOrbDoublePress(orbPressAt, now)
        orbPressAt = if (double) 0L else now
        return double
    }

    // the player for whatever is playing: ECHO's own, or the app whose session it is
    private fun openNowPlaying() {
        val music = _uiState.value.mediaStage() as? PanelStage.Music ?: return
        menuSound.play(MenuSound.SELECT)
        _uiState.update { it.copy(orbLevel = 0) }
        val pkg = music.packageName
        if (pkg != null) launching.launchAppWithDisc(pkg, music.art) else if (_uiState.value.musicPlayback.track != null) showMusicPlayer()
    }

    fun onOrbTapped() {
        markTouchInput()
        val kind = _uiState.value.orbKind() ?: return
        if (kind == OrbKind.MUSIC && orbDoublePressed()) return openNowPlaying()
        menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(orbLevel = if (it.orbLevel == 0) 1 else 0) }
    }

    fun onOrbTransport(command: StageCommand) {
        markTouchInput()
        transport(command, (_uiState.value.mediaStage() as? PanelStage.Music)?.packageName)
    }

    // true when the orb took the press
    // Start: the island takes the controller, as up from the top of a list does; nothing when it is empty
    private fun focusOrb(state: CrossbarUiState) {
        if (state.orbKind() == null) return
        orbDoublePressed()
        menuSound.play(MenuSound.SCROLL)
        _uiState.update { it.copy(orbLevel = 1) }
    }

    // the press as globalStep decides it; true when it was taken here
    private fun globalButton(action: GamepadAction, state: CrossbarUiState): Boolean {
        when (globalStep(action, state) ?: return false) {
            GlobalStep.APPS -> {
                _uiState.update { it.withScreensOverCrossbarClosed().copy(search = null) }
                onOpenAppDrawer()
            }
            GlobalStep.SEARCH -> {
                _uiState.update { it.withScreensOverCrossbarClosed() }
                librarySearch.openSearch(SearchScope.ALL)
            }
            GlobalStep.CLOSE_SEARCH -> librarySearch.closeSearch()
            GlobalStep.NOTIFICATIONS -> panel.pressNoticeIsland()
            GlobalStep.HOME -> {
                menuSound.play(MenuSound.BACK)
                _uiState.update {
                    it.withScreensOverCrossbarClosed().withDrawerAndSearchClosed().withSettingsClosed()
                        .copy(notificationsOpen = false, orbLevel = 0)
                }
            }
        }
        return true
    }

    private fun orbPressHandled(action: GamepadAction, state: CrossbarUiState): Boolean {
        if (state.orbLevel == 0) return false
        val kind = state.orbKind()
        if (kind == null) {
            _uiState.update { it.copy(orbLevel = 0) }
            return false
        }
        if (action == GamepadAction.OPEN_ISLAND && kind == OrbKind.MUSIC && orbDoublePressed()) {
            openNowPlaying()
            return true
        }
        when (val step = orbStep(action, state.orbLevel, kind)) {
            is OrbStep.Level -> {
                menuSound.play(if (step.level == 0) MenuSound.BACK else MenuSound.SCROLL)
                _uiState.update { it.copy(orbLevel = step.level) }
            }
            is OrbStep.Transport -> transport(step.command, (state.mediaStage() as? PanelStage.Music)?.packageName)
            OrbStep.Launch -> state.recentTop?.let { top ->
                if (!holdToLaunch(top) { recents.launchRecentTop() }) recents.launchRecentTop()
            }
            OrbStep.Stay -> Unit
            OrbStep.RestAndPass -> {
                _uiState.update { it.copy(orbLevel = 0) }
                return false
            }
        }
        return true
    }

    private fun runStageCommand(command: StageCommand) {
        val s = _uiState.value
        val focus = s.focusedNotice
        val external = (s.panelStage() as? PanelStage.Music)?.packageName
        when (command) {
            StageCommand.PLAY_PAUSE, StageCommand.NEXT_TRACK, StageCommand.PREV_TRACK -> transport(command, external)
            StageCommand.OPEN_APP -> {
                val pkg = external ?: return
                menuSound.play(MenuSound.SELECT)
                panel.closeNotifications()
                launching.launchAppWithDisc(pkg, (s.panelStage() as? PanelStage.Music)?.art)
            }
            StageCommand.OPEN_MUSIC -> {
                menuSound.play(MenuSound.SELECT)
                panel.closeNotifications()
                if (s.musicPlayback.track != null) showMusicPlayer()
            }
            StageCommand.LAUNCH_RECENT -> {
                panel.closeNotifications()
                recents.launchRecentTop()
            }
            StageCommand.OPEN_NOTICE -> {
                val key = (focus as? NoticeFocus.Notice)?.key ?: return
                panel.openAndroidNotice(key)
                panel.closeNotifications()
            }
            StageCommand.DISMISS -> {
                menuSound.play(MenuSound.BACK)
                when (focus) {
                    is NoticeFocus.Notice -> AndroidNotifications.dismiss(focus.key)
                    is NoticeFocus.Launcher -> com.echo.core.ui.notification.SystemToasts.dismiss(focus.id)
                    else -> Unit
                }
            }
            StageCommand.CLEAR_ALL -> {
                menuSound.play(MenuSound.BACK)
                s.androidNotices.filter { it.canDismiss }.forEach { AndroidNotifications.dismiss(it.key) }
                com.echo.core.ui.notification.SystemToasts.clear()
            }
        }
    }


    fun onContextMenuItemActivatedAt(index: Int) {
        val state = _uiState.value
        val menu = state.activeContextMenu ?: return

        when (val chosen = state.activeContextMenu?.state?.chose(index)) {
            is MenuSelect.Replace -> _uiState.update { it.copy(activeContextMenu = menu.copy(state = chosen.state)) }
            is MenuSelect.Run -> {
                _uiState.update { it.copy(activeContextMenu = menu.withSelected(index)) }
                activateContextMenuItem(chosen.action)
            }
            else -> Unit
        }
    }

    fun closeContextMenu() {
        _uiState.update { it.copy(activeContextMenu = null) }
    }

    fun popContextMenu() {
        _uiState.update { s ->
            val menu = s.activeContextMenu ?: return@update s
            s.copy(activeContextMenu = menu.state.parent?.let { menu.copy(state = it) })
        }
    }

    internal fun cancelConfirm() {
        _uiState.update {
            val picker = it.appPicker ?: return@update it
            it.copy(appPicker = picker.cancelConfirm())
        }
    }

    fun openGamePicker(categoryId: String) {
        _uiState.update { it.copy(gamePickerCategoryId = categoryId) }
    }

    fun closeGamePicker() {
        _uiState.update { it.copy(gamePickerCategoryId = null, pendingGamePickerAction = null) }
    }

    fun consumeGamePickerAction() {
        _uiState.update { it.copy(pendingGamePickerAction = null) }
    }

    fun confirmGamePicker(selectedGameIds: Set<Long>) {
        val categoryId = _uiState.value.gamePickerCategoryId ?: return
        menuSound.play(MenuSound.CONFIRM)
        closeGamePicker()

        viewModelScope.launch {
            selectedGameIds.forEach { gameId ->
                gameCategoryRepository.addGameToCategory(gameId, categoryId)
            }

            val category = _uiState.value.categories.getOrNull(_uiState.value.selectedCategoryIndex)
            if (category?.id == categoryId) {
                loadItemsForCategory(category)
            }
        }
    }

    internal fun openGameCategoryPicker(gameId: Long, fromCategoryId: String, action: String) {
        val items = buildList {
            _uiState.value.categories
                .filter { it.isGamingCategory && it.id != fromCategoryId && it.id != BuiltInCategory.GAMES }
                .forEach { cat ->
                    add(CrossbarContextMenuItem("cat_${cat.id}", cat.name))
                }
        }

        if (items.isEmpty()) return

        _uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = if (action == "move") "Move Game To" else "Add Game To", rows = items), gameId = gameId, categoryContext = fromCategoryId, pendingAppAction = action)
        )}
    }

    fun onPlatformLongPress(categoryIndex: Int) {
        _uiState.value.currentItems.getOrNull(categoryIndex)?.platformId?.let(::openPlatformContextMenu)
    }

    internal fun cardName(platformId: String): String =
        knownPlatformName(platformId) ?: platformId.uppercase()

    private val taskLabels = mutableMapOf<String, String>()

    internal fun addBackgroundTask(task: BackgroundTaskInfo) {
        taskLabels[task.id] = task.label
        taskNotifier.running(task.id, task.label, task.progress)
    }

    internal fun updateBackgroundTask(id: String, progress: Float) {
        val label = taskLabels[id] ?: return
        taskNotifier.running(id, label, progress.coerceIn(0f, 1f))
    }

    internal fun completeBackgroundTask(id: String, message: String? = null) {
        val label = taskLabels.remove(id) ?: "Done"
        taskNotifier.complete(id, label, message)
    }

    internal fun failBackgroundTask(id: String, message: String) {
        val label = taskLabels.remove(id) ?: "Task failed"
        taskNotifier.failed(id, label, message)
    }

    fun onCategorySelected(index: Int) {
        if (index != _uiState.value.selectedCategoryIndex) menuSound.play(MenuSound.SYSTEM_BROWSE)
        val category = _uiState.value.categories.getOrNull(index)

        _uiState.update { it.copy(selectedCategoryIndex = index, selectedItemIndex = 0, recentRailVisible = false, selectedPlatformId = null, musicNav = MusicNav.Root, videoNav = VideoNav.Root, photoNav = PhotoNav.Root, romFoldersOpen = false, activeAppDrawerFilter = null) }
        tintWaveForCategory(category)
        loadItemsForCategory(category)
    }

    fun onCategoryTapped(index: Int) {
        markTouchInput()
        val s = _uiState.value
        if (s.hasBlockingOverlay || s.isInSubItem) return
        onCategorySelected(index)
    }

    fun stepCategory(direction: Int) {
        markTouchInput()
        val s = _uiState.value
        if (s.hasBlockingOverlay) return

        if (s.isInSubItem) return
        when (swipeRailStep(direction, s.onLastPlayedHome, s.recentRailVisible)) {
            RailStep.Open, RailStep.Close -> {
                menuSound.play(MenuSound.SYSTEM_BROWSE)
                _uiState.update { it.copy(recentRailVisible = !s.recentRailVisible) }
                return
            }
            RailStep.Pass -> Unit
        }
        val next = s.stepToReachableCategory(direction)
        if (next != s.selectedCategoryIndex) onCategorySelected(next)
    }

    // MainActivity reports whether the device has a second display when it comes to the front
    fun secondDisplayPresent(present: Boolean) {
        if (_uiState.value.secondDisplayPresent != present) _uiState.update { it.copy(secondDisplayPresent = present) }
    }

    // a tap on a screen that shows the companion (true) or the XMB (false); with a second screen, the
    // controller then drives that screen's content
    fun touchedScreen(showsCompanion: Boolean) {
        if (_uiState.value.secondScreen) bottomScreen.setCompanionActive(showsCompanion)
    }

    // a column that cannot be reached now (Last Played, once a second screen shows it) is left for
    // the nearest one that can
    internal fun leaveUnreachableCategory() {
        val s = _uiState.value
        val current = s.categories.getOrNull(s.selectedCategoryIndex) ?: return
        if (s.categoryReachable(current)) return
        val next = s.stepToReachableCategory(+1).takeIf { it != s.selectedCategoryIndex } ?: s.stepToReachableCategory(-1)
        if (next != s.selectedCategoryIndex) onCategorySelected(next)
    }

    private fun CrossbarUiState.stepToReachableCategory(delta: Int): Int {
        var next = selectedCategoryIndex + delta
        while (next in categories.indices) {
            if (categoryReachable(categories[next])) return next
            next += delta
        }
        return selectedCategoryIndex
    }

    private fun moveItemCursor(delta: Int): Boolean {
        val s = _uiState.value
        if (s.hasBlockingOverlay || delta == 0) return false
        val max = (s.currentItems.size - 1).coerceAtLeast(0)
        val next = (s.selectedItemIndex + delta).coerceIn(0, max)
        if (next == s.selectedItemIndex) return false
        _uiState.update {
            // owner, 2026-10-04: on Last Played, up and down change the stage; only LEFT opens the rail
            it.copy(selectedItemIndex = next)
        }
        menuSound.play(MenuSound.SCROLL)
        return true
    }

    fun stepItem(steps: Int) {
        markTouchInput()
        moveItemCursor(steps)
    }

    fun onItemTap(index: Int) {
        markTouchInput()
        val s = _uiState.value
        if (s.hasBlockingOverlay) return
        if (index == s.selectedItemIndex) {
            // a game or app launches only by holding the launch button
            if (s.currentItems.getOrNull(index)?.launchesOut() != true) activateSelected()
        } else {
            val clamped = index.coerceIn(0, (s.currentItems.size - 1).coerceAtLeast(0))
            if (clamped != s.selectedItemIndex) {
                _uiState.update { it.copy(selectedItemIndex = clamped) }
                menuSound.play(MenuSound.SCROLL)
            }
        }
    }

    private fun activateSelected() {
        onItemSelected(_uiState.value.selectedItemIndex)
    }

    fun markTouchInput() {
        lastInteractionMs = SystemClock.elapsedRealtime()

        _uiState.update {
            if (!it.hintsAutoHide) {
                it.copy(lastInputWasTouch = true).withHintsShownNow()
            } else if (it.lastInputWasTouch &&
                !it.showContextMenuHint &&
                !it.showSettingsHint
            ) it
            else it.copy(
                lastInputWasTouch = true,
                showContextMenuHint = false,
                showSettingsHint = false,
            )
        }
    }

    private fun dispatchCategorySelection(item: CrossbarItem): Boolean {
        folders.romFolderSelection(item)?.let { return it }
        folders.mediaRootSelection(item)?.let { return it }
        return when (item.menuHostCategory(currentCategory()?.id)) {
            BuiltInCategory.MUSIC   -> music.handleMusicSelection(item)
            BuiltInCategory.VIDEO   -> video.handleVideoSelection(item)
            BuiltInCategory.PHOTO   -> gallery.handlePhotoSelection(item)
            BuiltInCategory.LIBRARY -> bookshelf.handleBooksSelection(item)
            else -> false
        }
    }

    private fun markControllerInput() {
        lastInteractionMs = SystemClock.elapsedRealtime()
        _uiState.update {
            if (!it.hintsAutoHide) {
                it.copy(lastInputWasTouch = false).withHintsShownNow()
            } else if (!it.lastInputWasTouch &&
                !it.showContextMenuHint &&
                !it.showSettingsHint
            ) it
            else it.copy(
                lastInputWasTouch = false,
                showContextMenuHint = false,
                showSettingsHint = false,
            )
        }
    }

    private fun backOutOfDrill(s: CrossbarUiState): Boolean {
        when (s.drillOutStep) {
            DrillOutStep.MUSIC -> music.backOutOfMusicView()

            DrillOutStep.VIDEO_LIBRARY -> video.openVideoView(VideoNav.Libraries)
            DrillOutStep.VIDEO_PLAYLIST -> video.openVideoView(VideoNav.Playlists)
            DrillOutStep.VIDEO_COLLECTION_CHILD -> video.openVideoView(VideoNav.Collections)
            DrillOutStep.VIDEO -> video.closeVideoView()

            DrillOutStep.PHOTO_LIBRARY -> gallery.openPhotoView(PhotoNav.Albums)
            DrillOutStep.PHOTO -> gallery.closePhotoView()
            DrillOutStep.LIBRARY_SERIES -> bookshelf.openBooksView(BooksNav.SeriesList)
            DrillOutStep.LIBRARY_GENRE -> bookshelf.openBooksView(BooksNav.Genres)
            DrillOutStep.LIBRARY_SHELF -> bookshelf.openBooksView(BooksNav.Shelves)
            DrillOutStep.LIBRARY -> bookshelf.closeBooksView()
            DrillOutStep.ROM_FOLDERS -> folders.closeRomFolders()
            DrillOutStep.PLATFORM_FOLDER -> closePlatformFolder()
            null -> return false
        }
        return true
    }

    fun onHomeBack() {
        markTouchInput()
        val s = _uiState.value
        if (s.hasBlockingOverlay) return
        menuSound.play(MenuSound.BACK)
        if (!backOutOfDrill(s)) onOpenAppDrawer()
    }

    // opens or launches an item as the column does: a video, book, track or album opens in ECHO; a game,
    // shortcut or app launches. False when the item is none of these. The bottom screen's Recent page
    // opens its items through this too.
    internal fun openItem(item: CrossbarItem): Boolean {
        when (item.type) {
            CrossbarItemType.VIDEO_FILE -> {
                menuSound.play(MenuSound.SELECT)
                _uiState.update { it.copy(activeVideoId = item.id.removePrefix("vid_"), activeVideoAutoPlay = true) }
                return true
            }
            CrossbarItemType.LIBRARY_BOOK -> {
                menuSound.play(MenuSound.SELECT)
                bookshelf.openBook(item.id.removePrefix("book_"))
                return true
            }
            CrossbarItemType.MUSIC_TRACK -> {
                menuSound.play(MenuSound.SELECT)
                music.openMusicPlayerForItem(item)
                return true
            }

            CrossbarItemType.MUSIC_GROUP -> {
                item.musicGroupKey?.let {
                    menuSound.play(MenuSound.SELECT)
                    music.openAlbumOnCrossbar(item.title, it)
                }
                return true
            }
            else -> Unit
        }

        if (item.gameId != null && item.isRealGame) {
            launching.launchGameDirectly(item.gameId)
            return true
        }

        if (item.launchIntentUri != null) {
            launching.launchStoredIntent(item.launchIntentUri, item.title)
            return true
        }

        if (item.shortcutId != null && item.packageName != null) {
            launching.launchHarvestedShortcut(item.packageName, item.shortcutId)
            return true
        }

        if (item.packageName != null) {
            launching.launchAppWithDisc(item.packageName, item.shelfCoverArt)
            return true
        }
        return false
    }

    fun onItemSelected(index: Int) {
        if (_uiState.value.hasBlockingOverlay) return
        _uiState.update { it.copy(selectedItemIndex = index) }
        val category = _uiState.value.categories.getOrNull(_uiState.value.selectedCategoryIndex)
        val item     = _uiState.value.currentItems.getOrNull(index)

        if (item != null && dispatchCategorySelection(item)) return

        val silentRow = item?.id in setOf(NO_GAMES_ITEM_ID, EMPTY_CATEGORY_ITEM_ID)

        val launchesGame = item?.gameId != null && item.isRealGame
        val launches = item?.launchesOut() == true

        val event = when {
            silentRow -> null
            launchesGame -> null
            launches -> MenuSound.LAUNCH
            else -> MenuSound.SELECT
        }
        event?.let { menuSound.play(it) }

        when (item?.id) {
            NO_CONSOLES_ITEM_ID -> {
                _uiState.update { it.withSettingsOpen("settings_library") }
                return
            }
            SETUP_GAP_ITEM_ID -> {
                _uiState.update {
                    it.withSettingsOpen(setupState.firstGap.repairScreenId)
                }
                return
            }
            ALL_GAMES_ITEM_ID -> {
                openAllGamesFolder()
                return
            }
            in genreItemIds -> {
                genreOfItemId(item?.id)?.let { genre ->
                    _uiState.update { it.copy(genreFilter = genre) }
                    openAllGamesFolder()
                }
                return
            }

            in SHELF_CARD_IDS -> {
                item?.id?.let { recents.openShelf(it) }
                return
            }
            MISSING_ITEM_ID -> {
                openMissingFolder()
                return
            }
            QUICK_SEARCH_ITEM_ID -> {
                librarySearch.openSearch(SearchScope.WEB)
                return
            }
            SEARCH_ITEM_ID -> {
                librarySearch.openSearch(SearchScope.GAMES)
                return
            }
            ADD_APPS_ITEM_ID -> {
                category?.id?.let { appPickerSection.openAppPicker(AppPickerTarget.CategoryShortcuts(it), "Add Apps") }
                return
            }
            ADD_GAMES_ITEM_ID -> {
                category?.id?.let { openGamePicker(it) }
                return
            }
            FIND_GAMES_ITEM_ID -> {
                (item.platformId ?: _uiState.value.selectedPlatformId)?.let {
                    appPickerSection.openAppPicker(AppPickerTarget.AndroidGames(it), "Find Games")
                }
                return
            }
            NO_GAMES_ITEM_ID,
            EMPTY_CATEGORY_ITEM_ID -> return
        }

        if (item != null && openItem(item)) return

        if (item?.gameId != null) {
            openContextMenuForFocusedItem()
            return
        }
        if (item?.platformId != null) {
            openPlatformFolder(item.platformId)
            return
        }

        when (item?.id) {
            SETUP_ITEM_ID -> {
                Timber.d("Opening settings screen: settings_library (via setup prompt)")
                _uiState.update { it.withSettingsOpen("settings_library") }
            }

            ANDROID_SETTINGS_ITEM_ID -> {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { Timber.w(it, "Could not open device settings") }
            }
            OPEN_SETTINGS_ITEM_ID -> {
                val root = com.echo.core.domain.model.SETTINGS_ROOT_SCREEN_ID
                Timber.d("Opening settings: $root")
                _uiState.update { it.withSettingsOpen(root) }
            }
            else -> when (category?.id) {
                BuiltInCategory.SETTINGS -> {
                    item?.id?.let { id ->
                        Timber.d("Opening settings screen: $id")
                        _uiState.update { it.withSettingsOpen(id) }
                    }
                }
                BuiltInCategory.ANDROID -> {
                    if (item?.id?.startsWith("drawer_") == true) {
                        val filter = item.id.removePrefix("drawer_").uppercase()
                        _uiState.update { it.copy(activeAppDrawerFilter = filter) }
                    }
                }
            }
        }
    }

    fun onItemLongPress(index: Int) {
        if (_uiState.value.hasBlockingOverlay) return
        openContextMenuFor(_uiState.value.currentItems.getOrNull(index))
    }

    private fun openPlatformFolder(platformId: String) {
        val gamesCategoryIndex = _uiState.value.categories.indexOfFirst { it.id == BuiltInCategory.GAMES }
        navigateRememberingCursor {
            it.copy(
                selectedCategoryIndex = gamesCategoryIndex.takeIf { index -> index >= 0 } ?: it.selectedCategoryIndex,
                selectedPlatformId = platformId,
            )
        }
    }

    private fun openAllGamesFolder() {
        val gamesCategoryIndex = _uiState.value.categories.indexOfFirst { it.id == BuiltInCategory.GAMES }
        navigateRememberingCursor {
            it.copy(
                selectedCategoryIndex = gamesCategoryIndex.takeIf { index -> index >= 0 } ?: it.selectedCategoryIndex,
                selectedPlatformId = ALL_GAMES_PLATFORM_ID,
            )
        }
    }

    private fun openMissingFolder() {
        val gamesCategoryIndex = _uiState.value.categories.indexOfFirst { it.id == BuiltInCategory.GAMES }
        navigateRememberingCursor {
            it.copy(
                selectedCategoryIndex = gamesCategoryIndex.takeIf { index -> index >= 0 } ?: it.selectedCategoryIndex,
                selectedPlatformId = MISSING_PLATFORM_ID,
            )
        }
    }

    // a genre folder is All Games narrowed to its genre, so leaving it clears the genre
    internal fun closePlatformFolder() = navigateRememberingCursor {
        it.copy(selectedPlatformId = null, genreFilter = it.genreFilter.takeUnless { _ -> it.gameGrouping == GameGrouping.GENRE })
    }

    fun toggleGameGrouping() =
        setGameGrouping(if (_uiState.value.gameGrouping == GameGrouping.GENRE) GameGrouping.SYSTEM else GameGrouping.GENRE)

    internal fun setGameGrouping(grouping: GameGrouping) {
        menuSound.play(MenuSound.SELECT)
        viewModelScope.launch { context.echoDataStore.edit { it[KEY_GAMES_GROUP_BY] = grouping.name } }
    }


    internal fun artRefsOf(game: Game?): List<String> = listOfNotNull(
        game?.artworkUri, game?.logoUri, game?.iconUri,
    )

    internal val MANUAL_MAX_SCROLL_STEPS_ = 20

    internal suspend fun ensureAppShortcut(packageName: String): Long {
        gameRepository.getAppEntry(packageName)?.let { return it.id }
        val label = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrDefault(packageName)
        return gameRepository.upsert(
            Game(
                title         = label,

                platformId    = com.echo.core.domain.model.PlatformIds.APP_SHORTCUT,
                packageName   = packageName,
                isManualEntry = true,
                contentType   = GameContentType.ANDROID_APP,
            )
        )
    }

    fun onOpenSettingsScreen(screenId: String) {
        if (screenId != com.echo.core.domain.model.SETTINGS_ROOT_SCREEN_ID &&
            com.echo.core.domain.model.settingsEntryFor(screenId) == null
        ) {
            Timber.w("Settings rail asked for a screen outside the catalog: %s", screenId)
            return
        }
        Timber.d("Settings rail -> %s", screenId)
        _uiState.update {
            it.withSettingsOpen(screenId).copy(
                settingsReturnTo = nextReturnAddress(it.activeSettingsScreen, screenId, it.settingsReturnTo),
            )
        }
    }

    fun onSettingsBack() {
        if (_uiState.value.settingsFromPanel && _uiState.value.settingsReturnTo == null) {
            panel.returnToPanelSettings()
            return
        }
        _uiState.value.settingsReturnTo?.let { returnTo ->
            _uiState.update {
                it.withSettingsOpen(returnTo).copy(
                    settingsReturnTo = null,
                    pendingSettingsAction = null,
                )
            }
            return
        }
        val current = _uiState.value.activeSettingsScreen
        if (current != null &&
            current != com.echo.core.domain.model.SETTINGS_ROOT_SCREEN_ID &&
            com.echo.core.domain.model.settingsEntryFor(current) != null
        ) {
            _uiState.update {
                it.withSettingsOpen(com.echo.core.domain.model.SETTINGS_ROOT_SCREEN_ID).copy(
                    pendingSettingsAction = null,
                )
            }
            return
        }
        onCloseSettingsScreen()
    }

    fun onCloseSettingsScreen() {
        Timber.d("Settings closed")

        val closing = _uiState.value.activeSettingsScreen
        if (closing in WIZARD_SCREEN_IDS) {
            markInitialSetupSeen()
        }
        if (_uiState.value.settingsFromPanel) {
            panel.returnToPanelSettings()
            return
        }
        _uiState.update { it.withSettingsClosed() }
    }

    fun openAndroidLibraryPicker() {
        _uiState.update { it.withSettingsClosed() }
        appPickerSection.openAppPicker(AppPickerTarget.AndroidGames(ANDROID_PLATFORM_ID), "Add Android Apps")
    }

    fun consumeSettingsAction() {
        _uiState.update { it.copy(pendingSettingsAction = null) }
    }

    fun onOpenAppDrawer() {
        _uiState.update { it.copy(activeAppDrawerFilter = com.echo.feature.appbar.AppFilter.DEFAULT.name) }
    }

    fun addAppToOpenCategory(packageName: String) {
        val category = currentCategory() ?: return
        if (!categoryShowsApps(category)) {
            SystemToasts.post("${category.name} can't hold apps", null, ToastKind.ERROR)
            return
        }
        viewModelScope.launch {
            appCategoryRepository.addToCategory(packageName, category.id)
            SystemToasts.post("Added to ${category.name}", null, ToastKind.SUCCESS)
        }
    }

    fun onCloseAppDrawer() {
        _uiState.update { it.copy(activeAppDrawerFilter = null, pendingDrawerAction = null, pendingDrawerTypedChar = null, drawerLetterRailHeld = false) }
    }

    fun consumeDrawerAction() {
        _uiState.update { it.copy(pendingDrawerAction = null) }
    }

    private val customIconGroups: List<com.echo.themekit.IconSlot.Group> =
    listOf(
        com.echo.themekit.IconSlot.Group.CATEGORY_BAR,
        com.echo.themekit.IconSlot.Group.ITEMS,
        com.echo.themekit.IconSlot.Group.STATUS,
        com.echo.themekit.IconSlot.Group.CONSOLE,
        com.echo.themekit.IconSlot.Group.ART,
    )

    // opens on `slotKey` when given (a column's or system's Change Icon), else on the first slot
    fun openCustomIcons(slotKey: String? = null) {
        val (groupIndex, slotIndex) = customIconStart(slotKey, customIconGroups)
        _uiState.update {
            it.withSettingsClosed().copy(
                customIconSession = CustomIconSession(groups = customIconGroups, groupIndex = groupIndex, slotIndex = slotIndex),
            )
        }
    }

    fun closeCustomIcons() {
        _uiState.update { it.copy(customIconSession = null, saveThemeNameDialog = null) }
    }

    fun onCustomIconGroupMove(dir: Int) {
        val session = _uiState.value.customIconSession ?: return
        val next = (session.groupIndex + dir).mod(session.groups.size)
        _uiState.update {
            it.copy(customIconSession = session.copy(groupIndex = next, slotIndex = 0, message = null))
        }
    }

    fun onCustomIconSlotMove(dir: Int) {
        val session = _uiState.value.customIconSession ?: return
        // one past the last slot is the Reset All Icons row
        val count = CustomizableIcons.group(session.group).size
        val next = (session.slotIndex + dir).coerceIn(0, count)
        _uiState.update { it.copy(customIconSession = session.copy(slotIndex = next, message = null)) }
    }

    fun onCustomIconSlotFocused(index: Int) {
        val session = _uiState.value.customIconSession ?: return
        _uiState.update { it.copy(customIconSession = session.copy(slotIndex = index, message = null)) }
    }

    fun onIconPicked(slotKey: String, uri: android.net.Uri) {
        val session = _uiState.value.customIconSession ?: return
        viewModelScope.launch {
            val mime = context.contentResolver.getType(uri)
            val result = customIconStore.import(slotKey, uri, mime)

            menuSound.play(if (result.ok) MenuSound.CONFIRM else MenuSound.ERROR)
            _uiState.update {
                val s = it.customIconSession ?: return@update it
                it.copy(customIconSession = s.copy(message = result.message, revision = s.revision + 1))
            }
        }
    }

    fun onResetSlot(slotKey: String) {
        viewModelScope.launch {
            val removed = customIconStore.clear(slotKey)
            _uiState.update {
                val s = it.customIconSession ?: return@update it
                val themed = it.iconOverrides.containsKey(slotKey)
                val message = when {
                    removed && themed -> context.getString(R.string.crossbar_icons_reset_removed_themed)
                    removed -> null
                    themed -> context.getString(R.string.crossbar_icons_reset_themed_slot)
                    else -> context.getString(R.string.crossbar_icons_reset_builtin_slot)
                }
                it.copy(customIconSession = s.copy(message = message, revision = s.revision + 1))
            }
        }
    }

    fun onResetAll() {
        viewModelScope.launch {
            val removed = customIconStore.clearAll()
            _uiState.update {
                val s = it.customIconSession ?: return@update it
                val message = when {
                    !removed -> context.getString(R.string.crossbar_icons_reset_all_none)
                    it.iconOverrides.isNotEmpty() -> context.getString(R.string.crossbar_icons_reset_all_themed)
                    else -> null
                }
                it.copy(customIconSession = s.copy(message = message, revision = s.revision + 1))
            }
        }
    }

    fun onCustomIconsActionConsumed() {
        _uiState.update { it.copy(pendingCustomIconsAction = null) }
    }

    @Volatile
    internal var bootEnabled: Boolean = true

    @Volatile
    internal var bootOnResume: Boolean = false

    fun onHostResumed() {
        viewModelScope.launch { runCatching { appCategoryRepository.refreshLastUsed() }.onFailure { Timber.w(it, "Could not re-read app usage") } }
        if (!bootEnabled || !bootOnResume) return
        _uiState.update { it.copy(showBootSequence = true) }
    }

    private fun selectedItemArt(): Any? =
        _uiState.value.currentItems.getOrNull(_uiState.value.selectedItemIndex)?.shelfCoverArt

    fun onStartupPermissionsSettled() {
        Timber.d("StartupSeq: notification permission settled")
        _uiState.update { it.copy(startupPermissionsSettled = true) }
    }

    private val startupSetupDecision = CompletableDeferred<InitialSetupDecision>()

    suspend fun firstRunWizardAsksForNotifications(): Boolean =
        wizardOwnsNotificationPrompt(startupSetupDecision.await())

    private fun checkInitialSetup() {
        viewModelScope.launch {
            val prefs = context.echoDataStore.data.first()
            val decision = initialSetupDecision(prefs, gameActions.existingCards())
            startupSetupDecision.complete(decision)
            when (decision) {
                InitialSetupDecision.ALREADY_SEEN ->
                    Timber.d("StartupSeq: initial setup already seen")
                InitialSetupDecision.SEED_AS_SEEN -> {
                    context.echoDataStore.edit { it[KEY_INITIAL_SETUP_SEEN] = true }
                    Timber.i("StartupSeq: existing configuration found, wizard seeded as seen")
                }
                InitialSetupDecision.OPEN_WIZARD -> {
                    context.echoDataStore.edit { it[KEY_INITIAL_SETUP_STARTED] = true }
                    Timber.i("StartupSeq: fresh install, opening first-run wizard")
                    _uiState.update { it.withSettingsOpen(INITIAL_SETUP_FIRST_RUN_SCREEN_ID) }
                }
            }

            _uiState.update { it.copy(initialSetupDecided = true) }
        }
    }

    private fun logStartupSequence() {
        viewModelScope.launch {
            _uiState
                .map { Triple(it.startupPermissionsSettled, it.showBootSequence, it.activeSettingsScreen) }
                .distinctUntilChanged()
                .transformWhile { emit(it); it.second }
                .collect { (settled, boot, screen) ->
                    Timber.v(
                        "StartupSeq: permissionsSettled=$settled showBootSequence=$boot " +
                            "activeSettingsScreen=$screen crossbarForegroundVisible=${!boot && screen == null}"
                    )
                }
        }
    }

    private fun markInitialSetupSeen() {
        viewModelScope.launch {
            context.echoDataStore.edit { it[KEY_INITIAL_SETUP_SEEN] = true }
        }
    }

    fun goToLibrary() {
        markInitialSetupSeen()
        _uiState.update { it.withSettingsClosed() }
        openAllGamesFolder()
        viewModelScope.launch {
            val first = runCatching { gameRepository.observeGamesOnly().first() }
                .getOrDefault(emptyList())
                .filterNot { it.isMissing }
                .minByOrNull { it.title.lowercase() }
            if (first != null) {
                val idx = _uiState.value.currentItems.indexOfFirst { it.gameId == first.id }
                if (idx > 0) _uiState.update { it.copy(selectedItemIndex = idx) }
            }
        }
    }

    internal var setupState: com.echo.feature.launcher.SetupState =
        com.echo.feature.launcher.SetupState()

    private fun observeSetupState() {
        viewModelScope.launch {
            setupStateProvider.observe().collect { fresh ->
                if (fresh != setupState) {
                    setupState = fresh

                    if (_uiState.value.currentItems.any { it.type == CrossbarItemType.EMPTY }) {
                        loadItemsForCategory(currentCategory())
                    }
                }
            }
        }
    }

    fun onUserInteraction() {
        lastInteractionMs = SystemClock.elapsedRealtime()
        if (!_uiState.value.hintsAutoHide) {
            _uiState.update { it.withHintsShownNow() }
        } else if (_uiState.value.showContextMenuHint ||
            _uiState.value.showSettingsHint
        ) {
            _uiState.update {
                it.copy(
                    showContextMenuHint = false,
                    showSettingsHint = false,
                )
            }
        }
    }

    private fun observeLibrarySetupState() {
        viewModelScope.launch {
            context.echoDataStore.data.collect { prefs ->
                val complete = prefs[KEY_SETUP_COMPLETE] ?: false
                _uiState.update { it.copy(librarySetupComplete = complete) }
            }
        }
    }

    private fun observeIconPreferences() {
        viewModelScope.launch {
            iconDisplayPreferences.animatedIconsFlow.collect { enabled ->
                animatedIconsEnabled = enabled
                if (!enabled) _uiState.update { it.copy(focusedGameVideo = null) }
            }
        }
        viewModelScope.launch {
            iconDisplayPreferences.gameMetadataFlow.collect { visible ->
                _uiState.update { it.copy(gameMetadataVisible = visible) }
            }
        }
        viewModelScope.launch {
            iconDisplayPreferences.rowCoverArtFlow.collect { cover ->
                _uiState.update { it.copy(iconStyle = if (cover) GameIconStyle.COVER_ART else GameIconStyle.PSP_RECTANGLE) }
            }
        }
        viewModelScope.launch {
            iconDisplayPreferences.itemBackdropFlow.collect { on ->
                _uiState.update { it.copy(itemBackdropEnabled = on) }
            }
        }
        viewModelScope.launch {
            iconDisplayPreferences.snapPlacementFlow.collect { placement ->

                _uiState.update { it.copy(snapPlacement = placement, focusedGameVideo = null) }
            }
        }
        viewModelScope.launch {
            iconDisplayPreferences.lingerDelaySecondsFlow.collect { seconds ->
                icon1LingerMs = (seconds * 1_000f).toLong()
            }
        }
    }

    @Volatile private var animatedIconsEnabled = true

    private val ACCENT_SETTLE_MS = 220L

    @Volatile private var icon1LingerMs = ICON1_LINGER_MS

    private fun observeFocusedItemAccent() {
        viewModelScope.launch {
            _uiState
                .map { s -> s.currentItems.getOrNull(s.selectedItemIndex)?.takeIf { it.backdropArt.isNotEmpty() } }

                .distinctUntilChanged { a, b -> a?.backdropIdentity() == b?.backdropIdentity() }
                .collectLatest { item ->
                    if (item == null) {
                        _uiState.update {
                            it.copy(focusedItemAccentArgb = null, focusedItemBackdrop = null)
                        }
                        return@collectLatest
                    }
                    kotlinx.coroutines.delay(ACCENT_SETTLE_MS)
                    val art = artworkAccent.resolve(*item.backdropArt.toTypedArray())

                    _uiState.update {
                        it.copy(
                            focusedItemAccentArgb = art?.accent,
                            focusedItemBackdrop = art?.uri,
                        )
                    }
                }
        }
    }

    private fun observeFocusedGameVideo() {
        viewModelScope.launch {
            _uiState
                .map { s ->
                    val item = s.currentItems.getOrNull(s.selectedItemIndex)

                    val eligible = item?.gameId != null && item.isRealGame && !s.hasBlockingOverlay
                    if (eligible) item.gameId else null
                }
                .distinctUntilChanged()
                .collectLatest { gameId ->
                    if (_uiState.value.focusedGameVideo?.gameId != gameId) {
                        _uiState.update { it.copy(focusedGameVideo = null) }
                    }
                    if (gameId == null) return@collectLatest
                    kotlinx.coroutines.delay(icon1LingerMs)
                    if (!videoSnapsAllowed()) {
                        Timber.d("ICON1: gates vetoed playback for game $gameId (toggle/battery/thermal)")
                        return@collectLatest
                    }

                    val uri = artworkStore.find(gameId, com.echo.feature.artwork.store.ArtworkKind.ICON1)
                        ?: artworkStore.find(gameId, com.echo.feature.artwork.store.ArtworkKind.VIDEO)
                    if (uri == null) {
                        Timber.d("ICON1: no icon video stored for game $gameId (enable Download Video Snaps + rescrape)")
                        return@collectLatest
                    }
                    Timber.d("ICON1: playing snap for game $gameId from $uri")
                    _uiState.update {
                        it.copy(
                            focusedGameVideo = com.echo.feature.crossbar.ui.FocusedGameVideo(
                                gameId = gameId,
                                uri = uri,
                                placement = it.snapPlacement,
                            ),
                        )
                    }
                }
        }
    }

    internal fun videoSnapsAllowed(): Boolean {
        if (!animatedIconsEnabled) return false
        val pm = context.getSystemService(android.os.PowerManager::class.java)
        if (pm?.isPowerSaveMode == true) return false
        if ((pm?.currentThermalStatus ?: 0) >= android.os.PowerManager.THERMAL_STATUS_MODERATE) return false
        val bm = context.getSystemService(android.os.BatteryManager::class.java)
        val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        if (level in 1 until 20 && bm?.isCharging != true) return false
        return true
    }

    private fun observeTouchNavButtonMode() {
        viewModelScope.launch {
            context.echoDataStore.data.collect { prefs ->
                val mode = com.echo.core.domain.model.TouchNavButtonMode
                    .fromName(prefs[KEY_TOUCH_NAV_BUTTON])
                val sensitivity = com.echo.core.domain.model.TouchSensitivity
                    .fromName(prefs[KEY_TOUCH_SENSITIVITY])
                val hintEnabled = prefs[KEY_CONTEXT_MENU_HINT] ?: ControllerHintPolicy.DEFAULT_ENABLED
                val hintDelaySeconds =
                    ControllerHintPolicy.clampDelay(
                        prefs[KEY_CONTEXT_MENU_HINT_DELAY_SECONDS] ?: ControllerHintPolicy.DEFAULT_DELAY_SECONDS
                    )
                val legibility = com.echo.core.domain.model.IconLegibilityStyle
                    .fromName(prefs[KEY_ICON_LEGIBILITY])
                val fadeByDistance = prefs[KEY_FADE_BY_DISTANCE] ?: true
                val cardArtGrid = com.echo.core.data.repository.IconDisplayPreferences.cardArtGrid(prefs)
                val recentsIncludeApps = prefs[KEY_RECENTS_INCLUDE_APPS] ?: false
                val interfaceChoices = com.echo.core.data.repository.InterfacePreferences.read(prefs)
                val textShadow = prefs[KEY_TEXT_SHADOW] ?: true
                _uiState.update {
                    it.copy(
                        touchNavButtonMode = mode,
                        touchSensitivity = sensitivity,
                        contextMenuHintEnabled = hintEnabled,
                        contextMenuHintDelaySeconds = hintDelaySeconds,
                        iconLegibility = legibility,
                        fadeByDistance = fadeByDistance,
                        cardArtGrid = cardArtGrid,
                        recentsIncludeApps = recentsIncludeApps,
                        interfaceChoices = interfaceChoices,
                        textShadow = textShadow,
                    )
                }
            }
        }
    }

    private fun observeBackgroundSettings() {
        viewModelScope.launch {
            context.echoDataStore.data.collect { prefs ->
                val style = runCatching {
                    WaveStyle.valueOf(prefs[KEY_WAVE_STYLE] ?: WaveStyle.ANIMATED.name)
                }.getOrDefault(WaveStyle.ANIMATED)
                val design = com.echo.core.ui.wave.WaveDesign.of(prefs[KEY_WAVE_DESIGN])
                val grouping = GameGrouping.fromName(prefs[KEY_GAMES_GROUP_BY])
                if (grouping != _uiState.value.gameGrouping) {
                    _uiState.update { it.copy(gameGrouping = grouping) }
                    if (categoryShowsGameRows(currentCategory())) loadItemsForCategory(currentCategory(), keepCursorOnRow = false)
                }
                _uiState.update {
                    it.copy(
                        waveStyle            = style,
                        motionStyle          = com.echo.core.ui.motion.MotionWallpaperPolicy.motionStyleOf(
                            prefs[androidx.datastore.preferences.core.stringPreferencesKey(com.echo.core.ui.motion.MotionWallpaperPolicy.KEY)],
                        ),
                        waveDesign           = design,
                        respectBatterySaver  = prefs[KEY_RESPECT_BATTERY] ?: true,
                        waveOverWallpaper    = prefs[KEY_WAVE_OVER_WALLPAPER] ?: false,
                        thermalThrottleAware = prefs[KEY_THERMAL_AWARE] ?: true,
                    )
                }
            }
        }
    }

    companion object {
        private val KEY_WAVE_STYLE        = stringPreferencesKey("display_wave_style")
        internal val KEY_GAMES_GROUP_BY   = stringPreferencesKey("games_group_by")
        private val KEY_WAVE_DESIGN       = stringPreferencesKey("display_wave_design")

        private val KEY_RESPECT_BATTERY   = booleanPreferencesKey("display_battery_saver")

        private val KEY_WAVE_OVER_WALLPAPER = booleanPreferencesKey("display_wave_over_wallpaper")
        private val KEY_THERMAL_AWARE     = booleanPreferencesKey("display_thermal_aware")
        internal val KEY_COLOR_SCHEME      = stringPreferencesKey("display_color_scheme")

        internal val KEY_ACCENT_OVERRIDE   = longPreferencesKey("theme_accent_override")

        internal val KEY_ICON_COLOR        = longPreferencesKey("theme_icon_color")

        internal val KEY_TEXT_COLOR        = longPreferencesKey("display_text_color")


        internal const val WAVE_IDLE_MS = 12_000L

        internal const val IDLE_HINT_POLL_MS  = 500L
        internal val KEY_CROSSBAR_LAYOUT_ADJUST = com.echo.core.data.datastore.CROSSBAR_LAYOUT_ADJUST_KEY
        private val KEY_SETUP_COMPLETE    = booleanPreferencesKey("library_setup_complete")

        private val KEY_INITIAL_SETUP_SEEN = com.echo.core.data.repository.InitialSetupFlag.KEY_SEEN

        private val KEY_INITIAL_SETUP_STARTED = com.echo.core.data.repository.InitialSetupFlag.KEY_STARTED

        internal fun returnAddressFor(screenId: String?): String? =
            screenId.takeIf { it in WIZARD_SCREEN_IDS }

        internal fun nextReturnAddress(from: String?, to: String, held: String?): String? =
            (returnAddressFor(from) ?: from.takeIf { it != null && com.echo.core.domain.model.settingsEntryFor(to)?.parent == it } ?: held)?.takeIf { it != to }

        internal val WIZARD_SCREEN_IDS: Set<String>
            get() = setOf(INITIAL_SETUP_SCREEN_ID, INITIAL_SETUP_FIRST_RUN_SCREEN_ID)

        internal const val INITIAL_SETUP_SCREEN_ID = "settings_initial_setup"

        internal const val INITIAL_SETUP_FIRST_RUN_SCREEN_ID = "settings_initial_setup_first"

        private val EXISTING_CONFIG_STRING_KEYS = listOf(
            stringPreferencesKey("library_rom_root_tree_uris"),
            stringPreferencesKey("library_rom_root_tree_uri"),
            stringPreferencesKey("artwork_folder_tree_uri"),

            stringPreferencesKey("sgdb_api_key"),
            stringPreferencesKey("igdb_client_id"),
            stringPreferencesKey("ss_username"),
            stringPreferencesKey("tmdb_api_key"),
        ) + com.echo.core.data.repository.MediaRootKind.entries.map { stringPreferencesKey(it.key) }

        internal fun hasExistingSetupConfig(prefs: androidx.datastore.preferences.core.Preferences): Boolean =
            prefs[KEY_SETUP_COMPLETE] == true ||
                EXISTING_CONFIG_STRING_KEYS.any { !prefs[it].isNullOrBlank() }

        internal fun initialSetupDecision(
            prefs: androidx.datastore.preferences.core.Preferences,
            cards: List<MemoryCard>,
        ): InitialSetupDecision = when {
            prefs[KEY_INITIAL_SETUP_SEEN] == true -> InitialSetupDecision.ALREADY_SEEN
            prefs[KEY_INITIAL_SETUP_STARTED] == true -> InitialSetupDecision.OPEN_WIZARD
            hasExistingSetupConfig(prefs) || cards.any { it.isUserLibrary() } -> InitialSetupDecision.SEED_AS_SEEN
            else -> InitialSetupDecision.OPEN_WIZARD
        }

        // The first-run wizard offers POST_NOTIFICATIONS itself, so the startup system prompt
        // waits until the wizard is done (owner, 2026-10-04).
        internal fun wizardOwnsNotificationPrompt(decision: InitialSetupDecision): Boolean =
            decision == InitialSetupDecision.OPEN_WIZARD

        private fun MemoryCard.isUserLibrary(): Boolean =
            platformId != ANDROID_PLATFORM_ID || gameCount > 0
        internal val KEY_CUSTOM_WALLPAPER  = stringPreferencesKey("display_custom_wallpaper")

        internal val KEY_MOTION_WALLPAPER = stringPreferencesKey("display_motion_wallpaper")

        internal val KEY_SHOW_BOOT       = booleanPreferencesKey("display_show_boot")
        internal val KEY_BOOT_ON_RESUME  = booleanPreferencesKey("display_boot_on_resume")

        private val KEY_TOUCH_NAV_BUTTON  = stringPreferencesKey("interface_touch_nav_button")

        private val KEY_CONTEXT_MENU_HINT = com.echo.core.data.repository.InterfacePreferences.KEY_BUTTON_HINTS_ON
        private val KEY_CONTEXT_MENU_HINT_DELAY_SECONDS =
            floatPreferencesKey("interface_context_menu_hint_delay_seconds")

        private val KEY_TOUCH_SENSITIVITY = stringPreferencesKey("interface_touch_sensitivity")

        private val KEY_ICON_LEGIBILITY = stringPreferencesKey("display_icon_legibility")

        private val KEY_FADE_BY_DISTANCE = booleanPreferencesKey("display_fade_by_distance")


        private val KEY_RECENTS_INCLUDE_APPS = booleanPreferencesKey("display_recents_include_apps")

        private val KEY_TEXT_SHADOW = booleanPreferencesKey("display_text_shadow")

        internal val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")

        internal val KEY_PROFILE_AVATAR = stringPreferencesKey("profile_avatar_uri")

        private const val ICON1_LINGER_MS = 1_500L
        private const val SETUP_ITEM_ID = "library_setup"
        internal const val NO_CONSOLES_ITEM_ID = "no_consoles"

        internal const val SETUP_GAP_ITEM_ID = "setup_gap"
        internal const val NO_GAMES_ITEM_ID    = "no_games"
        internal const val EMPTY_FAVORITES_ITEM_ID = "empty_favorites"
        internal const val EMPTY_CATEGORY_ITEM_ID = "empty_category"
        internal const val ALL_GAMES_ITEM_ID = "all_games"

        internal const val RESUME_DONE_FRACTION = 0.97f
        private const val ALL_GAMES_PLATFORM_ID = "__all_games__"

        internal const val FAVORITES_PLATFORM_ID = "__favorites__"
        internal const val MISSING_ITEM_ID = "missing_folder"
        internal const val MISSING_PLATFORM_ID = "__missing__"
        private const val EMPTY_MISSING_ITEM_ID = "empty_missing"

        private const val MISSING_REASON = "File not found on last scan"
        private const val ADD_APPS_ITEM_ID = "add_apps"

        internal const val RECENT_APP_ID_PREFIX = "recentapp_"
        private const val ADD_GAMES_ITEM_ID = "add_games"
        internal const val FIND_GAMES_ITEM_ID = "find_games"


        internal const val ADD_MUSIC_FOLDER_ITEM_ID = "add_music_folder"
        internal const val ADD_ROM_ROOT_ITEM_ID = "add_rom_root"
        internal const val ROM_FOLDERS_ITEM_ID = "rom_folders"
        private val NON_EMULATOR_PLATFORM_IDS = setOf(ANDROID_PLATFORM_ID, WINDOWS_PLATFORM_ID)
        private const val PLATFORM_EMU_PREFIX = "pemu_pick_"
        internal const val MEDIA_APP_NONE = "__none__"
        private const val VIDEO_PLAYER_BUILTIN = "builtin"
        private const val VIDEO_PLAYER_ASK = "ask"
        internal fun mediaRootItemId(kind: MediaRootKind, treeUri: String) = "mediaroot_${kind.name}_$treeUri"
        internal fun addMediaRootItemId(kind: MediaRootKind) = "add_mediaroot_${kind.name}"
        internal fun mediaFoldersItemId(kind: MediaRootKind) = "media_folders_${kind.name}"
        internal const val ALL_MUSIC_ITEM_ID = "all_music"
        internal const val NOW_PLAYING_ITEM_ID = "now_playing"
        internal const val PLAYLISTS_ITEM_ID = "playlists"

        // the Music rows that drill into the column, shown as the drill flyout's siblings
        internal val MUSIC_DRILL_ROOTS by lazy { setOf(ALL_MUSIC_ITEM_ID, MUSIC_ARTISTS_ITEM_ID, MUSIC_ALBUMS_ITEM_ID, MUSIC_GENRES_ITEM_ID, PLAYLISTS_ITEM_ID) }
        internal const val MUSIC_GENRES_ITEM_ID = "music_genres"
        internal const val MUSIC_ARTISTS_ITEM_ID = "music_artists"
        internal const val MUSIC_ALBUMS_ITEM_ID = "music_albums"
        internal const val ADD_MUSIC_APPS_ITEM_ID = "add_music_apps"
        internal const val CREATE_PLAYLIST_ITEM_ID = "create_playlist"
        internal const val ADD_TRACKS_ITEM_ID = "add_tracks"
        internal const val EMPTY_PLAYLIST_ITEM_ID = "empty_playlist"

        internal const val MUSIC_APPS_CATEGORY_ID = "music"

        internal const val ALL_VIDEOS_ITEM_ID = "all_videos"
        internal const val VIDEO_COLLECTIONS_ITEM_ID = "video_collections"
        internal const val RECENTLY_WATCHED_ITEM_ID = "recently_watched"
        internal const val FAVORITE_VIDEOS_ITEM_ID = "favorite_videos"
        internal const val VIDEO_PLAYLISTS_ITEM_ID = "video_playlists"
        internal const val CREATE_VIDEO_PLAYLIST_ITEM_ID = "create_video_playlist"
        internal const val VIDEO_LIBRARIES_ITEM_ID = "video_libraries"
        internal const val ADD_VIDEOS_ITEM_ID = "add_videos"
        internal const val ADD_VIDEO_APPS_ITEM_ID = "add_video_apps"
        internal const val VIDEO_APPS_CATEGORY_ID = "videos"

        internal const val ALL_PHOTOS_ITEM_ID = "all_photos"
        internal const val CAMERA_ITEM_ID = "photo_camera"
        internal const val ADD_PHOTO_LIBRARY_ITEM_ID = "add_photo_library"
        internal const val PHOTO_ALBUMS_ITEM_ID = "photo_albums"
        internal const val PHOTO_FAVORITES_ITEM_ID = "photo_favorites"
        internal const val OPEN_READER_ITEM_ID = "library_open_reader"
        internal const val BOOK_SHELVES_ITEM_ID = "library_shelves"
        internal const val BOOK_SERIES_ITEM_ID = "library_series"
        internal const val BOOK_GENRES_ITEM_ID = "library_genres"
        internal const val ALL_BOOKS_ITEM_ID = "all_books"
        internal const val ADD_BOOK_FOLDER_ITEM_ID = "add_book_folder"
        internal const val ADD_LIBRARY_APPS_ITEM_ID = "add_library_apps"

        internal val RECENTLY_PLAYED_LIMIT = com.echo.core.data.repository.InterfacePreferences.LAST_PLAYED_SIZES.max()

        internal val KEY_RECENT_APP_DISMISSALS = stringSetPreferencesKey("recent_app_dismissals")
        internal val KEY_RECENT_PINS = androidx.datastore.preferences.core.stringPreferencesKey("recent_pins")
        internal const val ADD_MENU_ITEM_ID = "add_menu"
        internal const val QUICK_SEARCH_ITEM_ID = "quick_search"
        internal const val SEARCH_ITEM_ID = "library_search"

        internal const val SEARCH_RESULTS_PER_LIBRARY = 40
        private const val NETWORK_CATEGORY_ID = "network"

        internal const val LIBRARY_APPS_CATEGORY_ID = BuiltInCategory.LIBRARY
        internal const val ADD_PHOTO_APPS_ITEM_ID = "add_photo_apps"
        internal const val PHOTO_APPS_CATEGORY_ID = "photos"

        internal const val MEMORY_CARD_ASSET_URI =
            "file:///android_asset/systems/physical-media/_default.png"

        internal const val MUSIC_PLAYER_MENU_MARKER = "__music_player__"

        val FALLBACK_CATEGORIES: List<Category> =
            com.echo.core.domain.model.BUILT_IN_CATEGORIES

        private val ANDROID_ITEMS = com.echo.feature.appbar.AppFilter.entries.map { filter ->
            CrossbarItem(
                id = "drawer_${filter.name.lowercase()}",
                title = filter.label,
                subtitle = filter.subtitle,
            )
        }

        internal const val ANDROID_SETTINGS_ITEM_ID = "settings_android_system"

        internal const val OPEN_SETTINGS_ITEM_ID = "settings_open"

        internal val SETTINGS_ROOT_ITEMS = listOf(
            CrossbarItem(
                id = OPEN_SETTINGS_ITEM_ID,
                title = "Settings",
                subtitle = "Library, emulators, appearance, media & system",
            ),
            CrossbarItem(id = ANDROID_SETTINGS_ITEM_ID, title = "Android Settings", subtitle = "Opens device settings"),
        )
    }

    private fun canonicalCrossbarCategories(categories: List<Category>): List<Category> =
        canonicalCrossbarCategories(categories, FALLBACK_CATEGORIES)

    private fun defaultCrossbarCategoryIndex(categories: List<Category>): Int =
        categories.indexOfFirst { it.id == BuiltInCategory.RECENTLY_PLAYED }
            .takeIf { it >= 0 }
            ?: categories.indexOfFirst { it.id == BuiltInCategory.GAMES }
                .takeIf { it >= 0 }
            ?: 0
}

internal fun CrossbarItem.backdropIdentity() =
    Triple(id, backdropArt, backdropArt.map { com.echo.core.ui.image.ArtworkRevisions.of(it) })
