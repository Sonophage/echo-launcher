package com.echo.feature.settings.ui

import androidx.compose.runtime.mutableIntStateOf
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.EchoContextMenuOverlay
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.detail.EchoOverlayCard
import com.echo.core.ui.detail.EchoOverlayTitle
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.preview.CombinedPreviews
import com.echo.core.ui.preview.EchoPreview
import com.echo.feature.launcher.PcLauncherAdapters
import com.echo.feature.settings.viewmodel.ADD_CONSOLE_FOCUS_KEY
import com.echo.feature.settings.viewmodel.EmulatorOption
import com.echo.feature.settings.viewmodel.IMPORT_PC_FOCUS_KEY
import com.echo.feature.settings.viewmodel.LibraryCardRow
import com.echo.feature.settings.viewmodel.LibraryManagerUiState
import com.echo.feature.settings.viewmodel.LibraryManagerViewModel
import com.echo.feature.settings.viewmodel.LibraryStep
import com.echo.feature.settings.viewmodel.PcGameRow
import com.echo.feature.settings.viewmodel.PcLauncherRow
import com.echo.feature.settings.viewmodel.PlatformOption
import com.echo.feature.settings.viewmodel.RootFolderRow

@Composable
fun LibraryManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAddAndroidApps: () -> Unit = {},

    startInImportPc: Boolean = false,
    viewModel: LibraryManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(startInImportPc) {
        if (startInImportPc) viewModel.openImportPcGames()
    }

    val setupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> viewModel.onRomFolderSetupPicked(uri) }

    LaunchedEffect(state.awaitingRomRootSetup) {
        if (state.awaitingRomRootSetup) setupPicker.launch(null)
    }

    LibraryManagerContent(
        state = state,
        onBack = { if (!viewModel.onBack()) onBack() },
        onAddAndroidApps = onAddAndroidApps,
        onOpenCardDetail = { viewModel.openCardDetail(it) },
        onStartAddConsole = { viewModel.startAddConsole() },
        onRequestRomFolderSetup = { viewModel.requestRomFolderSetup() },
        onScanAllConsoles = { viewModel.scanAllConsoles(it) },
        rescanOnReturn = viewModel.rescanOnReturn.collectAsState().value,
        onRescanOnReturn = { viewModel.setRescanOnReturn(it) },
        onDismissMessage = { viewModel.dismissMessage() },
        onPlatformChosen = { viewModel.onPlatformChosen(it) },
        onEmulatorChosen = { viewModel.onEmulatorChosen(it) },
        onConfirmAddConsole = { viewModel.confirmAddConsole(it) },
        onLoadEmulatorOptions = { viewModel.loadEmulatorOptionsForDetail() },
        onRemoveExtension = { p, e -> viewModel.removeExtension(p, e) },
        onAddExtension = { p, e -> viewModel.addExtension(p, e) },
        onScanConsole = { viewModel.scanConsole(it) },
        onScrapeArtwork = { viewModel.scrapeArtwork(it) },
        onBeginRename = { viewModel.beginRename(it) },
        onCancelRename = { viewModel.cancelRename() },
        onConfirmRename = { viewModel.confirmRename(it) },
        onToggleEnabled = { p, e -> viewModel.toggleEnabled(p, e) },
        onTogglePinned = { p, pin -> viewModel.togglePinned(p, pin) },
        onRemoveCard = { viewModel.removeCard(it) },
        onSetEmulatorForDetail = { viewModel.setEmulatorForDetail(it) },
        onOpenImportPcGames = { viewModel.openImportPcGames() },
        onSetVita3KFolder = { viewModel.setVita3KFolder(it) },
        onScanVitaGames = { viewModel.scanVitaGames() },
        onReleaseVita3KFolder = { viewModel.releaseVita3KFolder() },
        onRemoveApp = { viewModel.removeApp(it) },
        onRefreshHomeStatus = { viewModel.refreshHomeStatus() },
        onScanPcGamesFolder = { viewModel.scanPcGamesFolder(it) },
        onExportManualPcGames = { viewModel.exportManualPcGames() },
        onImportPcGame = { viewModel.importPcGame(it) },
        onImportAllPcGames = { viewModel.importAllPcGames() },
        onTestLaunchPcGame = { l, id, s -> viewModel.testLaunchPcGame(l, id, s) },
        onAddPcGameById = { l, id, t, s -> viewModel.addPcGameById(l, id, t, s) },
        homeRoleIntentProvider = { viewModel.homeRoleIntent() },
        modifier = modifier
    )
}

@Composable
private fun LibraryManagerContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onAddAndroidApps: () -> Unit,
    onOpenCardDetail: (String) -> Unit,
    onStartAddConsole: () -> Unit,
    onRequestRomFolderSetup: () -> Unit,
    onScanAllConsoles: (removeMissing: Boolean) -> Unit,
    onDismissMessage: () -> Unit,
    onPlatformChosen: (PlatformOption) -> Unit,
    onEmulatorChosen: (EmulatorOption) -> Unit,
    onConfirmAddConsole: (scanNow: Boolean) -> Unit,
    onLoadEmulatorOptions: () -> Unit,
    onRemoveExtension: (platformId: String, ext: String) -> Unit,
    onAddExtension: (platformId: String, ext: String) -> Unit,
    onScanConsole: (platformId: String) -> Unit,
    onScrapeArtwork: (platformId: String) -> Unit,
    onBeginRename: (platformId: String) -> Unit,
    onCancelRename: () -> Unit,
    onConfirmRename: (String) -> Unit,
    onToggleEnabled: (platformId: String, enabled: Boolean) -> Unit,
    onTogglePinned: (platformId: String, pinned: Boolean) -> Unit,
    onRemoveCard: (platformId: String) -> Unit,
    onSetEmulatorForDetail: (EmulatorOption) -> Unit,
    onOpenImportPcGames: () -> Unit,
    onSetVita3KFolder: (Uri) -> Unit,
    onScanVitaGames: () -> Unit,
    onReleaseVita3KFolder: () -> Unit,
    onRemoveApp: (Long) -> Unit,
    onRefreshHomeStatus: () -> Unit,
    onScanPcGamesFolder: (Uri) -> Unit,
    onExportManualPcGames: () -> Unit,
    onImportPcGame: (PcGameRow) -> Unit,
    onImportAllPcGames: () -> Unit,
    onTestLaunchPcGame: (PcLauncherRow, String, String?) -> Unit,
    onAddPcGameById: (PcLauncherRow, String, String, String?) -> Unit,
    homeRoleIntentProvider: () -> android.content.Intent?,
    modifier: Modifier = Modifier,
    rescanOnReturn: Boolean = true,
    onRescanOnReturn: (Boolean) -> Unit = {},
) {
    val handleBack: () -> Unit = onBack

    when (state.step) {
        LibraryStep.LIST          -> LibraryListContent(state, handleBack, onOpenCardDetail, onStartAddConsole, onRequestRomFolderSetup, onScanAllConsoles, onDismissMessage, modifier, rescanOnReturn, onRescanOnReturn)
        LibraryStep.PICK_PLATFORM -> PickPlatformContent(state, onBack = handleBack, onPlatformChosen = onPlatformChosen, modifier = modifier)
        LibraryStep.PICK_EMULATOR -> PickEmulatorContent(state, onBack = handleBack, onEmulatorChosen = onEmulatorChosen, modifier = modifier)
        LibraryStep.SCAN_PROMPT   -> ScanPromptContent(state, onBack = handleBack, onConfirmAddConsole = onConfirmAddConsole, modifier = modifier)
        LibraryStep.CARD_DETAIL   -> CardDetailContent(state, onBack = handleBack, onAddAndroidApps = onAddAndroidApps, onLoadEmulatorOptions = onLoadEmulatorOptions, onRemoveExtension = onRemoveExtension, onAddExtension = onAddExtension, onScanConsole = onScanConsole, onScrapeArtwork = onScrapeArtwork, onBeginRename = onBeginRename, onToggleEnabled = onToggleEnabled, onTogglePinned = onTogglePinned, onRemoveCard = onRemoveCard, onSetEmulatorForDetail = onSetEmulatorForDetail, onOpenImportPcGames = onOpenImportPcGames, onSetVita3KFolder = onSetVita3KFolder, onScanVitaGames = onScanVitaGames, onReleaseVita3KFolder = onReleaseVita3KFolder, onRemoveApp = onRemoveApp, modifier = modifier)
        LibraryStep.IMPORT_PC     -> ImportPcGamesContent(state, onBack = handleBack, onRefreshHomeStatus = onRefreshHomeStatus, onScanPcGamesFolder = onScanPcGamesFolder, onExportManualPcGames = onExportManualPcGames, onImportPcGame = onImportPcGame, onImportAllPcGames = onImportAllPcGames, onTestLaunchPcGame = onTestLaunchPcGame, onAddPcGameById = onAddPcGameById, onDismissMessage = onDismissMessage, homeRoleIntentProvider = homeRoleIntentProvider, modifier = modifier)
    }

    state.renameTargetPlatformId?.let { targetId ->
        val current = state.cards.firstOrNull { it.platformId == targetId }?.displayName ?: ""
        var text by remember(targetId) { mutableStateOf(current) }
        SettingsTextPromptOverlay(
            title = "Rename System",
            value = text,
            onValueChange = { text = it },
            onConfirm = { onConfirmRename(text) },
            onCancel = onCancelRename,
        )
    }
}

@Composable
private fun LibraryListContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onOpenCardDetail: (String) -> Unit,
    onStartAddConsole: () -> Unit,
    onRequestRomFolderSetup: () -> Unit,
    onScanAllConsoles: (removeMissing: Boolean) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier,
    rescanOnReturn: Boolean,
    onRescanOnReturn: (Boolean) -> Unit,
) {
    var confirmRescanAll by remember { mutableStateOf(false) }
    SettingsPageScaffold(
        subtitle = "Library Manager",
        onBack = onBack,
        modifier = modifier,
        restoreFocusKey = state.returnFocusKey,
    ) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            SettingsGroup("Consoles")

            val consoleCards = state.cards
            if (consoleCards.isEmpty()) {
                Hint("No systems yet. Add one and it appears inside Games.")
            } else {
                consoleCards.forEach { card ->
                    SettingsRow(
                        label    = card.displayName + if (!card.enabled) "  (Hidden)" else "",
                        sublabel = cardSublabel(card),
                        focusKey = card.platformId,
                        trailing = { if (card.pinned) Text("PINNED", color = SettingsAccent) },
                        onClick  = { onOpenCardDetail(card.platformId) },
                    )
                }
            }

            SettingsGroup("Manage")

            SettingsRow(
                label    = "Add Console",
                sublabel = "Pick a platform — its games live in the matching folder under your ROM Root",
                focusKey = ADD_CONSOLE_FOCUS_KEY,
                onClick  = { onStartAddConsole() },
            )
            SettingsRow(
                label    = "Set Up ROM Folders (ES-DE)",
                sublabel = "Pick an empty folder — ECHO creates the standard ES-DE system folders " +
                    "(gba, snes, psx…) for you to copy games into. No guessing folder names",
                onClick  = { onRequestRomFolderSetup() },
            )
            val anyScannable = state.cards.any { it.enabled && (it.treeUri != null || it.romDirectory != null) }
            SettingsRow(
                label    = "Scan All Consoles",
                sublabel = when {
                    state.scanningPlatformIds.isNotEmpty() -> "Scanning ${state.scanningPlatformIds.size}…"
                    state.cards.none { it.treeUri != null || it.romDirectory != null } -> "Configure a ROM folder first"
                    else -> "Scan every enabled console's folder"
                },
                onClick  = if (anyScannable) ({ onScanAllConsoles(false) }) else null,
            )
            SettingsRow(
                label    = "Re-Scan All (Remove Missing)",
                sublabel = "Also removes games whose ROM file no longer exists",
                onClick  = if (anyScannable) ({ confirmRescanAll = true }) else null,
            )

            SettingsToggleRow(
                label    = "Rescan On Return",
                sublabel = "Look for new and missing games when you come back to the launcher, at most every five minutes. " +
                    "Inserting a card still rescans either way",
                checked  = rescanOnReturn,
                onToggle = onRescanOnReturn,
            )

            state.message?.let { MessageRow(it) { onDismissMessage() } }
        }
    }
    // owner, 2026-10-07: the kit's confirm, not a Confirm row and a Cancel row in the list
    if (confirmRescanAll) {
        SettingsConfirmOverlay(
            title = "Re-Scan and Remove Missing Games?",
            message = "Removes library entries whose ROM file is gone. This can take a while with a large library.",
            confirmLabel = "Re-Scan",
            onConfirm = { confirmRescanAll = false; onScanAllConsoles(true) },
            onCancel = { confirmRescanAll = false },
        )
    }
}

@Composable
private fun PickPlatformContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onPlatformChosen: (PlatformOption) -> Unit,
    modifier: Modifier,
) {
    SettingsPageScaffold(heading = "Add Console", subtitle = "Choose Platform", onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            SettingsGroup("Supported Platforms")
            state.platformOptions.forEach { option ->
                SettingsRow(
                    label    = option.name,
                    sublabel = option.shortName,
                    onClick  = { onPlatformChosen(option) },
                )
            }
        }
    }
}

@Composable
private fun PickEmulatorContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onEmulatorChosen: (EmulatorOption) -> Unit,
    modifier: Modifier,
) {
    SettingsPageScaffold(heading = "Add Console", subtitle = "Assign Emulator", onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            SettingsGroup(state.pendingPlatformName ?: "Emulator")
            if (state.emulatorOptions.all { it.id == null }) {
                Hint("No installed emulators detected for this platform. You can assign one later from the console's detail screen.")
            }
            state.emulatorOptions.forEach { option ->
                SettingsRow(label = option.name, onClick = { onEmulatorChosen(option) })
            }
        }
    }
}

@Composable
private fun ScanPromptContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onConfirmAddConsole: (Boolean) -> Unit,
    modifier: Modifier,
) {
    SettingsPageScaffold(heading = "Add Console", subtitle = "Scan Now?", onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            SettingsGroup(state.pendingPlatformName ?: "New Console")
            SettingsValueRow(
                label = "ROM Directory",
                value = state.pendingDirectory?.substringAfterLast('/') ?: "Not set",
                sublabel = state.pendingDirectory,
            )
            SettingsRow(
                label    = "Scan Now",
                sublabel = "Create the system and scan its folder now",
                onClick  = { onConfirmAddConsole(true) },
            )
            SettingsRow(
                label    = "Add Without Scanning",
                sublabel = "Create the system now, scan later",
                onClick  = { onConfirmAddConsole(false) },
            )
        }
    }
}

@Composable
private fun CardDetailContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onAddAndroidApps: () -> Unit,
    onLoadEmulatorOptions: () -> Unit,
    onRemoveExtension: (String, String) -> Unit,
    onAddExtension: (String, String) -> Unit,
    onScanConsole: (String) -> Unit,
    onScrapeArtwork: (String) -> Unit,
    onBeginRename: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onRemoveCard: (String) -> Unit,
    onSetEmulatorForDetail: (EmulatorOption) -> Unit,
    onOpenImportPcGames: () -> Unit,
    onSetVita3KFolder: (Uri) -> Unit,
    onScanVitaGames: () -> Unit,
    onReleaseVita3KFolder: () -> Unit,
    onRemoveApp: (Long) -> Unit,
    modifier: Modifier,
) {
    val card = state.detailCard
    if (card == null) {
        SettingsPageScaffold(
            subtitle = "Not in your library",
            onBack = onBack,
            modifier = modifier,
        ) {
            val scrollState = rememberScrollState()
            LocalSettingsScrollStateRegistrar.current(scrollState)
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                SettingsGroup("Nothing here yet")
                SettingsValueRow(label = "No console to show", value = "")
                SettingsValueRow(
                    label = "PC appears once a PC game has been imported.",
                    value = "",
                )
                SettingsValueRow(
                    label = "Other consoles appear once you add them in Library Manager.",
                    value = "",
                )
            }
        }
        return
    }

    var showEmulatorDialog by remember { mutableStateOf(false) }
    var showRemoveConfirm  by remember { mutableStateOf(false) }
    var removeAppConfirm   by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var removeExtConfirm   by remember { mutableStateOf<String?>(null) }
    var newExt             by remember(card.platformId) { mutableStateOf("") }
    val isScanning = card.platformId in state.scanningPlatformIds
    val isAndroid = card.platformId == "android"
    val isWindows = card.platformId == "windows"
    val isVita    = card.platformId == "psvita"
    val vitaFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { onSetVita3KFolder(it) } }

    SettingsPageScaffold(subtitle = card.displayName, onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            if (isWindows) {
                SettingsGroup("Library")
                SettingsValueRow(
                    label    = "Games Directory",
                    value    = card.romDirectory?.substringAfterLast('/') ?: "Not set",
                    sublabel = card.romDirectory
                        ?: "Add a ROM Root — ECHO creates and uses <root>/windows automatically",
                )
                SettingsValueRow(label = "Games", value = card.gameCount.toString())

                SettingsGroup("Actions")
                SettingsRow(
                    label    = "Import PC Games",
                    sublabel = "Exported games, Add by ID, and launcher status",
                    focusKey = IMPORT_PC_FOCUS_KEY,
                    onClick  = onOpenImportPcGames,
                )
            } else if (isVita) {
                SettingsGroup("Library")
                SettingsValueRow(
                    label    = "Vita3K Data Folder",
                    value    = state.vita3KFolderLabel ?: "Not set",
                    sublabel = state.vita3KFolderLabel
                        ?.let { "Reading installed titles from this ux0 folder" }
                        ?: "Pick your Vita3K ux0 folder (e.g. Roms/vita/ux0) so ECHO can find games",
                    onClick  = { vitaFolderPicker.launch(null) },
                )
                SettingsValueRow(label = "Games", value = card.gameCount.toString())

                SettingsGroup("Actions")
                SettingsRow(
                    label    = "Scan For Vita Games",
                    sublabel = "Reads installed titles from ux0/app in your Vita3K data folder",
                    onClick  = if (!isScanning && state.vita3KFolderLabel != null) ({ onScanVitaGames() }) else null,
                )
                if (state.vita3KFolderLabel != null) {
                    SettingsRow(
                        label    = "Release Vita3K Folder",
                        sublabel = "Stop reading this ux0 folder. The files on disk are not touched",
                        onClick  = onReleaseVita3KFolder,
                    )
                }
            } else if (isAndroid) {
                SettingsGroup("Apps")
                SettingsRow(
                    label    = "Add Apps",
                    sublabel = "Pick installed apps to add to this library",
                    onClick  = onAddAndroidApps,
                )
                if (state.androidApps.isEmpty()) {
                    Hint("No apps yet — use Add Apps to pick installed apps for this library.")
                } else {
                    state.androidApps.forEach { app ->
                        SettingsRow(
                            label    = app.label,
                            trailing = { Text("Remove", color = SettingsAccent) },
                            onClick  = { removeAppConfirm = app.gameId to app.label },
                        )
                    }
                }
            } else {
                SettingsGroup("Library")
                SettingsValueRow(
                    label    = "ROM Directory",
                    value    = card.romDirectory?.substringAfterLast('/') ?: "Not set",
                    sublabel = card.romDirectory
                        ?: "Scans this console's folder under your ROM Root",
                )
                SettingsValueRow(
                    label   = "Emulator",
                    value   = card.emulatorName ?: "None",
                    onClick = { onLoadEmulatorOptions(); showEmulatorDialog = true },
                )
                SettingsValueRow(label = "Games", value = card.gameCount.toString())

                SettingsGroup("Supported Files")
                if (card.extensions.isEmpty()) {
                    Hint("No extensions set — add at least one so scanning can match this console's ROMs.")
                } else {
                    card.extensions.forEach { ext ->
                        SettingsRow(
                            label    = ".$ext",
                            trailing = { Text("Remove", color = SettingsAccent) },
                            onClick  = { removeExtConfirm = ext },
                        )
                    }
                }
                SettingsTextFieldRow(
                    label         = "Add Extension",
                    value         = newExt,
                    onValueChange = { newExt = it },
                    placeholder   = "e.g. iso, chd, zip",
                    helper        = "Matched case-insensitively when scanning.",
                )
                newExt.trim().lowercase().removePrefix(".").filter { it.isLetterOrDigit() }
                    .takeIf { it.isNotBlank() }
                    ?.let { clean ->
                        SettingsRow(
                            label   = "Add \".$clean\"",
                            onClick = { onAddExtension(card.platformId, newExt); newExt = "" },
                        )
                    }

                SettingsGroup("Actions")
                SettingsRow(
                    label    = "Scan This Console",
                    sublabel = when {
                        isScanning -> "Scanning…"
                        card.romDirectory == null -> "ROM directory not configured"
                        else -> "Scan only this console's folder"
                    },
                    onClick  = if (!isScanning && card.romDirectory != null) ({ onScanConsole(card.platformId) }) else null,
                )
            }

            if (isAndroid) SettingsGroup("Actions")

            SettingsRow(
                label    = "Scrape Missing Artwork",
                sublabel = "Fetch box art, logos and backgrounds for this card's games that have none",
                onClick  = { onScrapeArtwork(card.platformId) },
            )
            SettingsRow(label = "Rename System", onClick = { onBeginRename(card.platformId) })
            SettingsToggleRow(
                label    = "Show In Games",
                sublabel = "Show or hide this system",
                checked  = card.enabled,
                onToggle = { onToggleEnabled(card.platformId, it) },
            )
            SettingsToggleRow(
                label    = "Pin To Top",
                checked  = card.pinned,
                onToggle = { onTogglePinned(card.platformId, it) },
            )

            if (!isWindows) {
                SettingsGroup("Danger Zone")
                SettingsRow(
                    label    = "Remove System",
                    sublabel = "Removes this console and its games. ROM files are not deleted.",
                    trailing = { Text("Remove", color = SettingsAccent) },
                    onClick  = { showRemoveConfirm = true },
                )
            }
        }
    }

    if (showEmulatorDialog) {
        EmulatorPickerDialog(
            options    = state.emulatorOptions,
            onSelect   = { onSetEmulatorForDetail(it); showEmulatorDialog = false },
            onDismiss  = { showEmulatorDialog = false },
        )
    }

    if (showRemoveConfirm) {
        SettingsConfirmOverlay(
            title = "Remove ${card.displayName}?",
            message = "This removes the console and its scanned games from the library. " +
                "ROM files on disk are not deleted.",
            confirmLabel = "Remove",
            onConfirm = { showRemoveConfirm = false; onRemoveCard(card.platformId) },
            onCancel = { showRemoveConfirm = false },
        )
    }
    removeAppConfirm?.let { (gameId, label) ->
        SettingsConfirmOverlay(
            title = "Remove $label?",
            message = "Takes the app out of this library. The app stays installed.",
            confirmLabel = "Remove",
            onConfirm = { removeAppConfirm = null; onRemoveApp(gameId) },
            onCancel = { removeAppConfirm = null },
        )
    }
    removeExtConfirm?.let { ext ->
        SettingsConfirmOverlay(
            title = "Remove .$ext?",
            message = "Scans stop matching .$ext files for ${card.displayName}. Games already found stay until " +
                "a scan that removes missing games.",
            confirmLabel = "Remove",
            onConfirm = { removeExtConfirm = null; onRemoveExtension(card.platformId, ext) },
            onCancel = { removeExtConfirm = null },
        )
    }
}

@Composable
private fun EmulatorPickerDialog(
    options: List<EmulatorOption>,
    onSelect: (EmulatorOption) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsChoiceOverlay(
        title = "Set Emulator",
        options = options.map { it.name },

        selectedIndex = -1,
        onPick = { onSelect(options[it]) },
        onCancel = onDismiss,
    )
}

@Composable
private fun ImportPcGamesContent(
    state: LibraryManagerUiState,
    onBack: () -> Unit,
    onRefreshHomeStatus: () -> Unit,
    onScanPcGamesFolder: (Uri) -> Unit,
    onExportManualPcGames: () -> Unit,
    onImportPcGame: (PcGameRow) -> Unit,
    onImportAllPcGames: () -> Unit,
    onTestLaunchPcGame: (PcLauncherRow, String, String?) -> Unit,
    onAddPcGameById: (PcLauncherRow, String, String, String?) -> Unit,
    onDismissMessage: () -> Unit,
    homeRoleIntentProvider: () -> android.content.Intent?,
    modifier: Modifier,
) {
    var addTarget by remember { mutableStateOf<PcLauncherRow?>(null) }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { onScanPcGamesFolder(it) } }

    val homeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onRefreshHomeStatus() }

    SettingsPageScaffold(subtitle = "Import PC Games", onBack = onBack, modifier = modifier) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            state.message?.let { MessageRow(it) { onDismissMessage() } }

            SettingsGroup("Add To Home Capture")
            SettingsValueRow(
                label    = "ECHO as Home",
                value    = if (state.isHomeLauncher) "Active" else "Set…",
                sublabel = if (state.isHomeLauncher)
                    "Using \"Add to home\" inside a supported launcher imports the game here automatically"
                else
                    "Set ECHO as your Home app so a launcher's \"Add to home\" option imports the game into ECHO",
                onClick  = { runCatching { homeLauncher.launch(homeRoleIntentProvider()) } },
            )

            SettingsGroup("Exported Games")
            SettingsRow(
                label    = "Scan Import Folder",
                sublabel = "Pick the folder your launcher exports to — ECHO scans it for GameNative / " +
                    "Winlator exports (.steam · .epic · .gog · .amazon · .pcgame · .desktop) and ECHO's own " +
                    ".pfpgame exports, and imports them",
                onClick  = { importPicker.launch(null) },
            )
            SettingsRow(
                label    = "Export Manual Games",
                sublabel = "Writes a .pfpgame file into <windows>/import for each game added by ID or captured, " +
                    "and each pin with artwork, so a fresh install can bring them back with their artwork",
                onClick  = onExportManualPcGames,
            )

            SettingsGroup("PC Launchers")
            state.pcLaunchers.forEach { launcher ->
                when {
                    !launcher.installed -> SettingsValueRow(label = launcher.name, value = "Not installed")
                    launcher.canAddById -> SettingsValueRow(
                        label    = launcher.name,
                        value    = "Add by ID…",
                        sublabel = "Add a game by its ID — or use the app's own Add-to-home option",
                        onClick  = { addTarget = launcher },
                    )
                    else -> SettingsValueRow(
                        label    = launcher.name,
                        value    = "Installed",
                        sublabel = "No add-by-ID support — export the game to <windows>/import instead",
                    )
                }
            }

            SettingsGroup("Found Games (${state.pcGames.size})")
            if (state.pcGames.isEmpty()) {
                Hint("No PC games captured yet.")
            } else {
                state.pcGames.forEach { row ->
                    SettingsValueRow(
                        label    = row.title,
                        sublabel = row.launcherName,
                        value    = "Import",
                        onClick  = { onImportPcGame(row) },
                    )
                }
                SettingsRow(
                    label    = "Import All",
                    sublabel = "Add every found game to a collection named after its launcher",
                    onClick  = onImportAllPcGames,
                )
            }
        }
    }

    addTarget?.let { launcher ->
        AddPcGameDialog(
            launcher = launcher,
            onTest   = { id, source -> onTestLaunchPcGame(launcher, id, source) },
            onAdd    = { id, title, source -> onAddPcGameById(launcher, id, title, source); addTarget = null },
            onDismiss = { addTarget = null },
        )
    }
}

private enum class AddPcRow { ID, NAME, SOURCE, TEST, ADD }

// owner, 2026-10-07: on the kit, not a Material dialog. The rail menu holds the fields, the source, Test Launch
// and Add, so every one is reachable by pad; ID and name open the rail prompt
@Composable
private fun AddPcGameDialog(
    launcher: PcLauncherRow,
    onTest: (id: String, source: String?) -> Unit,
    onAdd: (id: String, title: String, source: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val adapter = PcLauncherAdapters.forType(launcher.type)
    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(adapter?.sources?.firstOrNull()) }
    var cursor by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<AddPcRow?>(null) }
    var draft by remember { mutableStateOf("") }

    val ready = id.isNotBlank() && title.isNotBlank()
    val rows = buildList {
        add(MenuRow(AddPcRow.ID, "Game ID · ${id.ifBlank { "not set" }}"))
        add(MenuRow(AddPcRow.NAME, "Game name · ${title.ifBlank { "not set" }}"))
        if (adapter != null && adapter.sources.size > 1) add(MenuRow(AddPcRow.SOURCE, "Source · $source"))
        if (id.isNotBlank()) add(MenuRow(AddPcRow.TEST, "Test Launch"))
        if (ready) add(MenuRow(AddPcRow.ADD, "Add"))
    }
    val at = cursor.coerceIn(0, rows.lastIndex)

    fun activate(row: AddPcRow) {
        when (row) {
            AddPcRow.ID -> { draft = id; editing = AddPcRow.ID }
            AddPcRow.NAME -> { draft = title; editing = AddPcRow.NAME }
            AddPcRow.SOURCE -> adapter?.sources?.let { all -> source = all[(all.indexOf(source) + 1) % all.size] }
            AddPcRow.TEST -> onTest(id, source)
            AddPcRow.ADD -> onAdd(id, title, source)
        }
    }

    val field = editing
    if (field != null) {
        SettingsTextPromptOverlay(
            title = if (field == AddPcRow.ID) "Game ID" else "Game name",
            value = draft,
            onValueChange = { draft = it },
            onConfirm = {
                if (field == AddPcRow.ID) id = draft.trim() else title = draft.trim()
                editing = null
            },
            onCancel = { editing = null },
        )
        return
    }

    SettingsOverlayInput { action ->
        when (action) {
            GamepadAction.NAVIGATE_UP -> cursor = (at - 1).coerceAtLeast(0)
            GamepadAction.NAVIGATE_DOWN -> cursor = (at + 1).coerceAtMost(rows.lastIndex)
            GamepadAction.SELECT -> rows.getOrNull(at)?.action?.let(::activate)
            GamepadAction.BACK -> onDismiss()
            else -> Unit
        }
    }
    EchoContextMenuOverlay(
        state = MenuState(title = "Add ${launcher.name} game", subtitle = adapter?.idPrompt, rows = rows, selectedIndex = at),
        onRowActivated = { index -> if (index == at) rows[index].action?.let(::activate) else cursor = index },
        onDismiss = onDismiss,
    )
}

private fun cardSublabel(card: LibraryCardRow): String = buildString {
    append(card.romDirectory ?: "No ROM directory")
    append("  ·  ${card.gameCount} game${if (card.gameCount == 1) "" else "s"}")
}

@Composable
private fun Hint(text: String) {
    Text(text = text, color = SettingsSubtext, modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp))
}

@Composable
private fun MessageRow(message: String, onDismiss: () -> Unit) {
    SettingsRow(
        label    = message,
        sublabel = "Tap to dismiss",
        trailing = { Text("✕", color = SettingsAccent, fontWeight = FontWeight.Bold) },
        onClick  = onDismiss,
    )
}

@CombinedPreviews
@Composable
fun LibraryManagerScreenPreview() {
    EchoPreview {
        LibraryManagerContent(
            state = SettingsPreviewData.libraryListState,
            onBack = {},
            onAddAndroidApps = {},
            onOpenCardDetail = {},
            onStartAddConsole = {},
            onRequestRomFolderSetup = {},
            onScanAllConsoles = {},
            onDismissMessage = {},
            onPlatformChosen = {},
            onEmulatorChosen = {},
            onConfirmAddConsole = {},
            onLoadEmulatorOptions = {},
            onRemoveExtension = { _, _ -> },
            onAddExtension = { _, _ -> },
            onScanConsole = {},
            onScrapeArtwork = {},
            onBeginRename = {},
            onCancelRename = {},
            onConfirmRename = {},
            onToggleEnabled = { _, _ -> },
            onTogglePinned = { _, _ -> },
            onRemoveCard = {},
            onSetEmulatorForDetail = {},
            onOpenImportPcGames = {},
            onSetVita3KFolder = {},
            onScanVitaGames = {},
            onReleaseVita3KFolder = {},
            onRemoveApp = {},
            onRefreshHomeStatus = {},
            onScanPcGamesFolder = {},
            onExportManualPcGames = {},
            onImportPcGame = {},
            onImportAllPcGames = {},
            onTestLaunchPcGame = { _, _, _ -> },
            onAddPcGameById = { _, _, _, _ -> },
            homeRoleIntentProvider = { null }
        )
    }
}
