package com.echo.feature.launcher

import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.domain.model.UiMediaSlot
import com.echo.core.ui.media.UiMediaAudioPlayer
import com.echo.core.ui.media.resolveGameBootAudio
import com.echo.themekit.UiMediaLimits
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

data class GameBootRequest(
    val gameTitle: String,
    val videoPath: String? = null,
    val audioPath: String? = null,

    val coverArt: String? = null,
    val style: com.echo.core.data.repository.GameBootStyle = com.echo.core.data.repository.GameBootStyle.DISC,
    // for Lens: the wide art that fills the screen and the card it starts from. Its colour is the waves'
    // (owner, 2026-10-05), which the crossbar knows
    val backdropArt: String? = null,
    val cardArt: String? = null,
)

// when the launch sound starts in each built-in animation
internal fun gameBootSoundDelayMs(style: com.echo.core.data.repository.GameBootStyle): Long = when (style) {
    com.echo.core.data.repository.GameBootStyle.DISC -> com.echo.core.ui.components.DiscCeremony.DiscOutStartMs.toLong()
    com.echo.core.data.repository.GameBootStyle.LENS -> com.echo.core.ui.components.LensCeremony.SOUND_MS.toLong()
}

@Singleton
class GameBootGate @Inject constructor(
    private val preferences: GameBootPreferences,
    private val uiMedia: UiMediaStore,
    private val audioPlayer: UiMediaAudioPlayer,

    @LaunchDispatcherScope private val scope: CoroutineScope,
) {
    private var pendingSound: Job? = null

    private val _active = MutableStateFlow<GameBootRequest?>(null)

    val active: StateFlow<GameBootRequest?> = _active.asStateFlow()

    @Volatile
    private var completion: CompletableDeferred<Unit>? = null

    val isActive: Boolean get() = _active.value != null

    suspend fun awaitPresentation(gameTitle: String, coverArt: String? = null, backdropArt: String? = null, cardArt: String? = null) {
        if (!preferences.gameBootEnabledFlow.first()) return
        if (isActive) {
            Timber.d("GameBoot already presenting — ignoring a second request for $gameTitle")
            return
        }
        val done = CompletableDeferred<Unit>()
        completion = done
        val (video, audio) = withContext(Dispatchers.IO) {
            val customVideo = uiMedia.pathFor(UiMediaSlot.GAMEBOOT_VIDEO)
            customVideo to resolveGameBootAudio(
                customVideoPath = customVideo,
                customAudioPath = uiMedia.pathFor(UiMediaSlot.GAMEBOOT_AUDIO),
            )
        }
        val style = preferences.styleFlow.first()
        _active.value = GameBootRequest(
            gameTitle = gameTitle, videoPath = video, audioPath = audio, coverArt = coverArt,
            style = style, backdropArt = backdropArt, cardArt = cardArt,
        )

        audio?.let { track ->
            if (video != null) {
                audioPlayer.play(uri = track, clipEndMs = UiMediaLimits.GAMEBOOT_SEQUENCE_MS, label = "gameboot")
            } else {
                pendingSound = scope.launch {
                    delay(gameBootSoundDelayMs(style))
                    audioPlayer.play(uri = track, clipEndMs = UiMediaLimits.GAMEBOOT_SEQUENCE_MS, label = "gameboot")
                }
            }
        }
        try {
            withTimeout(TIMEOUT_MS) { done.await() }
        } catch (_: TimeoutCancellationException) {
            Timber.w("GameBoot watchdog fired after ${TIMEOUT_MS}ms — launching anyway")
            clear()
        } finally {
            completion = null
        }
    }

    fun onPresentationFinished() {
        completion?.complete(Unit)
    }

    fun onPresentationDismissed() {
        clear()
    }

    private fun clear() {
        pendingSound?.cancel()
        pendingSound = null
        completion = null
        _active.value = null
    }

    companion object {
        const val TIMEOUT_MS = 13_000L
    }
}
