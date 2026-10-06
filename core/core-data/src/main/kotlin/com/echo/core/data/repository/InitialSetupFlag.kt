package com.echo.core.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object InitialSetupFlag {
    val KEY_SEEN = booleanPreferencesKey("initial_setup_seen")

    val KEY_STARTED = booleanPreferencesKey("initial_setup_started")

    // the first-run wizard's step, so a new ECHO activity opens setup where the user was
    val KEY_STEP = stringPreferencesKey("initial_setup_step")
}
