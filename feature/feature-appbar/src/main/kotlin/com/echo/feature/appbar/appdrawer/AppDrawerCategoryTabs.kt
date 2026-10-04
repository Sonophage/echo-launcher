package com.echo.feature.appbar.appdrawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.DesignUnits
import com.echo.feature.appbar.AppFilter

@Composable
internal fun AppDrawerCategoryTabs(
    activeFilter: AppFilter,
    filterCounts: Map<AppFilter, Int>,
    onFilterSelected: (AppFilter) -> Unit,
    u: DesignUnits,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(u.dp(30)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pad = LocalPadPrompts.current
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = u.dp(24), spacing = 0.dp)
        AppFilter.entries.forEach { filter ->
            AppDrawerCategoryTab(
                label = filter.label,
                count = filterCounts[filter] ?: 0,
                selected = filter == activeFilter,
                onClick = { onFilterSelected(filter) },
                u = u,
            )
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(24), spacing = 0.dp)
    }
}

@Composable
private fun AppDrawerCategoryTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    u: DesignUnits,
) {
    val bar by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "wallTabBar")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(u.dp(8)),
        modifier = Modifier
            .clip(RoundedCornerShape(u.dp(6)))
            .clickable(onClick = onClick)
            .padding(horizontal = u.dp(6), vertical = u.dp(6)),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(u.dp(5))) {
            Text(
                text = label,
                color = Color.White.copy(alpha = if (selected) 1f else 0.5f),
                fontSize = u.sp(16),
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    color = Color.White.copy(alpha = if (selected) 0.7f else 0.35f),
                    fontSize = u.sp(11),
                    fontWeight = FontWeight.Light,
                )
            }
        }
        Box(Modifier.width(u.dp(24) * bar).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White))
    }
}
