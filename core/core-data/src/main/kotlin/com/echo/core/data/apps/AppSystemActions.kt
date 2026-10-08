package com.echo.core.data.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import timber.log.Timber

// Android's own screens for an installed app: its App Info page and its uninstall prompt. One definition, used
// by the App Drawer and by the crossbar's and Recent's app menus (owner, 2026-10-08)
object AppSystemActions {
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Timber.w(it, "Could not open app info for $packageName") }
    }

    // Android asks before it uninstalls; ECHO never offers to uninstall itself
    fun uninstall(context: Context, packageName: String) {
        if (packageName == context.packageName) return
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Timber.w(it, "Could not launch uninstall for $packageName") }
    }
}
