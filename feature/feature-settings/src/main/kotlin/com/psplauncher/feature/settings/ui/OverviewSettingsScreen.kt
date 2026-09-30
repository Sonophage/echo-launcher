package com.psplauncher.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psplauncher.core.common.format.formatByteSize
import com.psplauncher.core.ui.components.PfpArtCard
import com.psplauncher.core.ui.components.PfpArtCardComplete
import com.psplauncher.core.ui.components.PfpArtCardPartial
import com.psplauncher.feature.settings.viewmodel.OverviewSettingsViewModel

@Composable
fun OverviewSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val card = Modifier.weight(1f).fillMaxHeight()

            PfpArtCard(
                title = "Library",
                artUri = state.libraryArt,
                focused = false,
                lit = true,
                accent = SettingsAccent,
                modifier = card,
            ) {
                Footer {
                    Headline(count(state.games, state.loading), "games")
                    Detail("${count(state.tracks, state.loading)} tracks · ${count(state.books, state.loading)} books")
                    Detail("${count(state.videos, state.loading)} videos")
                }
            }

            val art = state.artwork
            PfpArtCard(
                title = "Artwork",
                artUri = state.artworkArt,
                focused = false,
                lit = true,
                accent = SettingsAccent,
                modifier = card,
            ) {
                Footer {
                    Text(
                        "${art.complete} of ${art.total} complete",
                        color = if (art.missing == 0 && art.stale == 0) PfpArtCardComplete else PfpArtCardPartial,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                    Detail("${art.missing} missing · ${art.stale} stale")
                    Detail("Cache ${state.artworkCacheBytes?.let { formatByteSize(it) } ?: "measuring…"}")
                }
            }

            PfpArtCard(
                title = "Build",
                artUri = null,
                focused = false,
                lit = true,
                accent = SettingsAccent,
                modifier = card,
                emptyArt = {
                    Text(
                        packageInfo?.versionName ?: "unknown",
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                },
            ) {
                Footer {
                    Headline(packageInfo?.longVersionCode?.toString() ?: "—", "build")
                    Detail("On Android ${android.os.Build.VERSION.RELEASE}")
                }
            }
        }
    }
}

@Composable
private fun Footer(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.height(78.dp), content = content)
}

@Composable
private fun ColumnScope.Headline(value: String, unit: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("  $unit", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1,
            modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun ColumnScope.Detail(text: String) {
    Text(
        text, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

private fun count(value: Int, loading: Boolean): String = if (loading) "—" else value.toString()
