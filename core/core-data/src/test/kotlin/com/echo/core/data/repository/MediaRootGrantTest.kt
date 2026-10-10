package com.echo.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// seen on the Konker, 2026-10-10: a media folder was kept with read access only, so Delete From Device was refused
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MediaRootGrantTest {
    private val readWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    private val uri = Uri.parse("content://com.android.externalstorage.documents/tree/6DBF-B253%3APictures")

    private fun repo(resolver: ContentResolver) = MediaRootRepository(mockk<Context>(relaxed = true) { every { contentResolver } returns resolver })

    @Test
    fun `a media folder is kept with write access, so its files can be deleted`() {
        val resolver = mockk<ContentResolver>(relaxed = true)
        repo(resolver).persist(uri)
        verify(exactly = 1) { resolver.takePersistableUriPermission(uri, readWrite) }
    }

    @Test
    fun `a folder that gives no write is still kept for reading`() {
        val resolver = mockk<ContentResolver>(relaxed = true)
        every { resolver.takePersistableUriPermission(uri, readWrite) } throws SecurityException("no write")
        repo(resolver).persist(uri)
        verify(exactly = 1) { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
