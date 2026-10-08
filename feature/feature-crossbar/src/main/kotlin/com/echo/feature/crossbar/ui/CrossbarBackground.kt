package com.echo.feature.crossbar.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.echo.core.ui.motion.MotionWallpaperBackground
import com.echo.core.ui.motion.MotionWallpaperPolicy
import com.echo.core.ui.wave.WaveBackground
import com.echo.core.ui.wave.WaveLayers
import com.echo.core.ui.wave.WaveStyle

fun waveVisible(hasWallpaper: Boolean, waveOverWallpaper: Boolean, style: WaveStyle): Boolean =
    style.drawsWave && (!hasWallpaper || waveOverWallpaper)

@Composable
fun CrossbarBackground(
    waveStyle: WaveStyle,
    customWallpaperPath: String? = null,
    motionWallpaperPath: String? = null,
    motionDecision: MotionWallpaperPolicy.Decision = MotionWallpaperPolicy.Decision.PLAY,

    waveOverWallpaper: Boolean = false,

    wallpaperAccent: Long? = null,

    waveDrawnByCaller: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasWallpaper = customWallpaperPath != null
    val motionPlaying = hasWallpaper && motionWallpaperPath != null &&
        motionDecision != MotionWallpaperPolicy.Decision.POSTER
    val drawsWave = waveVisible(hasWallpaper, waveOverWallpaper, waveStyle) && !waveDrawnByCaller

    Box(modifier.fillMaxSize()) {
        when {
            motionPlaying -> MotionWallpaperBackground(
                posterPath = customWallpaperPath,
                motionPath = motionWallpaperPath,
                decision = motionDecision,
                modifier = Modifier.fillMaxSize(),
            )
            hasWallpaper -> WallpaperBackground(customWallpaperPath, Modifier.fillMaxSize())
            else -> WaveBackground(waveStyle, Modifier.fillMaxSize(), drawWave = drawsWave)
        }

        if (hasWallpaper && drawsWave) {
            WaveOverlay(waveStyle, wallpaperAccent, Modifier.fillMaxSize())
        }
    }
}

// owner, 2026-10-08: the selected item's colour reached the screen only through the wave, so with the wave off the
// crossbar and Recent lost it. Without the wave, the colour rises from the bottom as a soft wash instead
@Composable
fun AccentWash(accentArgb: Long?, modifier: Modifier = Modifier) {
    val target = accentWashColor(accentArgb)
    val color by androidx.compose.animation.animateColorAsState(target, androidx.compose.animation.core.tween(600), label = "accentWash")
    Box(modifier.fillMaxSize().background(
        androidx.compose.ui.graphics.Brush.verticalGradient(0f to Color.Transparent, 0.45f to color.copy(alpha = color.alpha * 0.35f), 1f to color),
    ))
}

// the wash's colour at its strongest: the accent at a third, or nothing when there is none
fun accentWashColor(accentArgb: Long?): Color =
    accentArgb?.let { Color(it or 0xFF000000L).copy(alpha = ACCENT_WASH_ALPHA) } ?: Color.Transparent

const val ACCENT_WASH_ALPHA = 0.32f

private fun waveTintFrom(accentArgb: Long): Color =
    lerp(Color(accentArgb or 0xFF000000L), Color.White, 0.62f)

// the colour the waves draw in for an accent; white when there is none
fun waveColorFor(accentArgb: Long?): Color = accentArgb?.let(::waveTintFrom) ?: Color.White

@Composable
fun WaveOverlay(
    waveStyle: WaveStyle,
    accentArgb: Long?,
    modifier: Modifier,
    speedScale: () -> Float = { 1f },
    glowScale: () -> Float = { 1f },
) {
    Box(modifier) {
        WaveLayers(waveStyle, waveColorFor(accentArgb), speedScale, glowScale)
    }
}

@Composable
fun rememberWavePowerThrottle(
    respectBatterySaver: Boolean,
    thermalThrottleAware: Boolean,
): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }

    var powerSave by remember { mutableStateOf(false) }
    DisposableEffect(powerManager, respectBatterySaver) {
        if (powerManager == null || !respectBatterySaver) {
            powerSave = false
            return@DisposableEffect onDispose {}
        }
        powerSave = powerManager.isPowerSaveMode
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                powerSave = powerManager.isPowerSaveMode
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    var thermalThrottling by remember { mutableStateOf(false) }
    DisposableEffect(powerManager, thermalThrottleAware) {
        if (powerManager == null || !thermalThrottleAware) {
            thermalThrottling = false
            return@DisposableEffect onDispose {}
        }
        thermalThrottling = powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            thermalThrottling = status >= PowerManager.THERMAL_STATUS_MODERATE
        }
        powerManager.addThermalStatusListener(listener)
        onDispose { powerManager.removeThermalStatusListener(listener) }
    }

    return powerSave || thermalThrottling
}

@Composable
private fun WallpaperBackground(
    customWallpaperPath: String,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model              = customWallpaperPath,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )

        Box(Modifier.fillMaxSize().background(Color(0x59000000)))
    }
}

