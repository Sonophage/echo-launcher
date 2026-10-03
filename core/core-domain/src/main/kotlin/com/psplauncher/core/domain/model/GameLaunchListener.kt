package com.psplauncher.core.domain.model

fun interface GameLaunchListener {
    suspend fun onGameLaunched(game: Game)
}
