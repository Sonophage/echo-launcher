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
import com.echo.core.data.launch.LaunchScreens
import com.echo.feature.settings.permissions.isGranted
import com.echo.feature.settings.permissions.onMainScreen
import com.echo.feature.settings.permissions.permissionStateLabel
import com.echo.feature.settings.permissions.restrictedHintShows
import com.echo.feature.settings.permissions.systemScreenIntents

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

// a special-access screen refused on a side-loaded install says "Controlled by Restricted Setting"; this row opens
// the steps past it. Android adds Allow restricted settings to App info's ⋮ menu only after the switch has been
// refused once, so the steps start at the switch
@Composable
internal fun RestrictedSettingsRow(rows: List<AppPermission>, readToken: Int, onShowSteps: () -> Unit) {
    val context = LocalContext.current
    val blocked = remember(readToken) { restrictedHintShows(Build.VERSION.SDK_INT, rows) { isGranted(context, it) } }
    if (!blocked) return
    SettingsRow(
        label = "Blocked as a restricted setting?",
        sublabel = "Android blocks some access for apps installed outside a store. Select to see how to allow it",
        onClick = onShowSteps,
    )
}

// the steps are a card, not the row's sublabel: on touch a row shows its sublabel only while it has focus, and a
// tap acts at once, so the steps were never seen. Drawn outside the page's scrolling column
@Composable
internal fun RestrictedSettingsSteps(onDismiss: () -> Unit) {
    val context = LocalContext.current
    SettingsConfirmOverlay(
        title = "Allow restricted settings",
        message = RESTRICTED_STEPS,
        confirmLabel = "Open App info",
        cancelLabel = "Close",
        destructive = false,
        onConfirm = {
            onDismiss()
            val options = LaunchScreens.mainScreenOptions(context)
            runCatching { context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options) }
        },
        onCancel = onDismiss,
    )
}

internal const val RESTRICTED_STEPS =
    "Android blocks this access for apps installed outside a store.\n\n" +
        "1. Select the permission and tap ECHO's switch. Close the message Android shows.\n" +
        "2. Select Open App info below.\n" +
        "3. Tap ⋮ at the top right and choose Allow restricted settings.\n" +
        "4. Come back and grant the permission again."

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
            val options = LaunchScreens.mainScreenOptions(context)
            runCatching { context.startActivity(appDetailsIntent(context), options) }
        }
        onDone()
    }
    val screen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onDone() }
    return { permission ->
        when (permission.route) {
            GrantRoute.REQUEST -> permission.manifestName?.let { asking = permission; request.launch(it) }
            // a screen this Android does not have throws, so the next one is tried. onMainScreen is read outside
            // the runCatching, so a failed lookup is not taken for a missing screen
            GrantRoute.SYSTEM_SCREEN -> systemScreenIntents(context, permission).map { it.onMainScreen(context) }
                .firstOrNull { intent -> runCatching { screen.launch(intent) }.isSuccess }
            GrantRoute.INSTALL_TIME -> Unit
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
