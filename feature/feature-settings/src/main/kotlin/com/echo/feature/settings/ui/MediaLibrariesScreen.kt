package com.echo.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.echo.core.data.repository.MediaRootKind
import com.echo.feature.settings.viewmodel.MediaLibrariesViewModel
import com.echo.feature.settings.viewmodel.rootDisplayName
import com.echo.feature.settings.viewmodel.slot

@Composable
fun MediaLibrariesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaLibrariesViewModel = hiltViewModel(),
) {
    val libraries by viewModel.libraries.collectAsState()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    var adding by remember { mutableStateOf<MediaRootKind?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val kind = adding
        adding = null
        if (kind != null && uri != null) viewModel.add(kind, uri)
    }
    var removing by remember { mutableStateOf<Pair<MediaRootKind, String>?>(null) }

    SettingsPageScaffold(subtitle = "Media Libraries", onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            MediaRootKind.entries.forEach { kind ->
                val label = kind.slot.label
                val folders = libraries[kind].orEmpty()
                SettingsGroup(label)
                folders.forEach { folder ->
                    SettingsRow(
                        label = folder.name.trimEnd('/').substringAfterLast('/'),
                        sublabel = if (folder.linked) folder.name else "Access lost: grant it again in Permissions",
                        trailing = { Text("Remove", color = SettingsAccent) },
                        onClick = { removing = kind to folder.treeUri },
                    )
                }
                SettingsRow(
                    label = "Add a ${label.lowercase()} folder",
                    sublabel = if (folders.isEmpty()) "With no folder, the ${label} category shows only its apps" else null,
                    onClick = { adding = kind; picker.launch(null) },
                )
                if (folders.isNotEmpty()) {
                    SettingsRow(label = "Rescan ${label.lowercase()}", sublabel = "Look for new and removed files", onClick = { viewModel.rescan(kind) })
                }
            }
        }
    }

    removing?.let { (kind, treeUri) ->
        SettingsConfirmOverlay(
            title = "Remove ${rootDisplayName(treeUri).trimEnd('/').substringAfterLast('/')}?",
            message = "ECHO stops reading this folder and forgets what it found there. The files are not deleted.",
            confirmLabel = "Remove",
            onConfirm = { removing = null; viewModel.remove(kind, treeUri) },
            onCancel = { removing = null },
        )
    }
}
