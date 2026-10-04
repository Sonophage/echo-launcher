package com.echo.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.echoDataStore: DataStore<Preferences> by preferencesDataStore(name = "pfp_prefs")

val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader")
