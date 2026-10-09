package com.echo.core.ui.wave

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

enum class WaveDesign(val label: String) {
    PSP("PSP"),
    ECHO_RINGS("Echo Rings"),
    ECHO_ARCS("Echo Arcs"),
    ;

    companion object {
        // the stored name, or PSP when none is set or it is not one ECHO knows
        fun of(name: String?): WaveDesign = entries.firstOrNull { it.name == name } ?: PSP
    }
}

val LocalWaveDesign = staticCompositionLocalOf { WaveDesign.PSP }

data class EchoRippleSpec(
    val sources: List<Pair<Float, Float>>,
    val rMin: Float,
    val rSpan: Float,
    val rings: Int,
    val speed: Float,
    val offset: Float,
    val bandY: Float,
    val bandH: Float,
    val gain: Float,
    val dot: Boolean,
)

internal val EchoRings = EchoRippleSpec(
    sources = listOf(0.30f to 0.60f, 0.50f to 0.60f, 0.70f to 0.60f),
    rMin = 0f, rSpan = 0.85f, rings = 4, speed = 0.05f, offset = 0.21f,
    bandY = 0.60f, bandH = 0.24f, gain = 0.45f, dot = true,
)

internal val EchoArcs = EchoRippleSpec(
    sources = listOf(0.18f to 1.55f, 0.50f to 1.75f, 0.82f to 1.55f),
    rMin = 0.75f, rSpan = 1.1f, rings = 5, speed = 0.035f, offset = 0.17f,
    bandY = 0.66f, bandH = 0.26f, gain = 0.32f, dot = false,
)

internal fun WaveDesign.rippleSpec(): EchoRippleSpec? = when (this) {
    WaveDesign.PSP -> null
    WaveDesign.ECHO_RINGS -> EchoRings
    WaveDesign.ECHO_ARCS -> EchoArcs
}

internal fun ripplePhase(time: Float, source: Int, ring: Int, spec: EchoRippleSpec): Float {
    val p = time * spec.speed + source * spec.offset + ring.toFloat() / spec.rings
    return p - floor(p)
}

// how bright a ring is at its phase (0 born, 1 gone): the one fade the wave and the orbs share
fun rippleEnvelope(phase: Float): Float = sin(PI.toFloat() * phase).coerceAtLeast(0f).pow(1.5f)

@Composable
internal fun EchoWave(spec: EchoRippleSpec, time: () -> Float, alphaScale: () -> Float, ampScale: Float, tint: Color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        EchoShaderWave(spec, time, alphaScale, ampScale, tint)
    } else {
        EchoFallbackWave(spec, time, alphaScale, ampScale, tint)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun EchoShaderWave(spec: EchoRippleSpec, time: () -> Float, alphaScale: () -> Float, ampScale: Float, tint: Color) {
    val shader = remember {
        runCatching { RuntimeShader(AGSL_ECHO) }
            .onFailure { Timber.e(it, "Echo wave shader did not compile; falling back to the Canvas rings") }
            .getOrNull()
    } ?: return EchoFallbackWave(spec, time, alphaScale, ampScale, tint)
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time())
        shader.setFloatUniform("alphaScale", alphaScale())
        shader.setFloatUniform("waveTint", tint.red, tint.green, tint.blue)
        spec.sources.forEachIndexed { i, (x, y) -> shader.setFloatUniform("s$i", x, y) }
        shader.setFloatUniform("rMin", spec.rMin)
        shader.setFloatUniform("rSpan", spec.rSpan * ampScale.coerceAtLeast(0.65f))
        shader.setFloatUniform("rings", spec.rings.toFloat())
        shader.setFloatUniform("speed", spec.speed)
        shader.setFloatUniform("offset", spec.offset)
        shader.setFloatUniform("band", spec.bandY, spec.bandH)
        shader.setFloatUniform("gain", spec.gain)
        shader.setFloatUniform("dotOn", if (spec.dot) 1f else 0f)
        drawRect(brush = brush)
    }
}

@Composable
private fun EchoFallbackWave(spec: EchoRippleSpec, time: () -> Float, alphaScale: () -> Float, ampScale: Float, tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val t = time()
        val h = size.height
        val w = size.width / h
        val alpha = alphaScale() * spec.gain
        val span = spec.rSpan * ampScale.coerceAtLeast(0.65f)
        spec.sources.forEachIndexed { i, (x, y) ->
            val center = Offset(x * w * h, y * h)
            repeat(spec.rings) { k ->
                val phase = ripplePhase(t, i, k, spec)
                val r = (spec.rMin + phase * span) * h
                val a = (rippleEnvelope(phase) * alpha).coerceIn(0f, 1f)
                drawCircle(tint.copy(alpha = a * 0.25f), r, center, style = Stroke(width = h * 0.03f))
                drawCircle(tint.copy(alpha = a), r, center, style = Stroke(width = h * 0.005f))
            }
        }
    }
}

private const val AGSL_ECHO = """
uniform float2 iResolution;
uniform float  iTime;
uniform float  alphaScale;
uniform float3 waveTint;
uniform float2 s0;
uniform float2 s1;
uniform float2 s2;
uniform float  rMin;
uniform float  rSpan;
uniform float  rings;
uniform float  speed;
uniform float  offset;
uniform float2 band;
uniform float  gain;
uniform float  dotOn;

float ripples(float2 uv, float2 c, float i) {
    float d = length(uv - c);
    float a = 0.0;
    for (int k = 0; k < 5; k++) {
        float fk = float(k);
        float on = step(fk + 0.5, rings) > 0.0 ? 1.0 : 0.0;
        float ph = fract(iTime * speed + i * offset + fk / rings);
        float r = rMin + ph * rSpan;
        float line = clamp(1.0 - abs(d - r) / 0.004, 0.0, 1.0);
        float glow = exp(-pow((d - r) / 0.025, 2.0)) * 0.22;
        a += (line + glow) * pow(max(sin(3.14159265 * ph), 0.0), 1.5) * on;
    }
    return a;
}

half4 main(float2 p) {
    float2 uv = p / iResolution.y;
    float w = iResolution.x / iResolution.y;
    float a = ripples(uv, float2(s0.x * w, s0.y), 0.0)
            + ripples(uv, float2(s1.x * w, s1.y), 1.0)
            + ripples(uv, float2(s2.x * w, s2.y), 2.0);
    float dotD = length(uv - float2(s1.x * w, s1.y));
    a += exp(-pow(dotD / 0.010, 2.0)) * 0.5 * dotOn;
    float b = exp(-pow((uv.y - band.x) / band.y, 2.0));
    a = clamp(a * b * gain * alphaScale, 0.0, 1.0);
    return half4(half3(waveTint) * half(a), half(a));
}
"""
