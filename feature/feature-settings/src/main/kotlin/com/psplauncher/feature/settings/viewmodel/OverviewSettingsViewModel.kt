package com.psplauncher.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class OverviewUiState(
    val games: Int = 0,
    val tracks: Int = 0,
    val books: Int = 0,
    val videos: Int = 0,
    val artwork: ArtworkStatus = ArtworkStatus(),

    val artworkCacheBytes: Long? = null,
    val artChoices: List<String> = emptyList(),
    val loading: Boolean = true,
)

internal fun pickOverviewArt(choices: List<String>, random: kotlin.random.Random): Pair<String?, String?> {
    val two = choices.shuffled(random).take(2)
    return two.getOrNull(0) to two.getOrNull(1)
}

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
            ) { games, tracks, books, videos ->
                OverviewUiState(
                    games = games.size, tracks = tracks.size, books = books.size, videos = videos.size,
                    artChoices = games.mapNotNull { it.artworkUri }.distinct(),
                    loading = false,
                )
            }.collect { counts ->
                _state.update {
                    it.copy(
                        games = counts.games, tracks = counts.tracks, books = counts.books, videos = counts.videos,
                        artChoices = counts.artChoices, loading = false,
                    )
                }
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
