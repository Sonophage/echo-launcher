package com.psplauncher.launcher.discord

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.psplauncher.core.data.discord.DiscordAuthRepository
import com.psplauncher.core.data.discord.DiscordPresenceController
import com.psplauncher.discord.DiscordNativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordBootstrap @Inject constructor(
    private val authRepository: DiscordAuthRepository,
    private val presence: DiscordPresenceController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onCreate(activity: ComponentActivity) {
        runCatching { DiscordNativeBridge.attachActivity(activity) }
            .onFailure { Timber.w(it, "Discord SDK could not attach"); return }
        activity.lifecycleScope.launch {
            if (authRepository.hasSession()) {
                authRepository.restoreSession()
                presence.refresh()
            }
        }
    }

    fun onResume() {
        scope.launch { presence.clearCurrentGame() }
    }
}
