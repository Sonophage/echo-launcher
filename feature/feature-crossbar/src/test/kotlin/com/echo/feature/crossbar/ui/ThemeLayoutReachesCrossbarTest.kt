package com.echo.feature.crossbar.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// owner, 2026-10-09: a theme's layout sizes the crossbar. A size read from CrossbarLayoutSpec.DEFAULT ignores the
// applied theme, so the crossbar reads LocalCrossbarLayout instead. categoryIconSelectedDp is left to the focus
// styles: the selected category icon is drawn the same size as the rest today
class ThemeLayoutReachesCrossbarTest {
    @Test
    fun `the crossbar takes its sizes from the applied theme, not from the default`() {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "feature/feature-crossbar/src/main/kotlin") }.first { it.isDirectory }
        val sources = dir.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(sources.size > 50, "found ${sources.size} sources; the scan is not reading the module")
        val hardCoded = sources.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line ->
                "${f.name}:${i + 1}".takeIf { "CrossbarLayoutSpec.DEFAULT." in line }
            }
        }
        assertEquals(emptyList(), hardCoded)
        assertTrue(sources.any { "LocalCrossbarLayout provides" in it.readText() }, "nothing provides the theme's layout")
    }
}
