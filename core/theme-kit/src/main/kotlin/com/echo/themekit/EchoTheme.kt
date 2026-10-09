package com.echo.themekit

import java.io.File
import java.io.OutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EchoThemeManifest(
    val manifest: String = MANIFEST_TYPE,
    val schemaVersion: Int = SCHEMA_VERSION,
    val name: String,

    val accentColor: String,

    val iconColor: String = ICON_COLOR_AUTO,

    val textColor: String = ICON_COLOR_AUTO,
    val waveStyle: String = WAVE_ANIMATED,

    val layout: CrossbarLayoutSpec? = null,
    val source: EchoThemeSource? = null,

    val created: String? = null,

    // parts a theme may set (owner, 2026-10-07); null leaves the person's own setting as it is
    val waveDesign: String? = null,
    val gameBootStyle: String? = null,
    val launchDiscStyle: String? = null,
    val buttonSet: String? = null,
    // a FocusStyle and a MotionPreset by name (owner, 2026-10-09)
    val focusStyle: String? = null,
    val motion: String? = null,

    // the look settings in ThemeSettings.KEYS (owner, 2026-10-09); anything else is ignored
    val settings: JsonObject? = null,
) {
    companion object {
        // owner, 2026-10-07: the format is .echo-theme. A bundle marked pfptheme, from before the rename,
        // still reads; it is written back as echo-theme
        const val MANIFEST_TYPE = "echo-theme"
        const val LEGACY_MANIFEST_TYPE = "pfptheme"

        fun isThemeManifest(type: String): Boolean = type == MANIFEST_TYPE || type == LEGACY_MANIFEST_TYPE

        const val SCHEMA_VERSION = 3
        const val ICON_COLOR_AUTO = "auto"
        const val WAVE_ANIMATED = "animated"
        const val WAVE_STATIC = "static"
        const val WAVE_REDUCED = "reduced"

        // the values each part takes. They mirror enums theme-kit cannot see; a test beside each enum
        // fails when they drift: WaveDesign (core-ui), GameBootStyle (core-data), ControllerDisplayType
        // (core-domain, the four that are button sets)
        val WAVE_DESIGNS = setOf("PSP", "ECHO_RINGS", "ECHO_ARCS")
        val GAME_START_STYLES = setOf("DISC", "LENS")
        val BUTTON_SETS = setOf("GENERIC", "XBOX", "NINTENDO", "PLAYSTATION")
        val FOCUS_STYLES = FocusStyle.entries.map { it.name }.toSet()
        val MOTION_PRESETS = MotionPreset.entries.map { it.name }.toSet()
    }
}

@Serializable
data class EchoThemeSource(
    val type: String,
    val file: String? = null,
    val firmware: String? = null,
) {
    companion object {
        const val TYPE_USER_CREATED = "user-created"
    }
}

class ThemeMotion internal constructor(

    val extension: String,
    private val copy: (OutputStream) -> Long,
) {
    fun copyTo(out: OutputStream): Long = copy(out)

    override fun equals(other: Any?): Boolean = other is ThemeMotion && extension == other.extension

    override fun hashCode(): Int = extension.hashCode()

    override fun toString(): String = "ThemeMotion($extension)"

    companion object {
        fun ofFile(file: File, extension: String = file.extension.lowercase()): ThemeMotion =
            ThemeMotion(extension) { out -> file.inputStream().use { it.copyTo(out) } }

        fun ofBytes(bytes: ByteArray, extension: String): ThemeMotion =
            ThemeMotion(extension) { out -> out.write(bytes); bytes.size.toLong() }
    }
}

data class ThemeImage(
    val bytes: ByteArray,

    val extension: String,
) {
    override fun equals(other: Any?): Boolean =
        other is ThemeImage && extension == other.extension && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + extension.hashCode()
}

data class EchoThemeBundle(
    val manifest: EchoThemeManifest,

    val wallpaper: ByteArray?,

    val preview: ByteArray?,

    val icons: Map<String, ThemeImage> = emptyMap(),

    val sysicons: Map<String, ThemeImage> = emptyMap(),

    val motion: ThemeMotion? = null,

    // sounds, boot and game-start media by ECHO's media slot key (ThemeMedia.FOLDERS)
    val media: Map<String, ThemeImage> = emptyMap(),

    // for the theme store (owner, 2026-10-07): the picture that heads its page, screenshots by file name,
    // and README.md, whose front matter is the theme's metadata (ThemeReadme)
    val hero: ThemeImage? = null,
    val screenshots: Map<String, ThemeImage> = emptyMap(),
    val readme: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is EchoThemeBundle &&
            hero == other.hero &&
            screenshots == other.screenshots &&
            readme == other.readme &&
            media == other.media &&
            manifest == other.manifest &&
            wallpaper.contentEquals(other.wallpaper) &&
            preview.contentEquals(other.preview) &&
            icons.keys == other.icons.keys &&
            icons.all { (key, image) -> image == other.icons[key] } &&
            sysicons.keys == other.sysicons.keys &&
            sysicons.all { (key, image) -> image == other.sysicons[key] } &&
            motion == other.motion

    override fun hashCode(): Int {
        var h = 31 * (31 * manifest.hashCode() + wallpaper.contentHashCode()) + preview.contentHashCode()
        for ((key, image) in icons) h = 31 * h + (key.hashCode() xor image.hashCode())
        for ((key, image) in sysicons) h = 31 * h + (key.hashCode() xor image.hashCode())
        motion?.let { h = 31 * h + it.hashCode() }
        for ((key, file) in media) h = 31 * h + (key.hashCode() xor file.hashCode())
        h = 31 * h + (hero?.hashCode() ?: 0) + screenshots.hashCode() + (readme?.hashCode() ?: 0)
        return h
    }
}
