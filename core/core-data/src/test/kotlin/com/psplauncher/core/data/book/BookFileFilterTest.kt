package com.psplauncher.core.data.book

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookFileFilterTest {
    @Test
    fun `the real epub mime is accepted`() =
        assertTrue(BookFileFilter.isBook("Dune.epub", "application/epub+zip"))

    @Test
    fun `an octet-stream epub is accepted on its extension`() =
        assertTrue(BookFileFilter.isBook("Dune.epub", "application/octet-stream"))

    @Test
    fun `a missing mime falls back to the extension, case insensitively`() {
        assertTrue(BookFileFilter.isBook("Dune.EPUB", null))
        assertTrue(BookFileFilter.isBook("Dune.EpUb", null))
    }

    @Test
    fun `a file the provider types as something else is refused`() {
        assertFalse(BookFileFilter.isBook("cover.jpg", "image/jpeg"))
        assertFalse(BookFileFilter.isBook("audiobook.m4b", "audio/mp4"))
    }

    @Test
    fun `an unknown extension under octet-stream is refused rather than guessed`() =
        assertFalse(BookFileFilter.isBook("notes.txt", "application/octet-stream"))

    @Test
    fun `an extensionless file is refused rather than guessed`() =
        assertFalse(BookFileFilter.isBook("Dune", null))

    @Test
    fun `a dotfile is not read as an extension`() =
        assertFalse(BookFileFilter.isBook(".epub", null))

    @Test
    fun `pdf and cbz are books, by type or by extension`() {
        assertTrue(BookFileFilter.isBook("Manual.pdf", "application/pdf"))
        assertTrue(BookFileFilter.isBook("Akira 01.cbz", "application/vnd.comicbook+zip"))
        assertTrue(BookFileFilter.isBook("Akira 01.cbz", "application/octet-stream"))
        assertTrue(BookFileFilter.isBook("Manual.PDF", null))
    }

    @Test
    fun `a generic file's type is read from its extension`() {
        kotlin.test.assertEquals(BookFileFilter.PDF_MIME, BookFileFilter.mimeForName("Manual.pdf"))
        kotlin.test.assertEquals(BookFileFilter.CBZ_MIME, BookFileFilter.mimeForName("Akira.cbz"))
        kotlin.test.assertEquals(BookFileFilter.EPUB_MIME, BookFileFilter.mimeForName("Dune.epub"))
    }
}
