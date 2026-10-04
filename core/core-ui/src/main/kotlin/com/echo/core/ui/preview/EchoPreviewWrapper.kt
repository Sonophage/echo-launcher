package com.echo.core.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.echo.core.ui.theme.DefaultEchoColors
import com.echo.core.ui.theme.EchoColors
import com.echo.core.ui.theme.EchoTheme

@Composable
fun EchoPreview(
    colors: EchoColors = DefaultEchoColors,
    content: @Composable () -> Unit,
) {
    EchoTheme(colors = colors) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@Composable
fun EchoScreenPreview(
    colors: EchoColors = DefaultEchoColors,
    content: @Composable () -> Unit,
) {
    EchoTheme(colors = colors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}
