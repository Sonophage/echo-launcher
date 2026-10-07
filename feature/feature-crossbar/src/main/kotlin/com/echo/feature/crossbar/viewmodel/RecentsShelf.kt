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
        // All, then only the kinds that have something in them (owner, 2026-10-05: an empty filter is not shown)
        fun shown(stocked: Set<RecentFilter>): List<RecentFilter> =
            entries.filter { it == ALL || it in stocked }
    }

    fun step(delta: Int, cycle: List<RecentFilter>): RecentFilter {
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

// a horizontal swipe takes the rail step its d-pad direction would (owner, 2026-10-04: swiping on
// Last Played changed category, so touch could never open the Recent rail); a negative step is LEFT
internal fun swipeRailStep(direction: Int, onLastPlayedHome: Boolean, railVisible: Boolean): RailStep =
    recentRailStep(if (direction < 0) GamepadAction.NAVIGATE_LEFT else GamepadAction.NAVIGATE_RIGHT, onLastPlayedHome, railVisible)

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

// apps is empty while Apps on the Recent shelf is off, so that filter goes with it
internal fun stockedRecentFilters(
    games: List<Any>, music: List<Any>, books: List<Any>, videos: List<Any>, apps: List<Any>,
): Set<RecentFilter> = buildSet {
    if (games.isNotEmpty()) add(RecentFilter.GAMES)
    if (music.isNotEmpty()) add(RecentFilter.MUSIC)
    if (books.isNotEmpty()) add(RecentFilter.BOOKS)
    if (videos.isNotEmpty()) add(RecentFilter.VIDEO)
    if (apps.isNotEmpty()) add(RecentFilter.APPS)
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

// owner, 2026-10-07: the pinned list leads the rail, above the dated groups
enum class RecentDay(val label: String) { PINNED("Pinned"), TODAY("Today"), YESTERDAY("Yesterday"), EARLIER("Earlier") }

internal fun groupRecentsByDay(
    items: List<CrossbarItem>,
    now: Long,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): List<Pair<RecentDay, List<IndexedValue<CrossbarItem>>>> {
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val byDay = items.withIndex().groupBy { (_, item) ->
        if (item.pinnedToRecent) return@groupBy RecentDay.PINNED
        when (item.lastOpenedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }) {
            today -> RecentDay.TODAY
            today.minusDays(1) -> RecentDay.YESTERDAY
            else -> RecentDay.EARLIER
        }
    }
    return RecentDay.entries.mapNotNull { day -> byDay[day]?.let { day to it } }
}

// owner, 2026-10-06: games and apps pinned under Recent, in the order they were pinned. A pin is "g:<game id>"
// or "a:<package>"; a pinned row's id is its own, so it never clashes with the same item's recent row
internal fun pinKey(item: CrossbarItem): String? = when {
    item.gameId != null -> "g:${item.gameId}"
    item.packageName != null -> "a:${item.packageName}"
    else -> null
}

internal fun parsePins(raw: String?): List<String> = raw?.split('\n')?.filter { it.isNotBlank() }?.distinct().orEmpty()

internal fun togglePin(pins: List<String>, key: String): List<String> = if (key in pins) pins - key else pins + key

internal fun pinnedRowId(key: String): String = "pin:$key"

// the pinned rows a filter shows: all of them under All, else those of the filter's kind
internal fun pinnedForFilter(pinned: List<CrossbarItem>, filter: RecentFilter): List<CrossbarItem> = when (filter) {
    RecentFilter.ALL -> pinned
    RecentFilter.GAMES -> pinned.filter { recentKind(it) == RecentKind.GAME }
    RecentFilter.APPS -> pinned.filter { recentKind(it) == RecentKind.APP }
    else -> emptyList()
}

// an app on the Recent shelf; the menu offers "Remove from Recent" only to ids that start this way
internal fun recentAppId(packageName: String): String = "${CrossbarViewModel.RECENT_APP_ID_PREFIX}$packageName"

internal val CrossbarItem.removableFromRecent: Boolean
    get() = when (type) {
        CrossbarItemType.VIDEO_FILE, CrossbarItemType.LIBRARY_BOOK, CrossbarItemType.MUSIC_TRACK -> true
        // owner, 2026-10-05: an album on the shelf is its recent tracks, and removing it clears them all
        CrossbarItemType.MUSIC_GROUP -> isRecentAlbum
        else -> gameId != null || packageName != null
    }

// the album row the shelf folds from consecutive tracks (recentMusicRows)
internal val CrossbarItem.isRecentAlbum: Boolean
    get() = type == CrossbarItemType.MUSIC_GROUP && musicGroupKey != null && id.startsWith(RECENT_ALBUM_ID_PREFIX)

internal const val RECENT_ALBUM_ID_PREFIX = "mg_alb_"

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
