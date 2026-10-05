package com.echo.core.ui.design

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

// The ECHO mark in four parts that move on their own, drawn from the same paths as res/drawable/echo_logo.xml
// (owner, 2026-10-05: the boot and launch animations from the "ECHO Animations v2" design spec). Coordinates
// are in the mark's 132-unit box; the four paths sit inside it after translate(-22, 12)
object EchoMarkPaths {
    val leftCrescent = PathParser().parsePathString("M66.889,18.685 A26,26 0 1,0 66.889,61.315 A30,30 0 0,1 66.889,18.685 Z").toPath()
    val rightCrescent = PathParser().parsePathString("M109.111,18.685 A26,26 0 1,1 109.111,61.315 A30,30 0 0,0 109.111,18.685 Z").toPath()
    val ring = PathParser().parsePathString("M67,40 a21,21 0 1,0 42,0 a21,21 0 1,0 -42,0").toPath()
    val dot = PathParser().parsePathString("M80,86 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0").toPath()

    const val BOX = 132f
    const val RING_LENGTH = 132f
    val ringCentre = Offset(88f, 40f)
    val dotCentre = Offset(88f, 86f)
}

// where each part of the mark is at one moment. Lengths in dp; x and y for the parts in mark units
data class MarkPose(
    val cx: Float,
    val cy: Float,
    // the mark's box, before ms
    val size: Float,
    val ms: Float = 1f,
    val alpha: Float = 1f,
    val lx: Float = 0f,
    val rx: Float = 0f,
    val crescentAlpha: Float = 1f,
    val ringAlpha: Float = 1f,
    val ringScaleX: Float = 1f,
    val ringScaleY: Float = 1f,
    // 0 draws the whole ring, RING_LENGTH draws none of it (the ring draws on from its top)
    val ringOffset: Float = 0f,
    val dotY: Float = 0f,
    val dotScale: Float = 1f,
    val dotAlpha: Float = 1f,
)

// the ring's centre on screen, in dp: the point the echo rings and the light leave from
fun MarkPose.ringCentreY(): Float = cy - 14f * size / EchoMarkPaths.BOX

fun DrawScope.drawEchoMark(p: MarkPose, color: Color = Color.White) {
    if (p.alpha <= 0f) return
    val k = p.size * p.ms / EchoMarkPaths.BOX * density
    translate(p.cx * density - 66f * k, p.cy * density - 66f * k) {
        scale(k, k, pivot = Offset.Zero) {
            translate(-22f, 12f) {
                translate(p.lx, 0f) { drawPath(EchoMarkPaths.leftCrescent, color, alpha = (p.alpha * p.crescentAlpha).coerceIn(0f, 1f)) }
                translate(p.rx, 0f) { drawPath(EchoMarkPaths.rightCrescent, color, alpha = (p.alpha * p.crescentAlpha).coerceIn(0f, 1f)) }
                if (p.ringAlpha > 0f) {
                    scale(p.ringScaleX, p.ringScaleY, pivot = EchoMarkPaths.ringCentre) {
                        rotate(90f, pivot = EchoMarkPaths.ringCentre) {
                            drawPath(
                                EchoMarkPaths.ring, color, alpha = (p.alpha * p.ringAlpha).coerceIn(0f, 1f),
                                style = Stroke(
                                    width = 5f,
                                    cap = StrokeCap.Butt,
                                    pathEffect = if (p.ringOffset > 0f) PathEffect.dashPathEffect(floatArrayOf(EchoMarkPaths.RING_LENGTH, EchoMarkPaths.RING_LENGTH), p.ringOffset) else null,
                                ),
                            )
                        }
                    }
                }
                translate(0f, p.dotY) {
                    scale(p.dotScale, p.dotScale, pivot = EchoMarkPaths.dotCentre) {
                        drawPath(EchoMarkPaths.dot, color, alpha = (p.alpha * p.dotAlpha).coerceIn(0f, 1f))
                    }
                }
            }
        }
    }
}

// one white ring leaving a point and fading as it grows, so it is never cut off mid-flight on screen. In dp
data class EchoRing(val x: Float, val y: Float, val radius: Float, val stroke: Float, val alpha: Float)

fun echoRing(t: Float, start: Float, dur: Float, x: Float, y: Float, r0: Float, r1: Float, op: Float, sw: Float = 1.6f): EchoRing? {
    val w = progress(t, start, start + dur)
    if (w <= 0f || w >= 1f) return null
    return EchoRing(x, y, mix(r0, r1, easeOut(w)), sw * (1 - w) + .4f, op * (1 - w))
}

fun DrawScope.drawEchoRings(rings: List<EchoRing>, color: Color = Color.White) {
    rings.forEach { r ->
        drawCircle(color, r.radius * density, Offset(r.x * density, r.y * density), alpha = r.alpha.coerceIn(0f, 1f), style = Stroke(r.stroke * density))
    }
}

// the spec's timing curves, so the code reads like its tables
fun progress(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)
fun easeOut(x: Float): Float = 1 - (1 - x).pow(3)
fun easeInOut(x: Float): Float = if (x < .5f) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2
fun mix(a: Float, b: Float, x: Float): Float = a + (b - a) * x

// the dot's wait pulse after a hand-off: 40 to 100 percent on a 1200 ms cosine
fun waitPulse(t: Float, t0: Float): Float = if (t < t0) 1f else .4f + .6f * (.5f + .5f * cos(2 * PI.toFloat() * (t - t0) / 1200f))
