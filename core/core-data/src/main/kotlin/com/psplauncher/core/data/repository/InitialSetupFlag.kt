package com.psplauncher.core.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey

object InitialSetupFlag {
    val KEY_SEEN = booleanPreferencesKey("initial_setup_seen")
}
