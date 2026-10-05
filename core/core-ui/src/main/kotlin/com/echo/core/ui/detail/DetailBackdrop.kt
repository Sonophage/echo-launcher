package com.echo.core.ui.detail

import androidx.compose.ui.graphics.Color

fun detailBackdropStops(pageTone: Color): Array<Pair<Float, Color>> = arrayOf(
    0f to pageTone.copy(alpha = 0.10f),
    0.32f to pageTone.copy(alpha = 0.46f),
    0.64f to pageTone.copy(alpha = 0.84f),
    1f to pageTone.copy(alpha = 0.97f),
)
