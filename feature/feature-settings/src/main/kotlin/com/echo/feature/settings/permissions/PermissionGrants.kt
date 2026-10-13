package com.echo.feature.settings.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.echo.core.data.permission.UsageAccess
import com.echo.core.ui.notification.AndroidNotifications

fun isGranted(context: Context, permission: AppPermission): Boolean = when (permission.id) {
    AppPermissions.USAGE_ACCESS -> UsageAccess.isGranted(context)
    AppPermissions.NOTIFICATION_LISTENER -> AndroidNotifications.isEnabled(context)
    else -> permission.manifestName?.let {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } ?: false
}

fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

// Android 13 and later block usage and notification access for an app installed outside a store until
// "Allow restricted settings" is turned on in its App info
fun restrictedSettingsApply(sdk: Int): Boolean = sdk >= android.os.Build.VERSION_CODES.TIRAMISU

// an Android screen started for a result opens on the Main Screen's display (#28); StartActivityForResult reads
// its launch options from this extra
fun Intent.onMainScreen(context: Context): Intent = putExtra(
    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE,
    com.echo.core.data.launch.LaunchScreens.mainScreenOptions(context),
)

fun systemScreenIntent(context: Context, permission: AppPermission): Intent = when (permission.id) {
    AppPermissions.NOTIFICATION_LISTENER -> AndroidNotifications.settingsIntent()
    AppPermissions.USAGE_ACCESS -> UsageAccess.settingsIntent()
    else -> appDetailsIntent(context)
}
