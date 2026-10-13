package com.echo.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

// spec #3: a pressed target draws the active focus style, the mark the controller shows, from the moment the finger
// lands. EchoTheme provides it as LocalIndication, so every clickable has it and no Material ripple is drawn. A
// target that wants no mark (a scrim, a whole screen) sets indication = null
object PressMark : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressMarkNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private const val PressMarkFadeMs = 150

private class PressMarkNode(private val source: InteractionSource) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val visible = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                    else -> return@collect
                }
                // on at once when the finger lands; a short fade on lift, so a quick tap is still seen
                launch {
                    if (presses.isNotEmpty()) visible.snapTo(1f) else visible.animateTo(0f, tween(PressMarkFadeMs)) { invalidateDraw() }
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val v = visible.value
        if (v > 0f) {
            val style = currentValueOf(LocalFocusStyle)
            if (style.glows) drawFocusGlow(v, FocusGlow.RowReach, FocusGlow.RowAlpha)
            else drawFocusMark(style, v, currentValueOf(LocalEchoColors).accentColor)
        }
        drawContent()
    }
}
