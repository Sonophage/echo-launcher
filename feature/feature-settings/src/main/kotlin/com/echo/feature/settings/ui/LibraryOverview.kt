package com.echo.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.echo.core.ui.design.PANEL_CARD_RADIUS
import com.echo.core.ui.design.PanelCardFill
import com.echo.core.ui.icons.rememberAppIcon
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
import com.echo.feature.settings.viewmodel.OverviewSettingsViewModel
import com.echo.feature.settings.viewmodel.OverviewMedia
import com.echo.feature.settings.viewmodel.overviewMediaRows
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Photo

// owner, 2026-10-06: the library at a glance, a tab of the Profile screen (it was Settings ▸ Overview): ECHO's
// version and artwork, the big numbers, then recently played, the games and the media
@Composable
fun LibraryOverview(
    modifier: Modifier = Modifier,
    viewModel: OverviewSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = state.counts

    val context = LocalContext.current
    val packageInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }

    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    // sized from the whole window like the panel kit
    val u = panelDesignUnits(window.width / density.density, window.height / density.density, density)

    Box(modifier) {
        val scrollState = rememberScrollState()
        // the same left edge as the settings rows (their 40dp, plus the rail row's own inset)
        // the Profile screen's own margins and its top, under the bar
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = u.dp(80), end = u.dp(80), top = u.dp(96), bottom = u.dp(64))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(24))) {
                val icon = rememberAppIcon(context.packageName)?.bitmap
                Box(Modifier.size(u.dp(96)).clip(RoundedCornerShape(u.dp(14))).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    icon?.let { Image(it, null, Modifier.fillMaxSize()) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
                    Text("Library", color = Color.White, fontSize = u.sp(52), fontWeight = FontWeight.ExtraLight, maxLines = 1)
                    Detail("ECHO ${packageInfo?.versionName ?: "?"}  ·  Android ${android.os.Build.VERSION.RELEASE}")
                    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
                        Chip("Artwork ${state.artwork.complete} of ${state.artwork.total}", u)
                        Chip("Cache ${state.artworkCacheBytes?.let { formatByteSize(it) } ?: "…"}", u)
                    }
                }
            }
            Row(Modifier.padding(top = u.dp(16)), horizontalArrangement = Arrangement.spacedBy(u.dp(48))) {
                BigStat(dash(c.games, state.loading), "Games", u)
                overviewMediaRows(c, state.mediaShown).forEach { row ->
                    val (value, label) = when (row.kind) {
                        OverviewMedia.MUSIC -> c.tracks to "Tracks"
                        OverviewMedia.VIDEO -> c.videos to "Videos"
                        OverviewMedia.PHOTOS -> c.photos to "Photos"
                        OverviewMedia.BOOKS -> c.books to "Books"
                    }
                    BigStat(dash(value, state.loading), label, u)
                }
            }
            Box(Modifier.padding(top = u.dp(12), bottom = u.dp(16)).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            // three columns side by side, as on the Profile page, so the page fits the short screen
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(u.dp(24))) {
                Column(Modifier.weight(1.2f)) {
                    SectionLabel("Recently played", u)
                    if (c.lastPlayed.isEmpty()) Detail("Nothing played yet")
                    c.lastPlayed.forEach { game ->
                        Card(u) {
                            AsyncImage(
                                model = rememberArtworkModel(game.artUri),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(u.dp(40)).clip(RoundedCornerShape(u.dp(10))),
                            )
                            Text(game.title, color = Color.White, fontSize = u.sp(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    SectionLabel("Games", u)
                    StatCard(Icons.Outlined.SportsEsports, count(c.consoles, state.loading, "console"), null, u)
                    StatCard(Icons.Outlined.PhoneAndroid, "${dash(c.android, state.loading)} Android", null, u)
                    StatCard(Icons.Outlined.Computer, "${dash(c.pc, state.loading)} PC", null, u)
                }
                val media = overviewMediaRows(c, state.mediaShown)
                if (media.isNotEmpty()) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Media", u)
                        // two to a row, so four kinds fit the short screen
                        media.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
                                pair.forEach { row ->
                                    val icon = when (row.kind) {
                                        OverviewMedia.MUSIC -> Icons.Outlined.MusicNote
                                        OverviewMedia.VIDEO -> Icons.Outlined.Movie
                                        OverviewMedia.PHOTOS -> Icons.Outlined.Photo
                                        OverviewMedia.BOOKS -> Icons.AutoMirrored.Outlined.MenuBook
                                    }
                                    StatCard(icon, row.main, row.detail, u, Modifier.weight(1f).padding(bottom = u.dp(6)))
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    main: String,
    detail: String?,
    u: DesignUnits,
    modifier: Modifier = Modifier.fillMaxWidth().padding(bottom = u.dp(6)),
) {
    Card(u, modifier) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(u.dp(24)))
        Column {
            Text(main, color = Color.White, fontSize = u.sp(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
            detail?.let { Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(12), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun SectionLabel(text: String, u: DesignUnits, top: Dp = 0.dp) {
    Text(text.uppercase(), style = u.eyebrow(), modifier = Modifier.padding(top = top, bottom = u.dp(10)))
}

@Composable
private fun Chip(text: String, u: DesignUnits) {
    Text(
        text, color = Color.White, fontSize = u.sp(13), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(u.dp(12))).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = u.dp(14), vertical = u.dp(8)),
    )
}

@Composable
private fun Card(u: DesignUnits, modifier: Modifier = Modifier.fillMaxWidth().padding(bottom = u.dp(6)), content: @Composable () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(u.dp(PANEL_CARD_RADIUS))).background(PanelCardFill).padding(horizontal = u.dp(14), vertical = u.dp(7)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) { content() }
}

@Composable
private fun BigStat(value: String, label: String, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
        Text(value, color = Color.White, fontSize = u.sp(32), fontWeight = FontWeight.ExtraLight, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(12), fontWeight = FontWeight.Light, maxLines = 1)
    }
}

@Composable
private fun Detail(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun dash(value: Int, loading: Boolean): String = if (loading) "—" else value.toString()

private fun count(value: Int, loading: Boolean, noun: String): String =
    if (loading) "— ${noun}s" else "$value ${if (value == 1) noun else noun + "s"}"
