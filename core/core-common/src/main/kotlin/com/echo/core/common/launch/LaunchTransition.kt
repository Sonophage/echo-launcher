package com.echo.core.common.launch

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle

object LaunchTransition {
    // display: where the launch opens, from LaunchScreens. Always given, as Android would otherwise open it on
    // the display of whichever window made the launch
    fun options(context: Context, display: Int): Bundle? =
        runCatching { ActivityOptions.makeCustomAnimation(context, 0, 0).setLaunchDisplayId(display).toBundle() }.getOrNull()

    fun Intent.withoutTransition(): Intent = addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
}
