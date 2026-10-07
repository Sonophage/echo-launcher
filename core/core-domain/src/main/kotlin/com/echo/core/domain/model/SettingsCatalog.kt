package com.echo.core.domain.model

enum class SettingsSectionId(

    val id: String,
    val title: String,

    val subtitle: String,
) {
    // owner, 2026-10-05: media folders get their own section, and Hidden Items moves here from Emulators
    LIBRARY("settings_section_library", "Library", "Media folders & hidden items"),
    EMULATORS("settings_section_emulators", "Emulators", "Library Manager, emulator profiles, RetroArch & your art"),
    // owner, 2026-10-06: Look & Feel splits in two, how ECHO looks and how you drive it; Overview is a Profile tab
    LOOK("settings_section_look_and_feel", "Look", "Theme, wallpaper, the crossbar, boot & sound"),
    CONTROLS("settings_section_controls", "Controls", "Controller & touch"),
    // owner, 2026-10-05: every account lives here, the artwork services' too, each kind on its own tab
    ACCOUNTS("settings_section_accounts", "Accounts", "Permissions, achievements, artwork services & Discord"),
    SYSTEM("settings_section_system", "System", "About, logs, backup, performance & credits"),
    SETUP("settings_section_setup", "Setup", "The guided setup wizard"),
}

data class SettingsEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: SettingsSectionId,
    // a screen opened from a row on this tab, not a tab of its own; Back returns to it
    val parent: String? = null,
)

val SETTINGS_CATALOG: List<SettingsEntry> = listOf(

    SettingsEntry("settings_media_libraries", "Media Libraries", "Your music, video, photo & book folders", SettingsSectionId.LIBRARY),
    SettingsEntry("settings_app_visibility", "Hidden Items", "Apps & games hidden everywhere or from one place", SettingsSectionId.LIBRARY),

    SettingsEntry("settings_library", "Library Manager", "ROM sources & scanning", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_artwork", "Artwork", "Your art, scraping & cache", SettingsSectionId.EMULATORS),

    SettingsEntry("settings_emulators_installed", "Installed", "Detected emulator profiles", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_emulators_custom", "Custom Emulators", "Custom profiles & Add Custom Emulator", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_emulators_retroarch", "RetroArch", "Core detection & linking", SettingsSectionId.EMULATORS),

    SettingsEntry("settings_themes", "Theme", "Colour scheme, accent & theme packs", SettingsSectionId.LOOK),
    // owner, 2026-10-07: each part of the look from any theme; opens from Theme
    SettingsEntry("settings_theme_mix", "Mix", "Each part of the look from any theme", SettingsSectionId.LOOK, parent = "settings_themes"),
    SettingsEntry("settings_appearance", "Wallpaper", "Wallpaper, wave & background motion", SettingsSectionId.LOOK),
    // owner, 2026-10-07: every crossbar setting in one tab; its categories open from there
    SettingsEntry("settings_layout", "Crossbar", "Sizes, rows, Last Played, status bar, categories & icons", SettingsSectionId.LOOK),
    SettingsEntry("settings_boot", "Boot", "Boot sequence, boot video & GameBoot", SettingsSectionId.LOOK),

    SettingsEntry("settings_audio", "Sound", "Menu sounds, menu music & boot audio", SettingsSectionId.LOOK),
    SettingsEntry("settings_categories", "Categories", "Crossbar categories & the collections inside them", SettingsSectionId.LOOK, parent = "settings_layout"),

    SettingsEntry("settings_controller", "Controller", "Button swaps, prompts, stick, triggers & scrolling", SettingsSectionId.CONTROLS),
    SettingsEntry("settings_touch", "Touch", "On-screen button, hints & the video player", SettingsSectionId.CONTROLS),

    SettingsEntry("settings_permissions", "Permissions", "What ECHO can reach, and how to grant it", SettingsSectionId.ACCOUNTS),
    SettingsEntry("settings_accounts", "Achievements", "RetroAchievements & Steam", SettingsSectionId.ACCOUNTS),
    SettingsEntry("settings_artwork_sources", "Artwork", "SteamGridDB, ScreenScraper, IGDB & TMDB, and their order", SettingsSectionId.ACCOUNTS),
    SettingsEntry("settings_discord", "Discord", "Sign in, friends & presence", SettingsSectionId.ACCOUNTS),

    SettingsEntry("settings_about", "About", "ECHO", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_logs", "Logs", "Debug & error log viewer", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_backup", "Backup & Restore", "Export & import", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_performance", "Performance", "Thermal, battery saver & rescanning", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_credits", "Credits", "Artwork & attributions", SettingsSectionId.SYSTEM),

    SettingsEntry("settings_initial_setup", "Setup Wizard", "Guided folder & account setup", SettingsSectionId.SETUP),
)

const val SETTINGS_ROOT_SCREEN_ID = "settings_root"

// the section's tabs; a sub-screen is not one
fun settingsEntriesIn(section: SettingsSectionId): List<SettingsEntry> =
    SETTINGS_CATALOG.filter { it.section == section && it.parent == null }

fun settingsEntryFor(screenId: String): SettingsEntry? =
    SETTINGS_CATALOG.firstOrNull { it.id == screenId }

fun settingsSectionFor(screenId: String): SettingsSectionId? = settingsEntryFor(screenId)?.section

fun settingsRailRows(screenId: String?): List<SettingsEntry> =
    screenId?.let(::settingsEntryFor)?.takeIf { it.parent == null }?.section?.let(::settingsEntriesIn) ?: emptyList()

fun settingsTabStepTarget(screenId: String?, delta: Int): String? {
    val tabs = settingsRailRows(screenId)
    val at = tabs.indexOfFirst { it.id == screenId }
    if (at < 0) return null
    return tabs[((at + delta) % tabs.size + tabs.size) % tabs.size].id
}
