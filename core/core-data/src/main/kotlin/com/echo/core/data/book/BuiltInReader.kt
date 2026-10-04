package com.echo.core.data.book

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.echo.core.domain.model.Book

object BuiltInReader {
    const val ACTIVITY_CLASS = "com.echo.feature.reader.ReaderActivity"
    const val EXTRA_BOOK_ID = "echo.reader.book_id"
    const val EXTRA_BOOK_URI = "echo.reader.book_uri"
    const val EXTRA_BOOK_TITLE = "echo.reader.book_title"
    const val EXTRA_BOOK_AUTHOR = "echo.reader.book_author"

    const val ASK_EVERY_TIME = "__ask__"

    fun isBuiltIn(defaultReader: String?): Boolean = defaultReader == null

    fun intent(context: Context, book: Book): Intent =
        Intent()
            .setComponent(ComponentName(context.packageName, ACTIVITY_CLASS))
            .putExtra(EXTRA_BOOK_ID, book.id)
            .putExtra(EXTRA_BOOK_URI, book.uri)
            .putExtra(EXTRA_BOOK_TITLE, book.displayTitle)
            .putExtra(EXTRA_BOOK_AUTHOR, book.author)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
