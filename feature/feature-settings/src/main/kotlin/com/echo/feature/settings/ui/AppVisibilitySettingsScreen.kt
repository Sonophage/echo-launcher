package com.echo.feature.settings.ui

import com.echo.core.ui.components.EchoTrio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.echo.feature.settings.viewmodel.AppVisibilityViewModel
import com.echo.feature.settings.viewmodel.HiddenEntry
import com.echo.feature.settings.viewmodel.HiddenItemGroup

@Composable
fun AppVisibilitySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppVisibilityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    SettingsPageScaffold(
        subtitle = "Hidden Items",
        onBack   = onBack,
        modifier = modifier,
    ) {
        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EchoTrio(color = SettingsAccent)
            }
            return@SettingsPageScaffold
        }

        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // owner, 2026-10-07: the kit's groups and rows; each hidden item is a group of the places it is hidden from
            if (state.groups.isEmpty()) {
                SettingsGroup("Hidden Items")
                SettingsRow(
                    label    = "Nothing is hidden",
                    sublabel = "Hide an app or game from its Options menu; unhide it here",
                )
            } else {
                state.groups.forEach { group ->
                    HiddenItemCard(
                        group = group,
                        onUnhide = { viewModel.unhide(it) },
                        onUnhideAll = { viewModel.unhideAll(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenItemCard(
    group: HiddenItemGroup,
    onUnhide: (HiddenEntry) -> Unit,
    onUnhideAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(group.label)

        group.entries.forEach { entry ->
            SettingsValueRow(
                label   = entry.locationLabel,
                value   = "Unhide",
                onClick = { onUnhide(entry) },
            )
        }
        if (group.entries.size > 1) {
            SettingsValueRow(
                label   = "All locations",
                value   = "Unhide all",
                onClick = onUnhideAll,
            )
        }
    }
}
