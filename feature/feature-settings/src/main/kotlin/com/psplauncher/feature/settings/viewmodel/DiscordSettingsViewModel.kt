package com.psplauncher.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psplauncher.core.data.discord.DiscordAuthRepository
import com.psplauncher.core.data.discord.DiscordPresenceController
import com.psplauncher.core.data.discord.DiscordSocialRepository
import com.psplauncher.core.domain.discord.DeviceLoginState
import com.psplauncher.core.domain.discord.DiscordUser
import com.psplauncher.core.ui.sound.MenuSound
import com.psplauncher.core.ui.sound.MenuSoundPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscordSettingsUiState(
    val signedIn: Boolean = false,
    val user: DiscordUser? = null,
    val shareActivity: Boolean = false,
    val genericActivity: Boolean = false,
    val login: DeviceLoginState? = null,
)

@HiltViewModel
class DiscordSettingsViewModel @Inject constructor(
    private val authRepository: DiscordAuthRepository,
    private val presence: DiscordPresenceController,
    social: DiscordSocialRepository,
    private val menuSound: MenuSoundPlayer,
) : ViewModel() {
    private val login = MutableStateFlow<DeviceLoginState?>(null)
    private var loginJob: Job? = null

    val uiState: StateFlow<DiscordSettingsUiState> = combine(
        social.signedIn,
        social.user,
        presence.observeShareEnabled(),
        presence.observeGenericMode(),
        login,
    ) { signedIn, user, share, generic, login ->
        DiscordSettingsUiState(signedIn, user, share, generic, login)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscordSettingsUiState())

    fun startSignIn() {
        loginJob?.cancel()
        login.value = DeviceLoginState.Requesting
        loginJob = viewModelScope.launch {
            authRepository.loginWithDeviceQr().collect { state ->
                login.value = state
                when (state) {
                    is DeviceLoginState.Success -> {
                        menuSound.play(MenuSound.CONFIRM)
                        presence.refresh()
                        login.value = null
                    }
                    DeviceLoginState.Expired, DeviceLoginState.Denied, is DeviceLoginState.Error ->
                        menuSound.play(MenuSound.ERROR)
                    else -> Unit
                }
            }
        }
    }

    fun cancelSignIn() {
        loginJob?.cancel()
        loginJob = null
        login.value = null
    }

    fun signOut() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun setShareActivity(on: Boolean) {
        viewModelScope.launch { presence.setShareEnabled(on) }
    }

    fun setGenericActivity(on: Boolean) {
        viewModelScope.launch { presence.setGenericMode(on) }
    }

    override fun onCleared() {
        loginJob?.cancel()
    }
}
