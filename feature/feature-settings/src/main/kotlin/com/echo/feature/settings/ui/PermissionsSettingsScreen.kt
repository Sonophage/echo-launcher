package com.echo.feature.settings.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.echo.feature.settings.permissions.AppPermissions
import com.echo.feature.settings.permissions.GrantRoute
import com.echo.feature.settings.viewmodel.PermissionsViewModel
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val folders by viewModel.folders.collectAsState()
    val message by viewModel.message.collectAsState()
    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.messageShown() }
    }
    val regrantFolder = rememberFolderRegrant(viewModel::regrant)

    var readToken by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                readToken++
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val ask = rememberPermissionAsker { readToken++ }

    val rows = remember { AppPermissions.forSdk(Build.VERSION.SDK_INT) }

    SettingsPageScaffold(
        subtitle = "Permissions",
        onBack = onBack,
        modifier = modifier,
    ) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            SettingsGroup("Special access")
            AppPermissionRows(rows.filter { it.route == GrantRoute.SYSTEM_SCREEN }, readToken, ask)
            RestrictedSettingsRow(rows, readToken)

            if (folders.isNotEmpty()) {
                SettingsGroup("Folders")
                FolderAccessRows(folders, regrantFolder)
            }

            SettingsGroup("Asked for when needed")
            AppPermissionRows(rows.filter { it.route == GrantRoute.REQUEST }, readToken, ask)

            SettingsGroup("Granted at install")
            AppPermissionRows(rows.filter { it.route == GrantRoute.INSTALL_TIME }, readToken, ask)
        }
    }
}
