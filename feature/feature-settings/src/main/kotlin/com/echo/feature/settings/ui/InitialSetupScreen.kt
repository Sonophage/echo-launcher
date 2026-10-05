package com.echo.feature.settings.ui

import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.echo.core.domain.model.settingsEntryFor
import com.echo.core.ui.preview.CombinedPreviews
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.settings.permissions.AppPermissions
import com.echo.feature.settings.ui.wizard.WizardScaffold
import com.echo.feature.settings.ui.wizard.WizardSplash
import com.echo.feature.settings.viewmodel.InitialSetupUiState
import com.echo.feature.settings.viewmodel.InitialSetupViewModel
import com.echo.feature.settings.viewmodel.RootFolderRow
import com.echo.feature.settings.viewmodel.pickerStartUri
import com.echo.feature.settings.viewmodel.SetupStep
import com.echo.feature.settings.viewmodel.StorageSlot
import com.echo.feature.settings.viewmodel.storageSlotsFor

@Composable
fun InitialSetupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,

    firstRun: Boolean = false,
    onOpenLibraryManager: () -> Unit = {},

    onGoToLibrary: () -> Unit = {},
    viewModel: InitialSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var splashDone by rememberSaveable { mutableStateOf(!firstRun) }
    if (!splashDone) {
        WizardSplash(onBegin = { splashDone = true })
        return
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetWizard() }
    }

    var pending by remember { mutableStateOf<Pair<StorageSlot, String?>?>(null) }
    val storagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val (slot, replacing) = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (uri != null) viewModel.onStoragePicked(slot, replacing, uri)
    }

    // after a rename or a new ECHO folder, Android needs the user to pick it once
    LaunchedEffect(state.artworkPickAgain) {
        state.artworkPickAgain?.let {
            pending = StorageSlot.ARTWORK to null
            storagePicker.launch(it.toUri())
            viewModel.artworkPickAgainLaunched()
        }
    }

    val retroPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.linkRetroArch(uri) }

    val vitaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.linkVitaFolder(uri) }

    var grantToken by remember { mutableIntStateOf(0) }
    val askPermission = rememberPermissionAsker { grantToken++; viewModel.refreshGrants() }
    val systemScreen = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { grantToken++; viewModel.refreshGrants() }

    val regrantFolder = rememberFolderRegrant(viewModel::regrantFolder)

    val openSettingsScreen = LocalSettingsOpenScreen.current
    val openScreen: (String) -> Unit = { id ->
        viewModel.parkForExcursion()
        openSettingsScreen(id)
    }

    val step = state.step
    val next = state.nextStep
    val continueRow: @Composable () -> Unit = {
        if (next != null) SettingsRow(label = "Continue", sublabel = "Next: ${titleFor(next)}", onClick = viewModel::nextStep)
    }

    WizardScaffold(
        stepNumber = state.stepNumber,
        stepCount = state.stepCount,
        title = "Setup",
        onBack = { if (!viewModel.previousStep() && !firstRun) onBack() },

        onSkip = onBack,
        backEnabled = step != SetupStep.FEATURES || !firstRun,
        message = state.message,
        onDismissMessage = viewModel::dismissMessage,
        heading = headingFor(step),
        hint = hintFor(step),
        contentKey = step,
        modifier = modifier,
    ) {
        when (step) {
            SetupStep.FEATURES -> {
                FeaturesPage(state, onGaming = viewModel::setGaming, onMedia = viewModel::setMedia)
                continueRow()
            }
            SetupStep.PERMISSIONS -> {
                PermissionsPage(
                    state = state,
                    grantToken = grantToken,
                    onRefresh = { grantToken++; viewModel.refreshGrants() },
                    onAsk = askPermission,
                    onSetAsHome = { systemScreen.launch(viewModel.homeRoleIntent()) },
                    onGrantFolder = regrantFolder,
                )
                continueRow()
            }
            SetupStep.STORAGE -> {
                StoragePage(
                    state = state,
                    onPick = { slot, replacing, start ->
                        pending = slot to replacing
                        storagePicker.launch(start)
                    },
                    onCreateConsoleFolders = viewModel::createStandardRomFolders,
                )
                continueRow()
            }
            SetupStep.EMULATORS -> {
                EmulatorsPage(
                    state = state,
                    onLinkRetroArch = { retroPicker.launch(null) },
                    onLinkVita = { vitaPicker.launch(null) },
                )
                continueRow()
            }
            SetupStep.ACCOUNTS -> AccountsPage(
                state = state,
                onOpenScreen = openScreen,
                onFinish = { viewModel.finishSetup(); onBack() },
                onGoToLibrary = { viewModel.finishSetup(); onGoToLibrary() },
            )
        }
    }
}

private const val RETROARCH_PICK_STEPS =
    "In the picker: tap ☰, choose RetroArch, then Use this folder. Not the /RetroArch folder on storage."

private fun titleFor(step: SetupStep): String = when (step) {
    SetupStep.FEATURES -> "What ECHO is for"
    SetupStep.PERMISSIONS -> "Permissions"
    SetupStep.STORAGE -> "Your folders"
    SetupStep.EMULATORS -> "Emulators"
    SetupStep.ACCOUNTS -> "Accounts"
}

private fun headingFor(step: SetupStep): String = when (step) {
    SetupStep.FEATURES    -> "What is ECHO for?"
    SetupStep.PERMISSIONS -> "Let ECHO see your library."
    SetupStep.STORAGE     -> "Point ECHO at your folders."
    SetupStep.EMULATORS   -> "Link your emulators."
    SetupStep.ACCOUNTS    -> "Connect your accounts."
}

private fun hintFor(step: SetupStep): String = when (step) {
    SetupStep.FEATURES    -> "Setup only asks about what you turn on. Libraries and Settings bring the rest back."
    SetupStep.PERMISSIONS -> "Each one turns something on. Everything here can be changed later in Settings."
    SetupStep.STORAGE     -> "Each row opens the picker on the folder ECHO found. Tap Use this folder, then Allow."
    SetupStep.EMULATORS   -> "So ECHO only offers the cores and games you actually have."
    SetupStep.ACCOUNTS    -> "All optional. Each opens its settings page, so every key is entered once."
}

@Composable
private fun FeaturesPage(state: InitialSetupUiState, onGaming: (Boolean) -> Unit, onMedia: (Boolean) -> Unit) {
    // your apps are always there, so both off is a launcher only
    SettingsGroup(
        when {
            state.gaming && state.media -> "The full suite"
            state.gaming -> "Apps and games"
            state.media -> "Apps and media"
            else -> "A launcher only"
        },
    )
    SettingsToggleRow(
        label = "Gaming",
        sublabel = "Your games folder, emulators and the Game column",
        focusKey = "features_first",
        checked = state.gaming,
        onToggle = onGaming,
    )
    SettingsToggleRow(
        label = "Media",
        sublabel = "Music, video, photos and books, with their folders and permissions",
        checked = state.media,
        onToggle = onMedia,
    )
}

@Composable
private fun PermissionsPage(
    state: InitialSetupUiState,
    grantToken: Int,
    onRefresh: () -> Unit,
    onAsk: (com.echo.feature.settings.permissions.AppPermission) -> Unit,
    onSetAsHome: () -> Unit,
    onGrantFolder: (com.echo.feature.settings.viewmodel.FolderAccessRow) -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        onRefresh()
        onPauseOrDispose { }
    }

    val rows = remember(state.media) { AppPermissions.forWizard(Build.VERSION.SDK_INT, state.media) }
    AppPermissionRows(rows, grantToken, onAsk, firstFocusKey = "perm_first")
    RestrictedSettingsRow(rows, grantToken)
    SettingsValueRow(
        label = "ECHO as Home",
        value = if (state.isHomeLauncher) "Active" else "Set…",
        sublabel = "Makes the Home button come back to ECHO",
        onClick = { if (!state.isHomeLauncher) onSetAsHome() },
    )
    // a folder linked before (a later setup, or a reinstall) whose access Android has dropped
    FolderAccessRows(state.folderAccess.filterNot { it.granted }, onGrantFolder)
}

@Composable
private fun StoragePage(
    state: InitialSetupUiState,
    onPick: (slot: StorageSlot, replacing: String?, start: Uri?) -> Unit,
    onCreateConsoleFolders: () -> Unit,
) {
    storageSlotsFor(state.gaming, state.media).forEachIndexed { index, slot ->
        val row = storageRow(state, slot)
        SettingsValueRow(
            label = slot.label,
            value = row.value,
            sublabel = row.sublabel,
            focusKey = if (index == 0) "storage_first" else null,
            onClick = { onPick(slot, row.replacing, row.start?.let(::pickerStartUri)) },
        )
    }
    if (state.romRoots.any { it.linked }) {
        SettingsRow(
            label = "Create console folders",
            sublabel = "One folder per console under your games folder, for a fresh card",
            onClick = onCreateConsoleFolders,
        )
    }
}

private data class StorageRowUi(val value: String, val sublabel: String?, val replacing: String?, val start: String?)

private fun storageRow(state: InitialSetupUiState, slot: StorageSlot): StorageRowUi {
    val suggestion = state.suggestions[slot]
    val suggestedName = suggestion?.let { Uri.decode(it).substringAfterLast(':') }
    if (slot == StorageSlot.ARTWORK) {
        val name = state.artworkFolderName
        return StorageRowUi(
            value = name ?: "Not set",
            sublabel = when {
                name == null -> "Pick where ECHO keeps its own folder, named ECHO: artwork, and its look as files"
                !name.equals(com.echo.core.data.repository.EchoFolder.NAME, ignoreCase = true) -> "Tap to make it ECHO's folder"
                else -> "Tap to change"
            },
            replacing = null,
            start = suggestion,
        )
    }
    val roots: List<RootFolderRow> = state.rootsFor(slot)
    val first = roots.firstOrNull()
    return when {
        first == null -> StorageRowUi(
            value = "Not set",
            sublabel = suggestedName?.let { "Tap to use $it" } ?: "Tap to choose a folder",
            replacing = null,
            start = suggestion,
        )
        !first.linked -> StorageRowUi(
            value = "Access lost",
            sublabel = "Tap to grant ${first.name} again",
            replacing = first.treeUri,
            start = first.treeUri,
        )
        else -> StorageRowUi(
            value = first.name + if (roots.size > 1) " +${roots.size - 1}" else "",
            sublabel = "Tap to change",
            replacing = first.treeUri,
            start = first.treeUri,
        )
    }
}

@Composable
private fun EmulatorsPage(
    state: InitialSetupUiState,
    onLinkRetroArch: () -> Unit,
    onLinkVita: () -> Unit,
) {
    if (state.retroArchInstalled) {
        SettingsGroup("RetroArch")
        SettingsValueRow(
            label = "Link RetroArch",
            value = when {
                state.retroArchDetecting -> "Checking…"
                !state.retroArchLinked -> "Not linked"
                state.retroArchCoreCount == 0 -> "No cores found"
                else -> "${state.retroArchCoreCount} cores"
            },
            sublabel = RETROARCH_PICK_STEPS,
            focusKey = "emulators_retroarch",
            onClick = onLinkRetroArch,
        )
    }
    if (state.vita3KInstalled) {
        SettingsGroup("Vita3K")
        SettingsValueRow(
            label = "Vita3K data folder",
            value = state.vitaFolderName ?: "Not set",
            sublabel = "Grant its ux0 folder so installed Vita titles show up",
            onClick = onLinkVita,
        )
    }
}

internal val ACCOUNT_SCREENS = listOf("settings_accounts", "settings_discord", "settings_artwork_sources")

@Composable
internal fun AccountsPage(
    state: InitialSetupUiState,
    onOpenScreen: (String) -> Unit,
    onFinish: () -> Unit,
    onGoToLibrary: () -> Unit,
) {
    ACCOUNT_SCREENS.map { requireNotNull(settingsEntryFor(it)) { "$it is not in the catalog" } }
        .forEachIndexed { index, entry ->
            SettingsValueRow(
                label = entry.title,
                value = if (entry.id == "settings_artwork_sources") "${state.scrapersConnected} of 4" else "Open",
                sublabel = entry.subtitle,
                focusKey = if (index == 0) "accounts_first" else null,
                onClick = { onOpenScreen(entry.id) },
            )
        }

    SettingsGroup("Done")
    if (state.romRoots.isNotEmpty()) {
        SettingsRow(label = "Go to your games", sublabel = "Finish and open All Games", onClick = onGoToLibrary)
    }
    SettingsRow(label = "Finish", sublabel = "Everything here stays in Settings", focusKey = "finish_done", onClick = onFinish)
}

@CombinedPreviews
@Composable
private fun StoragePagePreview() {
    EchoScreenPreview {
        WizardScaffold(
            stepNumber = 3,
            stepCount = 5,
            title = "Setup",
            onBack = {},
            heading = headingFor(SetupStep.STORAGE),
            hint = hintFor(SetupStep.STORAGE),
        ) {
            StoragePage(
                state = InitialSetupUiState(
                    musicRoots = listOf(RootFolderRow("content://t/x", "Music", linked = true)),
                    videoRoots = listOf(RootFolderRow("content://t/y", "Movies", linked = false)),
                    suggestions = mapOf(StorageSlot.GAMES to "content://d/document/6DBF-B253%3AEmulation%2FROMs"),
                ),
                onPick = { _, _, _ -> },
                onCreateConsoleFolders = {},
            )
        }
    }
}
