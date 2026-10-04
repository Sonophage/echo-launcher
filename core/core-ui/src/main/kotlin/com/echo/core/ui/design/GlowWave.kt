package com.echo.core.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// the crossbar's wave, handed down so the screens that paint their own backdrop can show it
// (owner, 2026-10-04); null when the wave is off
val LocalBackdropWave = compositionLocalOf<(@Composable () -> Unit)?> { null }

// the art behind every context menu's blurred backing: the crossbar's selection, so menus off the
// crossbar (drawer, settings, detail screens) get the Recent rail's backing too; null draws the plain fill
val LocalMenuBackdropArt = compositionLocalOf<String?> { null }

// which side a screen's glow sits on: the panel and settings glow left (panelBackdrop), the drawer's art right
enum class GlowSide(val x: Float) { LEFT(0.18f), RIGHT(0.82f) }

// the wave shows only inside the glow: the glow's own radial falloff is its mask
@Composable
fun GlowMaskedWave(side: GlowSide, modifier: Modifier = Modifier) {
    val wave = LocalBackdropWave.current ?: return
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Black, Color.Transparent),
                        center = Offset(size.width * side.x, size.height * 0.45f),
                        radius = size.width * 0.55f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) { wave() }
}
