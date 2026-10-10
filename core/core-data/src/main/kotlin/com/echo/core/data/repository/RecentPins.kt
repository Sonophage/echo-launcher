package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore

// owner, 2026-10-06: games and apps pinned under Recent, in the order they were pinned, one "g:<game id>" or
// "a:<package>" a line. The crossbar shows and toggles them; the library unpins a game that goes
// (owner, 2026-10-10), so both read this one definition
object RecentPins {
    val KEY = stringPreferencesKey("recent_pins")

    fun parse(raw: String?): List<String> = raw?.split('\n')?.filter { it.isNotBlank() }?.distinct().orEmpty()

    fun gameKey(gameId: Long): String = "g:$gameId"

    fun appKey(packageName: String): String = "a:$packageName"

    suspend fun unpinGames(context: Context, gameIds: Collection<Long>) {
        if (gameIds.isEmpty()) return
        val gone = gameIds.mapTo(HashSet(), ::gameKey)
        context.echoDataStore.edit { prefs ->
            val pins = parse(prefs[KEY])
            if (pins.any { it in gone }) prefs[KEY] = pins.filterNot { it in gone }.joinToString("\n")
        }
    }
}
