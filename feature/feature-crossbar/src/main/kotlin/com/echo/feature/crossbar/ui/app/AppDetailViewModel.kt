package com.echo.feature.crossbar.ui.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.repository.GameRepository
import com.echo.feature.artwork.api.SgdbArtType
import com.echo.feature.artwork.api.SteamGridDbApi
import com.echo.feature.artwork.api.SgdbApiKeyProvider
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.artwork.store.ArtworkStore
import com.echo.feature.crossbar.ui.detail.ArtPickerItem
import com.echo.feature.crossbar.ui.detail.ArtworkType
import com.echo.feature.crossbar.ui.detail.displayLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.echo.core.ui.components.moved
import com.echo.core.ui.components.chose
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuGroup

enum class AppDetailOption(
    val label: String,
    val group: MenuGroup = MenuGroup.MAIN,
    val isDestructive: Boolean = false,
) {
    CHANGE_NAME("Change Display Name", MenuGroup.SETTINGS),
    CHANGE_ICON("Change Game Icon", MenuGroup.SETTINGS),
    CHANGE_BACKGROUND("Change Background", MenuGroup.SETTINGS),
    RESET_ARTWORK("Reset All Artwork", MenuGroup.REMOVE, isDestructive = true),
    ;

    companion object {
        val OPTIONS_MENU = listOf(CHANGE_NAME)

        val ARTWORK_MENU = listOf(CHANGE_ICON, CHANGE_BACKGROUND, RESET_ARTWORK)

        fun menu(title: String, rows: List<AppDetailOption>, selectedIndex: Int) = MenuState(
            title = title,
            rows = rows.map { MenuRow(it, it.label, it.group, isDestructive = it.isDestructive) },
            selectedIndex = selectedIndex,
        )
    }
}

data class AppDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = true,

    val mainFocus: Int = 0,

    val showOptions: Boolean = false,
    val showArtworkMenu: Boolean = false,
    val optionsIndex: Int = 0,

    val showArtworkPicker: Boolean = false,
    val artworkPickerType: ArtworkType = ArtworkType.ICON,
    val artworkPickerLoading: Boolean = false,
    val artworkPickerItems: List<ArtPickerItem> = emptyList(),
    val artworkPickerFocus: Int = 0,
    val artworkPickerError: String? = null,
    val artworkIsProcessing: Boolean = false,
    val artworkMessage: String? = null,
    val artworkPendingLocal: ArtworkType? = null,
    val isEditingName: Boolean = false,
    val nameText: String = "",
    val closed: Boolean = false,
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val steamGridDb: SteamGridDbApi,
    private val sgdbKeyProvider: SgdbApiKeyProvider,
    private val artworkStore: ArtworkStore,
    private val appCategoryRepository: com.echo.feature.appbar.AppCategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun prepareForOpen() {
        _uiState.value = AppDetailUiState()
    }

    fun loadApp(gameId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val game = gameRepository.getById(gameId)
            _uiState.update { it.copy(game = game, isLoading = false) }
        }
    }

    fun openArtworkPickerFor(type: ArtworkType) {
        _uiState.update {
            it.copy(
                showArtworkPicker    = true,
                artworkPickerType    = type,
                artworkPickerItems   = emptyList(),
                artworkPickerFocus   = 0,
                artworkPickerError   = null,
                artworkPickerLoading = false,
                artworkMessage       = null,
            )
        }
        loadSgdbForType(type)
    }

    private fun loadSgdbForType(type: ArtworkType) {
        val game = _uiState.value.game ?: return
        val searchName = game.displayTitle
        viewModelScope.launch {
            _uiState.update { it.copy(artworkPickerLoading = true, artworkPickerError = null) }
            val sgdbKey = sgdbKeyProvider.getKey()
            if (sgdbKey.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        artworkPickerLoading = false,
                        artworkPickerError   = "SteamGridDB API key not configured — add one in Artwork Settings",
                    )
                }
                return@launch
            }
            val match = steamGridDb.searchGame(searchName).getOrElse { e ->
                Timber.w(e, "SGDB search failed for '$searchName'")
                _uiState.update {
                    it.copy(artworkPickerLoading = false, artworkPickerError = "Search failed: ${e.message}")
                }
                return@launch
            }.firstOrNull()

            if (match == null) {
                _uiState.update {
                    it.copy(
                        artworkPickerLoading = false,
                        artworkPickerError   = "No results for \"$searchName\" on SteamGridDB",
                    )
                }
                return@launch
            }

            val arts = steamGridDb.getArt(match.id, type.toSgdbArtType()).getOrElse { e ->
                Timber.w(e, "SGDB getArt failed for ${type.displayLabel}")
                _uiState.update {
                    it.copy(artworkPickerLoading = false, artworkPickerError = "Failed to load artwork: ${e.message}")
                }
                return@launch
            }

            val items = arts.map { ArtPickerItem(url = it.url, thumbUrl = it.thumb) }
            if (items.isEmpty()) {
                _uiState.update {
                    it.copy(
                        artworkPickerLoading = false,
                        artworkPickerError   = "No ${type.displayLabel} art found on SteamGridDB",
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(artworkPickerLoading = false, artworkPickerItems = items, artworkPickerFocus = 0)
            }
        }
    }

    fun closeArtworkPicker() {
        _uiState.update { it.copy(showArtworkPicker = false, artworkPickerItems = emptyList()) }
    }

    fun onSgdbArtSelected(url: String) {
        val game = _uiState.value.game ?: return
        val type = _uiState.value.artworkPickerType
        viewModelScope.launch {
            _uiState.update { it.copy(artworkIsProcessing = true, artworkPickerItems = emptyList()) }
            val localPath = artworkStore.saveVersionedFromUrl(game.id, type.toKind(), url)
            if (localPath == null) {
                _uiState.update {
                    it.copy(artworkIsProcessing = false, artworkMessage = "Could not download ${type.displayLabel}")
                }
                return@launch
            }
            saveArtwork(game.id, type, localPath)
            val updated = gameRepository.getById(game.id)
            _uiState.update {
                it.copy(
                    game               = updated ?: it.game,
                    artworkIsProcessing = false,
                    artworkMessage      = "${type.displayLabel} updated",
                    showArtworkPicker   = false,
                )
            }
        }
    }

    fun requestLocalFilePick(type: ArtworkType) {
        _uiState.update { it.copy(artworkPendingLocal = type) }
    }

    fun consumeLocalFilePick() {
        _uiState.update { it.copy(artworkPendingLocal = null) }
    }

    fun onLocalFilePicked(uri: Uri, type: ArtworkType) {
        val game = _uiState.value.game ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(artworkIsProcessing = true) }
            val localPath = artworkStore.saveVersionedFromUri(game.id, type.toKind(), uri)
            if (localPath == null) {
                _uiState.update {
                    it.copy(artworkIsProcessing = false, artworkMessage = "Could not import ${type.displayLabel}")
                }
                return@launch
            }
            saveArtwork(game.id, type, localPath)
            val updated = gameRepository.getById(game.id)
            _uiState.update {
                it.copy(
                    game               = updated ?: it.game,
                    artworkIsProcessing = false,
                    artworkMessage      = "${type.displayLabel} updated",
                    showArtworkPicker   = false,
                )
            }
        }
    }

    fun clearArtwork(type: ArtworkType) {
        val gameId = _uiState.value.game?.id ?: return
        viewModelScope.launch {
            saveArtwork(gameId, type, null)
            val updated = gameRepository.getById(gameId)
            _uiState.update {
                it.copy(
                    game              = updated ?: it.game,
                    artworkMessage    = "${type.displayLabel} reset",
                    showArtworkPicker = false,
                )
            }
        }
    }

    fun clearAllArtwork() {
        val gameId = _uiState.value.game?.id ?: return
        viewModelScope.launch {
            gameRepository.updateIconArt(gameId, null)
            gameRepository.updateBoxArt(gameId, null)
            val updated = gameRepository.getById(gameId)
            _uiState.update {
                it.copy(game = updated ?: it.game, artworkMessage = "All artwork reset")
            }
        }
    }

    fun startEditingName() {
        val current = _uiState.value.game?.displayTitle ?: ""
        _uiState.update { it.copy(isEditingName = true, nameText = current) }
    }

    fun onNameTextChanged(text: String) {
        _uiState.update { it.copy(nameText = text) }
    }

    fun confirmNameEdit() {
        val gameId = _uiState.value.game?.id ?: return
        val newName = _uiState.value.nameText.trim()
        viewModelScope.launch {
            gameRepository.updateUserTitleOverride(gameId, newName.ifBlank { null })
            val updated = gameRepository.getById(gameId)
            _uiState.update { it.copy(game = updated ?: it.game, isEditingName = false) }
        }
    }

    fun resetNameToDefault() {
        val gameId = _uiState.value.game?.id ?: return
        viewModelScope.launch {
            gameRepository.updateUserTitleOverride(gameId, null)
            val updated = gameRepository.getById(gameId)
            _uiState.update { it.copy(game = updated ?: it.game, isEditingName = false) }
        }
    }

    fun cancelNameEdit() {
        _uiState.update { it.copy(isEditingName = false) }
    }

    fun handleGamepadAction(action: GamepadAction) {
        val s = _uiState.value
        if (s.isEditingName) {
            if (action == GamepadAction.BACK) cancelNameEdit()
            return
        }
        if (s.showArtworkPicker) {
            handlePickerGamepad(action)
            return
        }
        if (s.showOptions || s.showArtworkMenu) {
            handleMenuGamepad(action, if (s.showOptions) AppDetailOption.OPTIONS_MENU else AppDetailOption.ARTWORK_MENU)
            return
        }
        handleMainGamepad(action)
    }

    private fun handleMainGamepad(action: GamepadAction) {
        when (action) {
            GamepadAction.NAVIGATE_LEFT  -> _uiState.update { it.copy(mainFocus = (it.mainFocus - 1).coerceIn(0, MAIN_FOCUS_LAST), artworkMessage = null) }
            GamepadAction.NAVIGATE_RIGHT -> _uiState.update { it.copy(mainFocus = (it.mainFocus + 1).coerceIn(0, MAIN_FOCUS_LAST), artworkMessage = null) }
            GamepadAction.NAVIGATE_UP    -> _uiState.update { it.copy(mainFocus = 0, artworkMessage = null) }
            GamepadAction.NAVIGATE_DOWN  -> _uiState.update { if (it.mainFocus == 0) it.copy(mainFocus = 1, artworkMessage = null) else it }
            GamepadAction.SELECT         -> when (_uiState.value.mainFocus) {
                0    -> launchApp()
                1    -> openOptions()
                else -> openArtworkMenu()
            }

            GamepadAction.OPEN_CONTEXT_MENU -> openOptions()
            GamepadAction.BACK -> close()
            else -> Unit
        }
    }

    private fun handleMenuGamepad(action: GamepadAction, rows: List<AppDetailOption>) {
        val menu = AppDetailOption.menu("", rows, _uiState.value.optionsIndex)
        when (action) {
            GamepadAction.NAVIGATE_UP   -> _uiState.update { it.copy(optionsIndex = menu.moved(-1).selectedIndex ?: 0) }
            GamepadAction.NAVIGATE_DOWN -> _uiState.update { it.copy(optionsIndex = menu.moved(+1).selectedIndex ?: 0) }
            GamepadAction.SELECT        -> onMenuRowActivated(rows, _uiState.value.optionsIndex)
            GamepadAction.BACK          -> closeMenus()
            else -> Unit
        }
    }

    fun onMenuRowActivated(rows: List<AppDetailOption>, index: Int) {
        val chosen = AppDetailOption.menu("", rows, index).chose(index)
        if (chosen is MenuSelect.Run) activateOption(chosen.action)
    }

    fun launchApp() {
        val game = _uiState.value.game ?: return
        val pkg = game.packageName
        if (pkg.isNullOrBlank()) {
            _uiState.update { it.copy(artworkMessage = "This app has no launchable package") }
            return
        }
        appCategoryRepository.launch(pkg)
    }

    fun openOptions() = _uiState.update {
        it.copy(showOptions = true, showArtworkMenu = false, optionsIndex = 0, artworkMessage = null)
    }

    fun openArtworkMenu() = _uiState.update {
        it.copy(showArtworkMenu = true, showOptions = false, optionsIndex = 0, artworkMessage = null)
    }

    fun closeMenus() = _uiState.update { it.copy(showOptions = false, showArtworkMenu = false) }

    fun activateOption(option: AppDetailOption) {
        closeMenus()
        when (option) {
            AppDetailOption.CHANGE_NAME       -> startEditingName()
            AppDetailOption.CHANGE_ICON       -> openArtworkPickerFor(ArtworkType.ICON)
            AppDetailOption.CHANGE_BACKGROUND -> openArtworkPickerFor(ArtworkType.BACKGROUND)
            AppDetailOption.RESET_ARTWORK     -> clearAllArtwork()
        }
    }

    private fun handlePickerGamepad(action: GamepadAction) {
        val items = _uiState.value.artworkPickerItems
        when (action) {
            GamepadAction.NAVIGATE_LEFT -> _uiState.update {
                it.copy(artworkPickerFocus = (it.artworkPickerFocus - 1).coerceAtLeast(0))
            }
            GamepadAction.NAVIGATE_RIGHT -> _uiState.update {
                it.copy(artworkPickerFocus = (it.artworkPickerFocus + 1).coerceAtMost((items.size - 1).coerceAtLeast(0)))
            }
            GamepadAction.SELECT -> {
                val item = items.getOrNull(_uiState.value.artworkPickerFocus) ?: return
                onSgdbArtSelected(item.url)
            }
            GamepadAction.BACK -> closeArtworkPicker()
            else -> Unit
        }
    }

    fun close() {
        _uiState.update { it.copy(closed = true) }
    }

    private companion object {
        const val MAIN_FOCUS_LAST = 2
    }

    private suspend fun saveArtwork(gameId: Long, type: ArtworkType, path: String?) {
        when (type) {
            ArtworkType.ICON       -> gameRepository.updateIconArt(gameId, path)
            ArtworkType.BACKGROUND -> gameRepository.updateBoxArt(gameId, path)
        }
    }

    private fun ArtworkType.toSgdbArtType() = when (this) {
        ArtworkType.ICON       -> SgdbArtType.GRID
        ArtworkType.BACKGROUND -> SgdbArtType.HERO
    }

    private fun ArtworkType.toKind() = when (this) {
        ArtworkType.ICON       -> ArtworkKind.ICON
        ArtworkType.BACKGROUND -> ArtworkKind.BACKGROUND
    }
}
