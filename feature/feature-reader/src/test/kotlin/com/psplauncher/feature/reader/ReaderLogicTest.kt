package com.psplauncher.feature.reader

import com.psplauncher.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderLogicTest {
    private val order = listOf("text/ch1.xhtml", "text/ch2.xhtml", "text/ch3.xhtml")
    private val chapters = listOf(
        ChapterEntry("Loomings", "text/ch1.xhtml", 0),
        ChapterEntry("The Carpet-Bag", "text/ch2.xhtml", 0),
        ChapterEntry("A section", "text/ch2.xhtml#s2", 1),
        ChapterEntry("The Spouter-Inn", "text/ch3.xhtml", 0),
    )
    private val positions = (1..9).map { i ->
        PagePosition(order[(i - 1) / 3], i, (i - 1) / 9.0)
    }

    @Test fun `the current chapter is the last one that starts at or before the page`() {
        assertEquals(1, currentChapterIndex(chapters.take(2) + chapters.last(), order, "text/ch2.xhtml"))
        assertEquals(0, currentChapterIndex(chapters, order, "text/ch1.xhtml#p5"))
    }

    @Test fun `chapter ticks sit where each top-level chapter starts, not at zero`() {
        assertEquals(listOf(3 / 9.0, 6 / 9.0), chapterStarts(chapters, positions))
    }

    @Test fun `the share left runs to the start of the next chapter, not the end of the book`() {
        assertEquals(1.0, chapterShareLeft(chapters, order, positions, "text/ch1.xhtml", 0.0)!!, 1e-9)
        assertEquals("a third of the way into chapter two", 2 / 3.0, chapterShareLeft(chapters, order, positions, "text/ch2.xhtml", 4 / 9.0)!!, 1e-9)
        assertEquals("the last chapter runs to the end of the book", 0.5, chapterShareLeft(chapters, order, positions, "text/ch3.xhtml", 6 / 9.0 + 3 / 18.0)!!, 1e-9)
    }

    @Test fun `next and previous chapter step through the contents`() {
        assertEquals("text/ch2.xhtml", adjacentChapterHref(chapters, order, "text/ch1.xhtml", 1))
        assertNull(adjacentChapterHref(chapters, order, "text/ch1.xhtml", -1))
    }

    @Test fun `while reading, the shoulders change chapter and B closes`() {
        assertEquals(ReaderCommand.NextChapter, readingCommand(GamepadAction.NEXT_CATEGORY))
        assertEquals(ReaderCommand.PageForward, readingCommand(GamepadAction.NAVIGATE_RIGHT))
        assertEquals(ReaderCommand.Close, readingCommand(GamepadAction.BACK))
        assertEquals(ReaderCommand.ToggleBookmark, readingCommand(GamepadAction.CHANGE_SORT))
    }

    @Test fun `left and right only adjust on the Display tab`() {
        assertEquals(ReaderCommand.Adjust(1), optionsCommand(GamepadAction.NAVIGATE_RIGHT, OptionsTab.DISPLAY))
        assertNull(optionsCommand(GamepadAction.NAVIGATE_RIGHT, OptionsTab.CONTENTS))
    }

    @Test fun `text size steps by ten percent and stops at its limits`() {
        val d = ReaderDisplay(textScale = 1.0f)
        assertEquals(1.1f, adjustDisplay(d, DisplayRow.TEXT_SIZE, 1).textScale, 0.0001f)
        assertEquals(ReaderDisplay.TEXT_SCALE_MAX, adjustDisplay(d.copy(textScale = 2.0f), DisplayRow.TEXT_SIZE, 1).textScale, 0.0001f)
        assertEquals(ReaderDisplay.TEXT_SCALE_MIN, adjustDisplay(d.copy(textScale = 0.8f), DisplayRow.TEXT_SIZE, -1).textScale, 0.0001f)
    }

    @Test fun `page colours cycle through dark, sepia and paper`() {
        assertEquals(ReaderPage.SEPIA, adjustDisplay(ReaderDisplay(), DisplayRow.PAGE, 1).page)
        assertEquals(ReaderPage.PAPER, adjustDisplay(ReaderDisplay(), DisplayRow.PAGE, -1).page)
    }

    @Test fun `a bookmark on the same page is removed instead of duplicated`() {
        val a = StoredBookmark("{}", "Loomings · 1%", 1)
        val b = StoredBookmark("{}", "Loomings · 1%", 2)
        assertEquals(listOf(a), toggleBookmark(emptyList(), a) { false })
        assertEquals(emptyList<StoredBookmark>(), toggleBookmark(listOf(a), b) { it == a })
    }
}
