package com.psplauncher.core.data.book

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.psplauncher.core.domain.model.Book

object BuiltInReader {
    const val ACTIVITY_CLASS = "com.psplauncher.feature.reader.ReaderActivity"
    const val EXTRA_BOOK_ID = "psplauncher.reader.book_id"
    const val EXTRA_BOOK_URI = "psplauncher.reader.book_uri"
    const val EXTRA_BOOK_TITLE = "psplauncher.reader.book_title"
    const val EXTRA_BOOK_AUTHOR = "psplauncher.reader.book_author"

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
