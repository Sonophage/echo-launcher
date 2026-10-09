package com.echo.core.ui.design

import android.os.Build

// the Unihertz Titan 2: a 1440 x 1440 screen and a hardware keyboard. It gets its own square layouts, and no other
// device's screens change (owner, 2026-10-09). Build.DEVICE is its codename, the same on every Titan 2
val IsTitan2: Boolean = Build.DEVICE == "Titan_2"
