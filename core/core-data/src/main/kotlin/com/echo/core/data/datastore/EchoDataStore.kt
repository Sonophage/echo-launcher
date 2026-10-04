package com.echo.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val Context.echoDataStore: DataStore<Preferences> by preferencesDataStore(name = "pfp_prefs")

val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader")

// the built-in reader's saved place in a book: a Readium Locator as JSON
fun readerPositionKey(bookId: String) = stringPreferencesKey("position_$bookId")

// how far through the book the saved place is, 0..1; null when the reader has no place for it
fun readerProgress(prefs: Preferences, bookId: String): Float? = prefs[readerPositionKey(bookId)]?.let { json ->
    runCatching {
        Json.parseToJsonElement(json).jsonObject["locations"]?.jsonObject?.get("totalProgression")?.jsonPrimitive?.floatOrNull
    }.getOrNull()
}?.coerceIn(0f, 1f)
