package com.echo.core.data.repository

fun interface CustomIconCacheEvictor {
    fun evict(path: String)
}
