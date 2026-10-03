package com.psplauncher.core.domain.model

enum class SettingsSectionId(

    val id: String,
    val title: String,

    val subtitle: String,
) {
    OVERVIEW("settings_section_overview", "Overview", "Library, artwork & build"),
    EMULATORS("settings_section_emulators", "Emulators", "Library Manager, emulator profiles, RetroArch, artwork & hidden games"),
    LOOK_AND_FEEL("settings_section_look_and_feel", "Look & Feel", "Theme, wallpaper, layout, sound, controls & touch"),
    SYSTEM("settings_section_system", "System", "Permissions, about, logs, backup & credits"),
    SETUP("settings_section_setup", "Setup", "The guided setup wizard"),
}

data class SettingsEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: SettingsSectionId,
)

val SETTINGS_CATALOG: List<SettingsEntry> = listOf(

    SettingsEntry("settings_overview", "Overview", "Library, artwork & build", SettingsSectionId.OVERVIEW),

    SettingsEntry("settings_library", "Library Manager", "ROM sources & scanning", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_artwork", "Artwork", "Your art, scraping & cache", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_artwork_sources", "Scraping Sources", "Source priority & service accounts", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_app_visibility", "Hidden Items", "Review apps & games you've hidden", SettingsSectionId.EMULATORS),

    SettingsEntry("settings_emulators_installed", "Installed", "Detected emulator profiles", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_emulators_custom", "Custom Emulators", "Custom profiles & Add Custom Emulator", SettingsSectionId.EMULATORS),
    SettingsEntry("settings_emulators_retroarch", "RetroArch", "Core detection & linking", SettingsSectionId.EMULATORS),

    SettingsEntry("settings_themes", "Theme", "Colour scheme, accent & theme packs", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_appearance", "Wallpaper & Text", "Wallpaper, wave, legibility, Last Played & status bar", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_layout", "Layout", "XMB layout & custom icons", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_boot", "Boot", "Boot sequence, boot video & GameBoot", SettingsSectionId.LOOK_AND_FEEL),

    SettingsEntry("settings_audio", "Sound", "Menu sounds, menu music & boot audio", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_categories", "Categories", "XMB categories & the collections inside them", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_controller", "Controller", "Button swaps, prompts, stick, triggers & scrolling", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_touch", "Touch", "On-screen button, hints & the video player", SettingsSectionId.LOOK_AND_FEEL),
    SettingsEntry("settings_performance", "Performance", "Thermal, battery saver & rescanning", SettingsSectionId.LOOK_AND_FEEL),

    SettingsEntry("settings_permissions", "Permissions", "What PSPLauncher can reach, and how to grant it", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_about", "About", "PSPLauncher", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_logs", "Logs", "Debug & error log viewer", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_backup", "Backup & Restore", "Export & import", SettingsSectionId.SYSTEM),
    SettingsEntry("settings_credits", "Credits", "Artwork & attributions", SettingsSectionId.SYSTEM),

    SettingsEntry("settings_initial_setup", "Setup Wizard", "Guided folder & account setup", SettingsSectionId.SETUP),
)

const val SETTINGS_ROOT_SCREEN_ID = "settings_root"

fun settingsEntriesIn(section: SettingsSectionId): List<SettingsEntry> =
    SETTINGS_CATALOG.filter { it.section == section }

fun settingsEntryFor(screenId: String): SettingsEntry? =
    SETTINGS_CATALOG.firstOrNull { it.id == screenId }

fun settingsSectionFor(screenId: String): SettingsSectionId? = settingsEntryFor(screenId)?.section

fun settingsRailRows(screenId: String?): List<SettingsEntry> =
    screenId?.let(::settingsSectionFor)?.let(::settingsEntriesIn) ?: emptyList()

fun settingsSectionStep(section: SettingsSectionId, delta: Int): SettingsSectionId {
    val all = SettingsSectionId.entries
    val next = ((section.ordinal + delta) % all.size + all.size) % all.size
    return all[next]
}

fun settingsSectionStepTarget(screenId: String?, delta: Int): String? {
    val section = screenId?.let(::settingsSectionFor) ?: return null
    return settingsEntriesIn(settingsSectionStep(section, delta)).firstOrNull()?.id
}

fun settingsTabStepTarget(screenId: String?, delta: Int): String? {
    val tabs = settingsRailRows(screenId)
    val at = tabs.indexOfFirst { it.id == screenId }
    if (at < 0) return null
    return tabs[((at + delta) % tabs.size + tabs.size) % tabs.size].id
}
