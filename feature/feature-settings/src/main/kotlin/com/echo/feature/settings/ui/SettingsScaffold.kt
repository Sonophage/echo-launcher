package com.echo.feature.settings.ui

import com.echo.core.ui.design.GlowSide
import com.echo.core.ui.design.GlowMaskedWave
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.ui.design.PANEL_CARD_RADIUS
import com.echo.core.ui.design.PANEL_FOCUS_RING_WIDTH
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.PanelCardFill
import com.echo.core.ui.design.PanelCardFocusFill
import com.echo.core.ui.design.PanelFocusRing
import com.echo.core.ui.design.panelBackdrop
import androidx.compose.ui.unit.em
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.DesignUnits
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import com.echo.themekit.CrossbarLayoutSpec
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.isDirectional
import com.echo.core.ui.sound.MenuSound
import com.echo.core.ui.components.ControllerHintStyle
import com.echo.core.ui.components.EchoControllerHints
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.primaryHint
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.gesture.dragToScroll
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.ui.theme.crossbarScrimAnchors
import kotlinx.coroutines.launch
import timber.log.Timber
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.components.contextMenuInk

val LocalSettingsScreenId = compositionLocalOf<String?> { null }

// the units the kit's rail rows are drawn in: slim, a white fill on focus (owner, 2026-10-04). Every
// settings page sets it; unset (a row drawn outside a settings page), rows keep the settings plate
val LocalSettingsRailUnits = compositionLocalOf<DesignUnits?> { null }

val LocalSettingsOpenScreen = compositionLocalOf<(String) -> Unit> { {} }

val LocalSettingsPendingAction = compositionLocalOf<GamepadAction?> { null }
val LocalSettingsActionConsumed = compositionLocalOf<() -> Unit> { {} }

val LocalSettingsTouchInput = compositionLocalOf<() -> Unit> { {} }

val LocalSettingsHostTouchInput = compositionLocalOf<() -> Unit> { {} }
val LocalSettingsShowControllerHint = compositionLocalOf { false }

val LocalSettingsPromptAction = compositionLocalOf<((GamepadAction) -> Unit)?> { null }

val LocalSettingsLeftBacksOut = compositionLocalOf { true }

val LocalSettingsLastInputWasTouch = compositionLocalOf { false }
val LocalSettingsCursorVisible = compositionLocalOf { true }

internal val LocalSettingsOverlayInput =
    compositionLocalOf<MutableState<((GamepadAction) -> Unit)?>?> { null }

@Composable
fun SettingsOverlayInput(onAction: (GamepadAction) -> Unit) {
    val slot = LocalSettingsOverlayInput.current
        ?: error("SettingsOverlayInput must be composed inside SettingsNavHost")
    val current by rememberUpdatedState(onAction)
    DisposableEffect(Unit) {
        slot.value = { action -> current(action) }
        onDispose { slot.value = null }
    }
}

internal val LocalSettingsFocusTracker =
    compositionLocalOf<((() -> Unit)?) -> Unit> { {} }

internal val LocalSettingsFocusRegistry =
    compositionLocalOf<SnapshotStateMap<String, FocusRequester>> { mutableStateMapOf() }

internal val LocalSettingsRegisterFirstFocusable =
    compositionLocalOf<(FocusRequester) -> Unit> { {} }

internal val LocalSettingsRowPositions =
    compositionLocalOf<SnapshotStateMap<FocusRequester, Float>?> { null }

internal val LocalSettingsRowSizes =
    compositionLocalOf<SnapshotStateMap<FocusRequester, Float>?> { null }
internal val LocalSettingsNavigationOrder =
    compositionLocalOf<SnapshotStateList<Pair<FocusRequester, ControllerNavItem>>?> { null }

internal val LocalSettingsRowActions =
    compositionLocalOf<SnapshotStateMap<String, SnapshotStateList<Pair<String, FocusRequester>>>?> { null }

internal val LocalSettingsScrollStateRegistrar =
    compositionLocalOf<(ScrollState) -> Unit> { {} }

internal val LocalSettingsReportFocused =
    compositionLocalOf<(FocusRequester) -> Unit> { {} }

internal val LocalSettingsEnterSliderMode =
    compositionLocalOf<(SettingsSliderNode) -> Unit> { {} }
internal val LocalSettingsSliderAdjusting = compositionLocalOf { false }

internal val LocalSettingsReportRemoved =
    compositionLocalOf<(FocusRequester) -> Unit> { {} }

private fun reseedFocus(
    rowPositions: Map<FocusRequester, Float>,
    lastFocusedY: Float?,
    firstRow: FocusRequester?,
) {
    val target = lastFocusedY?.let { anchor ->
        rowPositions.entries.minByOrNull { kotlin.math.abs(it.value - anchor) }?.key
    } ?: firstRow
    target?.let { runCatching { it.requestFocus() } }
}

val SettingsAccent: Color
    @Composable get() = LocalEchoColors.current.accentColor

val SettingsText: Color
    @Composable get() = Color.White
val SettingsSubtext: Color
    @Composable get() = Color.White.copy(alpha = 0.65f)

val SettingsTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 2f),
    blurRadius = 4f,
)

val SettingsDivider = com.echo.core.ui.theme.EchoPalette.Divider

val SETTINGS_COLUMN_MAX_WIDTH = 560.dp

private val SETTINGS_HELP_PANE_MIN_WIDTH = 220.dp

val LocalSettingsHelp = compositionLocalOf { mutableStateOf<String?>(null) }

data class SettingsFocusInfo(val label: String, val value: String?, val sublabel: String?)

internal val LocalSettingsFocusInfo = compositionLocalOf<MutableState<SettingsFocusInfo?>?> { null }

private val SETTINGS_ROW_SHAPE = RoundedCornerShape(PANEL_CARD_RADIUS.dp)

internal fun Modifier.settingsSelectedPlate(selected: Boolean): Modifier = this
    .clip(SETTINGS_ROW_SHAPE)
    .background(if (selected) PanelCardFocusFill else PanelCardFill, SETTINGS_ROW_SHAPE)
    .then(
        if (selected) Modifier.border(PANEL_FOCUS_RING_WIDTH.dp, PanelFocusRing, SETTINGS_ROW_SHAPE)
        else Modifier
    )


data class SettingsPickerOption(val label: String, val help: String? = null)

internal class SettingsPickerRequest(
    val title: String,
    val options: List<SettingsPickerOption>,
    val selectedIndex: Int,
    val onPick: (Int) -> Unit,
)

internal val LocalSettingsPicker =
    compositionLocalOf { mutableStateOf<SettingsPickerRequest?>(null) }

// a text field row's edit, drawn by the scaffold as the kit's rail prompt (owner, 2026-10-07: every settings
// text field on the kit, none a Material field)
internal class SettingsTextRequest(
    val title: String,
    val value: String,
    val placeholder: String,
    val isPassword: Boolean,
    val onDone: (String) -> Unit,
)

internal val LocalSettingsTextPrompt =
    compositionLocalOf { mutableStateOf<SettingsTextRequest?>(null) }

private const val SETTINGS_HELP_TEXT_SP = 13

internal val CONTENT_EDGE_MARGIN = 16.dp

internal const val SettingsContentViewportTag = "settings_content_viewport"

val SettingsDefaultHelperItems = listOf(
    ControllerPromptItem(GamepadAction.SELECT, "Enter"),
    ControllerPromptItem(GamepadAction.BACK, "Back"),
)

@Composable
fun SettingsScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,

    restoreFocusKey: String? = null,

    onInterceptAction: ((GamepadAction) -> Boolean)? = null,
    onTouchInput: () -> Unit = {},

    header: (@Composable () -> Unit)? = null,

    showDivider: Boolean = true,

    panelTint: Color? = null,

    backdrop: (@Composable () -> Unit)? = null,

    showRail: Boolean = true,

    paneText: SettingsPaneText? = null,

    footer: (@Composable () -> Unit)? = null,

    helperFooterItems: List<ControllerPromptItem> = SettingsDefaultHelperItems,

    contentKey: Any? = null,
    // the page uses the whole width, with no help pane (the Overview, owner 2026-10-05)
    fullWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    val leftBacksOut = LocalSettingsLeftBacksOut.current
    val focusManager = LocalFocusManager.current

    val contentScrollState = remember { mutableStateOf<ScrollState?>(null) }

    val bootstrapFR = remember { FocusRequester() }
    val pendingAction = LocalSettingsPendingAction.current
    val onConsumed = LocalSettingsActionConsumed.current

    val hostTouchInput = LocalSettingsHostTouchInput.current
    val notifyTouchInput = {
        onTouchInput()
        hostTouchInput()
    }

    val echoColors = LocalEchoColors.current

    val (scrimTop, scrimBottom) = remember(echoColors.backgroundTop, echoColors.backgroundBottom) {
        crossbarScrimAnchors(echoColors.backgroundTop, echoColors.backgroundBottom)
    }

    val focusedRowClick = remember { mutableStateOf<(() -> Unit)?>(null) }

    val navigationState = remember { ControllerNavigationState() }

    val menuSounds = com.echo.core.ui.sound.LocalMenuSounds.current

    val sliderNodeState = remember { mutableStateOf<SettingsSliderNode?>(null) }

    val lastInputWasTouch = LocalSettingsLastInputWasTouch.current
    val cursorVisible = remember { mutableStateOf(!lastInputWasTouch) }

    val helpText = remember { mutableStateOf<String?>(null) }

    val screenId = LocalSettingsScreenId.current
    val openScreen = LocalSettingsOpenScreen.current
    val tabEntries = remember(screenId, showRail) {
        if (showRail) com.echo.core.domain.model.settingsRailRows(screenId) else emptyList()
    }
    val focusInfo = remember { mutableStateOf<SettingsFocusInfo?>(null) }
    val openTab = { id: String ->
        if (id != screenId) {
            menuSounds(MenuSound.SYSTEM_BROWSE)
            openScreen(id)
        }
    }

    val overlayInput = LocalSettingsOverlayInput.current

    val pickerState = remember { mutableStateOf<SettingsPickerRequest?>(null) }
    val textPromptState = remember { mutableStateOf<SettingsTextRequest?>(null) }
    val pickerCursor = remember { mutableIntStateOf(0) }

    val touchScrolled = remember { mutableStateOf(false) }

    val focusRegistry = remember { mutableStateMapOf<String, FocusRequester>() }

    val firstRowFocus = remember { mutableStateOf<FocusRequester?>(null) }

    remember(contentKey) { firstRowFocus.value = null; contentKey }

    val rowPositions = remember { mutableStateMapOf<FocusRequester, Float>() }

    val rowSizes = remember { mutableStateMapOf<FocusRequester, Float>() }

    val navigationOrder =
        remember { androidx.compose.runtime.mutableStateListOf<Pair<FocusRequester, ControllerNavItem>>() }

    val rowActionFrs = remember {
        mutableStateMapOf<String, SnapshotStateList<Pair<String, FocusRequester>>>()
    }

    var focusedRow by remember { mutableStateOf<FocusRequester?>(null) }

    var userSteered by remember { mutableStateOf(false) }

    LaunchedEffect(navigationOrder, rowPositions) {
        snapshotFlow {
            val sorted = navigationOrder.sortedBy { (fr, _) -> rowPositions[fr] ?: Float.MAX_VALUE }
            val geometry = sorted.mapNotNull { (fr, item) ->
                rowPositions[fr]?.let { item.key to it }
            }.toMap()
            sorted to geometry
        }
            .collect { (entries, geometry) ->
                navigationState.updateItems(entries.map { it.second }, geometry)
                if (!userSteered && restoreFocusKey == null) {
                    entries.firstOrNull { it.second.focusable && it.second.enabled && it.second.selectable }?.first
                        ?.takeIf { it !== focusedRow }
                        ?.let { runCatching { it.requestFocus() } }
                }
            }
    }

    val firstVisibleContentY = remember { mutableStateOf<Float?>(null) }

    val contentViewportHeight = remember { mutableStateOf<Float?>(null) }

    var lastFocusedY by remember { mutableStateOf<Float?>(null) }
    var refocusTick by remember { mutableIntStateOf(0) }

    var focusRedirected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Timber.d("Settings focus: screen opened ($title / $subtitle) restoreKey=$restoreFocusKey")
        var attempts = 0

        while (!focusRedirected && attempts < 30) {
            withFrameNanos {  }
            val target =
                if (restoreFocusKey != null) focusRegistry[restoreFocusKey] else firstRowFocus.value
            if (target != null) {
                runCatching { target.requestFocus() }
            } else {
                runCatching { bootstrapFR.requestFocus() }
            }
            attempts++
        }

        if (!focusRedirected) {
            (firstRowFocus.value ?: bootstrapFR).let { runCatching { it.requestFocus() } }
        }
        Timber.d("Settings focus: default focus assigned=$focusRedirected after $attempts frame(s) ($subtitle)")
    }

    LaunchedEffect(contentKey) {
        if (contentKey == null) return@LaunchedEffect
        withFrameNanos { }
        firstRowFocus.value?.let { runCatching { it.requestFocus() } }
    }

    LaunchedEffect(refocusTick) {
        if (refocusTick == 0) return@LaunchedEffect
        withFrameNanos { }
        val sortedEntries = navigationOrder.sortedBy { (fr, _) -> rowPositions[fr] ?: Float.MAX_VALUE }
        navigationState.updateItems(
            sortedEntries.map { it.second },
            sortedEntries.mapNotNull { (fr, item) ->
                rowPositions[fr]?.let { item.key to it }
            }.toMap(),
        )
        val target = navigationState.focusedKey
            ?.let { key -> navigationOrder.firstOrNull { it.second.key == key }?.first }
            ?: firstRowFocus.value
        target?.let { runCatching { it.requestFocus() } }
        Timber.d("Settings focus: refocused after row removal (key=${navigationState.focusedKey})")
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(focusedRow) {
        val focused = focusedRow ?: return@LaunchedEffect
        withFrameNanos { }
        val y = rowPositions[focused] ?: return@LaunchedEffect
        val viewportTop = firstVisibleContentY.value ?: return@LaunchedEffect
        val viewportHeight = contentViewportHeight.value ?: return@LaunchedEffect
        val activeScrollState = contentScrollState.value ?: return@LaunchedEffect
        val rowHeight = rowSizes[focused] ?: return@LaunchedEffect
        val margin = with(density) { CONTENT_EDGE_MARGIN.toPx() }
        val viewportBottom = viewportTop + viewportHeight

        val target = when {
            y < viewportTop + margin ->
                (activeScrollState.value.toFloat() - (viewportTop + margin - y)).coerceAtLeast(0f)
            y + rowHeight > viewportBottom - margin ->
                activeScrollState.value.toFloat() + ((y + rowHeight) - (viewportBottom - margin))
            else -> null
        }
        target?.let {
            activeScrollState.animateScrollTo(it.toInt().coerceIn(0, activeScrollState.maxValue))
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    fun requestFocusFor(key: String?) {
        val fr = navigationOrder.firstOrNull { it.second.key == key }?.first
            ?: rowActionFrs.entries.firstOrNull { (_, actions) -> actions.any { it.first == key } }
                ?.value?.firstOrNull { it.first == key }?.second
        if (fr != null) {
            runCatching { fr.requestFocus() }
        } else {
            reseedFocus(rowPositions, lastFocusedY, firstRowFocus.value)
        }
    }

    LaunchedEffect(pickerState.value) {
        pickerState.value?.let { pickerCursor.intValue = it.selectedIndex.coerceAtLeast(0) }
    }

    LaunchedEffect(pendingAction) {
        if (pendingAction == null) return@LaunchedEffect
        userSteered = true

        cursorVisible.value = true

        val revivalPress = touchScrolled.value
        if (revivalPress) {
            val viewportTop = firstVisibleContentY.value
            val viewportHeight = contentViewportHeight.value
            val viewportCenter = if (viewportTop != null && viewportHeight != null) {
                viewportTop + viewportHeight / 2f
            } else {
                null
            }
            if (viewportCenter != null) {
                navigationState.focusNearestTo(viewportCenter)
            }
            touchScrolled.value = false
        }
        val focusedKey = navigationState.focusedKey
        if (focusedKey != null) {
            requestFocusFor(focusedKey)
        }
        Timber.d("Settings focus: action=$pendingAction focusedClick=${focusedRowClick.value != null}")

        overlayInput?.value?.let { handle ->
            handle(pendingAction)
            onConsumed()
            return@LaunchedEffect
        }

        if (onInterceptAction?.invoke(pendingAction) == true) {
            onConsumed()
            return@LaunchedEffect
        }

        if (revivalPress && pendingAction.isDirectional) {
            onConsumed()
            return@LaunchedEffect
        }

        if (pendingAction == GamepadAction.PREV_CATEGORY || pendingAction == GamepadAction.NEXT_CATEGORY) {
            val delta = if (pendingAction == GamepadAction.NEXT_CATEGORY) 1 else -1
            com.echo.core.domain.model.settingsTabStepTarget(screenId, delta)?.let(openTab)
            onConsumed()
            return@LaunchedEffect
        }

        val openPicker = pickerState.value
        if (openPicker != null) {
            val count = openPicker.options.size
            when (pendingAction) {
                GamepadAction.NAVIGATE_UP ->
                    pickerCursor.intValue = (pickerCursor.intValue - 1 + count) % count
                GamepadAction.NAVIGATE_DOWN ->
                    pickerCursor.intValue = (pickerCursor.intValue + 1) % count
                GamepadAction.SELECT -> {
                    openPicker.onPick(pickerCursor.intValue)
                    pickerState.value = null
                }
                GamepadAction.BACK -> pickerState.value = null
                else -> Unit
            }
            onConsumed()
            return@LaunchedEffect
        }

        val adjustingSlider = sliderNodeState.value
        if (adjustingSlider != null) {
            when (pendingAction) {
                GamepadAction.NAVIGATE_LEFT  -> adjustingSlider.onStep(-1)
                GamepadAction.NAVIGATE_RIGHT -> adjustingSlider.onStep(1)
                GamepadAction.SELECT,
                GamepadAction.BACK -> sliderNodeState.value = null
                GamepadAction.NAVIGATE_UP,
                GamepadAction.NAVIGATE_DOWN -> sliderNodeState.value = null
                else -> Unit
            }
            if (pendingAction != GamepadAction.NAVIGATE_UP &&
                pendingAction != GamepadAction.NAVIGATE_DOWN
            ) {
                onConsumed()
                return@LaunchedEffect
            }
        }
        when (pendingAction) {
            GamepadAction.NAVIGATE_UP -> {
                val previous = navigationState.focusedKey
                val target = navigationState.move(-1)

                if (target != null && target != previous) menuSounds(MenuSound.SCROLL)

                if (target != null && target == previous) {
                    val topScrollState = contentScrollState.value
                    if (topScrollState != null) {
                        coroutineScope.launch { topScrollState.animateScrollTo(0) }
                    }
                }
                requestFocusFor(target)
            }

            GamepadAction.NAVIGATE_DOWN -> {
                val previous = navigationState.focusedKey
                val target = navigationState.move(1)
                if (target != null && target != previous) menuSounds(MenuSound.SCROLL)
                requestFocusFor(target)
            }

            GamepadAction.NAVIGATE_LEFT -> {
                val target = navigationState.moveHorizontal(-1)
                when {
                    target != null -> { menuSounds(MenuSound.SCROLL); requestFocusFor(target) }
                    leftBacksOut -> { menuSounds(MenuSound.BACK); onBack() }
                }
            }

            GamepadAction.NAVIGATE_RIGHT -> {
                navigationState.moveHorizontal(1)?.let { menuSounds(MenuSound.SCROLL); requestFocusFor(it) }
            }

            GamepadAction.SELECT -> {
                menuSounds(MenuSound.SELECT)

                if (!navigationState.select()) focusedRowClick.value?.invoke()
            }

            GamepadAction.BACK -> {
                menuSounds(MenuSound.BACK)
                onBack()
            }

            else -> Unit
        }
        onConsumed()
    }

    CompositionLocalProvider(

        LocalSettingsCursorVisible provides cursorVisible.value,

        LocalSettingsFocusTracker provides { click ->
            focusedRowClick.value = click; focusRedirected = true
        },
        LocalSettingsTouchInput provides {
            userSteered = true
            cursorVisible.value = false
            touchScrolled.value = true

            sliderNodeState.value = null
            notifyTouchInput()
        },
        LocalSettingsHelp provides helpText,
        LocalSettingsPicker provides pickerState,
        LocalSettingsTextPrompt provides textPromptState,
        LocalSettingsFocusRegistry provides focusRegistry,

        LocalSettingsRegisterFirstFocusable provides { fr ->
            if (firstRowFocus.value == null) firstRowFocus.value = fr
        },
        LocalSettingsRowPositions provides rowPositions,
        LocalSettingsRowSizes provides rowSizes,
        LocalSettingsNavigationOrder provides navigationOrder,
        LocalSettingsReportFocused provides { fr ->
            focusedRow = fr
            rowPositions[fr]?.let { lastFocusedY = it }

            val key = navigationOrder.firstOrNull { it.first === fr }?.second?.key
                ?: rowActionFrs.entries.firstOrNull { (_, actions) -> actions.any { it.second === fr } }
                    ?.let { (_, actions) -> actions.firstOrNull { it.second === fr }?.first }
            if (key != null) navigationState.setFocused(key)
        },
        LocalSettingsReportRemoved provides { fr ->
            if (focusedRow == fr) {
                focusedRow = null
                refocusTick++
            }
        },
        LocalSettingsEnterSliderMode provides { node -> sliderNodeState.value = node },
        LocalSettingsSliderAdjusting provides (sliderNodeState.value != null),
        LocalSettingsRowActions provides rowActionFrs,
        LocalSettingsScrollStateRegistrar provides { state -> contentScrollState.value = state },
    ) {
        BoxWithConstraints(
            modifier = modifier

                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        cursorVisible.value = false
                        touchScrolled.value = true
                        notifyTouchInput()
                    }
                }
                .fillMaxSize()

                .then(
                    if (backdrop != null) {
                        Modifier
                    } else if (panelTint != null) {
                        Modifier.panelBackdrop(panelTint)
                    } else {
                        Modifier.background(Brush.verticalGradient(0f to scrimTop, 1f to scrimBottom))
                    }
                ),
        ) {
            val u = panelDesignUnits(maxWidth.value, maxHeight.value, density)
            val tabs = tabEntries.isNotEmpty()
            val paneShown = !fullWidth && (tabs || paneText != null) && maxWidth - SETTINGS_COLUMN_MAX_WIDTH >= SETTINGS_HELP_PANE_MIN_WIDTH
            backdrop?.invoke()
            // the wave shows inside the panel's glow, on the left (owner, 2026-10-04)
            if (backdrop == null && panelTint != null) GlowMaskedWave(GlowSide.LEFT)
            Column(
                modifier = Modifier
                    .fillMaxSize()

                    .imePadding(),
            ) {
                Spacer(Modifier.height(StatusStripHeight))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragToScroll(contentScrollState.value),
                ) {
                if (tabs) {
                    com.echo.core.ui.components.EchoTabRow(
                        labels = tabEntries.map { it.title },
                        current = tabEntries.indexOfFirst { it.id == screenId }.coerceAtLeast(0),
                        u = u,
                        onBack = onBack,
                        onPick = { i -> notifyTouchInput(); openTab(tabEntries[i].id) },
                    )
                } else if (header != null) {
                    header()
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties { canFocus = false }
                        .padding(horizontal = 48.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null,
                        ) { onBack() },
                    ) {
                        Text(
                            text = "◀",
                            color = SettingsSubtext,
                            fontSize = 18.sp,
                            style = EchoTextStyle.copy(shadow = SettingsTextShadow),
                            modifier = Modifier.padding(end = 20.dp),
                        )
                        Column {
                            Text(
                                text = title.uppercase(),
                                color = SettingsAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                style = EchoTextStyle.copy(shadow = SettingsTextShadow),
                            )
                            Text(
                                text = subtitle,
                                color = SettingsText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light,
                                style = EchoTextStyle.copy(shadow = SettingsTextShadow),
                            )
                        }
                    }
                }
                }

                if (showDivider && !tabs) HorizontalDivider(color = SettingsDivider)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                        .focusRequester(bootstrapFR)
                        .focusable()
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                focusManager.moveFocus(FocusDirection.Down)
                                focusRedirected = true
                                Timber.d("Settings focus: default focus → first item ($subtitle)")
                            }
                        }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag(SettingsContentViewportTag)

                        .onGloballyPositioned {
                            firstVisibleContentY.value = it.localToRoot(Offset.Zero).y
                            contentViewportHeight.value = it.size.height.toFloat()
                        }

                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val fade = CONTENT_EDGE_MARGIN.toPx().coerceAtMost(size.height)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startY = size.height - fade,
                                    endY = size.height,
                                ),
                                topLeft = Offset(0f, size.height - fade),
                                size = Size(size.width, fade),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(modifier = if (fullWidth) Modifier.weight(1f) else Modifier.widthIn(max = SETTINGS_COLUMN_MAX_WIDTH)) {
                            // every settings page draws its rows as the kit's rail rows (owner, 2026-10-04),
                            // as the first-run wizard does
                            CompositionLocalProvider(
                                LocalSettingsFocusInfo provides focusInfo.takeIf { paneShown },
                                LocalSettingsRailUnits provides (LocalSettingsRailUnits.current ?: u),
                            ) {
                                content()
                            }
                        }
                        if (paneShown) {
                            SettingsHelpPane(
                                info = focusInfo.value?.takeIf { cursorVisible.value },
                                page = paneText ?: tabPaneText(screenId, tabEntries),
                                u = u,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragToScroll(contentScrollState.value),
                ) {
                    if (footer == null) {
                        val help = helpText.value?.takeIf { cursorVisible.value && it.isNotBlank() && !paneShown }
                        val hints = if (LocalSettingsShowControllerHint.current) {
                            helperFooterItems.ifEmpty { SettingsDefaultHelperItems }
                        } else emptyList()
                        EchoHintBar(

                            items = hints,
                            onAction = LocalSettingsPromptAction.current,
                            primary = primaryHint(hints, focusInfo.value?.takeIf { cursorVisible.value }?.label),
                            centre = help?.let {
                                {
                                    Text(
                                        text = it,
                                        color = SettingsSubtext,
                                        fontSize = SETTINGS_HELP_TEXT_SP.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = EchoTextStyle.copy(shadow = SettingsTextShadow),
                                    )
                                }
                            },
                            modifier = Modifier.focusProperties { canFocus = false },
                        )
                    }
                    if (footer != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { canFocus = false },
                        ) {
                            footer()
                        }
                    }
                }
            }

            pickerState.value?.let { picker ->
                SettingsPickerPanel(
                    picker = picker,
                    cursor = pickerCursor.intValue,
                    onDismiss = { pickerState.value = null },
                )
            }
            textPromptState.value?.let { request ->
                var draft by remember(request) { mutableStateOf(request.value) }
                SettingsTextPromptOverlay(
                    title = request.title,
                    value = draft,
                    onValueChange = { draft = it },
                    onConfirm = { request.onDone(draft); textPromptState.value = null },
                    onCancel = { textPromptState.value = null },
                    placeholder = request.placeholder,
                    isPassword = request.isPassword,
                )
            }
        }
    }
}

// a picker is the kit's side rail, as every other choice list (owner, 2026-10-08: the floating dropdown
// was the last of the old style); the current choice is ticked
internal fun SettingsPickerRequest.menu(cursor: Int): com.echo.core.ui.components.MenuState<Int> =
    com.echo.core.ui.components.MenuState(
        title = title,
        rows = options.mapIndexed { i, option ->
            com.echo.core.ui.components.MenuRow(i, option.label, checked = i == selectedIndex)
        },
        selectedIndex = cursor,
    )

@Composable
private fun SettingsPickerPanel(picker: SettingsPickerRequest, cursor: Int, onDismiss: () -> Unit) {
    com.echo.core.ui.components.EchoContextMenuOverlay(
        state = picker.menu(cursor),
        onRowActivated = { index -> picker.onPick(index); onDismiss() },
        onDismiss = onDismiss,
    )
}

data class SettingsPaneText(val eyebrow: String, val title: String, val body: String?)

private fun tabPaneText(screenId: String?, tabEntries: List<com.echo.core.domain.model.SettingsEntry>): SettingsPaneText {
    val entry = tabEntries.firstOrNull { it.id == screenId }
    val position = tabEntries.indexOfFirst { it.id == screenId } + 1
    return SettingsPaneText(
        eyebrow = "${entry?.section?.title.orEmpty()} · $position of ${tabEntries.size}",
        title = entry?.title.orEmpty(),
        body = entry?.subtitle,
    )
}

@Composable
private fun SettingsHelpPane(
    info: SettingsFocusInfo?,
    page: SettingsPaneText,
    u: DesignUnits,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .focusProperties { canFocus = false }
            .padding(start = u.dp(48), end = u.dp(64), top = u.dp(32)),
        verticalArrangement = Arrangement.spacedBy(u.dp(16)),
    ) {
        Text(
            text = page.eyebrow.uppercase(),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = u.sp(13),
            letterSpacing = 0.18.em,
        )
        Text(
            text = info?.label ?: page.title,
            color = Color.White,
            fontSize = u.sp(40),
            lineHeight = u.sp(44),
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-0.02).em,
        )
        info?.value?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, color = Color.White, fontSize = u.sp(22), fontWeight = FontWeight.Light)
        }
        (if (info != null) info.sublabel else page.body)?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = u.sp(15),
                lineHeight = u.sp(23),
                fontWeight = FontWeight.Light,
            )
        }
    }
}


@Composable
fun SettingsGroup(title: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, top = 24.dp, bottom = 6.dp),
        text = title.uppercase(),
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.14.em,
    )
}

class SettingsRowAction(
    val label: String,
    val onClick: () -> Unit,
    val onLongPress: (() -> Unit)? = null,

    val actionFocusBackgroundColor: Color = Color.White.copy(alpha = 0.25f),
    val icon: @Composable () -> Unit,
)

private const val DISABLED_ROW_ALPHA = 0.4f

@Composable
fun SettingsRow(
    label: String,
    sublabel: String? = null,

    value: String? = null,
    focusKey: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,

    actions: List<SettingsRowAction> = emptyList(),

    onFocusChangedExternal: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,

    hideRowHighlightOnActionFocus: Boolean = false,

    enabled: Boolean = true,
) {
    val click = onClick?.takeIf { enabled }
    val help = LocalSettingsHelp.current
    val actionFocusCount = remember { mutableIntStateOf(0) }
    val anyActionFocused = actionFocusCount.intValue > 0
    val focusTracker = LocalSettingsFocusTracker.current
    val touchInput = LocalSettingsTouchInput.current
    val cursorVisible = LocalSettingsCursorVisible.current
    val reportFocused = LocalSettingsReportFocused.current
    val focusInfo = LocalSettingsFocusInfo.current
    var isFocused by remember { mutableStateOf(false) }
    if (isFocused && focusInfo != null) {
        DisposableEffect(label, value, sublabel) {
            val info = SettingsFocusInfo(label, value, sublabel)
            focusInfo.value = info
            onDispose { if (focusInfo.value == info) focusInfo.value = null }
        }
    }

    val row = rememberControllerRowRegistration(
        prefix = "row",
        focusKey = focusKey,
        claimInitialFocus = click != null,
        selectable = click != null,
        onSelect = click,
        trailingActionsFor = { rowKey ->
            actions.mapIndexed { index, action ->
                ControllerNavItem(
                    key = "$rowKey:action:$index",
                    focusable = true,
                    selectable = true,
                    enabled = true,
                    onSelect = action.onClick,
                )
            }
        },
    )

    val rowSelected =
        isFocused && cursorVisible && !(hideRowHighlightOnActionFocus && anyActionFocused)
    val rail = LocalSettingsRailUnits.current
    val ink = if (rail != null) contextMenuInk(rowSelected) else Color.White
    val textSize = rail?.sp(17) ?: CrossbarLayoutSpec.DEFAULT.itemTextSp.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(row.focusRequester)

            .then(row.positionReporting)

            .pointerInput(row.rowKey, click, onLongPress) {
                detectTapGestures(
                    onTap = { touchInput(); click?.invoke() },
                    onLongPress = { touchInput(); onLongPress?.invoke() },
                )
            }
            .onFocusChanged { state ->
                isFocused = state.isFocused
                onFocusChangedExternal?.invoke(state.isFocused)
                if (state.isFocused) {
                    focusTracker(click)
                    reportFocused(row.focusRequester)

                    help.value = null
                    Timber.d("Settings focus: row=\"$label\" clickable=${click != null}")
                }
            }

            .then(
                if (rail != null) {
                    Modifier
                        .padding(horizontal = 40.dp, vertical = 1.dp)
                        .clip(RoundedCornerShape(rail.dp(12)))
                        .background(if (rowSelected) Color.White else Color.Transparent)
                        .focusable()
                        .padding(horizontal = rail.dp(14), vertical = rail.dp(9))
                } else {
                    Modifier
                        .padding(horizontal = 40.dp, vertical = 3.dp)
                        .settingsSelectedPlate(rowSelected)
                        .focusable()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                },
            ),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = ink
                    .let { if (enabled) it else it.copy(alpha = it.alpha * DISABLED_ROW_ALPHA) },

                fontSize = textSize,
                fontWeight = if (rowSelected) FontWeight.Medium else FontWeight.Normal,
            )
        }
        if (value != null) {
            Spacer(Modifier.width(24.dp))
            Text(
                text = value,

                color = ink.copy(alpha = if (rowSelected) 0.9f else 0.6f)
                    .let { if (enabled) it else it.copy(alpha = it.alpha * DISABLED_ROW_ALPHA) },

                fontSize = textSize,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
        if (actions.isNotEmpty()) {
            Spacer(Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                actions.forEachIndexed { index, action ->
                    key(index) {
                        SettingsRowActionButton(
                            rowKey = row.rowKey,
                            index = index,
                            action = action,
                            onFocusedChanged = { focused ->
                                if (focused) actionFocusCount.intValue++
                                else actionFocusCount.intValue--
                            },
                        )
                    }
                }
            }
        }
      }

        if (rowSelected && focusInfo == null && !sublabel.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = sublabel,
                color = ink.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    sublabel: String? = null,
    focusKey: String? = null,
    leading: @Composable (() -> Unit)? = null,
    onFocusChangedExternal: ((Boolean) -> Unit)? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    SettingsRow(
        label = label,
        sublabel = sublabel,
        focusKey = focusKey,
        leading = leading,
        onFocusChangedExternal = onFocusChangedExternal,

        onClick = { onToggle(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PanelBase,
                    checkedTrackColor = Color.White,
                    checkedBorderColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = PanelCardFill,
                    uncheckedBorderColor = Color.White.copy(alpha = 0.3f),
                ),
            )
        },
    )
}

@Composable
fun SettingsValueRow(
    label: String,
    value: String,
    sublabel: String? = null,
    focusKey: String? = null,
    onFocusChangedExternal: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        label = label,
        sublabel = sublabel,
        value = value,
        focusKey = focusKey,
        onFocusChangedExternal = onFocusChangedExternal,
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
fun SettingsPickerRow(
    label: String,
    options: List<SettingsPickerOption>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    sublabel: String? = null,
    focusKey: String? = null,
    enabled: Boolean = true,
) {
    val picker = LocalSettingsPicker.current

    SettingsRow(
        label = label,
        sublabel = sublabel,
        value = options.getOrNull(selectedIndex)?.label ?: "",
        focusKey = focusKey,
        enabled = enabled,
        onClick = {
            picker.value = SettingsPickerRequest(
                title = label,
                options = options,
                selectedIndex = selectedIndex,
                onPick = onPick,
            )
        },
    )
}

// a job in progress, as a settings row: done / total when the count is known, "Working…" when it is not
// (owner, 2026-10-07: the kit's rows, no raw progress bars)
@Composable
fun SettingsProgressRow(label: String, done: Int? = null, total: Int? = null, sublabel: String? = null) {
    SettingsValueRow(
        label = label,
        value = if (done != null && total != null && total > 0) "$done / $total" else "Working…",
        sublabel = sublabel,
    )
}

// a value row; A (or a tap) opens the kit's rail prompt to edit it, and Save hands the text to [onValueChange]
@Composable
fun SettingsTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    focusKey: String? = null,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    helper: String? = null,
    enabled: Boolean = true,
) {
    val prompt = LocalSettingsTextPrompt.current
    val shown = when {
        value.isBlank() -> placeholder.ifBlank { "Not set" }
        isPassword -> "••••••••"
        singleLine -> value
        else -> value.lineSequence().first()
    }
    SettingsValueRow(
        label = label,
        value = shown,
        sublabel = helper,
        focusKey = focusKey,
        enabled = enabled,
        onClick = { prompt.value = SettingsTextRequest(label, value, placeholder, isPassword, onValueChange) },
    )
}
