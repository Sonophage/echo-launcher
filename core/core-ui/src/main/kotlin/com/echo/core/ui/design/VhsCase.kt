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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    BoxWithConstraints(modifier.clip(caseShape(u)).background(CaseShell).cassetteRibs()) {
        val spine = titanSpineWidth(u.dp(24), maxWidth, IsTitan2)
        val grow = spine / u.dp(24)
        VhsSpine(label, tint, u, grow, spine, Modifier.padding(start = u.dp(4), top = u.dp(6), bottom = u.dp(6)).width(spine).fillMaxHeight())
        Box(Modifier.fillMaxSize().padding(start = spine + u.dp(8), top = u.dp(6), end = u.dp(6), bottom = u.dp(6))) {
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

// the smallest an app's logo on a case is drawn: 72 design units is ~44dp on the Konker (owner, 2026-10-07: the
// App Drawer's and Search's logos stay readable on small screens)
val VHS_ICON_MIN = 64.dp

// an app's cover: its colour with ECHO's echo rings, its icon large and faint and again small and sharp, and its
// name. Without an icon, glyph stands in for it. [iconMin] keeps the icon from shrinking below that size
@Composable
fun VhsAppFace(label: String, icon: AppIconArt?, tint: Color, u: DesignUnits, iconMin: Dp = VHS_ICON_MIN, glyph: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(u.dp(4))).background(tint)) {
        val mark = titanFaceSize(u.dp(130), maxWidth, TITAN_MARK_SHARE, IsTitan2)
        val grow = mark / u.dp(130)
        val logo = titanFaceSize(maxOf(u.dp(72), iconMin), maxWidth, TITAN_ICON_SHARE, IsTitan2)
        Box(Modifier.fillMaxSize().coverRings()) {
            icon?.let { Image(it.bitmap, null, Modifier.align(Alignment.BottomEnd).offset(u.dp(30) * grow, u.dp(18) * grow).size(mark).rotate(-14f).graphicsLayer(alpha = 0.16f)) }
        }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = u.dp(8)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            if (icon != null) Image(icon.bitmap, null, Modifier.size(logo).shadow(u.dp(8), RoundedCornerShape(u.dp(18) * grow)))
            else glyph()
            Text(label.uppercase(), color = Color.White, fontSize = u.sp(13) * grow, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.04.em,
                lineHeight = u.sp(15) * grow, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// owner, 2026-10-09: on the Titan 2 a case's spine, logo and faint logo grow with the case, so Search's one large case
// is not a small logo on a field of colour. They never shrink, so a small case (the App Drawer's) keeps its sizes,
// and on any other device they are what they were
internal const val TITAN_SPINE_SHARE = 0.09f
internal const val TITAN_ICON_SHARE = 0.42f
internal const val TITAN_MARK_SHARE = 0.8f

internal fun titanSpineWidth(base: Dp, caseWidth: Dp, titan2: Boolean): Dp =
    if (titan2) maxOf(base, caseWidth * TITAN_SPINE_SHARE) else base

internal fun titanFaceSize(base: Dp, faceWidth: Dp, share: Float, titan2: Boolean): Dp =
    if (titan2) maxOf(base, faceWidth * share) else base

// owner, 2026-10-05: the spine reads as a VHS tape's: a cream label with the colour band and a play mark at the
// top, the kind running down it, tracking rules, and a black VHS tab at the foot
@Composable
private fun VhsSpine(label: String, tint: Color, u: DesignUnits, grow: Float, width: Dp, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(u.dp(3))).background(CaseLabel),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(u.dp(22) * grow).background(tint), contentAlignment = Alignment.Center) {
            Text("▶", color = Color.White, fontSize = u.sp(9) * grow)
        }
        Box(Modifier.weight(1f).padding(vertical = u.dp(8)), contentAlignment = Alignment.TopCenter) {
            Text(label.uppercase(), color = CaseInk, fontSize = u.sp(10) * grow, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                maxLines = 1, softWrap = false, modifier = Modifier.sideways())
        }
        Column(Modifier.padding(bottom = u.dp(5)), verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            repeat(3) { Box(Modifier.size(u.dp(14) * grow, 1.dp).background(CaseInk.copy(alpha = 0.6f))) }
        }
        Box(Modifier.fillMaxWidth().height(u.dp(20) * grow).background(CaseInk), contentAlignment = Alignment.Center) {
            // the legibility floor made the Titan 2's "VHS" wider than its spine; there it is sized to the spine
            val vhs = if (IsTitan2) with(LocalDensity.current) { (width * 0.42f).toSp() } else u.sp(6)
            Text("VHS", color = CaseLabel, fontSize = vhs, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        }
    }
}
