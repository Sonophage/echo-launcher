package com.psplauncher.core.common.format

fun relativeTime(now: Long, then: Long): String {
    val minutes = (now - then) / 60_000L
    return when {
        minutes < 1 -> "Now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
