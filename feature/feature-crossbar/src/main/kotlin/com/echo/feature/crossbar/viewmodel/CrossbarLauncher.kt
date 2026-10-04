package com.echo.feature.crossbar.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.resolve
import com.echo.core.ui.media.resolveGameBootAudio
import com.echo.feature.appbar.LauncherShortcutRepository
import com.echo.feature.launcher.LaunchDispatchResult
import com.echo.feature.launcher.LaunchRecoveryAction
import com.echo.feature.launcher.ResolvedLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CrossbarLauncher(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val launchDispatcher: com.echo.feature.launcher.LaunchDispatcher,
    private val launchResolver: com.echo.feature.launcher.GameLaunchResolver,
    private val intentResolver: com.echo.feature.launcher.EmulatorIntentResolver,
    private val gameBootGate: com.echo.feature.launcher.GameBootGate,
    private val mediaLaunchGate: com.echo.core.data.launch.MediaLaunchGate,
    private val launcherShortcutRepository: LauncherShortcutRepository,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun observeLaunchRecoveryRequests() {
        scope.launch {
            launchDispatcher.recoveryRequests.collect { request ->
                uiState.update { it.copy(launchRecovery = request) }
            }
        }
    }

    fun onLaunchRecoveryAction(action: LaunchRecoveryAction) {
        when (action) {
            LaunchRecoveryAction.DISMISS -> launchDispatcher.dismissRecovery()
            LaunchRecoveryAction.RETRY   -> {
                val request = uiState.value.launchRecovery ?: return
                launchDispatcher.dismissRecovery()
                launchGameDirectly(request.gameId)
            }
            LaunchRecoveryAction.CHANGE_EMULATOR -> {
                val gameId = uiState.value.launchRecovery?.gameId ?: return
                launchDispatcher.dismissRecovery()
                vm.openEmulatorPickerMenu(gameId)
            }
            LaunchRecoveryAction.PER_SYSTEM_DEFAULTS -> {
                val gameId = uiState.value.launchRecovery?.gameId
                launchDispatcher.dismissRecovery()
                scope.launch {
                    val platformId = gameId?.let { vm.gameRepository.getById(it) }?.platformId
                    if (platformId != null) vm.openDefaultEmulatorMenu(platformId)
                    else uiState.update { it.withSettingsOpen("settings_library") }
                }
            }

            LaunchRecoveryAction.OPEN_LIBRARY -> {
                launchDispatcher.dismissRecovery()
                uiState.update { it.withSettingsOpen("settings_library") }
            }
            LaunchRecoveryAction.COPY_DIAGNOSTIC -> {
                val request = uiState.value.launchRecovery ?: return
                val cm = vm.context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText(
                    "ECHO launch diagnostic", request.diagnostic,
                ))
                vm.taskNotifier.complete(
                    "launch_diag_${request.gameId}", request.gameTitle, "Diagnostic copied to clipboard",
                )
            }
        }
    }

    internal fun launchCamera() {
        runCatching {
            vm.context.startActivity(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { Timber.w(it, "Could not launch a camera app") }
    }

    internal fun setPlatformEmulator(platformId: String, profileId: String?) {
        vm.appAction { vm.memoryCardRepository.setEmulator(platformId, profileId) }
    }

    internal fun clearPlatformEmulatorOverrides(platformId: String) {
        vm.appAction { vm.gameRepository.clearPreferredEmulatorForPlatform(platformId) }
    }

    fun launchGameFromDrawer(gameId: Long) {
        uiState.update { it.copy(activeAppDrawerFilter = null, pendingDrawerAction = null) }
        launchGameDirectly(gameId)
    }

    internal fun launchGameDirectly(gameId: Long, discId: Long? = null) {
        scope.launch {
            val selected = vm.gameRepository.getById(gameId) ?: run {
                Timber.w("Direct launch requested for missing game id=$gameId")
                return@launch
            }
            val game = if (discId != null) vm.gameRepository.getById(discId) ?: selected else selected
            if (game.isMissing) {
                Timber.i("Direct launch refused for missing game: ${game.title}")
                return@launch
            }
            launchResolvedGame(game)
        }
    }

    private suspend fun launchResolvedGame(game: Game) {
        val shortcutId = game.shortcutId
        val packageName = game.packageName
        if (shortcutId != null && packageName != null) {
            launcherShortcutRepository.launch(packageName, shortcutId)
                .onFailure { e ->
                    Timber.w(e, "Direct shortcut launch failed")
                    launchDispatcher.recordPreflightFailure(game, null, "Couldn't launch: ${e.message}")
                }
            return
        }
        if (game.launchIntentUri != null) {
            runCatching {
                val parsed = Intent.parseUri(game.launchIntentUri, Intent.URI_INTENT_SCHEME)
                com.echo.core.common.security.ShortcutIntentSanitizer.sanitize(parsed, vm.context.packageManager)
                    ?: error("Captured shortcut is not safe to launch")
            }.onSuccess { intent -> launchIntentFromCrossbar(intent, game, null) }
                .onFailure { e ->
                    Timber.w(e, "Direct stored-intent launch failed")
                    launchDispatcher.recordPreflightFailure(game, null, "Couldn't launch: ${e.message}")
                }
            return
        }
        if (game.romPath.isNullOrBlank() && !game.packageName.isNullOrBlank()) {
            intentResolver.resolveNativeApp(game).fold(
                onSuccess = { intent -> launchIntentFromCrossbar(intent, game, null) },
                onFailure = { e ->
                    Timber.w(e, "Direct native-app launch failed")
                    launchDispatcher.recordPreflightFailure(game, null, e.message ?: "Could not launch ${game.title}")
                },
            )
            return
        }

        val resolvedLaunch = launchResolver.resolve(game).getOrElse { reason ->
            Timber.w(reason, "Direct launch unresolved: gameId=${game.id}, platform=${game.platformId}")

            launchDispatcher.recordPreflightFailure(
                game, null,
                reason.message ?: "No emulator is set up for ${game.platformId.uppercase()}.",
            )
            return
        }
        val profile = resolvedLaunch.profile

        val validation = runCatching { intentResolver.validateBeforeLaunch(game, profile) }
        if (validation.isFailure) {
            Timber.w(
                validation.exceptionOrNull(),
                "Direct emulator launch blocked by preflight: ${profile.name}",
            )
            val blocked = validation.exceptionOrNull() as? com.echo.feature.launcher.LaunchBlockedException
            launchDispatcher.recordPreflightFailure(
                game, resolvedLaunch,
                validation.exceptionOrNull()?.message ?: "Could not launch ${profile.name}",

                kind = blocked?.kind ?: com.echo.feature.launcher.LaunchFailureKind.UNKNOWN,
            )
            return
        }
        intentResolver.resolve(game, profile).fold(
            onSuccess = { intent -> launchIntentFromCrossbar(intent, game, resolvedLaunch) },
            onFailure = { e ->
                Timber.w(e, "Direct emulator launch failed: ${profile.name}")
                launchDispatcher.recordPreflightFailure(game, resolvedLaunch, e.message ?: "Could not launch ${profile.name}")
            },
        )
    }

    private suspend fun launchIntentFromCrossbar(intent: Intent, game: Game, resolved: ResolvedLaunch?) {
        when (val result = launchDispatcher.launch(game, resolved, intent)) {
            is LaunchDispatchResult.Rejected -> Timber.w("Direct launch rejected: ${result.message}")
            LaunchDispatchResult.Accepted -> Unit
        }
    }

    internal fun launchHarvestedShortcut(hostPackage: String?, shortcutId: String?) {
        if (hostPackage == null || shortcutId == null) return
        launcherShortcutRepository.launch(hostPackage, shortcutId).onFailure { e ->
            Timber.e(e, "Failed to launch shortcut $hostPackage/$shortcutId")
            vm.taskNotifier.failed("launch_sc_$shortcutId", hostPackage, "Couldn't launch: ${e.message}")
        }
    }

    internal fun launchStoredIntent(intentUri: String, label: String) {
        runCatching {
            val parsed = android.content.Intent.parseUri(intentUri, android.content.Intent.URI_INTENT_SCHEME)

            val launch = (com.echo.core.common.security.ShortcutIntentSanitizer
                .sanitize(parsed, vm.context.packageManager)
                ?: error("Captured shortcut is not safe to launch"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
            vm.context.startActivity(
                launch,
                com.echo.core.common.launch.LaunchTransition.options(vm.context),
            )
        }.onFailure { e ->
            Timber.e(e, "Failed to launch captured shortcut: $label")
            vm.taskNotifier.failed("launch_intent_${label.hashCode()}", label, "Couldn't launch: ${e.message}")
        }
    }

    internal fun observeBootPreferences() {
        scope.launch {
            val prefs = vm.context.echoDataStore.data.first()
            vm.bootEnabled = prefs[CrossbarViewModel.KEY_SHOW_BOOT] ?: true
            vm.bootOnResume = prefs[CrossbarViewModel.KEY_BOOT_ON_RESUME] ?: false
            if (!vm.bootEnabled) {
                uiState.update { it.copy(showBootSequence = false) }
            }
        }
        scope.launch {
            vm.context.echoDataStore.data
                .map { (it[CrossbarViewModel.KEY_SHOW_BOOT] ?: true) to (it[CrossbarViewModel.KEY_BOOT_ON_RESUME] ?: false) }
                .distinctUntilChanged()
                .collect { (enabled, onResume) ->
                    vm.bootEnabled = enabled
                    vm.bootOnResume = onResume
                }
        }

        scope.launch {
            vm.uiMediaStore.stamp
                .distinctUntilChanged()
                .collect {
                    val (video, audio) = withContext(Dispatchers.IO) {
                        val video = vm.uiMediaStore.pathFor(com.echo.core.domain.model.UiMediaSlot.BOOT_VIDEO)
                        val audio = vm.uiMediaStore.pathFor(com.echo.core.domain.model.UiMediaSlot.BOOT_AUDIO)
                        video to audio
                    }
                    uiState.update { it.copy(bootVideoPath = video, bootAudioPath = audio) }
                }
        }
    }

    internal fun observeMediaLaunch() {
        scope.launch {
            mediaLaunchGate.active.collect { request ->
                uiState.update { it.copy(discCeremony = request?.let { r -> DiscCeremonyState(r.art) }) }
            }
        }
    }

    internal suspend fun awaitDiscHandOff(art: Any?) = mediaLaunchGate.awaitHandOff(art)

    internal fun launchAppWithDisc(packageName: String, art: Any?) {
        scope.launch {
            awaitDiscHandOff(art ?: appLauncherIcon(packageName))
            vm.appCategoryRepository.launch(packageName)
        }
    }

    private fun appLauncherIcon(packageName: String): Any? =
        runCatching { vm.context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    fun onDiscCeremonyHandOff() = mediaLaunchGate.onHandOff()

    fun onDiscCeremonyFinished() = mediaLaunchGate.onDismissed()

    internal fun observeGameBoot() {
        scope.launch {
            gameBootGate.active.collect { request ->
                uiState.update {
                    if (request == null && it.gameBootIsPreview) it
                    else it.copy(activeGameBoot = request, gameBootIsPreview = false)
                }
            }
        }
    }

    fun onGameBootHandOff() {
        if (!uiState.value.gameBootIsPreview) gameBootGate.onPresentationFinished()
    }

    fun onGameBootComplete() {
        val wasPreview = uiState.value.gameBootIsPreview
        uiState.update { it.copy(activeGameBoot = null, gameBootIsPreview = false) }
        if (wasPreview) {
            vm.uiMediaAudioPlayer.stop()
        } else {
            gameBootGate.onPresentationFinished()
            gameBootGate.onPresentationDismissed()
        }
    }

    fun previewBootSequence() {
        uiState.update { it.copy(showBootSequence = true) }
    }

    fun previewGameBoot() {
        scope.launch {
            val (video, audio) = withContext(Dispatchers.IO) {
                val customVideo = vm.uiMediaStore.pathFor(com.echo.core.domain.model.UiMediaSlot.GAMEBOOT_VIDEO)
                customVideo to resolveGameBootAudio(
                    customVideoPath = customVideo,
                    customAudioPath = vm.uiMediaStore.pathFor(
                        com.echo.core.domain.model.UiMediaSlot.GAMEBOOT_AUDIO,
                    ),
                )
            }

            val previewArt = if (video != null) null else runCatching {
                vm.gameRepository.observeAllGames().first().firstNotNullOfOrNull { it.discFaceUri }
            }.getOrNull()

            audio?.let {
                vm.uiMediaAudioPlayer.play(
                    uri = it,
                    clipEndMs = com.echo.themekit.UiMediaLimits.GAMEBOOT_SEQUENCE_MS,
                    label = "gameboot-preview",
                )
            }
            uiState.update {
                it.copy(
                    activeGameBoot = com.echo.feature.launcher.GameBootRequest(
                        gameTitle = "Preview",
                        videoPath = video,
                        audioPath = audio,
                        coverArt = previewArt,
                    ),
                    gameBootIsPreview = true,
                )
            }
        }
    }

    fun onBootSequenceComplete() {
        Timber.d("StartupSeq: boot sequence complete")
        uiState.update { it.copy(showBootSequence = false) }
    }
}
