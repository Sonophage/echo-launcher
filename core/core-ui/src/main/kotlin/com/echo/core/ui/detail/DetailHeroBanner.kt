package com.echo.core.ui.detail

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val DetailHeroHeight: Dp = 220.dp

val DetailHeroWidth: Dp = DetailContentMaxWidth - DetailContentPadding * 2

val DetailHeroAspect: Float = DetailHeroWidth.value / DetailHeroHeight.value
