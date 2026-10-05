package com.echo.feature.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.echo.feature.settings.viewmodel.FolderAccessRow
import com.echo.feature.settings.viewmodel.StorageSlot
import com.echo.feature.settings.viewmodel.pickerStartUri

// the folders ECHO reads, each with whether Android still lets it open them; a lost one is granted again
@Composable
internal fun FolderAccessRows(rows: List<FolderAccessRow>, onGrant: (FolderAccessRow) -> Unit) {
    rows.forEach { row ->
        val kind = if (row.slot == StorageSlot.ARTWORK) "ECHO folder" else row.slot.label
        // the folder's own name in the label, so two Photos folders can be told apart without focus
        SettingsValueRow(
            label = "$kind · ${row.name.trimEnd('/').substringAfterLast('/')}",
            value = if (row.granted) "Granted" else "Grant…",
            sublabel = if (row.granted) row.name else "${row.name}: ECHO can no longer open it. Tap, then Use this folder and Allow",
            onClick = if (row.granted) null else ({ onGrant(row) }),
        )
    }
}

// opens the folder picker on the lost folder itself, so Use this folder grants that same one
@Composable
internal fun rememberFolderRegrant(onPicked: (FolderAccessRow, Uri) -> Unit): (FolderAccessRow) -> Unit {
    var pending by remember { mutableStateOf<FolderAccessRow?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val row = pending
        pending = null
        if (row != null && uri != null) onPicked(row, uri)
    }
    return { row ->
        pending = row
        picker.launch(pickerStartUri(row.treeUri))
    }
}
