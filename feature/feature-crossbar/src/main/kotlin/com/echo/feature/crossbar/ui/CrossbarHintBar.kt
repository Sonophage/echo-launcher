package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.EchoHintBar
import com.echo.feature.crossbar.viewmodel.CrossbarPrompt
import com.echo.feature.crossbar.viewmodel.CrossbarPrompts

@Composable
fun CrossbarHintBar(
    prompts: CrossbarPrompts,
    modifier: Modifier = Modifier,
    onAction: ((GamepadAction) -> Unit)? = null,
    accent: Color = LocalEchoColors.current.accentColor,
    leading: (@Composable () -> Unit)? = null,

    // over 0, A must be held this long; holding is true while the ring fills
    holdMs: Long = 0L,
    holding: Boolean = false,
    resumeHolding: Boolean = false,
    onResume: () -> Unit = {},
) {
    EchoHintBar(
        items = buildList {
            // kit hint row: Home (the Echo mark) leads, then Back
            add(ControllerPromptItem(listOf(GamepadAction.HOME), "Home"))
            add(prompts.back.item())
            prompts.right.forEach { add(it.item()) }
        },
        modifier = modifier,
        onAction = onAction,
        accent = accent,
        leading = leading.takeIf { prompts.resume == null },
        primary = prompts.primary?.let { p ->
            val detail = if (prompts.resume != null) p.target
                else if (holdMs > 0L) listOfNotNull("Hold to launch", p.target).filter { it.isNotBlank() }.joinToString(" ")
                else listOfNotNull(p.target, p.detail).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null }
            HintAction(p.action, p.verb, detail, holdMs, holding)
        },
        secondary = prompts.resume?.let { HintAction(it.action, it.verb, it.target, LAUNCH_HOLD_MS, resumeHolding, onTouch = onResume) },
    )
}

private fun CrossbarPrompt.item() =
    ControllerPromptItem(listOfNotNull(action, pairedWith), target?.takeIf { it.isNotBlank() }?.let { "$verb  $it" } ?: verb)
