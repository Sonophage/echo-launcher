package com.psplauncher.feature.achievements

import com.psplauncher.core.data.achievement.AchievementCredentialsProvider
import com.psplauncher.core.data.database.dao.AccountAchievementDao
import com.psplauncher.core.data.database.dao.AccountAchievementSetDao
import com.psplauncher.core.data.database.dao.AchievementMatchNoteDao
import com.psplauncher.core.data.database.dao.AchievementSetRow
import com.psplauncher.core.data.database.dao.ProviderGameLinkDao
import com.psplauncher.core.data.database.entity.AccountAchievementEntity
import com.psplauncher.core.data.database.entity.AccountAchievementSetEntity
import com.psplauncher.core.data.database.entity.ProviderGameLinkEntity
import com.psplauncher.core.domain.achievement.Achievement
import com.psplauncher.core.domain.achievement.AchievementProvider
import com.psplauncher.core.domain.achievement.AchievementSet
import com.psplauncher.core.domain.achievement.AchievementTotals
import com.psplauncher.core.domain.achievement.ShibaTier
import com.psplauncher.core.domain.repository.GameRepository
import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.api.SyncedCoin
import com.psplauncher.feature.achievements.provider.RemoteAchievementSources
import com.psplauncher.feature.achievements.provider.steam.SteamAppListResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val remoteSources: RemoteAchievementSources,
    private val credentials: AchievementCredentialsProvider,
    private val setDao: AccountAchievementSetDao,
    private val coinDao: AccountAchievementDao,
    private val linkDao: ProviderGameLinkDao,
    private val matchNoteDao: AchievementMatchNoteDao,
    private val steamResolver: SteamAppListResolver,
    private val gameRepository: GameRepository,
) : AchievementController {

    override fun observeSetForGame(gameId: Long): Flow<AchievementSet?> =
        setDao.observeSetForGame(gameId).map { it?.toAchievementSet() }

    override fun observeSets(): Flow<List<AchievementSet>> =
        setDao.observeSets().map { rows -> rows.mapNotNull { it.toAchievementSet() } }

    override fun observeAchievements(provider: AchievementProvider, providerGameId: String): Flow<List<Achievement>> =
        coinDao.observeForSet(provider.name, providerGameId).map { rows -> rows.map { it.toAchievement() } }

    override fun observeTotals(): Flow<AchievementTotals> =
        coinDao.observeTotals(AchievementProvider.RETRO_ACHIEVEMENTS.name)
            .map { AchievementTotals(unlocked = it.unlocked, total = it.total, raPoints = it.raPoints) }

    override suspend fun syncGame(
        gameId: Long,
        provider: AchievementProvider,
        providerGameId: String,
    ): ProviderSyncResult = syncEntry(provider, providerGameId, titleOf(gameId))

    override suspend fun syncAccountEntry(
        provider: AchievementProvider,
        providerGameId: String,
        title: String,
    ): ProviderSyncResult = syncEntry(provider, providerGameId, title)

    private suspend fun syncEntry(
        provider: AchievementProvider,
        providerGameId: String,
        title: String,
    ): ProviderSyncResult {
        val result = remoteSources.forProvider(provider).fetch(providerGameId)
        if (result !is ProviderSyncResult.Success) {
            Timber.i("Sync %s/%s (%s): %s", provider.name, providerGameId, title, result)
            return result
        }

        val now = System.currentTimeMillis()
        val resolvedId = result.providerGameId
        val storedSet = setDao.getSet(provider.name, resolvedId)
        coinDao.deleteForSet(provider.name, resolvedId)
        coinDao.upsertAll(result.coins.map { it.toEntity(provider, resolvedId) })
        setDao.upsert(
            summaryOf(provider, resolvedId, result.coins, now).copy(
                title = title.ifBlank { storedSet?.title.orEmpty() },
                iconUrl = storedSet?.iconUrl,
            ),
        )
        credentials.setLastSyncedAt(now)
        return result
    }

    private suspend fun titleOf(gameId: Long): String =
        gameRepository.getById(gameId)?.displayTitle.orEmpty()

    override suspend fun linkManually(gameId: Long, provider: AchievementProvider, providerGameId: String) {
        linkDao.upsert(
            ProviderGameLinkEntity(
                gameId = gameId,
                provider = provider.name,
                providerGameId = providerGameId.trim(),
                source = "MANUAL",
                resolvedAt = System.currentTimeMillis(),
            ),
        )
        matchNoteDao.deleteForGame(gameId)
    }

    override suspend fun resolveSteamLink(gameId: Long, title: String): String? {
        val appId = steamResolver.resolveAppId(title) ?: return null
        linkDao.upsert(
            ProviderGameLinkEntity(
                gameId = gameId,
                provider = AchievementProvider.STEAM.name,
                providerGameId = appId,
                source = "STEAM_TITLE",
                resolvedAt = System.currentTimeMillis(),
            ),
        )
        matchNoteDao.deleteForGame(gameId)
        return appId
    }

    override suspend fun syncGameById(gameId: Long): ProviderSyncResult {
        val link = linkDao.getForGame(gameId) ?: return ProviderSyncResult.NotLinked
        val provider = AchievementProvider.fromName(link.provider) ?: return ProviderSyncResult.NotLinked
        return syncGame(gameId, provider, link.providerGameId)
    }

    override suspend fun syncAllLinked(onProgress: (done: Int, total: Int) -> Unit): BatchSyncResult {
        val links = linkDao.getAll()
        val linkedIdentities = links.map { it.provider to it.providerGameId }.toHashSet()
        val accountOnly = setDao.getAllSets().filter { (it.provider to it.providerGameId) !in linkedIdentities }

        val total = links.size + accountOnly.size
        var synced = 0
        var noCoins = 0
        var failed = 0
        var missingCredentials = false
        fun tally(result: ProviderSyncResult) = when (result) {
            is ProviderSyncResult.Success -> synced++
            ProviderSyncResult.NotFound -> noCoins++
            ProviderSyncResult.MissingCredentials -> missingCredentials = true
            ProviderSyncResult.ProfileNotPublic -> failed++
            is ProviderSyncResult.Failed -> failed++
            ProviderSyncResult.NotLinked -> Unit
        }
        links.forEachIndexed { index, link ->
            onProgress(index, total)
            val provider = AchievementProvider.fromName(link.provider)
            if (provider == null) { failed++; return@forEachIndexed }
            tally(syncGame(link.gameId, provider, link.providerGameId))
        }
        accountOnly.forEachIndexed { index, set ->
            onProgress(links.size + index, total)
            val provider = AchievementProvider.fromName(set.provider)
            if (provider == null) { failed++; return@forEachIndexed }
            tally(syncAccountEntry(provider, set.providerGameId, set.title))
        }
        onProgress(total, total)
        return BatchSyncResult(
            total = total,
            synced = synced,
            noCoins = noCoins,
            failed = failed,
            missingCredentials = missingCredentials,
        )
    }
}

data class BatchSyncResult(
    val total: Int,
    val synced: Int,
    val noCoins: Int,
    val failed: Int,
    val missingCredentials: Boolean,
)

private fun AchievementSetRow.toAchievementSet(): AchievementSet? {
    val p = AchievementProvider.fromName(provider) ?: return null
    return AchievementSet(
        provider = p,
        providerGameId = providerGameId,
        gameId = libraryGameId,
        title = title,
        iconUrl = iconUrl,
        total = total,
        unlocked = unlocked,
        points = points,
        earnedPoints = earnedPoints,
        mastered = mastered,
        lastSyncedAt = lastSyncedAt,
        lastPlayedAt = lastPlayedAt,
    )
}

private fun AccountAchievementEntity.toAchievement() = Achievement(
    id = providerAchievementId,
    name = title,
    description = description,
    iconUrl = iconUrl,
    isHidden = isHidden,
    isUnlocked = isEarned,
    unlockedAt = earnedAt,
    globalPercent = globalRarity.takeIf { it >= 0 },
    points = points,
)

private fun SyncedCoin.toEntity(provider: AchievementProvider, providerGameId: String) = AccountAchievementEntity(
    provider = provider.name,
    providerGameId = providerGameId,
    providerAchievementId = providerAchievementId,
    title = title,
    description = description,
    tier = tier.name,
    globalRarity = globalRarity,
    iconUrl = iconUrl,
    isHidden = isHidden,
    isEarned = isEarned,
    earnedAt = earnedAt,
    points = points,
)

private fun summaryOf(
    provider: AchievementProvider,
    providerGameId: String,
    coins: List<SyncedCoin>,
    now: Long,
): AccountAchievementSetEntity {
    fun count(tier: ShibaTier, earnedOnly: Boolean) =
        coins.count { it.tier == tier && (!earnedOnly || it.isEarned) }

    val platinumCoins = coins.filter { it.tier == ShibaTier.PLATINUM }
    val mastered = if (platinumCoins.isNotEmpty()) platinumCoins.any { it.earnedHardcore }
                   else coins.isNotEmpty() && coins.all { it.earnedHardcore }
    return AccountAchievementSetEntity(
        provider = provider.name,
        providerGameId = providerGameId,
        title = "",
        bronzeTotal = count(ShibaTier.BRONZE, earnedOnly = false),
        silverTotal = count(ShibaTier.SILVER, earnedOnly = false),
        goldTotal = count(ShibaTier.GOLD, earnedOnly = false),
        bronzeEarned = count(ShibaTier.BRONZE, earnedOnly = true),
        silverEarned = count(ShibaTier.SILVER, earnedOnly = true),
        goldEarned = count(ShibaTier.GOLD, earnedOnly = true),
        mastered = mastered,
        lastSyncedAt = now,
    )
}
