package com.echo.core.domain.model

data class VideoPlaylist(
    val id: Long,
    val name: String,
    val videoCount: Int = 0,
)
