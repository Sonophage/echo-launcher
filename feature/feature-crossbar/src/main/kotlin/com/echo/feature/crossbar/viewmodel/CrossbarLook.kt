package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CustomIconStore
import com.echo.core.data.repository.EchoThemeStore
import com.echo.core.domain.model.CrossbarColorScheme
import com.echo.core.domain.model.displayLabel
import com.echo.core.domain.model.resolve
import com.echo.core.ui.icons.CustomIcon
import com.echo.core.ui.icons.GifFrameProbe
import com.echo.core.ui.sound.MenuSound
import com.echo.core.ui.theme.DefaultEchoColors
import com.echo.core.ui.theme.EchoColors
import com.echo.core.ui.theme.withWaveTint
import com.echo.themekit.CustomizableIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// owner, 2026-10-06: a new install starts on Black; an earlier install keeps Classic Blue (keepOldDefaults)
internal val DEFAULT_COLOR_SCHEME = CrossbarColorScheme.BLACK

class CrossbarLook(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal var baseThemeColors: EchoColors = DefaultEchoColors

    internal fun observeColorScheme() {
        scope.launch {
            vm.context.echoDataStore.data
                .map { prefs ->
                    SchemePrefs(
                        schemeName = prefs[CrossbarViewModel.KEY_COLOR_SCHEME] ?: DEFAULT_COLOR_SCHEME.name,
                        accentOverride = prefs[CrossbarViewModel.KEY_ACCENT_OVERRIDE],
                        iconColor = prefs[CrossbarViewModel.KEY_ICON_COLOR],
                        iconsStamp = prefs[com.echo.core.data.repository.EchoThemeStore.KEY_THEME_ICONS_STAMP],
                        layoutJson = prefs[com.echo.core.data.repository.EchoThemeStore.KEY_THEME_LAYOUT],
                        layoutAdjustJson = prefs[CrossbarViewModel.KEY_CROSSBAR_LAYOUT_ADJUST],
                        customIconsStamp = prefs[CustomIconStore.KEY_CUSTOM_ICONS_STAMP],
                        textColor = prefs[CrossbarViewModel.KEY_TEXT_COLOR],
                    )
                }
                .distinctUntilChanged()
                .collect { (name, accentOverride, iconColorArgb, iconsStamp, layoutJson, layoutAdjustJson, customIconsStamp, textColorArgb) ->
                    val base = if (accentOverride != null) {
                        DefaultEchoColors.withWaveTint(
                            androidx.compose.ui.graphics.Color(accentOverride and 0xFFFFFFFFL),
                        )
                    } else {
                        val scheme = runCatching { CrossbarColorScheme.valueOf(name) }
                            .getOrDefault(DEFAULT_COLOR_SCHEME)
                        val month = java.time.LocalDate.now().monthValue
                        scheme.resolve(month).toEchoColors()
                    }

                    val textColor = textColorArgb
                        ?.let { androidx.compose.ui.graphics.Color(it and 0xFFFFFFFFL) }
                        ?: base.textPrimary
                    baseThemeColors = base.copy(
                        iconColor = iconColorArgb
                            ?.let { androidx.compose.ui.graphics.Color(it and 0xFFFFFFFFL) }
                            ?: androidx.compose.ui.graphics.Color.White,
                        textPrimary = textColor,
                        textSecondary = textColor.copy(alpha = 0.7f),
                    )

                    val iconOverrides = if (iconsStamp != null) loadThemeIconOverrides() else emptyMap()
                    val customIcons =
                        if (customIconsStamp != null) vm.customIconStore.load() else emptyMap()

                    val themeSpec = com.echo.themekit.CrossbarLayoutSpecCodec.decode(layoutJson)
                        ?: com.echo.themekit.CrossbarLayoutSpec.DEFAULT

                    val adjustMap = com.echo.themekit.CrossbarLayoutAdjustCodec.decode(layoutAdjustJson)
                    uiState.update {
                        it.copy(
                            themeColors = baseThemeColors,
                            iconOverrides = iconOverrides,
                            customIcons = customIcons,
                            layoutSpec = themeSpec,
                            crossbarLayoutAdjustMap = adjustMap,
                        )
                    }
                }
        }
    }

    private suspend fun loadThemeIconOverrides(): Map<String, CustomIcon> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val iconsDir = java.io.File(vm.context.filesDir, EchoThemeStore.THEME_ICONS_DIR)
            iconsDir.listFiles { f -> f.isFile }.orEmpty().mapNotNull { file ->
                val key = file.nameWithoutExtension
                if (!CustomizableIcons.isValidKey(key)) return@mapNotNull null
                val ext = file.extension.lowercase()

                val bitmap = com.echo.core.data.repository.SafeMedia
                    .decodeFileCapped(file.absolutePath, maxDimension = 2048, targetDimension = 2048)
                    ?: return@mapNotNull null
                val firstFrame = bitmap.asImageBitmap()
                if (ext == "gif") {
                    if (GifFrameProbe.countFrames(file) > 1) {
                        key to CustomIcon.Animated(path = file.absolutePath, firstFrame = firstFrame)
                    } else {
                        key to CustomIcon.Still(firstFrame)
                    }
                } else {
                    key to CustomIcon.Still(firstFrame)
                }
            }.toMap()
        }

    private var colorSchemeOriginal: CrossbarColorScheme? = null

    private var accentOverrideOriginal: Long? = null

    fun openColorSchemePicker() {
        scope.launch {
            val prefs = vm.context.echoDataStore.data.first()
            val current = runCatching {
                CrossbarColorScheme.valueOf(prefs[CrossbarViewModel.KEY_COLOR_SCHEME] ?: DEFAULT_COLOR_SCHEME.name)
            }.getOrDefault(DEFAULT_COLOR_SCHEME)
            colorSchemeOriginal = current
            accentOverrideOriginal = prefs[CrossbarViewModel.KEY_ACCENT_OVERRIDE]

            val month = java.time.LocalDate.now().monthValue
            val options = CrossbarColorScheme.values().map { scheme ->
                ColorSchemeOption(
                    scheme   = scheme,
                    label    = scheme.displayLabel(),

                    sublabel = if (scheme == CrossbarColorScheme.ORIGINAL) "Changes with the month" else null,
                    swatch   = scheme.resolve(month).waveColor,
                )
            }
            val custom = prefs[CrossbarViewModel.KEY_ACCENT_OVERRIDE]
            val pickerOptions = options + ColorSchemeOption(
                scheme = null,
                label = "Custom",
                sublabel = "Choose a custom accent color",
                swatch = custom ?: 0xFF888888L,
                isCustom = true,
            )
            val index = if (custom != null) pickerOptions.lastIndex else options.indexOfFirst { it.scheme == current }.coerceAtLeast(0)
            uiState.update { it.copy(colorSchemePicker = ColorSchemePickerState(pickerOptions, index)) }
        }
    }

    internal fun moveColorSchemePicker(delta: Int) {
        val picker = uiState.value.colorSchemePicker ?: return
        val next = (picker.selectedIndex + delta).coerceIn(0, picker.options.lastIndex)
        if (next == picker.selectedIndex) { vm.gamepadInputHandler.cancelRepeat(); return }
        uiState.update { it.copy(colorSchemePicker = picker.copy(selectedIndex = next)) }
        picker.options[next].scheme?.let(::previewColorScheme)
    }

    fun onColorSchemeHighlightedAt(index: Int) {
        val picker = uiState.value.colorSchemePicker ?: return
        if (index !in picker.options.indices || index == picker.selectedIndex) return
        uiState.update { it.copy(colorSchemePicker = picker.copy(selectedIndex = index)) }
        picker.options[index].scheme?.let(::previewColorScheme)
    }

    private fun previewColorScheme(scheme: CrossbarColorScheme) {
        scope.launch {
            vm.context.echoDataStore.edit {
                it[CrossbarViewModel.KEY_COLOR_SCHEME] = scheme.name

                it.remove(CrossbarViewModel.KEY_ACCENT_OVERRIDE)
            }
        }
    }

    fun confirmColorSchemePicker() {
        val picker = uiState.value.colorSchemePicker ?: return
        val selected = picker.options.getOrNull(picker.selectedIndex)
        val chosen = selected?.scheme
        if (selected?.isCustom == true) {
            openCustomColorPicker()
            return
        }
        scope.launch {
            if (chosen != null) {
                vm.context.echoDataStore.edit {
                    it[CrossbarViewModel.KEY_COLOR_SCHEME] = chosen.name

                    it.remove(CrossbarViewModel.KEY_ACCENT_OVERRIDE)
                    it.remove(com.echo.core.data.repository.EchoThemeStore.KEY_THEME_ICONS_STAMP)
                    it.remove(com.echo.core.data.repository.EchoThemeStore.KEY_THEME_LAYOUT)
                }
            }
            colorSchemeOriginal = null
            accentOverrideOriginal = null
            uiState.update { it.copy(colorSchemePicker = null) }
        }
    }

    private fun openCustomColorPicker() {
        val argb = uiState.value.themeColors.accentColor.toArgb().toLong() and 0xFFFFFFFFL
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV((argb and 0xFFFFFFFFL).toInt(), hsv)
        uiState.update { state ->
            return@update state.copy(customColorPicker = CustomColorPickerState(hsv[0], hsv[1], hsv[2]))
        }
    }

    fun updateCustomColor(channel: Int, fraction: Float) {
        uiState.update { state ->
            val picker = state.customColorPicker ?: return@update state
            val clamped = fraction.coerceIn(0f, 1f)
            return@update state.copy(customColorPicker = picker.copy(
                hue = if (channel == 0) clamped * 360f else picker.hue,
                saturation = if (channel == 1) clamped else picker.saturation,
                brightness = if (channel == 2) clamped else picker.brightness,
                selectedChannel = channel,
            ))
        }
    }

    fun moveCustomColorChannel(delta: Int) {
        uiState.update { state ->
            val picker = state.customColorPicker ?: return@update state
            state.copy(customColorPicker = picker.copy(selectedChannel = (picker.selectedChannel + delta + 3) % 3))
        }
    }

    fun adjustCustomColor(delta: Float) {
        val picker = uiState.value.customColorPicker ?: return
        val value = when (picker.selectedChannel) {
            0 -> ((picker.hue / 360f) + delta).mod(1f)
            1 -> picker.saturation + delta
            else -> picker.brightness + delta
        }
        updateCustomColor(picker.selectedChannel, value)
    }

    fun confirmCustomColor() {
        val picker = uiState.value.customColorPicker ?: return
        scope.launch {
            vm.context.echoDataStore.edit { it[CrossbarViewModel.KEY_ACCENT_OVERRIDE] = android.graphics.Color.HSVToColor(floatArrayOf(picker.hue, picker.saturation, picker.brightness)).toLong() and 0xFFFFFFFFL }
            uiState.update { it.copy(customColorPicker = null, colorSchemePicker = null) }
        }
    }

    fun cancelCustomColor() {
        uiState.update { it.copy(customColorPicker = null) }
    }

    fun cancelColorSchemePicker() {
        val original = colorSchemeOriginal
        val accentOriginal = accentOverrideOriginal
        scope.launch {
            if (original != null) {
                vm.context.echoDataStore.edit {
                    it[CrossbarViewModel.KEY_COLOR_SCHEME] = original.name

                    if (accentOriginal != null) it[CrossbarViewModel.KEY_ACCENT_OVERRIDE] = accentOriginal
                }
            }
            colorSchemeOriginal = null
            accentOverrideOriginal = null
            uiState.update { it.copy(colorSchemePicker = null) }
        }
    }

    fun onThemeShareConsumed() {
        uiState.update { it.copy(pendingThemeShareFile = null) }
    }

    fun requestSaveCurrentLookAsTheme() {
        uiState.update {
            it.copy(saveThemeNameDialog = PlaylistNameDialogState(title = "Save Current Look as Theme"))
        }
    }

    fun confirmSaveCurrentLookAsTheme(name: String) {
        uiState.update { it.copy(saveThemeNameDialog = null) }
        menuSound.play(MenuSound.CONFIRM)
        saveCurrentLookAsTheme(name)
    }

    fun dismissSaveThemeNameDialog() {
        uiState.update { it.copy(saveThemeNameDialog = null) }
    }

    fun saveCurrentLookAsTheme(name: String) {
        scope.launch {
            val saved = vm.echoThemeStore.saveCurrentLook(name)
            val message = when {
                saved == null -> "Couldn't save the theme"
                else -> "Theme saved — ${saved.name}"
            }
            val shareFile = saved?.let { vm.echoThemeStore.exportForShare(it.id) }
            uiState.update {
                val s = it.customIconSession ?: return@update it
                it.copy(
                    customIconSession = s.copy(message = message, revision = s.revision + 1),
                    pendingThemeShareFile = shareFile,
                )
            }
        }
    }

    internal fun observeWallpaper() {
        scope.launch {
            vm.context.echoDataStore.data.collect { prefs ->
                val path = prefs[CrossbarViewModel.KEY_CUSTOM_WALLPAPER]

                val validPath = if (path != null && java.io.File(path).exists()) path else null

                val motionPath = prefs[CrossbarViewModel.KEY_MOTION_WALLPAPER]
                    ?.takeIf { validPath != null && java.io.File(it).exists() }

                val accent = prefs[com.echo.core.data.wallpaper.WallpaperAccentProbe.KEY_WALLPAPER_ACCENT]
                    ?.takeIf { validPath != null }
                uiState.update {
                    it.copy(
                        customWallpaperPath = validPath,
                        motionWallpaperPath = motionPath,
                        wallpaperAccent = accent,
                    )
                }
            }
        }
    }

    private data class SchemePrefs(
        val schemeName: String,
        val accentOverride: Long?,
        val iconColor: Long?,
        val iconsStamp: Long?,
        val layoutJson: String?,

        val layoutAdjustJson: String?,

        val customIconsStamp: Long?,

        val textColor: Long?,
    )

    internal fun onCustomColorButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP -> moveCustomColorChannel(-1)
            GamepadAction.NAVIGATE_DOWN -> moveCustomColorChannel(1)
            GamepadAction.NAVIGATE_LEFT -> adjustCustomColor(-0.04f)
            GamepadAction.NAVIGATE_RIGHT -> adjustCustomColor(0.04f)
            GamepadAction.SELECT -> confirmCustomColor()
            GamepadAction.BACK, GamepadAction.OPEN_CONTEXT_MENU -> cancelCustomColor()
            else -> Unit
        }
    }

    internal fun onSchemePickerButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP   -> moveColorSchemePicker(-1)
            GamepadAction.NAVIGATE_DOWN -> moveColorSchemePicker(+1)
            GamepadAction.SELECT        -> confirmColorSchemePicker()
            GamepadAction.BACK,
            GamepadAction.OPEN_CONTEXT_MENU    -> cancelColorSchemePicker()
            else -> Unit
        }
    }
}
