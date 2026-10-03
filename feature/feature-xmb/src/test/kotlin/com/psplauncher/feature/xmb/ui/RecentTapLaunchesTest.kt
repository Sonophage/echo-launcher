package com.psplauncher.feature.xmb.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.psplauncher.core.ui.preview.PfpScreenPreview
import com.psplauncher.feature.xmb.viewmodel.RecentFilter
import com.psplauncher.feature.xmb.viewmodel.XMBItem
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
        XMBItem(id = "a", title = "Alpha Game", gameId = 1L),
        XMBItem(id = "b", title = "Bravo Game", gameId = 2L),
        XMBItem(id = "c", title = "Charlie Game", gameId = 3L),
    )

    private fun page(selectedIndex: Int, railVisible: Boolean, onCardTapped: (Int) -> Unit) {
        composeRule.setContent {
            PfpScreenPreview {
                LastPlayedPage(
                    items = items,
                    selectedIndex = selectedIndex,
                    listState = rememberLazyListState(),
                    filter = RecentFilter.ALL,
                    railVisible = railVisible,
                    onCardTapped = onCardTapped,
                    onAction = {},
                )
            }
        }
    }

    // Guards the page's contract only. Whether that tap LAUNCHES is decided by
    // onRecentCardTap in XMBViewModel, which no test can reach: nothing in this
    // repo constructs XMBViewModel. That half is verified on hardware.
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
}
