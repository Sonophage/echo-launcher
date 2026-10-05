package com.echo.feature.crossbar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.echo.themekit.UiMediaLimits
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val FADE_OUT_MS = 600

private const val HARD_CAP_MS = 12_000L

// owner, 2026-10-05: with no boot video, the boot is BootRipple (the design spec's "Horizon Ripple"). A press of
// A or B (skipRequested) runs its 300 ms exit from the pressed frame; a boot video still skips at once
@Composable
fun BootSequenceOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    bootVideoPath: String? = null,
    bootAudioPath: String? = null,
    skipRequested: Boolean = false,
) {
    val overlayAlpha = remember { Animatable(1f) }
    val clock = remember { Animatable(0f) }
    var skipAt by remember { mutableStateOf<Float?>(null) }

    val currentComplete by rememberUpdatedState(onComplete)

    val completed = remember { AtomicBoolean(false) }
    val complete = { if (completed.compareAndSet(false, true)) currentComplete() }

    var presentationDone by remember { mutableStateOf(false) }

    var useLogoAnimation by remember(bootVideoPath) { mutableStateOf(bootVideoPath == null) }

    LaunchedEffect(Unit) {
        val endedNaturally = withTimeoutOrNull(HARD_CAP_MS) {
            snapshotFlow { presentationDone }.first { it }
        } != null
        if (!endedNaturally) {
            Timber.w("Boot watchdog fired after ${HARD_CAP_MS}ms — completing boot regardless")
        } else if (!useLogoAnimation) {
            overlayAlpha.animateTo(0f, animationSpec = tween(FADE_OUT_MS))
        }
        complete()
    }

    // the animation's own clock; its last 600 ms (or the 300 ms after a skip) are the fade to the home screen
    LaunchedEffect(useLogoAnimation, skipAt) {
        if (!useLogoAnimation) return@LaunchedEffect
        val end = BootRipple.endMs(skipAt)
        clock.animateTo(end, tween(((end - clock.value).coerceAtLeast(0f)).toInt(), easing = LinearEasing))
        presentationDone = true
    }

    LaunchedEffect(skipRequested) {
        if (!skipRequested) return@LaunchedEffect
        if (useLogoAnimation) { if (skipAt == null) skipAt = clock.value } else complete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(overlayAlpha.value),
        contentAlignment = Alignment.Center,
    ) {
        if (bootVideoPath != null && !useLogoAnimation) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                OneShotVideoLayer(
                    path = bootVideoPath,
                    clipEndMs = UiMediaLimits.BOOT_MAX_MS,
                    onEnded = { presentationDone = true },
                    onFailed = {
                        useLogoAnimation = true
                    },

                    muted = bootAudioPath != null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            BootRippleAnimation(clock.value, skipAt)
        }

        if (bootAudioPath != null) {
            // with the animation, the sound starts with the first ripple
            var soundDue by remember(useLogoAnimation) { mutableStateOf(!useLogoAnimation) }
            LaunchedEffect(useLogoAnimation) { if (useLogoAnimation) { delay(BootRipple.SOUND_MS); soundDue = true } }
            if (soundDue && skipAt == null) OneShotAudioLayer(path = bootAudioPath, clipEndMs = UiMediaLimits.BOOT_MAX_MS)
        }
    }
}
