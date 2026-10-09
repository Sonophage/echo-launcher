package com.echo.core.data.repository

import android.net.Uri

// the tests' way into the live importer: ECHO itself calls importBundleDetailed and reads its result
internal suspend fun EchoThemeStore.importBundle(uri: Uri): EchoThemeStore.SavedTheme? =
    (importBundleDetailed(uri) as? EchoThemeStore.ImportResult.Success)?.theme
