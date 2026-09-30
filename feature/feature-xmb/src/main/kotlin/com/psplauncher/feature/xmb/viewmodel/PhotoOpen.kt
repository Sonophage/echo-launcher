package com.psplauncher.feature.xmb.viewmodel

internal const val PHOTO_VIEWER_ASK = "ask"

internal sealed interface PhotoOpen {
    data object BuiltIn : PhotoOpen
    data object Ask : PhotoOpen
    data class App(val packageName: String) : PhotoOpen
}

internal fun photoOpenFor(defaultViewer: String?): PhotoOpen = when {
    defaultViewer.isNullOrBlank() -> PhotoOpen.BuiltIn
    defaultViewer == PHOTO_VIEWER_ASK -> PhotoOpen.Ask
    else -> PhotoOpen.App(defaultViewer)
}
