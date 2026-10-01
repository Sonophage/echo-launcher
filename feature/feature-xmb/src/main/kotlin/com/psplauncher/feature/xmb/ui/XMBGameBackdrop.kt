package com.psplauncher.feature.xmb.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.psplauncher.core.ui.wave.steppedFrameMs

const val XMB_STILL_SOLID_END = 0.40f

const val XMB_STILL_FADE_END = 0.68f

fun xmbStillOverVideoStops(): Array<Pair<Float, Color>> = arrayOf(
    0f to Color.Black,
    XMB_STILL_SOLID_END to Color.Black,
    XMB_STILL_FADE_END to Color.Transparent,
    1f to Color.Transparent,
)

fun Modifier.xmbStillOverVideo(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(colorStops = xmbStillOverVideoStops()),
            blendMode = BlendMode.DstIn,
        )
    }

const val XMB_BACKDROP_FADE_MS = 700

const val KEN_BURNS_MS = 24_000L

const val KEN_BURNS_ZOOM = 0.08f

data class KenBurnsFrame(val scale: Float, val dxFraction: Float, val dyFraction: Float)

fun kenBurnsFrame(elapsedMs: Long, seed: Int): KenBurnsFrame {
    val t = FastOutSlowInEasing.transform((elapsedMs.toFloat() / KEN_BURNS_MS).coerceIn(0f, 1f))
    val zoomIn = seed and 1 == 0
    val scale = 1f + KEN_BURNS_ZOOM * (if (zoomIn) t else 1f - t)
    val reach = (scale - 1f) / 2f
    val xSign = if (seed and 2 == 0) 1f else -1f
    val ySign = if (seed and 4 == 0) 1f else -1f
    return KenBurnsFrame(scale, xSign * reach * t, ySign * reach * t * 0.5f)
}

@Composable
fun Modifier.kenBurns(key: String, enabled: Boolean): Modifier {
    if (!enabled) return this
    val seed = key.hashCode()
    val frame by produceState(kenBurnsFrame(0L, seed), key) {
        var startMs = -1L
        while (true) {
            val done = withInfiniteAnimationFrameMillis { nowMs ->
                val stepped = steppedFrameMs(nowMs)
                if (startMs < 0L) startMs = stepped
                val elapsed = stepped - startMs
                value = kenBurnsFrame(elapsed, seed)
                elapsed >= KEN_BURNS_MS
            }
            if (done) break
        }
    }
    return graphicsLayer {
        scaleX = frame.scale
        scaleY = frame.scale
        translationX = frame.dxFraction * size.width
        translationY = frame.dyFraction * size.height
    }
}
