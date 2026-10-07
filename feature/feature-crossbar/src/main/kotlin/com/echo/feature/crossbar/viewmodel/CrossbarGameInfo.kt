package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.notification.AndroidNotice
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import com.echo.core.ui.sound.MenuSound
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.crossbar.ui.detail.ManualViewerUi
import com.echo.feature.crossbar.ui.detail.detailPanelContentFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrossbarGameInfo(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    fun onOpenGameInfo(item: CrossbarItem) {
        menuSound.play(MenuSound.SELECT)
        val info = GameInfoState(item)
        uiState.update { it.withGameInfoOpen(info) }
        scope.launch {
            val loaded = load(info)
            // it opens on its first view (achievements, when the game has them) unless one was picked meanwhile
            uiState.update { s ->
                val shown = s.gameInfo ?: return@update s
                if (shown.item.id != item.id) return@update s
                val untouched = shown.open == null && shown.cursor == null
                s.copy(gameInfo = loaded.copy(cursor = shown.cursor, open = if (untouched) loaded.firstSection() else shown.open))
            }
        }
    }

    // also what the bottom screen shows, so both screens describe an item the same way
    internal suspend fun load(info: GameInfoState): GameInfoState =
        withContext(Dispatchers.IO) { if (info.isApp) vm.loadAppInfo(info) else loadGameInfo(info) }

    private suspend fun loadGameInfo(info: GameInfoState): GameInfoState {
        val gid = info.item.gameId ?: return info
        val game = runCatching { vm.gameRepository.getById(gid) }.getOrNull() ?: return info
        val platform = com.echo.core.domain.model.platformLabel(game.platformId, runCatching { vm.platformDao.getById(game.platformId)?.name }.getOrNull())
        val media = (vm.artworkStore.findAll(gid, ArtworkKind.SCREENSHOT) + listOfNotNull(vm.artworkStore.find(gid, ArtworkKind.TITLESCREEN)))
            .map { com.echo.feature.crossbar.ui.detail.DetailMedia(it, isVideo = false) }
        val video = if (vm.videoSnapsAllowed()) vm.artworkStore.find(gid, ArtworkKind.ICON1) ?: vm.artworkStore.find(gid, ArtworkKind.VIDEO) else null
        val set = runCatching { vm.achievementController.observeSetForGame(gid).first() }.getOrNull()
        val achievements = set?.let { runCatching { vm.achievementController.observeAchievements(it.provider, it.providerGameId).first() }.getOrNull() }
        return info.copy(
            content = detailPanelContentFor(game, platform, media, video),
            achievementSet = set,
            achievements = achievements.orEmpty(),
            achievementsStat = set?.takeIf { it.total > 0 }?.let { "${it.unlocked}/${it.total}" },
            videoUri = vm.artworkStore.find(gid, ArtworkKind.VIDEO) ?: vm.artworkStore.find(gid, ArtworkKind.ICON1),
            manualPath = vm.artworkStore.find(gid, ArtworkKind.MANUAL),
        )
    }

    // LT/RT walk the screenshots, the info sheet and the video; a tap on the section row jumps there
    fun openGameInfoSection(section: GameInfoAction?) {
        val info = uiState.value.gameInfo ?: return
        if (info.open == section) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(open = section, infoScroll = 0, cursor = null)) }
    }

    internal fun handleGameInfoInput(info: GameInfoState, notices: List<com.echo.core.ui.notification.AndroidNotice>, action: GamepadAction) {
        if (!info.isApp && (action == GamepadAction.PREV_CATEGORY || action == GamepadAction.NEXT_CATEGORY)) {
            openGameInfoSection(stepGameInfoSection(info, if (action == GamepadAction.NEXT_CATEGORY) 1 else -1))
            return
        }
        // owner, 2026-10-06: the views sit under the band, so the screen's buttons stay what they are (A plays,
        // X opens the achievements, B leaves); only the video covers the screen, and B ends it
        when (info.open) {
            GameInfoAction.VIDEO -> {
                if (action == GamepadAction.BACK) closeGameInfoPanel()
                return
            }
            GameInfoAction.INFO -> if (action == GamepadAction.NAVIGATE_UP || action == GamepadAction.NAVIGATE_DOWN) {
                scrollGameInfo(if (action == GamepadAction.NAVIGATE_UP) -1 else +1)
                return
            }
            GameInfoAction.MANUAL -> if (action == GamepadAction.SELECT) {
                onGameInfoAction(GameInfoAction.MANUAL)
                return
            }
            // owner, 2026-10-06: the d-pad hovers the badges, X filters them, A on a badge opens the wall
            GameInfoAction.ACHIEVEMENTS -> when (action) {
                GamepadAction.CHANGE_SORT -> {
                    setAchievementFilter(BadgeFilter.entries[(info.achievementFilter.ordinal + 1) % BadgeFilter.entries.size])
                    return
                }
                in GAME_INFO_CURSOR_MOVES -> {
                    onGameInfoCursor(stepGameInfoCursor(info.cursor, info.badgesInView.size, action))
                    return
                }
                GamepadAction.SELECT -> if (info.cursor != null) {
                    vm.panel.openProfile(ProfileTab.ACHIEVEMENTS, info.item.gameId)
                    return
                }
                else -> Unit
            }
            else -> Unit
        }
        // the screenshot cursor belongs to the screenshots view
        if (info.open != null && action in GAME_INFO_CURSOR_MOVES) return
        when (action) {
            GamepadAction.BACK -> closeGameInfo()
            GamepadAction.SELECT -> {
                val notice = info.cursor?.let { info.notices(notices).getOrNull(it) }
                when {
                    notice != null -> vm.panel.openAndroidNotice(notice.key)
                    info.cursor == null -> {
                        if (!vm.holdToLaunch(info.item) { onGameInfoAction(GameInfoAction.PLAY) }) onGameInfoAction(GameInfoAction.PLAY)
                    }
                    else -> if (!vm.holdToLaunch(info.item) { playFromGameInfo(info.item) }) playFromGameInfo(info.item)
                }
            }
            GamepadAction.OPEN_CONTEXT_MENU -> openGameInfoOptions(info)
            GamepadAction.CHANGE_SORT -> if (info.achievementsStat != null) vm.panel.openProfile(ProfileTab.ACHIEVEMENTS, info.item.gameId)
            GamepadAction.NAVIGATE_LEFT,
            GamepadAction.NAVIGATE_RIGHT -> if (info.cursor != null) onGameInfoCursor(stepGameInfoCursor(info.cursor, info.cardCount(notices), action))
            GamepadAction.NAVIGATE_UP,
            GamepadAction.NAVIGATE_DOWN -> onGameInfoCursor(stepGameInfoCursor(info.cursor, info.cardCount(notices), action))
            else -> Unit
        }
    }

    fun setAchievementFilter(filter: BadgeFilter) {
        val info = uiState.value.gameInfo ?: return
        if (info.achievementFilter == filter) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(achievementFilter = filter, cursor = info.cursor?.let { 0 })) }
    }

    private val GAME_INFO_CURSOR_MOVES = setOf(
        GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN, GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT,
    )

    fun onGameInfoAction(action: GameInfoAction) {
        val info = uiState.value.gameInfo ?: return
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(cursor = null)) }
        when (action) {
            GameInfoAction.PLAY -> playFromGameInfo(info.item)
            GameInfoAction.OPTIONS -> openGameInfoOptions(info)
            GameInfoAction.MANUAL -> info.item.gameId?.let {
                menuSound.play(MenuSound.SELECT)
                openManualFor(it)
            }
            GameInfoAction.ACHIEVEMENTS -> vm.panel.openProfile(ProfileTab.ACHIEVEMENTS, info.item.gameId)
            GameInfoAction.INFO, GameInfoAction.VIDEO -> {
                menuSound.play(MenuSound.SELECT)
                uiState.update { it.copy(gameInfo = it.gameInfo?.copy(open = action, infoScroll = 0)) }
            }
        }
    }

    fun closeGameInfoPanel() {
        if (uiState.value.gameInfo?.open == null) return
        menuSound.play(MenuSound.BACK)
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(open = null)) }
    }

    private fun scrollGameInfo(delta: Int) = uiState.update { s ->
        val info = s.gameInfo?.takeIf { it.open == GameInfoAction.INFO } ?: return@update s
        s.copy(gameInfo = info.scrolledBy(delta))
    }

    fun onGameInfoScrollMax(max: Int) = uiState.update { s ->
        s.gameInfo?.let { s.copy(gameInfo = it.copy(infoScrollMax = max)) } ?: s
    }

    fun onGameInfoCursor(cursor: Int?) {
        val info = uiState.value.gameInfo ?: return
        if (cursor == info.cursor) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(cursor = cursor)) }
    }

    fun onGameInfoNoticeTapped(key: String) = vm.panel.openAndroidNotice(key)

    fun closeGameInfo() {
        if (uiState.value.gameInfo == null) return
        menuSound.play(MenuSound.BACK)
        uiState.update { it.copy(gameInfo = null) }
    }

    private fun openGameInfoOptions(info: GameInfoState) {
        if (info.isApp) vm.openAppContextMenu(info.item) else vm.gameActions.openGameContextMenu(info.item)
    }

    private fun playFromGameInfo(item: CrossbarItem) {
        uiState.update { it.copy(gameInfo = null) }
        val index = uiState.value.currentItems.indexOfFirst { it.id == item.id }
        when {
            index >= 0 -> vm.onItemSelected(index)
            item.gameId != null -> vm.launching.launchGameDirectly(item.gameId)
        }
    }

    internal fun openManualFor(gameId: Long) {
        vm.closeContextMenu()
        scope.launch {
            val game = vm.gameRepository.getById(gameId) ?: return@launch
            val path = vm.artworkStore.find(gameId, ArtworkKind.MANUAL)
            if (path == null) {
                SystemToasts.post("No manual available for this game", null, ToastKind.ERROR)
                return@launch
            }
            uiState.update {
                it.copy(manualViewer = ManualViewerUi(uri = path, title = game.displayTitle))
            }
        }
    }

    fun closeManualViewer() {
        if (uiState.value.manualViewer == null) return
        menuSound.play(MenuSound.BACK)
        uiState.update { it.copy(manualViewer = null) }
    }

    fun setManualPageCount(count: Int) = uiState.update { s ->
        val m = s.manualViewer ?: return@update s
        s.copy(manualViewer = m.copy(pageCount = count, page = m.page.coerceIn(0, (count - 1).coerceAtLeast(0))))
    }

    fun manualPrevPage() = uiState.update { s ->
        val m = s.manualViewer ?: return@update s
        s.copy(manualViewer = m.copy(page = (m.page - 1).coerceAtLeast(0), scrollSteps = 0))
    }

    fun manualNextPage() = uiState.update { s ->
        val m = s.manualViewer ?: return@update s
        s.copy(manualViewer = m.copy(
            page = (m.page + 1).coerceAtMost((m.pageCount - 1).coerceAtLeast(0)),
            scrollSteps = 0,
        ))
    }

    private fun scrollManual(delta: Int) = uiState.update { s ->
        val m = s.manualViewer ?: return@update s
        s.copy(manualViewer = m.copy(scrollSteps = (m.scrollSteps + delta).coerceIn(0, vm.MANUAL_MAX_SCROLL_STEPS_)))
    }

    internal fun handleManualViewerInput(action: GamepadAction) {
        when (action) {
            GamepadAction.NAVIGATE_LEFT  -> manualPrevPage()
            GamepadAction.NAVIGATE_RIGHT -> manualNextPage()
            GamepadAction.NAVIGATE_DOWN  -> scrollManual(+1)
            GamepadAction.NAVIGATE_UP    -> scrollManual(-1)
            GamepadAction.BACK           -> closeManualViewer()
            else -> Unit
        }
    }
}
