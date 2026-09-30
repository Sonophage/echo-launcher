package com.psplauncher.feature.settings.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.psplauncher.feature.settings.permissions.AppPermission
import com.psplauncher.feature.settings.permissions.AppPermissions
import com.psplauncher.feature.settings.permissions.GrantRoute
import com.psplauncher.feature.settings.permissions.permissionStateLabel
import com.psplauncher.feature.settings.permissions.isGranted
import com.psplauncher.feature.settings.permissions.systemScreenIntent

@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var readToken by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) readToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { readToken++ }

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
            rows.filter { it.route == GrantRoute.SYSTEM_SCREEN }.forEach { row ->
                PermissionRow(row, context, readToken) { openSystemScreen(context, row) }
            }

            SettingsGroup("Asked for when needed")
            rows.filter { it.route == GrantRoute.REQUEST }.forEach { row ->
                PermissionRow(row, context, readToken) {
                    row.manifestName?.let(requestPermission::launch)
                }
            }

            SettingsGroup("Granted at install")
            rows.filter { it.route == GrantRoute.INSTALL_TIME }.forEach { row ->
                PermissionRow(row, context, readToken, onClick = null)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: AppPermission,
    context: Context,
    readToken: Int,
    onClick: (() -> Unit)?,
) {
    val granted = remember(permission.id, readToken) { isGranted(context, permission) }
    SettingsValueRow(
        label = permission.label,
        value = permissionStateLabel(granted, permission.route),
        sublabel = permission.why,
        focusKey = "permission_${permission.id}",
        onClick = onClick,
    )
}

private fun openSystemScreen(context: Context, permission: AppPermission) {
    runCatching {
        context.startActivity(systemScreenIntent(context, permission).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
