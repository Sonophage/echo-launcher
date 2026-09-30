package com.psplauncher.feature.settings.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.psplauncher.core.ui.notification.AndroidNotifications
import com.psplauncher.feature.settings.permissions.AppPermission
import com.psplauncher.feature.settings.permissions.AppPermissions
import com.psplauncher.feature.settings.permissions.GrantRoute
import com.psplauncher.feature.settings.permissions.permissionStateLabel

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

private fun isGranted(context: Context, permission: AppPermission): Boolean = when (permission.id) {
    AppPermissions.USAGE_ACCESS -> hasUsageAccess(context)
    AppPermissions.NOTIFICATION_LISTENER -> AndroidNotifications.isEnabled(context)
    else -> permission.manifestName?.let {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } ?: false
}

private fun hasUsageAccess(context: Context): Boolean = runCatching {
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = ops.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName,
    )
    mode == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

private fun openSystemScreen(context: Context, permission: AppPermission) {
    val intent = when (permission.id) {
        AppPermissions.NOTIFICATION_LISTENER -> AndroidNotifications.settingsIntent()
        AppPermissions.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        else -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
