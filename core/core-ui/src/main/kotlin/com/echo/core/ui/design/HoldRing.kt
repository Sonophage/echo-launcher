package com.echo.core.ui.design

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.withTimeoutOrNull

// how long A, Y or a finger must stay down before anything launches (owner: every launch holds;
// kit: "Hold A; the ring fills, then launches")
// owner, 2026-10-04: long enough to read the card that rises with the hold
const val LAUNCH_HOLD_MS = 1000L

// fills over holdMs while A is held; empties quickly when A comes up early. It always starts empty, even
// when first composed mid-hold (the footer's card appears only once the hold starts)
@Composable
fun holdProgress(holding: Boolean, holdMs: Long): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(holding, holdMs) {
        if (holding) {
            progress.animateTo(1f, tween(((1f - progress.value) * holdMs).toInt(), easing = LinearEasing))
        } else {
            progress.animateTo(0f, tween(120, easing = LinearEasing))
        }
    }
    return progress.value
}

// the kit's progress ring, drawn round a circular glyph
fun Modifier.holdRing(progress: Float, color: Color, track: Color, stroke: Dp): Modifier = drawBehind {
    val w = stroke.toPx()
    val topLeft = Offset(w / 2, w / 2)
    val arcSize = Size(size.width - w, size.height - w)
    drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
    if (progress > 0f) drawArc(color, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
}

// the same progress traced round the edge of a pill button
fun Modifier.holdOutline(progress: Float, color: Color, stroke: Dp): Modifier = drawWithContent {
    drawContent()
    if (progress <= 0f) return@drawWithContent
    val w = stroke.toPx()
    val pill = Path().apply {
        addRoundRect(RoundRect(w / 2, w / 2, size.width - w / 2, size.height - w / 2, CornerRadius(size.height / 2 - w / 2)))
    }
    val measure = PathMeasure().apply { setPath(pill, false) }
    val trace = Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
    drawPath(trace, color, style = Stroke(w, cap = StrokeCap.Round))
}

// touch's hold-to-launch: a press shorter than holdMs does nothing; pressing reports the finger down so the ring can fill
@Composable
fun Modifier.pressAndHold(holdMs: Long, label: String, onPressing: (Boolean) -> Unit, onHeld: () -> Unit): Modifier {
    val pressing by rememberUpdatedState(onPressing)
    val held by rememberUpdatedState(onHeld)
    return semantics {
        role = Role.Button
        onClick(label) { held(); true }
    }.pointerInput(holdMs) {
        detectTapGestures(onPress = {
            pressing(true)
            val released = withTimeoutOrNull(holdMs) { tryAwaitRelease() }
            pressing(false)
            if (released == null) held()
        })
    }
}

// kit "Echo · press": two rings pulse out from an orb and fade over 400 ms; bump trigger to fire
@Composable
fun Modifier.echoPulse(trigger: Int, color: Color): Modifier {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(400, easing = LinearEasing))
    }
    return drawBehind {
        val p = pulse.value
        if (p >= 1f) return@drawBehind
        val r = size.minDimension / 2
        val w = 1.5.dp.toPx()
        drawCircle(color.copy(alpha = 0.4f * (1 - p)), r + r * 0.5f * p, style = Stroke(w))
        drawCircle(color.copy(alpha = 0.18f * (1 - p)), r + r * p, style = Stroke(w))
    }
}
