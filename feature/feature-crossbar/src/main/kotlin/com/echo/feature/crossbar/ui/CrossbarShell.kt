package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.LocalBackdropWave
import com.echo.core.ui.design.LocalMenuBackdropArt
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import androidx.compose.ui.graphics.toArgb
import com.echo.feature.crossbar.viewmodel.resumeHoldId
import com.echo.feature.crossbar.viewmodel.resumableFocus
import com.echo.feature.crossbar.viewmodel.sortRow
import com.echo.feature.crossbar.viewmodel.OrbKind
import com.echo.feature.crossbar.viewmodel.orbKind
import com.echo.feature.crossbar.viewmodel.holdMsFor
import androidx.compose.ui.graphics.ImageBitmap
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.ui.design.mediaAccent
import com.echo.core.ui.notification.AndroidNotifications
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.echo.core.ui.notification.SystemToast
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import com.echo.core.ui.detail.EchoConfirmOverlay
import com.echo.core.ui.detail.EchoDetailLaunchButton
import com.echo.core.ui.detail.EchoMessageOverlay
import com.echo.core.ui.detail.EchoOverlayCard
import com.echo.core.ui.detail.EchoOverlayTitle
import com.echo.core.ui.detail.EchoTextPromptOverlay
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.feature.crossbar.ui.detail.DetailPanelPage
import com.echo.feature.crossbar.ui.detail.DetailPanelStrip
import com.echo.feature.crossbar.ui.detail.GameDetailPanel
import com.echo.feature.crossbar.ui.detail.resolvePanelPage
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.motion.MotionWallpaperPolicy
import com.echo.core.ui.motion.rememberAppVisible
import com.echo.core.ui.theme.LocalEchoTextColors
import androidx.compose.foundation.lazy.rememberLazyListState
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.components.padPromptsShown
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.components.DiscLaunchCeremony
import com.echo.core.ui.components.CrossbarLetterRail
import com.echo.core.ui.components.letterAnchors
import com.echo.core.ui.components.ControllerHintEdgeGap
import com.echo.core.ui.preview.DevicePreviews
import com.echo.core.ui.preview.EchoPreview
import com.echo.core.ui.theme.DefaultEchoColors
import com.echo.core.ui.theme.withWaveTint
import com.echo.core.ui.theme.menuCursorFill
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.core.ui.theme.EchoTheme
import com.echo.feature.appbar.AppDrawerScreen
import com.echo.feature.appbar.AppFilter
import com.echo.feature.settings.ui.SettingsNavHost
import com.echo.feature.crossbar.preview.PreviewData
import com.echo.feature.crossbar.ui.app.AppDetailScreen
import com.echo.feature.artwork.studio.ArtworkStudioScreen
import com.echo.feature.crossbar.ui.detail.ManualViewerOverlay
import com.echo.feature.crossbar.ui.detail.MetadataPreviewPanel
import com.echo.feature.crossbar.ui.detail.VideoDetailScreen
import com.echo.feature.crossbar.ui.photo.PhotoViewerScreen
import com.echo.feature.crossbar.viewmodel.mediaStage
import com.echo.feature.crossbar.viewmodel.recentStage
import com.echo.feature.crossbar.viewmodel.PanelStage
import com.echo.feature.crossbar.viewmodel.clearableNoticeCount
import com.echo.feature.crossbar.viewmodel.stageActions
import com.echo.feature.crossbar.viewmodel.panelEntries
import com.echo.feature.crossbar.viewmodel.panelStage
import com.echo.feature.crossbar.viewmodel.focusedPillIndex
import com.echo.feature.crossbar.viewmodel.pillRowVisible
import com.echo.feature.crossbar.viewmodel.promptsFor
import com.echo.feature.crossbar.viewmodel.menuWithPills
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.crossbar.viewmodel.fanCoversToDraw
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel

private const val CROSSBAR_BASELINE_HEIGHT_DP = 468f

private const val CROSSBAR_BASELINE_WIDTH_DP = 832f
private const val CROSSBAR_MAX_SCALE = 2.5f

private const val CROSSBAR_MIN_SCALE = 0.75f

private val DRILL_CROSSBAR_LEFT_MARGIN = 16.dp

private val CAT_BAR_HEIGHT = 112.dp

@Composable
fun CrossbarShellContainer(
    viewModel: CrossbarViewModel = hiltViewModel(),
    onSettingsLongPress: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val shareContext = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(uiState.pendingThemeShareFile) {
        val file = uiState.pendingThemeShareFile ?: return@LaunchedEffect
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                shareContext,
                "${shareContext.packageName}.fileprovider",
                file,
            )
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareContext.startActivity(
                android.content.Intent.createChooser(send, "Share theme")
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        viewModel.look.onThemeShareConsumed()
    }

    val mediaRootPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri -> viewModel.folders.onMediaRootPicked(uri) }

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.panel.onProfileAvatarPicked(uri) }

    androidx.compose.runtime.LaunchedEffect(uiState.profileAvatarPick) {
        if (uiState.profileAvatarPick) {
            avatarPicker.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.rootPick) {
        val pick = uiState.rootPick ?: return@LaunchedEffect
        mediaRootPicker.launch(
            pick.relinkFrom?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() },
        )
    }

    Box(Modifier.fillMaxSize()) {
    val playbackPositions = remember(viewModel) { PlaybackPositions(viewModel.musicPositionMs, viewModel.externalPositionMs) }
    CompositionLocalProvider(
        LocalPlaybackPositions provides playbackPositions,
        com.echo.core.ui.wave.LocalWaveDesign provides uiState.waveDesign,
    ) {
    CrossbarShell(
        uiState = uiState,
        onCategorySelected = viewModel::onCategoryTapped,
        onStepCategory = viewModel::stepCategory,
        onStepItem = viewModel::stepItem,
        onTouchBack = viewModel::onHomeBack,
        onTouchInput = viewModel::markTouchInput,
        onCrossbarSortTapped = viewModel::onSortLabelTapped,
        onPanelPageTapped = viewModel.panel::onPanelPageTapped,
        onRecentFilterTapped = viewModel.recents::setRecentFilter,
        onDrawerTypedCharConsumed = viewModel::onDrawerTypedCharConsumed,
        onNotificationsToggled = viewModel.panel::toggleNotifications,
        onLaunchRecentTop = viewModel.recents::launchRecentTop,
        onGameInfoSectionPicked = viewModel.gameDetail::openGameInfoSection,
        onNoticeChipTapped = viewModel.panel::onNoticeChipTapped,
        onSortPicked = viewModel::onSortPicked,
        onOrbTapped = viewModel::onOrbTapped,
        onOrbTransport = viewModel::onOrbTransport,
        onNotificationsDismissed = viewModel.panel::closeNotifications,
        onPanelRowTapped = viewModel.panel::onPanelRowTapped,
        onPanelTabTapped = viewModel.panel::onPanelTabTapped,
        onNotificationsSwipedOpen = viewModel.panel::onNotificationsSwipedOpen,
        onNotificationsSwipedClosed = viewModel.panel::onNotificationsSwipedClosed,
        onPanelSettingTapped = viewModel.panel::onPanelSettingTapped,
        onOpenAppDrawer = viewModel::onOpenAppDrawer,
        onItemTap = viewModel::onItemTap,
        onRecentCardTap = viewModel.recents::onRecentCardTap,
        onItemLongPress = viewModel::onItemLongPress,
        onPlatformLongPress = viewModel::onPlatformLongPress,
        onUserInteraction = viewModel::onUserInteraction,
        onBootComplete = viewModel.launching::onBootSequenceComplete,
        onSettingsLongPress = onSettingsLongPress,

        onCloseSettingsScreen = viewModel::onSettingsBack,
        onOpenSettingsScreen = viewModel::onOpenSettingsScreen,
        onOpenCrossbarLayoutAdjust = viewModel::openCrossbarLayoutAdjust,
        onOpenCustomIcons = viewModel::openCustomIcons,
        onPreviewBootSequence = viewModel.launching::previewBootSequence,
        onPreviewGameBoot = viewModel.launching::previewGameBoot,
        onGameBootComplete = viewModel.launching::onGameBootComplete,
        onGameBootHandOff = viewModel.launching::onGameBootHandOff,
        onCloseCustomIcons = viewModel::closeCustomIcons,
        onCustomIconsActionConsumed = viewModel::onCustomIconsActionConsumed,
        onCustomIconsSlotFocused = viewModel::onCustomIconSlotFocused,
        onCustomIconGroupMove = viewModel::onCustomIconGroupMove,
        onCustomIconPicked = viewModel::onIconPicked,
        onCustomResetSlot = viewModel::onResetSlot,
        onCustomResetAll = viewModel::onResetAll,
        onSaveAsThemeRequested = viewModel.look::requestSaveCurrentLookAsTheme,
        onConfirmSaveAsTheme = viewModel.look::confirmSaveCurrentLookAsTheme,
        onDismissSaveAsTheme = viewModel.look::dismissSaveThemeNameDialog,
        onThemeShareConsumed = viewModel.look::onThemeShareConsumed,
        onSettingsActionConsumed = viewModel::consumeSettingsAction,
        onPromptTapped = viewModel::onPromptTapped,
        onPillActivated = viewModel::onPillActivated,
        focusedPillIndex = uiState.focusedPillIndex,
        onCloseAppDrawer = viewModel::onCloseAppDrawer,
        onAddAppToOpenCategory = viewModel::addAppToOpenCategory,
        onLaunchRomFromDrawer = viewModel.launching::launchGameFromDrawer,
        onOpenAppSearch = viewModel.librarySearch::openAppSearch,
        onLetterRailTouch = viewModel::onLetterRailTouch,
        onLetterRailReleased = viewModel::onLetterRailReleased,
        onDrawerActionConsumed = viewModel::consumeDrawerAction,
        onCloseArtworkStudio = viewModel.artworkTools::closeArtworkStudio,
        onArtworkStudioActionConsumed = viewModel.artworkTools::consumeArtworkStudioAction,
        onManualPageCount = viewModel.gameDetail::setManualPageCount,
        onManualPrevPage = viewModel.gameDetail::manualPrevPage,
        onManualNextPage = viewModel.gameDetail::manualNextPage,
        onCloseManual = viewModel.gameDetail::closeManualViewer,
        onMetadataPolicy = viewModel.artworkTools::selectMetadataPolicy,
        onMetadataSource = viewModel.artworkTools::cycleMetadataSource,
        onMetadataField = viewModel.artworkTools::toggleMetadataField,
        onMetadataApply = viewModel.artworkTools::applyMetadataPreview,
        onCloseMetadata = viewModel.artworkTools::closeMetadataPreview,
        onOpenLibraryManager = viewModel::openLibraryManager,
        onGoToLibrary = viewModel::goToLibrary,
        onCloseVideoDetail = viewModel.video::onCloseVideoDetail,
        onVideoDetailActionConsumed = viewModel.video::consumeVideoDetailAction,
        onClosePhotoViewer = viewModel.gallery::onClosePhotoViewer,
        onPhotoViewerActionConsumed = viewModel.gallery::consumePhotoViewerAction,
        onCloseAppDetail = viewModel::onCloseAppDetail,
        onGameInfoCardFocused = viewModel.gameDetail::onGameInfoCursor,
        onGameInfoNoticeTapped = viewModel.gameDetail::onGameInfoNoticeTapped,
        onGameInfoPanelClose = viewModel.gameDetail::closeGameInfoPanel,
        onGameInfoScrollMax = viewModel.gameDetail::onGameInfoScrollMax,
        onPanelProfileTapped = viewModel.panel::onPanelProfileTapped,
        onProfileSet = viewModel.panel::onProfileSetTapped,
        onProfileBadge = viewModel.panel::onProfileBadgeTapped,
        onProfileFilter = viewModel.panel::onProfileFilterTapped,
        onProfileFriend = viewModel.panel::onProfileFriendTapped,
        onProfileEditName = viewModel.panel::editProfileName,
        onProfilePickAvatar = viewModel.panel::pickProfileAvatar,
        onAppDetailActionConsumed = viewModel::consumeAppDetailAction,
        onContextMenuItemActivated = viewModel::onContextMenuItemActivatedAt,
        onContextMenuDismiss = viewModel::closeContextMenu,
        onOpenColorSchemePicker = viewModel.look::openColorSchemePicker,
        onColorSchemeHighlightedAt = viewModel.look::onColorSchemeHighlightedAt,
        onColorSchemeConfirm = viewModel.look::confirmColorSchemePicker,
        onColorSchemeCancel = viewModel.look::cancelColorSchemePicker,
        onCustomColorUpdate = viewModel.look::updateCustomColor,
        onCustomColorChannelMove = viewModel.look::moveCustomColorChannel,
        onCustomColorAdjust = viewModel.look::adjustCustomColor,
        onCustomColorConfirm = viewModel.look::confirmCustomColor,
        onCustomColorCancel = viewModel.look::cancelCustomColor,
        onCrossbarLayoutScale = viewModel::setCrossbarLayoutScale,
        onCrossbarLayoutHorizontal = viewModel::setCrossbarLayoutHorizontal,
        onCrossbarLayoutVertical = viewModel::setCrossbarLayoutVertical,
        onCrossbarLayoutToggleSliders = viewModel::toggleCrossbarLayoutSliders,
        onCrossbarLayoutReset = viewModel::resetCrossbarLayoutAdjust,
        onCrossbarLayoutSave = viewModel::saveCrossbarLayoutAdjust,
        onCrossbarLayoutCancel = viewModel::cancelCrossbarLayoutAdjust,
        onNamePromptTextChanged = viewModel::onNamePromptTextChanged,
        onConfirmAppRename = viewModel::onConfirmAppRename,
        onCancelAppRename = viewModel::onCancelAppRename,
        onConfirmCollectionName = viewModel.gameActions::onConfirmCollectionName,
        onCancelCollectionName = viewModel.gameActions::onCancelCollectionName,
        onConfirmPlaylistName = viewModel.music::onConfirmPlaylistName,
        onCancelPlaylistName = viewModel.music::onCancelPlaylistName,
        onMusicTrackPickerActivatedAt = viewModel.music::onMusicTrackPickerActivatedAt,
        onMusicTrackPickerConfirm = viewModel.music::onMusicTrackPickerConfirm,
        onMusicTrackPickerDismiss = viewModel.music::closeMusicTrackPicker,
        onSearchQueryChange = viewModel.librarySearch::onSearchQueryChange,
        onSearchActivatedAt = viewModel.librarySearch::onSearchActivatedAt,
        onSearchBack = viewModel.librarySearch::closeSearch,
        onSearchFocusedAt = viewModel.librarySearch::onSearchFocusedAt,
        onMusicBrowserQueryChange = viewModel.music::onMusicBrowserQueryChange,
        onMusicBrowserActivatedAt = viewModel.music::onMusicBrowserActivatedAt,
        onMusicBrowserLongPressAt = viewModel.music::onMusicBrowserLongPressAt,
        onMusicBrowserBack = viewModel.music::onMusicBrowserBack,
        onMusicBrowserSortTapped = viewModel.music::onMusicBrowserSortTapped,
        onMusicBrowserOptionsTapped = viewModel.music::onMusicBrowserOptionsTapped,
        onAppPickerTileTapped = viewModel.appPickerSection::onAppPickerTileTapped,
        onAppPickerTouchBrowse = viewModel.appPickerSection::onAppPickerTouchBrowse,
        onAppPickerHeaderBack = viewModel.appPickerSection::onAppPickerHeaderBack,
        onAppPickerSearchToggle = viewModel.appPickerSection::onAppPickerSearchToggle,
        onAppPickerQueryChange = viewModel.appPickerSection::onAppPickerQueryChange,
        onAppPickerSearchDone = viewModel.appPickerSection::onAppPickerSearchDone,
        onAppPickerApply = viewModel.appPickerSection::onAppPickerApply,
        onAppPickerConfirmRemoval = viewModel.appPickerSection::onAppPickerConfirmRemoval,
        onAppPickerCancelRemoval = viewModel.appPickerSection::onAppPickerCancelRemoval,
        onAppPickerColumnsMeasured = viewModel.appPickerSection::onAppPickerColumnsMeasured,
        onAppPickerDismiss = viewModel.appPickerSection::closeAppPicker,
        onGamePickerConfirm = viewModel::confirmGamePicker,
        onGamePickerDismiss = viewModel::closeGamePicker,
        onGamePickerActionConsumed = viewModel::consumeGamePickerAction,
        onDismissInfoDialog = viewModel::dismissInfoDialog,
        onWindowsSetupConfirm = viewModel::confirmWindowsSetupPrompt,
        onWindowsSetupDismiss = viewModel::dismissWindowsSetupPrompt,
        onLaunchRecoveryAction = viewModel.launching::onLaunchRecoveryAction,
        onMusicPlayPause = viewModel.music::musicPlayPause,
        onMusicPrev = viewModel.music::musicPrev,
        onMusicNext = viewModel.music::musicNext,
        onMusicSeekTo = viewModel.music::musicSeekTo,
        onMusicShuffle = viewModel.music::musicToggleShuffle,
        onQuickSettingTapped = viewModel::onQuickSettingTapped,
        onMusicRepeat = viewModel.music::musicCycleRepeat,
        onMusicPlayerBack = viewModel.music::closeMusicPlayer,
        onOpenAndroidLibraryPicker = viewModel::openAndroidLibraryPicker,
    )
    }

    uiState.discCeremony?.let { ceremony ->
        DiscLaunchCeremony(
            art = ceremony.art,
            onHandOff = viewModel.launching::onDiscCeremonyHandOff,
            onFinished = viewModel.launching::onDiscCeremonyFinished,
            modifier = Modifier.fillMaxSize(),
        )
    }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CrossbarShell(
    uiState: CrossbarUiState,
    onCategorySelected: (Int) -> Unit = {},
    onStepCategory: (Int) -> Unit = {},
    onStepItem: (Int) -> Unit = {},
    onTouchBack: () -> Unit = {},
    onTouchInput: () -> Unit = {},
    onCrossbarSortTapped: () -> Unit = {},
    onPanelPageTapped: (DetailPanelPage) -> Unit = {},

    onRecentFilterTapped: (RecentFilter) -> Unit = {},

    onDrawerTypedCharConsumed: () -> Unit = {},
    onNotificationsToggled: () -> Unit = {},
    onLaunchRecentTop: () -> Unit = {},
    onGameInfoSectionPicked: (com.echo.feature.crossbar.viewmodel.GameInfoAction?) -> Unit = {},
    onNoticeChipTapped: (com.echo.feature.crossbar.viewmodel.NoticeChip) -> Unit = {},
    onSortPicked: (com.echo.feature.crossbar.viewmodel.CrossbarSortMode) -> Unit = {},
    onOrbTapped: () -> Unit = {},
    onOrbTransport: (com.echo.feature.crossbar.viewmodel.StageCommand) -> Unit = {},
    onNotificationsDismissed: () -> Unit = {},
    onPanelRowTapped: (com.echo.feature.crossbar.viewmodel.NoticeFocus) -> Unit = {},
    onPanelTabTapped: (com.echo.feature.crossbar.viewmodel.PanelTab) -> Unit = {},
    onNotificationsSwipedOpen: () -> Unit = {},
    onNotificationsSwipedClosed: () -> Unit = {},
    onPanelSettingTapped: (Int) -> Unit = {},
    onOpenAppDrawer: () -> Unit = {},

    onItemTap: (Int) -> Unit = {},
    onRecentCardTap: (Int) -> Unit = {},
    onItemLongPress: (Int) -> Unit = {},
    onPlatformLongPress: (Int) -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onBootComplete: () -> Unit = {},
    onSettingsLongPress: () -> Unit = {},
    onCloseSettingsScreen: () -> Unit = {},
    onOpenSettingsScreen: (String) -> Unit = {},
    onOpenCrossbarLayoutAdjust: () -> Unit = {},
    onOpenCustomIcons: () -> Unit = {},
    onPreviewBootSequence: () -> Unit = {},
    onPreviewGameBoot: () -> Unit = {},
    onGameBootComplete: () -> Unit = {},
    onGameBootHandOff: () -> Unit = {},
    onCloseCustomIcons: () -> Unit = {},
    onCustomIconsActionConsumed: () -> Unit = {},
    onCustomIconsSlotFocused: (Int) -> Unit = {},
    onCustomIconGroupMove: (Int) -> Unit = {},
    onCustomIconPicked: (String, android.net.Uri) -> Unit = { _, _ -> },
    onCustomResetSlot: (String) -> Unit = {},
    onCustomResetAll: () -> Unit = {},
    onSaveAsThemeRequested: () -> Unit = {},
    onConfirmSaveAsTheme: (String) -> Unit = {},
    onDismissSaveAsTheme: () -> Unit = {},
    onThemeShareConsumed: () -> Unit = {},
    onSettingsActionConsumed: () -> Unit = {},

    onPromptTapped: (com.echo.core.domain.model.GamepadAction) -> Unit = {},

    onPillActivated: (String) -> Unit = {},

    focusedPillIndex: Int? = null,
    onCloseAppDrawer: () -> Unit = {},

    onAddAppToOpenCategory: (String) -> Unit = {},
    onLaunchRomFromDrawer: (Long) -> Unit = {},
    onOpenAppSearch: (String) -> Unit = {},

    onLetterRailTouch: (Int) -> Unit = {},
    onLetterRailReleased: () -> Unit = {},
    onDrawerActionConsumed: () -> Unit = {},
    onCloseArtworkStudio: () -> Unit = {},
    onArtworkStudioActionConsumed: () -> Unit = {},
    onManualPageCount: (Int) -> Unit = {},
    onManualPrevPage: () -> Unit = {},
    onManualNextPage: () -> Unit = {},
    onCloseManual: () -> Unit = {},
    onMetadataPolicy: (com.echo.feature.artwork.match.MetadataApplyPolicy) -> Unit = {},
    onMetadataSource: (Int) -> Unit = {},
    onMetadataField: (com.echo.feature.artwork.match.MetadataField) -> Unit = {},
    onMetadataApply: () -> Unit = {},
    onCloseMetadata: () -> Unit = {},
    onOpenLibraryManager: () -> Unit = {},
    onGoToLibrary: () -> Unit = {},
    onCloseVideoDetail: () -> Unit = {},
    onVideoDetailActionConsumed: () -> Unit = {},
    onClosePhotoViewer: () -> Unit = {},
    onPhotoViewerActionConsumed: () -> Unit = {},
    onCloseAppDetail: () -> Unit = {},
    onGameInfoCardFocused: (Int) -> Unit = {},
    onGameInfoNoticeTapped: (String) -> Unit = {},
    onGameInfoPanelClose: () -> Unit = {},
    onGameInfoScrollMax: (Int) -> Unit = {},
    onPanelProfileTapped: (com.echo.feature.crossbar.viewmodel.ProfileSpot, Int) -> Unit = { _, _ -> },
    onProfileSet: (Int) -> Unit = {},
    onProfileBadge: (Int) -> Unit = {},
    onProfileFilter: (com.echo.feature.crossbar.viewmodel.BadgeFilter) -> Unit = {},
    onProfileFriend: (Int) -> Unit = {},
    onProfileEditName: () -> Unit = {},
    onProfilePickAvatar: () -> Unit = {},
    onAppDetailActionConsumed: () -> Unit = {},
    onContextMenuItemActivated: (Int) -> Unit = {},
    onContextMenuDismiss: () -> Unit = {},
    onMusicPlayPause: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicSeekTo: (Int) -> Unit = {},
    onMusicShuffle: () -> Unit = {},
    onQuickSettingTapped: (com.echo.feature.crossbar.viewmodel.QuickSetting, Int) -> Unit = { _, _ -> },
    onMusicRepeat: () -> Unit = {},
    onMusicPlayerBack: () -> Unit = {},
    onOpenAndroidLibraryPicker: () -> Unit = {},
    onOpenColorSchemePicker: () -> Unit = {},
    onColorSchemeHighlightedAt: (Int) -> Unit = {},
    onColorSchemeConfirm: () -> Unit = {},
    onColorSchemeCancel: () -> Unit = {},
    onCustomColorUpdate: (Int, Float) -> Unit = { _, _ -> },
    onCustomColorChannelMove: (Int) -> Unit = {},
    onCustomColorAdjust: (Float) -> Unit = {},
    onCustomColorConfirm: () -> Unit = {},
    onCustomColorCancel: () -> Unit = {},
    onCrossbarLayoutScale: (Float) -> Unit = {},
    onCrossbarLayoutHorizontal: (Float) -> Unit = {},
    onCrossbarLayoutVertical: (Float) -> Unit = {},
    onCrossbarLayoutToggleSliders: () -> Unit = {},
    onCrossbarLayoutReset: () -> Unit = {},
    onCrossbarLayoutSave: () -> Unit = {},
    onCrossbarLayoutCancel: () -> Unit = {},
    onNamePromptTextChanged: (String) -> Unit = {},
    onConfirmAppRename: (String) -> Unit = {},
    onCancelAppRename: () -> Unit = {},
    onConfirmCollectionName: (String) -> Unit = {},
    onCancelCollectionName: () -> Unit = {},
    onConfirmPlaylistName: (String) -> Unit = {},
    onCancelPlaylistName: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActivatedAt: (Int) -> Unit = {},
    onSearchBack: () -> Unit = {},

    onSearchFocusedAt: (Int) -> Unit = {},
    onMusicBrowserQueryChange: (String) -> Unit = {},
    onMusicBrowserActivatedAt: (Int) -> Unit = {},
    onMusicBrowserLongPressAt: (Int) -> Unit = {},
    onMusicBrowserBack: () -> Unit = {},
    onMusicBrowserSortTapped: () -> Unit = {},
    onMusicBrowserOptionsTapped: () -> Unit = {},
    onMusicTrackPickerActivatedAt: (Int) -> Unit = {},
    onMusicTrackPickerConfirm: () -> Unit = {},
    onMusicTrackPickerDismiss: () -> Unit = {},
    onAppPickerTileTapped: (Int) -> Unit = {},
    onAppPickerTouchBrowse: (Int) -> Unit = {},
    onAppPickerHeaderBack: () -> Unit = {},
    onAppPickerSearchToggle: (Boolean) -> Unit = {},
    onAppPickerQueryChange: (String) -> Unit = {},
    onAppPickerSearchDone: () -> Unit = {},
    onAppPickerApply: () -> Unit = {},
    onAppPickerConfirmRemoval: () -> Unit = {},
    onAppPickerCancelRemoval: () -> Unit = {},

    onAppPickerColumnsMeasured: (Int) -> Unit = {},
    onAppPickerDismiss: () -> Unit = {},
    onGamePickerConfirm: (Set<Long>) -> Unit = { _ -> },
    onGamePickerDismiss: () -> Unit = {},
    onGamePickerActionConsumed: () -> Unit = {},
    onDismissInfoDialog: () -> Unit = {},
    onWindowsSetupConfirm: () -> Unit = {},
    onWindowsSetupDismiss: () -> Unit = {},
    onLaunchRecoveryAction: (com.echo.feature.launcher.LaunchRecoveryAction) -> Unit = {},
) {
    val themeWave = uiState.themeColors.waveColor
    val themeAccent = uiState.themeColors.accentColor

    val itemColor = uiState.focusedItemAccentArgb
        ?.takeIf { uiState.itemBackdropEnabled }
        ?.let { Color(it.toInt()) }
    val crossbarWave by androidx.compose.animation.animateColorAsState(
        targetValue = itemColor ?: themeWave,
        animationSpec = tween(durationMillis = 420),
        label = "crossbarItemWave",
    )
    val crossbarGameAccent by androidx.compose.animation.animateColorAsState(
        targetValue = itemColor ?: themeAccent,
        animationSpec = tween(durationMillis = 420),
        label = "crossbarItemAccent",
    )

    val crossbarColors = remember(uiState.themeColors, crossbarWave, crossbarGameAccent) {
        uiState.themeColors.withWaveTint(crossbarWave).copy(accentColor = crossbarGameAccent)
    }
    EchoTheme(colors = crossbarColors) {
      CompositionLocalProvider(
          com.echo.core.ui.icons.LocalCrossbarIconOverrides provides uiState.iconOverrides,

          LocalLiveRowProgress provides uiState.musicPlayback.let { pb ->
              if (pb.track != null && pb.durationMs > 0) {
                  LiveRowProgress(CrossbarViewModel.NOW_PLAYING_ITEM_ID, pb.durationMs.toLong())
              } else null
          },

          com.echo.core.ui.icons.LocalCustomIcons provides uiState.customIcons,

          LocalFocusedGameVideo provides uiState.focusedGameVideo,

          LocalPanelShowingVideo provides (uiState.effectivePanelPage == DetailPanelPage.VIDEO),

          com.echo.core.ui.icons.LocalIconLegibility provides uiState.iconLegibility,
      ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val baseDensity = LocalDensity.current

            val uiScale = minOf(
                maxHeight.value / CROSSBAR_BASELINE_HEIGHT_DP,
                maxWidth.value / CROSSBAR_BASELINE_WIDTH_DP,
            ).coerceIn(CROSSBAR_MIN_SCALE, CROSSBAR_MAX_SCALE)

            val config = LocalConfiguration.current
            val layoutAdjust = uiState.crossbarLayoutAdjust?.draft
                ?: uiState.crossbarLayoutAdjustMap[
                    com.echo.themekit.CrossbarFormFactor.forSmallestWidthDp(config.smallestScreenWidthDp).key
                ]
                ?: com.echo.themekit.CrossbarLayoutAdjust(
                    scale = 1f,
                    barLeftFraction = 0f,
                    barTopFraction = uiState.layoutSpec.barTopFraction,
                )

            CompositionLocalProvider(
                LocalPadPrompts provides padPromptsShown(rememberSystemStatus().controllerConnected, uiState.lastInputWasTouch),
                LocalMenuBackdropArt provides if (uiState.onLastPlayedHome) {
                    uiState.currentItems.getOrNull(uiState.selectedItemIndex)?.backdropArt?.firstOrNull()
                } else {
                    uiState.focusedItemBackdrop?.takeIf { uiState.itemBackdropEnabled }
                },
            ) {
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density * uiScale * layoutAdjust.scale, baseDensity.fontScale),
            ) {
        Box(modifier = Modifier.fillMaxSize().markTouches(onTouchInput)) {
            val waveCovered = !uiState.waveShown

            val powerThrottled = rememberWavePowerThrottle(
                respectBatterySaver  = uiState.respectBatterySaver,
                thermalThrottleAware = uiState.thermalThrottleAware,
            )

            val iconAnimatingAllowed = !powerThrottled && !uiState.hasBlockingOverlay

            val appVisible = rememberAppVisible()
            val motionDecision = MotionWallpaperPolicy.decide(
                MotionWallpaperPolicy.Inputs(
                    hasMotion = uiState.motionWallpaperPath != null,
                    hasPoster = uiState.customWallpaperPath != null,
                    style = uiState.waveStyle,
                    covered = waveCovered,
                    throttled = powerThrottled,
                    appVisible = appVisible,
                )
            )

            val effectiveWaveStyle = when {
                waveCovered -> com.echo.core.ui.wave.WaveStyle.OFF
                powerThrottled -> uiState.waveStyle.frozen
                else -> uiState.waveStyle
            }

            val gameBootWaveStyle = if (powerThrottled) uiState.waveStyle.frozen else uiState.waveStyle
            CrossbarBackground(
                waveStyle           = effectiveWaveStyle,
                customWallpaperPath = uiState.customWallpaperPath,
                waveOverWallpaper   = uiState.waveOverWallpaper,
                wallpaperAccent     = uiState.wallpaperAccent,
                motionWallpaperPath = uiState.motionWallpaperPath,
                motionDecision      = motionDecision,

                waveDrawnByCaller   = true,
                modifier            = Modifier.fillMaxSize(),
            )

            val recentsListState = rememberLazyListState()
            val panelItem = uiState.hoverPanelItem

            val panelContent = uiState.hoverPanelContent

            val panelLogo = panelContent?.logoUri
            val panelPage = panelContent?.let { resolvePanelPage(uiState.effectivePanelPage, it.pages) }
            val panelShowingVideo = panelPage == DetailPanelPage.VIDEO

            val selectedItem = uiState.currentItems.getOrNull(uiState.selectedItemIndex)
            val selectedBg = uiState.focusedItemBackdrop?.takeIf { uiState.itemBackdropEnabled }

            val backdrop: CrossbarBackdrop? = when {
                selectedBg != null -> CrossbarBackdrop.Art(selectedBg)
                uiState.itemBackdropEnabled && selectedItem?.isAndroidApp == true &&
                    selectedItem.packageName != null -> CrossbarBackdrop.AppIcon(selectedItem.packageName)
                else -> null
            }

            val backgroundSnap = uiState.focusedGameVideo?.takeIf {
                snapSiteFor(it.placement, panelShowingVideo) == SnapSite.BACKGROUND &&
                    it.gameId == selectedItem?.gameId
            }

            Crossfade(targetState = backdrop, animationSpec = tween(CROSSBAR_BACKDROP_FADE_MS), label = "crossbarGameBackground") { bg ->
                if (bg != null || backgroundSnap != null) {
                    Box(Modifier.fillMaxSize()) {
                        if (backgroundSnap != null) {
                            Icon1VideoOverlay(
                                videoUri = backgroundSnap.uri,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        when (bg) {
                            is CrossbarBackdrop.Art -> AsyncImage(
                                model = rememberArtworkModel(bg.uri),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .kenBurns(bg.uri, enabled = backgroundSnap == null && !powerThrottled)
                                    .then(if (backgroundSnap != null) Modifier.crossbarStillOverVideo() else Modifier),
                            )

                            is CrossbarBackdrop.AppIcon -> CrossbarAppIconBackdrop(
                                packageName = bg.packageName,
                                fallbackAccent = crossbarGameAccent,
                            )
                            null -> Unit
                        }

                        if (bg !is CrossbarBackdrop.AppIcon) {
                            val scrimBase = androidx.compose.ui.graphics.lerp(
                                Color(0xFF05050C), crossbarGameAccent, 0.22f,
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.horizontalGradient(
                                        0.0f to scrimBase.copy(alpha = 0.65f),
                                        0.5f to scrimBase.copy(alpha = 0.50f),
                                        1.0f to scrimBase.copy(alpha = 0.75f),
                                    )
                                )
                            )
                        }

                        if (backgroundSnap != null) {
                            Box(Modifier.fillMaxSize().background(Color(0x5905050C)))
                        }
                    }
                }
            }

            val launching = uiState.discCeremony != null || uiState.activeGameBoot != null
            val waveSpeed by animateFloatAsState(
                targetValue = when {
                    launching -> 2.1f
                    uiState.idle -> 0.45f
                    else -> 1f
                },
                animationSpec = tween(900),
                label = "crossbarWaveSpeed",
            )

            val waveGlow by animateFloatAsState(
                targetValue = if (launching) 1.7f else 1f,
                animationSpec = tween(if (launching) 260 else 1200),
                label = "crossbarWaveGlow",
            )
            // owner, 2026-10-04: the wave takes the colour of whatever is selected; an installed app has no
            // art file, so its icon gives the colour
            val focusedAppIcon = rememberAppIcon(uiState.focusedItem?.packageName?.takeIf { uiState.focusedItemAccentArgb == null })
            val waveAccent = uiState.focusedItemAccentArgb
                ?: focusedAppIcon?.color?.toArgb()?.toLong()?.and(0xFFFFFFFFL)
                ?: uiState.wallpaperAccent
            if (waveVisible(uiState.customWallpaperPath != null, uiState.waveOverWallpaper, effectiveWaveStyle)) {
                WaveOverlay(
                    waveStyle = effectiveWaveStyle,
                    accentArgb = waveAccent,
                    modifier = Modifier.fillMaxSize(),
                    speedScale = { waveSpeed },
                    glowScale = { waveGlow },
                )
            }
            // Last Played draws its own art over the background, so it draws the wave itself, above the art
            val homeWaveStyle = if (powerThrottled) uiState.waveStyle.frozen else uiState.waveStyle
            val homeWave: (@Composable () -> Unit)? = if (homeWaveStyle.drawsWave) {
                { WaveOverlay(homeWaveStyle, waveAccent, Modifier.fillMaxSize(), speedScale = { waveSpeed }, glowScale = { waveGlow }) }
            } else null

            val chromeFade by animateFloatAsState(
                if (uiState.activeContextMenu != null) 0f else 1f,
                tween(ChromeFadeMs),
                label = "chromeFade",
            )

            val aboveContextRail = when {
                uiState.activeContextMenu != null || uiState.notificationsOpen -> 1f
                !uiState.statusStripVisible -> 0f
                uiState.hasBlockingOverlay -> 1f
                else -> CrossbarChromeZ
            }

            val barCategories = remember(
                uiState.categories, uiState.lastInputWasTouch, uiState.shelfCards,
            ) {
                uiState.categories.filter { category ->
                    uiState.categoryReachable(category) &&
                        (uiState.lastInputWasTouch || category.id != BuiltInCategory.RECENTLY_PLAYED)
                }
            }
            val barSelected = remember(barCategories, uiState.selectedCategoryIndex) {
                uiState.categories.getOrNull(uiState.selectedCategoryIndex)
                    ?.let { current -> barCategories.indexOfFirst { it.id == current.id } }
                    ?.takeIf { it >= 0 }
                    ?: 0
            }
            val onBarCategory: (Int) -> Unit = { barIndex ->
                barCategories.getOrNull(barIndex)?.let { picked ->
                    val real = uiState.categories.indexOfFirst { it.id == picked.id }
                    if (real >= 0) onCategorySelected(real)
                }
            }

            var flash by remember { mutableStateOf<SystemToast?>(null) }
            LaunchedEffect(Unit) {
                SystemToasts.events.collect { toast ->
                    flash = toast
                    delay(if (toast.kind == ToastKind.ERROR) 5_200L else 3_200L)
                    if (flash?.id == toast.id) flash = null
                }
            }
            val notifications by SystemToasts.recent.collectAsState()

            val androidNotices = uiState.androidNotices
            val notificationsOpen = uiState.notificationsOpen

            val strip = LocalContext.current
            val androidAccess = remember(notificationsOpen) { AndroidNotifications.isEnabled(strip) }

            if (uiState.activeAppDrawerFilter == null &&
                uiState.musicBrowser == null &&
                uiState.search == null &&
                uiState.activeSettingsScreen == null &&
                uiState.activeVideoId == null &&
                uiState.activeAppId == null &&
                uiState.activePhotoViewer == null &&

                uiState.customIconSession == null
            ) {
            val onLastPlayedHome = uiState.onLastPlayedHome
            if (onLastPlayedHome) {
                LastPlayedPage(
                    items = uiState.currentItems,
                    selectedIndex = uiState.selectedItemIndex,
                    listState = recentsListState,
                    filter = uiState.recentFilter,
                    railVisible = uiState.recentRailVisible,
                    onCardTapped = onRecentCardTap,
                    wave = homeWave,
                    modifier = Modifier
                        .fillMaxSize()
                        .crossbarNavGestures(
                            onStepCategory = onStepCategory,
                            onStepItem = onStepItem,
                            onEdgeBack = onTouchBack,
                            stepScale = uiState.touchSensitivity.stepScale,
                        ),
                )
            } else {
            var pic0Visible by remember(panelLogo) { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(panelLogo) {
                if (panelLogo != null) {
                    kotlinx.coroutines.delay(650)
                    pic0Visible = true
                }
            }
            val pic0Alpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pic0Visible && panelLogo != null) 1f else 0f,
                animationSpec = if (pic0Visible) tween(500) else androidx.compose.animation.core.snap(),
                label = "pic0Fade",
            )
            val onLogoPage = panelPage == DetailPanelPage.LOGO

            val stripOpened = uiState.panelStripOpen

            val metadataAsSubtitle = uiState.gameMetadataVisible

            val rowLabelHidden = panelContent != null && stripOpened
            val panelAlpha = if (onLogoPage) pic0Alpha else 1f

            if (panelContent != null && panelPage != null && stripOpened &&
                (!onLogoPage || (panelLogo != null && pic0Alpha > 0f))
            ) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    val panelWidthFraction = if (onLogoPage) 0.30f else 0.42f

                    val panelHeightFraction = if (onLogoPage) 0.38f else 0.70f
                    val logoCenterOffset: Dp = if (uiState.drillTitle != null) {
                        val contentTop = uiState.layoutSpec.contentTopPaddingDp.dp
                        val crossHeight = maxHeight - contentTop
                        val anchorTop = crossHeight * layoutAdjust.barTopFraction + CAT_BAR_HEIGHT
                        val rowCenter = contentTop + anchorTop + ROW_HEIGHT / 2

                        val halfPanel = panelHeightFraction / 2f
                        rowCenter.coerceIn(maxHeight * halfPanel, maxHeight * (1f - halfPanel)) -
                            maxHeight / 2
                    } else {
                        0.dp
                    }
                    GameDetailPanel(
                        content = panelContent,
                        page = panelPage,

                        titleFallback = false,
                        modifier = Modifier
                            .fillMaxWidth(panelWidthFraction)
                            .fillMaxHeight(panelHeightFraction)
                            .offset(y = logoCenterOffset)
                            .padding(end = 44.dp)
                            .alpha(panelAlpha),
                    )
                }
            }

            val fanCovers = fanCoversToDraw(
                insideCovers = uiState.currentItems.getOrNull(uiState.selectedItemIndex)?.insideCovers.orEmpty(),
                cardArtGrid = uiState.cardArtGrid,
            )
            if (fanCovers.isNotEmpty()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    CrossbarCoverFan(
                        covers = fanCovers,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = -maxWidth * CrossbarCoverFanPlacement.RightInsetFraction,
                                y = maxHeight * CrossbarCoverFanPlacement.TopFraction,
                            )
                            .size(
                                width = maxWidth * CrossbarCoverFanPlacement.WidthFraction,
                                height = maxHeight * CrossbarCoverFanPlacement.HeightFraction,
                            ),
                    )
                }
            }

            if (panelContent != null && panelPage != null && panelPage != DetailPanelPage.LOGO) {
                DetailPanelStrip(
                    pages = panelContent.pages,
                    current = panelPage,
                    onPageTapped = onPanelPageTapped,
                    modifier = Modifier
                        .align(Alignment.TopEnd)

                        .padding(top = StripHeight + ControllerHintEdgeGap, end = ControllerHintEdgeGap),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()

                    .padding(top = uiState.layoutSpec.contentTopPaddingDp.dp)

                    .crossbarNavGestures(
                        onStepCategory = onStepCategory,
                        onStepItem = onStepItem,
                        onEdgeBack = onTouchBack,
                        stepScale = uiState.touchSensitivity.stepScale,

                        swipeBackEnabled = uiState.isInSubItem,
                        onSwipeBack = onTouchBack,
                    ),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val catBarHeight = CAT_BAR_HEIGHT

                    val barTop = maxHeight * layoutAdjust.barTopFraction

                    val columnBaseInset = CrossbarLeftAnchor + (CategorySlotWidth / 2) - LEADING_ICON_CENTER

                    val hShift = if (uiState.drillTitle != null) {
                        DRILL_CROSSBAR_LEFT_MARGIN - columnBaseInset
                    } else {
                        maxWidth * layoutAdjust.barLeftFraction
                    }

                    val anchorTop = barTop + catBarHeight
                    val startPad = columnBaseInset + hShift

                    if (uiState.drillTitle != null) {
                        CrossbarDrillFlyout(
                            onPillActivated = onPillActivated,
                            focusedPillIndex = focusedPillIndex,
                            siblings = uiState.drillSiblings,
                            siblingIndex = uiState.drillSiblingIndex,
                            items = uiState.currentItems,
                            selectedIndex = uiState.selectedItemIndex,
                            onItemSelected = onItemTap,
                            onItemLongPress = onItemLongPress,

                            onSiblingTap = { i -> if (i == uiState.drillSiblingIndex) onTouchBack() },
                            labelHiddenByPanel = rowLabelHidden,
                            cardArtGrid = uiState.cardArtGrid,
                            metadataAsSubtitle = metadataAsSubtitle,
                            iconStyle = uiState.iconStyle,
                            belowTopY = anchorTop,
                            iconAnimatingAllowed = iconAnimatingAllowed,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxSize()
                                .padding(start = startPad, end = 24.dp),
                        )
                    } else {
                        AnimatedContent(
                            targetState = uiState.selectedCategoryIndex,
                            transitionSpec = {
                                (fadeIn(tween(130)) + slideInVertically(tween(180)) { it / 8 })
                                    .togetherWith(fadeOut(tween(110)) + slideOutVertically(tween(140)) { -it / 10 })
                                    .using(SizeTransform(clip = false))
                            },
                            label = "crossbarCategoryItems",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxSize()
                                .padding(start = startPad, end = 24.dp),
                        ) { categoryIndex ->

                            val itemSelectedIndex =
                                if (categoryIndex == uiState.selectedCategoryIndex) uiState.selectedItemIndex else -1
                            CrossbarItemList(
                                onPillActivated = onPillActivated,
                                focusedPillIndex = focusedPillIndex,
                                pillFade = if (uiState.inColumn) chromeFade else 0f,
                                items = uiState.currentItems,
                                selectedIndex = itemSelectedIndex,
                                onItemSelected = onItemTap,
                                onItemLongPress = onItemLongPress,
                                iconStyle = uiState.iconStyle,
                                belowTopY = anchorTop,
                                fadeByDistance = uiState.fadeByDistance,
                                textShadow = uiState.textShadow,
                                iconAnimatingAllowed = iconAnimatingAllowed,
                                labelHiddenByPanel = rowLabelHidden,
                                cardArtGrid = uiState.cardArtGrid,
                                metadataAsSubtitle = metadataAsSubtitle,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    CompositionLocalProvider(LocalCrossbarHorizontalShift provides hShift) {
                        CrossbarCategoryBar(
                            categories = barCategories,
                            selectedIndex = barSelected,

                            onCategorySelected = onBarCategory,
                            onCategoryLongPress = { index ->
                                val id = barCategories.getOrNull(index)?.id
                                if (id == BuiltInCategory.SETTINGS) onSettingsLongPress()
                            },
                            drilledIn = uiState.drillTitle != null,
                            fadeByDistance = uiState.fadeByDistance,
                            iconAnimatingAllowed = iconAnimatingAllowed,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = barTop)
                                .fillMaxWidth()
                                .height(catBarHeight),
                        )
                    }
                }
            }
            }
            }

            val orbKind = uiState.orbKind()
            val musicActivity = (uiState.mediaStage() as? PanelStage.Music)?.takeIf { orbKind == OrbKind.MUSIC }?.let { music ->
                StripLiveActivity(
                    art = music.art,
                    title = music.title,
                    detail = listOfNotNull(music.artist, music.app).joinToString("  ·  "),
                    stage = music,
                    accentArgb = uiState.musicAccentArgb.takeIf { music.packageName == null },
                )
            }

            val busyActivity = uiState.artworkFetchTitle?.let {
                StripLiveActivity(art = null, title = "Refreshing artwork", detail = it)
            }

            val foregroundActivity = flash?.let {
                StripLiveActivity(art = null, title = it.title, detail = it.message, stage = PanelStage.Launcher(it))
            }
                ?: busyActivity
                ?: musicActivity

            val recentActivity = uiState.recentTop?.takeIf { uiState.interfaceChoices.islandShowsRecent }?.let { top ->
                StripLiveActivity(
                    art = top.shelfCoverArt,
                    title = top.title,
                    // a book adds how far it has been read (owner, 2026-10-04)
                    detail = listOfNotNull(top.subtitle?.takeIf { it.isNotBlank() }, top.progressLabel.takeIf { top.type == CrossbarItemType.LIBRARY_BOOK })
                        .joinToString("  ·  ").ifEmpty { null },
                    stage = uiState.recentStage(), accentArgb = uiState.recentTopAccentArgb)
            }

            val islandIsRecent = foregroundActivity == null && recentActivity != null
            val liveActivity = foregroundActivity ?: recentActivity

            val crossbarContext = uiState.stripShowsCrossbarContext

            val panelPull = rememberPanelPull(notificationsOpen)
            val battery = rememberBatteryReading()
            CompositionLocalProvider(LocalDensity provides baseDensity) {
            CrossbarStatusStrip(
                sortRow = uiState.sortRow()?.takeIf { crossbarContext && !uiState.onLastPlayedHome }
                    ?.let { (modes, active) -> modes.map { it.label } to modes.indexOf(active) },
                onSortPicked = { i -> uiState.sortRow()?.first?.getOrNull(i)?.let(onSortPicked) },
                live = liveActivity.takeIf { !notificationsOpen && uiState.activeSettingsScreen == null },

                onLiveAreaTapped = if (islandIsRecent) onLaunchRecentTop else onNotificationsToggled,

                orbLevel = if (flash == null && busyActivity == null && orbKind != null) uiState.orbLevel else -1,
                onOrbTapped = onOrbTapped,
                onOrbTransport = onOrbTransport,
                holdMs = if (islandIsRecent) holdMsFor(uiState.recentTop) else 0L,
                holding = islandIsRecent && uiState.launchHold == uiState.recentTop?.id,

                noticeCount = uiState.launcherNotices.size + androidNotices.size,
                onNoticeCountTapped = onNotificationsToggled,

                hints = StripHints(
                    shoulder = uiState.panelStripOpen && crossbarContext,

                    leftRight = uiState.pillRowVisible && crossbarContext,
                ),

                // owner, 2026-10-04: the XMB already shows its categories, so the centre carries only that
                // category's own filter (Last Played's, through centre) or its sort

                compact = !crossbarContext,
                battery = battery,

                centre = if (notificationsOpen) {
                    { u, tight ->
                        PanelTabsRow(uiState.panelTab, onPanelTabTapped, u, tight, Modifier.align(Alignment.Center))
                    }
                } else if (uiState.onLastPlayedHome && crossbarContext) {
                    { u, _ ->
                        RecentFilterRow(
                            filter = uiState.recentFilter,
                            u = u,
                            modifier = Modifier.align(Alignment.Center),
                            onFilterTapped = onRecentFilterTapped,
                            includeApps = uiState.recentsIncludeApps,
                        )
                    }
                } else null,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(aboveContextRail).then(
                    if (!notificationsOpen && uiState.activeSettingsScreen == null) {
                        Modifier.panelPullGesture(panelPull, onNotificationsSwipedOpen, onNotificationsSwipedClosed)
                    } else {
                        Modifier
                    },
                ),
            )
            }

            // owner, 2026-10-04: the battery line runs along the bottom edge, not the top
            BatteryLine(
                level = battery.level,
                charging = battery.charging,
                glint = uiState.waveShown,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(aboveContextRail + 1f),
            )

            val panelStage = uiState.panelStage()
            CompositionLocalProvider(LocalBackdropWave provides homeWave) {
                CrossbarNotificationBar(
                    open = notificationsOpen,
                    tab = uiState.panelTab,
                    entries = uiState.noticeEntries,
                    stage = panelStage,
                    focus = uiState.focusedNotice,
                    chip = uiState.noticeChip,
                    allCount = panelEntries(androidNotices, uiState.launcherNotices).size,
                    onChipTapped = onNoticeChipTapped,
                    androidAccessGranted = androidAccess,
                    onGrantAndroidAccess = {
                        onNotificationsDismissed()
                        runCatching {
                            strip.startActivity(
                                AndroidNotifications.settingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    quick = QuickSettingsState(
                        waveOn = uiState.waveStyle != com.echo.core.ui.wave.WaveStyle.OFF,
                        backdropOn = uiState.itemBackdropEnabled,
                        recentAppsOn = uiState.recentsIncludeApps,
                        chips = uiState.libraryChips,
                    ),
                    profile = uiState.profileData,
                    profileName = uiState.profileName,
                    profileAvatar = uiState.profileAvatar,
                    profileFocus = uiState.panelProfile,
                    onProfileTapped = onPanelProfileTapped,
                    quickFocus = uiState.panelQuick,
                    chipFocus = uiState.panelChip,
                    accent = com.echo.core.ui.theme.menuCursorEdge(),
                    onRowTapped = onPanelRowTapped,
                    settingFocus = uiState.panelSetting,
                    onQuickTapped = onQuickSettingTapped,
                    onSettingTapped = onPanelSettingTapped,
                    pull = panelPull,
                    onOpened = onNotificationsSwipedOpen,
                    onClosed = onNotificationsSwipedClosed,
                    modifier = Modifier.zIndex(NotificationBarZ),
                )
            }

            val rootActionsVisible = uiState.stripShowsCrossbarContext && !uiState.isInSubItem

            if (uiState.stripShowsCrossbarContext && chromeFade > 0f) {
                CrossbarLetterRail(
                    letters = remember(uiState.currentItems) {
                        letterAnchors(uiState.currentItems.map { it.title })?.map { it.letter }.orEmpty()
                    },
                    cursor = uiState.letterJump?.cursor,
                    onTouch = onLetterRailTouch,
                    onReleased = onLetterRailReleased,
                    modifier = Modifier
                        .alpha(chromeFade)
                        .zIndex(CrossbarChromeZ),
                )
            }

            AnimatedVisibility(

                visible = uiState.notificationsOpen ||
                    ((uiState.showContextMenuHint || rootActionsVisible) && uiState.stripShowsCrossbarContext),
                enter = fadeIn(tween(200)),
                exit = ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().zIndex(aboveContextRail),
            ) {
                CompositionLocalProvider(LocalDensity provides baseDensity) {
                    val hintItem = uiState.focusedItem?.takeIf { uiState.activeSettingsScreen == null }
                    val hintIcon = rememberAppIcon(hintItem?.packageName)
                    CrossbarHintBar(
                        prompts = promptsFor(uiState),
                        onAction = onPromptTapped,
                        accent = uiState.focusedItemAccentArgb?.takeIf { hintItem != null }?.let(::mediaAccent)
                            ?: hintIcon?.color
                            ?: menuCursorEdge(),
                        leading = hintItem?.let { hintTile(it, hintIcon?.bitmap) },
                        holdMs = holdMsFor(hintItem.takeIf { uiState.focusedPillIndex == null && !uiState.hasBlockingOverlay }),
                        holding = hintItem != null && uiState.launchHold == hintItem.id,
                        resumeHolding = uiState.resumableFocus()?.let { uiState.launchHold == resumeHoldId(it.id) } == true,
                    )
                }
            }

            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, baseDensity.fontScale),
            ) {
            if (uiState.colorSchemePicker == null) {
                CompositionLocalProvider(LocalBackdropWave provides homeWave) {
                                    uiState.activeSettingsScreen?.let { screenId ->
                        SettingsNavHost(
                            screenId = screenId,
                            onBack = onCloseSettingsScreen,
                            pendingGamepadAction = uiState.pendingSettingsAction,
                            onGamepadActionConsumed = onSettingsActionConsumed,
                            onPromptTapped = onPromptTapped,
                            showControllerHint = uiState.showSettingsHint,
                            leftBacksOut = uiState.leftBacksOut,
                            lastInputWasTouch = uiState.lastInputWasTouch,
                            onTouchInteraction = onTouchInput,
                            onOpenColorSchemePicker = onOpenColorSchemePicker,
                            onOpenCrossbarLayoutAdjust = onOpenCrossbarLayoutAdjust,
                            onOpenCustomIcons = onOpenCustomIcons,
                            onPreviewBootSequence = onPreviewBootSequence,
                            onPreviewGameBoot = onPreviewGameBoot,
                            onAddAndroidApps = onOpenAndroidLibraryPicker,
                            onOpenLibraryManager = onOpenLibraryManager,
                            onGoToLibrary = onGoToLibrary,
                            onOpenScreen = onOpenSettingsScreen,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (uiState.showBootSequence) {
                if (uiState.startupPermissionsSettled && uiState.initialSetupDecided) {
                    BootSequenceOverlay(
                        onComplete = onBootComplete,
                        bootVideoPath = uiState.bootVideoPath,
                        bootAudioPath = uiState.bootAudioPath,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                }
            }

            uiState.activeAppDrawerFilter?.let { filterName ->
                val initialFilter = runCatching { AppFilter.valueOf(filterName) }
                    .getOrDefault(AppFilter.DEFAULT)
                CompositionLocalProvider(LocalBackdropWave provides homeWave) {
                    AppDrawerScreen(
                        initialFilter = initialFilter,
                        onBack = onCloseAppDrawer,
                        pendingGamepadAction = uiState.pendingDrawerAction,
                        selectReleases = uiState.drawerSelectReleases,
                        typedChar = uiState.pendingDrawerTypedChar,
                        onTypedCharConsumed = onDrawerTypedCharConsumed,
                        onGamepadActionConsumed = onDrawerActionConsumed,
    
                        letterRailHeld = uiState.drawerLetterRailHeld,
    
                        onTouchInteraction = onTouchInput,
                        onAddToCrossBar = onAddAppToOpenCategory,
                        onLaunchRom = onLaunchRomFromDrawer,
                        onOpenAppSearch = onOpenAppSearch,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            uiState.search?.let { search ->
                SearchScreen(
                    state = search,
                    onQueryChange = onSearchQueryChange,
                    onActivateAt = onSearchActivatedAt,
                    onBack = onSearchBack,

                    onFocusAt = onSearchFocusedAt,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.musicBrowser?.let { browser ->
                MusicBrowserScreen(
                    state = browser,
                    onQueryChange = onMusicBrowserQueryChange,
                    onActivateAt = onMusicBrowserActivatedAt,
                    onLongPressAt = onMusicBrowserLongPressAt,
                    onBack = onMusicBrowserBack,
                    onSortTapped = onMusicBrowserSortTapped,
                    onOptionsTapped = onMusicBrowserOptionsTapped,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (uiState.musicPlayerVisible) {
                MusicPlayerScreen(
                    state = uiState.musicPlayback,
                    onPlayPause = onMusicPlayPause,
                    onPrev = onMusicPrev,
                    onNext = onMusicNext,
                    onSeekTo = onMusicSeekTo,
                    onShuffle = onMusicShuffle,
                    onRepeat = onMusicRepeat,
                    accentArgb = uiState.musicAccentArgb,
                    onBack = onMusicPlayerBack,
                    onAction = onPromptTapped,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.gameInfo?.let { info ->
                GameInfoScreen(
                    info = info,
                    androidNotices = uiState.androidNotices,
                    onAction = onPromptTapped,
                    onCardFocused = onGameInfoCardFocused,
                    onNoticeTapped = onGameInfoNoticeTapped,
                    launchHold = uiState.launchHold,
                    onSectionPicked = onGameInfoSectionPicked,
                    modifier = Modifier.fillMaxSize(),
                    onClosePanel = onGameInfoPanelClose,
                    onScrollMax = onGameInfoScrollMax,
                )
            }

            uiState.profile?.let { profile ->
                ProfileScreen(
                    profile = profile,
                    name = uiState.profileName,
                    avatar = uiState.profileAvatar,
                    onAction = onPromptTapped,
                    onSet = onProfileSet,
                    onBadge = onProfileBadge,
                    onFilter = onProfileFilter,
                    onFriend = onProfileFriend,
                    onEditName = onProfileEditName,
                    onPickAvatar = onProfilePickAvatar,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.activeContextMenu?.let { menu ->

                CompositionLocalProvider(LocalBackdropWave provides homeWave) {
                    EchoContextMenuOverlay(
                        state = uiState.menuWithPills() ?: menu.state,
                        onRowActivated = onContextMenuItemActivated,
                        onDismiss = onContextMenuDismiss,
                    )
                }
            }

            uiState.colorSchemePicker?.let { picker ->
                ColorSchemePickerOverlay(
                    state = picker,
                    onHighlightedAt = onColorSchemeHighlightedAt,
                    onConfirm = onColorSchemeConfirm,
                    onDismiss = onColorSchemeCancel,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.customColorPicker?.let { picker ->
                CustomColorPickerOverlay(
                    state = picker,
                    onChannelFraction = onCustomColorUpdate,
                    onConfirm = onCustomColorConfirm,
                    onCancel = onCustomColorCancel,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.crossbarLayoutAdjust?.let { session ->
                CrossbarLayoutAdjustOverlay(
                    draft = session.draft,
                    slidersVisible = session.slidersVisible,
                    onScale = onCrossbarLayoutScale,
                    onHorizontal = onCrossbarLayoutHorizontal,
                    onVertical = onCrossbarLayoutVertical,
                    onToggleSliders = onCrossbarLayoutToggleSliders,
                    onReset = onCrossbarLayoutReset,
                    onSave = onCrossbarLayoutSave,
                    onCancel = onCrossbarLayoutCancel,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.customIconSession?.let { session ->
                CustomIconsOverlay(
                    session = session,
                    customIcons = uiState.customIcons,
                    themeIcons = uiState.iconOverrides,
                    onSlotFocused = onCustomIconsSlotFocused,
                    onIconPicked = onCustomIconPicked,
                    onResetSlot = onCustomResetSlot,
                    onResetAll = onCustomResetAll,
                    onSaveAsTheme = onSaveAsThemeRequested,
                    onGroupMove = onCustomIconGroupMove,
                    onSlotMove = onCustomIconsSlotFocused,
                    onDone = onCloseCustomIcons,
                    forwardedAction = uiState.pendingCustomIconsAction,
                    onActionConsumed = onCustomIconsActionConsumed,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.saveThemeNameDialog?.let { dialog ->
                CollectionNameDialog(
                    title = dialog.title,
                    text = dialog.text,
                    onTextChange = onNamePromptTextChanged,
                    onConfirm = onConfirmSaveAsTheme,
                    onCancel = onDismissSaveAsTheme,
                )
            }

            uiState.renameAppTarget?.let {
                AppRenameDialog(
                    text = uiState.renameAppText,
                    onTextChange = onNamePromptTextChanged,
                    onConfirm = onConfirmAppRename,
                    onCancel = onCancelAppRename,
                )
            }

            uiState.collectionNameDialog?.let { dialog ->
                CollectionNameDialog(
                    title = dialog.title,
                    text = dialog.text,
                    onTextChange = onNamePromptTextChanged,
                    onConfirm = onConfirmCollectionName,
                    onCancel = onCancelCollectionName,
                    placeholder = dialog.placeholder,
                    confirmLabel = dialog.confirmLabel,
                    subtitle = dialog.subtitle,
                    resetLabel = dialog.resetLabel,
                    onReset = dialog.resetLabel?.let { { onNamePromptTextChanged("") } },
                )
            }

            uiState.playlistNameDialog?.let { dialog ->
                CollectionNameDialog(
                    title = dialog.title,
                    text = dialog.text,
                    onTextChange = onNamePromptTextChanged,
                    onConfirm = onConfirmPlaylistName,
                    onCancel = onCancelPlaylistName,
                )
            }

            uiState.infoDialog?.let { dialog ->
                InfoDialog(
                    title = dialog.title,
                    message = dialog.message,
                    onDismiss = onDismissInfoDialog,
                )
            }

            if (uiState.showWindowsSetupPrompt) {
                EchoConfirmOverlay(
                    title = "Finish your Windows Library",
                    message = "A PC game was added, but the Windows Games library has no folder " +
                        "yet. Set it up in Library Manager so game folders can be scanned.",
                    confirmLabel = "Set Up",
                    cancelLabel = "Later",
                    confirmFocused = true,
                    cancelFocused = false,
                    confirmFill = null,
                    onConfirm = onWindowsSetupConfirm,
                    onCancel = onWindowsSetupDismiss,
                )
            }

            uiState.launchRecovery?.let { recovery ->
                LaunchRecoverySheet(
                    recovery = recovery,
                    cursor = uiState.launchRecoveryCursor,
                    onAction = onLaunchRecoveryAction,
                )
            }

            uiState.musicTrackPicker?.let { picker ->
                MusicTrackPicker(
                    state = picker,
                    onActivateAt = onMusicTrackPickerActivatedAt,
                    onConfirm = onMusicTrackPickerConfirm,
                    onDismiss = onMusicTrackPickerDismiss,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.appPicker?.let { picker ->
                com.echo.feature.crossbar.ui.apppicker.AppPickerScreen(
                    state = picker,
                    onTileTapped = onAppPickerTileTapped,
                    onTouchBrowse = onAppPickerTouchBrowse,
                    onHeaderBack = onAppPickerHeaderBack,
                    onSearchToggle = onAppPickerSearchToggle,
                    onSearchChange = onAppPickerQueryChange,
                    onSearchDone = onAppPickerSearchDone,
                    onApply = onAppPickerApply,
                    onConfirmRemoval = onAppPickerConfirmRemoval,
                    onCancelRemoval = onAppPickerCancelRemoval,

                    onColumnsMeasured = onAppPickerColumnsMeasured,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.gamePickerCategoryId?.let {
                GamePickerScreen(
                    onConfirm = onGamePickerConfirm,
                    onCancel = onGamePickerDismiss,
                    pendingGamepadAction = uiState.pendingGamePickerAction,
                    onGamepadActionConsumed = onGamePickerActionConsumed,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // the detail screens' menus show the wave behind their backing, like the crossbar's
            CompositionLocalProvider(LocalBackdropWave provides homeWave) {
            uiState.artworkStudioGameId?.let { gameId ->
                ArtworkStudioScreen(
                    gameId = gameId,
                    onClose = onCloseArtworkStudio,
                    pendingGamepadAction = uiState.pendingArtworkStudioAction,
                    onGamepadActionConsumed = onArtworkStudioActionConsumed,
                    showTouchControls = uiState.resolvedShowTouchButton,
                    onTouchInput = onTouchInput,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.manualViewer?.let { manual ->
                ManualViewerOverlay(
                    source = manual.uri,
                    title = manual.title,
                    page = manual.page,
                    scrollSteps = manual.scrollSteps,
                    onPageCount = onManualPageCount,
                    onPrevPage = onManualPrevPage,
                    onNextPage = onManualNextPage,
                    onClose = onCloseManual,
                )
            }

            uiState.metadataPreview?.let { preview ->
                MetadataPreviewPanel(
                    ui = preview,
                    focusFill = menuCursorFill(),
                    focusEdge = menuCursorEdge(),
                    onSelectPolicy = onMetadataPolicy,
                    onCycleSource = onMetadataSource,
                    onToggleField = onMetadataField,
                    onApply = onMetadataApply,
                    onClose = onCloseMetadata,
                )
            }

            uiState.activeAppId?.let { appId ->
                AppDetailScreen(
                    gameId = appId,
                    onBack = onCloseAppDetail,
                    pendingGamepadAction = uiState.pendingAppDetailAction,
                    onGamepadActionConsumed = onAppDetailActionConsumed,
                    showTouchControls = uiState.resolvedShowTouchButton,
                    onTouchInput = onTouchInput,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.activeVideoId?.let { videoId ->
                VideoDetailScreen(
                    videoId = videoId,
                    onBack = onCloseVideoDetail,
                    autoPlay = uiState.activeVideoAutoPlay,
                    pendingGamepadAction = uiState.pendingVideoDetailAction,
                    onGamepadActionConsumed = onVideoDetailActionConsumed,
                    onTouchInput = onTouchInput,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.activePhotoViewer?.let { request ->
                PhotoViewerScreen(
                    photoId = request.photoId,
                    libraryId = request.libraryId,
                    openWallpaperPreview = request.openWallpaperPreview,
                    favoritesOnly = request.favoritesOnly,
                    onBack = onClosePhotoViewer,
                    pendingGamepadAction = uiState.pendingPhotoViewerAction,
                    onGamepadActionConsumed = onPhotoViewerActionConsumed,
                    onTouchInput = onTouchInput,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            }

            uiState.activeGameBoot?.let { request ->
                if (request.videoPath == null) {
                    DiscLaunchCeremony(
                        art = request.coverArt,
                        onHandOff = onGameBootHandOff,
                        onFinished = onGameBootComplete,
                        modifier = Modifier.fillMaxSize(),
                    )
                    return@let
                }
                GameBootOverlay(
                    gameTitle = request.gameTitle,
                    onComplete = onGameBootComplete,
                    videoPath = request.videoPath,
                    audioPath = request.audioPath,

                    waveStyle = gameBootWaveStyle,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            }
        }
            }
            }
        }
      }
    }
}

@Composable
private fun AppRenameDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    EchoTextPromptOverlay(
        title = "Rename Shortcut",
        value = text,
        placeholder = "Shortcut name",
        onValueChange = onTextChange,
        onConfirm = { onConfirm(text) },
        onCancel = onCancel,
    )
}

@Composable
private fun CollectionNameDialog(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    placeholder: String = "e.g. RPGs, Currently Playing",
    confirmLabel: String = "Save",
    subtitle: String? = null,
    resetLabel: String? = null,
    onReset: (() -> Unit)? = null,
) {
    EchoTextPromptOverlay(
        title = title,
        value = text,
        placeholder = placeholder,
        onValueChange = onTextChange,
        onConfirm = { onConfirm(text) },
        onCancel = onCancel,
        subtitle = subtitle,
        resetLabel = resetLabel,
        onReset = onReset,
        confirmLabel = confirmLabel,
    )
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    EchoMessageOverlay(title = title, message = message, onDismiss = onDismiss)
}

@Composable
private fun LaunchRecoverySheet(
    recovery: com.echo.feature.launcher.LaunchRecoveryRequest,
    cursor: Int,
    onAction: (com.echo.feature.launcher.LaunchRecoveryAction) -> Unit,
) {
    val actions = com.echo.feature.launcher.launchRecoveryActions(recovery)

    val bodyColor = com.echo.core.ui.theme.LocalEchoTextColors.current.secondary
    EchoOverlayCard(onScrimTap = { onAction(com.echo.feature.launcher.LaunchRecoveryAction.DISMISS) }) {
        EchoOverlayTitle("Couldn't launch ${recovery.gameTitle}")
        Spacer(Modifier.height(10.dp))
        Text(recovery.message, color = bodyColor, fontSize = 14.sp)
        recovery.historyLine?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = bodyColor.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEachIndexed { index, (action, label) ->
                EchoDetailLaunchButton(
                    label = label,
                    icon = null,
                    focused = index == cursor.coerceIn(0, actions.lastIndex),
                    onClick = { onAction(action) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@DevicePreviews
@Composable
private fun PreviewCrossbarDefault() {
    EchoPreview {
        CrossbarShell(uiState = PreviewData.defaultState)
    }
}

@OptIn(UnstableApi::class)
@DevicePreviews
@Composable
private fun PreviewCrossbarEmpty() {
    EchoPreview {
        CrossbarShell(uiState = PreviewData.emptyLibraryState)
    }
}

@OptIn(UnstableApi::class)
@DevicePreviews
@Composable
private fun PreviewCrossbarBoot() {
    EchoPreview {
        CrossbarShell(uiState = PreviewData.bootState)
    }
}

@OptIn(UnstableApi::class)
@Preview(name = "XMB - Red Theme", widthDp = 960, heightDp = 540)
@Composable
private fun PreviewCrossbarRedTheme() {
    val redColors = DefaultEchoColors.copy(
        backgroundTop = Color(0xFF8B0000),
        backgroundBottom = Color(0xFFB22222),
        waveColor = Color(0xFFFF4500)
    )
    EchoPreview(colors = redColors) {
        CrossbarShell(uiState = PreviewData.defaultState)
    }
}

private const val NotificationBarZ = 0.5f

private const val CrossbarChromeZ = 0.6f

private const val ChromeFadeMs = 160

private fun Modifier.markTouches(onTouch: () -> Unit): Modifier = pointerInput(onTouch) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) onTouch()
        }
    }
}

private fun hintTile(item: CrossbarItem, icon: ImageBitmap?): (@Composable () -> Unit)? {
    val art = item.coverUri ?: item.iconUri
    return when {
        art != null -> { ->
            AsyncImage(rememberArtworkModel(art), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        icon != null -> { -> androidx.compose.foundation.Image(icon, null, Modifier.fillMaxSize()) }
        else -> null
    }
}
