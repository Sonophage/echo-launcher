package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.achievement.Achievement
import com.psplauncher.core.domain.achievement.AchievementSet
import com.psplauncher.core.domain.achievement.AchievementTotals
import com.psplauncher.core.domain.discord.DiscordFriend
import com.psplauncher.core.domain.discord.DiscordUser
import com.psplauncher.core.domain.model.Game
import com.psplauncher.core.domain.model.GamepadAction

const val DEFAULT_PROFILE_NAME = "Player"

const val BADGE_COLUMNS = 6

enum class ProfileTab(val label: String) { OVERVIEW("Overview"), ACHIEVEMENTS("Achievements"), FRIENDS("Friends") }

enum class BadgeFilter(val label: String) { ALL("All"), UNLOCKED("Unlocked"), LOCKED("Locked") }

enum class RarityTier(val label: String) { LEGENDARY("Legendary"), EPIC("Epic"), RARE("Rare"), COMMON("Common"), UNKNOWN("Unknown") }

enum class FriendGroup(val label: String) { PLAYING("Playing"), ONLINE("Online"), OFFLINE("Offline") }

data class ProfileData(
    val games: Int = 0,
    val playTimeMs: Long = 0,
    val recent: List<Game> = emptyList(),
    val totals: AchievementTotals? = null,
    val sets: List<AchievementSet> = emptyList(),
    val badges: Map<String, List<Achievement>> = emptyMap(),
    val platforms: Map<Long, String> = emptyMap(),
    val raLinked: Boolean = false,
    val steamLinked: Boolean = false,
    val discordSignedIn: Boolean = false,
    val discordUser: DiscordUser? = null,
    val friends: List<DiscordFriend> = emptyList(),
)

data class ProfileState(
    val tab: ProfileTab = ProfileTab.OVERVIEW,
    val set: Int = 0,
    val inGrid: Boolean = false,
    val badge: Int = 0,
    val filter: BadgeFilter = BadgeFilter.ALL,
    val friend: Int = 0,
    val openOnGameId: Long? = null,
    val data: ProfileData = ProfileData(),
) {
    val focusedSet: AchievementSet? get() = data.sets.getOrNull(set)

    val visibleBadges: List<Achievement>
        get() = focusedSet?.let { filterBadges(data.badges[setKey(it)].orEmpty(), filter) }.orEmpty()

    val friendRows: List<DiscordFriend> get() = groupFriends(data.friends).flatMap { it.second }
}

fun setKey(set: AchievementSet): String = "${set.provider.name}:${set.providerGameId}"

fun rarityTier(globalPercent: Double?): RarityTier = when {
    globalPercent == null -> RarityTier.UNKNOWN
    globalPercent < 1.0 -> RarityTier.LEGENDARY
    globalPercent < 5.0 -> RarityTier.EPIC
    globalPercent < 20.0 -> RarityTier.RARE
    else -> RarityTier.COMMON
}

fun filterBadges(all: List<Achievement>, filter: BadgeFilter): List<Achievement> = when (filter) {
    BadgeFilter.ALL -> all.sortedByDescending { it.isUnlocked }
    BadgeFilter.UNLOCKED -> all.filter { it.isUnlocked }
    BadgeFilter.LOCKED -> all.filterNot { it.isUnlocked }
}

data class ShowcaseBadge(val achievement: Achievement, val game: String)

fun showcase(sets: List<AchievementSet>, badges: Map<String, List<Achievement>>, count: Int = 4): List<ShowcaseBadge> =
    sets.flatMap { set ->
        badges[setKey(set)].orEmpty().filter { it.isUnlocked && it.globalPercent != null }.map { ShowcaseBadge(it, set.title) }
    }.sortedBy { it.achievement.globalPercent }.take(count)

fun groupFriends(friends: List<DiscordFriend>): List<Pair<FriendGroup, List<DiscordFriend>>> {
    val byGroup = friends.sortedBy { it.label.lowercase() }.groupBy {
        when {
            it.activity != null -> FriendGroup.PLAYING
            it.presence.isOnline -> FriendGroup.ONLINE
            else -> FriendGroup.OFFLINE
        }
    }
    return FriendGroup.entries.mapNotNull { g -> byGroup[g]?.let { g to it } }
}

fun stepProfile(state: ProfileState, action: GamepadAction): ProfileState {
    val tabs = ProfileTab.entries
    return when (action) {
        GamepadAction.PREV_CATEGORY -> state.copy(tab = tabs[(state.tab.ordinal - 1 + tabs.size) % tabs.size])
        GamepadAction.NEXT_CATEGORY -> state.copy(tab = tabs[(state.tab.ordinal + 1) % tabs.size])
        else -> when (state.tab) {
            ProfileTab.OVERVIEW -> state
            ProfileTab.FRIENDS -> {
                val last = (state.friendRows.size - 1).coerceAtLeast(0)
                when (action) {
                    GamepadAction.NAVIGATE_UP -> state.copy(friend = (state.friend - 1).coerceAtLeast(0))
                    GamepadAction.NAVIGATE_DOWN -> state.copy(friend = (state.friend + 1).coerceAtMost(last))
                    else -> state
                }
            }
            ProfileTab.ACHIEVEMENTS -> stepAchievements(state, action)
        }
    }
}

private fun stepAchievements(state: ProfileState, action: GamepadAction): ProfileState {
    val sets = state.data.sets.size
    if (sets == 0) return state
    if (action == GamepadAction.OPEN_CONTEXT_MENU) {
        return state.copy(filter = BadgeFilter.entries[(state.filter.ordinal + 1) % BadgeFilter.entries.size], badge = 0)
    }
    val count = state.visibleBadges.size
    if (!state.inGrid) {
        return when (action) {
            GamepadAction.NAVIGATE_LEFT -> state.copy(set = (state.set - 1).coerceAtLeast(0), badge = 0)
            GamepadAction.NAVIGATE_RIGHT -> state.copy(set = (state.set + 1).coerceAtMost(sets - 1), badge = 0)
            GamepadAction.NAVIGATE_DOWN, GamepadAction.SELECT -> if (count > 0) state.copy(inGrid = true, badge = 0) else state
            else -> state
        }
    }
    val at = state.badge.coerceIn(0, (count - 1).coerceAtLeast(0))
    val column = at % BADGE_COLUMNS
    return when (action) {
        GamepadAction.NAVIGATE_LEFT -> if (column > 0) state.copy(badge = at - 1) else state
        GamepadAction.NAVIGATE_RIGHT -> if (column < BADGE_COLUMNS - 1 && at + 1 < count) state.copy(badge = at + 1) else state
        GamepadAction.NAVIGATE_UP -> if (at >= BADGE_COLUMNS) state.copy(badge = at - BADGE_COLUMNS) else state.copy(inGrid = false)
        GamepadAction.NAVIGATE_DOWN -> if (at + BADGE_COLUMNS < count) state.copy(badge = at + BADGE_COLUMNS) else state
        else -> state
    }
}

fun ProfileState.withData(next: ProfileData): ProfileState {
    val pinned = openOnGameId?.let { id -> next.sets.indexOfFirst { it.gameId == id }.takeIf { it >= 0 } }
    return copy(
        data = next,
        set = pinned ?: set.coerceIn(0, (next.sets.size - 1).coerceAtLeast(0)),
        openOnGameId = if (pinned != null) null else openOnGameId,
    )
}
