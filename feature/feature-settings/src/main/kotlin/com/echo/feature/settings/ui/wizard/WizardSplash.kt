package com.echo.feature.settings.ui.wizard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.R as CoreUiR
import com.echo.core.ui.icons.PortalIcon
import com.echo.core.ui.sound.LocalMenuSounds
import com.echo.core.ui.sound.MenuSound
import com.echo.core.ui.wave.WaveLayers
import com.echo.core.ui.wave.LocalWaveDesign
import com.echo.core.ui.wave.WaveDesign
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import com.echo.core.ui.wave.WaveStyle
import com.echo.feature.settings.ui.LocalSettingsActionConsumed
import com.echo.feature.settings.ui.LocalSettingsPendingAction

@Composable
fun WizardSplash(onBegin: () -> Unit) {
    val begin by rememberUpdatedState(onBegin)
    val menuSounds = LocalMenuSounds.current
    val pendingAction = LocalSettingsPendingAction.current
    val onConsumed = LocalSettingsActionConsumed.current

    var entering by remember { mutableStateOf(false) }
    val press = remember { Animatable(0f) }

    val start: () -> Unit = {
        if (!entering) {
            entering = true
            menuSounds(MenuSound.SELECT)
        }
    }

    LaunchedEffect(pendingAction) {
        if (pendingAction == GamepadAction.SELECT) {
            onConsumed()
            start()
        }
    }

    LaunchedEffect(entering) {
        if (!entering) return@LaunchedEffect
        press.animateTo(1f, tween(SplashEnterMs, easing = FastOutSlowInEasing))
        begin()
    }

    val idle = rememberInfiniteTransition(label = "splash-shimmer")
    val idleSweep by idle.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(ShimmerCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splash-shimmer-sweep",
    )

    val t = press.value

    val glow = FastOutSlowInEasing.transform((t / GlowPeak).coerceIn(0f, 1f))
    val exit = ((t - ExitStart) / (1f - ExitStart)).coerceIn(0f, 1f)
    val sweep = if (entering) -0.4f + t * 1.8f else idleSweep

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)

            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = start,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // the kit's own wave on the first screen, whatever wave the user later picks (owner, 2026-10-04)
        CompositionLocalProvider(LocalWaveDesign provides WaveDesign.ECHO_RINGS) { WaveLayers(WaveStyle.ANIMATED) }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(LogoFraction)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            scaleX = 1f + glow * GlowSwell + exit * ExitSwell
                            scaleY = scaleX
                            alpha = 1f - exit
                        }
                        .drawWithContent {
                            drawContent()

                            val span = size.width * ShimmerWidth
                            val head = size.width * sweep
                            drawRect(
                                brush = Brush.linearGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = if (entering) 0.95f else 0.55f),
                                    1f to Color.Transparent,
                                    start = Offset(head - span, 0f),
                                    end = Offset(head + span, size.height),
                                ),
                                blendMode = BlendMode.SrcAtop,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    PortalIcon(
                        painter = painterResource(CoreUiR.drawable.echo_logo),
                        contentDescription = "ECHO",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
            val fade = (1f - t * PromptFadeRate).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HintBarHeight)
                    .graphicsLayer { alpha = fade },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("EXTENSIBLE CONSOLE HANDHELD OPERATOR", style = u.eyebrow())
                Spacer(Modifier.height(u.dp(10)))
                Text(
                    "Your games, apps and media on one crossbar. Setup takes four short steps.",
                    style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.85f), fontSize = u.sp(20), fontWeight = FontWeight.Light),
                )
            }
            // the kit footer's A, as every other screen has it
            EchoHintBar(
                items = emptyList(),
                onAction = { if (it == GamepadAction.SELECT) start() },
                primary = HintAction(GamepadAction.SELECT, "Get started"),
                modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = fade },
            )
        }
    }
}

private const val SplashEnterMs = 900

private const val GlowPeak = 0.45f

private const val ExitStart = 0.55f

private const val GlowSwell = 0.04f
private const val ExitSwell = 0.22f

private const val ShimmerCycleMs = 4_200

private const val ShimmerWidth = 0.45f

private const val LogoFraction = 0.42f

private const val PromptFadeRate = 2.4f
