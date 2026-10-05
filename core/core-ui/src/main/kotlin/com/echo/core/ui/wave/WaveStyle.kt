package com.echo.core.ui.wave

enum class WaveStyle(val label: String) {
    ANIMATED("Animated"),
    REDUCED("Reduced"),
    STATIC("Static"),
    REDUCED_STATIC("Reduced + Static"),

    OFF("Off");

    // Quick Settings steps through the styles in this order, Off included (owner, 2026-10-05)
    val next: WaveStyle get() = entries[(ordinal + 1) % entries.size]

    val animated: Boolean get() = this == ANIMATED || this == REDUCED

    val reduced: Boolean get() = this == REDUCED || this == REDUCED_STATIC

    /**
     * OFF draws no wave at all, leaving the theme's background gradient. It is not
     * the same as STATIC, which still renders the shader once and leaves it on screen.
     */
    val drawsWave: Boolean get() = this != OFF

    val frozen: WaveStyle
        get() = when (this) {
            ANIMATED, STATIC -> STATIC
            REDUCED, REDUCED_STATIC -> REDUCED_STATIC

            OFF -> OFF
        }
}
