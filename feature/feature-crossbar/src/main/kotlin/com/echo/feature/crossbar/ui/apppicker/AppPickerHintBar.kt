package com.echo.feature.crossbar.ui.apppicker

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.ControllerIcon
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.theme.StorefrontColors

@Composable
internal fun AppPickerHintBar(
    title: String,
    selectedCount: Int,
    confirmingRemovals: Boolean,
    colors: StorefrontColors,
    modifier: Modifier = Modifier,
    onAction: ((GamepadAction) -> Unit)? = null,
) {
    val items = if (confirmingRemovals) {
        listOf(
            ControllerPromptItem.fixed(ControllerIcon.DPAD_ALL, "Choose"),
            ControllerPromptItem(GamepadAction.SELECT, "Confirm"),
            ControllerPromptItem(GamepadAction.BACK, "Cancel"),
        )
    } else {
        listOf(
            ControllerPromptItem.fixed(ControllerIcon.DPAD_ALL, "Navigate"),
            ControllerPromptItem(GamepadAction.SELECT, "Toggle"),
            ControllerPromptItem(GamepadAction.CHANGE_SORT, "Search"),
            ControllerPromptItem(GamepadAction.OPEN_ISLAND, "Apply"),
            ControllerPromptItem(GamepadAction.BACK, "Cancel"),
        )
    }

    EchoHintBar(
        items = items,
        modifier = modifier,
        onAction = onAction,
        centre = {
            Text(
                text = if (selectedCount == 0) title else "$title  ·  $selectedCount selected",
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
