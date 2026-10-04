package com.echo.feature.reader

data class ChapterEntry(val title: String, val href: String, val depth: Int)

data class PagePosition(val href: String, val position: Int, val totalProgression: Double)

fun hrefPath(href: String): String = href.substringBefore('#')

fun currentChapterIndex(chapters: List<ChapterEntry>, readingOrder: List<String>, locatorHref: String): Int {
    if (chapters.isEmpty()) return -1
    val order = readingOrder.map(::hrefPath)
    val here = order.indexOf(hrefPath(locatorHref))
    if (here < 0) return chapters.indexOfFirst { hrefPath(it.href) == hrefPath(locatorHref) }
    var best = -1
    chapters.forEachIndexed { i, c ->
        val at = order.indexOf(hrefPath(c.href))
        if (at in 0..here && (best < 0 || at >= order.indexOf(hrefPath(chapters[best].href)))) best = i
    }
    return best
}

fun chapterStarts(chapters: List<ChapterEntry>, positions: List<PagePosition>): List<Double> =
    chapters.filter { it.depth == 0 }.mapNotNull { c ->
        positions.firstOrNull { it.href == hrefPath(c.href) }?.totalProgression
    }.filter { it > 0.0 }.distinct()

// Readium positions are 1024-character slices of the source, not screens, so a count of them
// reads as "pages" and is wrong by a factor that changes with the text size. A share is honest.
fun chapterShareLeft(
    chapters: List<ChapterEntry>,
    readingOrder: List<String>,
    positions: List<PagePosition>,
    locatorHref: String,
    totalProgression: Double?,
): Double? {
    if (totalProgression == null || positions.isEmpty()) return null
    val order = readingOrder.map(::hrefPath)
    val here = order.indexOf(hrefPath(locatorHref))
    val chapter = chapters.getOrNull(currentChapterIndex(chapters, readingOrder, locatorHref)) ?: return null
    val start = positions.firstOrNull { it.href == hrefPath(chapter.href) }?.totalProgression ?: return null
    val next = chapters.firstOrNull { order.indexOf(hrefPath(it.href)) > here }
    val end = next?.let { n -> positions.firstOrNull { it.href == hrefPath(n.href) }?.totalProgression } ?: 1.0
    if (end <= start) return null
    return ((end - totalProgression) / (end - start)).coerceIn(0.0, 1.0)
}

fun adjacentChapterHref(chapters: List<ChapterEntry>, readingOrder: List<String>, locatorHref: String, delta: Int): String? {
    val current = currentChapterIndex(chapters, readingOrder, locatorHref)
    return chapters.getOrNull(current + delta)?.href
}
