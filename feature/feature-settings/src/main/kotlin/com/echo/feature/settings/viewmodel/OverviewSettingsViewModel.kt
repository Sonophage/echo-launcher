package com.echo.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.domain.model.primaryArtist
import com.echo.core.domain.repository.BookRepository
import com.echo.core.domain.repository.GameRepository
import com.echo.core.domain.repository.MusicRepository
import com.echo.core.domain.repository.VideoRepository
import com.echo.feature.artwork.api.ArtworkRepository
import com.echo.feature.artwork.api.ArtworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OverviewCover(val title: String, val artUri: String)

data class OverviewCounts(
    val games: Int = 0,
    val consoles: Int = 0,
    val android: Int = 0,
    val pc: Int = 0,
    val tracks: Int = 0,
    val artists: Int = 0,
    val books: Int = 0,
    val booksOpened: Int = 0,
    val videos: Int = 0,
    val videoCollections: Int = 0,
    val photos: Int = 0,
    val lastPlayed: List<OverviewCover> = emptyList(),
)

internal fun overviewCounts(
    games: List<com.echo.core.domain.model.Game>,
    tracks: List<com.echo.core.domain.model.MusicTrack>,
    books: List<com.echo.core.domain.model.Book>,
    videos: Int,
    videoCollections: Int,
): OverviewCounts {
    val real = games.filter { it.contentType == com.echo.core.domain.model.GameContentType.GAME }
    val pc = real.count { it.platformId == PC_PLATFORM }
    val android = real.count { it.packageName != null && it.platformId != PC_PLATFORM }
    val consoles = real.filter { it.packageName == null && it.platformId != PC_PLATFORM }.map { it.platformId }.distinct().size
    return OverviewCounts(
        games = real.size,
        consoles = consoles,
        android = android,
        pc = pc,
        tracks = tracks.size,
        artists = tracks.mapNotNull { it.primaryArtist }
            .map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().size,
        books = books.size,
        booksOpened = books.count { it.lastOpenedAt != null },
        videos = videos,
        videoCollections = videoCollections,
        lastPlayed = games.filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { g -> (g.iconUri ?: g.artworkUri)?.let { OverviewCover(g.displayTitle, it) } }
            .take(OVERVIEW_FAN_SIZE),
    )
}

internal const val OVERVIEW_FAN_SIZE = 3

enum class OverviewMedia { MUSIC, VIDEO, PHOTOS, BOOKS }

data class OverviewMediaRow(val kind: OverviewMedia, val main: String, val detail: String?)

// owner, 2026-10-05: the Overview's media column, one row per kind, only for the categories on the crossbar
internal fun overviewMediaRows(c: OverviewCounts, shown: Set<String>): List<OverviewMediaRow> = buildList {
    fun n(value: Int, noun: String) = "$value ${if (value == 1) noun else noun + "s"}"
    if (com.echo.core.domain.model.BuiltInCategory.MUSIC in shown) add(OverviewMediaRow(OverviewMedia.MUSIC, n(c.tracks, "track"), n(c.artists, "artist")))
    if (com.echo.core.domain.model.BuiltInCategory.VIDEO in shown) add(OverviewMediaRow(OverviewMedia.VIDEO, n(c.videos, "video"), n(c.videoCollections, "collection")))
    if (com.echo.core.domain.model.BuiltInCategory.PHOTO in shown) add(OverviewMediaRow(OverviewMedia.PHOTOS, n(c.photos, "photo"), null))
    if (com.echo.core.domain.model.BuiltInCategory.LIBRARY in shown) add(OverviewMediaRow(OverviewMedia.BOOKS, n(c.books, "book"), "${c.booksOpened} opened"))
}

private const val PC_PLATFORM = "windows"

data class OverviewUiState(
    val counts: OverviewCounts = OverviewCounts(),
    val artwork: ArtworkStatus = ArtworkStatus(),

    val artworkCacheBytes: Long? = null,
    val loading: Boolean = true,
    // the media categories on the crossbar; the Overview shows only those (owner, 2026-10-05)
    val mediaShown: Set<String> = emptySet(),
)

@HiltViewModel
class OverviewSettingsViewModel @Inject constructor(
    gameRepository: GameRepository,
    musicRepository: MusicRepository,
    bookRepository: BookRepository,
    videoRepository: VideoRepository,
    photoRepository: com.echo.core.domain.repository.PhotoRepository,
    categoryRepository: com.echo.core.data.repository.CategoryRepositoryImpl,
    private val artworkRepository: ArtworkRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OverviewUiState())
    val state: StateFlow<OverviewUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                gameRepository.observeAll(),
                musicRepository.observeAllTracks(),
                bookRepository.observeAllBooks(),
                videoRepository.observeAllVideos(),
                videoRepository.observePlaylists(),
            ) { games, tracks, books, videos, playlists ->
                overviewCounts(games, tracks, books, videos.size, playlists.size)
            }.combine(photoRepository.observeAllPhotos()) { counts, photos -> counts.copy(photos = photos.size) }
                .collect { counts -> _state.update { it.copy(counts = counts, loading = false) } }
        }
        viewModelScope.launch {
            categoryRepository.observeVisible().collect { visible ->
                _state.update { it.copy(mediaShown = visible.map { c -> c.id }.toSet()) }
            }
        }

        viewModelScope.launch {
            runCatching { artworkRepository.computeStatus() }
                .onSuccess { status -> _state.update { it.copy(artwork = status) } }
                .onFailure { Timber.w(it, "Overview could not read the artwork status") }
            runCatching { artworkRepository.cacheSizeBytes() }
                .onSuccess { bytes -> _state.update { it.copy(artworkCacheBytes = bytes) } }
                .onFailure { Timber.w(it, "Overview could not measure the artwork cache") }
        }
    }
}
