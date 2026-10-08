package com.echo.feature.settings.ui

import com.echo.themekit.CrossbarLayoutAdjust
import kotlin.math.roundToInt
import com.echo.core.domain.model.ControllerHintPolicy
import com.echo.core.domain.model.IconLegibilityStyle
import com.echo.core.data.repository.InterfacePreferences as IP
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.echo.core.ui.motion.MotionWallpaperBackground
import com.echo.core.ui.motion.MotionWallpaperPolicy
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.UiMediaSlot
import com.echo.feature.settings.viewmodel.DisplaySettingsUiState
import com.echo.feature.settings.viewmodel.DisplaySettingsViewModel

enum class DisplaySection { APPEARANCE, LAYOUT, BOOT, INPUT, PERFORMANCE }

@Composable
fun DisplaySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    section: DisplaySection? = null,
    onOpenCustomIcons: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onPreviewBootSequence: () -> Unit = {},
    onPreviewGameBoot: () -> Unit = {},
    viewModel: DisplaySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()


    var classicConfirmFocus by remember { mutableStateOf<Int?>(null) }

    var focusedSlot by remember { mutableStateOf<UiMediaSlot?>(null) }

    var focusTargetSlot by remember { mutableStateOf<UiMediaSlot?>(null) }
    var focusRequestToken by remember { mutableIntStateOf(0) }
    var importWasActive by remember { mutableStateOf(false) }

    fun requestMediaFocus(slot: UiMediaSlot) {
        focusTargetSlot = slot
        focusRequestToken++
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onWallpaperPicked(it) } }

    val uiMediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onUiMediaPicked(uri)
        } else {
            focusTargetSlot?.let(::requestMediaFocus)
        }
    }

    fun pickUiMedia(slot: UiMediaSlot) {
        requestMediaFocus(slot)
        viewModel.onUiMediaPickerLaunchedFor(slot)
        uiMediaPicker.launch(viewModel.uiMediaPickerMime(slot))
    }

    LaunchedEffect(state.wallpaperImporting) {
        if (state.wallpaperImporting) {
            importWasActive = true
        } else if (importWasActive) {
            importWasActive = false
            focusTargetSlot?.let(::requestMediaFocus)
        }
    }

    fun launchWallpaperPicker() {
        focusTargetSlot = null

        wallpaperPicker.launch(
            arrayOf(
                "image/png", "image/jpeg", "image/webp",
                "video/mp4", "video/webm", "image/gif",
            )
        )
    }

    SettingsPageScaffold(

        subtitle = when (section) {
            DisplaySection.APPEARANCE  -> "Wallpaper"
            DisplaySection.LAYOUT      -> "Crossbar"
            DisplaySection.BOOT        -> "Boot"
            DisplaySection.INPUT       -> "Touch"
            DisplaySection.PERFORMANCE -> "Performance"
            null                       -> "Display"
        },
        onBack   = onBack,
        modifier = modifier,

        helperFooterItems = focusedSlot?.let { slot ->
            MediaRowShortcuts.promptsFor(state.xyLayout, isAssigned = slot.isAssignedIn(state))
        } ?: emptyList(),
        onInterceptAction = { action ->

            classicConfirmFocus?.let { focused ->
                when (action) {
                    GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT ->
                        classicConfirmFocus = if (focused == PSP_CONFIRM_CANCEL) PSP_CONFIRM_APPLY else PSP_CONFIRM_CANCEL
                    GamepadAction.SELECT -> {
                        if (focused == PSP_CONFIRM_APPLY) viewModel.applyClassicLayout()
                        classicConfirmFocus = null
                    }
                    GamepadAction.BACK -> classicConfirmFocus = null
                    else -> Unit
                }
                return@SettingsPageScaffold true
            }

            if (state.wallpaperPreviewVisible) {
                if (action == GamepadAction.SELECT || action == GamepadAction.BACK) {
                    viewModel.hideWallpaperPreview()
                }
                return@SettingsPageScaffold true
            }

            val slot = focusedSlot ?: return@SettingsPageScaffold false
            when {
                MediaRowShortcuts.isNorthFace(action, state.xyLayout) && slot.isAssignedIn(state) -> {
                    requestMediaFocus(slot)
                    viewModel.clearUiMedia(slot)
                    true
                }
                MediaRowShortcuts.isWestFace(action, state.xyLayout) -> {
                    when (slot) {
                        UiMediaSlot.BOOT_VIDEO -> onPreviewBootSequence()
                        UiMediaSlot.GAMEBOOT_VIDEO -> onPreviewGameBoot()
                        else -> return@SettingsPageScaffold false
                    }
                    true
                }
                else -> false
            }
        },
    ) {
        val focusRegistry = LocalSettingsFocusRegistry.current
        LaunchedEffect(focusRequestToken) {
            if (focusRequestToken > 0) {
                withFrameNanos { }
                withFrameNanos { }
                focusTargetSlot?.let { slot ->
                    runCatching { focusRegistry["display_${slot.key}"]?.requestFocus() }
                }
            }
        }

        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            if (section == null || section == DisplaySection.APPEARANCE) {
                SettingsGroup("Appearance")

                if (state.wallpaperImporting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp, vertical = 8.dp),
                    )
                } else {
                    SettingsRow(
                        label    = "Choose Wallpaper",
                        sublabel = if (state.motionWallpaperPath != null) "Motion wallpaper set — a looping video replaces the wave"
                                   else if (state.customWallpaperPath != null) "Custom wallpaper set — replaces the wave"
                                   else "Pick an image or a short video (PNG, JPG, WEBP, MP4, WEBM, GIF) — replaces the wave",
                        onClick  = ::launchWallpaperPicker,
                    )

                    SettingsRow(
                        label    = "Preview Wallpaper",
                        sublabel = "See the selected wallpaper full-screen",
                        onClick  = { viewModel.showWallpaperPreview() },
                    )

                    if (state.customWallpaperPath != null) {
                        SettingsRow(
                            label    = "Reset Wallpaper",
                            sublabel = "Remove custom wallpaper and restore the default background",
                            onClick  = { viewModel.clearWallpaper() },
                        )
                    }
                }

                if (state.customWallpaperPath != null) {
                    SettingsToggleRow(
                        label    = "Wave Over Wallpaper",
                        sublabel = "Keep the wave, drawn on top of your wallpaper",
                        checked  = state.waveOverWallpaper,
                        onToggle = { viewModel.setWaveOverWallpaper(it) },
                    )
                }

                if (state.customWallpaperPath == null || state.waveOverWallpaper) {
                    SettingsPickerRow(
                        label    = "Wave Design",
                        options  = com.echo.core.ui.wave.WaveDesign.entries.map { SettingsPickerOption(it.label) },
                        selectedIndex = state.waveDesign.ordinal,
                        onPick   = { viewModel.setWaveDesign(com.echo.core.ui.wave.WaveDesign.entries[it]) },
                    )
                    SettingsPickerRow(
                        label    = "Wave Style",
                        options  = viewModel.waveStyleOptions.map { SettingsPickerOption(it.second) },
                        selectedIndex = viewModel.waveStyleOptions.indexOfFirst { it.first == state.waveStyle },
                        onPick   = { viewModel.setWaveStyle(viewModel.waveStyleOptions[it].first) },
                    )
                } else if (state.motionWallpaperPath != null) {
                    SettingsPickerRow(
                        label    = "Background Motion",
                        options  = viewModel.waveStyleOptions.map { SettingsPickerOption(it.second) },
                        selectedIndex = viewModel.waveStyleOptions.indexOfFirst { it.first == state.waveStyle },
                        onPick   = { viewModel.setWaveStyle(viewModel.waveStyleOptions[it].first) },
                    )
                }

            }
            if (section == null || section == DisplaySection.LAYOUT) {
                // owner, 2026-10-06: every size on one screen as sliders, no live overlay; each screen size (handheld,
                // foldable, tablet) keeps its own
                SettingsGroup("Sizes")
                val layout = state.layoutAdjust
                val percent: (Float) -> String = { "${(it * 100).roundToInt()}%" }
                SettingsSliderRow(
                    label = "Crossbar Size",
                    sublabel = "The crossbar's icons, rows and text",
                    focusKey = "layout_scale",
                    value = layout.scale,
                    onValueChange = { v -> viewModel.setLayout { it.copy(scale = v) } },
                    valueRange = CrossbarLayoutAdjust.SCALE_MIN..CrossbarLayoutAdjust.SCALE_MAX,
                    steps = sliderSteps(CrossbarLayoutAdjust.SCALE_MIN, CrossbarLayoutAdjust.SCALE_MAX, 0.05f),
                    valueFormatter = percent,
                )
                SettingsSliderRow(
                    label = "Top Bar Size",
                    sublabel = "The islands, the battery reading and the clock",
                    focusKey = "layout_header",
                    value = layout.headerScale,
                    onValueChange = { v -> viewModel.setLayout { it.copy(headerScale = v) } },
                    valueRange = CrossbarLayoutAdjust.CHROME_MIN..CrossbarLayoutAdjust.CHROME_MAX,
                    steps = sliderSteps(CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX, 0.05f),
                    valueFormatter = percent,
                )
                SettingsSliderRow(
                    label = "Footer Size",
                    sublabel = "The filter, the buttons and the A card",
                    focusKey = "layout_footer",
                    value = layout.footerScale,
                    onValueChange = { v -> viewModel.setLayout { it.copy(footerScale = v) } },
                    valueRange = CrossbarLayoutAdjust.CHROME_MIN..CrossbarLayoutAdjust.CHROME_MAX,
                    steps = sliderSteps(CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX, 0.05f),
                    valueFormatter = percent,
                )
                SettingsGroup("Crossbar Position")
                SettingsSliderRow(
                    label = "Left and Right",
                    focusKey = "layout_left",
                    value = layout.barLeftFraction,
                    onValueChange = { v -> viewModel.setLayout { it.copy(barLeftFraction = v) } },
                    valueRange = CrossbarLayoutAdjust.LEFT_MIN..CrossbarLayoutAdjust.LEFT_MAX,
                    steps = sliderSteps(CrossbarLayoutAdjust.LEFT_MIN, CrossbarLayoutAdjust.LEFT_MAX, 0.01f),
                    valueFormatter = { "${(it * 100).roundToInt()}" },
                )
                SettingsSliderRow(
                    label = "Up and Down",
                    focusKey = "layout_top",
                    value = layout.barTopFraction,
                    onValueChange = { v -> viewModel.setLayout { it.copy(barTopFraction = v) } },
                    valueRange = CrossbarLayoutAdjust.TOP_MIN..CrossbarLayoutAdjust.TOP_MAX,
                    steps = sliderSteps(CrossbarLayoutAdjust.TOP_MIN, CrossbarLayoutAdjust.TOP_MAX, 0.01f),
                    valueFormatter = { "${(it * 100).roundToInt()}" },
                )
                SettingsRow(
                    label    = "Reset Layout",
                    sublabel = "Every size and the position back to the start, for this screen",
                    onClick  = { viewModel.resetLayout() },
                )

                SettingsRow(
                    label    = "Classic Layout",
                    sublabel = if (state.classicLayoutApplied) {
                        "Applied to this screen. Move a slider above to leave it"
                    } else {
                        "Apply the PSP's own proportions to this screen"
                    },
                    enabled  = !state.classicLayoutApplied,
                    onClick  = { classicConfirmFocus = PSP_CONFIRM_CANCEL },
                )

                // owner, 2026-10-07: everything about the crossbar in one tab: sizes, rows, Last Played, the status
                // bar, its categories and icons
                SettingsGroup("Rows")
                SettingsPickerRow(
                    label    = "Icon Legibility",
                    sublabel = "How Crossbar icons separate from the background",
                    options  = IconLegibilityStyle.entries.map { SettingsPickerOption(it.label) },
                    selectedIndex = IconLegibilityStyle.entries.indexOf(state.iconLegibility),
                    onPick   = { viewModel.setIconLegibility(IconLegibilityStyle.entries[it]) },
                )

                SettingsToggleRow(
                    label    = "Card Art Grid",
                    sublabel = "Show a console card as four covers from inside it, instead of its console icon",
                    checked  = state.cardArtGrid,
                    onToggle = { viewModel.setCardArtGrid(it) },
                )

                SettingsToggleRow(
                    label    = "Fade By Distance",
                    sublabel = "Fade rows and icons further the further they sit from the cursor — off, every unselected one dims the same",
                    checked  = state.fadeByDistance,
                    onToggle = { viewModel.setFadeByDistance(it) },
                )

                SettingsToggleRow(
                    label    = "Text Shadow",
                    sublabel = "Drop shadow behind row helper text — keeps it readable over bright wallpaper regions",
                    checked  = state.textShadow,
                    onToggle = { viewModel.setTextShadow(it) },
                )

                SettingsGroup("Last Played")
                SettingsToggleRow(
                    label    = "Apps On The Recent Shelf",

                    sublabel = "Show recently used apps beside games, music, books and video. " +
                        "Needs usage access; without it no app has a last-used time and none appear",
                    checked  = state.recentsIncludeApps,
                    onToggle = { viewModel.setRecentsIncludeApps(it) },
                )

                SettingsPickerRow(
                    label    = "Last Played Size",
                    sublabel = "How many games, tracks, books, videos and apps the Last Played column keeps",
                    options  = IP.LAST_PLAYED_SIZES.map { SettingsPickerOption("$it") },
                    selectedIndex = IP.LAST_PLAYED_SIZES.indexOf(state.interfaceChoices.lastPlayedSize),
                    onPick   = { viewModel.setLastPlayedSize(IP.LAST_PLAYED_SIZES[it]) },
                )

                SettingsGroup("Status Bar")

                SettingsToggleRow(
                    label    = "Show Device Notifications",
                    sublabel = "List Android's notifications in the bar and count them. Off, the bar shows only ECHO's own",
                    checked  = state.interfaceChoices.showDeviceNotifications,
                    onToggle = { viewModel.setShowDeviceNotifications(it) },
                )

                SettingsToggleRow(
                    label    = "Last Opened In The Island",
                    sublabel = "With nothing playing or running, show the last thing you opened. Off, the island stays empty",
                    checked  = state.interfaceChoices.islandShowsRecent,
                    onToggle = { viewModel.setIslandShowsRecent(it) },
                )
                SettingsGroup("Categories")
                SettingsRow(
                    label    = "Categories",
                    sublabel = "The crossbar's categories and the collections inside them",
                    onClick  = onOpenCategories,
                )

                SettingsRow(
                    label    = "Customize Crossbar Icons",
                    sublabel = "Replace any icon with your own image or GIF — live over the Crossbar",
                    onClick  = onOpenCustomIcons,
                )
            }
            if (section == null || section == DisplaySection.BOOT) {
                SettingsGroup("Boot Sequence")

                SettingsToggleRow(
                    label    = "Show Boot Sequence",
                    sublabel = "PSP-style boot animation on every launch",
                    onFocusChangedExternal = { if (it) focusedSlot = null },
                    checked  = state.showBootSequence,
                    onToggle = { viewModel.setShowBootSequence(it) },
                )

                SettingsToggleRow(
                    label    = "Show Boot Sequence on Resume",
                    sublabel = "Also play when you come back to the launcher from another app",
                    onFocusChangedExternal = { if (it) focusedSlot = null },
                    checked  = state.showBootOnResume,
                    onToggle = { viewModel.setShowBootOnResume(it) },
                )

                MediaAssignmentRow(
                    label    = "Boot Video",
                    focusKey = "display_${UiMediaSlot.BOOT_VIDEO.key}",
                    sublabel = "Play your own video instead of the PSP logo animation " +
                        "(MP4 or WebM, up to 10 seconds)",
                    value    = state.bootVideoLabel,
                    isAssigned = state.bootVideoAssigned,
                    onPick   = { pickUiMedia(UiMediaSlot.BOOT_VIDEO) },
                    onPreview = onPreviewBootSequence,
                    onUseDefault = { viewModel.clearUiMedia(UiMediaSlot.BOOT_VIDEO) },
                    onFocusChanged = { focusedSlot = if (it) UiMediaSlot.BOOT_VIDEO else null },
                )

                SettingsGroup("Launch Disc  ·  two switches, one animation")

                SettingsToggleRow(
                    label    = "Launch Disc  (everything but games)",
                    sublabel = "The cover turns into a spinning disc between confirming something " +
                        "and it opening — films, books, music and apps.  Games have their own " +
                        "switch, GameBoot, directly below: it plays the SAME disc, and turning " +
                        "one off never affects the other.  Off opens these straight away.",
                    onFocusChangedExternal = { if (it) focusedSlot = null },
                    checked  = state.launchDiscEnabled,
                    onToggle = { viewModel.setLaunchDiscEnabled(it) },
                )

                if (state.launchDiscEnabled) {
                    // owner, 2026-10-05: the same choice of animation as GameBoot
                    SettingsValueRow(
                        label = "Launch Disc Style",
                        value = state.launchDiscStyle.label,
                        sublabel = "Disc spins the cover as a disc. Lens spins it inside the ECHO ring, then opens it like a lens",
                        onFocusChangedExternal = { if (it) focusedSlot = null },
                        onClick = { viewModel.cycleLaunchDiscStyle() },
                    )
                }

                SettingsGroup("GameBoot")

                SettingsToggleRow(
                    label    = "GameBoot  (games only)",
                    sublabel = "The same disc as Launch Disc above, for games — between " +
                        "confirming one and the emulator opening.  Two things only this switch " +
                        "has: a sound as the disc leaves, and the option to replace the whole " +
                        "thing with your own clip below.  Off is a silent launch, and it is why " +
                        "a game can open with no animation while a film still gets one.",
                    onFocusChangedExternal = { if (it) focusedSlot = null },
                    checked  = state.gameBootEnabled,
                    onToggle = { viewModel.setGameBootEnabled(it) },
                )

                if (state.gameBootEnabled && !state.gameBootVideoAssigned) {
                    // owner, 2026-10-05: Lens is a second built-in animation beside the disc
                    SettingsValueRow(
                        label = "GameBoot Style",
                        value = state.gameBootStyle.label,
                        sublabel = "Disc spins a disc and opens the game. Lens spins the game's own art inside the ECHO ring, then opens it like a lens",
                        onFocusChangedExternal = { if (it) focusedSlot = null },
                        onClick = { viewModel.cycleGameBootStyle() },
                    )
                }

                if (state.gameBootEnabled) {
                    MediaAssignmentRow(
                        label    = "GameBoot Video",
                        focusKey = "display_${UiMediaSlot.GAMEBOOT_VIDEO.key}",
                        sublabel = "Replace the built-in sequence with your own clip, which plays with " +
                            "its own sound — even with Menu Sounds off (MP4 or WebM, up to 10 seconds)",
                        value    = state.gameBootVideoLabel,
                        isAssigned = state.gameBootVideoAssigned,
                        onPick   = { pickUiMedia(UiMediaSlot.GAMEBOOT_VIDEO) },
                        onPreview = onPreviewGameBoot,
                        onUseDefault = { viewModel.clearUiMedia(UiMediaSlot.GAMEBOOT_VIDEO) },
                        onFocusChanged = { focusedSlot = if (it) UiMediaSlot.GAMEBOOT_VIDEO else null },
                    )
                }
            }
            if (section == null || section == DisplaySection.INPUT) {
                SettingsGroup("Interface")

                SettingsPickerRow(
                    label    = "Touch Navigation Button",
                    sublabel = "On-screen App Drawer / Back button",
                    options  = viewModel.touchNavButtonOptions.map { SettingsPickerOption(it.second) },
                    selectedIndex = viewModel.touchNavButtonOptions
                        .indexOfFirst { it.first == state.touchNavButtonMode },
                    onPick   = { viewModel.setTouchNavButtonMode(viewModel.touchNavButtonOptions[it].first) },
                )

                SettingsPickerRow(
                    label    = "Touch Sensitivity",
                    sublabel = "How far a swipe travels per Crossbar step",
                    options  = viewModel.touchSensitivityOptions.map { SettingsPickerOption(it.second) },
                    selectedIndex = viewModel.touchSensitivityOptions
                        .indexOfFirst { it.first == state.touchSensitivity },
                    onPick   = { viewModel.setTouchSensitivity(viewModel.touchSensitivityOptions[it].first) },
                )

                SettingsToggleRow(
                    label    = "Button Hints",
                    sublabel = "Show the on-screen button prompts, and let them be tapped",
                    checked  = state.contextMenuHintEnabled,
                    onToggle = { viewModel.setContextMenuHintEnabled(it) },
                )

                SettingsSliderRow(
                    label     = "Hint Delay",

                    sublabel  = "Always shown at ${formatHintDelay(ControllerHintPolicy.MIN_DELAY_SECONDS)}, " +
                        "or hide until a pause of up to ${formatHintDelay(ControllerHintPolicy.MAX_DELAY_SECONDS)}",
                    value     = state.contextMenuHintDelaySeconds,
                    onValueChange = viewModel::setContextMenuHintDelaySeconds,
                    valueRange = ControllerHintPolicy.DELAY_RANGE,
                    steps     = ControllerHintPolicy.DELAY_STEPS,
                    enabled  = state.contextMenuHintEnabled,
                    valueFormatter = { formatHintDelay(it) },
                )

                SettingsGroup("Video Player")

                SettingsPickerRow(
                    label    = "Seek Step",
                    sublabel = "How far left and right skip while a video plays",
                    options  = IP.VIDEO_SEEK_STEPS_SECONDS.map { SettingsPickerOption("$it s") },
                    selectedIndex = IP.VIDEO_SEEK_STEPS_SECONDS.indexOf(state.interfaceChoices.videoSeekStepSeconds),
                    onPick   = { viewModel.setVideoSeekStepSeconds(IP.VIDEO_SEEK_STEPS_SECONDS[it]) },
                )

                SettingsPickerRow(
                    label    = "Hide Controls After",
                    sublabel = "How long the player's controls stay up after the last press",
                    options  = IP.VIDEO_CONTROLS_HIDE_MS.map { SettingsPickerOption("${it / 1000.0} s".replace(".0 s", " s")) },
                    selectedIndex = IP.VIDEO_CONTROLS_HIDE_MS.indexOf(state.interfaceChoices.videoControlsHideMs),
                    onPick   = { viewModel.setVideoControlsHideMs(IP.VIDEO_CONTROLS_HIDE_MS[it]) },
                )
            }
            if (section == null || section == DisplaySection.PERFORMANCE) {
                SettingsGroup("Performance")

                SettingsToggleRow(
                    label    = "Thermal Throttle Awareness",
                    sublabel = "Automatically reduce background quality when device runs hot",
                    checked  = state.thermalThrottleAware,
                    onToggle = { viewModel.setThermalThrottleAware(it) },
                )

                SettingsToggleRow(
                    label    = "Battery Saver Mode",
                    sublabel = "Freeze the background (wave or motion wallpaper) when Battery Saver is active",
                    checked  = state.respectBatterySaver,
                    onToggle = { viewModel.setRespectBatterySaver(it) },
                )

                SettingsToggleRow(
                    label    = "Rescan On Return",
                    sublabel = "Look for new and missing games when you come back to the launcher, at most every five minutes. " +
                        "Inserting a card still rescans either way",
                    checked  = state.interfaceChoices.rescanOnReturn,
                    onToggle = { viewModel.setRescanOnReturn(it) },
                )

            }
        }
    }

    if (state.wallpaperPreviewVisible && state.customWallpaperPath != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { viewModel.hideWallpaperPreview() },
        ) {
            val posterPath = state.customWallpaperPath
            val motionPath = state.motionWallpaperPath
            if (motionPath != null && posterPath != null) {
                MotionWallpaperBackground(
                    posterPath = posterPath,
                    motionPath = motionPath,
                    decision   = MotionWallpaperPolicy.Decision.PLAY,
                    modifier   = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model              = state.customWallpaperPath,
                    contentDescription = "Wallpaper preview",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
        }
    }

    classicConfirmFocus?.let { focused ->
        ClassicLayoutConfirmPanel(
            focusedOption = focused,
            onCancel = { classicConfirmFocus = null },
            onApply = { viewModel.applyClassicLayout(); classicConfirmFocus = null },
        )
    }

    if (state.wallpaperMessage != null) {
        SettingsMessageOverlay(
            title = "Wallpaper",
            message = state.wallpaperMessage!!,
            onDismiss = { viewModel.dismissWallpaperMessage() },
        )
    }
}

private const val PSP_CONFIRM_CANCEL = 0
private const val PSP_CONFIRM_APPLY = 1

@Composable
private fun ClassicLayoutConfirmPanel(focusedOption: Int, onCancel: () -> Unit, onApply: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xF2101018))
                .border(1.dp, SettingsDivider, RoundedCornerShape(8.dp))

                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text("Apply Classic Layout?", color = SettingsText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sets this screen's Crossbar scale and crossbar position to the PSP's own proportions. " +
                    "Your current layout for this screen size is replaced; other screen sizes keep theirs.",
                color = SettingsSubtext,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClassicConfirmOption("Cancel", focusedOption == PSP_CONFIRM_CANCEL, onCancel)
                ClassicConfirmOption("Apply", focusedOption == PSP_CONFIRM_APPLY, onApply)
            }
        }
    }
}

@Composable
private fun ClassicConfirmOption(label: String, focused: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) SettingsAccent.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (focused) SettingsAccent else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (focused) Color.White else SettingsSubtext, fontSize = 14.sp)
    }
}

private fun formatHintDelay(seconds: Float): String = when {
    seconds <= 0f -> "Always"
    seconds % 1f == 0f -> "${seconds.toInt()}s"
    else -> "${seconds}s"
}

private fun UiMediaSlot.isAssignedIn(state: DisplaySettingsUiState): Boolean = when (this) {
    UiMediaSlot.BOOT_VIDEO -> state.bootVideoAssigned
    UiMediaSlot.GAMEBOOT_VIDEO -> state.gameBootVideoAssigned
    else -> false
}

// the stops between a slider's ends for a step size, as SettingsSliderRow counts them (the ends not included)
internal fun sliderSteps(min: Float, max: Float, step: Float): Int = (((max - min) / step).roundToInt() - 1).coerceAtLeast(0)
