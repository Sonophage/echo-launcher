package com.echo.themekit

// the parts a theme can set, each applied on its own from a theme's store page (owner, 2026-10-07)
enum class ThemePart(val label: String) {
    ICONS("Icons"),
    WALLPAPER("Wallpaper"),
    COLOURS("Colours and layout"),
    WAVE("Wave"),
    SOUNDS("Sounds"),
    BOOT("Boot"),
    GAME_START("Game start"),
    BUTTONS("Buttons"),
}

// the parts this theme has. Colours count when it names an accent, an icon or text colour, or a layout
fun EchoThemeBundle.parts(): Set<ThemePart> = buildSet {
    val m = manifest
    if (icons.isNotEmpty() || sysicons.isNotEmpty()) add(ThemePart.ICONS)
    if (wallpaper != null || motion != null) add(ThemePart.WALLPAPER)
    if (m.accentColor.isNotBlank() || m.iconColor != EchoThemeManifest.ICON_COLOR_AUTO ||
        m.textColor != EchoThemeManifest.ICON_COLOR_AUTO || m.layout != null
    ) add(ThemePart.COLOURS)
    if (m.waveDesign != null) add(ThemePart.WAVE)
    if (media.keys.any { ThemeMedia.FOLDERS[it] == ThemeMedia.SOUNDS }) add(ThemePart.SOUNDS)
    if (media.keys.any { ThemeMedia.FOLDERS[it] == ThemeMedia.BOOT }) add(ThemePart.BOOT)
    if (media.keys.any { ThemeMedia.FOLDERS[it] == ThemeMedia.GAME_START } || m.gameBootStyle != null || m.launchDiscStyle != null) {
        add(ThemePart.GAME_START)
    }
    if (m.buttonSet != null) add(ThemePart.BUTTONS)
}

// a theme's README.md: an optional front matter block of "key: value" lines between two "---" lines, which
// is the theme's metadata, then the text shown on its store page
data class ThemeReadme(
    val fields: Map<String, String>,
    val body: String,
) {
    val author: String? get() = fields["author"]
    val version: String? get() = fields["version"]
    val description: String? get() = fields["description"]
    val tags: List<String> get() = fields["tags"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    companion object {
        fun parse(text: String?): ThemeReadme {
            val lines = text.orEmpty().replace("\r\n", "\n").lines()
            if (lines.firstOrNull()?.trim() != "---") return ThemeReadme(emptyMap(), text.orEmpty().trim())
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (end < 0) return ThemeReadme(emptyMap(), text.orEmpty().trim())
            val fields = lines.subList(1, end + 1).mapNotNull { line ->
                line.indexOf(':').takeIf { it > 0 }?.let { i -> line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim() }
            }.filter { it.second.isNotEmpty() }.toMap()
            return ThemeReadme(fields, lines.drop(end + 2).joinToString("\n").trim())
        }
    }
}
