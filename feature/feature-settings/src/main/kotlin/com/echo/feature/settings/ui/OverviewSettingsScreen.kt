package com.echo.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.echo.core.common.format.formatByteSize
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.feature.artwork.api.ArtworkStatus
import com.echo.feature.settings.viewmodel.OverviewCover
import com.echo.feature.settings.viewmodel.OverviewSettingsViewModel

@Composable
fun OverviewSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = state.counts

    val context = LocalContext.current
    val packageInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }

    SettingsPageScaffold(
        subtitle = "Overview",
        onBack = onBack,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(top = 6.dp, bottom = 14.dp, end = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Eyebrow("Library")
                StatRow(Icons.Outlined.SportsEsports, 30.dp, alpha = 1f) {
                    Text(count(c.games, state.loading, "game"), style = Headline)
                    Detail(
                        listOf(
                            count(c.consoles, state.loading, "console"),
                            "${dash(c.android, state.loading)} Android",
                            "${dash(c.pc, state.loading)} PC",
                        ).joinToString("  ·  "),
                    )
                }
                StatRow(Icons.Outlined.MusicNote, 20.dp, alpha = 0.85f) {
                    StatLine(count(c.tracks, state.loading, "track"), count(c.artists, state.loading, "artist"))
                }
                StatRow(Icons.AutoMirrored.Outlined.MenuBook, 20.dp, alpha = 0.85f) {
                    StatLine(count(c.books, state.loading, "book"), "${dash(c.booksOpened, state.loading)} opened")
                }
                StatRow(Icons.Outlined.Movie, 20.dp, alpha = 0.85f) {
                    StatLine(count(c.videos, state.loading, "video"), count(c.videoCollections, state.loading, "collection"))
                }
                Spacer(Modifier.height(2.dp))
                Eyebrow("System")
                StatRow(Icons.Outlined.Image, 20.dp, alpha = 0.7f) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Artwork", style = RowText)
                        ArtworkBar(state.artwork, Modifier.width(64.dp).height(4.dp))
                        Detail("${state.artwork.complete} of ${state.artwork.total}  ·  cache ${state.artworkCacheBytes?.let { formatByteSize(it) } ?: "…"}")
                    }
                }
                StatRow(Icons.Outlined.Info, 20.dp, alpha = 0.55f) {
                    StatLine("Build ${packageInfo?.versionName ?: "?"}", "Android ${android.os.Build.VERSION.RELEASE}")
                }
            }

            CoverFan(c.lastPlayed, Modifier.weight(0.8f).fillMaxHeight())
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, size: Dp, alpha: Float, content: @Composable () -> Unit) {
    Row(
        Modifier.alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(size))
        }
        Column { content() }
    }
}

@Composable
private fun StatLine(main: String, detail: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(main, style = RowText, maxLines = 1)
        Detail(detail)
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.28.em),
        modifier = Modifier.padding(start = 50.dp),
    )
}

@Composable
private fun Detail(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ArtworkBar(status: ArtworkStatus, modifier: Modifier) {
    val total = status.total.coerceAtLeast(1)
    Row(modifier.clip(RoundedCornerShape(2.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(
            status.complete to Color(0xFF66BB6A),
            status.stale to Color(0xFFE0A030),
            status.missing to Color.White.copy(alpha = 0.25f),
        ).filter { it.first > 0 }.forEach { (n, color) ->
            Box(Modifier.weight(n.toFloat() / total).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun CoverFan(covers: List<OverviewCover>, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cardH = (maxHeight * 0.62f).coerceAtMost(maxWidth * 0.62f * 1.38f)
        val cardW = cardH / 1.38f
        if (covers.isEmpty()) {
            Detail("Nothing played yet")
            return@BoxWithConstraints
        }
        val (front, others) = covers.first() to covers.drop(1)
        others.getOrNull(1)?.let { Card(it, cardW, cardH, (-cardW * 0.42f), 8f, false) }
        others.getOrNull(0)?.let { Card(it, cardW, cardH, (cardW * 0.42f), -9f, false) }
        Card(front, cardW * 1.05f, cardH * 1.05f, 0.dp, 0f, true)
        Text(
            "Last played  ·  ${front.title}",
            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        )
    }
}

@Composable
private fun Card(cover: OverviewCover, w: Dp, h: Dp, dx: Dp, degrees: Float, front: Boolean) {
    AsyncImage(
        model = rememberArtworkModel(cover.artUri),
        contentDescription = if (front) cover.title else null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .offset(x = dx, y = -h * 0.06f)
            .rotate(degrees)
            .size(w, h)
            .shadow(16.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .then(if (front) Modifier.border(2.dp, SettingsAccent, RoundedCornerShape(10.dp)) else Modifier.alpha(0.85f)),
    )
}

private val Headline = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
private val RowText = TextStyle(color = Color.White, fontSize = 17.sp)

private fun dash(value: Int, loading: Boolean): String = if (loading) "—" else value.toString()

private fun count(value: Int, loading: Boolean, noun: String): String =
    if (loading) "— ${noun}s" else "$value ${if (value == 1) noun else noun + "s"}"
