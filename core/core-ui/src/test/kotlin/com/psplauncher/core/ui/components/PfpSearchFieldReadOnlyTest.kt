package com.psplauncher.core.ui.components

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.psplauncher.core.ui.theme.deriveStorefrontColors
import com.psplauncher.core.ui.preview.PfpScreenPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h480dp")
class PfpSearchFieldReadOnlyTest {
    @get:Rule
    val compose = createComposeRule()

    private fun field(readOnly: Boolean, onActivate: () -> Unit = {}, onQueryChange: (String) -> Unit = {}) {
        compose.setContent {
            PfpScreenPreview {
                PfpSearchField(
                    query = "",
                    active = false,
                    focusRequester = remember { FocusRequester() },
                    placeholder = "Search apps",
                    onActivate = onActivate,
                    onQueryChange = onQueryChange,
                    onDone = {},
                    colors = deriveStorefrontColors(),
                    readOnly = readOnly,
                )
            }
        }
    }

    @Test
    fun `a read-only field hands the tap on, because it opens the search page instead of typing`() {
        var opened = 0
        field(readOnly = true, onActivate = { opened++ })

        compose.onNodeWithText("Search apps").performClick()

        assertEquals("the whole pill is the button when it is read-only", 1, opened)
    }

    @Test
    fun `a read-only field takes no text, or the drawer would have a second search after all`() {
        val typed = mutableListOf<String>()
        field(readOnly = true, onQueryChange = { typed += it })

        runCatching { compose.onNodeWithText("Search apps").performTextInput("abc") }

        assertEquals("nothing may be typed into the stand-in", emptyList<String>(), typed)
    }

    @Test
    fun `an editable field still takes text, so the search page is unaffected`() {
        val typed = mutableListOf<String>()
        field(readOnly = false, onQueryChange = { typed += it })

        compose.onNodeWithText("Search apps").performTextInput("ab")

        assertEquals(listOf("ab"), typed)
    }
}
