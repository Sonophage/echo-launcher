package com.psplauncher.feature.achievements.provider.retro

import com.psplauncher.feature.achievements.api.ProviderSyncResult
import com.psplauncher.feature.achievements.api.SyncedCoin
import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val BADGE_BASE = "https://media.retroachievements.org/Badge"
private val RA_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

internal object RaCoinMapper {
    fun map(game: GetGameInfoAndUserProgress.Response, providerGameId: String): ProviderSyncResult {
        if (game.achievements.isEmpty()) return ProviderSyncResult.NotFound

        val players = game.numDistinctPlayersCasual.toDouble()
        val coins = game.achievements.values.map { a ->
            val awarded = a.numAwarded.toDouble()
            val percent = if (players > 0) awarded / players * 100.0 else SyncedCoin.RARITY_UNAVAILABLE

            val earnedAt = a.dateEarnedHardcore?.let(::parseRaDate) ?: a.dateEarned?.let(::parseRaDate)
            SyncedCoin(
                providerAchievementId = a.id,
                title = a.title,
                description = a.description,
                globalRarity = percent,
                iconUrl = a.badgeName.takeIf { it.isNotBlank() }?.let { "$BADGE_BASE/$it.png" },
                isHidden = false,
                isEarned = earnedAt != null,
                earnedAt = earnedAt,
                points = a.points.toInt(),
            )
        }
        return ProviderSyncResult.Success(providerGameId, coins)
    }

    private fun parseRaDate(raw: String): Long? = runCatching {
        LocalDateTime.parse(raw.trim(), RA_DATE).toInstant(ZoneOffset.UTC).toEpochMilli()
    }.getOrNull()
}
