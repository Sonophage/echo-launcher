package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.components.rowsShown

fun CrossbarUiState.menuRows(): List<CrossbarContextMenuItem> = activeContextMenu?.state?.rowsShown().orEmpty()
