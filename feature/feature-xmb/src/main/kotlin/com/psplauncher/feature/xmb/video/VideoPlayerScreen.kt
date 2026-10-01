package com.psplauncher.feature.xmb.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.psplauncher.core.ui.components.PfpHintBar
import com.psplauncher.core.ui.design.DesignUnits
import com.psplauncher.core.ui.design.MediaDesignFrame
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.domain.model.Video
import com.psplauncher.core.ui.components.ControllerPrompt
import com.psplauncher.core.ui.components.ControllerHintStyle
import com.psplauncher.core.ui.components.PfpControllerHints
import com.psplauncher.core.ui.components.ControllerPromptItem
import com.psplauncher.core.ui.theme.menuCursor
import com.psplauncher.core.ui.theme.menuCursorEdge
import kotlinx.coroutines.delay
import timber.log.Timber
import androidx.compose.runtime.collectAsState
import com.psplauncher.core.data.datastore.pfpDataStore
import com.psplauncher.core.data.repository.InterfacePreferences
import kotlinx.coroutines.flow.map

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Suppress("UnsafeOptInUsageError")
private val SCREEN_MODES = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
)

@UnstableApi
@Composable
fun VideoPlayerScreen(
    videos: List<Video>,
    startIndex: Int,
    startPositionMs: Long,
    onSaveResume: (videoId: String, positionMs: Long, durationMs: Long) -> Unit,
    onExit: () -> Unit,
    pendingGamepadAction: GamepadAction?,
    onGamepadActionConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    libraryName: String? = null,
    accentOf: suspend (String?) -> Long? = { null },
) {
    if (videos.isEmpty()) { onExit(); return }
    val context = androidx.compose.ui.platform.LocalContext.current
    val choices by androidx.compose.runtime.remember(context) {
        context.pfpDataStore.data.map(InterfacePreferences::read)
    }.collectAsState(initial = com.psplauncher.core.data.repository.InterfaceChoices())
    val seekStepMs = choices.videoSeekStepSeconds * 1_000L

    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, videos.lastIndex)) }
    val current = videos[index]

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var speedIndex by remember { mutableIntStateOf(SPEEDS.indexOf(1f)) }
    var screenModeIndex by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsPoke by remember { mutableIntStateOf(0) }
    var optionsOpen by remember { mutableStateOf(false) }
    var optionsRow by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var focus by remember { mutableStateOf(VideoControl.PLAY_PAUSE) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var accentArgb by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(current.id) { accentArgb = accentOf(current.customThumbnailUri ?: current.thumbnailUri) }

    val initialSeek = remember { startPositionMs }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) durationMs = duration.coerceAtLeast(0L)
                }
                override fun onPlayerError(error: PlaybackException) {
                    Timber.w(error, "Playback error for ${current.uri}")
                    errorMessage = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Video not found."
                        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Permission denied for this file."
                        PlaybackException.ERROR_CODE_DECODING_FAILED,
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "This video can't be decoded on this device."
                        else -> "Unable to play this video."
                    }
                }
            })
        }
    }

    LaunchedEffect(index) {
        errorMessage = null
        val uri = runCatching { Uri.parse(current.uri) }.getOrNull()
        if (uri == null) { errorMessage = "Video not found."; return@LaunchedEffect }
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        val seek = if (index == startIndex) initialSeek else 0L
        if (seek > 0L) player.seekTo(seek)
        player.playWhenReady = true
    }

    LaunchedEffect(isPlaying, controlsVisible) {
        while (isPlaying || controlsVisible) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(controlsPoke, optionsOpen, isPlaying) {
        if (optionsOpen || !isPlaying) { controlsVisible = true; return@LaunchedEffect }
        controlsVisible = true
        delay(choices.videoControlsHideMs.toLong())
        controlsVisible = false
    }

    val currentRef by rememberUpdatedState(current)
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                onSaveResume(currentRef.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                player.release()
            }
        }
    }

    fun poke() { controlsPoke++ }
    fun seekBy(deltaMs: Long) { player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L)); poke() }
    fun togglePlay() { if (player.isPlaying) player.pause() else player.play(); poke() }
    fun switchTo(newIndex: Int) {
        if (newIndex !in videos.indices) return

        onSaveResume(current.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
        index = newIndex
        poke()
    }
    fun activate(control: VideoControl) {
        when (control) {
            VideoControl.RESTART -> { player.seekTo(0L); poke() }
            VideoControl.PREVIOUS -> switchTo(index - 1)
            VideoControl.BACK -> seekBy(-seekStepMs)
            VideoControl.PLAY_PAUSE -> togglePlay()
            VideoControl.FORWARD -> seekBy(seekStepMs)
            VideoControl.NEXT -> switchTo(index + 1)
            VideoControl.SUBTITLES -> { cycleTrack(player, C.TRACK_TYPE_TEXT, allowOff = true); poke() }
            VideoControl.SPEED -> {
                speedIndex = (speedIndex + 1) % SPEEDS.size
                player.playbackParameters = PlaybackParameters(SPEEDS[speedIndex])
                poke()
            }
            VideoControl.SCREEN_MODE -> { screenModeIndex = (screenModeIndex + 1) % SCREEN_MODES.size; poke() }
        }
    }

    LaunchedEffect(pendingGamepadAction) {
        val action = pendingGamepadAction ?: return@LaunchedEffect
        if (optionsOpen) {
            when (action) {
                GamepadAction.NAVIGATE_UP   -> optionsRow = (optionsRow - 1 + OPTION_COUNT) % OPTION_COUNT
                GamepadAction.NAVIGATE_DOWN -> optionsRow = (optionsRow + 1) % OPTION_COUNT
                GamepadAction.SELECT, GamepadAction.NAVIGATE_RIGHT -> when (optionsRow) {
                    0 -> { speedIndex = (speedIndex + 1) % SPEEDS.size; player.playbackParameters = PlaybackParameters(SPEEDS[speedIndex]) }
                    1 -> cycleTrack(player, C.TRACK_TYPE_TEXT, allowOff = true)
                    2 -> cycleTrack(player, C.TRACK_TYPE_AUDIO, allowOff = false)
                    3 -> screenModeIndex = (screenModeIndex + 1) % SCREEN_MODES.size
                }
                GamepadAction.BACK, GamepadAction.OPEN_CONTEXT_MENU -> optionsOpen = false
                else -> Unit
            }
            onGamepadActionConsumed(); return@LaunchedEffect
        }
        if (errorMessage != null) {
            if (action == GamepadAction.SELECT || action == GamepadAction.BACK) onExit()
            onGamepadActionConsumed(); return@LaunchedEffect
        }
        when (action) {
            GamepadAction.SELECT -> if (!controlsVisible) togglePlay() else activate(focus)
            GamepadAction.BACK -> if (videoBackHides(controlsVisible, isPlaying)) controlsVisible = false else onExit()
            GamepadAction.NAVIGATE_LEFT -> { if (controlsVisible) focus = focus.step(-1); poke() }
            GamepadAction.NAVIGATE_RIGHT -> { if (controlsVisible) focus = focus.step(1); poke() }
            GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN -> poke()
            GamepadAction.PREV_PAGE -> seekBy(-seekStepMs)
            GamepadAction.NEXT_PAGE -> seekBy(seekStepMs)
            GamepadAction.PREV_CATEGORY -> switchTo(index - 1)
            GamepadAction.NEXT_CATEGORY -> switchTo(index + 1)
            GamepadAction.OPEN_CONTEXT_MENU -> { optionsOpen = true; optionsRow = 0 }
            else -> Unit
        }
        onGamepadActionConsumed()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { poke() },
    ) {
        if (errorMessage == null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        this.player = player
                        resizeMode = SCREEN_MODES[screenModeIndex].first
                    }
                },
                update = { it.resizeMode = SCREEN_MODES[screenModeIndex].first },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage!!, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    ControllerPrompt(
                        actions = listOf(GamepadAction.SELECT, GamepadAction.BACK),
                        label = "Go back",
                        labelColor = Color(0xFFB0B0B0),
                        labelStyle = TextStyle(fontSize = 13.sp),
                        glyphSize = 18.dp,
                    )
                }
            }
        }

        if (controlsVisible && errorMessage == null) {
            ControlsOverlay(
                title = current.displayTitle,
                eyebrow = listOfNotNull(libraryName, if (videos.size > 1) "${index + 1} of ${videos.size}" else null)
                    .joinToString("  ·  "),
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                isPlaying = isPlaying,
                speed = SPEEDS[speedIndex],
                screenMode = SCREEN_MODES[screenModeIndex].second,
                seekSeconds = (seekStepMs / 1000).toInt(),
                focus = focus,
                accent = com.psplauncher.core.ui.design.mediaAccent(accentArgb),
                onControl = { control -> focus = control; activate(control) },
                onSeekTo = { ms -> player.seekTo(ms); poke() },
            )
        }

        if (optionsOpen && errorMessage == null) {
            OptionsOverlay(
                selectedRow = optionsRow,
                speed = SPEEDS[speedIndex],
                subtitleLabel = currentTrackLabel(player, C.TRACK_TYPE_TEXT),
                audioLabel = currentTrackLabel(player, C.TRACK_TYPE_AUDIO),
                screenMode = SCREEN_MODES[screenModeIndex].second,
            )
        }
    }
}

private const val OPTION_COUNT = 4

enum class VideoControl {
    RESTART, PREVIOUS, BACK, PLAY_PAUSE, FORWARD, NEXT, SUBTITLES, SPEED, SCREEN_MODE;

    fun step(delta: Int): VideoControl = entries[(ordinal + delta).coerceIn(0, entries.lastIndex)]
}

fun videoBackHides(controlsVisible: Boolean, isPlaying: Boolean): Boolean = controlsVisible && isPlaying

@Composable
private fun ControlsOverlay(
    title: String,
    eyebrow: String,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    isPlaying: Boolean,
    speed: Float,
    screenMode: String,
    seekSeconds: Int,
    focus: VideoControl,
    accent: Color,
    onControl: (VideoControl) -> Unit,
    onSeekTo: (Long) -> Unit,
) = MediaDesignFrame { u ->
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(u.dp(380))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))),
    )

    Column(Modifier.align(Alignment.BottomStart).padding(start = u.dp(64), bottom = u.dp(236)).fillMaxWidth(0.62f)) {
        if (eyebrow.isNotBlank()) Text(eyebrow.uppercase(), style = u.eyebrow(Color.White.copy(alpha = 0.6f)), maxLines = 1)
        Spacer(Modifier.height(u.dp(8)))
        Text(title, style = TextStyle(color = Color.White, fontSize = u.sp(46), fontWeight = FontWeight.Medium, shadow = TitleShadow),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Row(Modifier.align(Alignment.BottomEnd).padding(end = u.dp(64), bottom = u.dp(240)), verticalAlignment = Alignment.Bottom) {
        Text(fmt(positionMs), color = Color.White, fontSize = u.sp(46), fontWeight = FontWeight.Medium,
            style = TextStyle(fontFeatureSettings = "tnum"))
        Spacer(Modifier.width(u.dp(10)))
        Text("/ ${fmt(durationMs)}", color = Color.White.copy(alpha = 0.5f), fontSize = u.sp(20),
            modifier = Modifier.padding(bottom = u.dp(8)))
    }

    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(start = u.dp(64), end = u.dp(64), bottom = u.dp(196))
            .fillMaxWidth()
            .height(u.dp(20))
            .pointerInput(durationMs) {
                detectTapGestures { p -> if (durationMs > 0) onSeekTo((p.x / size.width * durationMs).toLong()) }
            }
            .drawBehind {
                fun frac(ms: Long) = if (durationMs > 0) (ms.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                val y = size.height / 2
                val h = u.dp(4).toPx()
                drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), h, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width * frac(bufferedMs), y), h, StrokeCap.Round)
                drawLine(accent, Offset(0f, y), Offset(size.width * frac(positionMs), y), h, StrokeCap.Round)
                val x = size.width * frac(positionMs)
                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), u.dp(2).toPx())
            },
    )

    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = u.dp(70)),
        horizontalArrangement = Arrangement.spacedBy(u.dp(40)),
        verticalAlignment = Alignment.Top,
    ) {
        VideoControl.entries.forEach { control ->
            if (control == VideoControl.SUBTITLES) {
                Box(Modifier.padding(top = u.dp(0)).width(u.dp(1)).height(u.dp(34)).background(Color.White.copy(alpha = 0.2f)))
            }
            ControlSlot(control, control == focus, isPlaying, speed, screenMode, seekSeconds, accent, u) { onControl(control) }
        }
    }

    PfpHintBar(
        items = listOf(
            ControllerPromptItem(GamepadAction.SELECT, "Select"),
            ControllerPromptItem(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT), "Move"),
            ControllerPromptItem(listOf(GamepadAction.PREV_PAGE, GamepadAction.NEXT_PAGE), "Seek"),
            ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
            ControllerPromptItem(GamepadAction.BACK, if (isPlaying) "Hide" else "Close"),
        ),
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

@Composable
private fun ControlSlot(
    control: VideoControl,
    focused: Boolean,
    isPlaying: Boolean,
    speed: Float,
    screenMode: String,
    seekSeconds: Int,
    accent: Color,
    u: DesignUnits,
    onClick: () -> Unit,
) {
    val label = when (control) {
        VideoControl.RESTART -> "Restart"
        VideoControl.PREVIOUS -> "Previous"
        VideoControl.BACK -> "Back $seekSeconds s"
        VideoControl.PLAY_PAUSE -> if (isPlaying) "Pause" else "Play"
        VideoControl.FORWARD -> "Forward $seekSeconds s"
        VideoControl.NEXT -> "Next"
        VideoControl.SUBTITLES -> "Subtitles"
        VideoControl.SPEED -> "Speed"
        VideoControl.SCREEN_MODE -> screenMode
    }
    val tint = if (focused) Color.White else Color.White.copy(alpha = 0.45f)
    val glyph = if (focused) u.dp(56) else u.dp(34)
    Column(
        Modifier
            .width(if (focused) u.dp(96) else u.dp(64))
            .offset(y = if (focused) -u.dp(14) else 0.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(u.dp(10)),
    ) {
        Box(
            Modifier
                .height(glyph)
                .then(if (focused) Modifier.drawBehind {
                    drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent)), radius = size.maxDimension)
                } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when (control) {
                VideoControl.SPEED -> Text(speedLabel(speed), color = tint, fontSize = if (focused) u.sp(30) else u.sp(20), fontWeight = FontWeight.Bold)
                VideoControl.BACK, VideoControl.FORWARD -> Box(contentAlignment = Alignment.Center) {
                    Icon(if (control == VideoControl.BACK) Icons.Filled.Replay else Icons.Filled.Refresh, label,
                        tint = tint, modifier = Modifier.size(glyph))
                    Text("$seekSeconds", color = tint, fontSize = u.sp(if (focused) 13 else 8), fontWeight = FontWeight.Bold)
                }
                else -> Icon(controlIcon(control, isPlaying), label, tint = tint, modifier = Modifier.size(glyph))
            }
        }
        if (focused) {
            Text(label, style = TextStyle(color = Color.White, fontSize = u.sp(16), fontWeight = FontWeight.SemiBold, shadow = TitleShadow), maxLines = 1)
        }
    }
}

private fun controlIcon(control: VideoControl, isPlaying: Boolean): ImageVector = when (control) {
    VideoControl.RESTART -> Icons.Filled.RestartAlt
    VideoControl.PREVIOUS -> Icons.Filled.SkipPrevious
    VideoControl.PLAY_PAUSE -> if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
    VideoControl.NEXT -> Icons.Filled.SkipNext
    VideoControl.SUBTITLES -> Icons.Filled.Subtitles
    VideoControl.SCREEN_MODE -> Icons.Filled.AspectRatio
    VideoControl.BACK -> Icons.Filled.Replay
    VideoControl.FORWARD -> Icons.Filled.Refresh
    VideoControl.SPEED -> Icons.Filled.Speed
}

private fun speedLabel(speed: Float): String =
    (if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()) + "×"

private val TitleShadow = Shadow(Color(0xBF000000), Offset(0f, 2f), 4f)

@Composable
private fun OptionsOverlay(
    selectedRow: Int,
    speed: Float,
    subtitleLabel: String,
    audioLabel: String,
    screenMode: String,
) {
    val rows = listOf(
        "Playback Speed" to "${speed}×",
        "Subtitles" to subtitleLabel,
        "Audio Track" to audioLabel,
        "Screen Mode" to screenMode,
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .padding(40.dp)
                .width(320.dp)
                .background(Color(0xF0101018), RoundedCornerShape(14.dp))
                .padding(vertical = 16.dp),
        ) {
            Text(
                "Options",
                color = menuCursorEdge(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuCursor(i == selectedRow)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, color = Color.White, fontSize = 15.sp)
                    Text(value, color = menuCursorEdge(), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            PfpControllerHints(
                items = listOf(
                    ControllerPromptItem(
                        listOf(GamepadAction.SELECT, GamepadAction.NAVIGATE_RIGHT),
                        "Change",
                    ),
                    ControllerPromptItem(
                        listOf(GamepadAction.OPEN_CONTEXT_MENU, GamepadAction.BACK),
                        "Close",
                    ),
                ),
                style = ControllerHintStyle.OVERLAY,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

@UnstableApi
private fun cycleTrack(player: Player, trackType: Int, allowOff: Boolean) {
    val groups = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
    if (groups.isEmpty()) return

    val choices = buildList {
        if (allowOff) add(null)
        groups.forEach { g -> for (t in 0 until g.length) if (g.isTrackSupported(t)) add(g to t) }
    }
    if (choices.isEmpty()) return

    val currentIdx = choices.indexOfFirst { choice ->
        choice != null && choice.first.isTrackSelected(choice.second)
    }.let { if (it < 0 && allowOff) 0 else it }
    val next = choices[(currentIdx + 1).mod(choices.size)]
    val params = player.trackSelectionParameters.buildUpon()
    if (next == null) {
        params.setTrackTypeDisabled(trackType, true)
    } else {
        params.setTrackTypeDisabled(trackType, false)
        params.setOverrideForType(TrackSelectionOverride(next.first.mediaTrackGroup, next.second))
    }
    player.trackSelectionParameters = params.build()
}

@UnstableApi
private fun currentTrackLabel(player: Player, trackType: Int): String {
    val groups = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
    if (groups.isEmpty()) return if (trackType == C.TRACK_TYPE_TEXT) "None" else "Default"
    val selected = groups.flatMap { g -> (0 until g.length).mapNotNull { t -> if (g.isTrackSelected(t)) g.getTrackFormat(t) else null } }
        .firstOrNull()
    return when {
        selected == null && trackType == C.TRACK_TYPE_TEXT -> "Off"
        selected == null -> "Default"
        else -> selected.language?.uppercase() ?: selected.label ?: "Track"
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
