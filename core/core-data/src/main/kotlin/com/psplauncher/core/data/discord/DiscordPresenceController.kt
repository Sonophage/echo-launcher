package com.psplauncher.core.data.discord

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.psplauncher.core.data.datastore.pfpDataStore
import com.psplauncher.core.data.network.NetworkMonitor
import com.psplauncher.core.domain.discord.DiscordSanitize
import com.psplauncher.core.domain.discord.DiscordSessionActivator
import com.psplauncher.core.domain.model.Game
import com.psplauncher.core.domain.model.GameLaunchListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordPresenceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionActivator: DiscordSessionActivator,
    private val networkMonitor: NetworkMonitor,
) : GameLaunchListener {
    private val shareKey = booleanPreferencesKey("discord_share_activity")
    private val genericKey = booleanPreferencesKey("discord_generic_activity")

    @Volatile private var currentGame: String? = null

    fun observeShareEnabled(): Flow<Boolean> = context.pfpDataStore.data.map { it[shareKey] ?: false }
    fun observeGenericMode(): Flow<Boolean> = context.pfpDataStore.data.map { it[genericKey] ?: false }

    suspend fun isShareEnabled(): Boolean = context.pfpDataStore.data.first()[shareKey] ?: false
    suspend fun isGenericMode(): Boolean = context.pfpDataStore.data.first()[genericKey] ?: false

    suspend fun setShareEnabled(enabled: Boolean) {
        context.pfpDataStore.edit { it[shareKey] = enabled }
        refresh()
    }

    suspend fun setGenericMode(enabled: Boolean) {
        context.pfpDataStore.edit { it[genericKey] = enabled }
        refresh()
    }

    override suspend fun onGameLaunched(game: Game) = setCurrentGame(game.displayTitle)

    suspend fun setCurrentGame(title: String?) {
        val clean = title?.let { DiscordSanitize.text(it, DiscordSanitize.ACTIVITY_MAX) }?.takeIf { it.isNotBlank() }
        if (clean == currentGame) return
        currentGame = clean
        refresh()
    }

    suspend fun clearCurrentGame() = setCurrentGame(null)

    suspend fun refresh() {
        if (!networkMonitor.isOnline()) return
        if (isShareEnabled()) {
            val name = when {
                isGenericMode() -> GENERIC_NAME
                else -> currentGame ?: APP_NAME
            }
            sessionActivator.setActivity(name, null)
        } else {
            sessionActivator.clearActivity()
        }
    }

    private companion object {
        const val APP_NAME = "PSPLauncher"
        const val GENERIC_NAME = "a game"
    }
}
