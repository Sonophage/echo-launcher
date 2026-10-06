package com.echo.core.ui.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.echo.core.ui.icons.AppIconArt

// owner, 2026-10-05: the App Drawer and Search show an app or game as the same VHS case, so the two screens read as
// one: ribbed black cassette plastic, a VHS spine label down its left edge, and the cover in a window beside it.
// Each screen moves and rings the case its own way; this is the case itself

val CaseShell = Brush.verticalGradient(listOf(Color(0xFF232227), Color(0xFF141317)))
val CaseLabel = Color(0xFFE8E2D3)
val CaseInk = Color(0xFF1A1A1A)

fun caseShape(u: DesignUnits) = RoundedCornerShape(u.dp(8))

// the fine horizontal ridges of a VHS cassette's plastic, a light edge over a dark groove
fun Modifier.cassetteRibs(): Modifier = drawBehind {
    val step = 3.dp.toPx()
    val line = 1.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), line)
        drawLine(Color.Black.copy(alpha = 0.35f), Offset(0f, y + line), Offset(size.width, y + line), line)
        y += step
    }
}

// the window a cover shows through in a w × h face: the art's own shape, as large as fits. aspect is width / height
fun coverWindow(w: Float, h: Float, aspect: Float): Pair<Float, Float> =
    if (aspect > w / h) w to w / aspect else h * aspect to h

// the case: the caller's modifier sizes, lifts and rings it; face draws the cover in the window beside the spine
@Composable
fun VhsCase(label: String, tint: Color, u: DesignUnits, modifier: Modifier = Modifier, face: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(caseShape(u)).background(CaseShell).cassetteRibs()) {
        VhsSpine(label, tint, u, Modifier.padding(start = u.dp(4), top = u.dp(6), bottom = u.dp(6)).width(u.dp(24)).fillMaxHeight())
        Box(Modifier.fillMaxSize().padding(start = u.dp(32), top = u.dp(6), end = u.dp(6), bottom = u.dp(6))) {
            face()
            Box(Modifier.fillMaxSize().background(CoverSheen))
        }
    }
}

// owner, 2026-10-05: a cover keeps its own shape. Its window is sized to the art and centred, and the ribbed plastic
// shows round it, so a square Game Boy box is neither stretched, cropped nor padded with blur
@Composable
fun VhsCoverArt(model: Any?, u: DesignUnits) {
    var aspect by remember(model) { mutableStateOf<Float?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val (w, h) = coverWindow(maxWidth.value, maxHeight.value, aspect ?: (maxWidth / maxHeight))
        AsyncImage(
            model, null, contentScale = ContentScale.Fit,
            onSuccess = { s -> s.result.image.let { if (it.width > 0 && it.height > 0) aspect = it.width.toFloat() / it.height } },
            modifier = Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(u.dp(4))),
        )
    }
}

// an app's cover: its colour with ECHO's echo rings, its icon large and faint and again small and sharp, and its
// name. Without an icon, glyph stands in for it
@Composable
fun VhsAppFace(label: String, icon: AppIconArt?, tint: Color, u: DesignUnits, glyph: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(u.dp(4))).background(tint)) {
        Box(Modifier.fillMaxSize().coverRings()) {
            icon?.let { Image(it.bitmap, null, Modifier.align(Alignment.BottomEnd).offset(u.dp(30), u.dp(18)).size(u.dp(130)).rotate(-14f).graphicsLayer(alpha = 0.16f)) }
        }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = u.dp(8)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            if (icon != null) Image(icon.bitmap, null, Modifier.size(u.dp(72)).shadow(u.dp(8), RoundedCornerShape(u.dp(18))))
            else glyph()
            Text(label.uppercase(), color = Color.White, fontSize = u.sp(13), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.04.em,
                lineHeight = u.sp(15), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// owner, 2026-10-05: the spine reads as a VHS tape's: a cream label with the colour band and a play mark at the
// top, the kind running down it, tracking rules, and a black VHS tab at the foot
@Composable
private fun VhsSpine(label: String, tint: Color, u: DesignUnits, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(u.dp(3))).background(CaseLabel),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(u.dp(22)).background(tint), contentAlignment = Alignment.Center) {
            Text("▶", color = Color.White, fontSize = u.sp(9))
        }
        Box(Modifier.weight(1f).padding(vertical = u.dp(8)), contentAlignment = Alignment.TopCenter) {
            Text(label.uppercase(), color = CaseInk, fontSize = u.sp(10), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                maxLines = 1, softWrap = false, modifier = Modifier.sideways())
        }
        Column(Modifier.padding(bottom = u.dp(5)), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            repeat(3) { Box(Modifier.size(u.dp(14), 1.dp).background(CaseInk.copy(alpha = 0.6f))) }
        }
        Box(Modifier.fillMaxWidth().height(u.dp(20)).background(CaseInk), contentAlignment = Alignment.Center) {
            Text("VHS", color = CaseLabel, fontSize = u.sp(6), fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        }
    }
}
