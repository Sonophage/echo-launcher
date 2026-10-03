package com.psplauncher.feature.xmb.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.components.ControllerPromptItem
import com.psplauncher.core.ui.components.HintAction
import com.psplauncher.core.ui.components.PfpHintBar
import com.psplauncher.feature.xmb.viewmodel.XmbPrompt
import com.psplauncher.feature.xmb.viewmodel.XmbPrompts

@Composable
fun XmbHintBar(
    prompts: XmbPrompts,
    modifier: Modifier = Modifier,
    onAction: ((GamepadAction) -> Unit)? = null,
) {
    PfpHintBar(
        items = buildList {
            add(prompts.back.item())
            prompts.right.forEach { add(it.item()) }
        },
        modifier = modifier,
        onAction = onAction,
        primary = prompts.primary?.let { p ->
            HintAction(p.action, p.verb, listOfNotNull(p.target, p.detail).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null })
        },
        position = prompts.position,
    )
}

private fun XmbPrompt.item() =
    ControllerPromptItem(listOfNotNull(action, pairedWith), target?.takeIf { it.isNotBlank() }?.let { "$verb  $it" } ?: verb)
