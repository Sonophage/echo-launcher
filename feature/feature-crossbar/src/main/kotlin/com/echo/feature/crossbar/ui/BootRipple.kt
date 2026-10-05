package com.echo.feature.crossbar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.ui.design.EchoRing
import com.echo.core.ui.design.MarkPose
import com.echo.core.ui.design.drawEchoMark
import com.echo.core.ui.design.drawEchoRings
import com.echo.core.ui.design.easeInOut
import com.echo.core.ui.design.easeOut
import com.echo.core.ui.design.echoRing
import com.echo.core.ui.design.mix
import com.echo.core.ui.design.progress
import com.echo.core.ui.design.ringCentreY
import com.echo.core.ui.wave.WaveLayers
import com.echo.core.ui.wave.WaveStyle
import kotlin.math.PI
import kotlin.math.sin

// owner, 2026-10-05: the boot animation is "B4 Horizon Ripple" from the ECHO Animations v2 design spec. A line
// of light crosses the screen and collapses to a point; the point sends out three echo rings and the mark
// gathers round it. Every number below is the spec's. The owner's later call (same day): the ripple is the
// wave the user picked (PSP, Echo Rings or Echo Arcs), rising from the moment the rings would have left
object BootRipple {
    const val TOTAL_MS = 3500f
    const val SOUND_MS = 850L
    const val EXIT_START_MS = 2900f
    const val SKIP_EXIT_MS = 300f

    // when the exit ends for a skip pressed at skipAt (ms), or the natural end
    fun endMs(skipAt: Float?): Float = if (skipAt == null) TOTAL_MS else minOf(TOTAL_MS, skipAt + SKIP_EXIT_MS)
}

data class BootFrame(
    val mark: MarkPose,
    val lineWidth: Float,
    val lineAlpha: Float,
    val glowRadius: Float,
    val glowAlpha: Float,
    val rings: List<EchoRing>,
    // the user's chosen wave, rising in place of the rings
    val waveAlpha: Float,
    val wordAlpha: Float,
    val wordSpacing: Float,
    // the whole layer, black included; it fades to the home screen behind
    val layerAlpha: Float,
)

// one frame at t ms on a w × h dp screen. A skip at skipAt freezes every part at that frame and runs the exit from
// there over 300 ms, so each frame is a complete picture
fun bootRippleFrame(t: Float, w: Float, h: Float, skipAt: Float? = null): BootFrame {
    val ta = if (skipAt != null) minOf(t, skipAt) else t
    val size = .42f * h
    val k = size / 132f
    val cx = w / 2
    val cy = h / 2
    val ry = cy - 14f * k

    val g = easeOut(progress(ta, 0f, 500f))
    val c = easeInOut(progress(ta, 500f, 850f))
    val line = if (ta < 500f) .7f * w * g else mix(.7f * w, 0f, c)

    val a = easeOut(progress(ta, 700f, 1000f))
    val ring = easeOut(progress(ta, 1150f, 1650f))
    val crescents = easeOut(progress(ta, 1450f, 2100f))
    val rings = listOf(850f, 1100f, 1350f).mapNotNull { echoRing(ta, it, 1800f, cx, ry, 4 * k, .62f * w, .42f) }

    val word = easeOut(progress(ta, 1900f, 2500f))
    var ms = 1f + .012f * sin(progress(ta, 2500f, BootRipple.EXIT_START_MS) * PI.toFloat())
    var exit = easeInOut(progress(t, BootRipple.EXIT_START_MS, BootRipple.TOTAL_MS))
    if (skipAt != null) exit = maxOf(exit, easeInOut(progress(t, skipAt, skipAt + BootRipple.SKIP_EXIT_MS)))
    ms *= 1 + .04f * exit

    return BootFrame(
        mark = MarkPose(
            cx = cx, cy = cy, size = size, ms = ms,
            lx = -24 * (1 - crescents), rx = 24 * (1 - crescents),
            crescentAlpha = easeOut(progress(ta, 1450f, 1950f)),
            ringAlpha = ring, ringScaleX = mix(.85f, 1f, ring), ringScaleY = mix(.85f, 1f, ring),
            dotY = -46 + 46 * easeInOut(progress(ta, 1150f, 1650f)), dotScale = mix(.2f, 1f, a), dotAlpha = a,
        ),
        lineWidth = line,
        lineAlpha = 1 - progress(ta, 780f, 850f),
        glowRadius = .16f * h,
        glowAlpha = a * (1 - .7f * ring),
        rings = rings,
        waveAlpha = easeOut(progress(ta, 850f, 1650f)),
        wordAlpha = .6f * word,
        wordSpacing = mix(10f, 4f, word),
        layerAlpha = 1 - exit,
    )
}

@Composable
internal fun BootRippleAnimation(
    t: Float,
    skipAt: Float?,
    modifier: Modifier = Modifier,
    // the wave the crossbar draws; Off keeps the spec's rings
    waveStyle: WaveStyle = WaveStyle.OFF,
    waveTint: Color = Color.White,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val f = bootRippleFrame(t, maxWidth.value, maxHeight.value, skipAt)
        Box(Modifier.fillMaxSize().alpha(f.layerAlpha)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF04060C)))
            if (waveStyle.drawsWave && f.waveAlpha > 0f) {
                Box(Modifier.fillMaxSize().alpha(f.waveAlpha)) { WaveLayers(waveStyle, waveTint) }
            }
            Canvas(Modifier.fillMaxSize()) {
                val rx = f.mark.cx * density
                val ry = f.mark.ringCentreY() * density
                if (f.glowAlpha > 0f) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent), Offset(rx, ry), f.glowRadius * density),
                        f.glowRadius * density, Offset(rx, ry), alpha = f.glowAlpha.coerceIn(0f, 1f),
                    )
                }
                if (!waveStyle.drawsWave) drawEchoRings(f.rings)
                if (f.lineAlpha > 0f && f.lineWidth > 0f) {
                    val lw = f.lineWidth * density
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, .3f to Color.White, .7f to Color.White, 1f to Color.Transparent, startX = rx - lw / 2, endX = rx + lw / 2),
                        topLeft = Offset(rx - lw / 2, ry - .75f * density), size = Size(lw, 1.5f * density), alpha = f.lineAlpha.coerceIn(0f, 1f),
                    )
                }
                drawEchoMark(f.mark)
            }
            Text(
                "ECHO", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Light, letterSpacing = f.wordSpacing.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).alpha(f.wordAlpha),
            )
        }
    }
}
