package com.echo.core.domain.model

data class Platform(
    val id: String,
    val name: String,
    val shortName: String,
    val iconRes: String?,
    val accentColor: Long,
    val isPinnedToBar: Boolean = false,
    val barPosition: Int = -1,
    val preferredEmulatorPackage: String? = null,
    val romExtensions: List<String> = emptyList(),
)

object PlatformIds {
    const val WINDOWS = "windows"

    const val ANDROID = "android"

    const val APP_SHORTCUT = "app_shortcut"
}

fun platformLabel(platformId: String, storedName: String?): String = when {
    platformId == PlatformIds.APP_SHORTCUT -> "Android app"
    !storedName.isNullOrBlank() -> storedName
    else -> platformId.uppercase()
}
