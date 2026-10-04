package com.echo.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.repository.ControllerLayoutRepository
import com.echo.core.data.repository.ControllerMappingRepository
import com.echo.core.domain.model.ConfirmBackLayout
import com.echo.core.domain.model.ControllerDisplayType
import com.echo.core.domain.model.ControllerLayoutPrefs
import com.echo.core.domain.model.ScrollSpeed
import com.echo.core.domain.model.XYLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ControllerSettingsUiState(
    val layoutPrefs: ControllerLayoutPrefs = ControllerLayoutPrefs(),
)

@HiltViewModel
class ControllerSettingsViewModel @Inject constructor(
    private val mappingRepository: ControllerMappingRepository,
    private val layoutRepository: ControllerLayoutRepository,
) : ViewModel() {

    val uiState: StateFlow<ControllerSettingsUiState> = layoutRepository.prefs
        .map { layoutPrefs ->
            ControllerSettingsUiState(layoutPrefs = layoutPrefs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ControllerSettingsUiState())

    fun setConfirmBackLayout(layout: ConfirmBackLayout) {
        viewModelScope.launch { layoutRepository.setConfirmBackLayout(layout) }
    }

    fun setXYLayout(layout: XYLayout) {
        viewModelScope.launch { layoutRepository.setXYLayout(layout) }
    }

    fun setStickSensitivity(value: com.echo.core.domain.model.StickSensitivity) {
        viewModelScope.launch { layoutRepository.setStickSensitivity(value) }
    }

    fun setTriggerSensitivity(value: com.echo.core.domain.model.TriggerSensitivity) {
        viewModelScope.launch { layoutRepository.setTriggerSensitivity(value) }
    }

    fun setShoulderHoldTime(value: com.echo.core.domain.model.ShoulderHoldTime) {
        viewModelScope.launch { layoutRepository.setShoulderHoldTime(value) }
    }

    fun setScrollSpeed(speed: ScrollSpeed) {
        viewModelScope.launch { layoutRepository.setScrollSpeed(speed) }
    }

    fun setLeftBacksOut(enabled: Boolean) {
        viewModelScope.launch { layoutRepository.setLeftBacksOut(enabled) }
    }

    fun setDisplayType(type: ControllerDisplayType) {
        viewModelScope.launch { layoutRepository.setDisplayType(type) }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            mappingRepository.resetToDefaults()
            layoutRepository.resetAllPrefs()
        }
    }
}
