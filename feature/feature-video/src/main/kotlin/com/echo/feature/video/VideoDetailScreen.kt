package com.echo.feature.video

import com.echo.core.ui.components.EchoTrio
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.echo.core.common.format.formatByteSize
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.Video
import com.echo.core.domain.model.ControllerIcon
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.detail.EchoConfirmOverlay
import com.echo.core.ui.detail.EchoDetailLaunchButton
import com.echo.core.ui.detail.EchoMessageOverlay
import com.echo.core.ui.detail.EchoOverlayCard
import com.echo.core.ui.detail.EchoOverlayTitle
import com.echo.core.ui.detail.EchoTextPromptOverlay
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.ui.theme.menuCursor
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.design.menuBackdrop
import com.echo.feature.video.VideoPlayerScreen
import androidx.compose.runtime.ReadOnlyComposable
import com.echo.core.ui.theme.LocalEchoTextColors

private val PageBg = Color(0xFF06060C)

private val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.primary

private val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.secondary

@UnstableApi
@Composable
fun VideoDetailScreen(
    videoId: String,
    onBack: () -> Unit,
    pendingGamepadAction: GamepadAction? = null,
    onGamepadActionConsumed: () -> Unit = {},
    onTouchInput: () -> Unit = {},

    autoPlay: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: VideoDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val thumbnailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onThumbnailPicked(uri) }

    LaunchedEffect(videoId) { viewModel.loadVideo(videoId) }

    if (autoPlay) {
        val loadedVideoId = state.video?.id
        LaunchedEffect(loadedVideoId) {
            if (loadedVideoId == videoId) state.primaryActions.firstOrNull()?.let(viewModel::activate)
        }
        // owner, 2026-10-08: playing a video shows no details screen on the way in or out; when playback ends
        // the screen closes with it
        var started by remember(videoId) { mutableStateOf(false) }
        LaunchedEffect(state.playing, state.handedOffToPlayer) {
            if (state.playing || state.handedOffToPlayer) started = true
            else if (autoPlayEnded(started, state.playing, state.handedOffToPlayer)) onBack()
        }
    }

    LaunchedEffect(state.closed) { if (state.closed) { onBack(); viewModel.onClosedHandled() } }
    LaunchedEffect(state.pickThumbnail) {
        if (state.pickThumbnail) {
            thumbnailPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
            viewModel.consumeThumbnailPick()
        }
    }

    LaunchedEffect(pendingGamepadAction, state.playing) {
        val action = pendingGamepadAction ?: return@LaunchedEffect
        if (!state.playing) {
            viewModel.handleGamepadAction(action)
            onGamepadActionConsumed()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onReturnedFromExternal()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.handedOffToPlayer) {
        if (state.handedOffToPlayer) {
            kotlinx.coroutines.delay(8000)
            viewModel.clearExternalOverlay()
        }
    }

    if (state.isLoading) {
        Box(modifier.fillMaxSize().background(PageBg)) {
            EchoTrio(Modifier.align(Alignment.Center), color = menuCursorEdge())
        }
        return
    }
    val video = state.video ?: run { onBack(); return }
    // straight to the player: nothing but the page until it starts, unless it could not play
    if (autoPlay && !state.playing && state.launchError == null) {
        Box(modifier.fillMaxSize().background(PageBg))
        return
    }
    val echoColors = LocalEchoColors.current

    Box(
        modifier = modifier
            .fillMaxSize()

            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); onTouchInput() } }
            .background(
                Brush.verticalGradient(
                    0f to echoColors.backgroundTop.copy(alpha = 0.72f),
                    1f to echoColors.backgroundBottom.copy(alpha = 0.90f),
                )
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 920.dp).align(Alignment.Center)

                .padding(bottom = PromptRowClearance)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            video.effectiveThumbnailUri?.let { thumb ->
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                )
            }

            Text(video.displayTitle, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            // owner, 2026-10-08: the line was lost on a light poster; it is lit and shadowed like the title
            Text(metadataLine(video), color = TextPrimary.copy(alpha = 0.85f), fontSize = 13.sp,
                style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = 8f)))
            if (video.resumePositionMs > 0) {
                Text("Resume at ${fmtTime(video.resumePositionMs)}", color = menuCursorEdge(), fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            val primaries = state.primaryActions
            primaries.forEachIndexed { i, action ->

                DetailButton(
                    label = action.label,
                    icon = when (action) {
                        VideoDetailAction.RESUME  -> Icons.Filled.Replay
                        VideoDetailAction.RESTART -> Icons.Filled.SkipPrevious
                        else                      -> Icons.Filled.PlayArrow
                    },
                    focused = state.mainFocus == i,
                    onClick = { viewModel.activate(action) },
                )
            }
            state.actionMessage?.let {
                Text(it, color = menuCursorEdge(), fontSize = 12.sp)
                LaunchedEffect(it) { kotlinx.coroutines.delay(2500); viewModel.dismissMessage() }
            }
        }

        if (!state.playing) {
            EchoHintBar(
                items = videoDetailHelperItems(state),
                onAction = viewModel::handleGamepadAction,

                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (state.showOptions) {
            EchoContextMenuOverlay(
                state = state.optionsMenu,
                onRowActivated = viewModel::onOptionRowActivated,
                onDismiss = viewModel::closeOptions,
            )
        }

        if (state.showPlaylistPicker) {
            PlaylistPicker(
                options = state.playlistOptions,
                selectedIndex = state.playlistPickerIndex,
                onRowClick = viewModel::onPlaylistRowClick,
            )
        }

        if (state.creatingPlaylist) {
            RenameDialog(
                title = "New Playlist",
                confirmLabel = "Create",
                text = state.newPlaylistName,
                onTextChange = viewModel::onNewPlaylistNameChange,
                onConfirm = viewModel::confirmCreatePlaylist,
                onCancel = viewModel::cancelCreatePlaylist,
            )
        }

        if (state.infoVisible) {
            InfoDialog(video = video, onDismiss = { viewModel.handleGamepadAction(GamepadAction.BACK) })
        }

        if (state.isEditingTitle) {
            RenameDialog(
                text = state.titleText,
                onTextChange = viewModel::onTitleChanged,
                onConfirm = viewModel::saveTitle,
                onCancel = viewModel::cancelTitleEdit,
            )
        }

        if (state.confirmRemove) {
            EchoConfirmOverlay(
                title = "Remove from library?",
                message = "\"${video.displayTitle}\" will be removed from this library. " +
                    "The file on disk is not deleted.",
                confirmLabel = "Remove",
                cancelLabel = "Cancel",
                confirmFocused = false,
                cancelFocused = true,
                onConfirm = viewModel::confirmRemove,
                onCancel = { viewModel.handleGamepadAction(GamepadAction.BACK) },
            )
        }

        state.launchError?.let { err ->
            EchoMessageOverlay(
                title = "Can't play video",
                message = err,
                onDismiss = viewModel::dismissLaunchError,
                dismissLabel = "OK",
            )
        }

        if (state.playing) {
            VideoPlayerScreen(
                videos = state.siblings.ifEmpty { listOf(video) },
                startIndex = state.siblings.indexOfFirst { it.id == video.id }.coerceAtLeast(0),
                startPositionMs = state.playStartPositionMs,
                onSaveResume = viewModel::saveResume,
                onExit = viewModel::onPlaybackExit,
                libraryName = state.libraryName,
                accentOf = viewModel::accentOf,
                pendingGamepadAction = pendingGamepadAction,
                onGamepadActionConsumed = onGamepadActionConsumed,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailButton(
    label: String,
    icon: ImageVector,
    focused: Boolean,
    onClick: () -> Unit,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) { if (focused) bringIntoView.bringIntoView() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)

            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (focused) com.echo.core.ui.detail.DetailButtonFocusFill
                else com.echo.core.ui.detail.DetailButtonRest
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val content = if (focused) com.echo.core.ui.detail.DetailButtonFocusText else TextPrimary
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = content, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlaylistPicker(
    options: List<VideoPlaylistOption>,
    selectedIndex: Int,
    onRowClick: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize().menuBackdrop(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier.padding(36.dp).width(320.dp)
                .background(Color(0xF0101018), RoundedCornerShape(14.dp)).padding(vertical = 12.dp),
        ) {
            Text("Add to Playlist", color = menuCursorEdge(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            options.forEachIndexed { i, opt ->
                Text(
                    (if (opt.checked) "● " else "○ ") + opt.name,
                    color = if (i == selectedIndex) Color.White else TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onRowClick(i) }
                        .menuCursor(i == selectedIndex)
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                )
            }
            val createIndex = options.size
            Text(
                "+ Create New Playlist",
                color = if (createIndex == selectedIndex) Color.White else menuCursorEdge(),
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
                    .clickable { onRowClick(createIndex) }
                    .menuCursor(createIndex == selectedIndex)
                    .padding(horizontal = 20.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun InfoDialog(video: Video, onDismiss: () -> Unit) {
    EchoOverlayCard(onScrimTap = onDismiss) {
        EchoOverlayTitle(video.displayTitle)
        Spacer(Modifier.height(10.dp))
        InfoRow("Duration", fmtTime(video.durationMs ?: 0))
        video.resolutionLabel?.let { InfoRow("Resolution", it) }
        video.codec?.let { InfoRow("Format", it) }
        video.mimeType?.let { InfoRow("Type", it) }
        video.sizeBytes?.let { InfoRow("Size", formatByteSize(it)) }
        video.relativePath?.let { InfoRow("Location", it) }
        InfoRow("File", video.displayName)
        Spacer(Modifier.height(18.dp))
        EchoDetailLaunchButton(
            label = "OK",
            icon = null,
            focused = true,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text(value, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun RenameDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String = "Rename Title",
    confirmLabel: String = "Save",
) {
    EchoTextPromptOverlay(
        title = title,
        value = text,
        placeholder = "",
        onValueChange = onTextChange,
        onConfirm = onConfirm,
        onCancel = onCancel,
        confirmLabel = confirmLabel,
    )
}

private fun metadataLine(video: Video): String = buildList {
    video.durationMs?.let { add(fmtTime(it)) }
    video.resolutionLabel?.let { add(it) }
    video.codec?.let(::containerLabel)?.let { add(it) }
}.joinToString("  ·  ").ifEmpty { video.displayName }

// the scanner keeps the MIME subtype ("X-MATROSKA"); people know the file kind ("MKV")
internal fun containerLabel(subtype: String): String = when (subtype.lowercase()) {
    "x-matroska", "matroska" -> "MKV"
    "quicktime" -> "MOV"
    "x-msvideo", "avi" -> "AVI"
    "x-flv" -> "FLV"
    "3gpp" -> "3GP"
    "mp2t" -> "TS"
    "x-ms-wmv" -> "WMV"
    else -> subtype.uppercase().removePrefix("X-")
}

private fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private val PromptRowClearance = 44.dp

internal fun videoDetailHelperItems(state: VideoDetailUiState): List<ControllerPromptItem> = when {
    state.launchError != null -> listOf(ControllerPromptItem(GamepadAction.SELECT, "Dismiss"))
    state.confirmRemove -> listOf(
        ControllerPromptItem(GamepadAction.SELECT, "Remove"),
        ControllerPromptItem(GamepadAction.BACK, "Cancel"),
    )
    state.creatingPlaylist || state.isEditingTitle ->
        listOf(ControllerPromptItem(GamepadAction.BACK, "Cancel"))
    state.showPlaylistPicker || state.showOptions -> listOf(
        ControllerPromptItem.fixed(ControllerIcon.DPAD_ALL, "Navigate"),
        ControllerPromptItem(GamepadAction.SELECT, "Select"),
        ControllerPromptItem(GamepadAction.BACK, "Close"),
    )
    state.infoVisible -> listOf(ControllerPromptItem(GamepadAction.BACK, "Close"))
    else -> listOfNotNull(

        state.primaryActions.firstOrNull()?.let { ControllerPromptItem(GamepadAction.SELECT, it.label) },
        ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
        ControllerPromptItem(GamepadAction.BACK, "Back"),
    )
}

// a video opened to play closes once its playback has started and stopped
internal fun autoPlayEnded(started: Boolean, playing: Boolean, handedOff: Boolean): Boolean = started && !playing && !handedOff
