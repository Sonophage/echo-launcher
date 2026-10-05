package com.echo.core.ui.design

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

// which sides of the orb the neck flares out to the card on
enum class NeckFlare { LEFT, RIGHT, BOTH }

// owner, 2026-10-05: the neck that joins an orb to the card above or below it, so the two read as one shape.
// The orb's disc, its body down (or up) to the card's edge, and on each flared side one smooth curve that
// leaves the orb's side straight down and meets the card's edge flat, with no corner anywhere. Used by the top
// bar's islands and the footer's A orb, drawn behind both the orb and the card
fun DrawScope.drawIslandNeck(fill: Color, centre: Offset, radius: Float, cardEdgeY: Float, flare: Float, sides: NeckFlare) {
    drawCircle(fill, radius, centre)
    val top = minOf(centre.y, cardEdgeY)
    drawRect(fill, Offset(centre.x - radius, top), Size(radius * 2, kotlin.math.abs(cardEdgeY - centre.y)))
    fun side(sign: Float) {
        val sx = centre.x + sign * radius
        val ex = sx + sign * flare
        val path = Path().apply {
            moveTo(sx, centre.y)
            cubicTo(sx, centre.y + (cardEdgeY - centre.y) * 0.6f, ex - sign * flare * 0.55f, cardEdgeY, ex, cardEdgeY)
            lineTo(centre.x, cardEdgeY)
            lineTo(centre.x, centre.y)
            close()
        }
        drawPath(path, fill)
    }
    if (sides != NeckFlare.LEFT) side(1f)
    if (sides != NeckFlare.RIGHT) side(-1f)
}
