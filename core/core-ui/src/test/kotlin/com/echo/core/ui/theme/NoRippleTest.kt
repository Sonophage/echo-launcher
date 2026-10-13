package com.echo.core.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// spec #3: a press shows the active focus style (PressMark), never Material's ripple. EchoTheme provides the press
// mark as LocalIndication and turns off the ripple Material components draw; nothing may ask for a ripple itself
class NoRippleTest {
    private val rippleCall = Regex("""\b(ripple|rememberRipple|RippleConfiguration)\s*\(""")

    private fun repoRoot(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    @Test
    fun `the detector sees a ripple in each form it has been written in`() {
        // the notification orb's own ripple, before spec #3
        assertTrue(rippleCall.containsMatchIn("indication = androidx.compose.material3.ripple(bounded = false, radius = u.dp(28)),"))
        assertTrue(rippleCall.containsMatchIn("indication = rememberRipple(bounded = false)"))
        assertTrue(rippleCall.containsMatchIn("LocalRippleConfiguration provides RippleConfiguration(color = Color.Red)"))
        assertTrue(!rippleCall.containsMatchIn("LocalRippleConfiguration provides null,"))
    }

    @Test
    fun `no source asks for a Material ripple`() {
        val sources = repoRoot().walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
        assertTrue(sources.size > 300, "found ${sources.size} sources; the scan is not reading the repo")
        val ripples = sources.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> "${f.name}:${i + 1}".takeIf { rippleCall.containsMatchIn(line) } }
        }
        assertEquals(emptyList(), ripples)
    }

    @Test
    fun `the theme gives every clickable the press mark and turns the Material ripple off`() {
        val theme = File(repoRoot(), "core/core-ui/src/main/kotlin/com/echo/core/ui/theme/EchoTheme.kt").readText()
        assertTrue("LocalIndication provides PressMark" in theme, "EchoTheme does not provide the press mark")
        assertTrue("LocalRippleConfiguration provides null" in theme, "EchoTheme leaves Material components their ripple")
    }
}
