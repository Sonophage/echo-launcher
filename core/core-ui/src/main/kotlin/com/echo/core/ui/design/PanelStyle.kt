package com.echo.core.ui.design

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import com.echo.core.domain.model.SettingsSectionId

val PanelBase = Color(0xFF04060C)
val PanelEdgeShade = Color(0xB3020308)

val PanelCardFill = Color.White.copy(alpha = 0.06f)
val PanelCardFocusFill = Color.White.copy(alpha = 0.17f)
val PanelFocusRing = Color.White.copy(alpha = 0.9f)

const val PANEL_CARD_RADIUS = 14
const val PANEL_FOCUS_RING_WIDTH = 2

private const val PANEL_DESIGN_WIDTH = 1200f
const val PANEL_DESIGN_HEIGHT = 752f
private const val SQUARE_ASPECT = 1.4f

fun panelDesignUnits(widthDp: Float, heightDp: Float, density: Density): DesignUnits =
    DesignUnits(minOf(widthDp / PANEL_DESIGN_WIDTH, heightDp / PANEL_DESIGN_HEIGHT), density, panelIsSquare(widthDp, heightDp))

fun panelIsSquare(widthDp: Float, heightDp: Float): Boolean = widthDp < heightDp * SQUARE_ASPECT

fun Modifier.panelBackdrop(tint: Color): Modifier = this
    .background(PanelBase)
    .drawBehind {
        drawRect(Brush.radialGradient(
            listOf(tint.copy(alpha = 0.6f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.45f),
            radius = size.width * 0.55f,
        ))
        drawRect(Brush.horizontalGradient(0.45f to Color.Transparent, 1f to PanelEdgeShade))
    }

// the side rail the Recent rail and every context menu share (owner, 2026-10-04): its width in
// panel design units and its see-through backing, so the wave still shows behind it
const val RAIL_PANEL_WIDTH = 480
val RailPanelFill = Color.Black.copy(alpha = 0.45f)

val MenuScrim = Color(0xF7050201)

fun Modifier.menuBackdrop(): Modifier = background(
    Brush.horizontalGradient(
        0f to Color.Transparent,
        0.5f to MenuScrim.copy(alpha = MenuScrim.alpha * 0.45f),
        1f to MenuScrim,
    ),
)

fun panelSectionTint(section: SettingsSectionId?): Color = when (section) {
    SettingsSectionId.LIBRARY -> Color(0xFFB5303C)
    SettingsSectionId.EMULATORS -> Color(0xFF2C7A55)
    SettingsSectionId.LOOK -> Color(0xFF8E4FB8)
    SettingsSectionId.CONTROLS -> Color(0xFF2C5FD8)
    SettingsSectionId.ACCOUNTS -> Color(0xFF2A8A9A)
    SettingsSectionId.SYSTEM -> Color(0xFF4A5470)
    SettingsSectionId.SETUP -> Color(0xFFC0632A)
    null -> Color(0xFF222222)
}
