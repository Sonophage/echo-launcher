package com.echo.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-08: a track's or book's genre as it is shown and grouped
class MediaGenreTest {
    @Test
    fun `the owner's genre first, then the tag's first named part`() {
        assertEquals("K-Pop", mediaGenreName("Pop", "K-Pop"))
        assertEquals("Pop", mediaGenreName("Pop;Dance", null))
        assertEquals("an ID3 number prefix is dropped", "Rock", mediaGenreName("(17)Rock", null))
        assertEquals("Hip-Hop", mediaGenreName("Hip-Hop/Rap", null))
        assertNull("read and empty", mediaGenreName("", null))
        assertNull("a bare number names nothing", mediaGenreName("(17)", " "))
    }
}
