package com.psplauncher.core.data.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
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

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
