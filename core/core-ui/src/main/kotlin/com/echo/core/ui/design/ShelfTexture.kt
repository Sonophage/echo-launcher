package com.echo.core.ui.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// owner, 2026-10-05: the App Drawer and Search take the look of the "Drawer and Search Variations" design (round 6):
// a dark room with a texture on the wall, film grain over everything, and the selected item's colour as light.
// These are the shared surfaces; each screen composes them

val ShelfRoom = Color(0xFF0C0B0E)

// thin diagonal lines across the wall
fun Modifier.wallStripes(alpha: Float = 0.025f): Modifier = drawWithCache {
    val step = 7.dp.toPx()
    val line = 2.dp.toPx()
    onDrawBehind {
        var x = -size.height
        while (x < size.width) {
            drawLine(Color.White.copy(alpha = alpha), Offset(x, size.height), Offset(x + size.height, 0f), line)
            x += step
        }
    }
}

// darkens the edges
fun Modifier.vignette(strength: Float = 0.65f): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = strength),
        center = Offset(size.width / 2, size.height / 2), radius = maxOf(size.width, size.height) * 0.75f,
    )
    onDrawWithContent { drawContent(); drawRect(brush) }
}

// coloured light from one point of the room
fun Modifier.roomGlow(colour: Color, centre: Offset = Offset(0.15f, 0.4f)): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        listOf(colour.copy(alpha = 0.5f), Color.Transparent),
        center = Offset(size.width * centre.x, size.height * centre.y), radius = size.width * 0.6f,
    )
    onDrawBehind { drawRect(brush) }
}

private val grainTile: ImageBitmap by lazy {
    val n = 160
    val random = Random(7)
    val pixels = IntArray(n * n) { val v = random.nextInt(256); (0xFF shl 24) or (v shl 16) or (v shl 8) or v }
    android.graphics.Bitmap.createBitmap(pixels, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}

// film grain over everything below it
fun Modifier.filmGrain(alpha: Float = 0.14f): Modifier = drawWithCache {
    val brush = ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    onDrawWithContent { drawContent(); drawRect(brush, alpha = alpha, blendMode = BlendMode.Overlay) }
}

// owner, 2026-10-05: an app's cover carries ECHO's own echo rings, spreading from its lower corner, in place of
// the design's halftone dots
fun Modifier.coverRings(alpha: Float = 0.14f): Modifier = drawWithCache {
    val centre = Offset(size.width * 0.85f, size.height * 0.9f)
    val step = size.width * 0.22f
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
    onDrawWithContent {
        drawContent()
        for (i in 1..6) drawCircle(Color.White.copy(alpha = alpha * (1f - i * 0.12f)), step * i, centre, style = stroke)
    }
}

// light catching the plastic over a cover
val CoverSheen = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.22f), 0.32f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color.White.copy(alpha = 0.06f),
)

// lays a child out turned a quarter turn, for text running up a spine
fun Modifier.sideways(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints(maxWidth = constraints.maxHeight.takeIf { it != Constraints.Infinity } ?: Constraints.Infinity))
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(-(placeable.width - placeable.height) / 2, (placeable.width - placeable.height) / 2) { rotationZ = 90f }
    }
}
