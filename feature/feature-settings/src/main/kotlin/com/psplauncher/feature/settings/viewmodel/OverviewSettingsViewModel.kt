package com.psplauncher.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psplauncher.core.domain.model.primaryArtist
import com.psplauncher.core.domain.repository.BookRepository
import com.psplauncher.core.domain.repository.GameRepository
import com.psplauncher.core.domain.repository.MusicRepository
import com.psplauncher.core.domain.repository.VideoRepository
import com.psplauncher.feature.artwork.api.ArtworkRepository
import com.psplauncher.feature.artwork.api.ArtworkStatus
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
    val lastPlayed: List<OverviewCover> = emptyList(),
)

internal fun overviewCounts(
    games: List<com.psplauncher.core.domain.model.Game>,
    tracks: List<com.psplauncher.core.domain.model.MusicTrack>,
    books: List<com.psplauncher.core.domain.model.Book>,
    videos: Int,
    videoCollections: Int,
): OverviewCounts {
    val real = games.filter { it.contentType == com.psplauncher.core.domain.model.GameContentType.GAME }
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

private const val PC_PLATFORM = "windows"

data class OverviewUiState(
    val counts: OverviewCounts = OverviewCounts(),
    val artwork: ArtworkStatus = ArtworkStatus(),

    val artworkCacheBytes: Long? = null,
    val loading: Boolean = true,
)

@HiltViewModel
class OverviewSettingsViewModel @Inject constructor(
    gameRepository: GameRepository,
    musicRepository: MusicRepository,
    bookRepository: BookRepository,
    videoRepository: VideoRepository,
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
            }.collect { counts -> _state.update { it.copy(counts = counts, loading = false) } }
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
