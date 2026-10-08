package com.echo.core.ui.motion

import com.echo.core.ui.wave.WaveStyle

object MotionWallpaperPolicy {
    // the video wallpaper's own motion, apart from the wave (owner, 2026-10-08: turning the wave off stopped the
    // video). Unset, it plays: the wave no longer decides it
    const val KEY = "display_motion_style"

    fun motionStyleOf(saved: String?): WaveStyle =
        saved?.let { runCatching { WaveStyle.valueOf(it) }.getOrNull() } ?: WaveStyle.ANIMATED

    enum class Decision {
        POSTER,

        PLAY,

        PLAY_REDUCED,
    }

    data class Inputs(

        val hasMotion: Boolean,

        val hasPoster: Boolean,

        val style: WaveStyle,

        val covered: Boolean,

        val throttled: Boolean,

        val appVisible: Boolean,
    )

    fun decide(inputs: Inputs): Decision {
        if (!inputs.hasMotion || !inputs.hasPoster) return Decision.POSTER

        if (!inputs.style.animated) return Decision.POSTER

        if (inputs.covered || inputs.throttled || !inputs.appVisible) return Decision.POSTER
        return if (inputs.style.reduced) Decision.PLAY_REDUCED else Decision.PLAY
    }
}
