package com.echo.core.domain.model

enum class ConfirmBackLayout {
    STANDARD,

    REVERSED,
}

fun ConfirmBackLayout.displayLabel(): String = when (this) {
    ConfirmBackLayout.STANDARD -> "Standard (A = Confirm, B = Back)"
    ConfirmBackLayout.REVERSED -> "Reversed (B = Confirm, A = Back)"
}

enum class XYLayout {
    STANDARD,

    SWAPPED,
}

fun XYLayout.displayLabel(): String = when (this) {
    XYLayout.STANDARD -> "Standard (Y = Options, X = Details)"
    XYLayout.SWAPPED  -> "Swapped (X = Options, Y = Details)"
}

enum class ControllerDisplayType {
    GENERIC,
    XBOX,
    NINTENDO,
    PLAYSTATION,

    KEYBOARD,

    TOUCH;

    companion object {
        fun fromName(value: String?): ControllerDisplayType =
            entries.firstOrNull { it.name == value } ?: GENERIC
    }
}

fun ControllerDisplayType.displayLabel(): String = when (this) {
    ControllerDisplayType.GENERIC     -> "Generic"
    ControllerDisplayType.XBOX        -> "Xbox"
    ControllerDisplayType.NINTENDO    -> "Nintendo"
    ControllerDisplayType.PLAYSTATION -> "PlayStation"
    ControllerDisplayType.KEYBOARD    -> "Keyboard"
    ControllerDisplayType.TOUCH       -> "Touch"
}

enum class ScrollSpeed {
    SLOW,
    RELAXED,
    STANDARD,
    FAST,
}

enum class StickSensitivity(val deadZone: Float, val fullTilt: Float) {
    LOW(deadZone = 0.70f, fullTilt = 0.99f),

    STANDARD(deadZone = 0.58f, fullTilt = 0.95f),

    HIGH(deadZone = 0.45f, fullTilt = 0.88f);

    companion object {
        fun fromName(value: String?): StickSensitivity =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

fun StickSensitivity.displayLabel(): String = when (this) {
    StickSensitivity.LOW      -> "Low"
    StickSensitivity.STANDARD -> "Standard"
    StickSensitivity.HIGH     -> "High"
}

enum class TriggerSensitivity(val label: String, val press: Float, val release: Float) {
    LOW("Low", press = 0.85f, release = 0.55f),
    STANDARD("Standard", press = 0.6f, release = 0.3f),
    HIGH("High", press = 0.35f, release = 0.15f);

    companion object {
        fun fromName(value: String?): TriggerSensitivity =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

enum class ShoulderHoldTime(val label: String, val millis: Long) {
    SHORT("Short", 250L),
    STANDARD("Standard", 400L),
    LONG("Long", 650L);

    companion object {
        fun fromName(value: String?): ShoulderHoldTime =
            entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

fun ScrollSpeed.displayLabel(): String = when (this) {
    ScrollSpeed.SLOW     -> "Slow"
    ScrollSpeed.RELAXED  -> "Relaxed"
    ScrollSpeed.STANDARD -> "Standard"
    ScrollSpeed.FAST     -> "Fast"
}

data class ControllerLayoutPrefs(
    val confirmBackLayout: ConfirmBackLayout   = ConfirmBackLayout.STANDARD,
    val xyLayout: XYLayout                     = XYLayout.STANDARD,
    val displayType: ControllerDisplayType     = ControllerDisplayType.GENERIC,
    val scrollSpeed: ScrollSpeed               = ScrollSpeed.STANDARD,
    val stickSensitivity: StickSensitivity     = StickSensitivity.STANDARD,
    val triggerSensitivity: TriggerSensitivity = TriggerSensitivity.STANDARD,
    val shoulderHoldTime: ShoulderHoldTime     = ShoulderHoldTime.STANDARD,

    val leftBacksOut: Boolean                  = true,
)
