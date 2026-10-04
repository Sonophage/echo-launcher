package com.echo.core.ui.media

import com.echo.core.domain.model.UiMediaSlot
import kotlinx.coroutines.flow.Flow

interface UiMediaPaths {
    fun pathFor(slot: UiMediaSlot): String?

    val stamp: Flow<Long>

    val menuSoundsEnabled: Flow<Boolean>
}
