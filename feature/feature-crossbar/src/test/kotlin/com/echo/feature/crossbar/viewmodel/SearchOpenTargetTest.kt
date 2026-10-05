package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.Photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// a photo or track picked in search opened nothing: closeSearch emptied the result lists before the
// row was looked up in them (owner, 2026-10-04)
class SearchOpenTargetTest {
    private val photo = Photo(id = "p1", libraryId = "lib", uri = "content://p1", displayName = "0cf77d42.jpg")
    private val track = MusicTrack(id = "t1", folderId = "f", uri = "content://t1", displayName = "&burn.m4a")

    @Test
    fun `a photo row resolves to its photo from the held results`() =
        assertEquals(SearchOpen.Photo(photo), searchOpenTarget(photo.toSearchRow(), listOf(photo), emptyList()))

    @Test
    fun `a track row resolves to its track from the held results`() =
        assertEquals(SearchOpen.Track(track), searchOpenTarget(track.toSearchRow(), emptyList(), listOf(track)))

    @Test
    fun `once the results are gone there is nothing to open, which is why they are read first`() {
        assertNull(searchOpenTarget(photo.toSearchRow(), emptyList(), emptyList()))
        assertNull(searchOpenTarget(track.toSearchRow(), emptyList(), emptyList()))
    }
}
