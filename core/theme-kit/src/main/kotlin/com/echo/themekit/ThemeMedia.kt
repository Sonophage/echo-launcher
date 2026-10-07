package com.echo.themekit

// the media a theme can carry (owner, 2026-10-07): ECHO's media slots by key, and the folder each sits in
// within a theme folder. It mirrors UiMediaSlot in core-domain, which theme-kit cannot see;
// ThemeMediaSlotsTest there fails when the two drift.
object ThemeMedia {
    const val SOUNDS = "Sounds"
    const val BOOT = "Boot"
    const val GAME_START = "GameStart"

    val FOLDERS: Map<String, String> = mapOf(
        "sound_scroll" to SOUNDS,
        "sound_select" to SOUNDS,
        "sound_system_browse" to SOUNDS,
        "sound_back" to SOUNDS,
        "sound_confirm" to SOUNDS,
        "sound_error" to SOUNDS,
        "sound_launch" to SOUNDS,
        "sound_notification" to SOUNDS,
        "menu_music" to SOUNDS,
        "boot_video" to BOOT,
        "boot_audio" to BOOT,
        "launch_disc_audio" to GAME_START,
        "gameboot_audio" to GAME_START,
        "gameboot_video" to GAME_START,
    )

    // the formats ECHO stores media in; UiMediaStore checks each file again when it is applied
    val EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "mp4", "webm")

    fun isMedia(key: String, extension: String): Boolean = key in FOLDERS && extension.lowercase() in EXTENSIONS
}
