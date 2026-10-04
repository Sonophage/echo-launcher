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

fun systemScreenIntent(context: Context, permission: AppPermission): Intent = when (permission.id) {
    AppPermissions.NOTIFICATION_LISTENER -> AndroidNotifications.settingsIntent()
    AppPermissions.USAGE_ACCESS -> UsageAccess.settingsIntent()
    else -> Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
}
