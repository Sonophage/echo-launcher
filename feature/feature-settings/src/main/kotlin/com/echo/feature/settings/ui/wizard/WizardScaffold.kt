package com.echo.feature.settings.ui.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.echo.feature.settings.ui.SettingsRow
import com.echo.feature.settings.ui.SettingsPageTitle
import com.echo.feature.settings.ui.SettingsPaneText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.sound.LocalMenuSounds
import com.echo.core.ui.sound.MenuSound
import com.echo.core.domain.model.GamepadAction
import com.echo.feature.settings.ui.LocalSettingsScrollStateRegistrar
import com.echo.feature.settings.ui.SettingsScaffold
import com.echo.feature.settings.ui.LocalSettingsRailUnits
import com.echo.core.ui.design.LocalPanelTextScale
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.LocalEchoColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity



@Composable
fun WizardScaffold(

    stepNumber: Int?,

    stepCount: Int = 0,
    title: String,

    heading: String,

    hint: String? = null,
    onBack: () -> Unit,

    backEnabled: Boolean = true,

    onSkip: (() -> Unit)? = null,

    message: String? = null,
    onDismissMessage: (() -> Unit)? = null,

    contentKey: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val menuSounds = LocalMenuSounds.current
    val skip by rememberUpdatedState(onSkip)
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val textScale = LocalPanelTextScale.current
    val railUnits = with(density) { panelDesignUnits(window.width.toDp().value, window.height.toDp().value, density, textScale) }
    val step = if (stepNumber != null && stepCount > 0) "$title · Step $stepNumber of $stepCount" else title
    SettingsScaffold(
        title = title,
        subtitle = "",
        onBack = onBack,
        modifier = modifier,

        // the kit's accent and wave, not the old orange Setup tint (owner, 2026-10-04)
        panelTint = LocalEchoColors.current.accentColor,

        showRail = false,
        // the heading and the step are drawn once, top left; the pane carries the step's hint, or the focused row's
        // reason, with no eyebrow of its own (it repeated "Setup · Step 1 of 5")
        paneText = SettingsPaneText(eyebrow = "", title = hint ?: heading, body = null),
        header = { SettingsPageTitle(step, heading) },
        // the kit footer every settings page uses, with the wizard's own actions
        helperFooterItems = listOfNotNull(
            ControllerPromptItem(GamepadAction.SELECT, "Enter"),
            ControllerPromptItem(GamepadAction.BACK, "Back").takeIf { backEnabled },
            ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Skip Setup").takeIf { onSkip != null },
        ),

        onInterceptAction = { action ->
            if (action == GamepadAction.OPEN_CONTEXT_MENU && skip != null) {
                menuSounds(MenuSound.BACK)
                skip?.invoke()
                true
            } else {
                false
            }
        },
        contentKey = contentKey,
    ) {
        val scrollState = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scrollState)

        LaunchedEffect(contentKey) { scrollState.scrollTo(0) }

        var pagesSeen by remember { mutableIntStateOf(0) }
        LaunchedEffect(contentKey) {
            if (pagesSeen > 0) menuSounds(MenuSound.SYSTEM_BROWSE)
            pagesSeen++
        }
        CompositionLocalProvider(LocalSettingsRailUnits provides railUnits) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            if (message != null && onDismissMessage != null) {
                SettingsRow(label = message, sublabel = "Select to dismiss", onClick = onDismissMessage)
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}
