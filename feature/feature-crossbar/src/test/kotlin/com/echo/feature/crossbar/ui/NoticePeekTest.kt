package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.echo.feature.crossbar.viewmodel.NoticeIslandPress
import com.echo.feature.crossbar.viewmodel.noticeIslandPress

// owner, 2026-10-05: a new notification peeks from the right-hand island, once
class NoticePeekTest {
    private fun peek(at: Long) = NoticePeek(at, "Title", null, null)

    @Test
    fun `only a notification newer than any seen peeks`() {
        assertTrue(noticePeekDue(seenAt = 100L, newest = peek(200L)))
        assertFalse("one already there does not peek again", noticePeekDue(seenAt = 200L, newest = peek(200L)))
        assertFalse("an older one left after a dismissal does not peek", noticePeekDue(seenAt = 200L, newest = peek(150L)))
        assertFalse(noticePeekDue(seenAt = 0L, newest = null))
    }

    @Test
    fun `the first press brings the card out and the second opens the notifications`() {
        assertEquals(NoticeIslandPress.SHOW_CARD, noticeIslandPress(cardOut = false, hasNotice = true))
        assertEquals(NoticeIslandPress.OPEN_PANEL, noticeIslandPress(cardOut = true, hasNotice = true))
        assertEquals("nothing to show: open the notifications at once", NoticeIslandPress.OPEN_PANEL, noticeIslandPress(cardOut = false, hasNotice = false))
    }
}
