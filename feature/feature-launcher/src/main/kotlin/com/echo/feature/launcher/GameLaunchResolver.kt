package com.echo.feature.launcher

import com.echo.core.data.database.dao.PlatformDao
import com.echo.core.data.database.entity.PlatformEntity
import com.echo.core.data.repository.MemoryCardRepository
import com.echo.core.domain.model.Game
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameLaunchResolver @Inject constructor(
    private val profileRepository: EmulatorProfileRepository,
    private val memoryCardRepository: MemoryCardRepository,
    private val platformDao: PlatformDao,
) {
    suspend fun resolve(game: Game, platform: PlatformEntity? = null): Result<ResolvedLaunch> {
        val platformId = game.platformId
        return EmulatorLaunchResolver.resolve(
            platformId           = platformId,
            installedProfiles    = profileRepository.getInstalledProfiles(),

            platformProfiles     = profileRepository.getProfilesForPlatform(platformId),
            perGameOverride      = game.emulatorPackage?.takeIf { it.isNotBlank() },
            memoryCardEmulatorId = memoryCardRepository.getById(platformId)?.emulatorId?.takeIf { it.isNotBlank() },
            platformDefault      = (platform?.preferredEmulatorPackage
                ?: platformDao.getById(platformId)?.preferredEmulatorPackage)?.takeIf { it.isNotBlank() },
        )
    }
}
