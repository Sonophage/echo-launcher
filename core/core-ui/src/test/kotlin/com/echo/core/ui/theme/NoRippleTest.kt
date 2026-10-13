package com.echo.core.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// spec #3: a press shows the active focus style (PressMark), never Material's ripple. EchoTheme provides the press
// mark as LocalIndication and turns off the ripple Material components draw; nothing may ask for a ripple itself.
// EchoThemeTest checks what the theme provides
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

    private val sources: List<File> by lazy {
        repoRoot().walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
            .also { assertTrue(it.size > 300, "found ${it.size} sources; the scan is not reading the repo") }
    }

    @Test
    fun `no source asks for a Material ripple`() {
        val ripples = sources.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> "${f.name}:${i + 1}".takeIf { rippleCall.containsMatchIn(line) } }
        }
        assertEquals(emptyList(), ripples)
    }

    @Test
    fun `the control detector sees a Material control without its own source, and nothing else`() {
        val switch = "import androidx.compose.material3.Switch\n"
        assertEquals(listOf("Switch at 2"), materialCallsWithoutSource("${switch}Switch(\n  checked = on,\n  onCheckedChange = f,\n)"))
        assertEquals(emptyList(), materialCallsWithoutSource("${switch}Switch(checked = on, onCheckedChange = f(x), interactionSource = press)"))
        // an ECHO composable of a like name, and a control the file does not import, are not Material's
        assertEquals(emptyList(), materialCallsWithoutSource("import androidx.compose.material3.Button\nPanelButton(a, b) { }"))
        assertEquals(emptyList(), materialCallsWithoutSource("Checkbox(checked = on, onCheckedChange = f)"))
        assertEquals(listOf("IconButton at 1"), materialCallsWithoutSource("androidx.compose.material3.IconButton(onClick = f) { }"))
    }

    @Test
    fun `every Material control carries an interaction source`() {
        // Theme Studio is a desktop editor, not drawn inside EchoTheme
        val launcher = sources.filterNot { it.invariantSeparatorsPath.startsWith(File(repoRoot(), "studio").invariantSeparatorsPath + "/") }
        assertEquals(emptyList(), launcher.flatMap { f -> materialCallsWithoutSource(f.readText()).map { "${f.name}: $it" } })
    }
}

// a Material control draws its own ripple (switched off by EchoTheme), never LocalIndication, so a call that does not
// pass an interactionSource shows no press at all. The source carries the press mark, or the control's own focus look
private val materialControls = listOf(
    "Switch", "Slider", "RangeSlider", "Checkbox", "TriStateCheckbox", "RadioButton", "Button", "TextButton",
    "OutlinedButton", "FilledTonalButton", "ElevatedButton", "IconButton", "FilledIconButton", "IconToggleButton",
    "FilterChip", "AssistChip", "InputChip", "SuggestionChip",
)

internal fun materialCallsWithoutSource(source: String): List<String> {
    val imported = materialControls.filter { Regex("""import androidx\.compose\.material3\.$it\b""").containsMatchIn(source) }
    val call = Regex("""(?:(androidx\.compose\.material3\.)|(?<![\w.]))(${materialControls.joinToString("|")})\s*\(""")
    return call.findAll(source).mapNotNull { m ->
        val name = m.groupValues[2]
        if (m.groupValues[1].isEmpty() && name !in imported) return@mapNotNull null
        // the call's arguments, to its closing parenthesis
        var depth = 0
        var end = m.range.last
        while (end < source.length) {
            when (source[end]) {
                '(' -> depth++
                ')' -> if (--depth == 0) break
            }
            end++
        }
        val line = source.substring(0, m.range.first).count { it == '\n' } + 1
        "$name at $line".takeIf { "interactionSource" !in source.substring(m.range.first, end) }
    }.toList()
}
