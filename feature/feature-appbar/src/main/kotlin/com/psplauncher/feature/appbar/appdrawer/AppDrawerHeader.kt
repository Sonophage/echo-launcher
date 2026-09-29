package com.psplauncher.feature.appbar.appdrawer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.psplauncher.core.ui.components.PfpSearchField
import com.psplauncher.core.ui.theme.StorefrontColors

internal val HEADER_HEIGHT = 56.dp

@Composable
internal fun AppDrawerHeader(
    onOpenSearch: () -> Unit,
    colors: StorefrontColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PfpSearchField(
            query = "",
            active = false,
            focusRequester = remember { FocusRequester() },
            placeholder = "Search apps",
            onActivate = onOpenSearch,
            onQueryChange = {},
            onDone = {},
            colors = colors,
            readOnly = true,
        )
    }
}
