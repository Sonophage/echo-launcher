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

// a media section's buttons: who made it (artists, authors) or, with X, genres
internal fun mediaChips(cases: List<InstalledApp>, byGenre: Boolean): List<SystemChip> {
    val key: (InstalledApp) -> String? = { if (byGenre) it.media?.genre else it.media?.maker }
    return listOf(SystemChip(null, "All", cases.size)) +
        cases.mapNotNull(key).groupingBy { it }.eachCount()
            .map { (name, n) -> SystemChip(name, name, n) }
            .sortedWith(compareByDescending<SystemChip> { it.count }.thenBy { it.label.lowercase() })
}

internal fun List<InstalledApp>.ofMediaChip(id: String?, byGenre: Boolean): List<InstalledApp> =
    if (id == null) this else filter { (if (byGenre) it.media?.genre else it.media?.maker) == id }

// what X says it will do in a media section
internal fun mediaGroupingHint(section: AppFilter, byGenre: Boolean): String? = when (section) {
    AppFilter.MUSIC -> if (byGenre) "Group by Artist" else "Group by Genre"
    AppFilter.BOOKS -> if (byGenre) "Group by Author" else "Group by Genre"
    else -> null
}
