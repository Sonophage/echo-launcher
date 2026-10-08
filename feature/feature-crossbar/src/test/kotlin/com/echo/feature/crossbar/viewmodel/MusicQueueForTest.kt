package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.MusicTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-08: A on a Recent track must play it even when the crossbar is browsing something else
class MusicQueueForTest {
    private fun t(id: String, album: String?, n: Int?, folder: String = "f") =
        MusicTrack(id = id, folderId = folder, uri = "content://$id", displayName = "$id.flac", album = album, trackNumber = n)

    private val library = listOf(t("b2", "Born Pink", 2), t("x", "Other", 1), t("b1", "Born Pink", 1), t("solo", null, null))

    @Test
    fun `a track outside the browsed list plays in its album, in track order`() {
        val (queue, start) = musicQueueFor("b2", browsing = emptyList(), library = library)!!
        assertEquals(listOf("b1", "b2"), queue.map { it.id })
        assertEquals(1, start)
    }

    @Test
    fun `the browsed list wins when it holds the track, and a lone track plays alone`() {
        val browsing = listOf(t("x", "Other", 1), t("b2", "Born Pink", 2))
        assertEquals(browsing to 1, musicQueueFor("b2", browsing, library))
        assertEquals(listOf("solo"), musicQueueFor("solo", emptyList(), library)!!.first.map { it.id })
        assertNull(musicQueueFor("gone", emptyList(), library))
    }
}
