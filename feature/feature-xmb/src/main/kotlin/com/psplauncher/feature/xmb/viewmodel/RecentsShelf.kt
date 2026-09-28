package com.psplauncher.feature.xmb.viewmodel

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

    fun next(includeApps: Boolean): RecentFilter {
        val cycle = visible(includeApps)
        val here = cycle.indexOf(this)
        return if (here < 0) ALL else cycle[(here + 1) % cycle.size]
    }
}

internal fun mergeRecents(
    games: List<Pair<Long, XMBItem>>,
    music: List<Pair<Long, XMBItem>>,
    books: List<Pair<Long, XMBItem>>,
    videos: List<Pair<Long, XMBItem>>,

    apps: List<Pair<Long, XMBItem>>,
    filter: RecentFilter,
    limit: Int,
): List<XMBItem> {
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
        .map { it.second }
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
