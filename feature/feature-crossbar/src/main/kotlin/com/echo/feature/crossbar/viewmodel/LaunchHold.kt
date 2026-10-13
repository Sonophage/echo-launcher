package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.design.LAUNCH_HOLD_MS


// leaves ECHO: a real game, a shortcut or an installed app. Media plays inside ECHO and needs no hold.
internal fun CrossbarItem.launchesOut(): Boolean =
    (gameId != null && isRealGame) || launchIntentUri != null || packageName != null

// how long A, or a finger on the launch button, must be held for this item; 0 when it acts at once
internal fun holdMsFor(item: CrossbarItem?): Long = if (item?.launchesOut() == true) LAUNCH_HOLD_MS else 0L

// the hold id for Y-held Resume, so its ring is told apart from A Play's on the same game
internal fun resumeHoldId(itemId: String): String = "resume:$itemId"
