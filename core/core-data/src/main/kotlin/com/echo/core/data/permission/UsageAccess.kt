package com.echo.core.data.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

object UsageAccess {
    fun isGranted(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    // the screens to try, in order: a package uri opens the app's own switch (Android 10 and later), and a ROM
    // without that page takes the list of every app
    fun settingsIntents(packageName: String): List<Intent> = listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", packageName, null)),
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
    )
}
