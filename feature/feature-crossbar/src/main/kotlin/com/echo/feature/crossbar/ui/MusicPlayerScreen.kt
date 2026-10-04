package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.MEDIA_DESIGN_HEIGHT
import com.echo.core.ui.design.MediaDesignFrame
import com.echo.core.ui.design.mediaAccent
import com.echo.core.ui.design.mediaGlow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.primaryArtist
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.feature.crossbar.music.MusicPlaybackState
import com.echo.feature.crossbar.music.RepeatMode
import com.echo.feature.crossbar.viewmodel.formatDuration

private val TitleShadow = Shadow(Color(0xBF000000), Offset(0f, 2f), 4f)

@Composable
fun MusicPlayerScreen(
    state: MusicPlaybackState,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShuffle: () -> Unit = {},
    onRepeat: () -> Unit = {},
    accentArgb: Long? = null,
    onAction: ((GamepadAction) -> Unit)? = null,
) {
    val accent = mediaAccent(accentArgb)
    val track = state.track
    MediaDesignFrame(
        modifier
            .background(Color(0xFF070505))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
    ) { u ->
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(mediaGlow(accent, 0.20f), mediaGlow(accent, 0.06f), Color.Transparent),
            center = Offset(constraints.maxWidth * 0.72f, constraints.maxHeight * 0.28f),
            radius = constraints.maxWidth * 0.7f,
        )))

        val drop = if (u.square) (maxHeight - u.dp(MEDIA_DESIGN_HEIGHT)) / 2 else 0.dp
        Box(Modifier.align(Alignment.TopStart).offset(u.dp(96), u.dp(130) + drop).size(u.dp(400))) {
            Record(track?.artUri, u)
            Box(
                Modifier
                    .size(u.dp(400))
                    .shadow(u.dp(30), RoundedCornerShape(u.dp(14)))
                    .clip(RoundedCornerShape(u.dp(14)))
                    .background(Color(0xFF1E1716))
                    .border(u.dp(1), Color.White.copy(alpha = 0.10f), RoundedCornerShape(u.dp(14))),
                contentAlignment = Alignment.Center,
            ) {
                if (track?.artUri != null) {
                    AsyncImage(track.artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(u.dp(72)))
                }
            }
        }

        Column(Modifier.align(Alignment.TopStart).offset(u.dp(600), u.dp(128) + drop).width(u.dp(584))) {
            val position = if (state.queueSize > 1) "  ·  ${state.index + 1} of ${state.queueSize}" else ""
            Text("NOW PLAYING$position".uppercase(), style = u.eyebrow(), maxLines = 1)
            Spacer(Modifier.height(u.dp(18)))
            Text(
                track?.displayTitle ?: "Nothing playing",
                style = TextStyle(color = Color.White, fontSize = u.sp(46), lineHeight = u.sp(50), fontWeight = FontWeight.Medium, shadow = TitleShadow),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            track?.primaryArtist?.let {
                Spacer(Modifier.height(u.dp(10)))
                Text(it, color = Color.White.copy(alpha = 0.82f), fontSize = u.sp(20), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            track?.album?.let {
                Spacer(Modifier.height(u.dp(4)))
                Text(it, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(u.dp(38)))
            PlayerProgress(state.durationMs, accent, u, onSeekTo)

            Spacer(Modifier.height(u.dp(26)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(22))) {
                RoundIcon(Icons.Filled.Shuffle, if (state.shuffle) "Shuffle on" else "Shuffle off", u.dp(48), u.dp(22),
                    if (state.shuffle) accent else Color.White.copy(alpha = 0.5f), onShuffle)
                RoundIcon(Icons.Filled.SkipPrevious, "Previous track", u.dp(56), u.dp(30), Color.White, onPrev)
                Box(
                    Modifier
                        .size(u.dp(76))
                        .drawBehind {
                            drawCircle(accent, radius = size.minDimension / 2 + u.dp(5).toPx())
                            drawCircle(Color(0xFF120D0C), radius = size.minDimension / 2 + u.dp(3).toPx())
                        }
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF120D0C), modifier = Modifier.size(u.dp(34)),
                    )
                }
                RoundIcon(Icons.Filled.SkipNext, "Next track", u.dp(56), u.dp(30), Color.White, onNext)
                RoundIcon(
                    if (state.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Repeat ${state.repeat.name.lowercase()}", u.dp(48), u.dp(22),
                    if (state.repeat == RepeatMode.OFF) Color.White.copy(alpha = 0.5f) else accent, onRepeat,
                )
            }

            if (state.upNext.isNotEmpty()) {
                Spacer(Modifier.height(u.dp(34)))
                Text("UP NEXT", style = u.eyebrow())
                Spacer(Modifier.height(u.dp(10)))
                state.upNext.forEachIndexed { row, (queueIndex, next) ->
                    val fade = if (row == 0) 1f else 0.75f
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (row == 0 && state.upNext.size > 1) Modifier.drawBehind {
                                drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, size.height), Offset(size.width, size.height), 1f)
                            } else Modifier)
                            .padding(vertical = u.dp(9)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(u.dp(16)),
                    ) {
                        Text("${queueIndex + 1}", color = Color.White.copy(alpha = 0.4f), fontSize = u.sp(13), modifier = Modifier.width(u.dp(30)))
                        Text(next.displayTitle, color = Color.White.copy(alpha = fade), fontSize = u.sp(16), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        next.primaryArtist?.let {
                            Text(it, color = Color.White.copy(alpha = 0.55f * fade), fontSize = u.sp(14), maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.6f, fill = false))
                        }
                        next.durationMs?.let {
                            Text(formatDuration(it), color = Color.White.copy(alpha = 0.45f * fade), fontSize = u.sp(13),
                                maxLines = 1, modifier = Modifier.widthIn(min = u.dp(48)))
                        }
                    }
                }
            }
        }

        EchoHintBar(
            items = listOf(
                ControllerPromptItem(GamepadAction.SELECT, "Play / Pause"),
                ControllerPromptItem(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT), "Track"),
                ControllerPromptItem(listOf(GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN), "Seek"),
                ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
                ControllerPromptItem(GamepadAction.BACK, "Close"),
            ),
            onAction = onAction,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Record(artUri: String?, u: DesignUnits) {
    val slide = remember(artUri) { Animatable(0f) }
    LaunchedEffect(artUri) {
        slide.animateTo(1f, tween(RECORD_SLIDE_MS, delayMillis = RECORD_SLIDE_DELAY_MS, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .offset { IntOffset(u.dp(92 * slide.value).roundToPx(), u.dp(14).roundToPx()) }
            .size(u.dp(372))
            .shadow(u.dp(20), CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(
                0f to Color(0xFF1B1717), 0.16f to Color(0xFF1B1717), 0.165f to Color(0xFF3A3434),
                0.175f to Color(0xFF3A3434), 0.18f to Color(0xFF0D0B0B), 1f to Color(0xFF0D0B0B),
            ))
            .drawBehind {
                val groove = Stroke(1f)
                listOf(22, 52, 84).forEach { inset ->
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2 - u.dp(inset).toPx(), style = groove)
                }
                drawCircle(Color(0xFF050404), radius = u.dp(5).toPx())
            },
    )
}

@Composable
private fun PlayerProgress(durationMs: Int, accent: Color, u: DesignUnits, onSeekTo: (Int) -> Unit) {
    val positionMs = ownPositionMs()
    Scrubber(positionMs.toInt(), durationMs, accent, u, onSeekTo)
    Spacer(Modifier.height(u.dp(10)))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val times = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(13), fontFeatureSettings = "tnum")
        Text(formatDuration(positionMs), style = times)
        Text(formatDuration(durationMs.toLong()), style = times)
    }
}

@Composable
private fun Scrubber(positionMs: Int, durationMs: Int, accent: Color, u: DesignUnits, onSeekTo: (Int) -> Unit) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(u.dp(14))
            .pointerInput(durationMs) {
                detectTapGestures { p ->
                    if (durationMs > 0) onSeekTo((p.x / size.width * durationMs).toInt())
                }
            }
            .drawBehind {
                val y = size.height / 2
                val h = u.dp(4).toPx()
                drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), h, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(accent, Offset(0f, y), Offset(size.width * fraction, y), h, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val x = size.width * fraction
                drawCircle(Color.White.copy(alpha = 0.15f), radius = u.dp(11).toPx(), center = Offset(x, y))
                drawCircle(Color.White, radius = u.dp(7).toPx(), center = Offset(x, y))
            },
    )
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, box: Dp, glyph: Dp, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(box).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(glyph))
    }
}

private const val RECORD_SLIDE_MS = 700

private const val RECORD_SLIDE_DELAY_MS = 250
