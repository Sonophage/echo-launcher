package com.echo.feature.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.echo.core.data.achievement.AchievementCredentialsProvider
import com.echo.feature.achievements.AchievementController
import com.echo.feature.achievements.BatchSyncResult
import com.echo.feature.achievements.RaAccountImporter
import com.echo.feature.achievements.RaImportResult
import com.echo.feature.achievements.SteamImportWorker
import com.echo.feature.achievements.match.AchievementAutoMatcher
import com.echo.feature.achievements.match.MatchReport
import com.echo.feature.achievements.provider.steam.SteamRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AchievementsSettingsUiState(
    val enabled: Boolean = false,
    val hasRetroAchievements: Boolean = false,
    val raUsername: String = "",
    val hasSteam: Boolean = false,
    val steamId64: String = "",
    val lastSyncedLabel: String = "Never",
    val message: String? = null,
    val isMatching: Boolean = false,
    val matchDone: Int = 0,
    val matchTotal: Int = 0,
    val matchReport: MatchReport? = null,
    val isSyncing: Boolean = false,
    val syncDone: Int = 0,
    val syncTotal: Int = 0,
    val syncResult: BatchSyncResult? = null,
    val isImporting: Boolean = false,
    val importDone: Int = 0,
    val importTotal: Int = 0,
    val importResult: RaImportResult? = null,
    val isSteamImporting: Boolean = false,
    val steamImportDone: Int = 0,
    val steamImportTotal: Int = 0,
    val steamImportSummary: String? = null,
)

private data class Accounts(
    val raUsername: String?,
    val steamId64: String?,
    val enabled: Boolean,
    val lastSyncedAt: Long?,
)

private data class Extra(
    val message: String? = null,
    val isMatching: Boolean = false,
    val matchDone: Int = 0,
    val matchTotal: Int = 0,
    val matchReport: MatchReport? = null,
    val isSyncing: Boolean = false,
    val syncDone: Int = 0,
    val syncTotal: Int = 0,
    val syncResult: BatchSyncResult? = null,
    val isImporting: Boolean = false,
    val importDone: Int = 0,
    val importTotal: Int = 0,
    val importResult: RaImportResult? = null,
    val isSteamImporting: Boolean = false,
    val steamImportDone: Int = 0,
    val steamImportTotal: Int = 0,
    val steamImportSummary: String? = null,
)

@HiltViewModel
class AchievementsSettingsViewModel @Inject constructor(
    private val credentials: AchievementCredentialsProvider,
    private val steamApi: SteamRemoteDataSource,
    private val autoMatcher: AchievementAutoMatcher,
    private val repository: AchievementController,
    private val raImporter: RaAccountImporter,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val extra = MutableStateFlow(Extra())

    init {
        runCatching { WorkManager.getInstance(context) }.getOrNull()?.let { wm ->
            viewModelScope.launch {
                wm.getWorkInfosForUniqueWorkFlow(SteamImportWorker.UNIQUE_NAME)
                    .collect { infos -> onSteamImportWorkInfos(infos) }
            }
        }
    }

    private fun onSteamImportWorkInfos(infos: List<WorkInfo>) {
        val active = infos.firstOrNull { !it.state.isFinished }
        if (active != null) {
            val p = active.progress
            extra.update {
                it.copy(
                    isSteamImporting = true,
                    steamImportSummary = null,
                    steamImportDone = p.getInt(SteamImportWorker.KEY_DONE, 0),
                    steamImportTotal = p.getInt(SteamImportWorker.KEY_TOTAL, 0),
                )
            }
            return
        }
        if (!extra.value.isSteamImporting) return
        val finished = infos.maxByOrNull { it.state.ordinal }
        val summary = when (finished?.state) {
            WorkInfo.State.SUCCEEDED -> steamSummaryOf(finished.outputData)
            WorkInfo.State.CANCELLED -> "Import cancelled, progress so far is kept"
            else -> "Import failed, run it again to resume"
        }
        extra.update { it.copy(isSteamImporting = false, steamImportSummary = summary) }
    }

    private fun steamSummaryOf(out: Data): String = when {
        out.getBoolean(SteamImportWorker.KEY_MISSING_CREDENTIALS, false) ->
            "Steam needs credentials, connect your account first"
        out.getBoolean(SteamImportWorker.KEY_PROFILE_NOT_PUBLIC, false) ->
            "Your Steam profile's Game Details are private"
        else -> buildString {
            append("${out.getInt(SteamImportWorker.KEY_IMPORTED, 0)} imported")
            out.getInt(SteamImportWorker.KEY_NO_COINS, 0).takeIf { it > 0 }
                ?.let { append(" · $it without achievements") }
            out.getInt(SteamImportWorker.KEY_NO_PROGRESS, 0).takeIf { it > 0 }
                ?.let { append(" · $it not played yet") }
            out.getInt(SteamImportWorker.KEY_FAILED, 0).takeIf { it > 0 }
                ?.let { append(" · $it failed") }
        }
    }

    private val accounts = combine(
        credentials.raUsernameFlow,
        credentials.steamId64Flow,
        credentials.enabledFlow,
        credentials.lastSyncedAtFlow,
    ) { raUser, steamId, enabled, lastSynced ->
        Accounts(raUser, steamId, enabled, lastSynced)
    }

    val uiState: StateFlow<AchievementsSettingsUiState> = combine(accounts, extra) { acc, ex ->
        AchievementsSettingsUiState(
            enabled = acc.enabled,
            hasRetroAchievements = !acc.raUsername.isNullOrBlank(),
            raUsername = acc.raUsername.orEmpty(),
            hasSteam = !acc.steamId64.isNullOrBlank(),
            steamId64 = acc.steamId64.orEmpty(),
            lastSyncedLabel = acc.lastSyncedAt?.let { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(it)) } ?: "Never",
            message = ex.message,
            isMatching = ex.isMatching,
            matchDone = ex.matchDone,
            matchTotal = ex.matchTotal,
            matchReport = ex.matchReport,
            isSyncing = ex.isSyncing,
            syncDone = ex.syncDone,
            syncTotal = ex.syncTotal,
            syncResult = ex.syncResult,
            isImporting = ex.isImporting,
            importDone = ex.importDone,
            importTotal = ex.importTotal,
            importResult = ex.importResult,
            isSteamImporting = ex.isSteamImporting,
            steamImportDone = ex.steamImportDone,
            steamImportTotal = ex.steamImportTotal,
            steamImportSummary = ex.steamImportSummary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsSettingsUiState())

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { credentials.setEnabled(enabled) }
    }

    fun connectRetroAchievements(username: String, apiKey: String) {
        viewModelScope.launch {
            val protection = credentials.saveRetroAchievements(username, apiKey)
            val message = ServiceConnectors.unprotectedWarning("RetroAchievements Web API key", protection)
                ?: "RetroAchievements connected"
            extra.update { it.copy(message = message) }
        }
    }

    fun disconnectRetroAchievements() {
        viewModelScope.launch {
            credentials.clearRetroAchievements()
            extra.update { it.copy(message = "RetroAchievements disconnected") }
        }
    }

    fun connectSteam(idOrVanity: String, apiKey: String) {
        viewModelScope.launch {
            val message = ServiceConnectors.connectSteam(credentials, steamApi, idOrVanity, apiKey)
            extra.update { it.copy(message = message) }
        }
    }

    fun disconnectSteam() {
        viewModelScope.launch {
            credentials.clearSteam()
            extra.update { it.copy(message = "Steam disconnected") }
        }
    }

    private var syncJob: Job? = null

    fun syncNow() {
        if (extra.value.isMatching) return
        viewModelScope.launch {
            syncJob?.cancelAndJoin()
            extra.update { it.copy(isMatching = true, matchReport = null, matchDone = 0, matchTotal = 0) }
            val report = autoMatcher.matchUnlinked { done, total ->
                extra.update { it.copy(matchDone = done, matchTotal = total) }
            }
            extra.update { it.copy(isMatching = false, matchReport = report) }
            launchSyncAll().join()
        }
    }

    private fun launchSyncAll(): Job {
        val job = viewModelScope.launch {
            try {
                extra.update { it.copy(isSyncing = true, syncResult = null, syncDone = 0, syncTotal = 0) }
                val result = repository.syncAllLinked { done, total ->
                    extra.update { it.copy(syncDone = done, syncTotal = total) }
                }
                extra.update { it.copy(isSyncing = false, syncResult = result) }
            } finally {
                if (extra.value.isSyncing) extra.update { it.copy(isSyncing = false) }
            }
        }
        syncJob = job
        return job
    }

    fun importRaHistory() {
        if (extra.value.isImporting || extra.value.isSyncing || extra.value.isMatching) return
        viewModelScope.launch {
            try {
                extra.update { it.copy(isImporting = true, importResult = null, importDone = 0, importTotal = 0) }
                val result = raImporter.import { done, total ->
                    extra.update { it.copy(importDone = done, importTotal = total) }
                }
                extra.update { it.copy(isImporting = false, importResult = result) }
            } finally {
                if (extra.value.isImporting) extra.update { it.copy(isImporting = false) }
            }
        }
    }

    fun importSteamLibrary() {
        SteamImportWorker.enqueue(context)
    }

    fun cancelSteamImport() {
        SteamImportWorker.cancel(context)
    }

    fun dismissMessage() = extra.update { it.copy(message = null) }
    fun dismissReport() = extra.update { it.copy(matchReport = null) }
    fun dismissSyncResult() = extra.update { it.copy(syncResult = null) }
    fun dismissImportResult() = extra.update { it.copy(importResult = null) }
    fun dismissSteamImportSummary() = extra.update { it.copy(steamImportSummary = null) }
}
