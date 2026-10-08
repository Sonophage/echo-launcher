package com.echo.feature.photos

import com.echo.core.ui.components.EchoTrio
import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.MediaDefaultAccent
import com.echo.core.ui.design.MediaDesignFrame
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.echo.core.common.format.formatByteSize
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.Photo
import com.echo.core.ui.detail.EchoConfirmOverlay
import com.echo.core.ui.components.ControllerHintStyle
import com.echo.core.ui.components.EchoControllerHints
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.core.ui.components.EchoContextMenuOverlay
import androidx.compose.runtime.ReadOnlyComposable
import com.echo.core.ui.theme.LocalEchoTextColors

private val ViewerBg = Color(0xFF000000)

private val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.primary

private val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.secondary
private val PanelBg = Color(0xF0101018)

@Composable
fun PhotoViewerScreen(
    photoId: String,
    libraryId: String?,
    onBack: () -> Unit,
    openWallpaperPreview: Boolean = false,
    favoritesOnly: Boolean = false,
    pendingGamepadAction: GamepadAction? = null,
    onGamepadActionConsumed: () -> Unit = {},

    onTouchInput: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PhotoViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(photoId, libraryId, openWallpaperPreview, favoritesOnly) {
        viewModel.load(photoId, libraryId, openWallpaperPreview, favoritesOnly)
    }

    LaunchedEffect(state.closed) { if (state.closed) { onBack(); viewModel.onClosedHandled() } }
    LaunchedEffect(pendingGamepadAction) {
        val action = pendingGamepadAction ?: return@LaunchedEffect
        viewModel.handleGamepadAction(action)
        onGamepadActionConsumed()
    }

    if (state.isLoading) {
        Box(modifier.fillMaxSize().background(ViewerBg)) {
            EchoTrio(Modifier.align(Alignment.Center), color = menuCursorEdge())
        }
        return
    }
    val photo = state.photo ?: run { onBack(); return }

    val transformState = rememberTransformableState { _, zoomChange, panChange, _ ->
        viewModel.onGesture(zoomChange, panChange.x, panChange.y)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViewerBg)
            .onSizeChanged { viewModel.onViewSize(it.width.toFloat(), it.height.toFloat()) }
            .transformable(transformState)

            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onTouchInput(); viewModel.toggleControls() },
            ),
    ) {
        val infoOpen = state.infoVisible
        MediaDesignFrame { u ->
            // framing a wallpaper: the photo fills the screen as the wallpaper will, scaled to the frame's zoom
            val frame = state.wallpaperFrame
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.displayName,
                contentScale = if (state.wallpaperPreviewVisible) ContentScale.Crop else ContentScale.Fit,
                modifier = (if (infoOpen) Modifier
                    .align(Alignment.TopStart)
                    .offset(u.dp(64), u.dp(90))
                    .size(u.dp(720), u.dp(540))
                else Modifier.fillMaxSize())
                    .graphicsLayer(
                        scaleX = frame?.layerScale(state.zoom) ?: state.zoom,
                        scaleY = frame?.layerScale(state.zoom) ?: state.zoom,
                        translationX = state.panX,
                        translationY = state.panY,
                        rotationZ = state.rotationDegrees.toFloat(),
                    ),
            )

            if (state.controlsVisible && !state.wallpaperPreviewVisible && !infoOpen) {
                ViewerChrome(state, photo, u, onTouchInput, viewModel)
            }

            if (infoOpen) {
                InfoPanel(state, photo, u, viewModel)
            }
        }

        if (state.showOptions) {
            EchoContextMenuOverlay(
                state = state.optionsMenu,
                onRowActivated = viewModel::onOptionRowActivated,
                onDismiss = viewModel::closeOptions,
            )
        }

        if (state.wallpaperPreviewVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Set as launcher wallpaper?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Move and zoom the photo: the screen shows what the wallpaper keeps.", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                EchoControllerHints(
                    items = listOf(
                        ControllerPromptItem(GamepadAction.SELECT, "Apply"),
                        ControllerPromptItem(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT), "Move"),
                        ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Zoom"),
                        ControllerPromptItem(GamepadAction.BACK, "Cancel"),
                    ),
                    style = ControllerHintStyle.OVERLAY,
                )
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = viewModel::confirmWallpaper, enabled = !state.applyingWallpaper) {
                        Text(if (state.applyingWallpaper) "Applying…" else "Apply", color = menuCursorEdge())
                    }
                    TextButton(onClick = viewModel::cancelWallpaperPreview, enabled = !state.applyingWallpaper) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        }

        if (state.confirmRemove) {
            EchoConfirmOverlay(
                title = "Remove from library?",
                message = "\"${photo.displayName}\" will be removed from this library. " +
                    "The photo on disk is not deleted.",
                confirmLabel = "Remove",
                cancelLabel = "Cancel",
                confirmFocused = false,
                cancelFocused = true,
                onConfirm = viewModel::confirmRemove,
                onCancel = viewModel::cancelRemove,
            )
        }

        state.actionMessage?.let { msg ->
            Text(
                msg,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .background(PanelBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); viewModel.dismissMessage() }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.ViewerChrome(
    state: PhotoViewerUiState,
    photo: Photo,
    u: DesignUnits,
    onTouchInput: () -> Unit,
    viewModel: PhotoViewerViewModel,
) {
    val accent = MediaDefaultAccent
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(u.dp(150))
        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent))))
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(u.dp(330))
        .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Black.copy(alpha = 0.92f))))

    Column(Modifier.align(Alignment.TopStart).padding(start = u.dp(64), top = u.dp(32)), verticalArrangement = Arrangement.spacedBy(u.dp(6))) {
        val eyebrow = listOfNotNull(state.albumName, "${state.index + 1} of ${state.photos.size}").joinToString("  ·  ")
        Text(eyebrow.uppercase(), style = u.eyebrow(Color.White.copy(alpha = 0.6f)), maxLines = 1)
        Text(photo.displayName, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(30), fontWeight = FontWeight.Medium, shadow = PhotoShadow), maxLines = 1)
        val facts = listOfNotNull(photo.resolutionLabel, photo.sizeBytes?.let { formatByteSize(it) }).joinToString("  ·  ")
        if (facts.isNotBlank()) Text(facts, color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(14))
    }

    SideArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous photo", u, Modifier.align(Alignment.CenterStart).padding(start = u.dp(24))) {
        onTouchInput(); viewModel.step(-1)
    }
    SideArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next photo", u, Modifier.align(Alignment.CenterEnd).padding(end = u.dp(24))) {
        onTouchInput(); viewModel.step(+1)
    }

    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = u.dp(156)),
        horizontalArrangement = Arrangement.spacedBy(u.dp(10)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filmstripWindow(state.index, state.photos.size).forEach { i ->
            val current = i == state.index
            val distance = kotlin.math.abs(i - state.index)
            AsyncImage(
                model = state.photos[i].thumbnailUri ?: state.photos[i].uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (current) u.dp(120) else u.dp(96), if (current) u.dp(80) else u.dp(64))
                    .then(if (current) Modifier.border(u.dp(2), accent, RoundedCornerShape(u.dp(8))).padding(u.dp(4)) else Modifier)
                    .clip(RoundedCornerShape(u.dp(7)))
                    .graphicsLayer(alpha = if (current) 1f else (1f - distance * 0.18f).coerceAtLeast(0.4f))
                    .clickable { onTouchInput(); viewModel.jumpTo(i) },
            )
        }
    }

    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = u.dp(64)),
        horizontalArrangement = Arrangement.spacedBy(u.dp(36)),
        verticalAlignment = Alignment.Top,
    ) {
        PhotoControl.entries.forEach { control ->
            val focused = control == state.barFocus
            val tint = if (focused) Color.White else Color.White.copy(alpha = 0.45f)
            Column(
                Modifier
                    .width(if (focused) u.dp(110) else u.dp(56))
                    .offset(y = if (focused) -u.dp(10) else 0.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onTouchInput(); viewModel.activateControl(control)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.dp(8)),
            ) {
                Icon(
                    photoControlIcon(control), control.label, tint = if (control == PhotoControl.REMOVE && focused) Color(0xFFF07C85) else tint,
                    modifier = Modifier
                        .size(if (focused) u.dp(44) else u.dp(30))
                        .then(if (focused) Modifier.drawBehind {
                            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f), Color.Transparent)), radius = size.maxDimension)
                        } else Modifier),
                )
                if (focused) Text(control.label, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(15), fontWeight = FontWeight.SemiBold, shadow = PhotoShadow), maxLines = 1)
            }
        }
    }

    EchoHintBar(
        items = listOf(
            ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Photo"),
            ControllerPromptItem(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT), "Action"),
            ControllerPromptItem(GamepadAction.SELECT, "Select"),
            ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
            ControllerPromptItem(GamepadAction.BACK, "Hide"),
        ),
        onAction = { action ->
            onTouchInput()
            when (action) {
                GamepadAction.OPEN_CONTEXT_MENU -> viewModel.openOptions()
                else -> viewModel.handleGamepadAction(action)
            }
        },
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.InfoPanel(
    state: PhotoViewerUiState,
    photo: Photo,
    u: DesignUnits,
    viewModel: PhotoViewerViewModel,
) {
    val accent = MediaDefaultAccent
    Text(
        listOfNotNull(state.albumName, "${state.index + 1} of ${state.photos.size}").joinToString("  ·  ").uppercase(),
        style = u.eyebrow(Color.White.copy(alpha = 0.6f)),
        modifier = Modifier.align(Alignment.TopStart).padding(start = u.dp(64), top = u.dp(36)),
    )
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .width(u.dp(420))
            .fillMaxHeight()
            .background(Color(0xF50E0A09))
            .padding(horizontal = u.dp(36), vertical = u.dp(40)),
        verticalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        Text(photo.displayName, color = Color.White, fontSize = u.sp(30), maxLines = 2)
        Text("DETAILS", style = u.eyebrow(Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(top = u.dp(6)))
        Column {
            val rows = listOfNotNull(
                listOfNotNull(photo.resolutionLabel, photo.sizeBytes?.let { formatByteSize(it) }).joinToString(" · ").takeIf { it.isNotBlank() }?.let { "Size" to it },
                state.albumName?.let { "Album" to it },
                photo.relativePath?.let { "Path" to it },
            )
            rows.forEachIndexed { i, (k, v) ->
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (i < rows.lastIndex) Modifier.drawBehind {
                            drawLine(Color.White.copy(alpha = 0.07f), androidx.compose.ui.geometry.Offset(0f, size.height),
                                androidx.compose.ui.geometry.Offset(size.width, size.height), 1f)
                        } else Modifier)
                        .padding(vertical = u.dp(9)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(k, color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(15))
                    Spacer(Modifier.width(u.dp(12)))
                    Text(v, color = Color.White, fontSize = u.sp(15), maxLines = 1, textAlign = TextAlign.End)
                }
            }
        }
        Text("USE THIS PHOTO", style = u.eyebrow(Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(top = u.dp(8)))
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(4))) {
            PhotoInfoAction.entries.forEach { action ->
                val focused = action == state.infoFocus
                val color = if (action == PhotoInfoAction.REMOVE) Color(0xFFF07C85) else Color.White
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(u.dp(12)))
                        .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.07f)).border(u.dp(2), accent, RoundedCornerShape(u.dp(12))) else Modifier)
                        .clickable { viewModel.activateInfo(action) }
                        .padding(horizontal = u.dp(16), vertical = u.dp(12)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
                ) {
                    Icon(photoInfoIcon(action), null, tint = color, modifier = Modifier.size(u.dp(20)))
                    Text(action.labelFor(photo), color = color, fontSize = u.sp(16), fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
    EchoHintBar(
        items = listOf(
            ControllerPromptItem(GamepadAction.SELECT, "Select"),
            ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Photo"),
            ControllerPromptItem(GamepadAction.BACK, "Back"),
        ),
        onAction = { viewModel.handleGamepadAction(it) },
        modifier = Modifier.align(Alignment.BottomStart).width(u.dp(860)),
    )
}

@Composable
private fun SideArrow(icon: ImageVector, label: String, u: DesignUnits, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(u.dp(48)).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(u.dp(26)))
    }
}

fun filmstripWindow(index: Int, count: Int, size: Int = FILMSTRIP_SIZE): List<Int> {
    if (count <= 0) return emptyList()
    val half = size / 2
    val start = (index - half).coerceIn(0, (count - size).coerceAtLeast(0))
    return (start until minOf(count, start + size)).toList()
}

private const val FILMSTRIP_SIZE = 7

private fun photoControlIcon(control: PhotoControl): ImageVector = when (control) {
    PhotoControl.ZOOM -> Icons.Filled.ZoomIn
    PhotoControl.ROTATE -> Icons.AutoMirrored.Filled.RotateRight
    PhotoControl.SLIDESHOW -> Icons.Filled.PlayArrow
    PhotoControl.INFO -> Icons.Outlined.Info
    PhotoControl.WALLPAPER -> Icons.Filled.Wallpaper
    PhotoControl.REMOVE -> Icons.Outlined.Delete
}

private fun photoInfoIcon(action: PhotoInfoAction): ImageVector = when (action) {
    PhotoInfoAction.WALLPAPER -> Icons.Filled.Wallpaper
    PhotoInfoAction.FAVORITE -> Icons.Outlined.StarBorder
    PhotoInfoAction.ROTATE -> Icons.AutoMirrored.Filled.RotateRight
    PhotoInfoAction.REMOVE -> Icons.Outlined.Delete
}

private val PhotoShadow = androidx.compose.ui.graphics.Shadow(Color(0xBF000000), androidx.compose.ui.geometry.Offset(0f, 2f), 4f)
