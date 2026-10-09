package com.echo.themekit

// the Template theme ECHO writes to ECHO/Themes/Template (owner, 2026-10-07): every folder a theme can have,
// each with a README naming the files it takes, and a theme.json with every key. It is built from the same
// lists the codec checks against, so a file it names is a file a theme keeps. Copy the folder, rename it and
// fill it; ECHO never reads Template itself.
object EchoThemeTemplate {
    fun files(): Map<String, ByteArray> = linkedMapOf(
        EchoThemeFolder.MANIFEST to manifestJson(),
        "README.txt" to README.trimIndent(),
        EchoThemeFolder.README to STORE_README.trimIndent(),
        "Preview/README.txt" to PREVIEW.trimIndent(),
        "Icons/README.txt" to icons(),
        "Icons/Consoles/README.txt" to consoles(),
        "Wallpaper/README.txt" to WALLPAPER.trimIndent(),
        "Fonts/README.txt" to FONTS.trimIndent(),
        "${ThemeMedia.SOUNDS}/README.txt" to media(ThemeMedia.SOUNDS, "Interface sounds and menu music."),
        "${ThemeMedia.BOOT}/README.txt" to media(ThemeMedia.BOOT, "The boot animation and its sound."),
        "${ThemeMedia.GAME_START}/README.txt" to media(ThemeMedia.GAME_START, "What plays when a game starts: GameBoot and the launch disc."),
    ).mapValues { (_, text) -> (text.trimEnd() + "\n").toByteArray() }

    private fun manifestJson(): String =
        EchoThemeFolder.toFiles(
            EchoThemeBundle(EchoThemeManifest(name = "Template", accentColor = "#3B82F6"), wallpaper = null, preview = null),
        ).getValue(EchoThemeFolder.MANIFEST).decodeToString()

    private fun icons(): String =
        "Icons named after their slot, as .png or .gif (up to 8 MB each). A slot with no file keeps ECHO's own icon.\n\n" +
            IconSlots.ALL.joinToString("\n") { "${it.key}.png    ${it.displayName}" }

    private fun consoles(): String =
        "Console icons named after the console, as .png or .gif.\n\n" + SYSICON_PLATFORM_IDS.joinToString("\n") { "$it.png" }

    private fun media(folder: String, what: String): String =
        "$what Name each file after its slot, as ${ThemeMedia.EXTENSIONS.sorted().joinToString(", ") { ".$it" }}. " +
            "A slot with no file keeps the sound or animation you have.\n\n" +
            ThemeMedia.FOLDERS.filterValues { it == folder }.keys.joinToString("\n")

    private val README = """
        Template: an example ECHO theme. Copy this folder, rename the copy to your theme's name, and put your
        files in it. ECHO reads the copy the next time it starts, or when you choose Reload ECHO Folder.
        ECHO never reads this Template folder itself.

        theme.json   accentColor, iconColor and textColor are colours like #3B82F6 ("auto" lets ECHO pick).
                     waveStyle is animated, static or reduced.
                     waveDesign is ${EchoThemeManifest.WAVE_DESIGNS.joinToString(", ")}.
                     gameBootStyle and launchDiscStyle are ${EchoThemeManifest.GAME_START_STYLES.joinToString(" or ")}.
                     buttonSet is ${EchoThemeManifest.BUTTON_SETS.joinToString(", ")}.
                     focusStyle is ${EchoThemeManifest.FOCUS_STYLES.joinToString(", ")}.
                     motion is ${EchoThemeManifest.MOTION_PRESETS.joinToString(", ")}.
                     settings holds look settings by name: ${ThemeSettings.KEYS.keys.joinToString(", ")}.
                     Each is true or false; display_icon_legibility is a name such as CONTOUR_AUTO.
                     Leave a part as null and the theme keeps the setting the person has.
                     The theme's name is its folder's name.
        Icons/       Menu and crossbar icons. Console icons go in Icons/Consoles.
        Wallpaper/   wallpaper.png, and motion.mp4, motion.webm or motion.gif for a moving one.
        Fonts/       One font, .ttf or .otf, that ECHO draws all its text with.
        Sounds/      Interface sounds and menu music.
        Boot/        The boot animation and its sound.
        GameStart/   GameBoot and the launch disc.
        README.md    The theme's page in ECHO's theme store. The lines between the two --- lines
                     are its details (author, version, description, tags); the text after them is
                     shown on the page.
        Preview/     hero.jpg heads the theme's card and page; Screenshots/ holds up to
                     ${EchoThemeCodec.MAX_SCREENSHOTS} pictures for its page.

        Every part is optional: a theme with only Sounds is a sound pack.
    """

    private val STORE_README = """
        ---
        author: Your name
        version: 1.0
        description: One line, shown on the theme's card and at the top of its page.
        tags: dark, calm
        ---

        What your theme is, shown on its page in ECHO's theme store.
    """

    private val PREVIEW = """
        hero.jpg       the picture on the theme's card and at the top of its page (or hero.png, hero.webp)
        Screenshots/   up to ${EchoThemeCodec.MAX_SCREENSHOTS} pictures (.jpg, .png, .webp) for its page, shown in name order
    """

    private val FONTS = """
        One font file, ${EchoThemeCodec.FONT_EXTENSIONS.sorted().joinToString(" or ") { ".$it" }}, up to ${EchoThemeCodec.MAX_FONT_BYTES / (1024 * 1024)} MB, named as you like.
        ECHO draws all its text with it, in place of a font in the ECHO folder's Look/Fonts.
        A font Android cannot read is skipped and ECHO keeps its own.
    """

    private val WALLPAPER = """
        wallpaper.png   the still wallpaper
        motion.mp4      a moving wallpaper (or motion.webm, motion.gif)
        preview.png     the picture shown for the theme in ECHO's list (optional)
    """
}
