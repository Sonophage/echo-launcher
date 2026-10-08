package com.echo.feature.video

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: the video's details named its container "X-MATROSKA"
class ContainerLabelTest {
    @Test
    fun `MIME subtypes read as the file kinds people know`() {
        assertEquals("MKV", containerLabel("X-MATROSKA"))
        assertEquals("MP4", containerLabel("MP4"))
        assertEquals("MOV", containerLabel("quicktime"))
        assertEquals("an unknown one drops its x- prefix", "FOO", containerLabel("x-foo"))
    }
}
