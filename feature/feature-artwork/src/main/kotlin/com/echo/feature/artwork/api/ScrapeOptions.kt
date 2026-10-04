package com.echo.feature.artwork.api

data class ScrapeOptions(
    val downloadClearLogos: Boolean = true,

    val downloadManuals: Boolean = true,
    val downloadVideoSnaps: Boolean = false,

    val bypassSsCache: Boolean = false,

    val metadataOnly: Boolean = false,
)
