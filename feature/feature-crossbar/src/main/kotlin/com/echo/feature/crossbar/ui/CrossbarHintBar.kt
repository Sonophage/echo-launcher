package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.TriggerFilter
import com.echo.feature.crossbar.viewmodel.CrossbarPrompt
import com.echo.feature.crossbar.viewmodel.CrossbarPrompts
import com.echo.feature.crossbar.viewmodel.sortRow

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
    // the filter row, after Home and Back
    filters: (@Composable () -> Unit)? = null,
) {
    EchoHintBar(
        // owner, 2026-10-06: no Home hint (the guide button, or Select held, still goes home); the filter leads the
        // left side, and LB sits right beside RB
        items = buildList {
            val bumper = prompts.back.action == GamepadAction.PREV_PAGE
            if (!bumper) add(prompts.back.item())
            prompts.right.filter { it.action != GamepadAction.NEXT_PAGE }.forEach { add(it.item()) }
            if (bumper) add(prompts.back.item())
            prompts.right.filter { it.action == GamepadAction.NEXT_PAGE }.forEach { add(it.item()) }
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
        filter = filters,
    )
}

// owner, 2026-10-06: the footer's left side shows the current filter or sort as one LT/RT mark and its
// word; the triggers step it, a tap steps to the next. Nothing while a menu or another screen is over the crossbar
internal fun crossbarFooterFilters(
    state: com.echo.feature.crossbar.viewmodel.CrossbarUiState,
    onRecentFilter: (com.echo.feature.crossbar.viewmodel.RecentFilter) -> Unit,
    onSort: (com.echo.feature.crossbar.viewmodel.CrossbarSortMode) -> Unit,
): (@Composable () -> Unit)? {
    val (label, next) = footerFilter(state, onRecentFilter, onSort) ?: return null
    return { TriggerFilter(label, onTapped = next) }
}

// the word the footer's filter shows, and what a tap does; null when the screen has none
internal fun footerFilter(
    state: com.echo.feature.crossbar.viewmodel.CrossbarUiState,
    onRecentFilter: (com.echo.feature.crossbar.viewmodel.RecentFilter) -> Unit = {},
    onSort: (com.echo.feature.crossbar.viewmodel.CrossbarSortMode) -> Unit = {},
): Pair<String, () -> Unit>? {
    // the open panel's tabs are its own tab row (owner, 2026-10-07)
    if (state.notificationsOpen || state.hasBlockingOverlay) return null
    if (state.onLastPlayedHome) {
        val filters = state.recentFilters
        val at = filters.indexOf(state.recentFilter).coerceAtLeast(0)
        return filters[at].label to { onRecentFilter(filters[(at + 1) % filters.size]) }
    }
    val (modes, active) = state.sortRow() ?: return null
    val at = modes.indexOf(active).coerceAtLeast(0)
    return modes[at].label to { onSort(modes[(at + 1) % modes.size]) }
}

private fun CrossbarPrompt.item() =
    ControllerPromptItem(listOfNotNull(action, pairedWith), target?.takeIf { it.isNotBlank() }?.let { "$verb  $it" } ?: verb, icon = bumperIcon(action))

// owner, 2026-10-06: LB and RB show an icon, not a word, so the footer stays short
private fun bumperIcon(action: GamepadAction): androidx.compose.ui.graphics.vector.ImageVector? = when (action) {
    GamepadAction.PREV_PAGE -> Icons.Outlined.Apps
    GamepadAction.NEXT_PAGE -> Icons.Outlined.Search
    else -> null
}

