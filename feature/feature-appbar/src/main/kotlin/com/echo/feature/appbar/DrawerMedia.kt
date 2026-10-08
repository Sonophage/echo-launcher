package com.echo.feature.appbar

import com.echo.core.domain.model.Book
import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.Video
import com.echo.core.domain.model.genreName

// owner, 2026-10-08: the App Drawer holds the media libraries too: an album, a video or a book is a case on the wall,
// in its own section, shown only when that library has something in it
enum class MediaKind(val label: String) { MUSIC("Album"), VIDEO("Video"), BOOK("Book") }

// what a media case opens: the album's key, the video's or the book's id; who made it and its genre for the buttons
data class DrawerMedia(val kind: MediaKind, val ref: String, val maker: String?, val genre: String?)

internal const val MEDIA_KEY_PREFIX = "media:"

private fun mediaCase(kind: MediaKind, ref: String, label: String, art: String?, maker: String?, genre: String?, lastUsedAt: Long) =
    InstalledApp(
        packageName = "$MEDIA_KEY_PREFIX${kind.name}:$ref",
        label = label,
        icon = null,
        isGame = false,
        isEmulator = false,
        lastUsedAt = lastUsedAt,
        art = art,
        media = DrawerMedia(kind, ref, maker, genre),
    )

// one case per album, as Music's Albums lists them; an album's genre is its tracks' most common one
internal fun albumCases(tracks: List<MusicTrack>): List<InstalledApp> =
    tracks.filter { !it.album.isNullOrBlank() }
        .groupBy { it.album!!.trim().lowercase() }
        .map { (key, inAlbum) ->
            mediaCase(
                MediaKind.MUSIC, key, inAlbum.first().album!!.trim(),
                inAlbum.firstNotNullOfOrNull { it.artUri },
                inAlbum.firstNotNullOfOrNull { (it.albumArtist ?: it.artist)?.trim()?.ifBlank { null } },
                inAlbum.mapNotNull { it.genreName }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                inAlbum.maxOf { it.lastPlayedAt ?: 0L },
            )
        }

internal fun videoCases(videos: List<Video>): List<InstalledApp> = videos.map {
    mediaCase(MediaKind.VIDEO, it.id, it.displayTitle, it.posterUri ?: it.effectiveThumbnailUri, null, null, it.lastWatchedAt ?: 0L)
}

internal fun bookCases(books: List<Book>): List<InstalledApp> = books.map {
    mediaCase(MediaKind.BOOK, it.id, it.displayTitle, it.coverUri, it.author?.trim()?.ifBlank { null }, it.genreName, it.lastOpenedAt ?: 0L)
}

// owner, 2026-10-08: what a media section's buttons group by; X steps through them in this order
enum class MediaGrouping { MAKER, TITLE, GENRE;
    val next: MediaGrouping get() = entries[(ordinal + 1) % entries.size]
}

// a case's button under a grouping: who made it, its title's first letter, or its genre
private fun DrawerMediaChipKey(app: InstalledApp, by: MediaGrouping): String? = when (by) {
    MediaGrouping.MAKER -> app.media?.maker
    MediaGrouping.TITLE -> com.echo.core.ui.components.initialOf(app.label).toString()
    MediaGrouping.GENRE -> app.media?.genre
}

// a media section's buttons: artists or authors and genres by count, letters in A-Z order
internal fun mediaChips(cases: List<InstalledApp>, by: MediaGrouping): List<SystemChip> {
    val counted = cases.mapNotNull { DrawerMediaChipKey(it, by) }.groupingBy { it }.eachCount().map { (name, n) -> SystemChip(name, name, n) }
    val ordered = if (by == MediaGrouping.TITLE) counted.sortedBy { it.label }
        else counted.sortedWith(compareByDescending<SystemChip> { it.count }.thenBy { it.label.lowercase() })
    return listOf(SystemChip(null, "All", cases.size)) + ordered
}

internal fun List<InstalledApp>.ofMediaChip(id: String?, by: MediaGrouping): List<InstalledApp> =
    if (id == null) this else filter { DrawerMediaChipKey(it, by) == id }

// what X says it will do next in a media section
internal fun mediaGroupingHint(section: AppFilter, by: MediaGrouping): String? {
    val (maker, title) = when (section) {
        AppFilter.MUSIC -> "Artist" to "Album"
        AppFilter.BOOKS -> "Author" to "Title"
        else -> return null
    }
    return "Group by " + when (by.next) { MediaGrouping.MAKER -> maker; MediaGrouping.TITLE -> title; MediaGrouping.GENRE -> "Genre" }
}

// "12/40", or nothing when the game has no set or the set is empty
internal fun achievementsLabel(unlocked: Int?, total: Int?): String? =
    if (unlocked != null && total != null && total > 0) "$unlocked/$total" else null

data class EarnedBadge(val iconUrl: String?, val earned: Boolean, val earnedAt: Long?)

// the last achievements earned, newest first, as many as fit one row; those with no icon are skipped
internal fun recentBadges(coins: List<EarnedBadge>, max: Int = RECENT_BADGES): List<String> =
    coins.filter { it.earned && !it.iconUrl.isNullOrBlank() }
        .sortedByDescending { it.earnedAt ?: 0L }
        .take(max)
        .map { it.iconUrl!! }

internal const val RECENT_BADGES = 6

// owner, 2026-10-08: the info column names an album's artist (a book's author) and its genre
internal fun mediaByline(media: DrawerMedia): String? =
    listOfNotNull(media.maker, media.genre).joinToString("  ·  ").ifEmpty { null }

// what X says it will do in a media section
internal fun mediaGroupingHint(section: AppFilter, byGenre: Boolean): String? = when (section) {
    AppFilter.MUSIC -> if (byGenre) "Group by Artist" else "Group by Genre"
    AppFilter.BOOKS -> if (byGenre) "Group by Author" else "Group by Genre"
    else -> null
}
