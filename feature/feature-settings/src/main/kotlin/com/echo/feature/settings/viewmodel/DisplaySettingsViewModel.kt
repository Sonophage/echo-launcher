package com.echo.feature.settings.viewmodel

import com.echo.core.ui.wave.WaveDesign
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.ControllerLayoutRepository
import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.data.wallpaper.WallpaperAccentProbe.clearWallpaperAccent
import com.echo.core.data.wallpaper.StillWallpaper
import com.echo.core.domain.model.ControllerHintPolicy
import com.echo.core.domain.model.UiMediaKind
import com.echo.core.domain.model.UiMediaSlot
import com.echo.core.domain.model.IconLegibilityStyle
import com.echo.core.domain.model.resolve
import com.echo.core.domain.model.TouchNavButtonMode
import com.echo.core.domain.model.TouchSensitivity
import com.echo.core.domain.model.XYLayout
import com.echo.core.ui.wave.WaveStyle
import com.echo.themekit.MotionLimits
import com.echo.themekit.UiMediaLimits
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private val KEY_WAVE_STYLE         = stringPreferencesKey("display_wave_style")
private val KEY_WAVE_DESIGN        = stringPreferencesKey("display_wave_design")
private val KEY_SHOW_BOOT          = booleanPreferencesKey("display_show_boot")
private val KEY_BOOT_ON_RESUME     = booleanPreferencesKey("display_boot_on_resume")
private val KEY_THERMAL_AWARE      = booleanPreferencesKey("display_thermal_aware")
private val KEY_RESPECT_BATTERY    = booleanPreferencesKey("display_battery_saver")

private val KEY_WAVE_OVER_WALLPAPER = booleanPreferencesKey("display_wave_over_wallpaper")

private val KEY_TOUCH_NAV_BUTTON   = stringPreferencesKey("interface_touch_nav_button")

internal val BOOT_TAB_SOUNDS = listOf(UiMediaSlot.BOOT_AUDIO, UiMediaSlot.LAUNCH_DISC_AUDIO, UiMediaSlot.GAMEBOOT_AUDIO)
private val KEY_CONTEXT_MENU_HINT_DELAY_SECONDS = floatPreferencesKey("interface_context_menu_hint_delay_seconds")

private val KEY_TOUCH_SENSITIVITY  = stringPreferencesKey("interface_touch_sensitivity")


private val KEY_ICON_LEGIBILITY    = stringPreferencesKey("display_icon_legibility")

private val KEY_FADE_BY_DISTANCE = booleanPreferencesKey("display_fade_by_distance")


private val KEY_RECENTS_INCLUDE_APPS = com.echo.core.data.repository.InterfacePreferences.KEY_RECENTS_INCLUDE_APPS

private val KEY_TEXT_SHADOW = booleanPreferencesKey("display_text_shadow")

internal val KEY_CUSTOM_WALLPAPER  = stringPreferencesKey("display_custom_wallpaper")

internal val KEY_MOTION_WALLPAPER  = stringPreferencesKey("display_motion_wallpaper")

private val SUPPORTED_WALLPAPER_MIME = setOf("image/png", "image/jpeg", "image/webp") + MotionLimits.SUPPORTED_MIME

private val MOTION_WALLPAPER_MIME = MotionLimits.SUPPORTED_MIME

private const val WALLPAPER_SAVE_FAILED = "Couldn't save the wallpaper — try again"

private val TOUCH_NAV_BUTTON_LABELS = mapOf(
    TouchNavButtonMode.AUTO        to "Auto",
    TouchNavButtonMode.ALWAYS_SHOW to "Always Show",
    TouchNavButtonMode.ALWAYS_HIDE to "Always Hide",
)

private fun TouchSensitivity.label(): String = when (this) {
    TouchSensitivity.VERY_LOW -> "Very Low"
    TouchSensitivity.LOW      -> "Low"
    TouchSensitivity.NORMAL   -> "Normal"
    TouchSensitivity.HIGH     -> "High"
}

data class DisplaySettingsUiState(
    val waveStyle: WaveStyle = WaveStyle.ANIMATED,
    val motionStyle: WaveStyle = WaveStyle.ANIMATED,

    val waveDesign: WaveDesign = WaveDesign.PSP,
    val showBootSequence: Boolean = true,
    val showBootOnResume: Boolean = false,
    val thermalThrottleAware: Boolean = true,
    val respectBatterySaver: Boolean = true,

    val waveOverWallpaper: Boolean = false,
    val touchNavButtonMode: TouchNavButtonMode = TouchNavButtonMode.AUTO,

    val iconLegibility: IconLegibilityStyle = IconLegibilityStyle.DEFAULT,

    val fadeByDistance: Boolean = true,
    val gameRowsShowCovers: Boolean = false,
    val recentsIncludeApps: Boolean = false,

    val textShadow: Boolean = true,

    val interfaceChoices: com.echo.core.data.repository.InterfaceChoices =
        com.echo.core.data.repository.InterfaceChoices(),

    val contextMenuHintDelaySeconds: Float = ControllerHintPolicy.DEFAULT_DELAY_SECONDS,
    val touchSensitivity: TouchSensitivity = TouchSensitivity.NORMAL,

    val customWallpaperPath: String? = null,

    val motionWallpaperPath: String? = null,
    val wallpaperMessage: String? = null,
    val buttonHints: com.echo.core.data.repository.ButtonHints = com.echo.core.data.repository.ButtonHints.ALL,
    val wallpaperImporting: Boolean = false,
    val wallpaperPreviewVisible: Boolean = false,

    val bootVideoLabel: String = UI_MEDIA_DEFAULT_LABEL,
    val bootVideoAssigned: Boolean = false,
    // the Boot tab's sounds (boot, launch disc, GameBoot), each beside its animation
    val bootTabSoundLabels: Map<UiMediaSlot, String> = emptyMap(),
    val bootTabSoundsAssigned: Set<UiMediaSlot> = emptySet(),

    val gameBootEnabled: Boolean = true,
    val gameBootStyle: com.echo.core.data.repository.GameBootStyle = com.echo.core.data.repository.GameBootStyle.LENS,
    val launchDiscEnabled: Boolean = true,
    val launchDiscStyle: com.echo.core.data.repository.GameBootStyle = com.echo.core.data.repository.GameBootStyle.LENS,
    val gameBootVideoLabel: String = UI_MEDIA_DEFAULT_LABEL,
    val gameBootVideoAssigned: Boolean = false,

    val xyLayout: XYLayout = XYLayout.STANDARD,

    val classicLayoutApplied: Boolean = false,
    // this screen size's crossbar layout, for Settings ▸ Layout's sliders
    val layoutAdjust: com.echo.themekit.CrossbarLayoutAdjust = com.echo.themekit.CrossbarLayoutAdjust.DEFAULT,
) {
}

const val UI_MEDIA_DEFAULT_LABEL = "Default"

internal const val UI_MEDIA_IMPORT_FAILED = "Couldn't save that file — try again"

internal suspend fun saveThenPrune(save: suspend () -> Unit, prune: suspend () -> Unit): Boolean {
    try {
        save()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        timber.log.Timber.w(e, "Saving a wallpaper or UI media file failed")
        return false
    }
    prune()
    return true
}

@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uiMediaStore: UiMediaStore,
    private val gameBootPreferences: GameBootPreferences,
    private val launchDiscPreferences: com.echo.core.data.launch.LaunchDiscPreferences,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
    private val controllerLayout: ControllerLayoutRepository,
    private val stillWallpaper: StillWallpaper,
    private val motionWallpaper: com.echo.core.data.wallpaper.MotionWallpaper,
    private val bootAudioPreviewer: com.echo.core.ui.media.UiMediaAudioPlayer,

    @com.echo.feature.settings.di.SettingsIoDispatcher
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _wallpaperMessage  = MutableStateFlow<String?>(null)
    private val _wallpaperImporting = MutableStateFlow(false)
    private val _wallpaperPreviewVisible = MutableStateFlow(false)

    private var pendingUiMediaSlot: UiMediaSlot? = null

    val uiState: StateFlow<DisplaySettingsUiState> = combine(
        context.echoDataStore.data,
        _wallpaperMessage,
        _wallpaperImporting,
        _wallpaperPreviewVisible,
        controllerLayout.prefs,
    ) { prefs, msg, importing, previewVisible, layout ->

        val assigned = uiMediaStore.assignments()
        fun label(slot: UiMediaSlot): String = when {
            slot !in assigned -> UI_MEDIA_DEFAULT_LABEL
            else -> prefs[UiMediaStore.displayNameKey(slot)]
                ?: if (slot.kind == UiMediaKind.VIDEO) "Custom video" else "Custom sound"
        }
        DisplaySettingsUiState(
            waveStyle            = runCatching {
                WaveStyle.valueOf(prefs[KEY_WAVE_STYLE] ?: WaveStyle.ANIMATED.name)
            }.getOrDefault(WaveStyle.ANIMATED),
            motionStyle          = com.echo.core.ui.motion.MotionWallpaperPolicy.motionStyleOf(
                prefs[stringPreferencesKey(com.echo.core.ui.motion.MotionWallpaperPolicy.KEY)],
            ),
            waveDesign           = WaveDesign.of(prefs[KEY_WAVE_DESIGN]),
            showBootSequence     = prefs[KEY_SHOW_BOOT]       ?: true,
            showBootOnResume     = prefs[KEY_BOOT_ON_RESUME]  ?: false,
            thermalThrottleAware = prefs[KEY_THERMAL_AWARE]   ?: true,
            respectBatterySaver  = prefs[KEY_RESPECT_BATTERY] ?: true,
            waveOverWallpaper    = prefs[KEY_WAVE_OVER_WALLPAPER] ?: false,
            touchNavButtonMode   = TouchNavButtonMode.fromName(prefs[KEY_TOUCH_NAV_BUTTON]),
            iconLegibility       = IconLegibilityStyle.fromName(prefs[KEY_ICON_LEGIBILITY]),
            fadeByDistance       = prefs[KEY_FADE_BY_DISTANCE] ?: true,
            gameRowsShowCovers   = com.echo.core.data.repository.IconDisplayPreferences.gameRowsShowCovers(prefs),
            recentsIncludeApps   = prefs[KEY_RECENTS_INCLUDE_APPS] ?: false,
            textShadow           = prefs[KEY_TEXT_SHADOW] ?: true,
            interfaceChoices     = com.echo.core.data.repository.InterfacePreferences.read(prefs),
            buttonHints          = com.echo.core.data.repository.InterfacePreferences.buttonHints(prefs),
            contextMenuHintDelaySeconds = ControllerHintPolicy.clampDelay(
                prefs[KEY_CONTEXT_MENU_HINT_DELAY_SECONDS] ?: ControllerHintPolicy.DEFAULT_DELAY_SECONDS
            ),
            touchSensitivity     = TouchSensitivity.fromName(prefs[KEY_TOUCH_SENSITIVITY]),
            customWallpaperPath  = prefs[KEY_CUSTOM_WALLPAPER],
            motionWallpaperPath  = prefs[KEY_MOTION_WALLPAPER],
            wallpaperMessage     = msg,
            wallpaperImporting   = importing,
            wallpaperPreviewVisible = previewVisible,
            bootVideoLabel       = label(UiMediaSlot.BOOT_VIDEO),
            bootVideoAssigned    = UiMediaSlot.BOOT_VIDEO in assigned,
            bootTabSoundLabels   = BOOT_TAB_SOUNDS.associateWith { label(it) },
            bootTabSoundsAssigned = BOOT_TAB_SOUNDS.filterTo(HashSet()) { it in assigned },

            gameBootEnabled      = GameBootPreferences.resolve(prefs),
            gameBootStyle        = GameBootPreferences.styleOf(prefs),
            launchDiscEnabled    = com.echo.core.data.launch.LaunchDiscPreferences.resolve(prefs),
            launchDiscStyle      = com.echo.core.data.launch.LaunchDiscPreferences.styleOf(prefs),
            gameBootVideoLabel   = label(UiMediaSlot.GAMEBOOT_VIDEO),
            gameBootVideoAssigned = UiMediaSlot.GAMEBOOT_VIDEO in assigned,
            xyLayout             = layout.xyLayout,

            classicLayoutApplied     = ClassicCrossbarLayout.isApplied(prefs, ClassicCrossbarLayout.forWindow(context)),
            layoutAdjust             = ClassicCrossbarLayout.current(prefs, ClassicCrossbarLayout.forWindow(context).bucketKey),
        )
    }

        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettingsUiState())

    fun onUiMediaPickerLaunchedFor(slot: UiMediaSlot) {
        pendingUiMediaSlot = slot
    }

    fun onUiMediaPicked(uri: Uri) {
        val slot = pendingUiMediaSlot ?: return
        pendingUiMediaSlot = null
        viewModelScope.launch {
            _wallpaperImporting.value = true
            val result = try {
                uiMediaStore.import(slot, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiMediaStore.ImportResult(false, UI_MEDIA_IMPORT_FAILED)
            } finally {
                _wallpaperImporting.value = false
            }
            if (!result.ok) {
                menuSound.play(com.echo.core.ui.sound.MenuSound.ERROR)
                _wallpaperMessage.value = result.message
            }
        }
    }

    fun clearUiMedia(slot: UiMediaSlot) = viewModelScope.launch { uiMediaStore.clear(slot) }

    // owner, 2026-10-07/08: the boot, launch disc and GameBoot sounds sit on the Boot tab, beside their animations
    fun previewSound(slot: UiMediaSlot) = bootAudioPreviewer.play(slot = slot, customPath = uiMediaStore.pathFor(slot))

    override fun onCleared() {
        bootAudioPreviewer.stop()
    }

    fun setGameBootEnabled(enabled: Boolean) = viewModelScope.launch {
        gameBootPreferences.setGameBootEnabled(enabled)
    }

    fun cycleLaunchDiscStyle() = viewModelScope.launch {
        val styles = com.echo.core.data.repository.GameBootStyle.entries
        launchDiscPreferences.setStyle(styles[(uiState.value.launchDiscStyle.ordinal + 1) % styles.size])
    }

    fun cycleGameBootStyle() = viewModelScope.launch {
        val styles = com.echo.core.data.repository.GameBootStyle.entries
        gameBootPreferences.setStyle(styles[(uiState.value.gameBootStyle.ordinal + 1) % styles.size])
    }

    fun setLaunchDiscEnabled(enabled: Boolean) = viewModelScope.launch {
        launchDiscPreferences.setLaunchDiscEnabled(enabled)
    }

    fun uiMediaPickerMime(slot: UiMediaSlot): Array<String> =
        if (slot.kind == UiMediaKind.VIDEO) UiMediaLimits.VIDEO_MIME.toTypedArray()
        else UiMediaLimits.AUDIO_MIME.toTypedArray()

    fun setWaveStyle(style: WaveStyle) = save { it[KEY_WAVE_STYLE] = style.name }

    fun setMotionStyle(style: WaveStyle) = save { it[stringPreferencesKey(com.echo.core.ui.motion.MotionWallpaperPolicy.KEY)] = style.name }

    fun setWaveDesign(design: WaveDesign) = save { it[KEY_WAVE_DESIGN] = design.name }

    val waveStyleOptions: List<Pair<WaveStyle, String>> =
        WaveStyle.entries.map { it to it.label }
    val touchNavButtonOptions: List<Pair<TouchNavButtonMode, String>> =
        TouchNavButtonMode.entries.map { it to (TOUCH_NAV_BUTTON_LABELS[it] ?: it.name) }
    val touchSensitivityOptions: List<Pair<TouchSensitivity, String>> =
        TouchSensitivity.entries.map { it to it.label() }

    fun setIconLegibility(style: IconLegibilityStyle) = save { it[KEY_ICON_LEGIBILITY] = style.name }

    fun setFadeByDistance(v: Boolean) = save { it[KEY_FADE_BY_DISTANCE] = v }
    fun setGameRows(covers: Boolean) = save {
        with(com.echo.core.data.repository.IconDisplayPreferences) { it.setGameRows(covers) }
    }

    fun setRecentsIncludeApps(v: Boolean) = save { it[KEY_RECENTS_INCLUDE_APPS] = v }

    fun setTextShadow(v: Boolean) = save { it[KEY_TEXT_SHADOW] = v }

    fun setShowDeviceNotifications(v: Boolean) = save { it[com.echo.core.data.repository.InterfacePreferences.KEY_SHOW_DEVICE_NOTIFICATIONS] = v }
    fun setIslandShowsRecent(v: Boolean) = save { it[com.echo.core.data.repository.InterfacePreferences.KEY_ISLAND_SHOWS_RECENT] = v }
    fun setLastPlayedSize(v: Int) = save { it[com.echo.core.data.repository.InterfacePreferences.KEY_LAST_PLAYED_SIZE] = v }
    fun setVideoSeekStepSeconds(v: Int) = save { it[com.echo.core.data.repository.InterfacePreferences.KEY_VIDEO_SEEK_STEP_SECONDS] = v }
    fun setVideoControlsHideMs(v: Int) = save { it[com.echo.core.data.repository.InterfacePreferences.KEY_VIDEO_CONTROLS_HIDE_MS] = v }

    fun applyClassicLayout() = save { ClassicCrossbarLayout.write(it, ClassicCrossbarLayout.forWindow(context)) }

    fun setLayout(transform: (com.echo.themekit.CrossbarLayoutAdjust) -> com.echo.themekit.CrossbarLayoutAdjust) =
        save { ClassicCrossbarLayout.update(it, ClassicCrossbarLayout.forWindow(context).bucketKey, transform) }

    fun resetLayout() = setLayout { com.echo.themekit.CrossbarLayoutAdjust.DEFAULT }

    fun setShowBootSequence(v: Boolean)      = save { it[KEY_SHOW_BOOT]       = v }
    fun setShowBootOnResume(v: Boolean)      = save { it[KEY_BOOT_ON_RESUME]  = v }
    fun setThermalThrottleAware(v: Boolean)  = save { it[KEY_THERMAL_AWARE]   = v }
    fun setRespectBatterySaver(v: Boolean)   = save { it[KEY_RESPECT_BATTERY] = v }
    fun setWaveOverWallpaper(v: Boolean)     = save { it[KEY_WAVE_OVER_WALLPAPER] = v }
    fun setButtonHints(v: com.echo.core.data.repository.ButtonHints) = save {
        with(com.echo.core.data.repository.InterfacePreferences) { it.setButtonHints(v) }
    }
    fun setContextMenuHintDelaySeconds(v: Float) = save {
        it[KEY_CONTEXT_MENU_HINT_DELAY_SECONDS] = ControllerHintPolicy.clampDelay(v)
    }

    fun setTouchNavButtonMode(mode: TouchNavButtonMode) = save { it[KEY_TOUCH_NAV_BUTTON] = mode.name }

    fun setTouchSensitivity(level: TouchSensitivity) = save { it[KEY_TOUCH_SENSITIVITY] = level.name }

    fun onWallpaperPicked(uri: Uri) {
        viewModelScope.launch {
            val mime = context.contentResolver.getType(uri)
            if (mime != null && mime !in SUPPORTED_WALLPAPER_MIME) {
                _wallpaperMessage.value = "Unsupported format — use PNG, JPG, WEBP, MP4, WEBM, or GIF"
                return@launch
            }
            _wallpaperImporting.value = true
            try {
                if (mime in MOTION_WALLPAPER_MIME) {
                    importMotionWallpaper(uri, mime!!)
                } else {
                    importStillWallpaper(uri)
                }
            } finally {
                _wallpaperImporting.value = false
            }
        }
    }

    private suspend fun importStillWallpaper(uri: Uri) {
        _wallpaperMessage.value = when (stillWallpaper.apply { context.contentResolver.openInputStream(uri) }) {
            StillWallpaper.Result.APPLIED -> "Wallpaper applied"
            StillWallpaper.Result.SAVE_FAILED -> WALLPAPER_SAVE_FAILED
            StillWallpaper.Result.UNREADABLE -> "Couldn't read that file — try a different one"
        }
    }

    private suspend fun importMotionWallpaper(uri: Uri, mime: String) {
        val knownSize = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()?.takeIf { it > 0 }
        val result = motionWallpaper.apply(mime, knownSize) { context.contentResolver.openInputStream(uri) }
        _wallpaperMessage.value = result.message ?: "Motion wallpaper applied"
    }

    fun clearWallpaper() {
        viewModelScope.launch {
            val current = context.echoDataStore.data.first()
            val poster = current[KEY_CUSTOM_WALLPAPER]
            val motion = current[KEY_MOTION_WALLPAPER]

            val saved = saveThenPrune(
                save = {
                    context.echoDataStore.edit {
                        it.remove(KEY_CUSTOM_WALLPAPER)
                        it.remove(KEY_MOTION_WALLPAPER)
                        it.clearWallpaperAccent()
                    }
                },
                prune = {
                    withContext(io) {
                        poster?.let { runCatching { File(it).delete() } }
                        motion?.let { runCatching { File(it).delete() } }
                    }
                },
            )
            _wallpaperMessage.value = if (saved) "Wallpaper reset to default" else WALLPAPER_SAVE_FAILED
        }
    }

    fun dismissWallpaperMessage() {
        _wallpaperMessage.value = null
    }

    fun showWallpaperPreview() {
        if (uiState.value.customWallpaperPath != null) {
            _wallpaperPreviewVisible.value = true
        } else {
            _wallpaperMessage.value = "No wallpaper selected yet"
        }
    }

    fun hideWallpaperPreview() {
        _wallpaperPreviewVisible.value = false
    }

    private fun save(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        viewModelScope.launch { context.echoDataStore.edit { block(it) } }
    }
}
