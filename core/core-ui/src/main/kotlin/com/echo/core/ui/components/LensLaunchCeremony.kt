package com.echo.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.echo.core.ui.design.MarkPose
import com.echo.core.ui.design.drawEchoMark
import com.echo.core.ui.design.easeInOut
import com.echo.core.ui.design.easeOut
import com.echo.core.ui.design.mix
import com.echo.core.ui.design.progress
import com.echo.core.ui.design.waitPulse
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.wave.WaveLayers
import com.echo.core.ui.wave.WaveStyle
import kotlinx.coroutines.delay
import kotlin.math.hypot

// owner, 2026-10-05: the second game launch style, "L2 Lens" from the ECHO Animations v2 design spec, offered
// beside the disc. The game's art shrinks into the ECHO ring and spins there as a disc; with the launch sound
// the ring opens like a lens and the art fills the screen. Every number below is the spec's
object LensCeremony {
    const val SOUND_MS = 2700
    const val HAND_OFF_MS = 3400
    // the authored part ends at 4750; the frame then holds, its dot pulsing, until the game shows
    const val TOTAL_MS = 6500

    // spin-up 700-1300, constant 0.48 deg/ms to 2300, spin-down to 2700: exactly 720 degrees, so the art rests upright
    fun spinAngle(t: Float): Float {
        val v = 720f / 1500f
        return when {
            t <= 700f -> 0f
            t <= 1300f -> { val x = t - 700f; v * x * x / 1200f }
            t <= 2300f -> v * 300f + v * (t - 1300f)
            t <= 2700f -> { val x = t - 2300f; v * 1300f + v * (x - x * x / 800f) }
            else -> 720f
        }
    }
}

data class LensFrame(
    val dim: Float,
    // the pressed card on its way to the ring, in dp
    val cardX: Float, val cardY: Float, val cardW: Float, val cardH: Float, val cardRadius: Float, val cardAlpha: Float,
    val ringX: Float, val ringY: Float,
    val artRadius: Float, val artAlpha: Float, val artDim: Float, val artScale: Float, val artRotation: Float,
    val discAlpha: Float, val discRadius: Float,
    val glowRadius: Float, val glowAlpha: Float,
    val waveAlpha: Float,
    val mark: MarkPose,
    val titleY: Float, val titleAlpha: Float,
)

// one frame at t ms on a w × h dp screen, starting from a card centred on the screen
fun lensFrame(t: Float, w: Float, h: Float): LensFrame {
    val srcW = .202f * w
    val srcH = .193f * h
    val srcX = (w - srcW) / 2
    val srcY = (h - srcH) / 2

    var size = .36f * h
    val k = size / 132f
    var cy = h / 2 + 14f * k
    val rcx = w / 2
    val rcy = h / 2
    val ir = 18.5f * k
    val rMax = hypot(w / 2, h / 2) + 4

    val a = easeInOut(progress(t, 0f, 700f))
    val d = easeInOut(progress(t, 2700f, 3400f))
    val r = mix(ir, rMax, d)
    val glowR = mix(.1f * h, .8f * h, easeOut(progress(t, 300f, 1500f)))
    val c = easeOut(progress(t, 600f, 1400f))
    val dp = progress(t, 900f, 1400f)

    var mark = MarkPose(
        cx = w / 2, cy = cy, size = size,
        lx = -40 * (1 - c) - 60 * d, rx = 40 * (1 - c) + 60 * d, crescentAlpha = c * (1 - d),
        ringAlpha = if (t < 700f) 0f else 1 - progress(t, 2700f, 3000f),
        ringScaleX = r / ir * (20f / 21f) + 1f / 21f, ringScaleY = r / ir * (20f / 21f) + 1f / 21f,
        ringOffset = 132f * (1 - easeInOut(progress(t, 700f, 1200f))),
        dotY = 30 * (1 - easeOut(dp)), dotAlpha = easeOut(dp) * (1 - d),
    )
    val settled = easeOut(progress(t, 3600f, 4100f))
    if (t > 3400f) {
        // after the hand-off: a small mark low on the art, its dot pulsing while Android brings the game up
        size = .12f * h; cy = .66f * h
        mark = MarkPose(cx = w / 2, cy = cy, size = size, alpha = settled, dotAlpha = waitPulse(t, 4100f))
    }

    return LensFrame(
        dim = .85f * easeOut(progress(t, 0f, 600f)),
        cardX = mix(srcX, rcx - ir, a), cardY = mix(srcY, rcy - ir, a), cardW = mix(srcW, 2 * ir, a), cardH = mix(srcH, 2 * ir, a),
        cardRadius = mix(8f, ir, a), cardAlpha = 1 - progress(t, 600f, 800f),
        ringX = rcx, ringY = rcy,
        artRadius = r, artAlpha = progress(t, 600f, 800f), artDim = .4f * easeOut(progress(t, 3400f, 4000f)),
        artScale = mix(1.15f, 1f, d), artRotation = LensCeremony.spinAngle(t) % 360f,
        discAlpha = progress(t, 600f, 800f) * (1 - progress(t, 2650f, 2750f)), discRadius = ir,
        glowRadius = glowR, glowAlpha = easeOut(progress(t, 300f, 1200f)) * (1 - d),
        waveAlpha = .6f * easeOut(progress(t, 1200f, 2000f)) * (1 - d),
        mark = mark,
        titleY = .36f * h, titleAlpha = settled,
    )
}

@Composable
fun LensLaunchCeremony(
    title: String,
    // a URI string, or anything Coil loads (the launch disc passes an app's icon drawable)
    coverArt: Any?,
    backdropArt: Any?,
    // the colour the crossbar's waves are drawing in; null falls back to ECHO's blue
    accent: Color?,
    waveStyle: WaveStyle,
    onHandOff: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    // the launch disc's own sound, started as the lens opens; GameBoot plays its sound itself
    soundCue: (() -> Unit)? = null,
) {
    val handOff by rememberUpdatedState(onHandOff)
    val finished by rememberUpdatedState(onFinished)
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(LensCeremony.TOTAL_MS.toFloat(), tween(LensCeremony.TOTAL_MS, easing = LinearEasing))
        finished()
    }
    LaunchedEffect(Unit) {
        delay(LensCeremony.HAND_OFF_MS.toLong())
        handOff()
    }
    if (soundCue != null) LaunchedEffect(Unit) {
        delay(LensCeremony.SOUND_MS.toLong())
        soundCue()
    }
    val gc = accent ?: Color(0xFF128BC9)
    val art = (backdropArt ?: coverArt)?.let { if (it is String) rememberArtworkModel(it) else it }
    val card = (coverArt ?: backdropArt)?.let { if (it is String) rememberArtworkModel(it) else it }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val f = lensFrame(clock.value, maxWidth.value, maxHeight.value)
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().alpha(f.dim).background(Color(0xFF04060C)))
            // the art inside the ring, turning as a disc, then opening to the whole screen
            val px = LocalDensity.current.density
            Box(
                Modifier.fillMaxSize().alpha(f.artAlpha)
                    .clip(GenericShape { _, _ -> addOval(Rect(Offset(f.ringX * px, f.ringY * px), f.artRadius * px)) }),
            ) {
                if (art != null) {
                    AsyncImage(
                        model = art, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = f.artScale; scaleY = f.artScale; rotationZ = f.artRotation },
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(gc, Color(0xFF04060C)))))
                }
                Box(Modifier.fillMaxSize().alpha(f.artDim).background(Color(0xFF04060C)))
            }
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(f.ringX * density, f.ringY * density)
                if (f.discAlpha > 0f) {
                    val rd = f.discRadius * density
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color(0xFF04060C), .14f to Color(0xFF04060C), .145f to Color.White.copy(alpha = .25f),
                            .16f to Color.White.copy(alpha = .1f), .30f to Color.White.copy(alpha = .1f), .305f to Color.Transparent,
                            center = centre, radius = rd,
                        ),
                        rd, centre, alpha = f.discAlpha,
                    )
                    // the sheen stays put while the art turns, as light on a real disc does
                    rotate(-70f, centre) {
                        drawCircle(
                            Brush.sweepGradient(
                                0f to Color.Transparent, 30f / 360 to Color.White.copy(alpha = .22f), 70f / 360 to Color.Transparent,
                                180f / 360 to Color.Transparent, 210f / 360 to Color.White.copy(alpha = .16f), 250f / 360 to Color.Transparent,
                                1f to Color.Transparent, center = centre,
                            ),
                            rd, centre, alpha = f.discAlpha,
                        )
                    }
                }
                if (f.glowAlpha > 0f) {
                    val gr = f.glowRadius * density
                    val inner = (f.artRadius / f.glowRadius).coerceIn(0f, .99f)
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.Transparent, inner to Color.Transparent, (inner + .005f).coerceAtMost(.995f) to gc,
                            maxOf(inner + .01f, .45f).coerceAtMost(.999f) to gc.copy(alpha = .33f), 1f to Color.Transparent,
                            center = centre, radius = gr,
                        ),
                        gr, centre, alpha = f.glowAlpha,
                    )
                }
            }
            if (f.waveAlpha > 0f) Box(Modifier.fillMaxSize().alpha(f.waveAlpha)) { WaveLayers(waveStyle, gc) }
            // the pressed card travelling into the ring
            if (f.cardAlpha > 0f && card != null) {
                AsyncImage(
                    model = card, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.offset(f.cardX.dp, f.cardY.dp).size(f.cardW.dp, f.cardH.dp)
                        .clip(RoundedCornerShape(f.cardRadius.dp)).alpha(f.cardAlpha),
                )
            }
            Canvas(Modifier.fillMaxSize()) { drawEchoMark(f.mark) }
            Column(
                Modifier.fillMaxWidth().offset(y = f.titleY.dp).alpha(f.titleAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraLight, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
