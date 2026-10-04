package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction

enum class RecentFilter(val label: String) {
    ALL("All"),
    GAMES("Games"),
    MUSIC("Music"),
    BOOKS("Books"),
    VIDEO("Video"),

    APPS("Apps");

    companion object {
        fun visible(includeApps: Boolean): List<RecentFilter> =
            entries.filter { it != APPS || includeApps }
    }

    fun step(delta: Int, includeApps: Boolean): RecentFilter {
        val cycle = visible(includeApps)
        val here = cycle.indexOf(this)
        return if (here < 0) ALL else cycle[(here + delta).mod(cycle.size)]
    }
}

internal enum class RailStep { Open, Close, Pass }

internal fun recentRailStep(action: GamepadAction, onLastPlayedHome: Boolean, railVisible: Boolean): RailStep = when {
    !onLastPlayedHome -> RailStep.Pass
    action == GamepadAction.NAVIGATE_LEFT && !railVisible -> RailStep.Open
    (action == GamepadAction.NAVIGATE_RIGHT || action == GamepadAction.BACK) && railVisible -> RailStep.Close
    else -> RailStep.Pass
}

internal enum class RecentLaunch { GAME, STORED_INTENT, SHORTCUT, APP, VIDEO, BOOK, TRACK, ALBUM }

internal fun recentLaunchFor(item: CrossbarItem): RecentLaunch? = when {
    item.type == CrossbarItemType.VIDEO_FILE   -> RecentLaunch.VIDEO
    item.type == CrossbarItemType.LIBRARY_BOOK -> RecentLaunch.BOOK
    item.type == CrossbarItemType.MUSIC_TRACK  -> RecentLaunch.TRACK
    item.type == CrossbarItemType.MUSIC_GROUP && item.musicGroupKey != null -> RecentLaunch.ALBUM

    item.gameId != null && item.isRealGame              -> RecentLaunch.GAME
    item.launchIntentUri != null                        -> RecentLaunch.STORED_INTENT
    item.shortcutId != null && item.packageName != null -> RecentLaunch.SHORTCUT
    item.packageName != null                            -> RecentLaunch.APP

    else -> null
}

internal fun mergeRecents(
    games: List<Pair<Long, CrossbarItem>>,
    music: List<Pair<Long, CrossbarItem>>,
    books: List<Pair<Long, CrossbarItem>>,
    videos: List<Pair<Long, CrossbarItem>>,

    apps: List<Pair<Long, CrossbarItem>>,
    filter: RecentFilter,
    limit: Int,
): List<CrossbarItem> {
    val chosen = when (filter) {
        RecentFilter.ALL -> games + music + books + videos + apps
        RecentFilter.GAMES -> games
        RecentFilter.MUSIC -> music
        RecentFilter.BOOKS -> books
        RecentFilter.VIDEO -> videos
        RecentFilter.APPS -> apps
    }
    return chosen
        .sortedByDescending { it.first }
        .take(limit)
        .map { (at, item) -> item.copy(lastOpenedAt = at.takeIf { it > 0L }) }
}

enum class RecentDay(val label: String) { TODAY("Today"), YESTERDAY("Yesterday"), EARLIER("Earlier") }

internal fun groupRecentsByDay(
    items: List<CrossbarItem>,
    now: Long,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): List<Pair<RecentDay, List<IndexedValue<CrossbarItem>>>> {
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val byDay = items.withIndex().groupBy { (_, item) ->
        when (item.lastOpenedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }) {
            today -> RecentDay.TODAY
            today.minusDays(1) -> RecentDay.YESTERDAY
            else -> RecentDay.EARLIER
        }
    }
    return RecentDay.entries.mapNotNull { day -> byDay[day]?.let { day to it } }
}

// an app on the Recent shelf; the menu offers "Remove from Recent" only to ids that start this way
internal fun recentAppId(packageName: String): String = "${CrossbarViewModel.RECENT_APP_ID_PREFIX}$packageName"

internal val CrossbarItem.removableFromRecent: Boolean
    get() = when (type) {
        CrossbarItemType.VIDEO_FILE, CrossbarItemType.LIBRARY_BOOK, CrossbarItemType.MUSIC_TRACK -> true
        CrossbarItemType.MUSIC_GROUP -> false
        else -> gameId != null || packageName != null
    }

/**
 * An app's "last used" comes from Android's UsageStats and cannot be cleared, so
 * Remove from Recent cannot work the way it does for a game or a track, where the
 * stamp is ours and clearing it is the whole action. Instead the dismissal is
 * stamped, and the app returns the moment it is used again -- which is the same
 * behaviour, reached the only way it can be.
 *
 * This is deliberately NOT a HiddenPlacement. Hiding is permanent until undone in
 * Settings; this is not, and conflating the two put six apps in the Hidden Items
 * list that their owner only meant to clear off a shelf.
 */
private const val DISMISSAL_SEPARATOR = '|'

internal fun parseRecentDismissals(raw: Set<String>): Map<String, Long> =
    raw.mapNotNull { entry ->
        val at = entry.substringAfterLast(DISMISSAL_SEPARATOR).toLongOrNull() ?: return@mapNotNull null
        val pkg = entry.substringBeforeLast(DISMISSAL_SEPARATOR).takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        pkg to at
    }.toMap()

internal fun withRecentDismissal(raw: Set<String>, packageName: String, at: Long): Set<String> =
    raw.filterNot { it.substringBeforeLast(DISMISSAL_SEPARATOR) == packageName }.toSet() +
        "$packageName$DISMISSAL_SEPARATOR$at"

internal fun dismissedFromRecents(lastUsedAt: Long, dismissedAt: Long?): Boolean =
    dismissedAt != null && lastUsedAt <= dismissedAt
