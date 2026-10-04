package com.echo.core.data.book

import androidx.test.core.app.ApplicationProvider
import com.echo.core.domain.model.Book
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BookIntentResolverTest {
    private val resolver = BookIntentResolver(ApplicationProvider.getApplicationContext())

    private fun book(mime: String?) = Book(
        id = "b1",
        libraryId = "L1",
        uri = "content://com.example/tree/dune.epub",
        displayName = "dune.epub",
        mimeType = mime,
    )

    @Test
    fun `a provider that knows the type has it forwarded`() {
        assertEquals(
            "application/epub+zip",
            resolver.buildViewIntent(book("application/epub+zip"), null).type,
        )
    }

    @Test
    fun `octet-stream is replaced, not forwarded, or no reader resolves`() {
        assertEquals(
            BookFileFilter.EPUB_MIME,
            resolver.buildViewIntent(book("application/octet-stream"), null).type,
        )
    }

    @Test
    fun `a missing type falls back to epub, which is why the row exists`() {
        assertEquals(BookFileFilter.EPUB_MIME, resolver.buildViewIntent(book(null), null).type)
    }

    @Test
    fun `no reader chosen means no package pinned, so the chooser can run`() {
        assertNull(resolver.buildViewIntent(book(null), null).`package`)
        assertEquals(
            "com.flyersoft.moonreader",
            resolver.buildViewIntent(book(null), "com.flyersoft.moonreader").`package`,
        )
    }

    @Test
    fun `no reader chosen now means the built-in reader, and the intent names it`() {
        kotlin.test.assertTrue(BuiltInReader.isBuiltIn(null))
        kotlin.test.assertFalse(BuiltInReader.isBuiltIn(BuiltInReader.ASK_EVERY_TIME), "Ask Every Time is a choice, not the built-in reader")
        val intent = BuiltInReader.intent(ApplicationProvider.getApplicationContext(), book("application/pdf"))
        assertEquals(BuiltInReader.ACTIVITY_CLASS, intent.component?.className)
        assertEquals("b1", intent.getStringExtra(BuiltInReader.EXTRA_BOOK_ID))
        assertEquals("content://com.example/tree/dune.epub", intent.getStringExtra(BuiltInReader.EXTRA_BOOK_URI))
    }
}
