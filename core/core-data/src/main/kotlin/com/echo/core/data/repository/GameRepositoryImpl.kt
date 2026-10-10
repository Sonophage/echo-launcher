package com.echo.core.data.repository

import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.dao.PlaySessionDao
import com.echo.core.data.database.dao.PlatformDao
import com.echo.core.data.database.entity.toDomain
import com.echo.core.data.database.entity.toEntity
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameStats
import com.echo.core.domain.model.PlaySession
import com.echo.core.domain.model.RecentPlatform
import com.echo.core.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val gameDao: GameDao,
    private val playSessionDao: PlaySessionDao,
    private val platformDao: PlatformDao,
) : GameRepository {
    override fun observeAll(): Flow<List<Game>> =
        gameDao.observeAll().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeGamesOnly(): Flow<List<Game>> =
        gameDao.observeGamesOnly().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeGamesOnlyStats(): Flow<GameStats> = gameDao.observeGamesOnlyStats()

    override fun observeRecentGamesOnly(limit: Int): Flow<List<Game>> =
        gameDao.observeRecentGamesOnly(limit).map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeAllGames(): Flow<List<Game>> =
        gameDao.observeAllGames().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeFavorites(): Flow<List<Game>> =
        gameDao.observeFavorites().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeByPlayState(state: com.echo.core.domain.model.PlayState): Flow<List<Game>> =
        gameDao.observeByPlayState(state.name).map { rows -> rows.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observePlayStateCount(state: com.echo.core.domain.model.PlayState): Flow<Int> =
        gameDao.observePlayStateCount(state.name)

    override fun observeRecentlyAdded(): Flow<List<Game>> =
        gameDao.observeRecentlyAdded().map { rows -> rows.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeRecentlyAddedCount(): Flow<Int> = gameDao.observeRecentlyAddedCount()

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Game>> =
        gameDao.observeRecentlyPlayed(limit).map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observeByPlatform(platformId: String): Flow<List<Game>> =
        gameDao.observeByPlatform(platformId).map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun observePlatformGames(platformId: String): Flow<List<Game>> =
        gameDao.observePlatformGames(platformId).map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override suspend fun getByPlatform(platformId: String): List<Game> =
        gameDao.getByPlatformOnce(platformId).map { it.toDomain() }

    override fun observeRecentPlatforms(limit: Int): Flow<List<RecentPlatform>> {
        return combine(
            playSessionDao.observeRecentPlatformIds(),
            platformDao.observeAll(),
        ) { recentPlatformIds, allPlatforms ->
            val platformMap = allPlatforms.associateBy { it.id }

            recentPlatformIds.take(limit).mapNotNull { platformId ->
                val platformEntity = platformMap[platformId] ?: return@mapNotNull null
                val lastPlayedAt   = playSessionDao.getLastPlayedAt(platformId) ?: return@mapNotNull null
                val recentGameIds  = playSessionDao.getRecentGameIdsForPlatform(platformId, limit = 20)
                val recentGames    = recentGameIds.mapNotNull { gameDao.getById(it)?.toDomain() }

                RecentPlatform(
                    platform     = platformEntity.toDomain(),
                    lastPlayedAt = lastPlayedAt,
                    recentGames  = recentGames,
                )
            }
        }
    }

    override suspend fun getById(id: Long): Game? =
        gameDao.getById(id)?.toDomain()

    override suspend fun getDiscSetMembers(discSetKey: String): List<Game> =
        gameDao.getDiscSetMembers(discSetKey).map { it.toDomain() }

    override suspend fun getByPackageName(packageName: String): Game? =
        gameDao.getByPackageName(packageName)?.toDomain()

    override suspend fun getAppEntry(packageName: String): Game? =
        gameDao.getAppEntry(packageName)?.toDomain()

    override suspend fun getLauncherShortcut(packageName: String, shortcutId: String): Game? =
        gameDao.getLauncherShortcut(packageName, shortcutId)?.toDomain()

    override suspend fun getByIntentUri(intentUri: String): Game? =
        gameDao.getByIntentUri(intentUri)?.toDomain()

    override suspend fun upsert(game: Game): Long {
        val discSetKey = game.discSetKey
        if (game.isDiscPrimary && discSetKey != null) {
            gameDao.clearOtherDiscPrimaries(discSetKey, game.id)
        }

        val entity = game.toEntity()
        val existing = if (entity.id != 0L) gameDao.getById(entity.id) else null
        val merged = entity.copy(
            dateAdded = entity.dateAdded ?: existing?.dateAdded ?: System.currentTimeMillis(),
            playState = entity.playState ?: existing?.playState,
            genreOverride = entity.genreOverride ?: existing?.genreOverride,
        )
        return gameDao.upsert(merged)
    }

    override suspend fun delete(id: Long) {
        gameDao.deleteById(id)
        RecentPins.unpinGames(context, listOf(id))
        Timber.i("Game deleted: id=$id")
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        gameDao.setFavorite(id, isFavorite)

    override suspend fun setPlayState(id: Long, state: com.echo.core.domain.model.PlayState?) =
        gameDao.setPlayState(id, state?.name)

    override suspend fun setGenreOverride(id: Long, genre: com.echo.core.domain.model.GameGenre?) =
        gameDao.setGenreOverride(id, genre?.name)

    override suspend fun updateFavoriteSortOrder(id: Long, order: Int) =
        gameDao.updateFavoriteSortOrder(id, order)

    override suspend fun updateNote(id: Long, note: String?) =
        gameDao.updateNote(id, note)

    override suspend fun updateBoxArt(id: Long, uri: String?) =
        gameDao.updateArtwork(id, uri)

    override suspend fun updateLogoArt(id: Long, uri: String?) =
        gameDao.updateLogo(id, uri)

    override suspend fun updateIconArt(id: Long, uri: String?) =
        gameDao.updateIconUri(id, uri)

    override suspend fun setPreferredEmulator(id: Long, profileIdOrPackage: String?) =
        gameDao.setPreferredEmulator(id, profileIdOrPackage)

    override suspend fun clearPreferredEmulatorForPlatform(platformId: String) =
        gameDao.clearPreferredEmulatorForPlatform(platformId)

    override suspend fun setPreferredDisc(id: Long, discId: Long) =
        gameDao.setPreferredDisc(id, discId)

    override suspend fun clearLastPlayed(id: Long) = gameDao.clearLastPlayed(id)

    override suspend fun markOpened(id: Long, playedAt: Long) = gameDao.markOpened(id, playedAt)

    override suspend fun recordPlaySession(session: PlaySession) {
        playSessionDao.insert(session.toEntity())
        gameDao.addPlayTime(
            id             = session.gameId,
            durationMillis = session.durationMillis,
            playedAt       = session.launchedAt,
        )
        Timber.d("Play session recorded: gameId=${session.gameId}, platform=${session.platformId}")
    }

    override suspend fun updateScrapedTitle(id: Long, scrapedTitle: String?) =
        gameDao.updateScrapedTitle(id, scrapedTitle)

    override suspend fun updateStorefrontIdentity(id: Long, storefront: String?, storefrontGameId: String?) =
        gameDao.updateStorefrontIdentity(id, storefront, storefrontGameId)

    override suspend fun attachLauncherHandle(
        id: Long,
        packageName: String?,
        shortcutId: String?,
        launchIntentUri: String?,
    ) = gameDao.attachLauncherHandle(id, packageName, shortcutId, launchIntentUri)

    override suspend fun updateProviderMatch(id: Long, provider: String, providerGameId: Long?) =
        gameDao.updateProviderMatch(id, provider, providerGameId)

    override suspend fun getByStorefront(storefront: String, storefrontGameId: String): List<Game> =
        gameDao.getByStorefront(storefront, storefrontGameId).map { it.toDomain() }

    override suspend fun updateUserTitleOverride(id: Long, override: String?) =
        gameDao.updateUserTitleOverride(id, override)

    override suspend fun getMissingRoms(): List<Game> {
        val romPaths = gameDao.getAllRomPaths()
        val missingIds = romPaths
            .filter { !File(it.rom_path).exists() }
            .map { it.id }

        return missingIds.mapNotNull { gameDao.getById(it)?.toDomain() }
            .also { Timber.i("Missing ROM check: ${it.size} missing of ${romPaths.size} total") }
    }

    override fun observeMissing(): Flow<List<Game>> =
        gameDao.observeMissing().map { entities -> entities.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override suspend fun markSeen(romPaths: List<String>, seenAt: Long) =
        gameDao.markSeen(romPaths, seenAt)

    // owner, 2026-10-10: a game that goes comes off Pinned and loses its Playing or Backlog mark (the query clears
    // that); Recent already leaves out a missing game, and its play history stays
    override suspend fun markMissing(romPaths: List<String>) {
        gameDao.markMissing(romPaths)
        RecentPins.unpinGames(context, gameDao.idsForRomPaths(romPaths))
    }

    override suspend fun markMissingIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        gameDao.markMissingIds(ids)
        RecentPins.unpinGames(context, ids)
    }

    override suspend fun markSeenIds(ids: List<Long>) {
        if (ids.isNotEmpty()) gameDao.markSeenIds(ids)
    }

    override suspend fun deleteMissing(platformId: String): Int {
        val ids = gameDao.missingIds(platformId)
        return gameDao.deleteMissing(platformId).also {
            RecentPins.unpinGames(context, ids)
            Timber.i("Deleted $it missing games on $platformId")
        }
    }
}
