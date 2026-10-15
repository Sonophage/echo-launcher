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

// Android blocks a special access for an app installed outside a store until "Allow restricted settings" is
// turned on in its App info: notification access from Android 13, usage access from Android 15. An adb install
// is not blocked, so a debug build does not show it
fun restrictedSettingsApply(sdk: Int, permission: AppPermission): Boolean = when (permission.id) {
    AppPermissions.NOTIFICATION_LISTENER -> sdk >= android.os.Build.VERSION_CODES.TIRAMISU
    AppPermissions.USAGE_ACCESS -> sdk >= android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
    else -> false
}

fun restrictedHintShows(sdk: Int, rows: List<AppPermission>, granted: (AppPermission) -> Boolean): Boolean =
    rows.any { restrictedSettingsApply(sdk, it) && !granted(it) }

// an Android screen started for a result opens on the Main Screen's display (#28); StartActivityForResult reads
// its launch options from this extra
fun Intent.onMainScreen(context: Context): Intent = putExtra(
    androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE,
    com.echo.core.data.launch.LaunchScreens.mainScreenOptions(context),
)

// the screens to try, in order; the last is ECHO's App info, for a ROM that has none of the others
fun systemScreenIntents(context: Context, permission: AppPermission): List<Intent> = when (permission.id) {
    AppPermissions.NOTIFICATION_LISTENER -> listOf(AndroidNotifications.settingsIntent())
    AppPermissions.USAGE_ACCESS -> UsageAccess.settingsIntents(context.packageName)
    else -> emptyList()
} + appDetailsIntent(context)
