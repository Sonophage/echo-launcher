package com.echo.feature.settings.permissions

import android.os.Build

enum class GrantRoute {
    REQUEST,

    SYSTEM_SCREEN,

    INSTALL_TIME,
}

data class AppPermission(
    val id: String,
    val label: String,
    val why: String,
    val route: GrantRoute,
    val manifestName: String? = null,
    val minSdk: Int = 1,
    val maxSdk: Int = Int.MAX_VALUE,
)

object AppPermissions {
    const val USAGE_ACCESS = "usage_access"
    const val NOTIFICATION_LISTENER = "notification_listener"

    val ALL: List<AppPermission> = listOf(
        AppPermission(
            id = USAGE_ACCESS,
            label = "Usage access",
            why = "Sorts the app drawer by what you have opened recently",
            route = GrantRoute.SYSTEM_SCREEN,
        ),
        AppPermission(
            id = NOTIFICATION_LISTENER,
            label = "Notification access",
            why = "Shows notifications from other apps in the top bar",
            route = GrantRoute.SYSTEM_SCREEN,
        ),
        AppPermission(
            id = "post_notifications",
            label = "Post notifications",
            why = "Lets scans and downloads report progress",
            route = GrantRoute.REQUEST,
            manifestName = "android.permission.POST_NOTIFICATIONS",
            minSdk = Build.VERSION_CODES.TIRAMISU,
        ),
        AppPermission(
            id = "read_media_audio",
            label = "Music",
            why = "Reads the audio on this device for the Music column",
            route = GrantRoute.REQUEST,
            manifestName = "android.permission.READ_MEDIA_AUDIO",
            minSdk = Build.VERSION_CODES.TIRAMISU,
        ),
        AppPermission(
            id = "read_media_images",
            label = "Photos",
            why = "Reads images for the Photo column and wallpapers",
            route = GrantRoute.REQUEST,
            manifestName = "android.permission.READ_MEDIA_IMAGES",
            minSdk = Build.VERSION_CODES.TIRAMISU,
        ),
        AppPermission(
            id = "read_media_video",
            label = "Video",
            why = "Reads video for the Video column and previews",
            route = GrantRoute.REQUEST,
            manifestName = "android.permission.READ_MEDIA_VIDEO",
            minSdk = Build.VERSION_CODES.TIRAMISU,
        ),
        AppPermission(
            id = "read_external_storage",
            label = "Storage",
            why = "Reads media on older Android versions",
            route = GrantRoute.REQUEST,
            manifestName = "android.permission.READ_EXTERNAL_STORAGE",
            maxSdk = Build.VERSION_CODES.S_V2,
        ),
        AppPermission(
            id = "request_delete_packages",
            label = "Uninstall apps",
            why = "Lets Uninstall in an app's menu ask the system to remove it",
            route = GrantRoute.INSTALL_TIME,
            manifestName = "android.permission.REQUEST_DELETE_PACKAGES",
        ),
        AppPermission(
            id = "query_all_packages",
            label = "See installed apps",
            why = "Finds the apps and emulators the drawer and the launcher list",
            route = GrantRoute.INSTALL_TIME,
            manifestName = "android.permission.QUERY_ALL_PACKAGES",
        ),
    )

    fun forSdk(sdk: Int): List<AppPermission> = ALL.filter { sdk >= it.minSdk && sdk <= it.maxSdk }

    // a new device needs every grant the user can give, so setup offers all of them; install-time
    // rows have nothing to tap (owner, 2026-10-04: the media permissions were missing here)
    fun forWizard(sdk: Int): List<AppPermission> = forSdk(sdk).filter { it.route != GrantRoute.INSTALL_TIME }
}

// owner, 2026-10-05: Settings and Setup said "Not granted" and "Grant…" for the same row; one wording now
fun permissionStateLabel(granted: Boolean, route: GrantRoute): String = when {
    granted -> "Granted"
    route == GrantRoute.INSTALL_TIME -> "Unavailable"
    else -> "Grant…"
}
