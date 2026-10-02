package com.psplauncher.feature.reader

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.psplauncher.core.data.datastore.readerDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ReaderTypeface(val label: String) { SERIF("Serif"), SANS("Sans") }

enum class ReaderPage(val label: String, val background: Int, val text: Int) {
    DARK("Dark", 0xFF141110.toInt(), 0xFFE6E0D6.toInt()),
    SEPIA("Sepia", 0xFF2A2219.toInt(), 0xFFE8D9BF.toInt()),
    PAPER("Paper", 0xFFEDE6DA.toInt(), 0xFF2B2622.toInt()),
}

enum class ReaderLayout(val label: String) { TWO_PAGES("Two pages"), ONE_PAGE("Single page") }

data class ReaderDisplay(
    val textScale: Float = 1.0f,
    val typeface: ReaderTypeface = ReaderTypeface.SERIF,
    val page: ReaderPage = ReaderPage.DARK,
    val layout: ReaderLayout = ReaderLayout.TWO_PAGES,
) {
    fun withTextScale(delta: Float) = copy(textScale = clampTextScale(textScale + delta))

    companion object {
        const val TEXT_SCALE_MIN = 0.8f
        const val TEXT_SCALE_MAX = 2.0f
        const val TEXT_SCALE_STEP = 0.1f

        fun clampTextScale(value: Float): Float =
            (Math.round(value * 10f) / 10f).coerceIn(TEXT_SCALE_MIN, TEXT_SCALE_MAX)
    }
}

@Serializable
data class StoredBookmark(val locatorJson: String, val label: String, val createdAt: Long)

class ReaderStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val display: Flow<ReaderDisplay> = context.readerDataStore.data.map { p ->
        ReaderDisplay(
            textScale = ReaderDisplay.clampTextScale(p[KEY_TEXT_SCALE] ?: 1.0f),
            typeface = enumOr(p[KEY_TYPEFACE], ReaderTypeface.SERIF),
            page = enumOr(p[KEY_PAGE], ReaderPage.DARK),
            layout = enumOr(p[KEY_LAYOUT], ReaderLayout.TWO_PAGES),
        )
    }

    suspend fun saveDisplay(d: ReaderDisplay) {
        context.readerDataStore.edit {
            it[KEY_TEXT_SCALE] = d.textScale
            it[KEY_TYPEFACE] = d.typeface.name
            it[KEY_PAGE] = d.page.name
            it[KEY_LAYOUT] = d.layout.name
        }
    }

    suspend fun position(bookId: String): String? = context.readerDataStore.data.first()[positionKey(bookId)]

    suspend fun savePosition(bookId: String, locatorJson: String) {
        context.readerDataStore.edit { it[positionKey(bookId)] = locatorJson }
    }

    fun bookmarks(bookId: String): Flow<List<StoredBookmark>> = context.readerDataStore.data.map { p ->
        p[bookmarksKey(bookId)]?.let { runCatching { json.decodeFromString<List<StoredBookmark>>(it) }.getOrNull() }.orEmpty()
    }

    suspend fun saveBookmarks(bookId: String, marks: List<StoredBookmark>) {
        context.readerDataStore.edit { it[bookmarksKey(bookId)] = json.encodeToString(marks) }
    }

    private companion object {
        val KEY_TEXT_SCALE = floatPreferencesKey("display_text_scale")
        val KEY_TYPEFACE = stringPreferencesKey("display_typeface")
        val KEY_PAGE = stringPreferencesKey("display_page")
        val KEY_LAYOUT = stringPreferencesKey("display_layout")

        fun positionKey(bookId: String) = stringPreferencesKey("position_$bookId")
        fun bookmarksKey(bookId: String) = stringPreferencesKey("bookmarks_$bookId")

        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: fallback
    }
}

fun toggleBookmark(marks: List<StoredBookmark>, candidate: StoredBookmark, samePlace: (StoredBookmark) -> Boolean): List<StoredBookmark> =
    if (marks.any(samePlace)) marks.filterNot(samePlace) else (marks + candidate).sortedBy { it.createdAt }
