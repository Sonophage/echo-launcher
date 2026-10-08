package com.echo.launcher.debug

import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.entity.toEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSeeder @Inject constructor(
    private val gameDao: GameDao,
    private val debugController: DebugController,
) {
    suspend fun reseed(scenario: DebugScenario) {
        Timber.i("Debug reseed: clearing games, seeding scenario=${scenario.name}")

        val games = DebugGameFactory.gamesForScenario(scenario)
        gameDao.insertAll(games.map { it.toEntity() })

        debugController.setScenario(scenario)

        Timber.i("Debug reseed complete: ${games.size} games for scenario ${scenario.name}")
    }
}
