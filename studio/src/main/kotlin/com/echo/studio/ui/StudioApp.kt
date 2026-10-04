package com.echo.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.studio.StudioDialog
import com.echo.studio.StudioViewModel
import com.echo.studio.io.FileDialogs
import com.echo.studio.preview.PreviewRenderer
import com.echo.studio.preview.CrossbarPreviewCanvas
import com.echo.studio.preview.toPreviewModel
import com.echo.themekit.EchoThemeCodec
import java.awt.Frame

@Composable
fun StudioApp(viewModel: StudioViewModel, window: Frame) {
    val state by viewModel.state.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    OutlinedButton(onClick = viewModel::newTheme) { Text("New") }
                    OutlinedButton(onClick = {
                        FileDialogs.openFile(window, "Open theme", setOf(EchoThemeCodec.FILE_EXTENSION))
                            ?.let(viewModel::openFile)
                    }) { Text("Open…") }
                    OutlinedButton(onClick = {
                        FileDialogs.openFile(window, "Import wallpaper or video", setOf("png", "jpg", "jpeg", "bmp", "webp", "mp4", "m4v", "webm", "gif"))
                            ?.let(viewModel::onWallpaperPicked)
                    }) { Text("Wallpaper…") }
                    OutlinedButton(onClick = {
                        FileDialogs.saveFile(
                            window, "Export theme",
                            suggestedName = "${state.name.ifBlank { "theme" }}.${EchoThemeCodec.FILE_EXTENSION}",
                            extension = EchoThemeCodec.FILE_EXTENSION,
                        )?.let { file -> viewModel.exportTo(file) { s -> PreviewRenderer.renderPreviewPng(s) } }
                    }) { Text("Export…") }

                    Box(Modifier.weight(1f))
                    if (state.busy) {
                        Text("Working…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()

                Row(Modifier.weight(1f)) {
                    Column(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF141414))) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                        ) {
                            com.echo.studio.PreviewMode.entries.forEach { mode ->
                                val selected = state.previewMode == mode
                                if (selected) {
                                    androidx.compose.material3.Button(onClick = {}) { Text(mode.label, fontSize = 12.sp) }
                                } else {
                                    OutlinedButton(onClick = { viewModel.setPreviewMode(mode) }) { Text(mode.label, fontSize = 12.sp) }
                                }
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        ) {
                            CrossbarPreviewCanvas(state.toPreviewModel(), Modifier.fillMaxSize())
                        }
                    }
                    VerticalDivider()
                    Column(Modifier.width(360.dp).fillMaxHeight()) {
                        var tab by remember { mutableStateOf(0) }

                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                            TabButton("Theme", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                            TabButton("Icons", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                        }
                        HorizontalDivider()
                        when (tab) {
                            0 -> InspectorPanel(
                                state = state,
                                viewModel = viewModel,
                                onChooseWallpaper = {
                                    FileDialogs.openFile(window, "Import wallpaper or video", setOf("png", "jpg", "jpeg", "bmp", "webp", "mp4", "m4v", "webm", "gif"))
                                        ?.let(viewModel::onWallpaperPicked)
                                },
                                onChooseVideo = {
                                    FileDialogs.openFile(window, "Import video", setOf("mp4", "m4v"))
                                        ?.let(viewModel::importVideo)
                                },
                            )
                            1 -> IconEditorPanel(
                                state = state,
                                viewModel = viewModel,
                                onImportInto = { key ->
                                    FileDialogs.openFile(window, "Icon image", setOf("png", "jpg", "jpeg", "bmp", "webp"))
                                        ?.let { viewModel.setIconOverride(key, it) }
                                },
                                onExportTemplates = {
                                    FileDialogs.pickDirectory("Folder for icon templates")?.let { dir ->
                                        viewModel.exportIconTemplates(dir, PreviewRenderer::rasterizeDefaultIcon)
                                    }
                                },
                            )
                        }
                    }
                }
                HorizontalDivider()

                Text(
                    text = state.statusMessage
                        ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            StudioDialogs(viewModel)
            state.pendingWallpaper?.let { pending ->
                WallpaperImportDialog(
                    pending = pending,
                    onConfirm = viewModel::confirmWallpaper,
                    onCancel = viewModel::cancelWallpaperImport,
                )
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun StudioDialogs(viewModel: StudioViewModel) {
    val state by viewModel.state.collectAsState()
    when (val dialog = state.dialog) {
        null -> Unit
        is StudioDialog.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            confirmButton = { Button(onClick = viewModel::dismissDialog) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(dialog.message) },
        )
        is StudioDialog.Notice -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            confirmButton = { Button(onClick = viewModel::dismissDialog) { Text("OK") } },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
        )
    }
}
