package com.echo.feature.launcher

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.permission.UsageAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class OpenSession(val gameId: Long, val platformId: String, val packageName: String?, val launchedAt: Long)

data class ForegroundMark(val at: Long, val className: String, val resumed: Boolean, val screenOff: Boolean = false)

@Singleton
open class PlaySessionLedger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    open suspend fun open(session: OpenSession) {
        context.echoDataStore.edit { it[KEY] = encodeSession(session) }
    }

    open suspend fun take(): OpenSession? {
        var taken: String? = null
        context.echoDataStore.edit { prefs ->
            taken = prefs[KEY]
            prefs.remove(KEY)
        }
        return taken?.let(::decodeSession)
    }

    open suspend fun foregroundMillis(packageName: String, from: Long, to: Long): Long? = withContext(Dispatchers.IO) {
        foregroundMillisNow(packageName, from, to)
    }

    private fun foregroundMillisNow(packageName: String, from: Long, to: Long): Long? {
        if (!UsageAccess.isGranted(context)) return null
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val events = manager.queryEvents(from, to)
        val marks = mutableListOf<ForegroundMark>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            when {
                type == UsageEvents.Event.SCREEN_NON_INTERACTIVE -> marks += ForegroundMark(event.timeStamp, "", resumed = false, screenOff = true)
                event.packageName != packageName -> Unit
                type == UsageEvents.Event.ACTIVITY_RESUMED -> marks += ForegroundMark(event.timeStamp, event.className.orEmpty(), true)
                type == UsageEvents.Event.ACTIVITY_PAUSED || type == UsageEvents.Event.ACTIVITY_STOPPED ->
                    marks += ForegroundMark(event.timeStamp, event.className.orEmpty(), false)
            }
        }
        return foregroundMillis(marks, to)
    }

    private companion object {
        val KEY = stringPreferencesKey("open_play_session")
    }
}

fun foregroundMillis(marks: List<ForegroundMark>, to: Long): Long {
    val open = mutableSetOf<String>()
    var since = 0L
    var total = 0L
    for (mark in marks.sortedBy { it.at }) {
        val wasOpen = open.isNotEmpty()
        when {
            mark.screenOff -> open.clear()
            mark.resumed -> open += mark.className
            else -> open -= mark.className
        }
        if (!wasOpen && open.isNotEmpty()) since = mark.at
        if (wasOpen && open.isEmpty()) total += mark.at - since
    }
    if (open.isNotEmpty()) total += to - since
    return total
}

internal fun encodeSession(s: OpenSession): String =
    listOf(s.gameId.toString(), s.platformId, s.packageName.orEmpty(), s.launchedAt.toString()).joinToString("\t")

internal fun decodeSession(raw: String): OpenSession? {
    val parts = raw.split('\t')
    if (parts.size != 4) return null
    return OpenSession(
        gameId = parts[0].toLongOrNull() ?: return null,
        platformId = parts[1],
        packageName = parts[2].ifBlank { null },
        launchedAt = parts[3].toLongOrNull() ?: return null,
    )
}
