package com.echo.core.domain.model

fun interface GameLaunchListener {
    suspend fun onGameLaunched(game: Game)
}
