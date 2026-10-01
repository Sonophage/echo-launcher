package com.psplauncher.core.data.book

object BookFileFilter {
    const val EPUB_MIME = "application/epub+zip"
    const val PDF_MIME = "application/pdf"
    const val CBZ_MIME = "application/vnd.comicbook+zip"

    private const val GENERIC_MIME = "application/octet-stream"

    val BOOK_EXTENSIONS = setOf("epub", "pdf", "cbz")

    val BOOK_MIMES = setOf(EPUB_MIME, PDF_MIME, CBZ_MIME, "application/x-cbz")

    fun isBook(fileName: String, mimeType: String?): Boolean {
        if (mimeType != null && mimeType != GENERIC_MIME) return mimeType in BOOK_MIMES
        return extensionOf(fileName) in BOOK_EXTENSIONS
    }

    fun mimeForName(fileName: String): String = when (extensionOf(fileName)) {
        "pdf" -> PDF_MIME
        "cbz" -> CBZ_MIME
        else -> EPUB_MIME
    }

    private fun extensionOf(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > 0) fileName.substring(dot + 1).lowercase() else ""
    }
}
