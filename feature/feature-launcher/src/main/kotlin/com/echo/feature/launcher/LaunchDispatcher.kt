package com.echo.feature.launcher

import android.content.Context
import android.content.Intent
import com.echo.core.common.launch.LaunchTransition
import com.echo.core.common.launch.LaunchTransition.withoutTransition
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameLaunchListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

fun interface LaunchClock {
    fun now(): Long
}

data class PendingLaunch(
    val game: Game,
    val resolved: ResolvedLaunch?,
    val intentSummary: String,

    val dispatchedAtMs: Long,

    val dispatchedAtWallMs: Long,

    val packageName: String? = null,
)

sealed interface LaunchDispatchResult {
    data class Rejected(val message: String) : LaunchDispatchResult

    data object Accepted : LaunchDispatchResult
}

@Singleton
class LaunchDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outcomeRecorder: LaunchOutcomeRecorder,
    @LaunchDispatcherScope private val scope: CoroutineScope,
    @LaunchRealtimeClock private val clock: LaunchClock,
    @LaunchWallClock private val wallClock: LaunchClock,
    private val gameBootGate: GameBootGate,

    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
    private val autoCoreMemory: AutoCoreMemory,
    private val gameRepository: com.echo.core.domain.repository.GameRepository,
    private val ledger: PlaySessionLedger,
    private val launchListeners: Set<@JvmSuppressWildcards GameLaunchListener> = emptySet(),
) {
    private val _recoveryRequests = MutableStateFlow<LaunchRecoveryRequest?>(null)

    val recoveryRequests: StateFlow<LaunchRecoveryRequest?> = _recoveryRequests.asStateFlow()

    // the last game ECHO sent away and the package it went to. Memory only: a restarted ECHO offers no
    // Resume, which is also when the emulator is least likely to still hold the game
    private val _lastLaunch = MutableStateFlow<OpenSession?>(null)

    val lastLaunch: StateFlow<OpenSession?> = _lastLaunch.asStateFlow()

    private var pending: PendingLaunch? = null
    private var hostStopped = false
    private var watchdog: Job? = null

    suspend fun launch(game: Game, resolved: ResolvedLaunch?, intent: Intent): LaunchDispatchResult {
        // GameBoot's disc and card wear the game's cover, its icon slot, as every cover spot does (owner, 2026-10-05)
        val cover = com.echo.core.domain.model.coverArtOf(game.iconUri, game.artworkUri)
        gameBootGate.awaitPresentation(game.displayTitle, cover, backdropArt = game.artworkUri, cardArt = cover)
        return try {
            context.startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).withoutTransition(),
                LaunchTransition.options(context),
            )

            resolved?.profile?.takeIf { it.isRetroArchProfile() }?.let { profile ->
                autoCoreMemory.remember(game.platformId, profile.id)
            }
            val dispatchedAt = clock.now()
            val dispatchedAtWall = wallClock.now()

            val packageName = listOfNotNull(intent.component?.packageName, intent.`package`, resolved?.profile?.packageName)
                .firstOrNull { it.isNotBlank() }
            scope.launch {
                runCatching { gameRepository.markOpened(game.id, dispatchedAtWall) }
                    .onFailure { Timber.w(it, "Could not stamp gameId=${game.id} on the Last Played shelf") }
                launchListeners.forEach { listener ->
                    runCatching { listener.onGameLaunched(game) }
                        .onFailure { Timber.w(it, "A launch listener failed for gameId=${game.id}") }
                }
            }
            acceptPending(
                PendingLaunch(
                    game         = game,
                    resolved     = resolved,
                    intentSummary = intent.toUri(Intent.URI_INTENT_SCHEME),
                    dispatchedAtMs = dispatchedAt,
                    dispatchedAtWallMs = dispatchedAtWall,
                    packageName  = packageName,
                )
            )
            runCatching { ledger.open(OpenSession(game.id, game.platformId, packageName, dispatchedAtWall)) }
                .onFailure { Timber.w(it, "Could not note the open session for gameId=${game.id}") }
            if (packageName != null) _lastLaunch.value = OpenSession(game.id, game.platformId, packageName, dispatchedAtWall)
            LaunchDispatchResult.Accepted
        } catch (e: android.content.ActivityNotFoundException) {
            Timber.w(e, "Launch startActivity failed: emulator activity not found (gameId=${game.id})")
            settleImmediateFailure(game, resolved, "Emulator not found. Is it installed?")
        } catch (e: SecurityException) {
            Timber.w(e, "Launch startActivity failed: permission denied (gameId=${game.id})")
            settleImmediateFailure(game, resolved, "Permission denied launching emulator")
        } catch (e: Exception) {
            Timber.w(e, "Launch startActivity failed (gameId=${game.id})")
            settleImmediateFailure(game, resolved, "Could not open emulator: ${e.message}")
        }
    }

    // brings the last launch's package back as it was left. The launch itself clears the emulator's task
    // (CLEAR_TASK in the profiles), so this is the only way back into a running game. If Android has since
    // closed the emulator, its own start screen opens instead.
    suspend fun resume(game: Game): Boolean {
        val last = _lastLaunch.value?.takeIf { it.gameId == game.id } ?: return false
        val pkg = last.packageName ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return try {
            context.startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED).withoutTransition(),
                LaunchTransition.options(context),
            )
            val at = clock.now()
            val atWall = wallClock.now()
            acceptPending(PendingLaunch(game, null, intent.toUri(Intent.URI_INTENT_SCHEME), at, atWall, pkg))
            runCatching { ledger.open(OpenSession(game.id, game.platformId, pkg, atWall)) }
            true
        } catch (e: Exception) {
            Timber.w(e, "Resume failed for gameId=${game.id} ($pkg)")
            false
        }
    }

    suspend fun recordPreflightFailure(
        game: Game,
        resolved: ResolvedLaunch?,
        reason: String,
        offerRecovery: Boolean = true,
        kind: LaunchFailureKind = LaunchFailureKind.UNKNOWN,
    ) {
        Timber.w("Launch blocked by preflight: gameId=${game.id}, reason=$reason")
        outcomeRecorder.record(
            outcomeFor(game, resolved, LaunchOutcomeStatus.INTENT_FAILED, reason)
        )
        menuSound.play(com.echo.core.ui.sound.MenuSound.ERROR)
        if (offerRecovery) emitRecovery(game, resolved, reason, kind)
    }

    fun dismissRecovery() {
        _recoveryRequests.value = null
    }

    fun onHostStopped() {
        if (pending == null) return
        hostStopped = true
        watchdog?.cancel()
        watchdog = null
    }

    fun onHostResumed() {
        val p = pending ?: return settleOrphanedSession()
        pending = null
        val emulatorTookForeground = hostStopped
        hostStopped = false
        watchdog?.cancel()
        watchdog = null

        if (emulatorTookForeground) {
            val elapsedMs = clock.now() - p.dispatchedAtMs
            Timber.i("Launch session ${elapsedMs}ms — emulator covered the launcher — recording success")
            scope.launch {
                runCatching { ledger.take() }
                val playedMs = p.packageName
                    ?.let { runCatching { ledger.foregroundMillis(it, p.dispatchedAtWallMs, wallClock.now()) }.getOrNull() }
                    ?.takeIf { it > 0 } ?: elapsedMs
                outcomeRecorder.record(
                    outcomeFor(
                        p.game, p.resolved, LaunchOutcomeStatus.SUCCEEDED, reason = null,
                    ).copy(returnedAtMs = wallClock.now())
                )

                runCatching {
                    gameRepository.recordPlaySession(
                        com.echo.core.domain.model.PlaySession(
                            gameId         = p.game.id,
                            platformId     = p.game.platformId,

                            launchedAt     = p.dispatchedAtWallMs,
                            durationMillis = playedMs,
                        )
                    )
                }.onFailure { Timber.w(it, "Could not record play session for gameId=${p.game.id}") }
            }
        } else {
            Timber.w("Launch returned without the emulator covering the launcher (${clock.now() - p.dispatchedAtMs}ms)")
            scope.launch {
                runCatching { ledger.take() }
                outcomeRecorder.record(
                    outcomeFor(
                        p.game, p.resolved, LaunchOutcomeStatus.NEVER_FOREGROUNDED,
                        "The emulator never came to the foreground after launch.",
                    )
                )
                emitRecovery(
                    p.game, p.resolved,
                    "The emulator never appeared. Check that it is installed and up to date, " +
                        "then try launching again.",
                )
            }
        }
    }

    private fun settleOrphanedSession() {
        scope.launch {
            val open = runCatching { ledger.take() }.getOrNull() ?: return@launch
            val packageName = open.packageName ?: return@launch
            val playedMs = runCatching { ledger.foregroundMillis(packageName, open.launchedAt, wallClock.now()) }.getOrNull()
                ?.takeIf { it > 0 } ?: return@launch
            Timber.i("Settling a session the launcher did not see end: gameId=${open.gameId}, ${playedMs}ms")
            runCatching {
                gameRepository.recordPlaySession(
                    com.echo.core.domain.model.PlaySession(
                        gameId = open.gameId,
                        platformId = open.platformId,
                        launchedAt = open.launchedAt,
                        durationMillis = playedMs,
                    )
                )
            }.onFailure { Timber.w(it, "Could not record the orphaned session for gameId=${open.gameId}") }
        }
    }

    private fun acceptPending(p: PendingLaunch) {
        pending = p
        hostStopped = false

        watchdog = scope.launch {
            delay(STOP_WINDOW_MS)
            val stillPending = pending?.game?.id == p.game.id
            if (stillPending && !hostStopped) {
                pending = null
                runCatching { ledger.take() }
                Timber.w("No activity covered the launcher within ${STOP_WINDOW_MS}ms of dispatch")
                outcomeRecorder.record(
                    outcomeFor(
                        p.game, p.resolved, LaunchOutcomeStatus.NEVER_FOREGROUNDED,
                        "The emulator never came to the foreground after launch.",
                    )
                )
                emitRecovery(
                    p.game, p.resolved,
                    "The emulator never appeared. Check that it is installed and up to date, " +
                        "then try launching again.",
                )
            }
        }
    }

    private suspend fun settleImmediateFailure(
        game: Game,
        resolved: ResolvedLaunch?,
        message: String,
    ): LaunchDispatchResult {
        outcomeRecorder.record(
            outcomeFor(game, resolved, LaunchOutcomeStatus.INTENT_FAILED, message)
        )
        menuSound.play(com.echo.core.ui.sound.MenuSound.ERROR)
        emitRecovery(game, resolved, message)
        return LaunchDispatchResult.Rejected(message)
    }

    private suspend fun emitRecovery(
        game: Game,
        resolved: ResolvedLaunch?,
        message: String,
        kind: LaunchFailureKind = LaunchFailureKind.UNKNOWN,
    ) {
        val recent = runCatching { outcomeRecorder.recentForGame(game.id, RECENT_LIMIT) }
            .getOrDefault(emptyList())
        val recentFailures = recent.count { it.status != LaunchOutcomeStatus.SUCCEEDED }
        val historyLine = when {
            recent.isEmpty() -> null
            recentFailures == 0 -> null
            else -> "$recentFailures of the last ${recent.size} launches for this game failed."
        }
        _recoveryRequests.value = LaunchRecoveryRequest(
            gameId          = game.id,
            gameTitle       = game.displayTitle,
            platformId      = game.platformId,
            resolved        = resolved,
            message         = message,
            historyLine     = historyLine,
            diagnostic      = buildDiagnostic(game, resolved, recent.firstOrNull()),
            kind            = kind,
        )
    }

    private fun outcomeFor(
        game: Game,
        resolved: ResolvedLaunch?,
        status: LaunchOutcomeStatus,
        reason: String?,
    ) = LaunchOutcome(
        gameId        = game.id,
        gameTitle     = game.displayTitle,
        platformId    = game.platformId,
        emulatorId    = resolved?.profile?.id,
        emulatorName  = resolved?.profile?.name,
        corePath      = resolved?.corePath,
        coreName      = resolved?.coreName,
        source        = resolved?.source,
        status        = status,
        failureReason = reason,
        launchedAtMs  = System.currentTimeMillis(),
    )

    private fun buildDiagnostic(
        game: Game,
        resolved: ResolvedLaunch?,
        last: LaunchOutcome?,
    ): String = buildString {
        appendLine("ECHO — launch diagnostic")
        appendLine("Game: ${game.title} (id ${game.id})")
        appendLine("Platform: ${game.platformId}")
        appendLine("Emulator: ${resolved?.profile?.name ?: last?.emulatorName ?: "unknown"}")
        if (resolved?.coreName != null) appendLine("Core: ${resolved.coreName}")
        appendLine("Source: ${resolved?.source?.name ?: last?.source?.name ?: "n/a"}")
        appendLine("ROM: ${game.romPath ?: game.romUri ?: game.packageName ?: game.launchToken ?: "n/a"}")
        if (game.isMissing) appendLine("Missing: yes (file not found on last scan)")
        if (last?.failureReason != null) appendLine("Last failure: ${last.failureReason}")
    }

    companion object {
        const val STOP_WINDOW_MS = 6_000L
        private const val RECENT_LIMIT = 5
    }
}
