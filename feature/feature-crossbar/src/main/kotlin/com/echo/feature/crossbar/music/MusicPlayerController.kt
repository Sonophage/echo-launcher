package com.echo.feature.crossbar.music

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.echo.core.domain.model.MusicTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class MusicPlaybackState(
    val track: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val durationMs: Int = 0,
    val index: Int = 0,
    val queueSize: Int = 0,
    val isPrepared: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val upNext: List<IndexedValue<MusicTrack>> = emptyList(),
)

@Singleton
class MusicPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var queue: List<MusicTrack> = emptyList()
    private var order = PlayOrder(0, 0)
    private var repeat = RepeatMode.OFF
    private var failedInARow = 0
    private val index: Int get() = order.current
    private var tickJob: Job? = null

    private val _state = MutableStateFlow(MusicPlaybackState())
    val state: StateFlow<MusicPlaybackState> = _state

    private val _positionMs = MutableStateFlow(0)
    val positionMs: StateFlow<Int> = _positionMs

    var onTrackStarted: ((MusicTrack) -> Unit)? = null

    fun setQueue(tracks: List<MusicTrack>, startIndex: Int) {
        queue = tracks
        order = PlayOrder(tracks.size, startIndex, shuffle = order.shuffled)
        playCurrent()
    }

    fun playPause() {
        val p = player ?: return
        runCatching { if (p.isPlaying) p.pause() else p.start() }

        if (p.isPlaying) startTicker() else { tickJob?.cancel(); tickJob = null }
        emit()
    }

    fun next() = advance(auto = false)

    private fun advance(auto: Boolean) {
        if (order.advance(auto, repeat) != null) playCurrent() else seekTo(0)
    }

    private fun skipFailed() {
        if (++failedInARow >= queue.size) stop() else next()
    }

    fun prev() {
        if ((player?.currentPosition ?: 0) > 3000 || order.back() == null) seekTo(0)
        else playCurrent()
    }

    fun toggleShuffle() {
        order.setShuffle(!order.shuffled)
        emit()
    }

    fun cycleRepeat() {
        repeat = repeat.next()
        emit()
    }

    fun seekTo(ms: Int) {
        val p = player ?: return
        runCatching { p.seekTo(ms.coerceIn(0, p.duration.coerceAtLeast(0))) }
        emit()
    }

    fun seekBy(deltaMs: Int) {
        val p = player ?: return
        seekTo((p.currentPosition + deltaMs))
    }

    fun stop() {
        tickJob?.cancel(); tickJob = null
        releasePlayer()
        queue = emptyList(); order = PlayOrder(0, 0, shuffle = order.shuffled)
        _state.value = MusicPlaybackState()
        _positionMs.value = 0
    }

    fun currentTrack(): MusicTrack? = queue.getOrNull(index)

    private fun playCurrent() {
        val track = queue.getOrNull(index) ?: return stop()
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener { advance(auto = true) }
            setOnErrorListener { _, what, extra ->
                Timber.w("MediaPlayer error what=$what extra=$extra for ${track.displayTitle}")
                skipFailed(); true
            }
            setOnPreparedListener { mp ->
                failedInARow = 0
                runCatching { mp.start() }
                startTicker()
                emit()
                onTrackStarted?.invoke(track)
            }
            runCatching {
                setDataSource(context, Uri.parse(track.uri))
                prepareAsync()
            }.onFailure {
                Timber.w(it, "Failed to load ${track.displayTitle}; skipping")
                skipFailed()
            }
        }

        _positionMs.value = 0
        _state.value = MusicPlaybackState(
            track = track, isPlaying = false, durationMs = 0,
            index = index, queueSize = queue.size, isPrepared = false,
            shuffle = order.shuffled, repeat = repeat, upNext = upNext(),
        )
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                emit()
                delay(500)
            }
        }
    }

    private fun emit() {
        val p = player
        val track = queue.getOrNull(index)
        _positionMs.value = runCatching { p?.currentPosition ?: 0 }.getOrDefault(0)
        _state.value = MusicPlaybackState(
            track = track,
            isPlaying = runCatching { p?.isPlaying == true }.getOrDefault(false),
            durationMs = runCatching { p?.duration?.coerceAtLeast(0) ?: 0 }.getOrDefault(0),
            index = index,
            queueSize = queue.size,
            isPrepared = p != null,
            shuffle = order.shuffled,
            repeat = repeat,
            upNext = upNext(),
        )
    }

    private fun upNext(): List<IndexedValue<MusicTrack>> =
        order.upcoming(UP_NEXT_COUNT, repeat).mapNotNull { i -> queue.getOrNull(i)?.let { IndexedValue(i, it) } }

    private companion object {
        const val UP_NEXT_COUNT = 2
    }

    private fun releasePlayer() {
        player?.let { p -> runCatching { p.reset(); p.release() } }
        player = null
    }
}
