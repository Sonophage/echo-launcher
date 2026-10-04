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
            val loaded = withContext(Dispatchers.IO) { if (info.isApp) vm.loadAppInfo(info) else loadGameInfo(info) }
            uiState.update { s -> if (s.gameInfo?.item?.id == item.id) s.copy(gameInfo = loaded.copy(cursor = s.gameInfo.cursor, band = s.gameInfo.band, open = s.gameInfo.open)) else s }
        }
    }

    private suspend fun loadGameInfo(info: GameInfoState): GameInfoState {
        val gid = info.item.gameId ?: return info
        val game = runCatching { vm.gameRepository.getById(gid) }.getOrNull() ?: return info
        val platform = com.echo.core.domain.model.platformLabel(game.platformId, runCatching { vm.platformDao.getById(game.platformId)?.name }.getOrNull())
        val media = (vm.artworkStore.findAll(gid, ArtworkKind.SCREENSHOT) + listOfNotNull(vm.artworkStore.find(gid, ArtworkKind.TITLESCREEN)))
            .map { com.echo.feature.crossbar.ui.detail.DetailMedia(it, isVideo = false) }
        val video = if (vm.videoSnapsAllowed()) vm.artworkStore.find(gid, ArtworkKind.ICON1) ?: vm.artworkStore.find(gid, ArtworkKind.VIDEO) else null
        val set = runCatching { vm.achievementController.observeSetForGame(gid).first() }.getOrNull()
        return info.copy(
            content = detailPanelContentFor(game, platform, media, video),
            achievementsStat = set?.takeIf { it.total > 0 }?.let { "${it.unlocked}/${it.total}" },
            videoUri = vm.artworkStore.find(gid, ArtworkKind.VIDEO) ?: vm.artworkStore.find(gid, ArtworkKind.ICON1),
            manualPath = vm.artworkStore.find(gid, ArtworkKind.MANUAL),
        )
    }

    internal fun handleGameInfoInput(info: GameInfoState, notices: List<com.echo.core.ui.notification.AndroidNotice>, action: GamepadAction) {
        if (info.open != null) {
            when (action) {
                GamepadAction.BACK -> closeGameInfoPanel()
                GamepadAction.NAVIGATE_UP -> scrollGameInfo(-1)
                GamepadAction.NAVIGATE_DOWN -> scrollGameInfo(+1)
                else -> Unit
            }
            return
        }
        when (action) {
            GamepadAction.BACK -> closeGameInfo()
            GamepadAction.SELECT -> {
                val notice = info.cursor?.let { info.notices(notices).getOrNull(it) }
                when {
                    notice != null -> vm.panel.openAndroidNotice(notice.key)
                    info.cursor == null -> onGameInfoAction(info.band)
                    else -> playFromGameInfo(info.item)
                }
            }
            GamepadAction.OPEN_CONTEXT_MENU -> openGameInfoOptions(info)
            GamepadAction.CHANGE_SORT -> if (info.achievementsStat != null) vm.panel.openProfile(ProfileTab.ACHIEVEMENTS, info.item.gameId)
            GamepadAction.NAVIGATE_LEFT,
            GamepadAction.NAVIGATE_RIGHT -> if (info.cursor == null) {
                onGameInfoBand(stepGameInfoBand(info.band, gameInfoActions(info), if (action == GamepadAction.NAVIGATE_LEFT) -1 else +1))
            } else {
                onGameInfoCursor(stepGameInfoCursor(info.cursor, info.cardCount(notices), action))
            }
            GamepadAction.NAVIGATE_UP,
            GamepadAction.NAVIGATE_DOWN -> onGameInfoCursor(stepGameInfoCursor(info.cursor, info.cardCount(notices), action))
            else -> Unit
        }
    }

    fun onGameInfoBand(action: GameInfoAction) {
        val info = uiState.value.gameInfo ?: return
        if (action == info.band && info.cursor == null) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(band = action, cursor = null)) }
    }

    fun onGameInfoAction(action: GameInfoAction) {
        val info = uiState.value.gameInfo ?: return
        uiState.update { it.copy(gameInfo = it.gameInfo?.copy(band = action, cursor = null)) }
        when (action) {
            GameInfoAction.PLAY -> playFromGameInfo(info.item)
            GameInfoAction.OPTIONS -> openGameInfoOptions(info)
            GameInfoAction.MANUAL -> info.item.gameId?.let {
                menuSound.play(MenuSound.SELECT)
                openManualFor(it)
            }
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
