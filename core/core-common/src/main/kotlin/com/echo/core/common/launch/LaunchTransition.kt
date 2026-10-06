package com.echo.core.common.launch

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Display

object LaunchTransition {
    // always onto the main screen: on a device with a second screen, a launch made from the bottom screen
    // would otherwise open there
    fun options(context: Context): Bundle? =
        runCatching { ActivityOptions.makeCustomAnimation(context, 0, 0).setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle() }.getOrNull()

    fun Intent.withoutTransition(): Intent = addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
}
