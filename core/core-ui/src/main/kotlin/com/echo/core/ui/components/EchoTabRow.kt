package com.echo.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.design.DesignUnits

// the tab row along the top of a screen with tabs: back, the LT mark, the tabs with a bar under the current one,
// the RT mark. owner, 2026-10-07: Settings and the top panel draw the same row
@Composable
fun EchoTabRow(
    labels: List<String>,
    current: Int,
    u: DesignUnits,
    onBack: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        val info = listState.layoutInfo
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        val width = info.visibleItemsInfo.firstOrNull { it.index == current }?.size ?: 0
        listState.animateScrollToItem(current, if (viewport > width) -((viewport - width) / 2) else 0)
    }
    val pad = LocalPadPrompts.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .padding(start = u.dp(32), end = u.dp(80), top = u.dp(12), bottom = u.dp(14)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(u.dp(64))
                .clip(RoundedCornerShape(u.dp(32)))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("◀", color = Color.White.copy(alpha = 0.65f), fontSize = u.sp(18))
        }
        Spacer(Modifier.width(u.dp(16)))
        if (pad) ControllerPrompt(GamepadAction.PREV_CATEGORY, "", glyphSize = u.dp(30), spacing = 0.dp)
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(u.dp(10), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(labels) { index, label ->
                val on = index == current
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(u.dp(8)))
                        .clickable { onPick(index) }
                        .padding(horizontal = u.dp(8), vertical = u.dp(10)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(u.dp(8)),
                ) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = if (on) 1f else 0.5f),
                        fontSize = u.sp(15),
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
                        maxLines = 1,
                    )
                    val bar by animateFloatAsState(if (on) 1f else 0f, tween(250), label = "tabBar")
                    Box(Modifier.width(u.dp(22) * bar).height(u.dp(2)).clip(RoundedCornerShape(1.dp)).background(Color.White))
                }
            }
        }
        if (pad) ControllerPrompt(GamepadAction.NEXT_CATEGORY, "", glyphSize = u.dp(30), spacing = 0.dp)
    }
}
