package com.echo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TRIO_STEP_MS = 280L

// kit "Trio · loading": the mark's three circles, the ring travelling left to right while content loads
@Composable
fun EchoTrio(modifier: Modifier = Modifier, color: Color = Color.White, dot: Dp = 12.dp) {
    val step by produceState(0) {
        while (true) {
            delay(TRIO_STEP_MS)
            value = (value + 1) % 3
        }
    }
    Row(
        modifier.semantics { contentDescription = "Loading" },
        horizontalArrangement = Arrangement.spacedBy(dot * 0.375f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(dot)
                    .then(
                        if (i == step) Modifier.border(dot * 0.16f, color, CircleShape)
                        else Modifier.background(color.copy(alpha = 0.35f), CircleShape)
                    ),
            )
        }
    }
}
