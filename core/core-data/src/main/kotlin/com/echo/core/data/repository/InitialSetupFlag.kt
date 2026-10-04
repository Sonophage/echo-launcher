package com.echo.core.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey

object InitialSetupFlag {
    val KEY_SEEN = booleanPreferencesKey("initial_setup_seen")

    val KEY_STARTED = booleanPreferencesKey("initial_setup_started")
}
