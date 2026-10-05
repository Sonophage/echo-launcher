package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.RecentFilter
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w821dp-h462dp")
class RecentTapLaunchesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val items = listOf(
        CrossbarItem(id = "a", title = "Alpha Game", gameId = 1L),
        CrossbarItem(id = "b", title = "Bravo Game", gameId = 2L),
        CrossbarItem(id = "c", title = "Charlie Game", gameId = 3L),
    )

    private fun page(selectedIndex: Int, railVisible: Boolean, onCardPressed: (Int, Boolean) -> Unit = { _, _ -> }, onCardTapped: (Int) -> Unit) {
        composeRule.setContent {
            EchoScreenPreview {
                LastPlayedPage(
                    items = items,
                    selectedIndex = selectedIndex,
                    listState = rememberLazyListState(),
                    filter = RecentFilter.ALL,
                    railVisible = railVisible,
                    onCardTapped = onCardTapped,
                    onCardPressed = onCardPressed,
                )
            }
        }
    }

    // Guards the page's contract only. Whether that tap LAUNCHES is decided by
    // onRecentCardTap in CrossbarRecents, which no test can reach: nothing in this
    // repo constructs CrossbarViewModel. That half is verified on hardware.
    @Test
    fun `a tapped card carries its own index, not the focused one`() {
        val tapped = mutableListOf<Int>()
        page(selectedIndex = 0, railVisible = true, onCardTapped = { tapped += it })

        composeRule.onNodeWithText("Charlie Game").performClick()

        assertEquals(
            "a single tap on an unfocused recent must carry its own index straight through",
            listOf(2),
            tapped,
        )
    }

    @Test
    fun `tapping the art acts on the focused row, so it opens rather than toggling the rail`() {
        val tapped = mutableListOf<Int>()
        page(selectedIndex = 1, railVisible = false, onCardTapped = { tapped += it })

        composeRule.onNodeWithText("Bravo Game").performClick()

        assertEquals("the art carries the focused index", listOf(1), tapped)
    }

    // owner, 2026-10-05: holding a card or row should launch it, as holding A does. The page reports the
    // finger down and up with the row's own index; CrossbarRecents times the hold on the shared launch hold
    @Test
    fun `holding a row reports its own index down, then up`() {
        val presses = mutableListOf<Pair<Int, Boolean>>()
        page(selectedIndex = 0, railVisible = true, onCardPressed = { i, down -> presses += i to down }, onCardTapped = {})

        composeRule.onNodeWithText("Charlie Game").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.onNodeWithText("Charlie Game").performTouchInput { up() }
        composeRule.waitForIdle()

        assertEquals(listOf(2 to true, 2 to false), presses)
    }

    @Test
    fun `holding the art reports the focused index`() {
        val presses = mutableListOf<Pair<Int, Boolean>>()
        page(selectedIndex = 1, railVisible = false, onCardPressed = { i, down -> presses += i to down }, onCardTapped = {})

        composeRule.onNodeWithText("Bravo Game").performTouchInput { down(center) }
        composeRule.onNodeWithText("Bravo Game").performTouchInput { up() }
        composeRule.waitForIdle()

        assertEquals(listOf(1 to true, 1 to false), presses)
    }
}
