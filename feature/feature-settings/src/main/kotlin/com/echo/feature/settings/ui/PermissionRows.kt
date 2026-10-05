package com.echo.feature.settings.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.echo.feature.settings.permissions.AppPermission
import com.echo.feature.settings.permissions.GrantRoute
import com.echo.feature.settings.permissions.appDetailsIntent
import com.echo.feature.settings.permissions.isGranted
import com.echo.feature.settings.permissions.permissionStateLabel
import com.echo.feature.settings.permissions.restrictedSettingsApply
import com.echo.feature.settings.permissions.systemScreenIntent

// owner, 2026-10-05: Settings ▸ Permissions and Setup draw the same rows, ask the same way and re-read the
// same way, so a permission reads and behaves alike in both
@Composable
internal fun AppPermissionRows(
    rows: List<AppPermission>,
    readToken: Int,
    onAsk: (AppPermission) -> Unit,
    firstFocusKey: String? = null,
) {
    val context = LocalContext.current
    rows.forEachIndexed { index, permission ->
        val granted = remember(permission.id, readToken) { isGranted(context, permission) }
        SettingsValueRow(
            label = permission.label,
            value = permissionStateLabel(granted, permission.route),
            sublabel = permission.why,
            focusKey = if (index == 0 && firstFocusKey != null) firstFocusKey else "permission_${permission.id}",
            onClick = if (granted || permission.route == GrantRoute.INSTALL_TIME) null else ({ onAsk(permission) }),
        )
    }
}

// a special-access screen refused on a side-loaded install says "restricted setting"; this row explains the
// way past it and opens ECHO's App info, where the switch is
@Composable
internal fun RestrictedSettingsRow(rows: List<AppPermission>, readToken: Int) {
    val context = LocalContext.current
    val blocked = remember(readToken) {
        restrictedSettingsApply(Build.VERSION.SDK_INT) &&
            rows.any { it.route == GrantRoute.SYSTEM_SCREEN && !isGranted(context, it) }
    }
    if (!blocked) return
    SettingsRow(
        label = "Usage or notification access refused?",
        sublabel = "Android blocks them for apps installed outside a store. Open App info, tap ⋮, choose Allow restricted settings, then grant again",
        onClick = { runCatching { context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
    )
}

// asks for one permission: a runtime prompt, or its Android screen. A prompt Android no longer shows (the
// permission was denied for good) opens ECHO's App info instead of silently doing nothing
@Composable
internal fun rememberPermissionAsker(onDone: () -> Unit): (AppPermission) -> Unit {
    val context = LocalContext.current
    var asking by remember { mutableStateOf<AppPermission?>(null) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val permission = asking
        asking = null
        val activity = context.findActivity()
        if (!granted && permission?.manifestName != null && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.manifestName)
        ) {
            runCatching { context.startActivity(appDetailsIntent(context)) }
        }
        onDone()
    }
    val screen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onDone() }
    return { permission ->
        when (permission.route) {
            GrantRoute.REQUEST -> permission.manifestName?.let { asking = permission; request.launch(it) }
            GrantRoute.SYSTEM_SCREEN -> runCatching { screen.launch(systemScreenIntent(context, permission)) }
            GrantRoute.INSTALL_TIME -> Unit
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
