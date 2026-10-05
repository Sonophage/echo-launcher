package com.echo.feature.crossbar.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.echo.core.ui.preview.EchoScreenPreview
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
class StripNoticeCountTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun strip(
        noticeCount: Int,
        live: StripLiveActivity? = null,
        onCountTapped: () -> Unit = {},
    ) {
        composeRule.setContent {
            EchoScreenPreview {
                CrossbarStatusStrip(
                    live = live,
                    noticeCount = noticeCount,
                    onNoticeIslandPressed = onCountTapped,
                )
            }
        }
    }

    @Test
    fun `the count sits in the right-hand cluster, so the island is free for media and games`() {
        strip(noticeCount = 3, live = StripLiveActivity(art = null, title = "Some Song", detail = "An Artist"))

        composeRule.onNodeWithContentDescription("3 notifications").assertIsDisplayed()

        val countRight = composeRule.onNodeWithContentDescription("3 notifications")
            .fetchSemanticsNode().boundsInRoot.left
        val islandRight = composeRule.onNodeWithText("Some Song")
            .fetchSemanticsNode().boundsInRoot.right

        assertEquals(
            "the notification count must draw to the right of the island, not inside it",
            true,
            countRight > islandRight,
        )
    }

    @Test
    fun `no notifications draws no counter at all, rather than a zero`() {
        strip(noticeCount = 0)
        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    // owner, 2026-10-05: the island takes the press; the view model decides card first, then the panel
    fun `tapping the notification island is what reaches the notifications`() {
        var taps = 0
        strip(noticeCount = 2, onCountTapped = { taps++ })

        composeRule.onNodeWithContentDescription("2 notifications").performClick()

        assertEquals("the island is the tap target for the notifications", 1, taps)
    }
}
