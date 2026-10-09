package com.echo.core.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ScrollState
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.StatusStripHeight

internal val DetailTextPrimary: Color @Composable @ReadOnlyComposable get() = detailPalette().textPrimary
internal val DetailTextMuted: Color @Composable @ReadOnlyComposable get() = detailPalette().textMuted
internal val DetailDivider: Color @Composable @ReadOnlyComposable get() = detailPalette().divider

val DetailButtonRest = Color.White.copy(alpha = 0.13f)

val DetailButtonRestCompact = Color.White.copy(alpha = 0.06f)

val DetailButtonFocusFill = Color(0xFFEDEDED)

val DetailButtonFocusText = Color(0xFF101014)

val DetailContentMaxWidth: Dp = 920.dp
val DetailContentPadding: Dp = 28.dp

val DetailFooterHeight: Dp = 58.dp

internal val DetailTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.72f),
    offset = Offset(0f, 2f),
    blurRadius = 4f,
)

@Composable
@ReadOnlyComposable
internal fun detailSurfaceTop(): Color = detailPalette().pageTop

@Composable
@ReadOnlyComposable
internal fun detailSurfaceBottom(): Color = detailPalette().pageBottom

val LocalDetailViewportHeight = staticCompositionLocalOf { Dp.Infinity }
