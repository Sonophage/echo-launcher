package com.echo.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

// Move on the crossbar only sees what is shown. Settings lives on the panel and disabled systems are not
// in the Games column; if their stored slots moved, a later Move would swap with an invisible neighbour
// and look like nothing happened.
class ShownOrderTest {
    @Test
    fun `shown ids take the new order and a hidden id keeps its slot`() {
        val all = listOf("games", "music", "settings", "video", "photo")
        val shown = listOf("video", "games", "music", "photo")
        assertEquals(listOf("video", "games", "settings", "music", "photo"), reorderedKeepingHidden(all, shown))
    }

    @Test
    fun `an id that is not stored is ignored, never duplicated`() {
        assertEquals(listOf("b", "a"), reorderedKeepingHidden(listOf("a", "b"), listOf("b", "ghost", "a")))
    }

    @Test
    fun `an unchanged order writes back the same order`() {
        val all = listOf("a", "x", "b", "c")
        assertEquals(all, reorderedKeepingHidden(all, listOf("a", "b", "c")))
    }
}
