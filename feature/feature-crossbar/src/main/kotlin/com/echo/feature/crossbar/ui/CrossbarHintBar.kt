package com.echo.feature.crossbar.ui

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
) {
    EchoHintBar(
        items = buildList {
            add(prompts.back.item())
            prompts.right.forEach { add(it.item()) }
        },
        modifier = modifier,
        onAction = onAction,
        accent = accent,
        leading = leading,
        primary = prompts.primary?.let { p ->
            HintAction(p.action, p.verb, listOfNotNull(p.target, p.detail).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null })
        },
    )
}

private fun CrossbarPrompt.item() =
    ControllerPromptItem(listOfNotNull(action, pairedWith), target?.takeIf { it.isNotBlank() }?.let { "$verb  $it" } ?: verb)
