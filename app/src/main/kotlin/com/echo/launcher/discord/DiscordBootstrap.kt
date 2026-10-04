package com.echo.launcher.discord

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.echo.core.data.discord.DiscordAuthRepository
import com.echo.core.data.discord.DiscordPresenceController
import com.echo.discord.DiscordNativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordBootstrap @Inject constructor(
    private val authRepository: DiscordAuthRepository,
    private val presence: DiscordPresenceController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onCreate(activity: ComponentActivity) {
        DiscordNativeBridge.attachActivity(activity)
        activity.lifecycleScope.launch {
            if (authRepository.hasSession()) {
                authRepository.restoreSession()
                presence.refresh()
            }
        }
    }

    fun onResume() {
        scope.launch { presence.clearCurrentGame() }
        scope.launch { authRepository.refreshIfExpiring() }
    }
}
