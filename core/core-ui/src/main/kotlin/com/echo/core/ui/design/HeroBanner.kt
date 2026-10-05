package com.echo.core.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// owner, 2026-10-05: the App Drawer and Search share one hero banner, a bit taller than the drawer's was
const val HERO_BANNER_HEIGHT = 290
const val HERO_BANNER_SHORT_HEIGHT = 120
const val HERO_BANNER_SIDE = 80
private const val HERO_ART_START = 380

// the highlighted item across the top: its art on the right, fading into the info on the left
@Composable
fun HeroBanner(
    u: DesignUnits,
    tint: Color?,
    modifier: Modifier = Modifier,
    short: Boolean = false,
    art: @Composable BoxScope.() -> Unit,
    info: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(u.dp(if (short) HERO_BANNER_SHORT_HEIGHT else HERO_BANNER_HEIGHT))
            .clip(RoundedCornerShape(u.dp(22)))
            .background(tint?.copy(alpha = 0.35f) ?: Color.White.copy(alpha = 0.06f)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = u.dp(HERO_ART_START))
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(ArtFade, blendMode = BlendMode.DstIn)
                },
            content = art,
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PanelBase.copy(alpha = 0.85f), 0.55f to Color.Transparent)))
        info()
    }
}

private val ArtFade = Brush.horizontalGradient(
    0f to Color.Transparent,
    0.14f to Color.Black.copy(alpha = 0.35f),
    0.3f to Color.Black.copy(alpha = 0.8f),
    0.46f to Color.Black,
)
