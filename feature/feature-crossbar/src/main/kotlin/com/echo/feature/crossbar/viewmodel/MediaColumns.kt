package com.echo.feature.crossbar.viewmodel

import com.echo.core.data.repository.MediaRootKind
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.ADD_MENU_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.mediaFoldersItemId
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.ALL_BOOKS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.ALL_MUSIC_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.ALL_PHOTOS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.PHOTO_FAVORITES_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.ALL_VIDEOS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.BOOK_SERIES_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.BOOK_SHELVES_ITEM_ID
import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.primaryArtist
import com.echo.core.domain.model.genreName
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.CAMERA_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.MEMORY_CARD_ASSET_URI
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.MUSIC_ALBUMS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.MUSIC_ARTISTS_ITEM_ID
import com.echo.core.domain.model.Video
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.NOW_PLAYING_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.OPEN_READER_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.PHOTO_ALBUMS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.PLAYLISTS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.VIDEO_COLLECTIONS_ITEM_ID
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel.Companion.VIDEO_LIBRARIES_ITEM_ID

private fun List<CrossbarItem>.withColumnCovers(pool: List<String>): List<CrossbarItem> {
    if (pool.isEmpty()) return this
    var slot = 0
    return map { item ->
        if (item.type in SINGLE_MEDIA_ITEM_TYPES) item
        else item.copy(insideCovers = pool.gridSliceAt(slot++))
    }
}

private val SINGLE_MEDIA_ITEM_TYPES =
    setOf(CrossbarItemType.MUSIC_TRACK, CrossbarItemType.VIDEO_FILE, CrossbarItemType.LIBRARY_BOOK)

internal fun CrossbarUiState.musicRootSections(): List<CrossbarItem> {
    val folders = musicFolders
    val totalTracks = folders.sumOf { it.trackCount }
    return buildList {
        musicPlayback.track?.let { track ->
            add(
                CrossbarItem(
                    id       = NOW_PLAYING_ITEM_ID,
                    title    = track.displayTitle,
                    subtitle = listOfNotNull("Now Playing", track.artist).joinToString("  ·  "),
                    coverUri = track.artUri,
                    type     = CrossbarItemType.MUSIC_TRACK,
                )
            )
        }

        add(
            CrossbarItem(
                id       = ALL_MUSIC_ITEM_ID,
                title    = "Songs",
                subtitle = countLabel(totalTracks, "track", "tracks"),
                coverUri = MEMORY_CARD_ASSET_URI,
                type     = CrossbarItemType.MEMORY_CARD,
            )
        )
        add(
            CrossbarItem(
                id       = MUSIC_ARTISTS_ITEM_ID,
                title    = "Artists",
                subtitle = "Browse by who made it",
                type     = CrossbarItemType.MUSIC_ARTISTS,
            )
        )
        add(
            CrossbarItem(
                id       = MUSIC_ALBUMS_ITEM_ID,
                title    = "Albums",
                subtitle = "Browse by release",
                type     = CrossbarItemType.MUSIC_ALBUMS,
            )
        )
        add(
            CrossbarItem(
                id       = CrossbarViewModel.MUSIC_GENRES_ITEM_ID,
                title    = "Genres",
                subtitle = "Browse by sound",
                type     = CrossbarItemType.MUSIC_GENRES,
            )
        )
        add(
            CrossbarItem(
                id       = PLAYLISTS_ITEM_ID,
                title    = "Playlists",
                subtitle = "Build and play your own track lists",
                type     = CrossbarItemType.PLAYLIST,
            )
        )
        add(
            CrossbarItem(
                id       = mediaFoldersItemId(MediaRootKind.MUSIC),
                title    = "Folders",
                subtitle = countLabel(folders.size, "folder", "folders"),
                type          = CrossbarItemType.MEDIA_ROOT,
                mediaRootKind = MediaRootKind.MUSIC,
            )
        )
    }.withColumnCovers(mediaCovers.music)
}

internal fun Video.toCrossbarRow(lead: String? = null): CrossbarItem = CrossbarItem(
    id       = "vid_$id",
    title    = displayTitle,
    subtitle = listOfNotNull(lead, videoRowSubtitle(durationMs, resolutionLabel, sizeBytes))
        .joinToString("  ·  "),
    type     = CrossbarItemType.VIDEO_FILE,
    mediaUri = uri,
    mimeType = mimeType,
    coverUri = effectiveThumbnailUri,
    progressFraction = videoProgressFraction(resumePositionMs, durationMs),
    progressLabel    = videoProgressLabel(resumePositionMs, durationMs),
)

internal fun CrossbarUiState.videoRootSections(): List<CrossbarItem> {
    val libraries = videoLibraries
    val totalVideos = libraries.sumOf { it.videoCount }
    return buildList {
        resumeVideo?.let { video ->
            add(video.toCrossbarRow(lead = "Resume"))
        }

        add(
            CrossbarItem(
                id       = ALL_VIDEOS_ITEM_ID,
                title    = "Videos",
                subtitle = countLabel(totalVideos, "video", "videos"),
                coverUri = MEMORY_CARD_ASSET_URI,
                type     = CrossbarItemType.MEMORY_CARD,
            )
        )

        add(
            CrossbarItem(
                id       = VIDEO_COLLECTIONS_ITEM_ID,
                title    = "Collections",
                subtitle = "Recently Watched, Favorites & Playlists",
                type     = CrossbarItemType.VIDEO_COLLECTIONS,
            )
        )
        add(
            CrossbarItem(
                id       = VIDEO_LIBRARIES_ITEM_ID,
                title    = "Video Libraries",
                subtitle = countLabel(libraries.size, "library", "libraries"),
                type     = CrossbarItemType.VIDEO_LIBRARY,
            )
        )
        add(
            CrossbarItem(
                id       = mediaFoldersItemId(MediaRootKind.VIDEO),
                title    = "Folders",
                subtitle = countLabel(libraries.size, "folder", "folders"),
                type          = CrossbarItemType.MEDIA_ROOT,
                mediaRootKind = MediaRootKind.VIDEO,
            )
        )
    }.withColumnCovers(mediaCovers.video)
}

internal fun CrossbarUiState.photoRootSections(cameraAvailable: Boolean): List<CrossbarItem> {
    val libraries = photoLibraries
    val totalPhotos = libraries.sumOf { it.photoCount }
    return buildList {
        if (cameraAvailable) {
            add(
                CrossbarItem(
                    id       = CAMERA_ITEM_ID,
                    title    = "Camera",
                    subtitle = "Open the camera",
                    type     = CrossbarItemType.CAMERA,
                )
            )
        }

        add(
            CrossbarItem(
                id       = ALL_PHOTOS_ITEM_ID,
                title    = "Photos",
                subtitle = countLabel(totalPhotos, "photo", "photos"),
                coverUri = MEMORY_CARD_ASSET_URI,
                type     = CrossbarItemType.MEMORY_CARD,
            )
        )
        if (photoFavoriteCount > 0) add(
            CrossbarItem(
                id       = PHOTO_FAVORITES_ITEM_ID,
                title    = "Favourites",
                subtitle = countLabel(photoFavoriteCount, "photo", "photos"),
                type     = CrossbarItemType.PHOTO_FAVORITES,
            )
        )
        add(
            CrossbarItem(
                id       = PHOTO_ALBUMS_ITEM_ID,
                title    = "Albums",
                subtitle = countLabel(libraries.size, "album", "albums"),
                type     = CrossbarItemType.PHOTO_ALBUMS,
            )
        )
        add(
            CrossbarItem(
                id       = mediaFoldersItemId(MediaRootKind.PHOTO),
                title    = "Folders",
                subtitle = countLabel(libraries.size, "folder", "folders"),
                type          = CrossbarItemType.MEDIA_ROOT,
                mediaRootKind = MediaRootKind.PHOTO,
            )
        )
    }.withColumnCovers(mediaCovers.photo)
}

internal fun CrossbarUiState.booksRootSections(): List<CrossbarItem> {
    val shelves = bookLibraries
    val totalBooks = shelves.sumOf { it.bookCount }
    val reader = defaultReader
    return buildList {
        continueBook?.let { book ->
            add(
                CrossbarItem(
                    id       = "book_${book.id}",
                    title    = book.displayTitle,
                    subtitle = listOfNotNull(
                        "Continue reading",
                        book.lastOpenedAt?.let { relativeDateTime(it) },
                    ).joinToString("  ·  "),
                    coverUri = book.coverUri,
                    type     = CrossbarItemType.LIBRARY_BOOK,
                )
            )
        }

        if (reader != null && reader != com.echo.core.data.book.BuiltInReader.ASK_EVERY_TIME) {
            add(
                CrossbarItem(
                    id       = OPEN_READER_ITEM_ID,
                    title    = defaultReaderLabel ?: "Open Reader",
                    subtitle = "Open your reader",
                    type     = CrossbarItemType.LIBRARY_READER,
                )
            )
        }

        if (shelves.size > 1) {
            add(
                CrossbarItem(
                    id       = BOOK_SHELVES_ITEM_ID,
                    title    = "Shelves",
                    subtitle = countLabel(shelves.size, "shelf", "shelves"),
                    type     = CrossbarItemType.LIBRARY_SHELVES,
                )
            )
        }

        if (bookGenres.isNotEmpty()) {
            add(
                CrossbarItem(
                    id       = CrossbarViewModel.BOOK_GENRES_ITEM_ID,
                    title    = "Genres",
                    subtitle = countLabel(bookGenres.size, "genre", "genres"),
                    type     = CrossbarItemType.LIBRARY_SERIES,
                )
            )
        }
        val series = bookSeries
        if (series.isNotEmpty()) {
            add(
                CrossbarItem(
                    id       = BOOK_SERIES_ITEM_ID,
                    title    = "Series",
                    subtitle = countLabel(series.size, "series", "series"),
                    type     = CrossbarItemType.LIBRARY_SERIES,
                )
            )
        }
        add(
            CrossbarItem(
                id       = ALL_BOOKS_ITEM_ID,
                title    = "Books",
                subtitle = countLabel(totalBooks, "book", "books"),
                coverUri = MEMORY_CARD_ASSET_URI,
                type     = CrossbarItemType.MEMORY_CARD,
            )
        )
        // the settings row is last, as in every other column (owner, 2026-10-08)
        add(
            CrossbarItem(
                id       = mediaFoldersItemId(MediaRootKind.BOOK),
                title    = "Folders",
                subtitle = countLabel(shelves.size, "folder", "folders"),
                type          = CrossbarItemType.MEDIA_ROOT,
                mediaRootKind = MediaRootKind.BOOK,
            )
        )
    }.withColumnCovers(mediaCovers.books)
}

internal fun mediaColumn(
    sections: List<CrossbarItem>,
    apps: List<CrossbarItem>,
    addRows: List<CrossbarItem>,
): List<CrossbarItem> = apps + sections + collapseAddRows(addRows)

// the rows that add a media folder; they go with the browse rows when a category has no folder
internal val ADD_FOLDER_ITEM_IDS = setOf(
    CrossbarViewModel.ADD_MUSIC_FOLDER_ITEM_ID,
    CrossbarViewModel.ADD_VIDEOS_ITEM_ID,
    CrossbarViewModel.ADD_PHOTO_LIBRARY_ITEM_ID,
    CrossbarViewModel.ADD_BOOK_FOLDER_ITEM_ID,
)

// owner, 2026-10-05: a media category with no folder stays on the bar with only its apps and Add Apps;
// its browse rows, Folders, Add Folder and search go (folders are added in Settings > Library)
internal fun folderlessColumn(apps: List<CrossbarItem>, addRows: List<CrossbarItem>): List<CrossbarItem> =
    apps + addRows.filterNot { it.id in ADD_FOLDER_ITEM_IDS }

internal fun collapseAddRows(rows: List<CrossbarItem>): List<CrossbarItem> = when {
    rows.size <= 1 -> rows
    else -> listOf(
        CrossbarItem(
            id       = ADD_MENU_ITEM_ID,
            title    = "Add",
            subtitle = rows.joinToString("  ·  ") { it.title.removePrefix("Add ") },
            type     = CrossbarItemType.ADD_ACTION,
        )
    )
}

data class MusicGroup(
    val key: String,
    val name: String,
    val subtitle: String,
    val trackCount: Int,
    val artUri: String?,
)

internal fun String?.musicGroupKey(): String = this?.trim()?.lowercase().orEmpty()

private const val CreditSeparator = ", "

internal fun List<MusicTrack>.soloCredits(): Set<String> =
    mapNotNullTo(mutableSetOf()) { it.primaryArtist.musicGroupKey().ifEmpty { null } }

internal fun MusicTrack.actsUnder(soloCredits: Set<String>): List<String> {
    val credit = primaryArtist?.trim()?.ifBlank { null } ?: return listOf("")
    val parts = credit.split(CreditSeparator).map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return listOf(credit)

    val acts = mutableListOf<String>()
    val pending = mutableListOf<String>()
    fun flush() {
        if (pending.isNotEmpty()) { acts += pending.joinToString(CreditSeparator); pending.clear() }
    }
    parts.forEach { part ->
        if (part.musicGroupKey() in soloCredits) { flush(); acts += part } else pending += part
    }
    flush()
    return acts
}

internal fun List<MusicTrack>.artistGroups(): List<MusicGroup> {
    val solo = soloCredits()
    return musicGroups({ it.actsUnder(solo) }, "Unknown Artist") { tracks ->
        countLabel(tracks.size, "track", "tracks")
    }
}

internal fun List<MusicTrack>.tracksByArtistKey(key: String): List<MusicTrack> {
    val solo = soloCredits()
    return filter { track -> track.actsUnder(solo).any { it.musicGroupKey() == key } }
}

// owner, 2026-10-08: the tracks by genre, the owner's own first; untagged tracks gather under No Genre
internal fun List<MusicTrack>.genreGroups(): List<MusicGroup> =
    musicGroups({ listOf(it.genreName) }, "No Genre") { tracks -> countLabel(tracks.size, "track", "tracks") }

internal fun List<MusicTrack>.albumGroups(): List<MusicGroup> =
    musicGroups({ listOf(it.album) }, "Unknown Album") { tracks ->
        val artists = tracks.mapNotNull { it.primaryArtist?.trim()?.ifBlank { null } }.distinct()
        listOfNotNull(
            when (artists.size) {
                0    -> null
                1    -> artists.single()
                else -> "Various Artists"
            },
            countLabel(tracks.size, "track", "tracks"),
        ).joinToString("  ·  ")
    }

private fun List<MusicTrack>.musicGroups(
    tag: (MusicTrack) -> List<String?>,
    unknownName: String,
    subtitle: (List<MusicTrack>) -> String,
): List<MusicGroup> =
    flatMap { track -> tag(track).map { it to track } }
        .groupBy { (value, _) -> value.musicGroupKey() }
        .map { (key, entries) ->
            val tracks = entries.map { it.second }
            MusicGroup(
                key = key,
                name = entries.firstNotNullOfOrNull { it.first?.trim()?.ifBlank { null } } ?: unknownName,
                subtitle = subtitle(tracks),
                trackCount = tracks.size,
                artUri = tracks.firstNotNullOfOrNull { it.artUri },
            )
        }

        .sortedWith(compareBy({ it.key.isEmpty() }, { it.name.lowercase() }))

internal fun List<MusicTrack>.toMusicItems(): List<CrossbarItem> = map { track ->
    CrossbarItem(
        id            = "mt_${track.id}",
        title         = track.displayTitle,
        subtitle      = musicRowSubtitle(track.artist, track.album, track.durationMs),
        type          = CrossbarItemType.MUSIC_TRACK,
        mediaUri      = track.uri,
        mimeType      = track.mimeType,
        coverUri      = track.artUri,
        musicFolderId = track.folderId,
        musicGroupKey = track.album.musicGroupKey().ifEmpty { null },
    )
}

internal fun List<MusicTrack>.recentMusicRows(): List<Pair<Long, CrossbarItem>> {
    val rows = mutableListOf<Pair<Long, CrossbarItem>>()
    // an album played in two separate runs makes two rows; the second gets its own id, as a list may not hold
    // one key twice (the Thor crashed on "mg_alb_paper radio")
    val runsOfAlbum = mutableMapOf<String, Int>()
    var i = 0
    while (i < size) {
        val key = this[i].album.musicGroupKey()

        var end = i + 1
        if (key.isNotEmpty()) {
            while (end < size && this[end].album.musicGroupKey() == key) end++
        }
        val run = subList(i, end)
        rows += if (run.size == 1) {
            (run[0].lastPlayedAt ?: 0L) to run.toMusicItems().single()
        } else {
            val name = run.firstNotNullOfOrNull { it.album?.trim()?.ifBlank { null } } ?: "Album"
            val nth = runsOfAlbum.merge(key, 1, Int::plus)!!
            run.maxOf { it.lastPlayedAt ?: 0L } to CrossbarItem(
                id            = "$RECENT_ALBUM_ID_PREFIX$key" + if (nth > 1) "#$nth" else "",
                title         = name,
                subtitle      = countLabel(run.size, "track", "tracks"),
                coverUri      = run.firstNotNullOfOrNull { it.artUri },
                musicGroupKey = key,
                type          = CrossbarItemType.MUSIC_GROUP,
            )
        }
        i = end
    }
    return rows
}

// owner, 2026-10-08: a column's own folder row is that column's settings, named after it ("Music Settings",
// "Emulation Settings"), not "Folders"
internal fun columnSettingsTitle(columnName: String?): String = "${columnName?.takeIf { it.isNotBlank() } ?: "Library"} Settings"

internal val CrossbarItem.isColumnFoldersRow: Boolean
    get() = mediaRootKind != null && id == CrossbarViewModel.mediaFoldersItemId(mediaRootKind)
