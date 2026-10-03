package com.psplauncher.feature.achievements.provider.steam

import com.psplauncher.core.domain.achievement.ShibaTier
import com.psplauncher.feature.achievements.api.SyncedCoin

internal object SteamCoinMapper {
    private val COMPLETION_PATTERNS = listOf(
        Regex(
            """\b(?:unlock|earn|obtain|get|collect|complete)(?:e?d)? (?:every|all) (?:the )?(?:other )?achievements?\b""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\bearn(?:ed)? all base[- ]game achievements?\b""", RegexOption.IGNORE_CASE),
        Regex("""\ball (?:other )?achievements (?:unlocked|earned|obtained|completed)\b""", RegexOption.IGNORE_CASE),
    )

    fun map(
        appId: String,
        schema: List<SteamSchemaAchievement>,
        percentByName: Map<String, Double>,
        earnedByName: Map<String, SteamPlayerAchievement>,
    ): List<SyncedCoin> {
        val overrideApiName = SteamPlatinumOverrides.completionApiName(appId)
        return schema.map { a ->
            val percent: Double? = percentByName[a.name]
            val earned = earnedByName[a.name]
            val isEarned = earned?.achieved == 1
            val isPlatinum = a.name == overrideApiName || isCompletionCandidate(a.description)
            SyncedCoin(
                providerAchievementId = a.name,
                title = a.displayName ?: a.name,
                description = a.description.orEmpty(),
                tier = if (isPlatinum) ShibaTier.PLATINUM else ShibaTier.forRarity(percent),
                globalRarity = percent ?: SyncedCoin.RARITY_UNAVAILABLE,
                iconUrl = if (isEarned) a.icon else (a.icongray ?: a.icon),
                isHidden = a.hidden == 1,
                isEarned = isEarned,

                earnedHardcore = isEarned,

                earnedAt = earned?.unlocktime?.takeIf { it > 0 }?.times(1_000),
            )
        }
    }

    fun isCompletionCandidate(description: String?): Boolean {
        if (description.isNullOrBlank()) return false
        return COMPLETION_PATTERNS.any { it.containsMatchIn(description) }
    }
}
