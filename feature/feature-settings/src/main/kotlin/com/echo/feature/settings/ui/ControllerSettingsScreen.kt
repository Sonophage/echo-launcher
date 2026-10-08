package com.echo.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.echo.core.domain.model.ConfirmBackLayout
import com.echo.core.domain.model.ControllerDisplayType
import com.echo.core.domain.model.XYLayout
import com.echo.core.domain.model.ScrollSpeed
import com.echo.core.domain.model.StickSensitivity
import com.echo.core.domain.model.displayLabel
import com.echo.feature.settings.viewmodel.ControllerSettingsViewModel

@Composable
fun ControllerSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ControllerSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }

    SettingsPageScaffold(
        subtitle = "Controller",
        onBack   = onBack,
        modifier = modifier,
    ) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            SettingsGroup("A / B Swap")

            SettingsPickerRow(
                label    = "A / B Swap",
                sublabel = "Which button confirms and which goes back, in every launcher menu · " +
                    state.layoutPrefs.confirmBackLayout.displayLabel(),
                options  = ConfirmBackLayout.entries.map {
                    SettingsPickerOption(
                        label = if (it == ConfirmBackLayout.STANDARD) "Off" else "On",
                        help  = it.displayLabel(),
                    )
                },
                selectedIndex = ConfirmBackLayout.entries.indexOf(state.layoutPrefs.confirmBackLayout),
                onPick   = { viewModel.setConfirmBackLayout(ConfirmBackLayout.entries[it]) },
            )

            SettingsGroup("X / Y Swap")

            SettingsPickerRow(
                label    = "X / Y Swap",
                sublabel = "Swaps X and Y in the launcher only, never in emulators · " + state.layoutPrefs.xyLayout.displayLabel(),
                options  = XYLayout.entries.map {
                    SettingsPickerOption(
                        label = if (it == XYLayout.STANDARD) "Off" else "On",
                        help  = it.displayLabel(),
                    )
                },
                selectedIndex = XYLayout.entries.indexOf(state.layoutPrefs.xyLayout),
                onPick   = { viewModel.setXYLayout(XYLayout.entries[it]) },
            )

            SettingsGroup("Controller Type")

            SettingsPickerRow(
                label    = "Type",
                sublabel = "Which button icons and labels the help bar at the bottom shows",
                options  = ControllerDisplayType.entries.map { SettingsPickerOption(it.displayLabel()) },
                selectedIndex = ControllerDisplayType.entries.indexOf(state.layoutPrefs.displayType),
                onPick   = { viewModel.setDisplayType(ControllerDisplayType.entries[it]) },
            )

            SettingsGroup("Stick")

            SettingsPickerRow(
                label    = "Stick Sensitivity",
                sublabel = "How far the stick moves before it navigates, and before it counts as a full tilt. " +
                    "Low needs a firmer push and reaches top speed later",
                options  = StickSensitivity.entries.map { SettingsPickerOption(it.displayLabel()) },
                selectedIndex = StickSensitivity.entries.indexOf(state.layoutPrefs.stickSensitivity),
                onPick   = { viewModel.setStickSensitivity(StickSensitivity.entries[it]) },
            )

            SettingsGroup("Triggers & Shoulders")
            SettingsPickerRow(
                label    = "Trigger Sensitivity",
                sublabel = "How far L2 and R2 must be pulled to turn a page. High suits short or worn triggers",
                options  = com.echo.core.domain.model.TriggerSensitivity.entries.map { SettingsPickerOption(it.label) },
                selectedIndex = com.echo.core.domain.model.TriggerSensitivity.entries
                    .indexOf(state.layoutPrefs.triggerSensitivity),
                onPick   = { viewModel.setTriggerSensitivity(com.echo.core.domain.model.TriggerSensitivity.entries[it]) },
            )
            SettingsPickerRow(
                label    = "Shoulder Hold Time",
                sublabel = "How long L1 or R1 must be held before it counts as a hold rather than a press",
                options  = com.echo.core.domain.model.ShoulderHoldTime.entries.map {
                    SettingsPickerOption("${it.label} (${it.millis} ms)")
                },
                selectedIndex = com.echo.core.domain.model.ShoulderHoldTime.entries
                    .indexOf(state.layoutPrefs.shoulderHoldTime),
                onPick   = { viewModel.setShoulderHoldTime(com.echo.core.domain.model.ShoulderHoldTime.entries[it]) },
            )

            SettingsGroup("Scroll Speed")

            SettingsPickerRow(
                label    = "Scroll Speed",
                sublabel = "How fast held D-pad and stick navigation scrolls. Holding longer speeds it up, " +
                    "and a full stick tilt goes twice as fast",
                options  = ScrollSpeed.entries.map { SettingsPickerOption(it.displayLabel()) },
                selectedIndex = ScrollSpeed.entries.indexOf(state.layoutPrefs.scrollSpeed),
                onPick   = { viewModel.setScrollSpeed(ScrollSpeed.entries[it]) },
            )

            SettingsGroup("Navigation")
            SettingsToggleRow(
                label    = "Left Backs Out",
                sublabel = "Press LEFT to leave a folder, flyout or settings screen — " +
                    "only where LEFT does nothing else",
                checked  = state.layoutPrefs.leftBacksOut,
                onToggle = { viewModel.setLeftBacksOut(it) },
            )

            SettingsGroup("Reset")
            SettingsRow(
                label    = "Reset All Controller Settings",
                sublabel = "Returns every controller setting to its default",
                onClick  = { confirmReset = true },
            )
        }
    }

    if (confirmReset) {
        SettingsConfirmOverlay(
            title = "Reset Controller Settings?",
            message = "Every controller setting returns to its default.",
            confirmLabel = "Reset",
            onConfirm = { confirmReset = false; viewModel.resetToDefaults() },
            onCancel = { confirmReset = false },
        )
    }
}
