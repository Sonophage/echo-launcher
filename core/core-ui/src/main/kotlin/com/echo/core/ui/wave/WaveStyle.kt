package com.echo.core.ui.wave

enum class WaveStyle {
    ANIMATED,
    REDUCED,
    STATIC,
    REDUCED_STATIC,

    OFF;

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
