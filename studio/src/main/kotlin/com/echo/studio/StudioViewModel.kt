package com.echo.studio

import androidx.compose.ui.graphics.ImageBitmap
import com.echo.studio.io.ImageCodecs
import com.echo.studio.io.ColorHex
import com.echo.studio.io.VideoCodecs
import com.echo.themekit.IconGifSupport
import com.echo.themekit.IconSlots
import com.echo.themekit.MotionLimits
import com.echo.themekit.CrossbarLayoutSpecCodec
import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.EchoThemeSource
import com.echo.themekit.ThemeImage
import com.echo.themekit.ThemeMotion
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface IconColorChoice {
    data object Auto : IconColorChoice
    data class Custom(val argb: Int) : IconColorChoice
}

sealed interface TextColorChoice {
    data object Auto : TextColorChoice
    data class Custom(val argb: Int) : TextColorChoice
}

enum class PreviewMode(val label: String) {
    HOME("Home"),
    CONTEXT_MENU("Menu"),
    FULLSCREEN_MENU("Fullscreen"),
}

enum class WallpaperPreset(val label: String, val width: Int, val height: Int) {
    PSP("PSP (480×272)", 480, 272),
    HD("HD (1280×720)", 1280, 720),
    FULL_HD("Full HD (1920×1080)", 1920, 1080),
    ORIGINAL("Keep original", 0, 0),
}

data class PendingWallpaper(
    val source: java.awt.image.BufferedImage,
    val fileName: String,
    val thumbnail: androidx.compose.ui.graphics.ImageBitmap?,
)

sealed interface StudioDialog {
    data class Error(val message: String) : StudioDialog

    data class Notice(val title: String, val message: String) : StudioDialog
}

data class StudioState(
    val name: String = "Untitled Theme",
    val accentArgb: Int = ColorHex.DEFAULT_ACCENT,
    val iconColor: IconColorChoice = IconColorChoice.Auto,
    val textColor: TextColorChoice = TextColorChoice.Auto,
    val waveStyle: String = EchoThemeManifest.WAVE_ANIMATED,
    val wallpaperPng: ByteArray? = null,
    val wallpaperBitmap: ImageBitmap? = null,
    val wallpaperFileName: String? = null,

    val wallpaperBusy: Boolean = false,

    val pendingWallpaper: PendingWallpaper? = null,

    val motionFile: File? = null,

    val motionFileName: String? = null,

    val layout: com.echo.themekit.CrossbarLayoutSpec = com.echo.themekit.CrossbarLayoutSpec.DEFAULT,

    val previewMode: PreviewMode = PreviewMode.HOME,

    val iconOverrides: Map<String, ByteArray> = emptyMap(),

    val iconExtensions: Map<String, String> = emptyMap(),

    val iconBitmaps: Map<String, ImageBitmap> = emptyMap(),
    val source: EchoThemeSource? = null,
    // parts Studio does not edit, kept as opened so an export does not drop them (owner, 2026-10-07): the
    // sounds and boot and game-start media, the console icons, and the wave design, game-start styles and
    // button set
    val keptMedia: Map<String, com.echo.themekit.ThemeImage> = emptyMap(),
    val keptSysicons: Map<String, com.echo.themekit.ThemeImage> = emptyMap(),
    val keptManifest: EchoThemeManifest? = null,
    val busy: Boolean = false,
    val statusMessage: String? = null,
    val dialog: StudioDialog? = null,

) {
}

class StudioViewModel(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private var pendingMotion: File? = null
    private var pendingMotionName: String? = null

    private fun scratchMotion(source: File): File {
        val scratch = File.createTempFile("studio-motion-", ".${source.extension.lowercase()}")
        source.copyTo(scratch, overwrite = true)
        scratch.deleteOnExit()
        return scratch
    }

    private fun discardMotion(state: StudioState) {
        state.motionFile?.delete()
    }

    internal fun clearMotion() {
        discardMotion(_state.value)
        _state.update { it.copy(motionFile = null, motionFileName = null) }
    }

    fun newTheme() {
        abandonPendingMotion()
        discardMotion(_state.value)
        _state.update { StudioState() }
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun setAccent(argb: Int) = _state.update { it.copy(accentArgb = argb) }
    fun setIconColor(choice: IconColorChoice) = _state.update { it.copy(iconColor = choice) }
    fun setTextColor(choice: TextColorChoice) = _state.update { it.copy(textColor = choice) }
    fun setWaveStyle(style: String) = _state.update { it.copy(waveStyle = style) }
    fun setPreviewMode(mode: PreviewMode) = _state.update { it.copy(previewMode = mode) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    fun setBarTopFraction(fraction: Float) = _state.update {
        it.copy(
            layout = it.layout.copy(
                barTopFraction = fraction.coerceIn(
                    CrossbarLayoutSpecCodec.BAR_TOP_MIN,
                    CrossbarLayoutSpecCodec.BAR_TOP_MAX,
                ),
            ),
        )
    }

    fun resetLayout() = _state.update { it.copy(layout = com.echo.themekit.CrossbarLayoutSpec.DEFAULT) }

    fun detectBarTop() = runBusy {
        val png = _state.value.wallpaperPng ?: return@runBusy
        val image = ImageCodecs.decodeImage(png) ?: return@runBusy

        val detected = com.echo.themekit.CrossBandDetector.detectBarTopFraction(
            ImageCodecs.toArgbImage(image),
        )
        if (detected != null) {
            _state.update {
                it.copy(
                    layout = it.layout.copy(barTopFraction = detected),
                    statusMessage = "Crossbar detected at ${(detected * 100).toInt()}% of the wallpaper",
                )
            }
        } else {
            _state.update { it.copy(statusMessage = "No crossbar band found in this wallpaper") }
        }
    }

    fun openFile(file: File) {
        when (file.extension.lowercase()) {
            in EchoThemeCodec.READABLE_EXTENSIONS -> openEchoTheme(file)
            else -> _state.update { it.copy(dialog = StudioDialog.Error("Unsupported file type: .${file.extension}")) }
        }
    }

    private fun openEchoTheme(file: File) = runBusy {
        val bundle = EchoThemeCodec.read(file)
        if (bundle == null) {
            _state.update { it.copy(dialog = StudioDialog.Error("${file.name} is not a valid ECHO theme")) }
        } else {
            hydrate(bundle, "Opened ${file.name}")
        }
    }

    private fun hydrate(bundle: EchoThemeBundle, status: String) {
        val manifest = bundle.manifest
        val iconBitmaps = bundle.icons.mapNotNull { (key, png) ->
            ImageCodecs.toImageBitmap(png.bytes)?.let { key to it }
        }.toMap()
        val wallpaperBusy = bundle.wallpaper
            ?.let(ImageCodecs::decodeImage)
            ?.let { com.echo.themekit.WallpaperMetrics.isBusy(ImageCodecs.toArgbImage(it)) }
            ?: false

        abandonPendingMotion()
        discardMotion(_state.value)
        var motionSpillError: String? = null
        val motionFile = bundle.motion?.let { motion ->
            runCatching {
                val scratch = File.createTempFile("studio-motion-", ".${motion.extension}")
                scratch.outputStream().use { motion.copyTo(it) }
                scratch.deleteOnExit()
                scratch
            }.onFailure { e -> motionSpillError = e.message }.getOrNull()
        }
        _state.update {
            StudioState(
                name = manifest.name,
                accentArgb = ColorHex.parseHexRgb(manifest.accentColor) ?: ColorHex.DEFAULT_ACCENT,
                iconColor = manifest.iconColor
                    .takeIf { c -> c != EchoThemeManifest.ICON_COLOR_AUTO }
                    ?.let { c -> ColorHex.parseHexRgb(c) }
                    ?.let { argb -> IconColorChoice.Custom(argb) }
                    ?: IconColorChoice.Auto,
                textColor = manifest.textColor
                    .takeIf { c -> c != EchoThemeManifest.ICON_COLOR_AUTO }
                    ?.let { c -> ColorHex.parseHexRgb(c) }
                    ?.let { argb -> TextColorChoice.Custom(argb) }
                    ?: TextColorChoice.Auto,
                waveStyle = manifest.waveStyle,
                wallpaperPng = bundle.wallpaper,
                wallpaperBitmap = bundle.wallpaper?.let(ImageCodecs::toImageBitmap),
                wallpaperFileName = manifest.source?.file,

                iconOverrides = bundle.icons.mapValues { (_, image) -> image.bytes },
                iconExtensions = bundle.icons.mapValues { (_, image) -> image.extension.lowercase() },
                iconBitmaps = iconBitmaps,
                wallpaperBusy = wallpaperBusy,
                motionFile = motionFile,

                motionFileName = motionFile?.let { "motion.${it.extension}" },
                layout = manifest.layout?.let(CrossbarLayoutSpecCodec::sanitize)
                    ?: com.echo.themekit.CrossbarLayoutSpec.DEFAULT,
                source = manifest.source,
                keptMedia = bundle.media,
                keptSysicons = bundle.sysicons,
                keptManifest = manifest,
                statusMessage = status,
            )
        }

        motionSpillError?.let { message ->
            _state.update {
                it.copy(dialog = StudioDialog.Notice("Motion wallpaper not kept", message))
            }
        }
    }

    private fun abandonPendingMotion() {
        pendingMotion?.delete()
        pendingMotion = null
        pendingMotionName = null
    }

    private val MOTION_PICK_EXTENSIONS = setOf("mp4", "m4v", "webm")

    fun onWallpaperPicked(file: File) {
        if (file.extension.lowercase() in MOTION_PICK_EXTENSIONS) importVideo(file) else stageWallpaper(file)
    }

    fun stageWallpaper(file: File) = runBusy {
        val image = ImageCodecs.loadImage(file)
        if (image == null) {
            _state.update { it.copy(dialog = StudioDialog.Error("${file.name} is not a readable image")) }
            return@runBusy
        }
        abandonPendingMotion()
        stage(image, file.name)
    }

    fun restageEmbeddedWallpaper() = runBusy {
        val current = _state.value
        val image = current.wallpaperPng?.let(ImageCodecs::decodeImage) ?: return@runBusy
        abandonPendingMotion()
        stage(image, current.wallpaperFileName ?: "wallpaper")
    }

    fun importVideo(file: File) = runBusy {
        when (val outcome = VideoCodecs.accept(file)) {
            is VideoCodecs.Outcome.Rejected ->

                _state.update { it.copy(dialog = StudioDialog.Error(outcome.message)) }

            is VideoCodecs.Outcome.Accepted -> {
                val scratch = scratchMotion(file)

                discardMotion(_state.value)
                pendingMotion?.delete()
                pendingMotion = scratch
                pendingMotionName = file.name
                stage(outcome.poster, file.name)
                _state.update { it.copy(motionFile = null, motionFileName = null) }
            }
        }
    }

    private fun stage(image: java.awt.image.BufferedImage, name: String) {
        _state.update {
            it.copy(
                pendingWallpaper = PendingWallpaper(
                    source = image,
                    fileName = name,
                    thumbnail = ImageCodecs.toImageBitmap(ImageCodecs.toPngBytes(ImageCodecs.thumbnail(image, 320))),
                ),
            )
        }
    }

    fun cancelWallpaperImport() {
        abandonPendingMotion()
        _state.update { it.copy(pendingWallpaper = null) }
    }

    fun confirmWallpaper(preset: WallpaperPreset) = runBusy {
        val pending = _state.value.pendingWallpaper ?: return@runBusy
        val image = if (preset == WallpaperPreset.ORIGINAL) pending.source
        else ImageCodecs.centerCropScale(pending.source, preset.width, preset.height)
        val png = ImageCodecs.toPngBytes(image)
        val bitmap = ImageCodecs.toImageBitmap(png)
        val bmp = ImageCodecs.toArgbImage(image)

        val derived = com.echo.themekit.AccentDeriver.deriveAccent(bmp)

        val stagedVideo = pendingMotion
        val stagedName = pendingMotionName
        pendingMotion = null
        pendingMotionName = null
        _state.update {
            it.copy(
                pendingWallpaper = null,
                wallpaperPng = png,
                wallpaperBitmap = bitmap,
                wallpaperFileName = pending.fileName,
                wallpaperBusy = com.echo.themekit.WallpaperMetrics.isBusy(bmp),
                motionFile = stagedVideo ?: it.motionFile,
                motionFileName = stagedName ?: it.motionFileName,
                accentArgb = derived ?: it.accentArgb,
                statusMessage = "Wallpaper: ${pending.fileName} (${image.width}×${image.height})",
            )
        }
    }

    fun clearWallpaper() {
        discardMotion(_state.value)
        _state.update {
            it.copy(
                wallpaperPng = null,
                wallpaperBitmap = null,
                wallpaperFileName = null,
                wallpaperBusy = false,
                motionFile = null,
                motionFileName = null,
            )
        }
    }

    fun setIconOverride(key: String, file: File) = runBusy {
        val slot = IconSlots.byKey(key) ?: return@runBusy

        val bytes = com.echo.studio.io.SafeIo.readBytesCapped(file)
        val decoded = bytes?.let(ImageCodecs::decodeImage)
        if (bytes == null || decoded == null) {
            _state.update { it.copy(dialog = StudioDialog.Error("${file.name} is not a readable image")) }
            return@runBusy
        }

        if (bytes.size > EchoThemeCodec.MAX_ICON_BYTES) {
            _state.update { it.copy(dialog = StudioDialog.Error(IconGifSupport.MSG_TOO_LARGE_BYTES)) }
            return@runBusy
        }
        val frames = IconGifSupport.countFrames(bytes)
        if (IconGifSupport.isGif(bytes) && frames > 1) {
            val (width, height) = IconGifSupport.logicalScreenSize(bytes) ?: (decoded.width to decoded.height)
            IconGifSupport.validateAnimated(width, height, frames, IconGifSupport.durationMs(bytes))
                ?.let { rejection ->
                    _state.update { it.copy(dialog = StudioDialog.Error(rejection)) }
                    return@runBusy
                }
            val gifBitmap = ImageCodecs.toImageBitmap(bytes)
            if (gifBitmap == null) {
                _state.update { it.copy(dialog = StudioDialog.Error(IconGifSupport.MSG_UNDECODABLE)) }
                return@runBusy
            }
            _state.update {
                it.copy(
                    iconOverrides = it.iconOverrides + (key to bytes),
                    iconExtensions = it.iconExtensions + (key to "gif"),
                    iconBitmaps = it.iconBitmaps + (key to gifBitmap),
                )
            }
            return@runBusy
        }

        val png = ImageCodecs.normalizeIconPng(file, slot.templateSizePx)
        val bitmap = png?.let(ImageCodecs::toImageBitmap)
        if (png == null || bitmap == null) {
            _state.update { it.copy(dialog = StudioDialog.Error("${file.name} is not a readable image")) }
            return@runBusy
        }
        _state.update {
            it.copy(
                iconOverrides = it.iconOverrides + (key to png),
                iconExtensions = it.iconExtensions + (key to "png"),
                iconBitmaps = it.iconBitmaps + (key to bitmap),
            )
        }
    }

    fun clearIconOverride(key: String) = _state.update {
        it.copy(
            iconOverrides = it.iconOverrides - key,
            iconExtensions = it.iconExtensions - key,
            iconBitmaps = it.iconBitmaps - key,
        )
    }

    fun clearAllIconOverrides() = _state.update {
        it.copy(iconOverrides = emptyMap(), iconExtensions = emptyMap(), iconBitmaps = emptyMap())
    }

    fun buildManifest(state: StudioState = _state.value, today: LocalDate = LocalDate.now()): EchoThemeManifest =
        EchoThemeManifest(
            name = state.name.ifBlank { "Untitled Theme" },
            accentColor = ColorHex.toHexRgb(state.accentArgb),
            iconColor = when (val c = state.iconColor) {
                IconColorChoice.Auto -> EchoThemeManifest.ICON_COLOR_AUTO
                is IconColorChoice.Custom -> ColorHex.toHexRgb(c.argb)
            },
            textColor = when (val c = state.textColor) {
                TextColorChoice.Auto -> EchoThemeManifest.ICON_COLOR_AUTO
                is TextColorChoice.Custom -> ColorHex.toHexRgb(c.argb)
            },
            waveStyle = state.waveStyle,

            layout = state.layout.takeUnless { it == com.echo.themekit.CrossbarLayoutSpec.DEFAULT },
            source = state.source ?: EchoThemeSource(type = EchoThemeSource.TYPE_USER_CREATED),
            created = today.toString(),
            waveDesign = state.keptManifest?.waveDesign,
            gameBootStyle = state.keptManifest?.gameBootStyle,
            launchDiscStyle = state.keptManifest?.launchDiscStyle,
            buttonSet = state.keptManifest?.buttonSet,
        )

    fun exportTo(file: File, renderPreview: suspend (StudioState) -> ByteArray?) = runBusy {
        val snapshot = _state.value

        val motion = snapshot.motionFile
            ?.takeIf { snapshot.wallpaperPng != null && it.isFile }
            ?.let { video ->

                MotionLimits.bundleExtensionFor(video.extension)?.let { ThemeMotion.ofFile(video, it) }
            }
        val bundle = EchoThemeBundle(
            manifest = buildManifest(snapshot),
            wallpaper = snapshot.wallpaperPng,
            preview = runCatching { renderPreview(snapshot) }.getOrNull(),

            icons = snapshot.iconOverrides.mapValues { (key, png) ->
                ThemeImage(png, snapshot.iconExtensions[key] ?: "png")
            },
            motion = motion,
            sysicons = snapshot.keptSysicons,
            media = snapshot.keptMedia,
        )
        runCatching { file.outputStream().use { EchoThemeCodec.write(bundle, it) } }
            .onSuccess { _state.update { it.copy(statusMessage = "Exported ${file.name}") } }
            .onFailure { e -> _state.update { it.copy(dialog = StudioDialog.Error("Export failed: ${e.message}")) } }
    }

    fun exportIconTemplates(dir: File, rasterize: (key: String, sizePx: Int) -> ByteArray) = runBusy {
        runCatching {
            dir.mkdirs()
            for (slot in IconSlots.ALL) {
                File(dir, "${slot.key}.png").writeBytes(rasterize(slot.key, slot.templateSizePx))
            }
        }
            .onSuccess { _state.update { it.copy(statusMessage = "Templates exported to ${dir.name} (${IconSlots.ALL.size} icons)") } }
            .onFailure { e -> _state.update { it.copy(dialog = StudioDialog.Error("Template export failed: ${e.message}")) } }
    }

    internal fun runBusy(block: suspend () -> Unit) {
        scope.launch {
            _state.update { it.copy(busy = true) }
            try {
                withContext(Dispatchers.IO) { block() }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    internal fun update(transform: (StudioState) -> StudioState) = _state.update(transform)
}
