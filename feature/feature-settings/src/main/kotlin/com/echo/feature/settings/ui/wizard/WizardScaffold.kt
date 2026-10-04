package com.echo.feature.settings.ui.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.echo.feature.settings.ui.SettingsRow
import com.echo.feature.settings.ui.SettingsPageTitle
import com.echo.feature.settings.ui.SettingsPaneText
import com.echo.core.domain.model.SettingsSectionId
import com.echo.core.ui.design.panelSectionTint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.ui.components.ControllerHintStyle
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoControllerHints
import com.echo.core.ui.sound.LocalMenuSounds
import com.echo.core.ui.sound.MenuSound
import com.echo.core.domain.model.GamepadAction
import com.echo.feature.settings.ui.LocalSettingsPromptAction
import com.echo.feature.settings.ui.LocalSettingsScrollStateRegistrar
import com.echo.feature.settings.ui.SettingsScaffold



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

    footerNote: String? = null,

    contentKey: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val menuSounds = LocalMenuSounds.current
    val skip by rememberUpdatedState(onSkip)
    SettingsScaffold(
        title = title,
        subtitle = "",
        onBack = onBack,
        modifier = modifier,

        panelTint = panelSectionTint(SettingsSectionId.SETUP),

        showRail = false,
        paneText = SettingsPaneText(
            eyebrow = if (stepNumber != null && stepCount > 0) "$title · Step $stepNumber of $stepCount" else title,
            title = heading,
            body = hint,
        ),
        header = { SettingsPageTitle(if (stepNumber != null && stepCount > 0) "$title · Step $stepNumber of $stepCount" else title, heading) },
        footer = { WizardFooter(backEnabled, onSkip != null, footerNote) },

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





@Composable
private fun WizardFooter(backEnabled: Boolean, skippable: Boolean, note: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (note != null) {
            Text(
                text = note,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        EchoControllerHints(
            items = listOfNotNull(
                ControllerPromptItem(GamepadAction.SELECT, "Enter"),
                ControllerPromptItem(GamepadAction.BACK, "Back").takeIf { backEnabled },
                ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Skip Setup")
                    .takeIf { skippable },
            ),
            style = ControllerHintStyle.INLINE,

            onAction = LocalSettingsPromptAction.current,
        )
    }
}




