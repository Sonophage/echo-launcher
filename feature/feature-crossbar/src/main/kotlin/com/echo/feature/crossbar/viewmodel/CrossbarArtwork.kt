package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import com.echo.feature.artwork.api.ArtworkRepository
import com.echo.feature.artwork.match.MetadataApply
import com.echo.feature.artwork.match.MetadataApplyPolicy
import com.echo.feature.artwork.match.MetadataField
import com.echo.feature.crossbar.ui.detail.MetadataPreviewUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CrossbarArtwork(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val artworkRepository: ArtworkRepository,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun observeMediaCovers() {
        scope.launch {
            combine(
                vm.musicRepository.observeNewestArtUris(MEDIA_COVER_POOL),
                vm.videoRepository.observeNewestArtUris(MEDIA_COVER_POOL),
                vm.photoRepository.observeNewestArtUris(MEDIA_COVER_POOL),
                vm.bookRepository.observeNewestArtUris(MEDIA_COVER_POOL),
            ) { music, video, photo, books -> MediaCovers(music, video, photo, books) }
                .collect { covers ->
                    if (uiState.value.mediaCovers == covers) return@collect
                    uiState.update { it.copy(mediaCovers = covers) }
                    val id = vm.currentCategory()?.id
                    if (id == BuiltInCategory.MUSIC || id == BuiltInCategory.VIDEO ||
                        id == BuiltInCategory.PHOTO || id == BuiltInCategory.LIBRARY
                    ) {
                        vm.loadItemsForCategory(vm.currentCategory(), keepCursorOnRow = true)
                    }
                }
        }
    }


    internal fun scrapeMissingArtworkForPlatform(platformId: String) {
        scope.launch {
            val taskId = "scrape_missing_$platformId"
            vm.addBackgroundTask(BackgroundTaskInfo(id = taskId, label = "Scraping missing artwork: ${vm.cardName(platformId)}", progress = 0f))
            runCatching {
                artworkRepository.scrapeMissingForPlatform(platformId) { p ->
                    vm.updateBackgroundTask(taskId, p.current.toFloat() / p.total.coerceAtLeast(1))
                }
            }.onSuccess { result ->
                vm.completeBackgroundTask(taskId,
                    if (result.total == 0) "No games are missing artwork"
                    else "${result.succeeded} of ${result.total} game(s) updated"
                )
                vm.loadItemsForCategory(vm.currentCategory())
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                vm.failBackgroundTask(taskId, "Artwork scrape failed")
            }
        }
    }

    internal fun updatePlatformMetadata(platformId: String) {
        scope.launch {
            val taskId = "update_metadata_$platformId"
            vm.addBackgroundTask(BackgroundTaskInfo(id = taskId, label = "Updating metadata: ${vm.cardName(platformId)}", progress = 0f))
            runCatching {
                artworkRepository.updateMetadataForPlatform(platformId) { p ->
                    vm.updateBackgroundTask(taskId, p.current.toFloat() / p.total.coerceAtLeast(1))
                }
            }.onSuccess { result ->
                vm.completeBackgroundTask(taskId,
                    if (result.total == 0) "No games on this card"
                    else "${result.succeeded} of ${result.total} game(s) updated"
                )
                vm.loadItemsForCategory(vm.currentCategory())
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                vm.failBackgroundTask(taskId, "Metadata update failed")
            }
        }
    }

    fun openArtworkStudio(gameId: Long) {
        vm.closeContextMenu()
        uiState.update { it.copy(artworkStudioGameId = gameId) }
    }

    fun consumeArtworkStudioAction() =
        uiState.update { it.copy(pendingArtworkStudioAction = null) }

    fun closeArtworkStudio() {
        val id = uiState.value.artworkStudioGameId
        uiState.update { it.copy(artworkStudioGameId = null) }
        if (id != null) scope.launch { vm.loadItemsForCategory(vm.currentCategory()) }
    }

    internal fun fetchArtworkFor(gameId: Long) {
        vm.closeContextMenu()
        if (uiState.value.artworkFetchTitle != null) return
        scope.launch {
            val before = vm.gameRepository.getById(gameId)
            uiState.update { it.copy(artworkFetchTitle = before?.displayTitle ?: "this game") }
            val result = artworkRepository.fetchArtworkForGame(gameId, before?.title.orEmpty())
            val updated = vm.gameRepository.getById(gameId)
            artworkRepository.evictFromImageCache((vm.artRefsOf(before) + vm.artRefsOf(updated)).toSet())
            uiState.update { it.copy(artworkFetchTitle = null) }
            SystemToasts.post(
                when {
                    result.success -> "Artwork updated"
                    result.skipped -> "Already has artwork"
                    else           -> result.errorMessage ?: "Artwork fetch failed"
                },
                null,
                if (result.success || result.skipped) ToastKind.SUCCESS else ToastKind.ERROR,
            )
            vm.loadItemsForCategory(vm.currentCategory())
        }
    }

    internal fun openMetadataPreviewFor(gameId: Long) {
        vm.closeContextMenu()
        if (uiState.value.metadataPreview != null) return
        val generation = ++metadataPreviewGeneration
        uiState.update { it.copy(metadataPreview = MetadataPreviewUi(), metadataPreviewGameId = gameId) }
        scope.launch {
            val outcome = runCatching { artworkRepository.fetchMetadataPreview(gameId) }
                .onFailure { Timber.w(it, "Metadata preview failed for game $gameId") }
            val preview = outcome.getOrNull()
            if (generation != metadataPreviewGeneration) return@launch
            uiState.update { s ->
                if (s.metadataPreview == null) return@update s
                if (preview == null || preview.presets.isEmpty()) {
                    return@update s.copy(
                        metadataPreview = MetadataPreviewUi(loading = false, failed = outcome.isFailure),
                    )
                }
                s.copy(metadataPreview = MetadataPreviewUi(
                    loading = false,
                    current = preview.current,
                    presets = preview.presets,
                    chosen  = MetadataApply.changedFields(preview.current, preview.presets.first()),
                ))
            }
        }
    }

    fun closeMetadataPreview() {
        metadataPreviewGeneration++
        uiState.update { it.copy(metadataPreview = null, metadataPreviewGameId = null) }
    }

    fun selectMetadataPolicy(policy: MetadataApplyPolicy) = updateMetadataPreview { it.copy(policy = policy) }

    private fun cycleMetadataPolicy(delta: Int) = updateMetadataPreview { p ->
        val all = MetadataApplyPolicy.entries
        p.copy(policy = all[(p.policy.ordinal + delta).mod(all.size)])
    }

    fun cycleMetadataSource(delta: Int) = updateMetadataPreview { p ->
        if (p.presets.size < 2) return@updateMetadataPreview p
        val index = (p.presetIndex + delta).mod(p.presets.size)
        val next = p.copy(presetIndex = index, chosen = MetadataApply.changedFields(p.current, p.presets[index]))
        next.copy(focus = next.focus.coerceIn(0, next.applyIndex))
    }

    fun toggleMetadataField(field: MetadataField) = updateMetadataPreview { p ->
        p.copy(
            policy = MetadataApplyPolicy.CHOOSE_FIELDS,
            chosen = if (field in p.chosen) p.chosen - field else p.chosen + field,
        )
    }

    private fun moveMetadataFocus(delta: Int) = updateMetadataPreview { p ->
        p.copy(focus = (p.focus + delta).coerceIn(0, p.applyIndex))
    }

    fun applyMetadataPreview() {
        val gameId = uiState.value.metadataPreviewGameId ?: return
        val p = uiState.value.metadataPreview ?: return
        if (p.loading || p.applying) return

        val preset = p.preset ?: return closeMetadataPreview()
        if (p.policy == MetadataApplyPolicy.KEEP_CURRENT) {
            closeMetadataPreview()
            SystemToasts.post("Kept current metadata", null, ToastKind.SUCCESS)
            return
        }
        uiState.update { it.copy(metadataPreview = p.copy(applying = true)) }
        scope.launch {
            val written = runCatching { artworkRepository.applyMetadata(gameId, preset, p.policy, p.chosen) }
                .onFailure { Timber.w(it, "Metadata apply failed for game $gameId") }
            metadataPreviewGeneration++
            uiState.update { it.copy(metadataPreview = null, metadataPreviewGameId = null) }
            SystemToasts.post(
                written.fold(
                    onSuccess = { fields ->
                        when (fields.size) {
                            0    -> "Nothing to change"
                            1    -> "Updated 1 field from ${preset.provider.label}"
                            else -> "Updated ${fields.size} fields from ${preset.provider.label}"
                        }
                    },
                    onFailure = { "Metadata update failed" },
                ),
                null,
                if (written.isSuccess) ToastKind.SUCCESS else ToastKind.ERROR,
            )
            vm.loadItemsForCategory(vm.currentCategory())
        }
    }

    private fun updateMetadataPreview(
        transform: (MetadataPreviewUi) -> MetadataPreviewUi,
    ) = uiState.update { s ->
        val p = s.metadataPreview ?: return@update s
        if (p.loading || p.applying) s else s.copy(metadataPreview = transform(p))
    }

    internal fun handleMetadataPreviewInput(action: GamepadAction) {
        val p = uiState.value.metadataPreview ?: return
        if (p.applying) return
        if (p.nothingFound) {
            if (action == GamepadAction.SELECT || action == GamepadAction.BACK) closeMetadataPreview()
            return
        }
        when (action) {
            GamepadAction.BACK           -> closeMetadataPreview()
            GamepadAction.NAVIGATE_LEFT  -> cycleMetadataPolicy(-1)
            GamepadAction.NAVIGATE_RIGHT -> cycleMetadataPolicy(+1)
            GamepadAction.PREV_CATEGORY  -> cycleMetadataSource(-1)
            GamepadAction.NEXT_CATEGORY  -> cycleMetadataSource(+1)
            GamepadAction.NAVIGATE_UP    -> moveMetadataFocus(-1)
            GamepadAction.NAVIGATE_DOWN  -> moveMetadataFocus(+1)
            GamepadAction.SELECT         ->
                if (p.focus >= p.applyIndex) applyMetadataPreview()
                else p.rows.getOrNull(p.focus)?.let { toggleMetadataField(it.field) }
            else -> Unit
        }
    }

    private var metadataPreviewGeneration = 0
}
