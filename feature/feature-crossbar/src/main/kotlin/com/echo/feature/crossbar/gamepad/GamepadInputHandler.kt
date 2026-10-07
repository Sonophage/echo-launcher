package com.echo.feature.crossbar.gamepad

import android.os.SystemClock
import android.view.InputDevice
import com.echo.core.data.repository.ControllerRegistry
import com.echo.core.data.repository.RemapCoordinator
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.GamepadMappings
import com.echo.core.domain.model.ScrollSpeed
import com.echo.core.domain.model.StickSensitivity
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private const val STICK_DEAD_ZONE_FLOOR = 0.35f

private const val STICK_RELEASE_FACTOR = 0.6f

private const val HAT_DEAD_ZONE = 0.5f

private const val DUPLICATE_WINDOW_MS = 80L

// a second d-pad direction pressed while one is held counts only once it has lasted this long: a hard press
// rocks onto the diagonal and back in less, and must stay one move (owner, 2026-10-06)
internal const val CHORD_SETTLE_MS = 80L

private const val STICK_FULL_TILT_RAMP_FACTOR = 2

// owner, 2026-10-06: a press of Select opens the notifications, holding it is Home
internal const val SELECT_HOLD_MS = 500L

internal data class RepeatTuning(
    val initialDelayMs: Long,
    val baseIntervalMs: Long,
    val fastIntervalMs: Long,
    val rampSteps: Int,
)

internal fun ScrollSpeed.tuning(): RepeatTuning = when (this) {
    ScrollSpeed.SLOW     -> RepeatTuning(initialDelayMs = 450, baseIntervalMs = 170, fastIntervalMs = 120, rampSteps = 8)
    ScrollSpeed.RELAXED  -> RepeatTuning(initialDelayMs = 350, baseIntervalMs = 130, fastIntervalMs = 80, rampSteps = 6)
    ScrollSpeed.STANDARD -> RepeatTuning(initialDelayMs = 250, baseIntervalMs = 110, fastIntervalMs = 50, rampSteps = 5)
    ScrollSpeed.FAST     -> RepeatTuning(initialDelayMs = 180, baseIntervalMs = 90,  fastIntervalMs = 35, rampSteps = 4)
}

private enum class TabSource { KEY, LEFT_AXIS, RIGHT_AXIS }

sealed interface ShoulderHold {
    val action: GamepadAction
    data class Start(override val action: GamepadAction) : ShoulderHold
    data class End(override val action: GamepadAction) : ShoulderHold
}


@Singleton
class GamepadInputHandler @Inject constructor(
    private val remapCoordinator: RemapCoordinator,
    private val registry: ControllerRegistry,
) {
    private val _actions = MutableSharedFlow<GamepadAction>(extraBufferCapacity = 16)
    val actions: SharedFlow<GamepadAction> = _actions.asSharedFlow()

    private val _shoulderHolds = MutableSharedFlow<ShoulderHold>(extraBufferCapacity = 8)
    val shoulderHolds: SharedFlow<ShoulderHold> = _shoulderHolds.asSharedFlow()

    // A or Y coming up; ends a hold-to-launch (A) or a hold-to-resume (Y)
    private val _holdReleases = MutableSharedFlow<GamepadAction>(extraBufferCapacity = 4)
    val holdReleases: SharedFlow<GamepadAction> = _holdReleases.asSharedFlow()

    private var shoulderJob: Job? = null
    private var shoulderHeld: GamepadAction? = null

    var currentMappings: GamepadMappings = GamepadMappings()

    var scrollSpeed: ScrollSpeed = ScrollSpeed.STANDARD

    var stickSensitivity: StickSensitivity = StickSensitivity.STANDARD

    var triggerSensitivity: com.echo.core.domain.model.TriggerSensitivity =
        com.echo.core.domain.model.TriggerSensitivity.STANDARD

    var shoulderHoldMs: Long = com.echo.core.domain.model.ShoulderHoldTime.STANDARD.millis

    var scope: CoroutineScope? = null

    var bypassToComposeFocus: Boolean = false

    private var repeatJob: Job? = null

    // Select is down and has not yet become Home
    private var notificationsJob: Job? = null

    private var leftTriggerDown = false
    private var rightTriggerDown = false
    private val tabOwner = mutableMapOf<GamepadAction, TabSource>()
    private val lastPageEmitAt = mutableMapOf<GamepadAction, Long>()
    private var lastStickAction: GamepadAction? = null

    @Volatile private var stickMagnitude: Float = 0f

    private var prevHatX: Float = 0f

    // a second direction waiting out CHORD_SETTLE_MS
    private var chordJob: Job? = null
    private var prevHatY: Float = 0f

    private val lastDirectionalEmitAt = mutableMapOf<GamepadAction, Long>()

    internal var clock: () -> Long = SystemClock::uptimeMillis

    fun onKeyEvent(event: KeyEvent): Boolean {
        remapCoordinator.captureNextKey?.let { capture ->
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                remapCoordinator.captureNextKey = null
                capture(event.keyCode)
            }
            return true
        }

        val action = currentMappings.actionFor(event.keyCode) ?: return false
        registry.markActive(event.deviceId)

        if (bypassToComposeFocus && action != GamepadAction.BACK) return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    if (action.isDirectional() && isDuplicateDirection(action)) return true

                    if (action.isDirectional()) {
                        startRepeat(action)
                    }

                    if (action.isShoulder()) {
                        tabPress(action, TabSource.KEY)
                        return true
                    }
                    if (action == GamepadAction.OPEN_NOTIFICATIONS) {
                        armHomeHold()
                        return true
                    }
                    emit(action, physical = true)
                }
                true
            }
            KeyEvent.ACTION_UP -> {
                if (action.isDirectional()) cancelRepeat()
                if (action.isShoulder()) tabRelease(action, TabSource.KEY)
                if (action == GamepadAction.SELECT || action == GamepadAction.OPEN_CONTEXT_MENU) _holdReleases.tryEmit(action)
                if (action == GamepadAction.OPEN_NOTIFICATIONS) releaseHomeHold()
                true
            }
            else -> false
        }
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) {
            return false
        }
        if (event.action != MotionEvent.ACTION_MOVE) return false
        registry.markActive(event.deviceId)

        val x = event.getAxisValue(MotionEvent.AXIS_X)
        val y = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val triggersFired = handleTriggers(event)

        val stickAction = stickDirection(x, y, stickFlatFor(event.deviceId))
        val hatAction = hatDirection(hatX, hatY)

        val motionAction = hatAction ?: stickAction

        stickMagnitude = if (motionAction != null) maxOf(abs(x), abs(y), abs(hatX), abs(hatY)) else 0f

        val diagonal = abs(hatX) > HAT_DEAD_ZONE && abs(hatY) > HAT_DEAD_ZONE
        val s = scope
        if (diagonal && lastStickAction != null && motionAction != lastStickAction && s != null) {
            // the held direction goes on until the new one has lasted; a rock back cancels it
            if (chordJob == null) chordJob = s.launch {
                delay(CHORD_SETTLE_MS)
                chordJob = null
                steer(motionAction)
            }
        } else {
            chordJob?.cancel()
            chordJob = null
            steer(motionAction)
        }

        prevHatX = hatX
        prevHatY = hatY
        return motionAction != null || triggersFired
    }

    private fun steer(motionAction: GamepadAction?) {
        if (motionAction == lastStickAction) return
        cancelRepeat()
        lastStickAction = motionAction
        if (motionAction != null && !isDuplicateDirection(motionAction)) {
            startRepeat(motionAction)
            emit(motionAction, physical = true)
        }
    }

    private fun handleTriggers(event: MotionEvent): Boolean {
        val left = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
        val right = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))

        val leftNow = triggerDown(leftTriggerDown, left, triggerSensitivity)
        val rightNow = triggerDown(rightTriggerDown, right, triggerSensitivity)

        // an analog trigger does what its key is bound to, holds included
        var fired = false
        currentMappings.actionFor(KeyEvent.KEYCODE_BUTTON_L2)?.let { fired = triggerEdge(it, leftTriggerDown, leftNow, TabSource.LEFT_AXIS) || fired }
        currentMappings.actionFor(KeyEvent.KEYCODE_BUTTON_R2)?.let { fired = triggerEdge(it, rightTriggerDown, rightNow, TabSource.RIGHT_AXIS) || fired }

        leftTriggerDown = leftNow
        rightTriggerDown = rightNow
        return fired
    }

    private fun triggerEdge(action: GamepadAction, was: Boolean, now: Boolean, source: TabSource): Boolean = when {
        now && !was -> if (action.isShoulder()) { tabPress(action, source); true } else emit(action, physical = true)
        was && !now && action.isShoulder() -> { tabRelease(action, source); true }
        else -> false
    }

    // a pad may report one trigger as a key and an axis at once. Whichever path pressed it owns the
    // press: the other path's press and release are ignored until then, so one pull is one step,
    // and either path works again as soon as the press ends
    private fun tabPress(action: GamepadAction, source: TabSource) {
        if (tabOwner.containsKey(action)) return
        tabOwner[action] = source
        armShoulderHold(action)
    }

    private fun tabRelease(action: GamepadAction, source: TabSource) {
        if (tabOwner[action] != source) return
        tabOwner.remove(action)
        releaseShoulder(action)
    }



    private fun emit(action: GamepadAction, physical: Boolean = false): Boolean {
        if (action == GamepadAction.PREV_PAGE || action == GamepadAction.NEXT_PAGE) {
            val last = lastPageEmitAt[action]
            if (last != null && clock() - last < DUPLICATE_WINDOW_MS) return false
            lastPageEmitAt[action] = clock()
        }
        if (physical && action.isDirectional()) lastDirectionalEmitAt[action] = clock()
        _actions.tryEmit(action)
        Timber.v("Gamepad action: $action")
        return true
    }

    private fun startRepeat(action: GamepadAction) {
        val s = scope ?: return
        startRepeating(action, s)
    }

    fun startRepeating(action: GamepadAction, s: CoroutineScope) {
        repeatJob?.cancel()
        repeatJob = s.launch {
            val t = scrollSpeed.tuning()
            delay(t.initialDelayMs)
            var step = 0
            while (true) {
                emit(action)
                step++

                val climbed = rampStepFor(step, stickMagnitude, stickSensitivity.fullTilt)
                delay(rampedInterval(climbed, t.baseIntervalMs, t.fastIntervalMs, t.rampSteps))
            }
        }
    }

    fun cancelRepeat() {
        repeatJob?.cancel()
        repeatJob = null
    }

    private fun stickDirection(x: Float, y: Float, flat: Float): GamepadAction? {
        val activation = maxOf(stickSensitivity.deadZone, STICK_DEAD_ZONE_FLOOR, flat)
        val release = activation * STICK_RELEASE_FACTOR

        val strong = when {
            y < -activation -> GamepadAction.NAVIGATE_UP
            y >  activation -> GamepadAction.NAVIGATE_DOWN
            x < -activation -> GamepadAction.NAVIGATE_LEFT
            x >  activation -> GamepadAction.NAVIGATE_RIGHT
            else -> null
        }
        if (strong != null) return strong

        val engaged = lastStickAction ?: return null
        val stillEngaged = when (engaged) {
            GamepadAction.NAVIGATE_UP -> y < -release
            GamepadAction.NAVIGATE_DOWN -> y > release
            GamepadAction.NAVIGATE_LEFT -> x < -release
            GamepadAction.NAVIGATE_RIGHT -> x > release
            else -> false
        }
        return if (stillEngaged) engaged else null
    }

    private fun hatDirection(hatX: Float, hatY: Float): GamepadAction? =
        hatDirectionNewestFirst(hatX, hatY, prevHatX, prevHatY, lastStickAction)

    private fun stickFlatFor(deviceId: Int): Float =
        runCatching { InputDevice.getDevice(deviceId)?.getMotionRange(MotionEvent.AXIS_X)?.flat }
            .getOrNull() ?: 0f

    private fun isDuplicateDirection(action: GamepadAction): Boolean {
        if (!action.isDirectional()) return false
        val now = clock()
        val last = lastDirectionalEmitAt[action]
        return last != null && now - last < DUPLICATE_WINDOW_MS
    }

    // with no scope to time the hold, a press is the notifications at once
    private fun armHomeHold() {
        val s = scope ?: run { emit(GamepadAction.OPEN_NOTIFICATIONS, physical = true); return }
        notificationsJob?.cancel()
        notificationsJob = s.launch {
            delay(SELECT_HOLD_MS)
            notificationsJob = null
            emit(GamepadAction.HOME, physical = true)
        }
    }

    // let go before the hold time: it was a press
    private fun releaseHomeHold() {
        val job = notificationsJob ?: return
        notificationsJob = null
        job.cancel()
        emit(GamepadAction.OPEN_NOTIFICATIONS, physical = true)
    }

    private fun armShoulderHold(action: GamepadAction) {
        shoulderJob?.cancel()
        shoulderHeld = null
        shoulderJob = scope?.launch {
            delay(shoulderHoldMs)
            shoulderHeld = action
            _shoulderHolds.tryEmit(ShoulderHold.Start(action))
        }
    }

    private fun releaseShoulder(action: GamepadAction) {
        shoulderJob?.cancel()
        shoulderJob = null
        val held = shoulderHeld
        shoulderHeld = null
        if (held != null) _shoulderHolds.tryEmit(ShoulderHold.End(held)) else emit(action, physical = true)
    }

    private fun GamepadAction.isShoulder() =
        this == GamepadAction.PREV_CATEGORY || this == GamepadAction.NEXT_CATEGORY

    private fun GamepadAction.isDirectional() = this in setOf(
        GamepadAction.NAVIGATE_UP,
        GamepadAction.NAVIGATE_DOWN,
        GamepadAction.NAVIGATE_LEFT,
        GamepadAction.NAVIGATE_RIGHT,
    )
}

internal fun hatDirectionNewestFirst(
    hatX: Float,
    hatY: Float,
    prevHatX: Float,
    prevHatY: Float,
    held: GamepadAction?,
    deadZone: Float = HAT_DEAD_ZONE,
): GamepadAction? {
    val xDir = when {
        hatX < -deadZone -> GamepadAction.NAVIGATE_LEFT
        hatX >  deadZone -> GamepadAction.NAVIGATE_RIGHT
        else -> null
    }
    val yDir = when {
        hatY < -deadZone -> GamepadAction.NAVIGATE_UP
        hatY >  deadZone -> GamepadAction.NAVIGATE_DOWN
        else -> null
    }
    if (xDir == null) return yDir
    if (yDir == null) return xDir

    val xIsNew = abs(prevHatX) <= deadZone
    val yIsNew = abs(prevHatY) <= deadZone
    if (xIsNew != yIsNew) return if (xIsNew) xDir else yDir

    return when (held) {
        yDir -> yDir
        xDir -> xDir
        else -> yDir
    }
}

internal fun rampStepFor(repeats: Int, stickMagnitude: Float, fullTilt: Float): Int =
    if (stickMagnitude >= fullTilt) repeats * STICK_FULL_TILT_RAMP_FACTOR else repeats

internal fun rampedInterval(step: Int, base: Long, fast: Long, rampSteps: Int): Long =
    if (step >= rampSteps) fast else base - (base - fast) * step / rampSteps

internal fun triggerDown(
    wasDown: Boolean,
    value: Float,
    sensitivity: com.echo.core.domain.model.TriggerSensitivity =
        com.echo.core.domain.model.TriggerSensitivity.STANDARD,
): Boolean = if (wasDown) value > sensitivity.release else value >= sensitivity.press
