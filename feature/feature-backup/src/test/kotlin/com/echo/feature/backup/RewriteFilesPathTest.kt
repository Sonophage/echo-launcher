package com.echo.feature.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// a backup taken by com.psplauncher and restored into com.echo.launcher (2.0.0) kept video posters
// pointing at the old package's files dir
class RewriteFilesPathTest {
    private val here = "/data/user/0/com.echo.launcher.debug/files"

    @Test
    fun `a file URI under another app's files dir moves here and stays a file URI`() = assertEquals(
        "file://$here/video_posters/a9e4.jpg",
        rewriteFilesPath("file:///data/user/0/com.psplauncher.launcher.debug/files/video_posters/a9e4.jpg", here),
    )

    @Test
    fun `a bare path moves here and stays bare`() = assertEquals(
        "$here/artwork/1.jpg",
        rewriteFilesPath("/data/data/com.psplauncher.launcher/files/artwork/1.jpg", here),
    )

    @Test
    fun `anything outside a files dir is left alone`() {
        val saf = "content://com.android.externalstorage.documents/tree/6DBF-B253%3AEmulation%2FECHO"
        assertEquals(saf, rewriteFilesPath(saf, here))
        assertNull(rewriteFilesPath(null, here))
    }
}
