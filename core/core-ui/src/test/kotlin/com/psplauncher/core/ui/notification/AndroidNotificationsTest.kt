package com.psplauncher.core.ui.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidNotificationsTest {
    @Before
    fun reset() = AndroidNotifications.disconnected()

    private fun notice(key: String, at: Long) =
        AndroidNotice(key = key, appLabel = "App", title = "T", text = null, postedAt = at)

    @Test
    fun `newest first, whatever order the system hands them over in`() {
        AndroidNotifications.publish(listOf(notice("a", 100), notice("b", 300), notice("c", 200)))
        assertEquals(listOf("b", "c", "a"), AndroidNotifications.active.value.map { it.key })
    }

    @Test
    fun `publishing REPLACES, so a dismissed notification actually goes`() {
        AndroidNotifications.publish(listOf(notice("a", 100), notice("b", 200)))
        AndroidNotifications.publish(listOf(notice("b", 200)))
        assertEquals(listOf("b"), AndroidNotifications.active.value.map { it.key })
    }

    @Test
    fun `losing the service empties it rather than freezing the last known set`() {
        AndroidNotifications.publish(listOf(notice("a", 100)))
        AndroidNotifications.disconnected()
        assertTrue(AndroidNotifications.active.value.isEmpty())
    }

    @Test
    fun `it starts empty, which is what ungranted access looks like`() {
        assertTrue(AndroidNotifications.active.value.isEmpty())
    }

    @Test
    fun `a notification carrying neither title nor text still draws, under the app's name`() {
        val notice = noticeOf("k", "Stremio", 5L, NoticeExtras())
        assertNotNull("a custom-RemoteViews notification has no title or text extras and must not vanish", notice)
        assertEquals("Stremio", notice!!.title)
        assertNull(notice.text)
    }

    @Test
    fun `a group summary is dropped, or a bundled app draws twice`() {
        assertNull(
            noticeOf("k", "Gmail", 5L, NoticeExtras(title = "3 new messages"), isGroupSummary = true),
        )
    }

    @Test
    fun `text falls back big text, then a message line, then sub text, then info text`() {
        fun textOf(e: NoticeExtras) = noticeOf("k", "App", 5L, e)!!.text

        assertEquals("plain", textOf(NoticeExtras(text = "plain", bigText = "big", message = "msg")))
        assertEquals("big", textOf(NoticeExtras(bigText = "big", message = "msg", subText = "sub")))
        assertEquals("msg", textOf(NoticeExtras(message = "msg", subText = "sub", infoText = "info")))
        assertEquals("sub", textOf(NoticeExtras(subText = "sub", infoText = "info")))
        assertEquals("info", textOf(NoticeExtras(infoText = "info")))
    }

    @Test
    fun `a blank extra is not a value, so it does not shadow the one behind it`() {
        val notice = noticeOf("k", "App", 5L, NoticeExtras(title = "   ", text = "", bigText = "real"))
        assertEquals("App", notice!!.title)
        assertEquals("real", notice.text)
    }

    @Test
    fun `a media session or a transport notice is now playing, so it leaves the list it could never be cleared from`() {
        assertTrue(isMediaPlayback(hasMediaSession = true, category = null))
        assertTrue("Stremio posts category=transport", isMediaPlayback(hasMediaSession = false, category = "transport"))
        assertTrue(!isMediaPlayback(hasMediaSession = false, category = "msg"))
        assertTrue(!isMediaPlayback(hasMediaSession = false, category = null))
        assertNull(noticeOf("k", "Stremio", 5L, NoticeExtras(title = "Hotel Del Luna"), mediaPlayback = true))
    }
}
