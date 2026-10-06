package com.echo.feature.crossbar.viewmodel

val CrossbarItem.isInstalledApp: Boolean get() = gameId == null && packageName != null
