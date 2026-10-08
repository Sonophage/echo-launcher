package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.achievement.Achievement
import com.echo.core.domain.achievement.AchievementSet
import com.echo.core.domain.achievement.AchievementTotals
import com.echo.core.domain.discord.DiscordFriend
import com.echo.core.domain.discord.DiscordUser
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GamepadAction

const val DEFAULT_PROFILE_NAME = "Player"

const val BADGE_COLUMNS = 6

// owner, 2026-10-06: Overview (the library at a glance) moved here from Settings
enum class ProfileTab { ACHIEVEMENTS, FRIENDS, OVERVIEW }

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
    // each set's game art, for a set with no icon of its own (Steam sets have none)
    val setArt: Map<Long, String> = emptyMap(),
    val platforms: Map<Long, String> = emptyMap(),
    val raLinked: Boolean = false,
    val steamLinked: Boolean = false,
    val discordSignedIn: Boolean = false,
    val discordUser: DiscordUser? = null,
    val friends: List<DiscordFriend> = emptyList(),
)

data class ProfileState(
    val tab: ProfileTab = ProfileTab.ACHIEVEMENTS,
    val set: Int = 0,
    val inGrid: Boolean = false,
    val badge: Int = 0,
    val filter: BadgeFilter = BadgeFilter.ALL,
    val friend: Int = 0,
    val openOnGameId: Long? = null,
    val returnToPanel: Boolean = false,
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

data class ShowcaseBadge(val achievement: Achievement, val set: AchievementSet) {
    val game: String get() = set.title
}

fun showcase(sets: List<AchievementSet>, badges: Map<String, List<Achievement>>, count: Int = 4): List<ShowcaseBadge> =
    sets.flatMap { set ->
        badges[setKey(set)].orEmpty().filter { it.isUnlocked && it.globalPercent != null }.map { ShowcaseBadge(it, set) }
    }.sortedBy { it.achievement.globalPercent }.take(count)

fun showcaseSet(data: ProfileData): Int =
    showcase(data.sets, data.badges, 1).firstOrNull()?.let { data.sets.indexOf(it.set) }?.coerceAtLeast(0) ?: 0

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

fun stepProfile(state: ProfileState, action: GamepadAction): ProfileState = when (state.tab) {
    ProfileTab.FRIENDS -> {
        val last = (state.friendRows.size - 1).coerceAtLeast(0)
        when (action) {
            GamepadAction.NAVIGATE_UP -> state.copy(friend = (state.friend - 1).coerceAtLeast(0))
            GamepadAction.NAVIGATE_DOWN -> state.copy(friend = (state.friend + 1).coerceAtMost(last))
            else -> state
        }
    }
    ProfileTab.ACHIEVEMENTS -> stepAchievements(state, action)
    ProfileTab.OVERVIEW -> state
}

private fun stepAchievements(state: ProfileState, action: GamepadAction): ProfileState {
    val sets = state.data.sets.size
    if (sets == 0) return state
    // the filter is X, as sorting and filtering are everywhere (owner, 2026-10-06)
    if (action == GamepadAction.CHANGE_SORT) {
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

// owner, 2026-10-07: the achievements strip opens on the game played last. A game not in the library has no
// last-played time, so its newest unlock stands in; a set with neither goes after, in the order it came
fun setsByLastPlayed(sets: List<com.echo.core.domain.achievement.AchievementSet>): List<com.echo.core.domain.achievement.AchievementSet> =
    sets.sortedByDescending { maxOf(it.lastPlayedAt ?: Long.MIN_VALUE, it.lastUnlockedAt ?: Long.MIN_VALUE) }

fun recentlyPlayed(games: List<Game>, count: Int = RECENTLY_PLAYED_COUNT): List<Game> =
    games.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }
        .distinctBy { it.discSetKey ?: it.id.toString() }.take(count)

fun profileBanner(recent: List<Game>): String? =
    recent.firstOrNull()?.let { listOfNotNull(it.artworkUri, it.iconUri).firstOrNull { uri -> uri.isNotBlank() } }

const val RECENTLY_PLAYED_COUNT = 3

// STATS is the row of big numbers, which opens Overview (owner, 2026-10-06)
enum class ProfileSpot { EDIT, EDIT_NAME, EDIT_PICTURE, STATS, RECENT, SHOWCASE, FRIENDS }

data class ProfileFocus(val spot: ProfileSpot = ProfileSpot.EDIT, val recent: Int = 0) {
    val choosing: Boolean get() = spot == ProfileSpot.EDIT_NAME || spot == ProfileSpot.EDIT_PICTURE
}

fun moveProfileFocus(focus: ProfileFocus, move: PanelMove, recents: Int): ProfileFocus {
    val last = (recents - 1).coerceAtLeast(0)
    val at = focus.recent.coerceIn(0, last)
    val body = if (recents > 0) ProfileFocus(ProfileSpot.RECENT, at) else focus.copy(spot = ProfileSpot.SHOWCASE)
    val stats = focus.copy(spot = ProfileSpot.STATS)
    return when (focus.spot) {
        ProfileSpot.EDIT -> if (move == PanelMove.DOWN) stats else focus
        ProfileSpot.STATS -> when (move) {
            PanelMove.UP -> focus.copy(spot = ProfileSpot.EDIT)
            PanelMove.DOWN -> body
            else -> focus
        }
        ProfileSpot.EDIT_NAME -> when (move) {
            PanelMove.RIGHT -> focus.copy(spot = ProfileSpot.EDIT_PICTURE)
            PanelMove.DOWN -> body
            else -> focus
        }
        ProfileSpot.EDIT_PICTURE -> when (move) {
            PanelMove.LEFT -> focus.copy(spot = ProfileSpot.EDIT_NAME)
            PanelMove.DOWN -> body
            else -> focus
        }
        ProfileSpot.RECENT -> when (move) {
            PanelMove.UP -> if (at > 0) focus.copy(recent = at - 1) else stats
            PanelMove.DOWN -> focus.copy(recent = (at + 1).coerceAtMost(last))
            PanelMove.RIGHT -> focus.copy(spot = ProfileSpot.SHOWCASE)
            else -> focus
        }
        ProfileSpot.SHOWCASE -> when (move) {
            PanelMove.LEFT -> if (recents > 0) body else focus
            PanelMove.RIGHT -> focus.copy(spot = ProfileSpot.FRIENDS)
            PanelMove.UP -> stats
            else -> focus
        }
        ProfileSpot.FRIENDS -> when (move) {
            PanelMove.LEFT -> focus.copy(spot = ProfileSpot.SHOWCASE)
            PanelMove.UP -> stats
            else -> focus
        }
    }
}
