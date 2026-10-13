package com.echo.core.ui.design

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// one launch hold at a time, for pad A and for a finger on a launch button alike; onChange carries the held
// item's id, or null when nothing is held. The footer's A card rises while an id is held
class LaunchHold(private val scope: CoroutineScope, private val onChange: (String?) -> Unit) {
    private var job: Job? = null
    // true while the hold running was started by a finger, not by A
    private var pressed = false

    fun start(itemId: String, launch: () -> Unit) {
        pressed = false
        job?.cancel()
        onChange(itemId)
        job = scope.launch {
            delay(LAUNCH_HOLD_MS)
            job = null
            onChange(null)
            launch()
        }
    }

    // true when a hold was let go before it finished
    fun release(): Boolean {
        val held = job ?: return false
        held.cancel()
        job = null
        onChange(null)
        return true
    }

    // a finger down on a launch button or card holds as A does (issue #36); lift lets go of only that hold, so a
    // finger lifted elsewhere never cuts short a hold on A
    fun press(itemId: String, launch: () -> Unit) {
        start(itemId, launch)
        pressed = true
    }

    fun lift() {
        if (!pressed) return
        pressed = false
        release()
    }
}
