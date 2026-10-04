package com.echo.feature.artwork.store

fun isPortableRef(uri: String?): Boolean =
    uri != null && uri.startsWith("content://", ignoreCase = true)
