package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// leaves ECHO: a real game, a shortcut or an installed app. Media plays inside ECHO and needs no hold.
internal fun CrossbarItem.launchesOut(): Boolean =
    (gameId != null && isRealGame) || launchIntentUri != null || packageName != null

// how long A, or a finger on the launch button, must be held for this item; 0 when it acts at once
internal fun holdMsFor(item: CrossbarItem?): Long = if (item?.launchesOut() == true) LAUNCH_HOLD_MS else 0L

// the hold id for Y Resume, so its ring is told apart from A Play's on the same game
internal fun resumeHoldId(itemId: String): String = "resume:$itemId"

// one hold at a time; onChange carries the held item's id, or null when nothing is held
internal class LaunchHold(private val scope: CoroutineScope, private val onChange: (String?) -> Unit) {
    private var job: Job? = null

    fun start(itemId: String, launch: () -> Unit) {
        job?.cancel()
        onChange(itemId)
        job = scope.launch {
            delay(LAUNCH_HOLD_MS)
            job = null
            onChange(null)
            launch()
        }
    }

    fun release() {
        val held = job ?: return
        held.cancel()
        job = null
        onChange(null)
    }
}
