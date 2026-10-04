package com.echo.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.echo.feature.settings.viewmodel.AchievementsSettingsViewModel

@Composable
fun AccountsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AchievementsSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var raUsernameDraft by remember(state.raUsername) { mutableStateOf("") }
    var raKeyDraft by remember(state.hasRetroAchievements) { mutableStateOf("") }
    var steamIdDraft by remember(state.steamId64) { mutableStateOf("") }
    var steamKeyDraft by remember(state.hasSteam) { mutableStateOf("") }

    SettingsPageScaffold(
        subtitle = "Accounts",
        onBack   = onBack,
        modifier = modifier,
    ) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            SettingsToggleRow(
                label    = "Achievements",
                sublabel = "Sync your RetroAchievements and Steam achievements",
                checked  = state.enabled,
                onToggle = { viewModel.setEnabled(it) },
            )

            SettingsGroup("RetroAchievements")

            if (state.hasRetroAchievements) {
                SettingsValueRow(label = "Connected as", value = state.raUsername)
            }
            SettingsTextFieldRow(
                label         = "Username",
                value         = raUsernameDraft,
                onValueChange = { raUsernameDraft = it },
                placeholder   = state.raUsername.ifBlank { "Your RetroAchievements username" },
            )
            SettingsTextFieldRow(
                label         = if (state.hasRetroAchievements) "Web API Key (saved)" else "Web API Key",
                value         = raKeyDraft,
                onValueChange = { raKeyDraft = it },
                placeholder   = if (state.hasRetroAchievements) "••••••••  (tap to replace)" else "Paste your Web API key",
                isPassword    = true,
                helper        = "retroachievements.org, Settings, Keys, Web API Key",
            )
            if (raUsernameDraft.isNotBlank() && raKeyDraft.isNotBlank()) {
                SettingsRow(
                    label   = "Connect RetroAchievements",
                    onClick = {
                        viewModel.connectRetroAchievements(raUsernameDraft, raKeyDraft)
                        raUsernameDraft = ""
                        raKeyDraft = ""
                    },
                )
            }
            if (state.hasRetroAchievements) {
                SettingsRow(
                    label    = "Disconnect RetroAchievements",
                    sublabel = "Removes your username and key from this device",
                    onClick  = { viewModel.disconnectRetroAchievements() },
                )
            }

            SettingsGroup("Steam")

            if (state.hasSteam) {
                SettingsValueRow(label = "SteamID64", value = state.steamId64)
            }
            SettingsTextFieldRow(
                label         = "SteamID64 or profile name",
                value         = steamIdDraft,
                onValueChange = { steamIdDraft = it },
                placeholder   = state.steamId64.ifBlank { "76561… or your profile name" },
                helper        = "A profile name is resolved to a SteamID64 when you connect",
            )
            SettingsTextFieldRow(
                label         = if (state.hasSteam) "Web API Key (saved)" else "Web API Key",
                value         = steamKeyDraft,
                onValueChange = { steamKeyDraft = it },
                placeholder   = if (state.hasSteam) "••••••••  (tap to replace)" else "Paste your Steam Web API key",
                isPassword    = true,
                helper        = "steamcommunity.com/dev. Your profile's Game Details must be public",
            )
            if (steamIdDraft.isNotBlank() && steamKeyDraft.isNotBlank()) {
                SettingsRow(
                    label   = "Connect Steam",
                    onClick = {
                        viewModel.connectSteam(steamIdDraft, steamKeyDraft)
                        steamIdDraft = ""
                        steamKeyDraft = ""
                    },
                )
            }
            if (state.hasSteam) {
                SettingsRow(
                    label    = "Disconnect Steam",
                    sublabel = "Removes your SteamID and key from this device",
                    onClick  = { viewModel.disconnectSteam() },
                )
            }

            state.message?.let {
                SettingsRow(label = it, sublabel = "Tap to dismiss", onClick = { viewModel.dismissMessage() })
            }

            SettingsGroup("Sync")

            SettingsRow(
                label    = "Sync now",
                value    = state.lastSyncedLabel,
                sublabel = when {
                    !state.enabled -> "Turn on Achievements to sync"
                    state.isMatching -> "Matching games ${state.matchDone} / ${state.matchTotal}"
                    state.isSyncing -> "Syncing ${state.syncDone} / ${state.syncTotal}"
                    else -> "Link your games by ROM hash and Steam id, then fetch every set"
                },
                enabled  = state.enabled && !state.isMatching && !state.isSyncing,
                onClick  = { viewModel.syncNow() },
            )
            state.matchReport?.let { report ->
                SettingsRow(
                    label    = "Matched ${report.matched} · Not matched ${report.unmatched.size}",
                    sublabel = "Tap to dismiss",
                    onClick  = { viewModel.dismissReport() },
                )
            }
            state.syncResult?.let { r ->
                SettingsRow(
                    label    = buildString {
                        append("${r.synced} synced")
                        if (r.noCoins > 0) append(" · ${r.noCoins} without achievements")
                        if (r.failed > 0) append(" · ${r.failed} failed")
                    },
                    sublabel = if (r.missingCredentials) "Some accounts need credentials. Tap to dismiss"
                               else "Tap to dismiss",
                    onClick  = { viewModel.dismissSyncResult() },
                )
            }

            SettingsRow(
                label    = "Import RetroAchievements history",
                sublabel = if (state.isImporting) "Importing ${state.importDone} / ${state.importTotal}"
                           else "Every game you have progress in, with or without a ROM here",
                enabled  = state.enabled && state.hasRetroAchievements && !state.isImporting,
                onClick  = { viewModel.importRaHistory() },
            )
            state.importResult?.let { r ->
                SettingsRow(
                    label    = if (r.missingCredentials) "RetroAchievements needs credentials"
                               else "${r.imported} imported · ${r.noCoins} without achievements · ${r.failed} failed",
                    sublabel = "Tap to dismiss",
                    onClick  = { viewModel.dismissImportResult() },
                )
            }

            if (state.isSteamImporting) {
                SettingsRow(
                    label    = "Cancel Steam library import",
                    sublabel = "Importing ${state.steamImportDone} / ${state.steamImportTotal}. Progress so far is kept",
                    onClick  = { viewModel.cancelSteamImport() },
                )
            } else {
                SettingsRow(
                    label    = "Import Steam library",
                    sublabel = "Every owned game you have played. A large library takes a while",
                    enabled  = state.enabled && state.hasSteam,
                    onClick  = { viewModel.importSteamLibrary() },
                )
            }
            state.steamImportSummary?.let {
                SettingsRow(label = it, sublabel = "Tap to dismiss", onClick = { viewModel.dismissSteamImportSummary() })
            }
        }
    }
}
