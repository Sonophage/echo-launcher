package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.rowsShown

fun CrossbarUiState.menuWithPills(): MenuState<String>? =
    activeContextMenu?.state?.copy(withheld = focusedPills().map { it.id }.toSet())

fun CrossbarUiState.menuRows(): List<CrossbarContextMenuItem> = menuWithPills()?.rowsShown().orEmpty()
