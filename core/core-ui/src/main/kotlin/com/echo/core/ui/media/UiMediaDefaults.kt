package com.echo.core.ui.media

fun resolveGameBootAudio(
    customVideoPath: String?,
    customAudioPath: String?,
): String? = if (customVideoPath != null) null else customAudioPath
