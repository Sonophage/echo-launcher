package com.psplauncher.feature.settings.permissions

import android.os.Build

/**
 * How a permission is asked for, which decides what tapping its row does.
 */
enum class GrantRoute {
    /** A runtime permission the app can request in a dialog. */
    REQUEST,

    /** Special access that only the system settings app can grant. */
    SYSTEM_SCREEN,

    /** Granted at install and never revocable, so the row is informational. */
    INSTALL_TIME,
}

/**
 * One line in the permissions list.
 *
 * [why] is what the permission buys the user, not what the API is called. A row that
 * cannot say what it is for does not belong on this screen.
 */
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

    /**
     * Everything the manifest declares that a person could reasonably want to see or
     * change, plus the two special accesses that are not manifest permissions at all.
     *
     * Deliberately absent: ACCESS_NETWORK_STATE, INTERNET, FOREGROUND_SERVICE,
     * FOREGROUND_SERVICE_MEDIA_PLAYBACK and RECEIVE_BOOT_COMPLETED. They are granted at
     * install, cannot be revoked, and listing them would pad the screen with rows that
     * never change and never need an action.
     */
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

    /** The rows worth drawing on this device: a permission for an Android it never runs is noise. */
    fun forSdk(sdk: Int): List<AppPermission> = ALL.filter { sdk >= it.minSdk && sdk <= it.maxSdk }
}

/** What the row says on its right-hand side. */
fun permissionStateLabel(granted: Boolean, route: GrantRoute): String = when {
    granted -> "Granted"
    route == GrantRoute.INSTALL_TIME -> "Unavailable"
    else -> "Not granted"
}
