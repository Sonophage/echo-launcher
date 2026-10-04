package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.components.LetterAnchor
import com.echo.core.ui.components.LetterJumpState
import com.echo.core.ui.components.letterAnchors as anchorsOfTitles
import com.echo.core.ui.components.letterJumpFor as jumpForTitles

internal fun letterAnchors(items: List<CrossbarItem>): List<LetterAnchor>? =
    anchorsOfTitles(items.map { it.title })

internal fun letterJumpFor(items: List<CrossbarItem>, currentIndex: Int): LetterJumpState? =
    jumpForTitles(items.map { it.title }, currentIndex)
