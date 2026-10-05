package com.echo.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.em
import com.echo.core.domain.model.settingsEntryFor
import com.echo.core.ui.design.panelSectionTint
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.settingsSectionFor
import com.echo.core.ui.components.ControllerPromptItem

@Composable
fun SettingsPageScaffold(

    subtitle: String,

    heading: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusKey: String? = null,
    onInterceptAction: ((GamepadAction) -> Boolean)? = null,
    helperFooterItems: List<ControllerPromptItem> = SettingsDefaultHelperItems,
    // drawn behind the whole page, under the content (the Overview's last-played art)
    backdrop: (@Composable () -> Unit)? = null,
    fullWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    SettingsScaffold(
        title = "Settings",
        subtitle = subtitle,
        onBack = onBack,
        modifier = modifier,
        restoreFocusKey = restoreFocusKey,
        onInterceptAction = onInterceptAction,
        helperFooterItems = helperFooterItems,

        panelTint = panelSectionTint(LocalSettingsScreenId.current?.let { settingsSectionFor(it) }),

        header = {
            val screenId = LocalSettingsScreenId.current
            val section = remember(screenId) {
                screenId?.let { settingsSectionFor(it) }
            }
            val entry = remember(screenId) { screenId?.let { settingsEntryFor(it) } }
            SettingsPageTitle(section?.title, heading ?: entry?.title ?: subtitle)
        },

        showDivider = false,
        showRail = heading == null,
        backdrop = backdrop,
        fullWidth = fullWidth,
    ) {
        Column(Modifier.fillMaxWidth()) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun SettingsPageTitle(eyebrow: String?, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .padding(start = 52.dp, end = 40.dp, top = 14.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        eyebrow?.let {
            Text(
                text = it.uppercase(),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                letterSpacing = 0.18.em,
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-0.02).em,
        )
    }
}

@Composable
internal fun rememberReadOnlyPageScroll(
    scrollState: androidx.compose.foundation.ScrollState,
): (GamepadAction) -> Boolean {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { 120.dp.toPx() }
    return remember(scrollState, stepPx) {
        { action ->
            when (action) {
                GamepadAction.NAVIGATE_UP -> {
                    scope.launch { scrollState.animateScrollBy(-stepPx) }; true
                }
                GamepadAction.NAVIGATE_DOWN -> {
                    scope.launch { scrollState.animateScrollBy(stepPx) }; true
                }
                else -> false
            }
        }
    }
}
