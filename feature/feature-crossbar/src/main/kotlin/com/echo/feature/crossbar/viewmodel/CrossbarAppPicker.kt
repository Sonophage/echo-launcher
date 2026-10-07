package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.move
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarAppPicker(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun openAppPicker(target: AppPickerTarget, title: String) {
        scope.launch {
            val installed = vm.appCategoryRepository.allInstalledApps()

            val entries = installed.map {
                AppPickerEntry(packageName = it.packageName, label = it.label)
            }
            val membership: Set<String> = when (target) {
                is AppPickerTarget.AndroidGames ->
                    vm.gameRepository.observeByPlatform(target.platformId).first()
                        .mapNotNull { it.packageName }
                        .toSet()
                is AppPickerTarget.CategoryShortcuts ->
                    vm.appCategoryRepository.packagesIn(target.categoryId)
            }
            uiState.update {
                it.copy(appPicker = AppPickerState(
                    title           = title,
                    target          = target,
                    apps            = entries,
                    selected        = membership,
                    initialSelected = membership,
                ))
            }
        }
    }

    fun onAppPickerColumnsMeasured(columns: Int) {
        if (columns <= 0) return
        uiState.update {
            val picker = it.appPicker ?: return@update it
            if (picker.columns == columns) it else it.copy(appPicker = picker.copy(columns = columns))
        }
    }

    fun onAppPickerTileTapped(index: Int) {
        vm.markTouchInput()

        if (uiState.value.appPicker?.confirmingRemovals == true) return
        uiState.update {
            val picker = it.appPicker ?: return@update it
            val visible = picker.visibleApps()
            val app = visible.getOrNull(index) ?: return@update it
            it.copy(appPicker = picker.copy(focusedIndex = index, usingTouch = true)
                .toggle(app.packageName))
        }
    }

    fun onAppPickerTouchBrowse(index: Int) {
        vm.markTouchInput()
        if (uiState.value.appPicker?.confirmingRemovals == true) return
        uiState.update {
            val picker = it.appPicker ?: return@update it
            val lastIndex = (picker.visibleApps().size - 1).coerceAtLeast(0)
            it.copy(appPicker = picker.copy(
                focusedIndex = index.coerceIn(0, lastIndex),
                usingTouch = true,
            ))
        }
    }

    fun onAppPickerHeaderBack() {
        vm.markTouchInput()
        handleAppPickerBack()
    }

    fun onAppPickerConfirmRemoval() {
        vm.markTouchInput()
        commitAppPicker()
    }

    fun onAppPickerCancelRemoval() {
        vm.markTouchInput()
        vm.cancelConfirm()
    }

    fun onAppPickerApply() {
        vm.markTouchInput()
        requestApplyAppPicker()
    }

    fun onAppPickerSearchToggle(active: Boolean) {
        vm.markTouchInput()
        uiState.update {
            val picker = it.appPicker ?: return@update it
            it.copy(appPicker = (if (active) picker.copy(searchActive = true) else closeAppPickerSearch(picker)).clampFocus())
        }
    }

    fun onAppPickerQueryChange(query: String) {
        uiState.update {
            val picker = it.appPicker ?: return@update it

            it.copy(appPicker = picker.copy(query = query).clampFocus())
        }
    }

    internal fun closeAppPickerSearch(picker: AppPickerState): AppPickerState =
        picker.copy(searchActive = false, query = "")

    fun onAppPickerSearchDone() {
    }

    internal fun moveAppPicker(action: GamepadAction) {
        uiState.update { state ->
            val picker = state.appPicker ?: return@update state

            state.copy(appPicker = if (picker.confirmingRemovals) picker.moveConfirm(action) else picker.move(action))
        }
    }

    internal fun toggleFocusedApp() {
        uiState.update {
            val picker = it.appPicker ?: return@update it
            val app = picker.visibleApps().getOrNull(picker.focusedIndex) ?: return@update it
            it.copy(appPicker = picker.toggle(app.packageName))
        }
    }

    fun closeAppPicker() {
        uiState.update { it.copy(appPicker = null) }
    }

    internal fun requestApplyAppPicker() {
        val picker = uiState.value.appPicker ?: return
        val adds = picker.pendingAdds()
        val removals = picker.pendingRemovals()
        if (adds.isEmpty() && removals.isEmpty()) {
            closeAppPicker()
            return
        }
        if (removals.isNotEmpty() && !picker.confirmingRemovals) {
            uiState.update { state ->
                state.copy(appPicker = state.appPicker?.openConfirm())
            }
            return
        }
        commitAppPicker()
    }

    internal fun commitAppPicker() {
        val picker = uiState.value.appPicker ?: return
        val adds = picker.pendingAdds()
        val removals = picker.pendingRemovals()
        if (adds.isEmpty() && removals.isEmpty()) {
            closeAppPicker()
            return
        }

        menuSound.play(MenuSound.CONFIRM)
        val target = picker.target
        closeAppPicker()

        scope.launch {
            when (target) {
                is AppPickerTarget.AndroidGames -> {
                    if (adds.isNotEmpty()) vm.gameActions.importAndroidGames(target.platformId, adds)
                    if (removals.isNotEmpty()) vm.gameActions.removeAndroidGames(target.platformId, removals)

                    vm.memoryCardRepository.recountGames(target.platformId)
                }
                is AppPickerTarget.CategoryShortcuts -> {
                    adds.forEach { pkg -> vm.appCategoryRepository.addToCategory(pkg, target.categoryId) }
                    removals.forEach { pkg -> vm.appCategoryRepository.removeFromCategory(pkg, target.categoryId) }
                }
            }
        }
    }

    internal fun handleAppPickerBack() {
        val picker = uiState.value.appPicker ?: return
        when {
            picker.searchActive -> uiState.update { state ->
                state.copy(appPicker = state.appPicker?.let(::closeAppPickerSearch)?.clampFocus())
            }
            picker.confirmingRemovals -> vm.cancelConfirm()
            else -> closeAppPicker()
        }
    }

    internal fun onButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP,
            GamepadAction.NAVIGATE_DOWN,
            GamepadAction.NAVIGATE_LEFT,
            GamepadAction.NAVIGATE_RIGHT -> moveAppPicker(action)

            GamepadAction.SELECT -> {
                val picker = state.appPicker ?: return
                if (picker.confirmingRemovals) {
                    if (picker.confirmFocusedOption == AppPickerState.CONFIRM_REMOVE) commitAppPicker()
                    else vm.cancelConfirm()
                } else toggleFocusedApp()
            }

            GamepadAction.HOME, GamepadAction.OPEN_ISLAND -> requestApplyAppPicker()
            GamepadAction.CHANGE_SORT -> uiState.update { s ->
                s.copy(appPicker = s.appPicker?.let { p ->
                    (if (p.searchActive) closeAppPickerSearch(p) else p.copy(searchActive = true)).clampFocus()
                })
            }

            GamepadAction.BACK,
            GamepadAction.OPEN_CONTEXT_MENU -> handleAppPickerBack()
            else -> Unit
        }
    }
}
