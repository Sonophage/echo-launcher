package com.echo.core.data.launch

// Close it first (#30). An app already running can refuse Launch on Other Screen and come forward on its old
// display; Chrome moves both ways while running, so ECHO does not warn on every running app (owner, 2026-10-12).
// Android shows ECHO no other app's display, so ECHO reads its own: about a second after the launch, an app that
// moved covers ECHO on the target display, and one that stayed covers it on the old display. A launch that did not
// start covers neither, and is not warned.
internal fun stayedOnOldDisplay(old: Int, target: Int, echoShownOn: Set<Int>): Boolean =
    target != old && target in echoShownOn && old !in echoShownOn
