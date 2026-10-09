package com.echo.themekit

// how the crossbar marks what is focused (owner, 2026-10-09). CLASSIC is ECHO's own: brighter, a little larger,
// a soft glow. HALO rings the icon in the accent, LIFT raises it over the rest, BRACKET frames it with corners
enum class FocusStyle { CLASSIC, HALO, LIFT, BRACKET }

// how the crossbar moves (owner, 2026-10-09). CLASSIC is ECHO's own quick spring with a slight bounce; SNAPPY has
// no bounce, SOFT settles slowly, STILL jumps and only fades
enum class MotionPreset { CLASSIC, SNAPPY, SOFT, STILL }

fun focusStyleOf(name: String?): FocusStyle = FocusStyle.entries.firstOrNull { it.name == name } ?: FocusStyle.CLASSIC

fun motionPresetOf(name: String?): MotionPreset = MotionPreset.entries.firstOrNull { it.name == name } ?: MotionPreset.CLASSIC
