package com.echo.feature.crossbar.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// how long A must be held before a game or app opens (kit: "Hold A; the ring fills, then launches")
const val LAUNCH_HOLD_MS = 600L

// leaves ECHO: a real game, a shortcut or an installed app. Media plays inside ECHO and needs no hold.
internal fun CrossbarItem.launchesOut(): Boolean =
    (gameId != null && isRealGame) || launchIntentUri != null || packageName != null

// how long a pad A press on this item must be held; 0 when it acts at once
internal fun holdMsFor(item: CrossbarItem?, pad: Boolean): Long =
    if (pad && item?.launchesOut() == true) LAUNCH_HOLD_MS else 0L

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
