package com.psplauncher.core.ui.design

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.psplauncher.core.domain.model.SettingsSectionId

val PanelBase = Color(0xFF04060C)
val PanelEdgeShade = Color(0xB3020308)

val PanelCardFill = Color.White.copy(alpha = 0.06f)
val PanelCardFocusFill = Color.White.copy(alpha = 0.17f)
val PanelFocusRing = Color.White.copy(alpha = 0.9f)

const val PANEL_CARD_RADIUS = 14
const val PANEL_FOCUS_RING_WIDTH = 2
const val PANEL_UNFOCUSED_ALPHA = 0.55f

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

fun panelSectionTint(section: SettingsSectionId?): Color = when (section) {
    SettingsSectionId.OVERVIEW -> Color(0xFF2C5FD8)
    SettingsSectionId.EMULATORS -> Color(0xFF2C7A55)
    SettingsSectionId.LOOK_AND_FEEL -> Color(0xFF8E4FB8)
    SettingsSectionId.SYSTEM -> Color(0xFF4A5470)
    SettingsSectionId.SETUP -> Color(0xFFC0632A)
    null -> Color(0xFF222222)
}
