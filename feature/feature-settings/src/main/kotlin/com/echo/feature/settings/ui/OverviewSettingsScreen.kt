package com.echo.feature.settings.ui

import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.common.format.formatByteSize
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.feature.artwork.api.ArtworkStatus
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
        // owner, 2026-10-05: restyled like the Profile panel: the last-played art as a banner behind big
        // numbers, a rule, then the details in columns
        Box(Modifier.fillMaxSize().padding(top = 6.dp, bottom = 14.dp, end = 26.dp).clip(RoundedCornerShape(18.dp))) {
            // sized from the whole window like the panel kit, not from this half of the page
            val density = LocalDensity.current
            val window = LocalWindowInfo.current.containerSize
            val u = panelDesignUnits(window.width / density.density, window.height / density.density, density)
            c.lastPlayed.firstOrNull()?.let { banner ->
                AsyncImage(
                    model = rememberArtworkModel(banner.artUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.4f),
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.55f),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to PanelBase.copy(alpha = 0.55f), 0.3f to PanelBase.copy(alpha = 0.3f), 0.55f to PanelBase.copy(alpha = 0.8f), 1f to PanelBase.copy(alpha = 0.95f))))
            }
            Column(Modifier.fillMaxSize().padding(u.dp(40)), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
                Text("Library", color = Color.White, fontSize = u.sp(52), fontWeight = FontWeight.ExtraLight, maxLines = 1)
                c.lastPlayed.firstOrNull()?.let { Detail("Last played  ·  ${it.title}") }
                Row(Modifier.padding(top = u.dp(18)), horizontalArrangement = Arrangement.spacedBy(u.dp(48))) {
                    BigStat(dash(c.games, state.loading), "Games", u)
                    BigStat(dash(c.tracks, state.loading), "Tracks", u)
                    BigStat(dash(c.books, state.loading), "Books", u)
                    BigStat(dash(c.videos, state.loading), "Videos", u)
                }
                Box(Modifier.padding(top = u.dp(18), bottom = u.dp(18)).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(40))) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
                        Eyebrow("In the library")
                        StatRow(Icons.Outlined.SportsEsports, 20.dp, alpha = 0.9f) {
                            StatLine(count(c.consoles, state.loading, "console"), "${dash(c.android, state.loading)} Android  ·  ${dash(c.pc, state.loading)} PC")
                        }
                        StatRow(Icons.Outlined.MusicNote, 20.dp, alpha = 0.9f) { StatLine(count(c.artists, state.loading, "artist"), "") }
                        StatRow(Icons.AutoMirrored.Outlined.MenuBook, 20.dp, alpha = 0.9f) { StatLine("${dash(c.booksOpened, state.loading)} books opened", "") }
                        StatRow(Icons.Outlined.Movie, 20.dp, alpha = 0.9f) { StatLine(count(c.videoCollections, state.loading, "collection"), "") }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
                        Eyebrow("System")
                        StatRow(Icons.Outlined.Image, 20.dp, alpha = 0.9f) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Artwork", style = RowText)
                                ArtworkBar(state.artwork, Modifier.width(64.dp).height(4.dp))
                            }
                            Detail("${state.artwork.complete} of ${state.artwork.total}  ·  cache ${state.artworkCacheBytes?.let { formatByteSize(it) } ?: "…"}")
                        }
                        StatRow(Icons.Outlined.Info, 20.dp, alpha = 0.9f) {
                            StatLine("Build ${packageInfo?.versionName ?: "?"}", "Android ${android.os.Build.VERSION.RELEASE}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
        Text(value, color = Color.White, fontSize = u.sp(32), fontWeight = FontWeight.ExtraLight, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
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
        style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.28.em),
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

private val RowText = EchoTextStyle.copy(color = Color.White, fontSize = 17.sp)

private fun dash(value: Int, loading: Boolean): String = if (loading) "—" else value.toString()

private fun count(value: Int, loading: Boolean, noun: String): String =
    if (loading) "— ${noun}s" else "$value ${if (value == 1) noun else noun + "s"}"
