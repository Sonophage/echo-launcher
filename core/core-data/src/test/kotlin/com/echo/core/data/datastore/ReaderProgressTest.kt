package com.echo.core.data.datastore

import androidx.datastore.preferences.core.preferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderProgressTest {
    // the shape Readium's Locator.toJSON() writes, which is what the reader saves
    private val locator = """
        {"href":"OEBPS/ch07.xhtml","type":"application/xhtml+xml","title":"Chapter 7",
         "locations":{"progression":0.5,"position":118,"totalProgression":0.42},
         "text":{"highlight":"It was a pleasure to burn."}}
    """.trimIndent()

    @Test
    fun `the key is the one the reader has always written, so saved places survive an update`() =
        assertEquals("position_b1", readerPositionKey("b1").name)

    @Test
    fun `progress is the whole book's, not the chapter's`() =
        assertEquals(0.42f, readerProgress(preferencesOf(readerPositionKey("b1") to locator), "b1")!!, 0.0001f)

    @Test
    fun `a book the reader has no place for has no progress`() {
        assertNull(readerProgress(preferencesOf(readerPositionKey("other") to locator), "b1"))
        assertNull(readerProgress(preferencesOf(readerPositionKey("b1") to """{"href":"a","locations":{"position":3}}"""), "b1"))
    }

    @Test
    fun `a damaged saved place gives no progress instead of crashing the crossbar`() =
        assertNull(readerProgress(preferencesOf(readerPositionKey("b1") to "{not json"), "b1"))
}
